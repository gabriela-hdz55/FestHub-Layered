package com.festhub.frontend;

import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import com.festhub.proto.Artist;
import com.festhub.proto.ArtistResponse;
import com.festhub.proto.BookingCategory;
import com.festhub.proto.BookingCategoryResponse;
import com.festhub.proto.ConcertEvent;
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
import com.festhub.proto.PromoMedia;
import com.festhub.proto.PromoMediaResponse;
import com.festhub.proto.RelationshipResponse;
import com.festhub.proto.UpdateArtistRequest;
import com.festhub.proto.UpdateBookingCategoryRequest;
import com.festhub.proto.UpdateConcertEventRequest;
import com.festhub.proto.UpdatePromoMediaRequest;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;

public class Main {

    public static void main(String[] args) throws Exception {
        String backendHost = System.getenv().getOrDefault("BACKEND_HOST", "backend");

        ManagedChannel channel = ManagedChannelBuilder
                .forAddress(backendHost, 50051)
                .usePlaintext()
                .build();

        FestHubGrpc.FestHubBlockingStub stub = FestHubGrpc.newBlockingStub(channel);

        Runtime.getRuntime().addShutdownHook(new Thread(channel::shutdown));

        HttpServer http = HttpServer.create(new InetSocketAddress(8080), 0);

        http.createContext("/", ex -> {
            if (!"GET".equals(ex.getRequestMethod())) {
                respond(ex, 405, "Method Not Allowed", "text/plain");
                return;
            }

            try {
                byte[] fileBytes = java.nio.file.Files.readAllBytes(
                        java.nio.file.Paths.get("/public/index.html"));
                respond(ex, 200, new String(fileBytes, StandardCharsets.UTF_8), "text/html");
            } catch (Exception exception) {
                exception.printStackTrace();
                respond(ex, 404, "404: index.html not found.", "text/plain");
            }
        });

        // ARTIST CRUD

        http.createContext("/api/artist/create", ex -> {
            if (!requireMethod(ex, "POST")) {
                return;
            }

            try {
                Map<String, String> form = readForm(ex);

                ArtistResponse response = stub.createArtist(
                        CreateArtistRequest.newBuilder()
                                .setName(required(form, "name"))
                                .setBookingContact(required(form, "contact"))
                                .build());

                respond(ex, 200, formatArtist(response.getArtist()), "text/plain");
            } catch (StatusRuntimeException exception) {
                respondGrpcError(ex, exception);
            } catch (IllegalArgumentException exception) {
                respond(ex, 400, exception.getMessage(), "text/plain");
            } catch (Exception exception) {
                exception.printStackTrace();
                respond(ex, 500, "Frontend failed to create artist.", "text/plain");
            }
        });

        http.createContext("/api/artist/read", ex -> {
            if (!requireMethod(ex, "GET")) {
                return;
            }

            try {
                ArtistResponse response = stub.getArtist(
                        GetByIdRequest.newBuilder()
                                .setId(requiredQuery(ex, "id"))
                                .build());

                respond(ex, 200, formatArtist(response.getArtist()), "text/plain");
            } catch (StatusRuntimeException exception) {
                respondGrpcError(ex, exception);
            } catch (IllegalArgumentException exception) {
                respond(ex, 400, exception.getMessage(), "text/plain");
            } catch (Exception exception) {
                exception.printStackTrace();
                respond(ex, 500, "Frontend failed to read artist.", "text/plain");
            }
        });

        http.createContext("/api/artist/update", ex -> {
            if (!requireMethod(ex, "POST")) {
                return;
            }

            try {
                Map<String, String> form = readForm(ex);

                ArtistResponse response = stub.updateArtist(
                        UpdateArtistRequest.newBuilder()
                                .setId(required(form, "id"))
                                .setName(required(form, "name"))
                                .setBookingContact(required(form, "contact"))
                                .build());

                respond(ex, 200, formatArtist(response.getArtist()), "text/plain");
            } catch (StatusRuntimeException exception) {
                respondGrpcError(ex, exception);
            } catch (IllegalArgumentException exception) {
                respond(ex, 400, exception.getMessage(), "text/plain");
            } catch (Exception exception) {
                exception.printStackTrace();
                respond(ex, 500, "Frontend failed to update artist.", "text/plain");
            }
        });

        http.createContext("/api/artist/delete", ex -> {
            if (!requireMethod(ex, "DELETE")) {
                return;
            }

            try {
                DeleteResponse response = stub.deleteArtist(
                        GetByIdRequest.newBuilder()
                                .setId(requiredQuery(ex, "id"))
                                .build());

                respond(ex, 200, "Deleted: " + response.getDeleted(), "text/plain");
            } catch (StatusRuntimeException exception) {
                respondGrpcError(ex, exception);
            } catch (IllegalArgumentException exception) {
                respond(ex, 400, exception.getMessage(), "text/plain");
            } catch (Exception exception) {
                exception.printStackTrace();
                respond(ex, 500, "Frontend failed to delete artist.", "text/plain");
            }
        });

        // CONCERT EVENT CRUD

        http.createContext("/api/event/create", ex -> {
            if (!requireMethod(ex, "POST")) {
                return;
            }

            try {
                Map<String, String> form = readForm(ex);

                ConcertEventResponse response = stub.createConcertEvent(
                        CreateConcertEventRequest.newBuilder()
                                .setName(required(form, "name"))
                                .setDescription(required(form, "desc"))
                                .setAvailableTickets(parseInt(required(form, "tickets"), "tickets"))
                                .setTicketPriceCents(dollarsToCents(required(form, "price")))
                                .build());

                respond(ex, 200, formatConcertEvent(response.getConcertEvent()), "text/plain");
            } catch (StatusRuntimeException exception) {
                respondGrpcError(ex, exception);
            } catch (IllegalArgumentException exception) {
                respond(ex, 400, exception.getMessage(), "text/plain");
            } catch (Exception exception) {
                exception.printStackTrace();
                respond(ex, 500, "Frontend failed to create concert event.", "text/plain");
            }
        });

        http.createContext("/api/event/read", ex -> {
            if (!requireMethod(ex, "GET")) {
                return;
            }

            try {
                ConcertEventResponse response = stub.getConcertEvent(
                        GetByIdRequest.newBuilder()
                                .setId(requiredQuery(ex, "id"))
                                .build());

                respond(ex, 200, formatConcertEvent(response.getConcertEvent()), "text/plain");
            } catch (StatusRuntimeException exception) {
                respondGrpcError(ex, exception);
            } catch (IllegalArgumentException exception) {
                respond(ex, 400, exception.getMessage(), "text/plain");
            } catch (Exception exception) {
                exception.printStackTrace();
                respond(ex, 500, "Frontend failed to read concert event.", "text/plain");
            }
        });

        http.createContext("/api/event/update", ex -> {
            if (!requireMethod(ex, "POST")) {
                return;
            }

            try {
                Map<String, String> form = readForm(ex);

                UpdateConcertEventRequest.Builder request = UpdateConcertEventRequest.newBuilder()
                        .setId(required(form, "id"));

                putIfPresent(form, "name", request::setName);
                putIfPresent(form, "desc", request::setDescription);

                if (hasText(form.get("tickets"))) {
                    request.setAvailableTickets(parseInt(form.get("tickets"), "tickets"));
                }

                if (hasText(form.get("price"))) {
                    request.setTicketPriceCents(dollarsToCents(form.get("price")));
                }

                ConcertEventResponse response = stub.updateConcertEvent(request.build());
                respond(ex, 200, formatConcertEvent(response.getConcertEvent()), "text/plain");
            } catch (StatusRuntimeException exception) {
                respondGrpcError(ex, exception);
            } catch (IllegalArgumentException exception) {
                respond(ex, 400, exception.getMessage(), "text/plain");
            } catch (Exception exception) {
                exception.printStackTrace();
                respond(ex, 500, "Frontend failed to update concert event.", "text/plain");
            }
        });

        http.createContext("/api/event/delete", ex -> {
            if (!requireMethod(ex, "DELETE")) {
                return;
            }

            try {
                DeleteResponse response = stub.deleteConcertEvent(
                        GetByIdRequest.newBuilder()
                                .setId(requiredQuery(ex, "id"))
                                .build());

                respond(ex, 200, "Deleted: " + response.getDeleted(), "text/plain");
            } catch (StatusRuntimeException exception) {
                respondGrpcError(ex, exception);
            } catch (IllegalArgumentException exception) {
                respond(ex, 400, exception.getMessage(), "text/plain");
            } catch (Exception exception) {
                exception.printStackTrace();
                respond(ex, 500, "Frontend failed to delete concert event.", "text/plain");
            }
        });

        // BOOKING CATEGORY CRUD

        http.createContext("/api/category/create", ex -> {
            if (!requireMethod(ex, "POST")) {
                return;
            }

            try {
                Map<String, String> form = readForm(ex);

                BookingCategoryResponse response = stub.createBookingCategory(
                        CreateBookingCategoryRequest.newBuilder()
                                .setName(required(form, "name"))
                                .setDescription(required(form, "desc"))
                                .build());

                respond(ex, 200, formatBookingCategory(response.getBookingCategory()), "text/plain");
            } catch (StatusRuntimeException exception) {
                respondGrpcError(ex, exception);
            } catch (IllegalArgumentException exception) {
                respond(ex, 400, exception.getMessage(), "text/plain");
            } catch (Exception exception) {
                exception.printStackTrace();
                respond(ex, 500, "Frontend failed to create booking category.", "text/plain");
            }
        });

        http.createContext("/api/category/read", ex -> {
            if (!requireMethod(ex, "GET")) {
                return;
            }

            try {
                BookingCategoryResponse response = stub.getBookingCategory(
                        GetByIdRequest.newBuilder()
                                .setId(requiredQuery(ex, "id"))
                                .build());

                respond(ex, 200, formatBookingCategory(response.getBookingCategory()), "text/plain");
            } catch (StatusRuntimeException exception) {
                respondGrpcError(ex, exception);
            } catch (IllegalArgumentException exception) {
                respond(ex, 400, exception.getMessage(), "text/plain");
            } catch (Exception exception) {
                exception.printStackTrace();
                respond(ex, 500, "Frontend failed to read booking category.", "text/plain");
            }
        });

        http.createContext("/api/category/update", ex -> {
            if (!requireMethod(ex, "POST")) {
                return;
            }

            try {
                Map<String, String> form = readForm(ex);

                BookingCategoryResponse response = stub.updateBookingCategory(
                        UpdateBookingCategoryRequest.newBuilder()
                                .setId(required(form, "id"))
                                .setName(required(form, "name"))
                                .setDescription(required(form, "desc"))
                                .build());

                respond(ex, 200, formatBookingCategory(response.getBookingCategory()), "text/plain");
            } catch (StatusRuntimeException exception) {
                respondGrpcError(ex, exception);
            } catch (IllegalArgumentException exception) {
                respond(ex, 400, exception.getMessage(), "text/plain");
            } catch (Exception exception) {
                exception.printStackTrace();
                respond(ex, 500, "Frontend failed to update booking category.", "text/plain");
            }
        });

        http.createContext("/api/category/delete", ex -> {
            if (!requireMethod(ex, "DELETE")) {
                return;
            }

            try {
                DeleteResponse response = stub.deleteBookingCategory(
                        GetByIdRequest.newBuilder()
                                .setId(requiredQuery(ex, "id"))
                                .build());

                respond(ex, 200, "Deleted: " + response.getDeleted(), "text/plain");
            } catch (StatusRuntimeException exception) {
                respondGrpcError(ex, exception);
            } catch (IllegalArgumentException exception) {
                respond(ex, 400, exception.getMessage(), "text/plain");
            } catch (Exception exception) {
                exception.printStackTrace();
                respond(ex, 500, "Frontend failed to delete booking category.", "text/plain");
            }
        });

        // PROMOTIONAL MEDIA CRUD

        http.createContext("/api/media/create", ex -> {
            if (!requireMethod(ex, "POST")) {
                return;
            }

            try {
                Map<String, String> form = readForm(ex);

                PromoMediaResponse response = stub.createPromoMedia(
                        CreatePromoMediaRequest.newBuilder()
                                .setEventId(required(form, "eventId"))
                                .setImageUrl(required(form, "url"))
                                .build());

                respond(ex, 200, formatPromoMedia(response.getPromoMedia()), "text/plain");
            } catch (StatusRuntimeException exception) {
                respondGrpcError(ex, exception);
            } catch (IllegalArgumentException exception) {
                respond(ex, 400, exception.getMessage(), "text/plain");
            } catch (Exception exception) {
                exception.printStackTrace();
                respond(ex, 500, "Frontend failed to create promotional media.", "text/plain");
            }
        });

        http.createContext("/api/media/read", ex -> {
            if (!requireMethod(ex, "GET")) {
                return;
            }

            try {
                PromoMediaResponse response = stub.getPromoMedia(
                        GetByIdRequest.newBuilder()
                                .setId(requiredQuery(ex, "id"))
                                .build());

                respond(ex, 200, formatPromoMedia(response.getPromoMedia()), "text/plain");
            } catch (StatusRuntimeException exception) {
                respondGrpcError(ex, exception);
            } catch (IllegalArgumentException exception) {
                respond(ex, 400, exception.getMessage(), "text/plain");
            } catch (Exception exception) {
                exception.printStackTrace();
                respond(ex, 500, "Frontend failed to read promotional media.", "text/plain");
            }
        });

        http.createContext("/api/media/update", ex -> {
            if (!requireMethod(ex, "POST")) {
                return;
            }

            try {
                Map<String, String> form = readForm(ex);

                PromoMediaResponse response = stub.updatePromoMedia(
                        UpdatePromoMediaRequest.newBuilder()
                                .setId(required(form, "id"))
                                .setEventId(required(form, "eventId"))
                                .setImageUrl(required(form, "url"))
                                .build());

                respond(ex, 200, formatPromoMedia(response.getPromoMedia()), "text/plain");
            } catch (StatusRuntimeException exception) {
                respondGrpcError(ex, exception);
            } catch (IllegalArgumentException exception) {
                respond(ex, 400, exception.getMessage(), "text/plain");
            } catch (Exception exception) {
                exception.printStackTrace();
                respond(ex, 500, "Frontend failed to update promotional media.", "text/plain");
            }
        });

        http.createContext("/api/media/delete", ex -> {
            if (!requireMethod(ex, "DELETE")) {
                return;
            }

            try {
                DeleteResponse response = stub.deletePromoMedia(
                        GetByIdRequest.newBuilder()
                                .setId(requiredQuery(ex, "id"))
                                .build());

                respond(ex, 200, "Deleted: " + response.getDeleted(), "text/plain");
            } catch (StatusRuntimeException exception) {
                respondGrpcError(ex, exception);
            } catch (IllegalArgumentException exception) {
                respond(ex, 400, exception.getMessage(), "text/plain");
            } catch (Exception exception) {
                exception.printStackTrace();
                respond(ex, 500, "Frontend failed to delete promotional media.", "text/plain");
            }
        });

        // RELATIONSHIP MANAGEMENT

        http.createContext("/api/relationship/artist/add", ex -> {
            if (!requireMethod(ex, "POST")) {
                return;
            }
            handleArtistRelationship(ex, stub, true);
        });

        http.createContext("/api/relationship/artist/remove", ex -> {
            if (!requireMethod(ex, "POST")) {
                return;
            }
            handleArtistRelationship(ex, stub, false);
        });

        http.createContext("/api/relationship/category/add", ex -> {
            if (!requireMethod(ex, "POST")) {
                return;
            }
            handleCategoryRelationship(ex, stub, true);
        });

        http.createContext("/api/relationship/category/remove", ex -> {
            if (!requireMethod(ex, "POST")) {
                return;
            }
            handleCategoryRelationship(ex, stub, false);
        });

        http.start();
        System.out.println("Frontend listening on :8080, backend at " + backendHost + ":50051");
    }

    private static void handleArtistRelationship(
            HttpExchange ex,
            FestHubGrpc.FestHubBlockingStub stub,
            boolean add) throws IOException {

        try {
            Map<String, String> form = readForm(ex);
            EventArtistRequest request = EventArtistRequest.newBuilder()
                    .setEventId(required(form, "eventId"))
                    .setArtistId(required(form, "artistId"))
                    .build();

            RelationshipResponse response = add
                    ? stub.addArtistToEvent(request)
                    : stub.removeArtistFromEvent(request);

            respond(ex, 200, "Changed: " + response.getChanged(), "text/plain");
        } catch (StatusRuntimeException exception) {
            respondGrpcError(ex, exception);
        } catch (IllegalArgumentException exception) {
            respond(ex, 400, exception.getMessage(), "text/plain");
        } catch (Exception exception) {
            exception.printStackTrace();
            respond(ex, 500, "Frontend failed to modify artist-event relationship.", "text/plain");
        }
    }

    private static void handleCategoryRelationship(
            HttpExchange ex,
            FestHubGrpc.FestHubBlockingStub stub,
            boolean add) throws IOException {

        try {
            Map<String, String> form = readForm(ex);
            EventCategoryRequest request = EventCategoryRequest.newBuilder()
                    .setEventId(required(form, "eventId"))
                    .setCategoryId(required(form, "categoryId"))
                    .build();

            RelationshipResponse response = add
                    ? stub.addCategoryToEvent(request)
                    : stub.removeCategoryFromEvent(request);

            respond(ex, 200, "Changed: " + response.getChanged(), "text/plain");
        } catch (StatusRuntimeException exception) {
            respondGrpcError(ex, exception);
        } catch (IllegalArgumentException exception) {
            respond(ex, 400, exception.getMessage(), "text/plain");
        } catch (Exception exception) {
            exception.printStackTrace();
            respond(ex, 500, "Frontend failed to modify category-event relationship.", "text/plain");
        }
    }

    private static boolean requireMethod(HttpExchange ex, String method) throws IOException {
        if (method.equals(ex.getRequestMethod())) {
            return true;
        }

        respond(ex, 405, "Method Not Allowed", "text/plain");
        return false;
    }

    private static Map<String, String> readForm(HttpExchange ex) throws IOException {
        String body = new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        return parseUrlEncoded(body);
    }

    private static Map<String, String> parseUrlEncoded(String encoded) {
        Map<String, String> values = new HashMap<>();

        if (encoded == null || encoded.isBlank()) {
            return values;
        }

        for (String pair : encoded.split("&")) {
            String[] parts = pair.split("=", 2);
            String key = URLDecoder.decode(parts[0], StandardCharsets.UTF_8);
            String value = parts.length == 2
                    ? URLDecoder.decode(parts[1], StandardCharsets.UTF_8)
                    : "";
            values.put(key, value);
        }

        return values;
    }

    private static String requiredQuery(HttpExchange ex, String key) {
        Map<String, String> query = parseUrlEncoded(ex.getRequestURI().getRawQuery());
        return required(query, key);
    }

    private static String required(Map<String, String> values, String key) {
        String value = values.get(key);
        if (!hasText(value)) {
            throw new IllegalArgumentException("Missing required field: " + key);
        }
        return value;
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private static int parseInt(String value, String fieldName) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException(fieldName + " must be a whole number");
        }
    }

    private static long dollarsToCents(String value) {
        try {
            return new BigDecimal(value)
                    .movePointRight(2)
                    .setScale(0, RoundingMode.UNNECESSARY)
                    .longValueExact();
        } catch (ArithmeticException | NumberFormatException exception) {
            throw new IllegalArgumentException("price must be a valid dollar amount with at most two decimals");
        }
    }

    private static String centsToDollars(long cents) {
        return BigDecimal.valueOf(cents, 2).toPlainString();
    }

    private static void putIfPresent(
            Map<String, String> values,
            String key,
            java.util.function.Consumer<String> setter) {

        String value = values.get(key);
        if (hasText(value)) {
            setter.accept(value);
        }
    }

    private static void respondGrpcError(HttpExchange ex, StatusRuntimeException exception)
            throws IOException {

        Status status = exception.getStatus();
        int httpCode = switch (status.getCode()) {
            case INVALID_ARGUMENT -> 400;
            case NOT_FOUND -> 404;
            case PERMISSION_DENIED -> 403;
            case ALREADY_EXISTS, FAILED_PRECONDITION, ABORTED -> 409;
            case UNAVAILABLE -> 503;
            default -> 500;
        };

        String message = status.getDescription();
        if (!hasText(message)) {
            message = status.getCode().name();
        }

        respond(ex, httpCode, message, "text/plain");
    }

    private static String formatArtist(Artist artist) {
        return String.join("\n",
                "ID: " + artist.getId(),
                "Name: " + artist.getName(),
                "Booking Contact: " + artist.getBookingContact(),
                "Event IDs: " + artist.getEventIdsList());
    }

    private static String formatConcertEvent(ConcertEvent event) {
        return String.join("\n",
                "ID: " + event.getId(),
                "Name: " + event.getName(),
                "Description: " + event.getDescription(),
                "Available Tickets: " + event.getAvailableTickets(),
                "Ticket Price: $" + centsToDollars(event.getTicketPriceCents()),
                "Artist IDs: " + event.getArtistIdsList(),
                "Category IDs: " + event.getCategoryIdsList(),
                "Promo Media IDs: " + event.getPromoMediaIdsList());
    }

    private static String formatBookingCategory(BookingCategory category) {
        return String.join("\n",
                "ID: " + category.getId(),
                "Name: " + category.getName(),
                "Description: " + category.getDescription(),
                "Event IDs: " + category.getEventIdsList());
    }

    private static String formatPromoMedia(PromoMedia media) {
        return String.join("\n",
                "ID: " + media.getId(),
                "Event ID: " + media.getEventId(),
                "Image URL: " + media.getImageUrl());
    }

    private static void respond(HttpExchange ex, int code, String body, String type)
            throws IOException {

        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type", type + "; charset=utf-8");
        ex.sendResponseHeaders(code, bytes.length);

        try (OutputStream os = ex.getResponseBody()) {
            os.write(bytes);
        }
    }
}
