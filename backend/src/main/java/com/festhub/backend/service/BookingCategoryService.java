package com.festhub.backend.service;

import com.festhub.backend.model.BookingCategory;
import com.festhub.backend.repository.BookingCategoryRepository;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public final class BookingCategoryService {

    private static final int MAX_NAME_LENGTH = 2000;
    private static final int MAX_DESCRIPTION_LENGTH = 10000;

    private final BookingCategoryRepository repository;

    public BookingCategoryService(BookingCategoryRepository repository) {
        this.repository = Objects.requireNonNull(repository);
    }

    public BookingCategory create(String name, String description) {
        validate(name, description);

        BookingCategory category = new BookingCategory(null, name, description);

        return repository.save(category);
    }

    public Optional<BookingCategory> getById(UUID id) {
        Objects.requireNonNull(id);
        return repository.findById(id);
    }

    public List<BookingCategory> listAll() {
        return repository.findAll();
    }

    public Optional<BookingCategory> update(
            UUID id,
            String name,
            String description) {
        Objects.requireNonNull(id);
        validate(name, description);

        BookingCategory category = new BookingCategory(id, name, description);

        return repository.update(category);
    }

    public boolean delete(UUID id) {
        Objects.requireNonNull(id);
        return repository.deleteById(id);
    }

    private static void validate(String name, String description) {
        if (name == null) {
            throw new IllegalArgumentException(
                    "Booking category name cannot be null");
        }

        if (name.length() > MAX_NAME_LENGTH) {
            throw new IllegalArgumentException(
                    "Booking category name cannot exceed "
                            + MAX_NAME_LENGTH
                            + " characters");
        }

        if (description == null) {
            throw new IllegalArgumentException(
                    "Booking category description cannot be null");
        }

        if (description.length() > MAX_DESCRIPTION_LENGTH) {
            throw new IllegalArgumentException(
                    "Booking category description cannot exceed "
                            + MAX_DESCRIPTION_LENGTH
                            + " characters");
        }
    }
}
