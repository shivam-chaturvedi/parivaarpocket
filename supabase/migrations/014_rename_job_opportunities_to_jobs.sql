-- Rename the legacy job_opportunities table to jobs and add API metadata.
alter table if exists public.job_applications
    drop constraint if exists job_applications_job_id_fkey;

drop policy if exists "Jobs are readable by the app" on public.job_opportunities;

alter table if exists public.job_opportunities
    rename to jobs;

alter table if exists public.jobs enable row level security;
drop policy if exists "Jobs are readable by the app" on public.jobs;
create policy "Jobs are readable by the app" on public.jobs
    for select using (auth.role() in ('anon', 'authenticated'));

alter table if exists public.jobs
    add column if not exists external_job_id text;
create unique index if not exists jobs_external_job_id_idx on public.jobs (external_job_id);

alter table if exists public.job_applications
    add constraint job_applications_job_id_fkey
        foreign key (job_id) references public.jobs(id);
