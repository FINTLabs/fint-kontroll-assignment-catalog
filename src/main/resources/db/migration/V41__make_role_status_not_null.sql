UPDATE assignment_roles
SET role_status = 'ACTIVE'
WHERE role_status IS NULL;

ALTER TABLE assignment_roles
    ALTER COLUMN role_status SET NOT NULL;
