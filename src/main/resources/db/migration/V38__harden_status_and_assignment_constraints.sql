-- Backfill legacy status values and enforce assignment invariants.
UPDATE assignment_roles
SET role_status = 'ACTIVE'
WHERE role_status IS NULL;

ALTER TABLE assignment_roles
    ALTER COLUMN role_status SET NOT NULL;

UPDATE users
SET status = 'ACTIVE'
WHERE status IS NULL;

ALTER TABLE users
    ALTER COLUMN status SET NOT NULL,
    ALTER COLUMN identity_provider_user_object_id SET NOT NULL;

ALTER TABLE assignments
    ADD CONSTRAINT ck_assignments_exactly_one_target
        CHECK (num_nonnulls(user_ref, role_ref, device_group_ref) = 1) NOT VALID;

CREATE UNIQUE INDEX IF NOT EXISTS uk_assignments_active_user_resource
    ON assignments (resource_ref, user_ref)
    WHERE assignment_removed_date IS NULL
      AND user_ref IS NOT NULL;

CREATE UNIQUE INDEX IF NOT EXISTS uk_assignments_active_role_resource
    ON assignments (resource_ref, role_ref)
    WHERE assignment_removed_date IS NULL
      AND role_ref IS NOT NULL;
