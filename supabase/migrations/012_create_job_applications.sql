-- Create job_applications table to track student applications
create table if not exists public.job_applications (
    id uuid primary key default gen_random_uuid(),
    job_id uuid not null references public.job_opportunities(id),
    user_email text not null,
    status text not null default 'Pending', -- Pending, Approved, Rejected
    applied_at timestamptz not null default now()
);

-- RLS Policies
alter table public.job_applications enable row level security;

-- Allow public access (as per recent permissive policy change request)
drop policy if exists "Job applications are readable" on public.job_applications;
drop policy if exists "Job applications are writable" on public.job_applications;

create policy "Public access job_applications" on public.job_applications
    for all using (true) with check (true);
