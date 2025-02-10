ALTER TABLE flattened_assignments DISABLE TRIGGER ALL;

INSERT INTO flattened_assignments (id, user_ref, resource_ref, assignment_creation_date, assignment_termination_date)
SELECT generate_series(1, 100000),
       generate_series(1, 100000),
       generate_series(1, 100000),
       NOW(),
       NULL;

ALTER TABLE flattened_assignments ENABLE TRIGGER ALL;


