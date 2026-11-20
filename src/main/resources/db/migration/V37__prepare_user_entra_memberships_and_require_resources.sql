-- Prepare the canonical membership table and require valid resources.
-- Run db/manual/migrate_flattened_assignments_to_user_entra_memberships.sql
-- after Flyway reaches V39 and before starting the new application version.
-- Remove resources without Entra groups and their dependent projections.
DELETE FROM flattened_device_assignments
WHERE resource_ref IN (
    SELECT id
    FROM assignment_resources
    WHERE identity_provider_group_object_id IS NULL
);

DELETE FROM flattened_assignments
WHERE resource_ref IN (
    SELECT id
    FROM assignment_resources
    WHERE identity_provider_group_object_id IS NULL
);

DELETE FROM assignments
WHERE resource_ref IN (
    SELECT id
    FROM assignment_resources
    WHERE identity_provider_group_object_id IS NULL
);

DELETE FROM assignment_resources
WHERE identity_provider_group_object_id IS NULL;

ALTER TABLE flattened_assignments
    RENAME TO flattened_user_assignments;

ALTER TABLE flattened_user_assignments
    DROP COLUMN assigner_ref;

CREATE TABLE user_entra_memberships
(
    id                        BIGSERIAL PRIMARY KEY,
    user_entra_id             UUID                     NOT NULL,
    resource_entra_id         UUID                     NOT NULL,
    created_date              TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    membership_status         VARCHAR(255)             NOT NULL,
    entra_status              VARCHAR(255)             NOT NULL,
    sent_to_entra_at          TIMESTAMP WITH TIME ZONE,
    deletion_sent_to_entra_at TIMESTAMP WITH TIME ZONE,
    CONSTRAINT uk_user_resource UNIQUE (user_entra_id, resource_entra_id)
);

ALTER TABLE flattened_user_assignments
    ADD COLUMN user_entra_membership_id BIGINT;

ALTER TABLE flattened_user_assignments
    ADD CONSTRAINT fk_flattened_user_assignments_user_entra_membership
        FOREIGN KEY (user_entra_membership_id)
            REFERENCES user_entra_memberships (id);

alter table assignment_resources
    alter column identity_provider_group_object_id set not null;

-- Backfill the assignment group ID from its resource before enforcing the invariant.
UPDATE assignments assignment
SET entra_group_id = resource.identity_provider_group_object_id
FROM assignment_resources resource
WHERE assignment.resource_ref = resource.id
  AND assignment.entra_group_id IS NULL;

UPDATE flattened_user_assignments flattened_assignment
SET resource_ref = COALESCE(flattened_assignment.resource_ref, assignment.resource_ref),
    identity_provider_group_object_id = COALESCE(
        flattened_assignment.identity_provider_group_object_id,
        assignment.entra_group_id
    )
FROM assignments assignment
WHERE flattened_assignment.assignment_id = assignment.id
  AND (flattened_assignment.resource_ref IS NULL
       OR flattened_assignment.identity_provider_group_object_id IS NULL);

DELETE FROM flattened_device_assignments flattened_assignment
USING assignments assignment
WHERE flattened_assignment.assignment_id = assignment.id
  AND (assignment.resource_ref IS NULL OR assignment.entra_group_id IS NULL);

DELETE FROM flattened_user_assignments flattened_assignment
USING assignments assignment
WHERE flattened_assignment.assignment_id = assignment.id
  AND (assignment.resource_ref IS NULL OR assignment.entra_group_id IS NULL);

DELETE FROM flattened_user_assignments
WHERE resource_ref IS NULL;

DELETE FROM assignments
WHERE resource_ref IS NULL
   OR entra_group_id IS NULL;

ALTER TABLE assignments
    ALTER COLUMN resource_ref SET NOT NULL,
    ALTER COLUMN entra_group_id SET NOT NULL;

ALTER TABLE flattened_user_assignments
    ALTER COLUMN resource_ref SET NOT NULL;
