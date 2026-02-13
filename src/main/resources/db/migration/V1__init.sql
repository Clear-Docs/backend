CREATE TABLE limits
(
  id             UUID    NOT NULL,
  max_connectors INTEGER NOT NULL,
  created_at     TIMESTAMP WITHOUT TIME ZONE NOT NULL,
  updated_at     TIMESTAMP WITHOUT TIME ZONE NOT NULL,
  CONSTRAINT pk_limits PRIMARY KEY (id)
);

CREATE TABLE plans
(
  id          UUID         NOT NULL,
  code        VARCHAR(255) NOT NULL,
  title       VARCHAR(255) NOT NULL,
  price_rub   INTEGER      NOT NULL,
  period_days INTEGER      NOT NULL,
  created_at  TIMESTAMP WITHOUT TIME ZONE NOT NULL,
  updated_at  TIMESTAMP WITHOUT TIME ZONE NOT NULL,
  limit_id    UUID         NOT NULL,
  CONSTRAINT pk_plans PRIMARY KEY (id)
);

ALTER TABLE plans
  ADD CONSTRAINT uc_plans_limit UNIQUE (limit_id);

ALTER TABLE plans
  ADD CONSTRAINT fk_plans_on_limit FOREIGN KEY (limit_id) REFERENCES limits (id);