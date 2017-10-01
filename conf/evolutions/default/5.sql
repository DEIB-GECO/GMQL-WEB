# --- !Ups

ALTER TABLE user
  ADD user_type VARCHAR(8) DEFAULT 'UNKNOWN' NOT NULL AFTER username;


# --- !Downs

ALTER TABLE user_type
  DROP COLUMN type;