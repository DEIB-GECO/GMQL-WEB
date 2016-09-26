# --- !Ups

create table user (
  id                        bigint auto_increment not null,
  auth_token                varchar(255),
  username                  varchar(256) not null,
  email_address             varchar(256) not null,
  sha_password              varbinary(64) not null,
  first_name                varchar(256) not null,
  last_name                 varchar(256) not null,
  creation_date             timestamp not null,
  last_used_date            timestamp not null,
  constraint uq_user_username unique (username),
  constraint uq_user_email_address unique (email_address),
  constraint pk_user primary key (id))
;




# --- !Downs

SET REFERENTIAL_INTEGRITY FALSE;

drop table if exists user;

SET REFERENTIAL_INTEGRITY TRUE;

