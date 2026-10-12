package com.festhub.backend.model;

import java.util.UUID;

public record PromoMedia(
        UUID id,
        UUID eventId,
        String imageUrl) {
}
