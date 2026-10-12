package com.festhub.backend.model;

import java.util.UUID;

public record Artist(
        UUID id,
        String name,
        String bookingContact) {
}
