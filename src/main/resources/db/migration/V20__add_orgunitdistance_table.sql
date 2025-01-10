create table orgunit_distance
(
    key             varchar(255) primary key,
    orgunitid       varchar(255),
    suborgunitid    varchar(255),
    distance        bigint
);