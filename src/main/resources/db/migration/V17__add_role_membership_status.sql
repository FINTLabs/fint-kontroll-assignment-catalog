alter table assignment_roles ADD role_status varchar(255);
alter table assignment_roles ADD role_status_changed timestamp;

alter table assignment_memberships ADD member_status varchar(255);
alter table assignment_memberships ADD member_status_changed timestamp;
