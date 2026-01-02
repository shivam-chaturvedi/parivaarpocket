-- Drop legacy jobs table and recreate with the new schema required by the Work module
alter table if exists public.job_applications
    drop constraint if exists job_applications_job_id_fkey;

drop table if exists public.jobs;

create table if not exists public.jobs (
    id text primary key,
    title text not null,
    company_name text,
    location text,
    locality text,
    job_link text,
    pub_date_ts_milli bigint,
    formatted_relative_time text,
    salary_min numeric,
    salary_max numeric,
    salary_type text,
    created_at timestamptz not null default now()
);

alter table public.jobs enable row level security;
drop policy if exists "Jobs are readable by the app" on public.jobs;
create policy "Jobs are readable by the app" on public.jobs
    for select using (auth.role() in ('anon', 'authenticated'));

alter table public.job_applications
    add constraint job_applications_job_id_fkey
        foreign key (job_id) references public.jobs(id);
