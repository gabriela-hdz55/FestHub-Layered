package com.festhub.backend.grpc;

import com.festhub.backend.model.Artist;
import com.festhub.backend.model.BookingCategory;
import com.festhub.backend.model.ConcertEvent;
import com.festhub.backend.model.PromoMedia;
import com.festhub.backend.service.ArtistService;
import com.festhub.backend.service.BookingCategoryService;
import com.festhub.backend.service.ConcertEventService;
import com.festhub.backend.service.PromoMediaService;
import com.festhub.backend.service.RelationshipService;
import com.festhub.proto.ArtistResponse;
import com.festhub.proto.BookingCategoryResponse;
import com.festhub.proto.ConcertEventResponse;
import com.festhub.proto.CreateArtistRequest;
import com.festhub.proto.CreateBookingCategoryRequest;
import com.festhub.proto.CreateConcertEventRequest;
import com.festhub.proto.CreatePromoMediaRequest;
import com.festhub.proto.DeleteResponse;
import com.festhub.proto.EventArtistRequest;
import com.festhub.proto.EventCategoryRequest;
import com.festhub.proto.FestHubGrpc;
import com.festhub.proto.GetByIdRequest;
import com.festhub.proto.ListArtistsResponse;
import com.festhub.proto.ListBookingCategoriesResponse;
import com.festhub.proto.ListConcertEventsResponse;
import com.festhub.proto.ListPromoMediaResponse;
import com.festhub.proto.ListRequest;
import com.festhub.proto.PromoMediaResponse;
import com.festhub.proto.RelationshipResponse;
import com.festhub.proto.UpdateArtistRequest;
import com.festhub.proto.UpdateBookingCategoryRequest;
import com.festhub.proto.UpdateConcertEventRequest;
import com.festhub.proto.UpdatePromoMediaRequest;

import io.grpc.Status;
import io.grpc.stub.StreamObserver;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;

public final class FestHubServiceImpl
        extends FestHubGrpc.FestHubImplBase {

    private final BookingCategoryService bookingCategoryService;
    private final ArtistService artistService;
    private final PromoMediaService promoMediaService;
    private final ConcertEventService concertEventService;
    private final RelationshipService relationshipService;

    public FestHubServiceImpl(
            BookingCategoryService bookingCategoryService,
            ArtistService artistService,
            PromoMediaService promoMediaService,
            ConcertEventService concertEventService,
            RelationshipService relationshipService) {
        this.bookingCategoryService = bookingCategoryService;
        this.artistService = artistService;
        this.promoMediaService = promoMediaService;
        this.concertEventService = concertEventService;
        this.relationshipService = relationshipService;
    }

    @Override
    public void createBookingCategory(
            CreateBookingCategoryRequest request,
            StreamObserver<BookingCategoryResponse> responseObserver) {
        try {
            BookingCategory category = bookingCategoryService.create(
                    request.getName(),
                    request.getDescription());

            responseObserver.onNext(
                    BookingCategoryResponse.newBuilder()
                            .setBookingCategory(toProto(category))
                            .build());

            responseObserver.onCompleted();
        } catch (IllegalArgumentException exception) {
            responseObserver.onError(
                    Status.INVALID_ARGUMENT
                            .withDescription(exception.getMessage())
                            .asRuntimeException());
        } catch (IllegalStateException exception) {
            responseObserver.onError(
                    Status.INTERNAL
                            .withDescription("Failed to create booking category")
                            .withCause(exception)
                            .asRuntimeException());
        }
    }

    @Override
    public void getBookingCategory(
            GetByIdRequest request,
            StreamObserver<BookingCategoryResponse> responseObserver) {
        UUID id;

        try {
            id = UUID.fromString(request.getId());
        } catch (IllegalArgumentException exception) {
            responseObserver.onError(
                    Status.INVALID_ARGUMENT
                            .withDescription("Invalid booking category ID")
                            .asRuntimeException());
            return;
        }

        try {
            Optional<BookingCategory> category = bookingCategoryService.getById(id);

            if (category.isEmpty()) {
                responseObserver.onError(
                        Status.NOT_FOUND
                                .withDescription(
                                        "Booking category not found")
                                .asRuntimeException());
                return;
            }

            responseObserver.onNext(
                    BookingCategoryResponse.newBuilder()
                            .setBookingCategory(
                                    toProto(category.get()))
                            .build());

            responseObserver.onCompleted();
        } catch (IllegalStateException exception) {
            responseObserver.onError(
                    Status.INTERNAL
                            .withDescription("Failed to get booking category")
                            .withCause(exception)
                            .asRuntimeException());
        }
    }

    @Override
    public void listBookingCategories(
            ListRequest request,
            StreamObserver<ListBookingCategoriesResponse> responseObserver) {
        try {
            ListBookingCategoriesResponse.Builder response = ListBookingCategoriesResponse.newBuilder();

            for (BookingCategory category : bookingCategoryService.listAll()) {
                response.addBookingCategories(toProto(category));
            }

            responseObserver.onNext(response.build());
            responseObserver.onCompleted();
        } catch (IllegalStateException exception) {
            responseObserver.onError(
                    Status.INTERNAL
                            .withDescription(
                                    "Failed to list booking categories")
                            .withCause(exception)
                            .asRuntimeException());
        }
    }

    @Override
    public void updateBookingCategory(
            UpdateBookingCategoryRequest request,
            StreamObserver<BookingCategoryResponse> responseObserver) {
        UUID id;

        try {
            id = UUID.fromString(request.getId());
        } catch (IllegalArgumentException exception) {
            responseObserver.onError(
                    Status.INVALID_ARGUMENT
                            .withDescription("Invalid booking category ID")
                            .asRuntimeException());
            return;
        }

        try {
            Optional<BookingCategory> category = bookingCategoryService.update(
                    id,
                    request.getName(),
                    request.getDescription());

            if (category.isEmpty()) {
                responseObserver.onError(
                        Status.NOT_FOUND
                                .withDescription(
                                        "Booking category not found")
                                .asRuntimeException());
                return;
            }

            responseObserver.onNext(
                    BookingCategoryResponse.newBuilder()
                            .setBookingCategory(
                                    toProto(category.get()))
                            .build());

            responseObserver.onCompleted();
        } catch (IllegalArgumentException exception) {
            responseObserver.onError(
                    Status.INVALID_ARGUMENT
                            .withDescription(exception.getMessage())
                            .asRuntimeException());
        } catch (IllegalStateException exception) {
            responseObserver.onError(
                    Status.INTERNAL
                            .withDescription(
                                    "Failed to update booking category")
                            .withCause(exception)
                            .asRuntimeException());
        }
    }

    @Override
    public void deleteBookingCategory(
            GetByIdRequest request,
            StreamObserver<DeleteResponse> responseObserver) {
        UUID id;

        try {
            id = UUID.fromString(request.getId());
        } catch (IllegalArgumentException exception) {
            responseObserver.onError(
                    Status.INVALID_ARGUMENT
                            .withDescription("Invalid booking category ID")
                            .asRuntimeException());
            return;
        }

        try {
            boolean deleted = bookingCategoryService.delete(id);

            if (!deleted) {
                responseObserver.onError(
                        Status.NOT_FOUND
                                .withDescription(
                                        "Booking category not found")
                                .asRuntimeException());
                return;
            }

            responseObserver.onNext(
                    DeleteResponse.newBuilder()
                            .setDeleted(true)
                            .build());

            responseObserver.onCompleted();
        } catch (IllegalStateException exception) {
            responseObserver.onError(
                    Status.INTERNAL
                            .withDescription(
                                    "Failed to delete booking category")
                            .withCause(exception)
                            .asRuntimeException());
        }
    }

    @Override
    public void createArtist(
            CreateArtistRequest request,
            StreamObserver<ArtistResponse> responseObserver) {
        try {
            Artist artist = artistService.create(
                    request.getName(),
                    request.getBookingContact());

            responseObserver.onNext(
                    ArtistResponse.newBuilder()
                            .setArtist(toProto(artist))
                            .build());

            responseObserver.onCompleted();
        } catch (IllegalArgumentException exception) {
            responseObserver.onError(
                    Status.INVALID_ARGUMENT
                            .withDescription(exception.getMessage())
                            .asRuntimeException());
        } catch (IllegalStateException exception) {
            responseObserver.onError(
                    Status.INTERNAL
                            .withDescription("Failed to create artist")
                            .withCause(exception)
                            .asRuntimeException());
        }
    }

    @Override
    public void getArtist(
            GetByIdRequest request,
            StreamObserver<ArtistResponse> responseObserver) {
        UUID id;

        try {
            id = UUID.fromString(request.getId());
        } catch (IllegalArgumentException exception) {
            responseObserver.onError(
                    Status.INVALID_ARGUMENT
                            .withDescription("Invalid artist ID")
                            .asRuntimeException());
            return;
        }

        try {
            Optional<Artist> artist = artistService.getById(id);

            if (artist.isEmpty()) {
                responseObserver.onError(
                        Status.NOT_FOUND
                                .withDescription(
                                        "Artist not found")
                                .asRuntimeException());
                return;
            }

            responseObserver.onNext(
                    ArtistResponse.newBuilder()
                            .setArtist(
                                    toProto(artist.get()))
                            .build());

            responseObserver.onCompleted();
        } catch (IllegalStateException exception) {
            responseObserver.onError(
                    Status.INTERNAL
                            .withDescription("Failed to get artist")
                            .withCause(exception)
                            .asRuntimeException());
        }
    }

    @Override
    public void listArtists(
            ListRequest request,
            StreamObserver<ListArtistsResponse> responseObserver) {
        try {
            ListArtistsResponse.Builder response = ListArtistsResponse.newBuilder();

            for (Artist artist : artistService.listAll()) {
                response.addArtists(toProto(artist));
            }

            responseObserver.onNext(response.build());
            responseObserver.onCompleted();
        } catch (IllegalStateException exception) {
            responseObserver.onError(
                    Status.INTERNAL
                            .withDescription(
                                    "Failed to list artists")
                            .withCause(exception)
                            .asRuntimeException());
        }
    }

    @Override
    public void updateArtist(
            UpdateArtistRequest request,
            StreamObserver<ArtistResponse> responseObserver) {
        UUID id;

        try {
            id = UUID.fromString(request.getId());
        } catch (IllegalArgumentException exception) {
            responseObserver.onError(
                    Status.INVALID_ARGUMENT
                            .withDescription("Invalid artist ID")
                            .asRuntimeException());
            return;
        }

        try {
            Optional<Artist> artist = artistService.update(
                    id,
                    request.getName(),
                    request.getBookingContact());

            if (artist.isEmpty()) {
                responseObserver.onError(
                        Status.NOT_FOUND
                                .withDescription(
                                        "Artist not found")
                                .asRuntimeException());
                return;
            }

            responseObserver.onNext(
                    ArtistResponse.newBuilder()
                            .setArtist(
                                    toProto(artist.get()))
                            .build());

            responseObserver.onCompleted();
        } catch (IllegalArgumentException exception) {
            responseObserver.onError(
                    Status.INVALID_ARGUMENT
                            .withDescription(exception.getMessage())
                            .asRuntimeException());
        } catch (IllegalStateException exception) {
            responseObserver.onError(
                    Status.INTERNAL
                            .withDescription(
                                    "Failed to update artist")
                            .withCause(exception)
                            .asRuntimeException());
        }
    }

    @Override
    public void deleteArtist(
            GetByIdRequest request,
            StreamObserver<DeleteResponse> responseObserver) {
        UUID id;

        try {
            id = UUID.fromString(request.getId());
        } catch (IllegalArgumentException exception) {
            responseObserver.onError(
                    Status.INVALID_ARGUMENT
                            .withDescription("Invalid artist ID")
                            .asRuntimeException());
            return;
        }

        try {
            boolean deleted = artistService.delete(id);

            if (!deleted) {
                responseObserver.onError(
                        Status.NOT_FOUND
                                .withDescription(
                                        "Artist not found")
                                .asRuntimeException());
                return;
            }

            responseObserver.onNext(
                    DeleteResponse.newBuilder()
                            .setDeleted(true)
                            .build());

            responseObserver.onCompleted();
        } catch (IllegalStateException exception) {
            responseObserver.onError(
                    Status.INTERNAL
                            .withDescription(
                                    "Failed to delete artist")
                            .withCause(exception)
                            .asRuntimeException());
        }
    }

    private com.festhub.proto.BookingCategory toProto(
            BookingCategory category) {
        List<UUID> eventIds = relationshipService.findEventIdsByCategoryId(category.id());

        com.festhub.proto.BookingCategory.Builder builder = com.festhub.proto.BookingCategory.newBuilder()
                .setId(category.id().toString())
                .setName(category.name())
                .setDescription(category.description());

        for (UUID eventId : eventIds) {
            builder.addEventIds(eventId.toString());
        }

        return builder.build();
    }

    private com.festhub.proto.Artist toProto(
            Artist artist) {
        List<UUID> eventIds = relationshipService.findEventIdsByArtistId(artist.id());

        com.festhub.proto.Artist.Builder builder = com.festhub.proto.Artist.newBuilder()
                .setId(artist.id().toString())
                .setName(artist.name())
                .setBookingContact(artist.bookingContact());

        for (UUID eventId : eventIds) {
            builder.addEventIds(eventId.toString());
        }

        return builder.build();
    }

    @Override
    public void createPromoMedia(
            CreatePromoMediaRequest request,
            StreamObserver<PromoMediaResponse> responseObserver) {
        UUID eventId;

        try {
            eventId = UUID.fromString(request.getEventId());
        } catch (IllegalArgumentException exception) {
            responseObserver.onError(
                    Status.INVALID_ARGUMENT
                            .withDescription("Invalid event ID")
                            .asRuntimeException());
            return;
        }

        try {
            PromoMedia promoMedia = promoMediaService.create(
                    eventId,
                    request.getImageUrl());

            responseObserver.onNext(
                    PromoMediaResponse.newBuilder()
                            .setPromoMedia(toProto(promoMedia))
                            .build());

            responseObserver.onCompleted();
        } catch (IllegalArgumentException exception) {
            responseObserver.onError(
                    Status.INVALID_ARGUMENT
                            .withDescription(exception.getMessage())
                            .asRuntimeException());
        } catch (IllegalStateException exception) {
            responseObserver.onError(
                    Status.INTERNAL
                            .withDescription("Failed to create promo media")
                            .withCause(exception)
                            .asRuntimeException());
        }
    }

    @Override
    public void getPromoMedia(
            GetByIdRequest request,
            StreamObserver<PromoMediaResponse> responseObserver) {
        UUID id;

        try {
            id = UUID.fromString(request.getId());
        } catch (IllegalArgumentException exception) {
            responseObserver.onError(
                    Status.INVALID_ARGUMENT
                            .withDescription("Invalid promo media ID")
                            .asRuntimeException());
            return;
        }

        try {
            Optional<PromoMedia> promoMedia = promoMediaService.getById(id);

            if (promoMedia.isEmpty()) {
                responseObserver.onError(
                        Status.NOT_FOUND
                                .withDescription(
                                        "Promo media not found")
                                .asRuntimeException());
                return;
            }

            responseObserver.onNext(
                    PromoMediaResponse.newBuilder()
                            .setPromoMedia(
                                    toProto(promoMedia.get()))
                            .build());

            responseObserver.onCompleted();
        } catch (IllegalStateException exception) {
            responseObserver.onError(
                    Status.INTERNAL
                            .withDescription("Failed to get promo media")
                            .withCause(exception)
                            .asRuntimeException());
        }
    }

    @Override
    public void listPromoMedia(
            ListRequest request,
            StreamObserver<ListPromoMediaResponse> responseObserver) {
        try {
            ListPromoMediaResponse.Builder response = ListPromoMediaResponse.newBuilder();

            for (PromoMedia promoMedia : promoMediaService.listAll()) {
                response.addPromoMedia(toProto(promoMedia));
            }

            responseObserver.onNext(response.build());
            responseObserver.onCompleted();
        } catch (IllegalStateException exception) {
            responseObserver.onError(
                    Status.INTERNAL
                            .withDescription(
                                    "Failed to list promo media")
                            .withCause(exception)
                            .asRuntimeException());
        }
    }

    @Override
    public void updatePromoMedia(
            UpdatePromoMediaRequest request,
            StreamObserver<PromoMediaResponse> responseObserver) {
        UUID id;

        try {
            id = UUID.fromString(request.getId());
        } catch (IllegalArgumentException exception) {
            responseObserver.onError(
                    Status.INVALID_ARGUMENT
                            .withDescription("Invalid promo media ID")
                            .asRuntimeException());
            return;
        }

        UUID eventId;

        try {
            eventId = UUID.fromString(request.getEventId());
        } catch (IllegalArgumentException exception) {
            responseObserver.onError(
                    Status.INVALID_ARGUMENT
                            .withDescription("Invalid event ID")
                            .asRuntimeException());
            return;
        }

        try {
            Optional<PromoMedia> promoMedia = promoMediaService.update(
                    id,
                    eventId,
                    request.getImageUrl());

            if (promoMedia.isEmpty()) {
                responseObserver.onError(
                        Status.NOT_FOUND
                                .withDescription(
                                        "Promo media not found")
                                .asRuntimeException());
                return;
            }

            responseObserver.onNext(
                    PromoMediaResponse.newBuilder()
                            .setPromoMedia(
                                    toProto(promoMedia.get()))
                            .build());

            responseObserver.onCompleted();
        } catch (IllegalArgumentException exception) {
            responseObserver.onError(
                    Status.INVALID_ARGUMENT
                            .withDescription(exception.getMessage())
                            .asRuntimeException());
        } catch (IllegalStateException exception) {
            responseObserver.onError(
                    Status.INTERNAL
                            .withDescription(
                                    "Failed to update promo media")
                            .withCause(exception)
                            .asRuntimeException());
        }
    }

    @Override
    public void deletePromoMedia(
            GetByIdRequest request,
            StreamObserver<DeleteResponse> responseObserver) {
        UUID id;

        try {
            id = UUID.fromString(request.getId());
        } catch (IllegalArgumentException exception) {
            responseObserver.onError(
                    Status.INVALID_ARGUMENT
                            .withDescription("Invalid promo media ID")
                            .asRuntimeException());
            return;
        }

        try {
            boolean deleted = promoMediaService.delete(id);

            if (!deleted) {
                responseObserver.onError(
                        Status.NOT_FOUND
                                .withDescription(
                                        "Promo media not found")
                                .asRuntimeException());
                return;
            }

            responseObserver.onNext(
                    DeleteResponse.newBuilder()
                            .setDeleted(true)
                            .build());

            responseObserver.onCompleted();
        } catch (IllegalStateException exception) {
            responseObserver.onError(
                    Status.INTERNAL
                            .withDescription(
                                    "Failed to delete promo media")
                            .withCause(exception)
                            .asRuntimeException());
        }
    }

    private static com.festhub.proto.PromoMedia toProto(
            PromoMedia promoMedia) {
        return com.festhub.proto.PromoMedia.newBuilder()
                .setId(promoMedia.id().toString())
                .setEventId(promoMedia.eventId().toString())
                .setImageUrl(promoMedia.imageUrl())
                .build();
    }

    @Override
    public void createConcertEvent(
            CreateConcertEventRequest request,
            StreamObserver<ConcertEventResponse> responseObserver) {
        UUID id = null;

        if (request.hasId()) {
            try {
                id = UUID.fromString(request.getId());
            } catch (IllegalArgumentException exception) {
                responseObserver.onError(
                        Status.INVALID_ARGUMENT
                                .withDescription("Invalid concert event ID")
                                .asRuntimeException());
                return;
            }
        }

        try {
            ConcertEvent event = concertEventService.create(
                    id,
                    request.getName(),
                    request.getDescription(),
                    request.getAvailableTickets(),
                    request.getTicketPriceCents());

            responseObserver.onNext(
                    ConcertEventResponse.newBuilder()
                            .setConcertEvent(toProto(event))
                            .build());

            responseObserver.onCompleted();
        } catch (IllegalArgumentException exception) {
            responseObserver.onError(
                    Status.INVALID_ARGUMENT
                            .withDescription(exception.getMessage())
                            .asRuntimeException());
        } catch (IllegalStateException exception) {
            responseObserver.onError(
                    Status.INTERNAL
                            .withDescription("Failed to create concert event")
                            .withCause(exception)
                            .asRuntimeException());
        }
    }

    @Override
    public void getConcertEvent(
            GetByIdRequest request,
            StreamObserver<ConcertEventResponse> responseObserver) {
        UUID id;

        try {
            id = UUID.fromString(request.getId());
        } catch (IllegalArgumentException exception) {
            responseObserver.onError(
                    Status.INVALID_ARGUMENT
                            .withDescription("Invalid concert event ID")
                            .asRuntimeException());
            return;
        }

        try {
            Optional<ConcertEvent> event = concertEventService.getById(id);

            if (event.isEmpty()) {
                responseObserver.onError(
                        Status.NOT_FOUND
                                .withDescription(
                                        "Concert event not found")
                                .asRuntimeException());
                return;
            }

            responseObserver.onNext(
                    ConcertEventResponse.newBuilder()
                            .setConcertEvent(
                                    toProto(event.get()))
                            .build());

            responseObserver.onCompleted();
        } catch (IllegalStateException exception) {
            responseObserver.onError(
                    Status.INTERNAL
                            .withDescription("Failed to get concert event")
                            .withCause(exception)
                            .asRuntimeException());
        }
    }

    @Override
    public void listConcertEvents(
            ListRequest request,
            StreamObserver<ListConcertEventsResponse> responseObserver) {
        try {
            ListConcertEventsResponse.Builder response = ListConcertEventsResponse.newBuilder();

            for (ConcertEvent event : concertEventService.listAll()) {
                response.addConcertEvents(toProto(event));
            }

            responseObserver.onNext(response.build());
            responseObserver.onCompleted();
        } catch (IllegalStateException exception) {
            responseObserver.onError(
                    Status.INTERNAL
                            .withDescription(
                                    "Failed to list concert events")
                            .withCause(exception)
                            .asRuntimeException());
        }
    }

    @Override
    public void updateConcertEvent(
            UpdateConcertEventRequest request,
            StreamObserver<ConcertEventResponse> responseObserver) {
        UUID id;

        try {
            id = UUID.fromString(request.getId());
        } catch (IllegalArgumentException exception) {
            responseObserver.onError(
                    Status.INVALID_ARGUMENT
                            .withDescription("Invalid concert event ID")
                            .asRuntimeException());
            return;
        }

        Optional<String> name = request.hasName()
                ? Optional.of(request.getName())
                : Optional.empty();
        Optional<String> description = request.hasDescription()
                ? Optional.of(request.getDescription())
                : Optional.empty();
        Optional<Integer> availableTickets = request.hasAvailableTickets()
                ? Optional.of(request.getAvailableTickets())
                : Optional.empty();
        Optional<Long> ticketPriceCents = request.hasTicketPriceCents()
                ? Optional.of(request.getTicketPriceCents())
                : Optional.empty();

        try {
            Optional<ConcertEvent> event = concertEventService.update(
                    id,
                    name,
                    description,
                    availableTickets,
                    ticketPriceCents);

            if (event.isEmpty()) {
                responseObserver.onError(
                        Status.NOT_FOUND
                                .withDescription(
                                        "Concert event not found")
                                .asRuntimeException());
                return;
            }

            responseObserver.onNext(
                    ConcertEventResponse.newBuilder()
                            .setConcertEvent(
                                    toProto(event.get()))
                            .build());

            responseObserver.onCompleted();
        } catch (IllegalArgumentException exception) {
            responseObserver.onError(
                    Status.INVALID_ARGUMENT
                            .withDescription(exception.getMessage())
                            .asRuntimeException());
        } catch (IllegalStateException exception) {
            responseObserver.onError(
                    Status.INTERNAL
                            .withDescription(
                                    "Failed to update concert event")
                            .withCause(exception)
                            .asRuntimeException());
        }
    }

    @Override
    public void deleteConcertEvent(
            GetByIdRequest request,
            StreamObserver<DeleteResponse> responseObserver) {
        UUID id;

        try {
            id = UUID.fromString(request.getId());
        } catch (IllegalArgumentException exception) {
            responseObserver.onError(
                    Status.INVALID_ARGUMENT
                            .withDescription("Invalid concert event ID")
                            .asRuntimeException());
            return;
        }

        try {
            boolean deleted = concertEventService.delete(id);

            if (!deleted) {
                responseObserver.onError(
                        Status.NOT_FOUND
                                .withDescription(
                                        "Concert event not found")
                                .asRuntimeException());
                return;
            }

            responseObserver.onNext(
                    DeleteResponse.newBuilder()
                            .setDeleted(true)
                            .build());

            responseObserver.onCompleted();
        } catch (IllegalStateException exception) {
            responseObserver.onError(
                    Status.INTERNAL
                            .withDescription(
                                    "Failed to delete concert event")
                            .withCause(exception)
                            .asRuntimeException());
        }
    }

    private com.festhub.proto.ConcertEvent toProto(
            ConcertEvent event) {
        List<UUID> artistIds = relationshipService.findArtistIdsByEventId(event.id());
        List<UUID> categoryIds = relationshipService.findCategoryIdsByEventId(event.id());
        List<UUID> promoMediaIds = relationshipService.findPromoMediaIdsByEventId(event.id());

        com.festhub.proto.ConcertEvent.Builder builder = com.festhub.proto.ConcertEvent.newBuilder()
                .setId(event.id().toString())
                .setName(event.name())
                .setDescription(event.description())
                .setAvailableTickets(event.availableTickets())
                .setTicketPriceCents(event.ticketPriceCents());

        for (UUID artistId : artistIds) {
            builder.addArtistIds(artistId.toString());
        }
        for (UUID categoryId : categoryIds) {
            builder.addCategoryIds(categoryId.toString());
        }
        for (UUID promoMediaId : promoMediaIds) {
            builder.addPromoMediaIds(promoMediaId.toString());
        }

        return builder.build();
    }

    @Override
    public void addArtistToEvent(
            EventArtistRequest request,
            StreamObserver<RelationshipResponse> responseObserver) {
        UUID eventId;
        UUID artistId;

        try {
            eventId = UUID.fromString(request.getEventId());
        } catch (IllegalArgumentException exception) {
            responseObserver.onError(
                    Status.INVALID_ARGUMENT
                            .withDescription("Invalid event ID")
                            .asRuntimeException());
            return;
        }

        try {
            artistId = UUID.fromString(request.getArtistId());
        } catch (IllegalArgumentException exception) {
            responseObserver.onError(
                    Status.INVALID_ARGUMENT
                            .withDescription("Invalid artist ID")
                            .asRuntimeException());
            return;
        }

        try {
            boolean changed = relationshipService.addArtistToEvent(eventId, artistId);

            responseObserver.onNext(
                    RelationshipResponse.newBuilder()
                            .setChanged(changed)
                            .build());

            responseObserver.onCompleted();
        } catch (NoSuchElementException exception) {
            responseObserver.onError(
                    Status.NOT_FOUND
                            .withDescription(exception.getMessage())
                            .asRuntimeException());
        } catch (IllegalStateException exception) {
            responseObserver.onError(
                    Status.INTERNAL
                            .withDescription("Failed to add artist to event")
                            .withCause(exception)
                            .asRuntimeException());
        }
    }

    @Override
    public void removeArtistFromEvent(
            EventArtistRequest request,
            StreamObserver<RelationshipResponse> responseObserver) {
        UUID eventId;
        UUID artistId;

        try {
            eventId = UUID.fromString(request.getEventId());
        } catch (IllegalArgumentException exception) {
            responseObserver.onError(
                    Status.INVALID_ARGUMENT
                            .withDescription("Invalid event ID")
                            .asRuntimeException());
            return;
        }

        try {
            artistId = UUID.fromString(request.getArtistId());
        } catch (IllegalArgumentException exception) {
            responseObserver.onError(
                    Status.INVALID_ARGUMENT
                            .withDescription("Invalid artist ID")
                            .asRuntimeException());
            return;
        }

        try {
            boolean changed = relationshipService.removeArtistFromEvent(eventId, artistId);

            responseObserver.onNext(
                    RelationshipResponse.newBuilder()
                            .setChanged(changed)
                            .build());

            responseObserver.onCompleted();
        } catch (NoSuchElementException exception) {
            responseObserver.onError(
                    Status.NOT_FOUND
                            .withDescription(exception.getMessage())
                            .asRuntimeException());
        } catch (IllegalStateException exception) {
            responseObserver.onError(
                    Status.INTERNAL
                            .withDescription("Failed to remove artist from event")
                            .withCause(exception)
                            .asRuntimeException());
        }
    }

    @Override
    public void addCategoryToEvent(
            EventCategoryRequest request,
            StreamObserver<RelationshipResponse> responseObserver) {
        UUID eventId;
        UUID categoryId;

        try {
            eventId = UUID.fromString(request.getEventId());
        } catch (IllegalArgumentException exception) {
            responseObserver.onError(
                    Status.INVALID_ARGUMENT
                            .withDescription("Invalid event ID")
                            .asRuntimeException());
            return;
        }

        try {
            categoryId = UUID.fromString(request.getCategoryId());
        } catch (IllegalArgumentException exception) {
            responseObserver.onError(
                    Status.INVALID_ARGUMENT
                            .withDescription("Invalid category ID")
                            .asRuntimeException());
            return;
        }

        try {
            boolean changed = relationshipService.addCategoryToEvent(eventId, categoryId);

            responseObserver.onNext(
                    RelationshipResponse.newBuilder()
                            .setChanged(changed)
                            .build());

            responseObserver.onCompleted();
        } catch (NoSuchElementException exception) {
            responseObserver.onError(
                    Status.NOT_FOUND
                            .withDescription(exception.getMessage())
                            .asRuntimeException());
        } catch (IllegalStateException exception) {
            responseObserver.onError(
                    Status.INTERNAL
                            .withDescription("Failed to add category to event")
                            .withCause(exception)
                            .asRuntimeException());
        }
    }

    @Override
    public void removeCategoryFromEvent(
            EventCategoryRequest request,
            StreamObserver<RelationshipResponse> responseObserver) {
        UUID eventId;
        UUID categoryId;

        try {
            eventId = UUID.fromString(request.getEventId());
        } catch (IllegalArgumentException exception) {
            responseObserver.onError(
                    Status.INVALID_ARGUMENT
                            .withDescription("Invalid event ID")
                            .asRuntimeException());
            return;
        }

        try {
            categoryId = UUID.fromString(request.getCategoryId());
        } catch (IllegalArgumentException exception) {
            responseObserver.onError(
                    Status.INVALID_ARGUMENT
                            .withDescription("Invalid category ID")
                            .asRuntimeException());
            return;
        }

        try {
            boolean changed = relationshipService.removeCategoryFromEvent(eventId, categoryId);

            responseObserver.onNext(
                    RelationshipResponse.newBuilder()
                            .setChanged(changed)
                            .build());

            responseObserver.onCompleted();
        } catch (NoSuchElementException exception) {
            responseObserver.onError(
                    Status.NOT_FOUND
                            .withDescription(exception.getMessage())
                            .asRuntimeException());
        } catch (IllegalStateException exception) {
            responseObserver.onError(
                    Status.INTERNAL
                            .withDescription("Failed to remove category from event")
                            .withCause(exception)
                            .asRuntimeException());
        }
    }
}