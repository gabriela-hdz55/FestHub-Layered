-- ---------------------------------------------------------
-- CONCERT EVENT
-- ---------------------------------------------------------
CREATE TABLE IF NOT EXISTS concert_event (
    id                 UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name               TEXT    NOT NULL,
    description        TEXT    NOT NULL,
    available_tickets  INTEGER NOT NULL,
    ticket_price_cents INTEGER NOT NULL   -- price stored in cents to avoid floating point issues
);

-- ---------------------------------------------------------
-- ARTIST
-- ---------------------------------------------------------
CREATE TABLE IF NOT EXISTS artist (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name            TEXT NOT NULL,
    booking_contact TEXT NOT NULL
);

-- ---------------------------------------------------------
-- BOOKING CATEGORY
-- ---------------------------------------------------------
CREATE TABLE IF NOT EXISTS booking_category (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name        TEXT NOT NULL,
    description TEXT NOT NULL
);

-- ---------------------------------------------------------
-- PROMOTIONAL MEDIA
-- ---------------------------------------------------------
CREATE TABLE IF NOT EXISTS promo_media (
    id        UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    event_id  UUID NOT NULL REFERENCES concert_event(id) ON DELETE CASCADE,
    image_url TEXT NOT NULL
);

-- ---------------------------------------------------------
-- EVENT ARTIST (join table)
-- ---------------------------------------------------------
CREATE TABLE IF NOT EXISTS event_artist (
    event_id  UUID NOT NULL REFERENCES concert_event(id) ON DELETE CASCADE,
    artist_id UUID NOT NULL REFERENCES artist(id)        ON DELETE CASCADE,
    PRIMARY KEY (event_id, artist_id)
);

-- ---------------------------------------------------------
-- EVENT CATEGORY (join table)
-- ---------------------------------------------------------
CREATE TABLE IF NOT EXISTS event_category (
    event_id    UUID NOT NULL REFERENCES concert_event(id)    ON DELETE CASCADE,
    category_id UUID NOT NULL REFERENCES booking_category(id) ON DELETE CASCADE,
    PRIMARY KEY (event_id, category_id)
);