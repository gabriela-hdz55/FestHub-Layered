package com.festhub.backend.repository;

import com.festhub.backend.model.Artist;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ArtistRepository {

    Artist save(Artist artist);

    Optional<Artist> findById(UUID id);

    List<Artist> findAll();

    Optional<Artist> update(Artist artist);

    boolean deleteById(UUID id);
}
