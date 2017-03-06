# --- !Ups

ALTER TABLE user
  ADD deleted BOOLEAN DEFAULT FALSE NOT NULL;

CREATE INDEX id_user_auth_token
  ON user (auth_token);


# --- !Downs

SET REFERENTIAL_INTEGRITY FALSE;

ALTER TABLE user
  DROP COLUMN deleted;

SET REFERENTIAL_INTEGRITY TRUE;


DROP INDEX IF EXISTS id_user_auth_token;

