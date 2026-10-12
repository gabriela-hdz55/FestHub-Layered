[CmdletBinding()]
param()

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

$RepoRoot = Split-Path -Parent $PSScriptRoot
Set-Location $RepoRoot

$NetworkName = "festhub-layered_app_net"
$AllowedClientIp = "10.10.1.10"
$GrpcImage = "fullstorydev/grpcurl:latest"
$ProtoDir = Join-Path $RepoRoot "proto\src\main\proto"
$TestSuffix = "{0}-{1}" -f (Get-Date -Format "yyyyMMddHHmmss"), ([Guid]::NewGuid().ToString("N").Substring(0, 8))

$script:FrontendWasRunning = $false
$script:FrontendContainerExisted = $false
$script:PassedTests = 0
$script:Failed = $false

function Write-Step {
    param([string]$Message)
    Write-Host "`n==> $Message" -ForegroundColor Cyan
}

function Pass-Test {
    param([string]$Name)
    $script:PassedTests++
    Write-Host "[PASS] $Name" -ForegroundColor Green
}

function Fail-Test {
    param(
        [string]$Message,
        [object]$Result = $null
    )

    Write-Host "[FAIL] $Message" -ForegroundColor Red
    if ($null -ne $Result) {
        Write-Host "RPC exit code: $($Result.ExitCode)" -ForegroundColor DarkRed
        Write-Host "RPC output:" -ForegroundColor DarkRed
        Write-Host $Result.Output
    }
    throw $Message
}

function Invoke-CheckedNative {
    param(
        [string]$Description,
        [scriptblock]$Command
    )

    & $Command
    if ($LASTEXITCODE -ne 0) {
        throw "$Description failed with exit code $LASTEXITCODE"
    }
}

function Get-JsonProperty {
    param(
        [object]$Object,
        [string]$Name
    )

    if ($null -eq $Object) {
        return $null
    }

    $property = $Object.PSObject.Properties[$Name]
    if ($null -eq $property) {
        return $null
    }

    return $property.Value
}

function Invoke-FestHubGrpc {
    param(
        [Parameter(Mandatory = $true)]
        [string]$Method,

        [Parameter(Mandatory = $true)]
        [object]$Payload
    )

    $json = $Payload | ConvertTo-Json -Compress -Depth 20
    $mountSpec = "type=bind,source=$ProtoDir,target=/protos,readonly"

    $dockerArgs = @(
        "run", "-i", "--rm",
        "--network", $NetworkName,
        "--ip", $AllowedClientIp,
        "--mount", $mountSpec,
        $GrpcImage,
        "-plaintext",
        "-import-path", "/protos",
        "-proto", "app.proto",
        "-d", "@",
        "backend:50051",
        "FestHub/$Method"
    )

    # Expected failing gRPC calls (INVALID_ARGUMENT / NOT_FOUND) write to stderr.
    # With the script-wide ErrorActionPreference = "Stop", Windows PowerShell 5.1
    # can turn native stderr into a terminating NativeCommandError before we can
    # inspect grpcurl's exit code/output. Temporarily allow native stderr here.
    $previousErrorActionPreference = $ErrorActionPreference
    try {
        $ErrorActionPreference = "Continue"
        $outputLines = $json | & docker @dockerArgs 2>&1
        $exitCode = $LASTEXITCODE
    }
    finally {
        $ErrorActionPreference = $previousErrorActionPreference
    }

    $output = ($outputLines | Out-String).Trim()

    $parsed = $null
    if ($exitCode -eq 0 -and -not [string]::IsNullOrWhiteSpace($output)) {
        try {
            $parsed = $output | ConvertFrom-Json
        }
        catch {
            # Keep the raw output. Assertions will report it if JSON was expected.
        }
    }

    return [PSCustomObject]@{
        Method   = $Method
        ExitCode = $exitCode
        Output   = $output
        Json     = $parsed
    }
}

function Assert-GrpcSuccess {
    param(
        [object]$Result,
        [string]$Context
    )

    if ($Result.ExitCode -ne 0) {
        Fail-Test "$Context expected gRPC success" $Result
    }
}

function Assert-GrpcError {
    param(
        [object]$Result,
        [string]$Code,
        [string]$MessageContains,
        [string]$Context
    )

    if ($Result.ExitCode -eq 0) {
        Fail-Test "$Context expected gRPC error $Code but the RPC succeeded" $Result
    }

    if ($Result.Output -notmatch [regex]::Escape("Code: $Code")) {
        Fail-Test "$Context expected Code: $Code" $Result
    }

    if (-not [string]::IsNullOrWhiteSpace($MessageContains) -and
        $Result.Output -notmatch [regex]::Escape($MessageContains)) {
        Fail-Test "$Context expected error message containing '$MessageContains'" $Result
    }
}

function Assert-Equal {
    param(
        [object]$Expected,
        [object]$Actual,
        [string]$Context
    )

    if ([string]$Expected -ne [string]$Actual) {
        Fail-Test "$Context expected '$Expected' but got '$Actual'"
    }
}

function Assert-True {
    param(
        [bool]$Condition,
        [string]$Context
    )

    if (-not $Condition) {
        Fail-Test "$Context expected true"
    }
}

function Assert-False {
    param(
        [bool]$Condition,
        [string]$Context
    )

    if ($Condition) {
        Fail-Test "$Context expected false"
    }
}

function Assert-ArrayContains {
    param(
        [object]$Values,
        [string]$Expected,
        [string]$Context
    )

    $items = @($Values)
    if (-not ($items -contains $Expected)) {
        Fail-Test "$Context expected array to contain '$Expected'; actual: $($items -join ', ')"
    }
}

function Assert-ArrayNotContains {
    param(
        [object]$Values,
        [string]$Unexpected,
        [string]$Context
    )

    $items = @($Values)
    if ($items -contains $Unexpected) {
        Fail-Test "$Context expected array not to contain '$Unexpected'; actual: $($items -join ', ')"
    }
}

function Get-ProtoBool {
    param(
        [object]$Json,
        [string]$PropertyName
    )

    $value = Get-JsonProperty $Json $PropertyName
    if ($null -eq $value) {
        # Protobuf JSON omits default false values, so {} means false.
        return $false
    }

    return [bool]$value
}

function New-TestArtist {
    param([string]$Label)

    $result = Invoke-FestHubGrpc "CreateArtist" @{
        name           = "$Label $TestSuffix"
        bookingContact = "integration-$TestSuffix@example.com"
    }
    Assert-GrpcSuccess $result "CreateArtist ($Label)"

    $artist = Get-JsonProperty $result.Json "artist"
    $id = Get-JsonProperty $artist "id"
    if ([string]::IsNullOrWhiteSpace([string]$id)) {
        Fail-Test "CreateArtist ($Label) did not return artist.id" $result
    }
    return [string]$id
}

function New-TestEvent {
    param(
        [string]$Name,
        [long]$TicketPriceCents,
        [string]$Description = "FestHub integration test",
        [int]$AvailableTickets = 10
    )

    $result = Invoke-FestHubGrpc "CreateConcertEvent" @{
        name             = $Name
        description      = $Description
        availableTickets = $AvailableTickets
        ticketPriceCents = $TicketPriceCents
    }
    Assert-GrpcSuccess $result "CreateConcertEvent ($Name)"

    $event = Get-JsonProperty $result.Json "concertEvent"
    $id = Get-JsonProperty $event "id"
    if ([string]::IsNullOrWhiteSpace([string]$id)) {
        Fail-Test "CreateConcertEvent ($Name) did not return concertEvent.id" $result
    }
    return [string]$id
}

function New-TestCategory {
    param([string]$Label)

    $result = Invoke-FestHubGrpc "CreateBookingCategory" @{
        name        = "$Label $TestSuffix"
        description = "FestHub integration category $TestSuffix"
    }
    Assert-GrpcSuccess $result "CreateBookingCategory ($Label)"

    $category = Get-JsonProperty $result.Json "bookingCategory"
    $id = Get-JsonProperty $category "id"
    if ([string]::IsNullOrWhiteSpace([string]$id)) {
        Fail-Test "CreateBookingCategory ($Label) did not return bookingCategory.id" $result
    }
    return [string]$id
}

function New-TestPromoMedia {
    param(
        [string]$EventId,
        [string]$Label
    )

    $result = Invoke-FestHubGrpc "CreatePromoMedia" @{
        eventId  = $EventId
        imageUrl = "https://example.com/$Label-$TestSuffix.jpg"
    }
    Assert-GrpcSuccess $result "CreatePromoMedia ($Label)"

    $promo = Get-JsonProperty $result.Json "promoMedia"
    $id = Get-JsonProperty $promo "id"
    if ([string]::IsNullOrWhiteSpace([string]$id)) {
        Fail-Test "CreatePromoMedia ($Label) did not return promoMedia.id" $result
    }
    return [string]$id
}

try {
    Write-Step "Inspecting frontend container state"
    $runningServices = @(& docker compose ps --status running --services 2>$null)
    if ($LASTEXITCODE -ne 0) {
        throw "docker compose ps --status running failed"
    }
    $allServicesWithContainers = @(& docker compose ps -a --services 2>$null)
    if ($LASTEXITCODE -ne 0) {
        throw "docker compose ps -a failed"
    }

    $script:FrontendWasRunning = $runningServices -contains "frontend"
    $script:FrontendContainerExisted = $allServicesWithContainers -contains "frontend"

    Write-Step "Building and starting database/backend"
    Invoke-CheckedNative "docker compose up" {
        & docker compose up -d --build db backend
    }

    Write-Step "Ensuring grpcurl image is available"
    Invoke-CheckedNative "docker pull grpcurl" {
        & docker pull $GrpcImage | Out-Null
    }

    if ($script:FrontendContainerExisted) {
        Write-Step "Temporarily removing frontend container to free $AllowedClientIp"
        Invoke-CheckedNative "docker compose rm frontend" {
            & docker compose rm -sf frontend | Out-Null
        }
    }

    # TEST 1
    Write-Step "TEST 1 - Basic relationship + idempotency + hydration"
    $basicArtist = New-TestArtist "Basic Artist"
    $basicEvent = New-TestEvent "Basic Event $TestSuffix" 5000

    $result = Invoke-FestHubGrpc "AddArtistToEvent" @{ eventId = $basicEvent; artistId = $basicArtist }
    Assert-GrpcSuccess $result "TEST 1 first AddArtistToEvent"
    Assert-True (Get-ProtoBool $result.Json "changed") "TEST 1 first add changed"

    $result = Invoke-FestHubGrpc "AddArtistToEvent" @{ eventId = $basicEvent; artistId = $basicArtist }
    Assert-GrpcSuccess $result "TEST 1 duplicate AddArtistToEvent"
    Assert-False (Get-ProtoBool $result.Json "changed") "TEST 1 duplicate add changed"

    $artistResult = Invoke-FestHubGrpc "GetArtist" @{ id = $basicArtist }
    Assert-GrpcSuccess $artistResult "TEST 1 GetArtist hydration"
    $artistObj = Get-JsonProperty $artistResult.Json "artist"
    Assert-ArrayContains (Get-JsonProperty $artistObj "eventIds") $basicEvent "TEST 1 Artist.event_ids hydration"

    $eventResult = Invoke-FestHubGrpc "GetConcertEvent" @{ id = $basicEvent }
    Assert-GrpcSuccess $eventResult "TEST 1 GetConcertEvent hydration"
    $eventObj = Get-JsonProperty $eventResult.Json "concertEvent"
    Assert-ArrayContains (Get-JsonProperty $eventObj "artistIds") $basicArtist "TEST 1 ConcertEvent.artist_ids hydration"
    Pass-Test "TEST 1 - Basic relationship + idempotency + hydration"

    # TEST 2
    Write-Step "TEST 2 - Artist ticket-price cap"
    $capArtist = New-TestArtist "Cap Artist"
    $capEventA = New-TestEvent "Cap A $TestSuffix" 30000000
    $capEventB = New-TestEvent "Cap B $TestSuffix" 20000000
    $capEventC = New-TestEvent "Cap C $TestSuffix" 100

    $result = Invoke-FestHubGrpc "AddArtistToEvent" @{ eventId = $capEventA; artistId = $capArtist }
    Assert-GrpcSuccess $result 'TEST 2 add $300,000 event'
    Assert-True (Get-ProtoBool $result.Json "changed") "TEST 2 add Event A"

    $result = Invoke-FestHubGrpc "AddArtistToEvent" @{ eventId = $capEventB; artistId = $capArtist }
    Assert-GrpcSuccess $result 'TEST 2 add $200,000 event'
    Assert-True (Get-ProtoBool $result.Json "changed") "TEST 2 add Event B"

    $result = Invoke-FestHubGrpc "AddArtistToEvent" @{ eventId = $capEventC; artistId = $capArtist }
    Assert-GrpcError $result "InvalidArgument" "ticket price cap" "TEST 2 add event above cap"

    $artistResult = Invoke-FestHubGrpc "GetArtist" @{ id = $capArtist }
    Assert-GrpcSuccess $artistResult "TEST 2 GetArtist after rejected add"
    $artistObj = Get-JsonProperty $artistResult.Json "artist"
    Assert-ArrayNotContains (Get-JsonProperty $artistObj "eventIds") $capEventC "TEST 2 rejected Event C must not be hydrated"
    Pass-Test "TEST 2 - Artist ticket-price cap"

    # TEST 3
    Write-Step "TEST 3 - Duplicate add at exact cap"
    $result = Invoke-FestHubGrpc "AddArtistToEvent" @{ eventId = $capEventA; artistId = $capArtist }
    Assert-GrpcSuccess $result "TEST 3 duplicate add at cap"
    Assert-False (Get-ProtoBool $result.Json "changed") "TEST 3 duplicate add changed"
    Pass-Test "TEST 3 - Duplicate add at exact cap"

    # TEST 4
    Write-Step "TEST 4 - Per-Artist event-name collision"
    $collisionArtistA = New-TestArtist "Collision Artist A"
    $collisionName = "Gala Integration $TestSuffix"
    $collisionEvent1 = New-TestEvent $collisionName 5000 "collision event one"
    $collisionEvent2 = New-TestEvent $collisionName 5000 "collision event two"

    $result = Invoke-FestHubGrpc "AddArtistToEvent" @{ eventId = $collisionEvent1; artistId = $collisionArtistA }
    Assert-GrpcSuccess $result "TEST 4 first same-name event"
    Assert-True (Get-ProtoBool $result.Json "changed") "TEST 4 first collision add"

    $result = Invoke-FestHubGrpc "AddArtistToEvent" @{ eventId = $collisionEvent2; artistId = $collisionArtistA }
    Assert-GrpcError $result "InvalidArgument" "event" "TEST 4 same Artist duplicate event name"

    $collisionArtistB = New-TestArtist "Collision Artist B"
    $result = Invoke-FestHubGrpc "AddArtistToEvent" @{ eventId = $collisionEvent2; artistId = $collisionArtistB }
    Assert-GrpcSuccess $result "TEST 4 different Artist same event name"
    Assert-True (Get-ProtoBool $result.Json "changed") "TEST 4 different Artist add"
    Pass-Test "TEST 4 - Per-Artist event-name collision"

    # TEST 5
    Write-Step "TEST 5 - ConcertEvent price update cannot violate Artist cap"
    $result = Invoke-FestHubGrpc "UpdateConcertEvent" @{ id = $capEventA; ticketPriceCents = 30000100 }
    Assert-GrpcError $result "InvalidArgument" "500,000" "TEST 5 price update above cap"

    $eventResult = Invoke-FestHubGrpc "GetConcertEvent" @{ id = $capEventA }
    Assert-GrpcSuccess $eventResult "TEST 5 GetConcertEvent after rejected update"
    $eventObj = Get-JsonProperty $eventResult.Json "concertEvent"
    Assert-Equal 30000000 (Get-JsonProperty $eventObj "ticketPriceCents") "TEST 5 persisted price after rollback"
    Pass-Test "TEST 5 - ConcertEvent price update rollback"

    # TEST 6
    Write-Step "TEST 6 - ConcertEvent rename cannot create lineup collision"
    $renameArtist = New-TestArtist "Rename Artist"
    $alphaName = "Integration Alpha $TestSuffix"
    $betaName = "Integration Beta $TestSuffix"
    $renameEventA = New-TestEvent $alphaName 5000
    $renameEventB = New-TestEvent $betaName 5000

    $result = Invoke-FestHubGrpc "AddArtistToEvent" @{ eventId = $renameEventA; artistId = $renameArtist }
    Assert-GrpcSuccess $result "TEST 6 link alpha"
    $result = Invoke-FestHubGrpc "AddArtistToEvent" @{ eventId = $renameEventB; artistId = $renameArtist }
    Assert-GrpcSuccess $result "TEST 6 link beta"

    $result = Invoke-FestHubGrpc "UpdateConcertEvent" @{ id = $renameEventA; name = $betaName }
    Assert-GrpcError $result "InvalidArgument" "name" "TEST 6 rename collision"

    $eventResult = Invoke-FestHubGrpc "GetConcertEvent" @{ id = $renameEventA }
    Assert-GrpcSuccess $eventResult "TEST 6 GetConcertEvent after rejected rename"
    $eventObj = Get-JsonProperty $eventResult.Json "concertEvent"
    Assert-Equal $alphaName (Get-JsonProperty $eventObj "name") "TEST 6 persisted name after rollback"
    Pass-Test "TEST 6 - ConcertEvent rename rollback"

    # TEST 7
    Write-Step "TEST 7 - BookingCategory relationship hydration + idempotency"
    $categoryEvent = New-TestEvent "Category Event $TestSuffix" 5000
    $categoryId = New-TestCategory "Hydration Category"

    $result = Invoke-FestHubGrpc "AddCategoryToEvent" @{ eventId = $categoryEvent; categoryId = $categoryId }
    Assert-GrpcSuccess $result "TEST 7 AddCategoryToEvent"
    Assert-True (Get-ProtoBool $result.Json "changed") "TEST 7 first category add"

    $eventResult = Invoke-FestHubGrpc "GetConcertEvent" @{ id = $categoryEvent }
    Assert-GrpcSuccess $eventResult "TEST 7 GetConcertEvent category hydration"
    $eventObj = Get-JsonProperty $eventResult.Json "concertEvent"
    Assert-ArrayContains (Get-JsonProperty $eventObj "categoryIds") $categoryId "TEST 7 ConcertEvent.category_ids hydration"

    $categoryResult = Invoke-FestHubGrpc "GetBookingCategory" @{ id = $categoryId }
    Assert-GrpcSuccess $categoryResult "TEST 7 GetBookingCategory hydration"
    $categoryObj = Get-JsonProperty $categoryResult.Json "bookingCategory"
    Assert-ArrayContains (Get-JsonProperty $categoryObj "eventIds") $categoryEvent "TEST 7 BookingCategory.event_ids hydration"

    $result = Invoke-FestHubGrpc "AddCategoryToEvent" @{ eventId = $categoryEvent; categoryId = $categoryId }
    Assert-GrpcSuccess $result "TEST 7 duplicate AddCategoryToEvent"
    Assert-False (Get-ProtoBool $result.Json "changed") "TEST 7 duplicate category add changed"
    Pass-Test "TEST 7 - BookingCategory hydration + idempotency"

    # TEST 8
    Write-Step "TEST 8 - Remove relationships"
    $result = Invoke-FestHubGrpc "RemoveArtistFromEvent" @{ eventId = $basicEvent; artistId = $basicArtist }
    Assert-GrpcSuccess $result "TEST 8 RemoveArtistFromEvent first"
    Assert-True (Get-ProtoBool $result.Json "changed") "TEST 8 first artist removal"

    $result = Invoke-FestHubGrpc "RemoveArtistFromEvent" @{ eventId = $basicEvent; artistId = $basicArtist }
    Assert-GrpcSuccess $result "TEST 8 RemoveArtistFromEvent second"
    Assert-False (Get-ProtoBool $result.Json "changed") "TEST 8 second artist removal"

    $result = Invoke-FestHubGrpc "RemoveCategoryFromEvent" @{ eventId = $categoryEvent; categoryId = $categoryId }
    Assert-GrpcSuccess $result "TEST 8 RemoveCategoryFromEvent first"
    Assert-True (Get-ProtoBool $result.Json "changed") "TEST 8 first category removal"

    $result = Invoke-FestHubGrpc "RemoveCategoryFromEvent" @{ eventId = $categoryEvent; categoryId = $categoryId }
    Assert-GrpcSuccess $result "TEST 8 RemoveCategoryFromEvent second"
    Assert-False (Get-ProtoBool $result.Json "changed") "TEST 8 second category removal"
    Pass-Test "TEST 8 - Remove relationships"

    # TEST 9 + 10
    Write-Step "TEST 9/10 - Event deletion, cascades, orphan cleanup, shared-category survival"
    $deleteEventX = New-TestEvent "Delete X $TestSuffix" 5000
    $deleteEventY = New-TestEvent "Delete Y $TestSuffix" 5000
    $onlyXCategory = New-TestCategory "Only X Category"
    $sharedCategory = New-TestCategory "Shared Category"
    $deleteArtist = New-TestArtist "Delete Survivor Artist"

    $result = Invoke-FestHubGrpc "AddCategoryToEvent" @{ eventId = $deleteEventX; categoryId = $onlyXCategory }
    Assert-GrpcSuccess $result "TEST 9 link OnlyX -> X"
    $result = Invoke-FestHubGrpc "AddCategoryToEvent" @{ eventId = $deleteEventX; categoryId = $sharedCategory }
    Assert-GrpcSuccess $result "TEST 9 link Shared -> X"
    $result = Invoke-FestHubGrpc "AddCategoryToEvent" @{ eventId = $deleteEventY; categoryId = $sharedCategory }
    Assert-GrpcSuccess $result "TEST 9 link Shared -> Y"
    $result = Invoke-FestHubGrpc "AddArtistToEvent" @{ eventId = $deleteEventX; artistId = $deleteArtist }
    Assert-GrpcSuccess $result "TEST 9 link Artist -> X"

    $promoId = New-TestPromoMedia $deleteEventX "delete-cascade"

    $result = Invoke-FestHubGrpc "DeleteConcertEvent" @{ id = $deleteEventX }
    Assert-GrpcSuccess $result "TEST 9 DeleteConcertEvent"
    Assert-True (Get-ProtoBool $result.Json "deleted") "TEST 9 deleted response"

    $result = Invoke-FestHubGrpc "GetConcertEvent" @{ id = $deleteEventX }
    Assert-GrpcError $result "NotFound" "Concert event not found" "TEST 9 deleted event lookup"

    $result = Invoke-FestHubGrpc "GetBookingCategory" @{ id = $onlyXCategory }
    Assert-GrpcError $result "NotFound" "Booking category not found" "TEST 9 orphan category lookup"

    $sharedResult = Invoke-FestHubGrpc "GetBookingCategory" @{ id = $sharedCategory }
    Assert-GrpcSuccess $sharedResult "TEST 9 shared category survives"
    $sharedObj = Get-JsonProperty $sharedResult.Json "bookingCategory"
    Assert-ArrayContains (Get-JsonProperty $sharedObj "eventIds") $deleteEventY "TEST 10 shared category retains Event Y"
    Assert-ArrayNotContains (Get-JsonProperty $sharedObj "eventIds") $deleteEventX "TEST 10 shared category drops Event X"

    $artistResult = Invoke-FestHubGrpc "GetArtist" @{ id = $deleteArtist }
    Assert-GrpcSuccess $artistResult "TEST 9 Artist survives event deletion"
    $artistObj = Get-JsonProperty $artistResult.Json "artist"
    Assert-ArrayNotContains (Get-JsonProperty $artistObj "eventIds") $deleteEventX "TEST 9 Artist relationship cascaded"

    $result = Invoke-FestHubGrpc "GetPromoMedia" @{ id = $promoId }
    Assert-GrpcError $result "NotFound" "Promo media not found" "TEST 9 PromoMedia cascade"
    Pass-Test "TEST 9 - Event deletion + orphan/cascade integrity"
    Pass-Test "TEST 10 - Shared category survives deletion"

    # TEST 11
    Write-Step "TEST 11 - Invalid UUID handling"
    $result = Invoke-FestHubGrpc "GetConcertEvent" @{ id = "not-a-uuid" }
    Assert-GrpcError $result "InvalidArgument" "Invalid concert event ID" "TEST 11 invalid GetConcertEvent UUID"

    $result = Invoke-FestHubGrpc "AddArtistToEvent" @{ eventId = "not-a-uuid"; artistId = $capArtist }
    Assert-GrpcError $result "InvalidArgument" "Invalid event ID" "TEST 11 invalid AddArtistToEvent UUID"

    $result = Invoke-FestHubGrpc "AddCategoryToEvent" @{ eventId = "not-a-uuid"; categoryId = $categoryId }
    Assert-GrpcError $result "InvalidArgument" "Invalid event ID" "TEST 11 invalid AddCategoryToEvent UUID"
    Pass-Test "TEST 11 - Invalid UUID handling"

    # TEST 12
    Write-Step "TEST 12 - Missing entity relationship behavior"
    $missingEventId = [Guid]::NewGuid().ToString()
    $missingArtistId = [Guid]::NewGuid().ToString()
    $missingCategoryId = [Guid]::NewGuid().ToString()

    $result = Invoke-FestHubGrpc "AddArtistToEvent" @{ eventId = $missingEventId; artistId = $capArtist }
    Assert-GrpcError $result "NotFound" "not found" "TEST 12 missing Event in AddArtistToEvent"

    $result = Invoke-FestHubGrpc "AddArtistToEvent" @{ eventId = $capEventC; artistId = $missingArtistId }
    Assert-GrpcError $result "NotFound" "not found" "TEST 12 missing Artist in AddArtistToEvent"

    $result = Invoke-FestHubGrpc "AddCategoryToEvent" @{ eventId = $missingEventId; categoryId = $categoryId }
    Assert-GrpcError $result "NotFound" "not found" "TEST 12 missing Event in AddCategoryToEvent"

    $result = Invoke-FestHubGrpc "AddCategoryToEvent" @{ eventId = $categoryEvent; categoryId = $missingCategoryId }
    Assert-GrpcError $result "NotFound" "not found" "TEST 12 missing Category in AddCategoryToEvent"
    Pass-Test "TEST 12 - Missing entity relationship behavior"

    Write-Host "`n=============================================" -ForegroundColor Green
    Write-Host "ALL BACKEND INTEGRATION TESTS PASSED" -ForegroundColor Green
    Write-Host "Passed test groups: $script:PassedTests" -ForegroundColor Green
    Write-Host "Test suffix: $TestSuffix" -ForegroundColor Green
    Write-Host "=============================================" -ForegroundColor Green
}
catch {
    $script:Failed = $true
    Write-Host "`nBackend integration test suite failed:" -ForegroundColor Red
    Write-Host $_.Exception.Message -ForegroundColor Red
    if ($null -ne $_.InvocationInfo -and $_.InvocationInfo.ScriptLineNumber) {
        Write-Host "Script line: $($_.InvocationInfo.ScriptLineNumber)" -ForegroundColor DarkRed
    }
}
finally {
    Write-Step "Restoring frontend container state"
    try {
        if ($script:FrontendWasRunning) {
            & docker compose up -d frontend | Out-Null
            if ($LASTEXITCODE -ne 0) {
                Write-Host "[WARN] Failed to restart frontend (exit $LASTEXITCODE)." -ForegroundColor Yellow
                $script:Failed = $true
            }
            else {
                Write-Host "Frontend restored to running state." -ForegroundColor Green
            }
        }
        elseif ($script:FrontendContainerExisted) {
            & docker compose create frontend | Out-Null
            if ($LASTEXITCODE -ne 0) {
                Write-Host "[WARN] Failed to recreate previously stopped frontend container (exit $LASTEXITCODE)." -ForegroundColor Yellow
                $script:Failed = $true
            }
            else {
                Write-Host "Frontend restored to stopped/created state." -ForegroundColor Green
            }
        }
        else {
            Write-Host "Frontend did not exist before the test; no restoration needed."
        }
    }
    catch {
        Write-Host "[WARN] Frontend restoration threw: $($_.Exception.Message)" -ForegroundColor Yellow
        $script:Failed = $true
    }
}

if ($script:Failed) {
    Write-Host "`nFinal test script exit code: 1" -ForegroundColor Red
    exit 1
}

Write-Host "`nFinal test script exit code: 0" -ForegroundColor Green
exit 0
