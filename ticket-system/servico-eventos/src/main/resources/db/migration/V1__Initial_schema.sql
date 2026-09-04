-- servico-eventos database schema

CREATE TABLE IF NOT EXISTS events (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(200) NOT NULL,
    description VARCHAR(1000),
    date TIMESTAMP NOT NULL,
    location VARCHAR(200) NOT NULL,
    CONSTRAINT check_event_name_length CHECK (char_length(name) >= 3),
    CONSTRAINT check_location_length CHECK (char_length(location) >= 3)
);

CREATE TABLE IF NOT EXISTS ticket_types (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    price DECIMAL(19, 2) NOT NULL,
    total_quantity INT NOT NULL,
    available_quantity INT NOT NULL,
    event_id BIGINT NOT NULL,
    CONSTRAINT fk_ticket_type_event FOREIGN KEY (event_id) REFERENCES events(id) ON DELETE CASCADE,
    CONSTRAINT check_price_positive CHECK (price >= 0),
    CONSTRAINT check_total_quantity_positive CHECK (total_quantity >= 0),
    CONSTRAINT check_available_quantity_positive CHECK (available_quantity >= 0),
    CONSTRAINT check_available_lte_total CHECK (available_quantity <= total_quantity)
);

-- Create indexes for faster queries
CREATE INDEX idx_events_date ON events(date);
CREATE INDEX idx_events_location ON events(location);
CREATE INDEX idx_ticket_types_event_id ON ticket_types(event_id);
