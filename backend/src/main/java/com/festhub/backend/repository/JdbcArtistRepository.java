package com.festhub.backend.repository;

import com.festhub.backend.data.Database;
import com.festhub.backend.model.Artist;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public final class JdbcArtistRepository
        implements ArtistRepository {

    private static final String INSERT_SQL = """
            INSERT INTO artist (name, booking_contact)
            VALUES (?, ?)
            RETURNING id, name, booking_contact
            """;

    private static final String FIND_BY_ID_SQL = """
            SELECT id, name, booking_contact
            FROM artist
            WHERE id = ?
            """;

    private static final String FIND_ALL_SQL = """
            SELECT id, name, booking_contact
            FROM artist
            ORDER BY name
            """;

    private static final String UPDATE_SQL = """
            UPDATE artist
            SET name = ?, booking_contact = ?
            WHERE id = ?
            RETURNING id, name, booking_contact
            """;

    private static final String DELETE_SQL = """
            DELETE FROM artist
            WHERE id = ?
            """;

    @Override
    public Artist save(Artist artist) {
        try (
                Connection connection = Database.connect();
                PreparedStatement statement = connection.prepareStatement(INSERT_SQL)) {
            statement.setString(1, artist.name());
            statement.setString(2, artist.bookingContact());

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return mapRow(resultSet);
                }

                throw new IllegalStateException(
                        "Creating artist returned no row");
            }
        } catch (SQLException exception) {
            throw new IllegalStateException(
                    "Failed to create artist",
                    exception);
        }
    }

    @Override
    public Optional<Artist> findById(UUID id) {
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
                    "Failed to find artist",
                    exception);
        }
    }

    @Override
    public List<Artist> findAll() {
        List<Artist> artists = new ArrayList<>();

        try (
                Connection connection = Database.connect();
                PreparedStatement statement = connection.prepareStatement(FIND_ALL_SQL);
                ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                artists.add(mapRow(resultSet));
            }

            return artists;
        } catch (SQLException exception) {
            throw new IllegalStateException(
                    "Failed to list artists",
                    exception);
        }
    }

    @Override
    public Optional<Artist> update(Artist artist) {
        try (
                Connection connection = Database.connect();
                PreparedStatement statement = connection.prepareStatement(UPDATE_SQL)) {
            statement.setString(1, artist.name());
            statement.setString(2, artist.bookingContact());
            statement.setObject(3, artist.id());

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.of(mapRow(resultSet));
                }

                return Optional.empty();
            }
        } catch (SQLException exception) {
            throw new IllegalStateException(
                    "Failed to update artist",
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
                    "Failed to delete artist",
                    exception);
        }
    }

    private Artist mapRow(ResultSet resultSet)
            throws SQLException {
        return new Artist(
                resultSet.getObject("id", UUID.class),
                resultSet.getString("name"),
                resultSet.getString("booking_contact"));
    }
}
