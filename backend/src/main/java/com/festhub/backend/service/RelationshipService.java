package com.festhub.backend.service;

import com.festhub.backend.repository.ArtistRepository;
import com.festhub.backend.repository.BookingCategoryRepository;
import com.festhub.backend.repository.ConcertEventRepository;
import com.festhub.backend.repository.RelationshipRepository;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.UUID;

public final class RelationshipService {

    private final RelationshipRepository relationshipRepository;
    private final ConcertEventRepository concertEventRepository;
    private final ArtistRepository artistRepository;
    private final BookingCategoryRepository bookingCategoryRepository;

    public RelationshipService(
            RelationshipRepository relationshipRepository,
            ConcertEventRepository concertEventRepository,
            ArtistRepository artistRepository,
            BookingCategoryRepository bookingCategoryRepository) {
        this.relationshipRepository = Objects.requireNonNull(relationshipRepository);
        this.concertEventRepository = Objects.requireNonNull(concertEventRepository);
        this.artistRepository = Objects.requireNonNull(artistRepository);
        this.bookingCategoryRepository = Objects.requireNonNull(bookingCategoryRepository);
    }

    public boolean addArtistToEvent(UUID eventId, UUID artistId) {
        Objects.requireNonNull(eventId);
        Objects.requireNonNull(artistId);

        verifyEventExists(eventId);
        verifyArtistExists(artistId);

        return relationshipRepository.addArtistToEvent(eventId, artistId);
    }

    public boolean removeArtistFromEvent(UUID eventId, UUID artistId) {
        Objects.requireNonNull(eventId);
        Objects.requireNonNull(artistId);

        verifyEventExists(eventId);
        verifyArtistExists(artistId);

        return relationshipRepository.removeArtistFromEvent(eventId, artistId);
    }

    public boolean addCategoryToEvent(UUID eventId, UUID categoryId) {
        Objects.requireNonNull(eventId);
        Objects.requireNonNull(categoryId);

        verifyEventExists(eventId);
        verifyCategoryExists(categoryId);

        return relationshipRepository.addCategoryToEvent(eventId, categoryId);
    }

    public boolean removeCategoryFromEvent(UUID eventId, UUID categoryId) {
        Objects.requireNonNull(eventId);
        Objects.requireNonNull(categoryId);

        verifyEventExists(eventId);
        verifyCategoryExists(categoryId);

        return relationshipRepository.removeCategoryFromEvent(eventId, categoryId);
    }

    public List<UUID> findArtistIdsByEventId(UUID eventId) {
        Objects.requireNonNull(eventId);
        return relationshipRepository.findArtistIdsByEventId(eventId);
    }

    public List<UUID> findEventIdsByArtistId(UUID artistId) {
        Objects.requireNonNull(artistId);
        return relationshipRepository.findEventIdsByArtistId(artistId);
    }

    public List<UUID> findCategoryIdsByEventId(UUID eventId) {
        Objects.requireNonNull(eventId);
        return relationshipRepository.findCategoryIdsByEventId(eventId);
    }

    public List<UUID> findEventIdsByCategoryId(UUID categoryId) {
        Objects.requireNonNull(categoryId);
        return relationshipRepository.findEventIdsByCategoryId(categoryId);
    }

    public List<UUID> findPromoMediaIdsByEventId(UUID eventId) {
        Objects.requireNonNull(eventId);
        return relationshipRepository.findPromoMediaIdsByEventId(eventId);
    }

    private void verifyEventExists(UUID eventId) {
        if (concertEventRepository.findById(eventId).isEmpty()) {
            throw new NoSuchElementException("Concert event not found");
        }
    }

    private void verifyArtistExists(UUID artistId) {
        if (artistRepository.findById(artistId).isEmpty()) {
            throw new NoSuchElementException("Artist not found");
        }
    }

    private void verifyCategoryExists(UUID categoryId) {
        if (bookingCategoryRepository.findById(categoryId).isEmpty()) {
            throw new NoSuchElementException("Booking category not found");
        }
    }
}
