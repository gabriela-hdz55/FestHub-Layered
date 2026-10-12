package com.festhub.backend.repository;

import java.util.List;
import java.util.UUID;

public interface RelationshipRepository {

    boolean addArtistToEvent(
            UUID eventId,
            UUID artistId,
            long maxTicketPriceSumCents);

    boolean removeArtistFromEvent(UUID eventId, UUID artistId);

    boolean addCategoryToEvent(UUID eventId, UUID categoryId);

    boolean removeCategoryFromEvent(UUID eventId, UUID categoryId);

    List<UUID> findArtistIdsByEventId(UUID eventId);

    List<UUID> findEventIdsByArtistId(UUID artistId);

    List<UUID> findCategoryIdsByEventId(UUID eventId);

    List<UUID> findEventIdsByCategoryId(UUID categoryId);

    List<UUID> findPromoMediaIdsByEventId(UUID eventId);
}
