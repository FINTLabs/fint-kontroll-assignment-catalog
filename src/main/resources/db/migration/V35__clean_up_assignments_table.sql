ALTER TABLE assignments
    DROP COLUMN assigner_role_ref,
    DROP COLUMN assigner_ref,
    DROP COLUMN valid_from,
    DROP COLUMN valid_to,
    DROP COLUMN assigner_azure_ad_user_id;