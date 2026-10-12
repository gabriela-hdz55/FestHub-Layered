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
    public boolean deleteById(UUID id) {
        try (
                Connection connection = Database.connect();
                PreparedStatement statement = connection.prepareStatement(DELETE_SQL)) {
            statement.setObject(1, id);

            return statement.executeUpdate() > 0;
        } catch (SQLException exception) {
            throw new IllegalStateException(
                    "Failed to delete concert event",
                    exception);
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
