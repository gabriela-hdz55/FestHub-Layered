package com.festhub.backend.model;

import java.util.UUID;

public record BookingCategory(
        UUID id,
        String name,
        String description) {
}
