CREATE TABLE policy_products (
    policy_id UUID NOT NULL,
    product_name VARCHAR(255) NOT NULL,
    FOREIGN KEY (policy_id) REFERENCES policies(id)
);

CREATE INDEX idx_policy_products_policy_id ON policy_products(policy_id);
