alter table assignment_roles ADD created_date timestamp with time zone;
alter table assignment_roles ADD modified_date timestamp with time zone;
alter table assignment_roles ADD created_by varchar(255);
alter table assignment_roles ADD modified_by varchar(255);

alter table assignment_resources ADD created_date timestamp with time zone;
alter table assignment_resources ADD modified_date timestamp with time zone;
alter table assignment_resources ADD created_by varchar(255);
alter table assignment_resources ADD modified_by varchar(255);

alter table assignment_memberships ADD created_date timestamp with time zone;
alter table assignment_memberships ADD modified_date timestamp with time zone;
alter table assignment_memberships ADD created_by varchar(255);
alter table assignment_memberships ADD modified_by varchar(255);

alter table assignments ADD created_date timestamp with time zone;
alter table assignments ADD modified_date timestamp with time zone;
alter table assignments ADD created_by varchar(255);
alter table assignments ADD modified_by varchar(255);

alter table flattened_assignments ADD created_date timestamp with time zone;
alter table flattened_assignments ADD modified_date timestamp with time zone;
alter table flattened_assignments ADD created_by varchar(255);
alter table flattened_assignments ADD modified_by varchar(255);


