alter table assignments ADD azure_ad_group_id uuid;
alter table assignments ADD azure_ad_user_id uuid;
alter table assignment_resources ADD azure_ad_group_id uuid;
alter table users ADD azure_ad_user_id uuid;