create table application_resource_location
(
    id                    int8 primary key,
    applicationresourceid int8,
    resourceid            varchar(255),
    orgunitid             varchar(255),
    orgunitname           varchar(255),
    resourcelimit         bigint
);