# --- !Ups

ALTER TABLE user
  DROP COLUMN IF EXISTS auth_token;


CREATE TABLE authentication (
  id             BIGINT AUTO_INCREMENT NOT NULL,
  user_id        BIGINT                NOT NULL,
  auth_type      VARCHAR(8),
  auth_token     VARCHAR(255)          NOT NULL,
  creation_date  TIMESTAMP             NOT NULL,
  last_used_date TIMESTAMP             NOT NULL,
  CONSTRAINT pk_authentication PRIMARY KEY (id),
  CONSTRAINT uq_authentication_auth_type_auth_token UNIQUE (auth_type, auth_token),
  FOREIGN KEY (user_id) REFERENCES user (id)
);

CREATE INDEX id_authentication_user_id
  ON authentication (user_id);

# --- !Downs

SET REFERENTIAL_INTEGRITY FALSE;

DROP TABLE IF EXISTS authentication;

SET REFERENTIAL_INTEGRITY TRUE;

