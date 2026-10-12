package com.festhub.backend.repository;

import com.festhub.backend.data.Database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import java.util.NoSuchElementException;

public final class JdbcRelationshipRepository
        implements RelationshipRepository {

    private static final String LOCK_CONCERT_EVENT_FOR_UPDATE_SQL = """
            SELECT id, name, ticket_price_cents
            FROM concert_event
            WHERE id = ?
            FOR UPDATE
            """;

    private static final String LOCK_ARTIST_FOR_UPDATE_SQL = """
            SELECT id
            FROM artist
            WHERE id = ?
            FOR UPDATE
            """;

    private static final String CHECK_EXISTING_RELATIONSHIP_SQL = """
            SELECT 1
            FROM event_artist
            WHERE event_id = ? AND artist_id = ?
            """;

    private static final String CHECK_EVENT_NAME_COLLISION_SQL = """
            SELECT 1
            FROM concert_event ce
            JOIN event_artist ea ON ea.event_id = ce.id
            WHERE ea.artist_id = ? AND ce.id != ? AND ce.name = ?
            """;

    private static final String CALCULATE_ARTIST_TICKET_PRICE_SUM_SQL = """
            SELECT COALESCE(SUM(ce.ticket_price_cents), 0)
            FROM concert_event ce
            JOIN event_artist ea ON ea.event_id = ce.id
            WHERE ea.artist_id = ?
            """;

    private static final String INSERT_RELATIONSHIP_SQL = """
            INSERT INTO event_artist (event_id, artist_id)
            VALUES (?, ?)
            """;

    private static final String REMOVE_ARTIST_SQL = """
            DELETE FROM event_artist
            WHERE event_id = ? AND artist_id = ?
            """;

    private static final String ADD_CATEGORY_SQL = """
            INSERT INTO event_category (event_id, category_id)
            VALUES (?, ?)
            ON CONFLICT DO NOTHING
            """;

    private static final String REMOVE_CATEGORY_SQL = """
            DELETE FROM event_category
            WHERE event_id = ? AND category_id = ?
            """;

    private static final String FIND_ARTIST_IDS_BY_EVENT_SQL = """
            SELECT artist_id
            FROM event_artist
            WHERE event_id = ?
            ORDER BY artist_id
            """;

    private static final String FIND_EVENT_IDS_BY_ARTIST_SQL = """
            SELECT event_id
            FROM event_artist
            WHERE artist_id = ?
            ORDER BY event_id
            """;

    private static final String FIND_CATEGORY_IDS_BY_EVENT_SQL = """
            SELECT category_id
            FROM event_category
            WHERE event_id = ?
            ORDER BY category_id
            """;

    private static final String FIND_EVENT_IDS_BY_CATEGORY_SQL = """
            SELECT event_id
            FROM event_category
            WHERE category_id = ?
            ORDER BY event_id
            """;

    private static final String FIND_PROMO_MEDIA_IDS_BY_EVENT_SQL = """
            SELECT id
            FROM promo_media
            WHERE event_id = ?
            ORDER BY id
            """;

    @Override
    public boolean addArtistToEvent(
            UUID eventId,
            UUID artistId,
            long maxTicketPriceSumCents) {
        try (Connection connection = Database.connect()) {
            connection.setAutoCommit(false);

            try {
                // 1. Lock and load ConcertEvent first
                String eventName;
                long eventTicketPrice;
                try (PreparedStatement stmt = connection.prepareStatement(LOCK_CONCERT_EVENT_FOR_UPDATE_SQL)) {
                    stmt.setObject(1, eventId);
                    try (ResultSet rs = stmt.executeQuery()) {
                        if (!rs.next()) {
                            connection.rollback();
                            throw new NoSuchElementException("Concert event not found");
                        }
                        eventName = rs.getString("name");
                        eventTicketPrice = rs.getLong("ticket_price_cents");
                    }
                }

                // 2. Lock Artist second
                try (PreparedStatement stmt = connection.prepareStatement(LOCK_ARTIST_FOR_UPDATE_SQL)) {
                    stmt.setObject(1, artistId);
                    try (ResultSet rs = stmt.executeQuery()) {
                        if (!rs.next()) {
                            connection.rollback();
                            throw new NoSuchElementException("Artist not found");
                        }
                    }
                }

                // 3. Check existing relationship
                try (PreparedStatement stmt = connection.prepareStatement(CHECK_EXISTING_RELATIONSHIP_SQL)) {
                    stmt.setObject(1, eventId);
                    stmt.setObject(2, artistId);
                    try (ResultSet rs = stmt.executeQuery()) {
                        if (rs.next()) {
                            connection.commit();
                            return false;
                        }
                    }
                }

                // 4. Check event-name collision
                try (PreparedStatement stmt = connection.prepareStatement(CHECK_EVENT_NAME_COLLISION_SQL)) {
                    stmt.setObject(1, artistId);
                    stmt.setObject(2, eventId);
                    stmt.setString(3, eventName);
                    try (ResultSet rs = stmt.executeQuery()) {
                        if (rs.next()) {
                            connection.rollback();
                            throw new IllegalArgumentException(
                                    "Artist is already booked for another concert event named '" + eventName + "'");
                        }
                    }
                }

                // 5. Calculate projected Artist ticket-price total
                long currentSum;
                try (PreparedStatement stmt = connection.prepareStatement(CALCULATE_ARTIST_TICKET_PRICE_SUM_SQL)) {
                    stmt.setObject(1, artistId);
                    try (ResultSet rs = stmt.executeQuery()) {
                        rs.next();
                        currentSum = rs.getLong(1);
                    }
                }

                long projectedTotal = currentSum + eventTicketPrice;
                if (projectedTotal > maxTicketPriceSumCents) {
                    connection.rollback();
                    throw new IllegalArgumentException(
                            "Artist ticket price cap of $500,000 exceeded (projected total: "
                                    + projectedTotal
                                    + " cents, cap: "
                                    + maxTicketPriceSumCents
                                    + " cents)");
                }

                // 6. Insert event_artist relationship
                try (PreparedStatement stmt = connection.prepareStatement(INSERT_RELATIONSHIP_SQL)) {
                    stmt.setObject(1, eventId);
                    stmt.setObject(2, artistId);
                    stmt.executeUpdate();
                }

                connection.commit();
                return true;
            } catch (SQLException exception) {
                connection.rollback();
                throw new IllegalStateException("Failed to add artist to event", exception);
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to manage database connection for adding artist to event", exception);
        }
    }

    @Override
    public boolean removeArtistFromEvent(UUID eventId, UUID artistId) {
        try (
                Connection connection = Database.connect();
                PreparedStatement statement = connection.prepareStatement(REMOVE_ARTIST_SQL)) {
            statement.setObject(1, eventId);
            statement.setObject(2, artistId);

            return statement.executeUpdate() > 0;
        } catch (SQLException exception) {
            throw new IllegalStateException(
                    "Failed to remove artist from event",
                    exception);
        }
    }

    @Override
    public boolean addCategoryToEvent(UUID eventId, UUID categoryId) {
        try (
                Connection connection = Database.connect();
                PreparedStatement statement = connection.prepareStatement(ADD_CATEGORY_SQL)) {
            statement.setObject(1, eventId);
            statement.setObject(2, categoryId);

            return statement.executeUpdate() > 0;
        } catch (SQLException exception) {
            throw new IllegalStateException(
                    "Failed to add category to event",
                    exception);
        }
    }

    @Override
    public boolean removeCategoryFromEvent(UUID eventId, UUID categoryId) {
        try (
                Connection connection = Database.connect();
                PreparedStatement statement = connection.prepareStatement(REMOVE_CATEGORY_SQL)) {
            statement.setObject(1, eventId);
            statement.setObject(2, categoryId);

            return statement.executeUpdate() > 0;
        } catch (SQLException exception) {
            throw new IllegalStateException(
                    "Failed to remove category from event",
                    exception);
        }
    }

    @Override
    public List<UUID> findArtistIdsByEventId(UUID eventId) {
        return findIds(FIND_ARTIST_IDS_BY_EVENT_SQL, eventId, "artist_id", "Failed to find artist IDs for event");
    }

    @Override
    public List<UUID> findEventIdsByArtistId(UUID artistId) {
        return findIds(FIND_EVENT_IDS_BY_ARTIST_SQL, artistId, "event_id", "Failed to find event IDs for artist");
    }

    @Override
    public List<UUID> findCategoryIdsByEventId(UUID eventId) {
        return findIds(FIND_CATEGORY_IDS_BY_EVENT_SQL, eventId, "category_id", "Failed to find category IDs for event");
    }

    @Override
    public List<UUID> findEventIdsByCategoryId(UUID categoryId) {
        return findIds(FIND_EVENT_IDS_BY_CATEGORY_SQL, categoryId, "event_id", "Failed to find event IDs for category");
    }

    @Override
    public List<UUID> findPromoMediaIdsByEventId(UUID eventId) {
        return findIds(FIND_PROMO_MEDIA_IDS_BY_EVENT_SQL, eventId, "id", "Failed to find promo media IDs for event");
    }

    private List<UUID> findIds(
            String sql,
            UUID parameter,
            String columnName,
            String errorMessage) {
        List<UUID> ids = new ArrayList<>();

        try (
                Connection connection = Database.connect();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setObject(1, parameter);

            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    ids.add(resultSet.getObject(columnName, UUID.class));
                }

                return ids;
            }
        } catch (SQLException exception) {
            throw new IllegalStateException(
                    errorMessage,
                    exception);
        }
    }
}
