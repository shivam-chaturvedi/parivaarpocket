-- Log every student interaction for analytics/record keeping
create table if not exists public.student_activity_logs (
    id uuid primary key default gen_random_uuid(),
    user_email text not null,
    activity_type text not null,
    activity_data jsonb not null default '{}'::jsonb,
    created_at timestamptz not null default now()
);

alter table public.student_activity_logs enable row level security;
drop policy if exists "Student activity logs are readable" on public.student_activity_logs;
drop policy if exists "Student activity logs are writable" on public.student_activity_logs;
create policy "Student activity logs are readable" on public.student_activity_logs
    for select using (auth.role() in ('anon', 'authenticated'));
create policy "Student activity logs are writable" on public.student_activity_logs
    for insert with check (auth.role() in ('anon', 'authenticated'));

create index if not exists student_activity_logs_user_email_idx on public.student_activity_logs (user_email);
