package com.festhub.backend.repository;

import com.festhub.backend.model.ConcertEvent;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ConcertEventRepository {

    ConcertEvent save(ConcertEvent event);

    Optional<ConcertEvent> findById(UUID id);

    List<ConcertEvent> findAll();

    Optional<ConcertEvent> update(ConcertEvent event);

    boolean deleteById(UUID id);
}
