 CREATE TABLE payments
(
    id             UUID                        NOT NULL,
    payment_system VARCHAR(255),
    payment_status VARCHAR(255),
    user_id        UUID,
    plan_id        UUID,
    amount         DECIMAL,
    external_id    VARCHAR(255),
    created_at     TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    updated_at     TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    CONSTRAINT pk_payments PRIMARY KEY (id)
);

ALTER TABLE payments
    ADD CONSTRAINT fk_payments_on_plan FOREIGN KEY (plan_id) REFERENCES plans (id);

ALTER TABLE payments
    ADD CONSTRAINT fk_payments_on_user FOREIGN KEY (user_id) REFERENCES users (id);

