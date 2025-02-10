alter table assignment_resources ADD number_of_resources_assigned bigint;
alter table assignments ADD resource_consumer_org_unit_name varchar(255);
