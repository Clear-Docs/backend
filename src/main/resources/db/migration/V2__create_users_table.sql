CREATE TABLE users
(
  id           UUID                        NOT NULL,
  firebase_uid VARCHAR(255) UNIQUE         NOT NULL,
  email        VARCHAR(255)                NOT NULL,
  name         VARCHAR(255),
  created_at   TIMESTAMP WITHOUT TIME ZONE NOT NULL,
  updated_at   TIMESTAMP WITHOUT TIME ZONE NOT NULL,
  plan_id      UUID                        NOT NULL,
  CONSTRAINT pk_users PRIMARY KEY (id)
);

ALTER TABLE users
  ADD CONSTRAINT fk_users_on_plan FOREIGN KEY (plan_id) REFERENCES plans (id);