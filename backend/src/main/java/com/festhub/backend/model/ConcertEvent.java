package com.festhub.backend.model;

import java.util.UUID;

public record ConcertEvent(
        UUID id,
        String name,
        String description,
        int availableTickets,
        long ticketPriceCents) {
}
