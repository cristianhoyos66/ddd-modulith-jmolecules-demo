-- Customers module
CREATE TABLE customer (
    id UUID NOT NULL PRIMARY KEY,
    name VARCHAR(255),
    address VARCHAR(255)
);

-- Catalog module
CREATE TABLE product (
    id UUID NOT NULL PRIMARY KEY,
    name VARCHAR(255),
    amount NUMERIC(38, 2),
    currency VARCHAR(3)
);

-- Inventory module
CREATE TABLE inventory_item (
    id UUID NOT NULL PRIMARY KEY,
    product UUID,
    stock BIGINT NOT NULL
);

-- Orders module
CREATE TABLE orders (
    id UUID NOT NULL PRIMARY KEY,
    customer UUID,
    status SMALLINT
);

CREATE TABLE line_item (
    id UUID NOT NULL PRIMARY KEY,
    product UUID,
    quantity BIGINT NOT NULL,
    line_items_id UUID,
    CONSTRAINT fk_line_item_orders FOREIGN KEY (line_items_id) REFERENCES orders (id)
);

-- Spring Modulith event publication registry (JPA variant)
CREATE TABLE event_publication (
    id UUID NOT NULL PRIMARY KEY,
    listener_id VARCHAR(512) NOT NULL,
    event_type VARCHAR(512) NOT NULL,
    serialized_event VARCHAR(4000) NOT NULL,
    publication_date TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    completion_date TIMESTAMP(6) WITH TIME ZONE,
    last_resubmission_date TIMESTAMP(6) WITH TIME ZONE,
    completion_attempts INTEGER NOT NULL DEFAULT 0,
    status VARCHAR(32)
);

CREATE INDEX event_publication_by_completion_date_idx ON event_publication (completion_date);
