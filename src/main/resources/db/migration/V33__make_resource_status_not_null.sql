UPDATE assignment_resources
SET status = 'ACTIVE'
WHERE status IS NULL;

ALTER TABLE assignment_resources
    ALTER COLUMN status SET NOT NULL;
