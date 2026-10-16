create table device_entra_memberships
(
    id                        BIGSERIAL PRIMARY KEY    NOT NULL,
    device_entra_id           UUID                     NOT NULL,
    resource_entra_id         UUID                     NOT NULL,
    created_date              TIMESTAMP WITH TIME ZONE NOT NULL,
    membership_status           VARCHAR(255)             NOT NULL,
    entra_status            VARCHAR(255)             NOT NULL,
    sent_to_entra_at          TIMESTAMP WITH TIME ZONE,
    deletion_sent_to_entra_at TIMESTAMP WITH TIME ZONE,
    CONSTRAINT uk_device_resource UNIQUE (device_entra_id, resource_entra_id)
);

create table flattened_device_assignments
(
    id                                 BIGSERIAL PRIMARY KEY    NOT NULL,
    assignment_id                      BIGINT                   NOT NULL,
    device_ref                         BIGINT                   NOT NULL,
    created_date                       TIMESTAMP WITH TIME ZONE NOT NULL,
    modified_date                      TIMESTAMP WITH TIME ZONE NOT NULL,
    termination_date                   TIMESTAMP WITH TIME ZONE,
    termination_reason                 VARCHAR(255),
    resource_ref                       BIGINT                   NOT NULL,
    identity_provider_device_object_id UUID                     NOT NULL,
    identity_provider_group_object_id  UUID                     NOT NULL,
    assignment_via_group_ref           BIGINT,
    assignment_creation_date           TIMESTAMP WITH TIME ZONE,
    resource_consumer_org_unit_id      VARCHAR(255),
    device_entra_membership_id                      BIGINT,
    CONSTRAINT fk_device_assignments_device FOREIGN KEY (device_ref) REFERENCES devices (id), -- should we have the foreign key here?
    CONSTRAINT fk_device_assignments_assignment FOREIGN KEY (assignment_id) REFERENCES assignments (id),
    CONSTRAINT fk_device_assignments_resource FOREIGN KEY (resource_ref) REFERENCES assignment_resources (id),
    CONSTRAINT fk_device_assignments_group FOREIGN KEY (assignment_via_group_ref) REFERENCES device_groups (id),
    CONSTRAINT fk_device_assignments_device_entra_membership FOREIGN KEY (device_entra_membership_id) REFERENCES device_entra_memberships (id)
);

CREATE INDEX idx_device_assignments_assignment_id ON flattened_device_assignments (assignment_id);
CREATE INDEX idx_flattened_assignments_devobj_grpobj_termdate on flattened_device_assignments (identity_provider_device_object_id,
                                                                                               identity_provider_group_object_id,
                                                                                               termination_date);
CREATE INDEX idx_flattened_assignments_resource_ref on flattened_device_assignments (resource_ref);
CREATE INDEX idx_flattened_assignments_device_ref on flattened_device_assignments (device_ref);
CREATE INDEX idx_flattened_assignments_device_group_ref on flattened_device_assignments (assignment_via_group_ref);

alter table assignments
    ADD device_group_ref BIGINT;
alter table assignments
    ADD CONSTRAINT fk_assignments_device_group FOREIGN KEY (device_group_ref) REFERENCES device_groups (id);