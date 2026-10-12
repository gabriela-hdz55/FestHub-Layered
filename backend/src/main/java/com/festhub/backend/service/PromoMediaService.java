package com.festhub.backend.service;

import com.festhub.backend.model.PromoMedia;
import com.festhub.backend.repository.PromoMediaRepository;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public final class PromoMediaService {

    private final PromoMediaRepository repository;

    public PromoMediaService(PromoMediaRepository repository) {
        this.repository = Objects.requireNonNull(repository);
    }

    public PromoMedia create(UUID eventId, String imageUrl) {
        validate(eventId, imageUrl);

        PromoMedia promoMedia = new PromoMedia(null, eventId, imageUrl);

        return repository.save(promoMedia);
    }

    public Optional<PromoMedia> getById(UUID id) {
        Objects.requireNonNull(id);
        return repository.findById(id);
    }

    public List<PromoMedia> listAll() {
        return repository.findAll();
    }

    public Optional<PromoMedia> update(
            UUID id,
            UUID eventId,
            String imageUrl) {
        Objects.requireNonNull(id);
        validate(eventId, imageUrl);

        PromoMedia promoMedia = new PromoMedia(id, eventId, imageUrl);

        return repository.update(promoMedia);
    }

    public boolean delete(UUID id) {
        Objects.requireNonNull(id);
        return repository.deleteById(id);
    }

    private static void validate(UUID eventId, String imageUrl) {
        if (eventId == null) {
            throw new IllegalArgumentException(
                    "Promo media event ID cannot be null");
        }

        if (imageUrl == null) {
            throw new IllegalArgumentException(
                    "Promo media image URL cannot be null");
        }
    }
}
