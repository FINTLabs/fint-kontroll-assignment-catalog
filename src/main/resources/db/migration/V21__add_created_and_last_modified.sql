alter table users ADD created_date timestamp with time zone;
alter table users ADD modified_date timestamp with time zone;
alter table users ADD created_by varchar(255);
alter table users ADD modified_by varchar(255);
