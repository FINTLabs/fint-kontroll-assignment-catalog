DELETE FROM flattened_user_assignments
WHERE identity_provider_user_object_id IS NULL
   OR identity_provider_group_object_id IS NULL
   OR user_entra_membership_id IS NULL;

ALTER TABLE flattened_user_assignments
    ALTER COLUMN identity_provider_user_object_id SET NOT NULL,
    ALTER COLUMN identity_provider_group_object_id SET NOT NULL,
    ALTER COLUMN user_entra_membership_id SET NOT NULL;

UPDATE users
SET status = 'ACTIVE'
WHERE status IS NULL;

ALTER TABLE users
    ALTER COLUMN status SET NOT NULL;
