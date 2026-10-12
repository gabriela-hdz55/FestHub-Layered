package com.festhub.backend.repository;

import com.festhub.backend.data.Database;
import com.festhub.backend.model.ConcertEvent;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public final class JdbcConcertEventRepository
        implements ConcertEventRepository {

    private static final String INSERT_WITH_ID_SQL = """
            INSERT INTO concert_event (id, name, description, available_tickets, ticket_price_cents)
            VALUES (?, ?, ?, ?, ?)
            RETURNING id, name, description, available_tickets, ticket_price_cents
            """;

    private static final String INSERT_WITHOUT_ID_SQL = """
            INSERT INTO concert_event (name, description, available_tickets, ticket_price_cents)
            VALUES (?, ?, ?, ?)
            RETURNING id, name, description, available_tickets, ticket_price_cents
            """;

    private static final String FIND_BY_ID_SQL = """
            SELECT id, name, description, available_tickets, ticket_price_cents
            FROM concert_event
            WHERE id = ?
            """;

    private static final String FIND_ALL_SQL = """
            SELECT id, name, description, available_tickets, ticket_price_cents
            FROM concert_event
            ORDER BY name
            """;

    private static final String UPDATE_SQL = """
            UPDATE concert_event
            SET name = ?, description = ?, available_tickets = ?, ticket_price_cents = ?
            WHERE id = ?
            RETURNING id, name, description, available_tickets, ticket_price_cents
            """;

    private static final String DELETE_SQL = """
            DELETE FROM concert_event
            WHERE id = ?
            """;

    @Override
    public ConcertEvent save(ConcertEvent event) {
        boolean hasExplicitId = event.id() != null;
        String sql = hasExplicitId ? INSERT_WITH_ID_SQL : INSERT_WITHOUT_ID_SQL;

        try (
                Connection connection = Database.connect();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            if (hasExplicitId) {
                statement.setObject(1, event.id());
                statement.setString(2, event.name());
                statement.setString(3, event.description());
                statement.setInt(4, event.availableTickets());
                statement.setLong(5, event.ticketPriceCents());
            } else {
                statement.setString(1, event.name());
                statement.setString(2, event.description());
                statement.setInt(3, event.availableTickets());
                statement.setLong(4, event.ticketPriceCents());
            }

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return mapRow(resultSet);
                }

                throw new IllegalStateException(
                        "Creating concert event returned no row");
            }
        } catch (SQLException exception) {
            throw new IllegalStateException(
                    "Failed to create concert event",
                    exception);
        }
    }

    @Override
    public Optional<ConcertEvent> findById(UUID id) {
        try (
                Connection connection = Database.connect();
                PreparedStatement statement = connection.prepareStatement(FIND_BY_ID_SQL)) {
            statement.setObject(1, id);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.of(mapRow(resultSet));
                }

                return Optional.empty();
            }
        } catch (SQLException exception) {
            throw new IllegalStateException(
                    "Failed to find concert event",
                    exception);
        }
    }

    @Override
    public List<ConcertEvent> findAll() {
        List<ConcertEvent> events = new ArrayList<>();

        try (
                Connection connection = Database.connect();
                PreparedStatement statement = connection.prepareStatement(FIND_ALL_SQL);
                ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                events.add(mapRow(resultSet));
            }

            return events;
        } catch (SQLException exception) {
            throw new IllegalStateException(
                    "Failed to list concert events",
                    exception);
        }
    }

    private static final String LOCK_EVENT_FOR_UPDATE_SQL = """
            SELECT id, name, description, available_tickets, ticket_price_cents
            FROM concert_event
            WHERE id = ?
            FOR UPDATE
            """;

    private static final String FIND_ASSOCIATED_ARTISTS_ORDERED_SQL = """
            SELECT artist_id
            FROM event_artist
            WHERE event_id = ?
            ORDER BY artist_id
            """;

    private static final String LOCK_ARTIST_FOR_UPDATE_SQL = """
            SELECT id
            FROM artist
            WHERE id = ?
            FOR UPDATE
            """;

    private static final String CHECK_ARTIST_OTHER_EVENT_COLLISION_SQL = """
            SELECT 1
            FROM concert_event ce
            JOIN event_artist ea ON ea.event_id = ce.id
            WHERE ea.artist_id = ? AND ce.id != ? AND ce.name = ?
            """;

    private static final String CALCULATE_ARTIST_TOTAL_TICKET_PRICE_SQL = """
            SELECT COALESCE(SUM(ce.ticket_price_cents), 0)
            FROM concert_event ce
            JOIN event_artist ea ON ea.event_id = ce.id
            WHERE ea.artist_id = ?
            """;

    private static final String FIND_ASSOCIATED_CATEGORY_IDS_SQL = """
            SELECT category_id
            FROM event_category
            WHERE event_id = ?
            """;

    private static final String COUNT_REMAINING_CATEGORY_LINKS_SQL = """
            SELECT COUNT(*)
            FROM event_category
            WHERE category_id = ?
            """;

    private static final String DELETE_BOOKING_CATEGORY_SQL = """
            DELETE FROM booking_category
            WHERE id = ?
            """;

    @Override
    public Optional<ConcertEvent> update(ConcertEvent event) {
        try (
                Connection connection = Database.connect();
                PreparedStatement statement = connection.prepareStatement(UPDATE_SQL)) {
            statement.setString(1, event.name());
            statement.setString(2, event.description());
            statement.setInt(3, event.availableTickets());
            statement.setLong(4, event.ticketPriceCents());
            statement.setObject(5, event.id());

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.of(mapRow(resultSet));
                }

                return Optional.empty();
            }
        } catch (SQLException exception) {
            throw new IllegalStateException(
                    "Failed to update concert event",
                    exception);
        }
    }

    @Override
    public Optional<ConcertEvent> updateWithCrossRecordChecks(
            UUID id,
            Optional<String> name,
            Optional<String> description,
            Optional<Integer> availableTickets,
            Optional<Long> ticketPriceCents,
            long maxArtistTicketPriceSumCents) {
        try (Connection connection = Database.connect()) {
            connection.setAutoCommit(false);

            try {
                // a. Lock & load ConcertEvent first
                ConcertEvent existing;
                try (PreparedStatement stmt = connection.prepareStatement(LOCK_EVENT_FOR_UPDATE_SQL)) {
                    stmt.setObject(1, id);
                    try (ResultSet rs = stmt.executeQuery()) {
                        if (!rs.next()) {
                            connection.rollback();
                            return Optional.empty();
                        }
                        existing = mapRow(rs);
                    }
                }

                String mergedName = name.orElse(existing.name());
                String mergedDesc = description.orElse(existing.description());
                int mergedTickets = availableTickets.orElse(existing.availableTickets());
                long mergedPrice = ticketPriceCents.orElse(existing.ticketPriceCents());

                // Validate complete merged state
                validateEventInvariants(mergedName, mergedDesc, mergedTickets, mergedPrice);

                // b. Identify associated Artists in deterministic UUID order
                List<UUID> associatedArtistIds = new ArrayList<>();
                try (PreparedStatement stmt = connection.prepareStatement(FIND_ASSOCIATED_ARTISTS_ORDERED_SQL)) {
                    stmt.setObject(1, id);
                    try (ResultSet rs = stmt.executeQuery()) {
                        while (rs.next()) {
                            associatedArtistIds.add(rs.getObject("artist_id", UUID.class));
                        }
                    }
                }

                // c. Lock affected Artist rows in deterministic UUID order
                for (UUID artistId : associatedArtistIds) {
                    try (PreparedStatement stmt = connection.prepareStatement(LOCK_ARTIST_FOR_UPDATE_SQL)) {
                        stmt.setObject(1, artistId);
                        try (ResultSet rs = stmt.executeQuery()) {
                            // Row locked
                        }
                    }
                }

                // d. Validate per-Artist event-name collisions (if name changed)
                if (!mergedName.equals(existing.name())) {
                    for (UUID artistId : associatedArtistIds) {
                        try (PreparedStatement stmt = connection.prepareStatement(CHECK_ARTIST_OTHER_EVENT_COLLISION_SQL)) {
                            stmt.setObject(1, artistId);
                            stmt.setObject(2, id);
                            stmt.setString(3, mergedName);
                            try (ResultSet rs = stmt.executeQuery()) {
                                if (rs.next()) {
                                    connection.rollback();
                                    throw new IllegalArgumentException(
                                            "Renaming concert event to '" + mergedName + "' conflicts with an existing event name for an associated artist");
                                }
                            }
                        }
                    }
                }

                // e. Validate projected ticket-price totals (if ticketPriceCents changed)
                if (mergedPrice != existing.ticketPriceCents()) {
                    for (UUID artistId : associatedArtistIds) {
                        long currentArtistSum;
                        try (PreparedStatement stmt = connection.prepareStatement(CALCULATE_ARTIST_TOTAL_TICKET_PRICE_SQL)) {
                            stmt.setObject(1, artistId);
                            try (ResultSet rs = stmt.executeQuery()) {
                                rs.next();
                                currentArtistSum = rs.getLong(1);
                            }
                        }

                        long projectedTotal = currentArtistSum - existing.ticketPriceCents() + mergedPrice;
                        if (projectedTotal > maxArtistTicketPriceSumCents) {
                            connection.rollback();
                            throw new IllegalArgumentException(
                                    "Updating concert event ticket price would cause an associated artist to exceed the $500,000 cap (projected total: "
                                            + projectedTotal
                                            + " cents, cap: "
                                            + maxArtistTicketPriceSumCents
                                            + " cents)");
                        }
                    }
                }

                // f. Execute UPDATE concert_event
                ConcertEvent updatedEvent;
                try (PreparedStatement stmt = connection.prepareStatement(UPDATE_SQL)) {
                    stmt.setString(1, mergedName);
                    stmt.setString(2, mergedDesc);
                    stmt.setInt(3, mergedTickets);
                    stmt.setLong(4, mergedPrice);
                    stmt.setObject(5, id);
                    try (ResultSet rs = stmt.executeQuery()) {
                        if (rs.next()) {
                            updatedEvent = mapRow(rs);
                        } else {
                            connection.rollback();
                            return Optional.empty();
                        }
                    }
                }

                // g. Commit
                connection.commit();
                return Optional.of(updatedEvent);
            } catch (SQLException exception) {
                connection.rollback();
                throw new IllegalStateException("Failed to update concert event with cross-record checks", exception);
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to manage database connection for updating concert event", exception);
        }
    }

    @Override
    public boolean deleteById(UUID id) {
        try (Connection connection = Database.connect()) {
            connection.setAutoCommit(false);

            try {
                // 1. Capture associated BookingCategory IDs
                List<UUID> associatedCategoryIds = new ArrayList<>();
                try (PreparedStatement stmt = connection.prepareStatement(FIND_ASSOCIATED_CATEGORY_IDS_SQL)) {
                    stmt.setObject(1, id);
                    try (ResultSet rs = stmt.executeQuery()) {
                        while (rs.next()) {
                            associatedCategoryIds.add(rs.getObject("category_id", UUID.class));
                        }
                    }
                }

                // 2. Delete concert_event (PostgreSQL cascades promo_media, event_artist, event_category)
                int affectedRows;
                try (PreparedStatement stmt = connection.prepareStatement(DELETE_SQL)) {
                    stmt.setObject(1, id);
                    affectedRows = stmt.executeUpdate();
                }

                if (affectedRows == 0) {
                    connection.rollback();
                    return false;
                }

                // 3. For each previously associated category, check if orphaned and delete
                for (UUID categoryId : associatedCategoryIds) {
                    long remainingLinks;
                    try (PreparedStatement stmt = connection.prepareStatement(COUNT_REMAINING_CATEGORY_LINKS_SQL)) {
                        stmt.setObject(1, categoryId);
                        try (ResultSet rs = stmt.executeQuery()) {
                            rs.next();
                            remainingLinks = rs.getLong(1);
                        }
                    }

                    if (remainingLinks == 0) {
                        try (PreparedStatement stmt = connection.prepareStatement(DELETE_BOOKING_CATEGORY_SQL)) {
                            stmt.setObject(1, categoryId);
                            stmt.executeUpdate();
                        }
                    }
                }

                connection.commit();
                return true;
            } catch (SQLException exception) {
                connection.rollback();
                throw new IllegalStateException(
                        "Failed to delete concert event and clean up orphaned categories",
                        exception);
            }
        } catch (SQLException exception) {
            throw new IllegalStateException(
                    "Failed to manage database connection for deleting concert event",
                    exception);
        }
    }

    private static void validateEventInvariants(
            String name,
            String description,
            int availableTickets,
            long ticketPriceCents) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException(
                    "Concert event name cannot be null or blank");
        }

        if (name.length() > 2000) {
            throw new IllegalArgumentException(
                    "Concert event name cannot exceed 2000 characters");
        }

        if (description == null || description.isBlank()) {
            throw new IllegalArgumentException(
                    "Concert event description cannot be null or blank");
        }

        if (description.length() > 10000) {
            throw new IllegalArgumentException(
                    "Concert event description cannot exceed 10000 characters");
        }

        if (availableTickets < 0) {
            throw new IllegalArgumentException(
                    "Available tickets cannot be negative");
        }

        if (ticketPriceCents <= 0) {
            throw new IllegalArgumentException(
                    "Ticket price must be greater than zero");
        }

        if (availableTickets == 0 && ticketPriceCents > 10000L) {
            throw new IllegalArgumentException(
                    "Sold-out concert event ticket price cannot exceed 10000 cents");
        }
    }

    private ConcertEvent mapRow(ResultSet resultSet)
            throws SQLException {
        return new ConcertEvent(
                resultSet.getObject("id", UUID.class),
                resultSet.getString("name"),
                resultSet.getString("description"),
                resultSet.getInt("available_tickets"),
                resultSet.getLong("ticket_price_cents"));
    }
}
