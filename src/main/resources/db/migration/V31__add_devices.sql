CREATE TABLE devices
(
    id                        BIGINT PRIMARY KEY,
    source_id                 VARCHAR(255) NOT NULL UNIQUE,
    serial_number             VARCHAR(255) NOT NULL,
    data_object_id            UUID NOT NULL,
    name                      VARCHAR(255),
    is_private_property       BOOLEAN,
    is_shared                 BOOLEAN,
    status                    VARCHAR(50)  NOT NULL,
    status_changed            TIMESTAMP,
    created_date              TIMESTAMP    NOT NULL,
    modified_date             TIMESTAMP    NOT NULL,
    device_type               VARCHAR(100) NOT NULL,
    platform                  VARCHAR(100) NOT NULL,
    administrator_org_unit_id VARCHAR(255),
    owner_org_unit_id         VARCHAR(255)
);

CREATE INDEX ix_device_serial_number ON devices (serial_number);
CREATE INDEX ix_device_status ON devices (status);

-- DEVICE_GROUPS
CREATE TABLE device_groups
(
    id            BIGINT PRIMARY KEY,
    source_id     VARCHAR(255) NOT NULL,
    name          VARCHAR(255) NOT NULL,
    org_unit_id   VARCHAR(255),
    created_date  TIMESTAMP    NOT NULL,
    modified_date TIMESTAMP    NOT NULL,
    platform      VARCHAR(100) NOT NULL,
    device_type   VARCHAR(100) NOT NULL,
    no_of_members BIGINT NOT NULL
);

CREATE INDEX ux_device_groups_source_id ON device_groups (source_id);

-- DEVICE_GROUP_MEMBERSHIP
CREATE TABLE device_group_memberships
(
    device_group_id           BIGINT NOT NULL,
    device_id                 BIGINT NOT NULL,
    membership_status         VARCHAR(50),
    membership_status_changed TIMESTAMP,
    primary key (device_group_id, device_id),
    CONSTRAINT fk_dgm_device FOREIGN KEY (device_id) REFERENCES devices (id),
    CONSTRAINT fk_dgm_device_group FOREIGN KEY (device_group_id) REFERENCES device_groups (id)
);

CREATE INDEX ix_dgm_device_id ON device_group_memberships (device_id);
CREATE INDEX ix_dgm_device_group_id ON device_group_memberships (device_group_id);