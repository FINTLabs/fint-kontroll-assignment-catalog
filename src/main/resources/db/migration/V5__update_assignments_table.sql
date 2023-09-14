alter table assignments ADD user_first_name varchar(255);
alter table assignments ADD user_last_name  varchar(255);
alter table assignments ADD user_user_type  varchar(255);
alter table assignments ADD resource_name varchar(255);
alter table users ADD user_type varchar(255);
/* TODO later:
alter table user ADD username  varchar(255);
alter table assignments ADD user_username  varchar(255);
alter table assignments ADD assigner_first_name varchar(255);
alter table assignments ADD assigner_last_name varchar(255);
alter table assignments ADD assigner_username varchar(255);
alter table assignments ADD assigner_rolename varchar(255);
alter table assigment_roles ADD role_name varchar(255);
alter table assigment_roles ADD role_type varchar(255);
alter table assignments ADD role_name varchar(255);
alter table assignments ADD role_roletype  varchar(255);*/