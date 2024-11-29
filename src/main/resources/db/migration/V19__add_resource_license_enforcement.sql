alter table assignment_resources ADD license_enforcement varchar(255);
alter table assignments ADD license_enforcement varchar(255);
alter table flattened_assignments ADD license_enforcement varchar(255);
alter table assignments ADD resource_consumer_org_unit_id varchar(255);
alter table flattened_assignments ADD resource_consumer_org_unit_id varchar(255);