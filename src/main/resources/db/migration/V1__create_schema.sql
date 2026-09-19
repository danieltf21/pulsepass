CREATE TABLE venues (
                        id        BIGSERIAL PRIMARY KEY,
                        code      VARCHAR(50)  NOT NULL,
                        name      VARCHAR(150) NOT NULL,
                        city      VARCHAR(100) NOT NULL,
                        address   VARCHAR(255) NOT NULL,
                        capacity  INTEGER      NOT NULL,
                        active    BOOLEAN      NOT NULL DEFAULT TRUE,
                        CONSTRAINT uq_venues_code UNIQUE (code),
                        CONSTRAINT ck_venues_capacity CHECK (capacity > 0)
);

CREATE TABLE artists (
                         id          BIGSERIAL PRIMARY KEY,
                         stage_name  VARCHAR(150) NOT NULL,
                         country     VARCHAR(100),
                         genre       VARCHAR(100),
                         active      BOOLEAN      NOT NULL DEFAULT TRUE,
                         CONSTRAINT uq_artists_stage_name UNIQUE (stage_name)
);

CREATE TABLE users (
                       id        BIGSERIAL PRIMARY KEY,
                       username  VARCHAR(50)  NOT NULL,
                       email     VARCHAR(150) NOT NULL,
                       active    BOOLEAN      NOT NULL DEFAULT TRUE,
                       CONSTRAINT uq_users_username UNIQUE (username),
                       CONSTRAINT uq_users_email UNIQUE (email)
);

CREATE TABLE events (
                        id           BIGSERIAL PRIMARY KEY,
                        event_code   VARCHAR(50)   NOT NULL,
                        name         VARCHAR(200)  NOT NULL,
                        description  VARCHAR(1000),
                        category     VARCHAR(30)   NOT NULL,
                        status       VARCHAR(30)   NOT NULL,
                        event_date   TIMESTAMP     NOT NULL,
                        minimum_age  INTEGER       NOT NULL DEFAULT 0,
                        venue_id     BIGINT        NOT NULL,
                        CONSTRAINT uq_events_event_code UNIQUE (event_code),
                        CONSTRAINT fk_events_venue FOREIGN KEY (venue_id) REFERENCES venues (id),
                        CONSTRAINT ck_events_category CHECK (category IN
                                                             ('MUSIC','SPORTS','TECHNOLOGY','EDUCATION','CULTURE','ENTERTAINMENT')),
                        CONSTRAINT ck_events_status CHECK (status IN
                                                           ('DRAFT','PUBLISHED','SOLD_OUT','CANCELLED','FINISHED')),
                        CONSTRAINT ck_events_minimum_age CHECK (minimum_age >= 0)
);
CREATE INDEX idx_events_venue_id ON events (venue_id);
CREATE INDEX idx_events_status_date ON events (status, event_date);

CREATE TABLE event_artists (
                               event_id   BIGINT NOT NULL,
                               artist_id  BIGINT NOT NULL,
                               PRIMARY KEY (event_id, artist_id),
                               CONSTRAINT fk_event_artists_event  FOREIGN KEY (event_id)  REFERENCES events (id),
                               CONSTRAINT fk_event_artists_artist FOREIGN KEY (artist_id) REFERENCES artists (id)
);
CREATE INDEX idx_event_artists_artist_id ON event_artists (artist_id);

CREATE TABLE user_profiles (
                               id          BIGSERIAL PRIMARY KEY,
                               first_name  VARCHAR(100) NOT NULL,
                               last_name   VARCHAR(100) NOT NULL,
                               phone       VARCHAR(30),
                               city        VARCHAR(100),
                               birth_date  DATE,
                               user_id     BIGINT NOT NULL,
                               CONSTRAINT uq_user_profiles_user_id UNIQUE (user_id),
                               CONSTRAINT fk_user_profiles_user FOREIGN KEY (user_id) REFERENCES users (id)
);

CREATE TABLE tickets (
                         id             BIGSERIAL PRIMARY KEY,
                         ticket_code    VARCHAR(50)    NOT NULL,
                         type           VARCHAR(20)    NOT NULL,
                         price          NUMERIC(12, 2) NOT NULL,
                         status         VARCHAR(20)    NOT NULL,
                         purchase_date  TIMESTAMP      NOT NULL,
                         user_id        BIGINT         NOT NULL,
                         event_id       BIGINT         NOT NULL,
                         CONSTRAINT uq_tickets_ticket_code UNIQUE (ticket_code),
                         CONSTRAINT ck_tickets_price CHECK (price >= 0),
                         CONSTRAINT ck_tickets_type CHECK (type IN ('GENERAL','VIP','BACKSTAGE','STUDENT')),
                         CONSTRAINT ck_tickets_status CHECK (status IN ('RESERVED','PAID','CANCELLED','USED')),
                         CONSTRAINT fk_tickets_user  FOREIGN KEY (user_id)  REFERENCES users (id),
                         CONSTRAINT fk_tickets_event FOREIGN KEY (event_id) REFERENCES events (id)
);
CREATE INDEX idx_tickets_user_id ON tickets (user_id);
CREATE INDEX idx_tickets_event_status ON tickets (event_id, status);