package com.festhub.backend.repository;

import com.festhub.backend.data.Database;
import com.festhub.backend.model.BookingCategory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public final class JdbcBookingCategoryRepository
        implements BookingCategoryRepository {

    private static final String INSERT_SQL = """
            INSERT INTO booking_category (name, description)
            VALUES (?, ?)
            RETURNING id, name, description
            """;

    private static final String FIND_BY_ID_SQL = """
            SELECT id, name, description
            FROM booking_category
            WHERE id = ?
            """;

    private static final String FIND_ALL_SQL = """
            SELECT id, name, description
            FROM booking_category
            ORDER BY name
            """;

    private static final String UPDATE_SQL = """
            UPDATE booking_category
            SET name = ?, description = ?
            WHERE id = ?
            RETURNING id, name, description
            """;

    private static final String DELETE_SQL = """
            DELETE FROM booking_category
            WHERE id = ?
            """;

    @Override
    public BookingCategory save(BookingCategory category) {
        try (
                Connection connection = Database.connect();
                PreparedStatement statement = connection.prepareStatement(INSERT_SQL)) {
            statement.setString(1, category.name());
            statement.setString(2, category.description());

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return mapRow(resultSet);
                }

                throw new IllegalStateException(
                        "Creating booking category returned no row");
            }
        } catch (SQLException exception) {
            throw new IllegalStateException(
                    "Failed to create booking category",
                    exception);
        }
    }

    @Override
    public Optional<BookingCategory> findById(UUID id) {
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
                    "Failed to find booking category",
                    exception);
        }
    }

    @Override
    public List<BookingCategory> findAll() {
        List<BookingCategory> categories = new ArrayList<>();

        try (
                Connection connection = Database.connect();
                PreparedStatement statement = connection.prepareStatement(FIND_ALL_SQL);
                ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                categories.add(mapRow(resultSet));
            }

            return categories;
        } catch (SQLException exception) {
            throw new IllegalStateException(
                    "Failed to list booking categories",
                    exception);
        }
    }

    @Override
    public Optional<BookingCategory> update(BookingCategory category) {
        try (
                Connection connection = Database.connect();
                PreparedStatement statement = connection.prepareStatement(UPDATE_SQL)) {
            statement.setString(1, category.name());
            statement.setString(2, category.description());
            statement.setObject(3, category.id());

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.of(mapRow(resultSet));
                }

                return Optional.empty();
            }
        } catch (SQLException exception) {
            throw new IllegalStateException(
                    "Failed to update booking category",
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
                    "Failed to delete booking category",
                    exception);
        }
    }

    private BookingCategory mapRow(ResultSet resultSet)
            throws SQLException {
        return new BookingCategory(
                resultSet.getObject("id", UUID.class),
                resultSet.getString("name"),
                resultSet.getString("description"));
    }
}
