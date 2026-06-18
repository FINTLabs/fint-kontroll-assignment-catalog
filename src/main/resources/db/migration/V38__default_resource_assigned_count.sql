update assignment_resources
set number_of_resources_assigned = 0
where number_of_resources_assigned is null;

alter table assignment_resources
    alter column number_of_resources_assigned set default 0;

alter table assignment_resources
    alter column number_of_resources_assigned set not null;
