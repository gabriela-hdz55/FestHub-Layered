package com.festhub.backend.service;

import com.festhub.backend.model.Artist;
import com.festhub.backend.repository.ArtistRepository;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public final class ArtistService {

    private final ArtistRepository repository;

    public ArtistService(ArtistRepository repository) {
        this.repository = Objects.requireNonNull(repository);
    }

    public Artist create(String name, String bookingContact) {
        validate(name, bookingContact);

        Artist artist = new Artist(null, name, bookingContact);

        return repository.save(artist);
    }

    public Optional<Artist> getById(UUID id) {
        Objects.requireNonNull(id);
        return repository.findById(id);
    }

    public List<Artist> listAll() {
        return repository.findAll();
    }

    public Optional<Artist> update(
            UUID id,
            String name,
            String bookingContact) {
        Objects.requireNonNull(id);
        validate(name, bookingContact);

        Artist artist = new Artist(id, name, bookingContact);

        return repository.update(artist);
    }

    public boolean delete(UUID id) {
        Objects.requireNonNull(id);
        return repository.deleteById(id);
    }

    private static void validate(String name, String bookingContact) {
        if (name == null) {
            throw new IllegalArgumentException(
                    "Artist name cannot be null");
        }

        if (bookingContact == null) {
            throw new IllegalArgumentException(
                    "Artist booking contact cannot be null");
        }
    }
}
