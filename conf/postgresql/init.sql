DROP TABLE IF EXISTS customer;
CREATE TABLE customer (
    id SERIAL PRIMARY KEY,
    name VARCHAR(32) NOT NULL,
    credit_limit INTEGER NOT NULL,
    balance INTEGER NOT NULL
);

DROP TABLE IF EXISTS transaction;
CREATE TABLE transaction (
    id SERIAL PRIMARY KEY,
    customer_id INTEGER NOT NULL,
    amount INTEGER NOT NULL,
    type CHAR(1) NOT NULL,
    description VARCHAR(10),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_customer_id_transaction
        FOREIGN KEY (customer_id) REFERENCES customer(id)
);

DO $$
BEGIN
	INSERT INTO customer (name, credit_limit, balance)
	VALUES
		('o barato sai caro', 1000 * 100, 0),
		('zan corp ltda', 800 * 100, 0),
		('les cruders', 10000 * 100, 0),
		('padaria joia de cocaia', 100000 * 100, 0),
		('kid mais', 5000 * 100, 0);
END;
$$;

CREATE OR REPLACE PROCEDURE update_balance_and_create_transaction(
    p_customer_id INTEGER,
    p_amount INTEGER,
    p_type CHAR,
    p_description VARCHAR
)
LANGUAGE plpgsql
AS $$
BEGIN
    IF p_type = 'd' THEN
        UPDATE customer SET balance = balance - p_amount WHERE id = p_customer_id;
    ELSIF p_type = 'c' THEN
        UPDATE customer SET balance = balance + p_amount WHERE id = p_customer_id;
    END IF;

    INSERT INTO transaction (customer_id, amount, type, description)
    VALUES (p_customer_id, p_amount, p_type, p_description);
END;
$$;