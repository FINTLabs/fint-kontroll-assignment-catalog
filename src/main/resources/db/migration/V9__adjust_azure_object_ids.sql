alter table assignment_resources DROP azure_ad_group_id;
alter table users DROP azure_ad_user_id;
alter table assignment_resources ADD identity_provider_group_object_id uuid;
alter table users ADD identity_provider_user_object_id uuid;