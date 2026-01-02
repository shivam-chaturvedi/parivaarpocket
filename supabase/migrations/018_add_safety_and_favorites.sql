-- Migration to add safety guidance and contact info to jobs, and create job_favorites table
-- Also ensuring all filter columns are present to avoid PGRST204
alter table public.jobs 
add column if not exists category text,
add column if not exists required_skills text[],
add column if not exists working_hours text,
add column if not exists safety_guidance text,
add column if not exists contact_info text;

-- Ensure RLS on jobs is also permissive
drop policy if exists "Jobs are readable by the app" on public.jobs;
create policy "Jobs are readable by the app" on public.jobs
    for all using (true) with check (true);

create table if not exists public.job_favorites (
    id uuid primary key default gen_random_uuid(),
    user_email text not null,
    job_id text not null references public.jobs(id) on delete cascade,
    created_at timestamptz not null default now(),
    unique(user_email, job_id)
);

alter table public.job_favorites enable row level security;

-- Allow all users to manage everything for now as requested
drop policy if exists "Users can manage their own favorites" on public.job_favorites;
create policy "Public access job_favorites" on public.job_favorites
    for all using (true) with check (true);

comment on column public.jobs.safety_guidance is 'Extracted safety advice for the job';
comment on column public.jobs.contact_info is 'How to contact the employer (extracted)';
comment on column public.jobs.category is 'Categorization: Tutoring, Delivery, Retail, Internship, General';
comment on column public.jobs.required_skills is 'Array of skills extracted from title/description';
comment on column public.jobs.working_hours is 'Working hour details (e.g., Full-time, Part-time)';
