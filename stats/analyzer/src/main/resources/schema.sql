CREATE TABLE user_actions (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id BIGINT NOT NULL,
    event_id BIGINT NOT NULL,
    rating DOUBLE PRECISION NOT NULL,
    timestamp TIMESTAMP NOT NULL,

    UNIQUE (user_id, event_id)
);

CREATE TABLE event_similarity (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    event_a BIGINT NOT NULL,
    event_b BIGINT NOT NULL,
    score DOUBLE PRECISION NOT NULL,
    timestamp TIMESTAMP NOT NULL,

    UNIQUE(event_a, event_b)
);