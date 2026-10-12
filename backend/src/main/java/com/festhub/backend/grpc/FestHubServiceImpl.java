package com.festhub.backend.grpc;

import com.festhub.backend.model.BookingCategory;
import com.festhub.backend.service.BookingCategoryService;
import com.festhub.proto.BookingCategoryResponse;
import com.festhub.proto.CreateBookingCategoryRequest;
import com.festhub.proto.DeleteResponse;
import com.festhub.proto.FestHubGrpc;
import com.festhub.proto.GetByIdRequest;
import com.festhub.proto.ListBookingCategoriesResponse;
import com.festhub.proto.ListRequest;
import com.festhub.proto.UpdateBookingCategoryRequest;

import io.grpc.Status;
import io.grpc.stub.StreamObserver;

import java.util.Optional;
import java.util.UUID;

public final class FestHubServiceImpl
        extends FestHubGrpc.FestHubImplBase {

    private final BookingCategoryService bookingCategoryService;

    public FestHubServiceImpl(
            BookingCategoryService bookingCategoryService) {
        this.bookingCategoryService = bookingCategoryService;
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

    private static com.festhub.proto.BookingCategory toProto(
            BookingCategory category) {
        return com.festhub.proto.BookingCategory.newBuilder()
                .setId(category.id().toString())
                .setName(category.name())
                .setDescription(category.description())
                .build();
    }
}