-- Remove obsolete assignment fields, initialize resource counts, and adopt Entra naming.
ALTER TABLE assignments
    DROP COLUMN assigner_role_ref,
    DROP COLUMN assigner_ref,
    DROP COLUMN valid_from,
    DROP COLUMN valid_to,
    DROP COLUMN assigner_azure_ad_user_id;

UPDATE assignment_resources
SET number_of_resources_assigned = 0
WHERE number_of_resources_assigned IS NULL;

ALTER TABLE assignment_resources
    ALTER COLUMN number_of_resources_assigned SET DEFAULT 0,
    ALTER COLUMN number_of_resources_assigned SET NOT NULL;

ALTER TABLE assignments RENAME COLUMN azure_ad_group_id TO entra_group_id;
ALTER TABLE assignments RENAME COLUMN azure_ad_user_id TO entra_user_id;
