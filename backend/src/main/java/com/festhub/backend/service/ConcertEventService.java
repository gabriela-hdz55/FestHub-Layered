package com.festhub.backend.service;

import com.festhub.backend.model.ConcertEvent;
import com.festhub.backend.repository.ConcertEventRepository;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public final class ConcertEventService {

    private static final int MAX_NAME_LENGTH = 2000;
    private static final int MAX_DESCRIPTION_LENGTH = 10000;
    private static final long MAX_SOLD_OUT_PRICE_CENTS = 10000L;

    private final ConcertEventRepository repository;

    public ConcertEventService(ConcertEventRepository repository) {
        this.repository = Objects.requireNonNull(repository);
    }

    public ConcertEvent create(
            UUID id,
            String name,
            String description,
            int availableTickets,
            long ticketPriceCents) {
        validate(name, description, availableTickets, ticketPriceCents);

        ConcertEvent event = new ConcertEvent(
                id,
                name,
                description,
                availableTickets,
                ticketPriceCents);

        return repository.save(event);
    }

    public Optional<ConcertEvent> getById(UUID id) {
        Objects.requireNonNull(id);
        return repository.findById(id);
    }

    public List<ConcertEvent> listAll() {
        return repository.findAll();
    }

    public Optional<ConcertEvent> update(
            UUID id,
            Optional<String> name,
            Optional<String> description,
            Optional<Integer> availableTickets,
            Optional<Long> ticketPriceCents) {
        Objects.requireNonNull(id);
        Objects.requireNonNull(name);
        Objects.requireNonNull(description);
        Objects.requireNonNull(availableTickets);
        Objects.requireNonNull(ticketPriceCents);

        Optional<ConcertEvent> existingOptional = repository.findById(id);
        if (existingOptional.isEmpty()) {
            return Optional.empty();
        }

        ConcertEvent existing = existingOptional.get();

        String mergedName = name.orElse(existing.name());
        String mergedDescription = description.orElse(existing.description());
        int mergedAvailableTickets = availableTickets.orElse(existing.availableTickets());
        long mergedTicketPriceCents = ticketPriceCents.orElse(existing.ticketPriceCents());

        validate(mergedName, mergedDescription, mergedAvailableTickets, mergedTicketPriceCents);

        ConcertEvent mergedEvent = new ConcertEvent(
                id,
                mergedName,
                mergedDescription,
                mergedAvailableTickets,
                mergedTicketPriceCents);

        return repository.update(mergedEvent);
    }

    public boolean delete(UUID id) {
        Objects.requireNonNull(id);
        return repository.deleteById(id);
    }

    private static void validate(
            String name,
            String description,
            int availableTickets,
            long ticketPriceCents) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException(
                    "Concert event name cannot be null or blank");
        }

        if (name.length() > MAX_NAME_LENGTH) {
            throw new IllegalArgumentException(
                    "Concert event name cannot exceed "
                            + MAX_NAME_LENGTH
                            + " characters");
        }

        if (description == null || description.isBlank()) {
            throw new IllegalArgumentException(
                    "Concert event description cannot be null or blank");
        }

        if (description.length() > MAX_DESCRIPTION_LENGTH) {
            throw new IllegalArgumentException(
                    "Concert event description cannot exceed "
                            + MAX_DESCRIPTION_LENGTH
                            + " characters");
        }

        if (availableTickets < 0) {
            throw new IllegalArgumentException(
                    "Available tickets cannot be negative");
        }

        if (ticketPriceCents <= 0) {
            throw new IllegalArgumentException(
                    "Ticket price must be greater than zero");
        }

        if (availableTickets == 0 && ticketPriceCents > MAX_SOLD_OUT_PRICE_CENTS) {
            throw new IllegalArgumentException(
                    "Sold-out concert event ticket price cannot exceed "
                            + MAX_SOLD_OUT_PRICE_CENTS
                            + " cents");
        }
    }
}
