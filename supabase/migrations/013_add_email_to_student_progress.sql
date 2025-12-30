
-- Add user_email column to student_progress table
alter table public.student_progress
add column if not exists user_email text;

-- Update existing records if possible (best effort, assuming owner_id linkage or manual fix later)
-- Note: We can't easily join auth.users here due to permissions, so we'll rely on app to populate it 
-- or manual backfill. For new records, it will be required.

-- Make it not null eventually, but for now allow null until backfilled
-- alter table public.student_progress alter column user_email set not null;
