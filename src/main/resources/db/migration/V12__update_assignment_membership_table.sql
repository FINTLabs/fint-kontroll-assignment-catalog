create table assignment_memberships (

      id VARCHAR(255),
      role_id int8,
      member_id int8,
      identity_provider_user_object_id uuid,
      primary key (id));