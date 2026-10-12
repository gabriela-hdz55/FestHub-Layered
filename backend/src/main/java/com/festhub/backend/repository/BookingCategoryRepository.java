package com.festhub.backend.repository;

import com.festhub.backend.model.BookingCategory;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BookingCategoryRepository {

    BookingCategory save(BookingCategory category);

    Optional<BookingCategory> findById(UUID id);

    List<BookingCategory> findAll();

    Optional<BookingCategory> update(BookingCategory category);

    boolean deleteById(UUID id);
}
