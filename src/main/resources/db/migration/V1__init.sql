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

INSERT INTO limits
VALUES ('2a819753-5013-4723-b1b0-7adfcb84d476', 1, now(), now()),
       ('2a819753-5013-4723-b1b0-7adfcb84d477', 10, now(), now());

INSERT INTO plans
VALUES ('2a819753-5013-4723-b1b0-7adfcb84d466', 'FREE', 'Бесплатный', 0, 0, now(), now(), '2a819753-5013-4723-b1b0-7adfcb84d476'),
       ('2a819753-5013-4723-b1b0-7adfcb84d467', 'MONTH', 'Подписка на месяц', 990, 30, now(), now(), '2a819753-5013-4723-b1b0-7adfcb84d477');