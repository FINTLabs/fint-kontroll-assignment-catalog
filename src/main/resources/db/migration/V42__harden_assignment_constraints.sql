ALTER TABLE assignments
    ADD CONSTRAINT ck_assignments_exactly_one_target
        CHECK (num_nonnulls(user_ref, role_ref, device_group_ref) = 1) NOT VALID;

ALTER TABLE users
    ALTER COLUMN identity_provider_user_object_id SET NOT NULL;

CREATE UNIQUE INDEX IF NOT EXISTS uk_assignments_active_user_resource
    ON assignments (resource_ref, user_ref)
    WHERE assignment_removed_date IS NULL
      AND user_ref IS NOT NULL;

CREATE UNIQUE INDEX IF NOT EXISTS uk_assignments_active_role_resource
    ON assignments (resource_ref, role_ref)
    WHERE assignment_removed_date IS NULL
      AND role_ref IS NOT NULL;
