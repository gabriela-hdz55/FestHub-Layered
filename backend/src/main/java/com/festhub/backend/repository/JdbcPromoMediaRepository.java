package com.festhub.backend.repository;

import com.festhub.backend.data.Database;
import com.festhub.backend.model.PromoMedia;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public final class JdbcPromoMediaRepository
        implements PromoMediaRepository {

    private static final String INSERT_SQL = """
            INSERT INTO promo_media (event_id, image_url)
            VALUES (?, ?)
            RETURNING id, event_id, image_url
            """;

    private static final String FIND_BY_ID_SQL = """
            SELECT id, event_id, image_url
            FROM promo_media
            WHERE id = ?
            """;

    private static final String FIND_ALL_SQL = """
            SELECT id, event_id, image_url
            FROM promo_media
            ORDER BY image_url
            """;

    private static final String UPDATE_SQL = """
            UPDATE promo_media
            SET event_id = ?, image_url = ?
            WHERE id = ?
            RETURNING id, event_id, image_url
            """;

    private static final String DELETE_SQL = """
            DELETE FROM promo_media
            WHERE id = ?
            """;

    @Override
    public PromoMedia save(PromoMedia promoMedia) {
        try (
                Connection connection = Database.connect();
                PreparedStatement statement = connection.prepareStatement(INSERT_SQL)) {
            statement.setObject(1, promoMedia.eventId());
            statement.setString(2, promoMedia.imageUrl());

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return mapRow(resultSet);
                }

                throw new IllegalStateException(
                        "Creating promo media returned no row");
            }
        } catch (SQLException exception) {
            throw new IllegalStateException(
                    "Failed to create promo media",
                    exception);
        }
    }

    @Override
    public Optional<PromoMedia> findById(UUID id) {
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
                    "Failed to find promo media",
                    exception);
        }
    }

    @Override
    public List<PromoMedia> findAll() {
        List<PromoMedia> promoMediaList = new ArrayList<>();

        try (
                Connection connection = Database.connect();
                PreparedStatement statement = connection.prepareStatement(FIND_ALL_SQL);
                ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                promoMediaList.add(mapRow(resultSet));
            }

            return promoMediaList;
        } catch (SQLException exception) {
            throw new IllegalStateException(
                    "Failed to list promo media",
                    exception);
        }
    }

    @Override
    public Optional<PromoMedia> update(PromoMedia promoMedia) {
        try (
                Connection connection = Database.connect();
                PreparedStatement statement = connection.prepareStatement(UPDATE_SQL)) {
            statement.setObject(1, promoMedia.eventId());
            statement.setString(2, promoMedia.imageUrl());
            statement.setObject(3, promoMedia.id());

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.of(mapRow(resultSet));
                }

                return Optional.empty();
            }
        } catch (SQLException exception) {
            throw new IllegalStateException(
                    "Failed to update promo media",
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
                    "Failed to delete promo media",
                    exception);
        }
    }

    private PromoMedia mapRow(ResultSet resultSet)
            throws SQLException {
        return new PromoMedia(
                resultSet.getObject("id", UUID.class),
                resultSet.getObject("event_id", UUID.class),
                resultSet.getString("image_url"));
    }
}
