CREATE TABLE customers
(
    customer_id   SERIAL PRIMARY KEY,
    first_name    VARCHAR(255),
    last_name     VARCHAR(255),
    phone         VARCHAR(50),
    email         VARCHAR(255),
    reward_points INT,
    join_date     DATE,
    CONSTRAINT customers_email_unique UNIQUE (email)
);
CREATE INDEX idx_customers_email ON customers (email);

CREATE TABLE employees
(
    employee_id   SERIAL PRIMARY KEY,
    first_name    VARCHAR(255),
    last_name     VARCHAR(255),
    role          VARCHAR(50),
    start_date    DATE,
    email         VARCHAR(255) UNIQUE,
    password_hash VARCHAR(255),
    is_active     BOOLEAN
);

CREATE TABLE menu_items
(
    menu_item_id SERIAL PRIMARY KEY,
    name         VARCHAR(255),
    category     VARCHAR(255),
    size         VARCHAR(50) NULL,
    base_price   DECIMAL(10, 2),
    is_available BOOLEAN,
    is_hot       BOOLEAN NOT NULL DEFAULT false
);

CREATE TABLE menu_item_images
(
    name  VARCHAR(255) PRIMARY KEY,
    image BYTEA NOT NULL
);

CREATE TABLE inventory
(
    inventory_id      SERIAL PRIMARY KEY,
    item_name         VARCHAR(255),
    category          VARCHAR(255),
    current_quantity  DECIMAL(10, 2),
    unit              VARCHAR(50),
    reorder_threshold DECIMAL(10, 2),
    supplier_cost     DECIMAL(10, 2),
    last_restock_date DATE
);

CREATE TABLE menu_item_contents
(
    menu_item_id INT REFERENCES menu_items (menu_item_id),
    inventory_id INT REFERENCES inventory (inventory_id),
    quantity     DECIMAL(10, 2),
    PRIMARY KEY (menu_item_id, inventory_id)
);

CREATE TABLE orders
(
    order_id       SERIAL PRIMARY KEY,
    employee_id    INT REFERENCES employees (employee_id),
    customer_id    INT NULL REFERENCES customers (customer_id),
    timestamp      TIMESTAMP,
    total_price    DECIMAL(10, 2),
    tip_amount     DECIMAL(10, 2) DEFAULT 0.00,
    payment_method VARCHAR(50)
);

CREATE TABLE order_items
(
    order_item_id  SERIAL PRIMARY KEY,
    order_id       INT REFERENCES orders (order_id),
    menu_item_id   INT REFERENCES menu_items (menu_item_id),
    parent_item_id INT         NULL REFERENCES order_items (order_item_id),
    quantity       INT,
    ice_level      VARCHAR(50) NULL,
    sugar_level    VARCHAR(50) NULL,
    item_price     DECIMAL(10, 2)
);

CREATE TABLE restock_orders
(
    restock_id       SERIAL PRIMARY KEY,
    employee_id      INT REFERENCES employees (employee_id),
    inventory_id     INT REFERENCES inventory (inventory_id),
    quantity_ordered DECIMAL(10, 2),
    order_date       TIMESTAMP,
    status           VARCHAR(50)
);

CREATE TABLE z_report_run
(
    business_date DATE PRIMARY KEY,
    generated_at  TIMESTAMP NOT NULL DEFAULT now(),
    generated_by  TEXT NOT NULL
);

CREATE TABLE z_report_signatures
(
    business_date DATE NOT NULL REFERENCES z_report_run (business_date) ON DELETE CASCADE,
    employee_id   INT NOT NULL REFERENCES employees (employee_id),
    signed_at     TIMESTAMP NOT NULL DEFAULT now(),
    PRIMARY KEY (business_date, employee_id)
);
