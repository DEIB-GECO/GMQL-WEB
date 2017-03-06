# --- !Ups

CREATE TABLE query (
  id             BIGINT AUTO_INCREMENT NOT NULL,
  user_id        BIGINT                NOT NULL,
  name           VARCHAR(256)          NOT NULL,
  text           TEXT                  NOT NULL,
  creation_date  TIMESTAMP             NOT NULL,
  last_used_date TIMESTAMP             NOT NULL,
  deleted        BOOLEAN               NOT NULL,
  CONSTRAINT pk_query PRIMARY KEY (id),
  CONSTRAINT uq_query_user_id_name UNIQUE (user_id, name),
  FOREIGN KEY (user_id) REFERENCES user (id)
);

CREATE INDEX id_query_user_id
  ON query (user_id);


# --- !Downs

SET REFERENTIAL_INTEGRITY FALSE;

DROP TABLE IF EXISTS query;

SET REFERENTIAL_INTEGRITY TRUE;

