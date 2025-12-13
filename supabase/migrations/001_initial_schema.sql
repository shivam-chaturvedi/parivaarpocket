-- Enable UUID helper used for primary keys
create extension if not exists "pgcrypto";

-- Lessons table mirrors the learning module metadata
create table if not exists public.lessons (
    id uuid primary key default gen_random_uuid(),
    title text not null,
    difficulty text not null default 'Beginner',
    description text,
    progress_percent int not null default 0,
    quizzes_completed int not null default 0,
    quizzes_total int not null default 0,
    created_at timestamptz not null default now()
);
alter table public.lessons enable row level security;
drop policy if exists "Lessons are readable by the app" on public.lessons;
drop policy if exists "Lesssons are readable by the app" on public.lessons;
create policy "Lesssons are readable by the app" on public.lessons
    for select using (auth.role() in ('anon', 'authenticated'));

-- Quiz results table that backs the leaderboard + metrics
create table if not exists public.quiz_results (
    id uuid primary key default gen_random_uuid(),
    title text not null,
    score int not null default 0,
    difficulty text not null default 'Beginner',
    coins_awarded int not null default 0,
    created_at timestamptz not null default now()
);
alter table public.quiz_results enable row level security;
drop policy if exists "Quiz results are readable by the app" on public.quiz_results;
create policy "Quiz results are readable by the app" on public.quiz_results
    for select using (auth.role() in ('anon', 'authenticated'));

-- Job opportunities table that the work module consumes
create table if not exists public.job_opportunities (
    id uuid primary key default gen_random_uuid(),
    title text not null,
    company text not null,
    location text not null,
    category text,
    hours text,
    pay_range text,
    required_skills text[],
    safety_notes text,
    contact text,
    suitability_score int not null default 0,
    created_at timestamptz not null default now()
);
alter table public.job_opportunities enable row level security;
drop policy if exists "Jobs are readable by the app" on public.job_opportunities;
create policy "Jobs are readable by the app" on public.job_opportunities
    for select using (auth.role() in ('anon', 'authenticated'));

-- Notifications that appear in the sidebar
create table if not exists public.notifications (
    id uuid primary key default gen_random_uuid(),
    title text not null,
    description text,
    severity text not null default 'info',
    notify_date date not null default current_date,
    created_at timestamptz not null default now()
);
alter table public.notifications enable row level security;
drop policy if exists "Notifications are readable by the app" on public.notifications;
create policy "Notifications are readable by the app" on public.notifications
    for select using (auth.role() in ('anon', 'authenticated'));

-- Wallet entries for budgeting
create table if not exists public.wallet_entries (
    id uuid primary key default gen_random_uuid(),
    owner_id uuid references auth.users(id),
    owner_email text not null,
    entry_type text not null default 'expense',
    category text,
    amount numeric not null default 0,
    note text,
    entry_date date not null default current_date,
    created_at timestamptz not null default now()
);
alter table public.wallet_entries enable row level security;
drop policy if exists "Wallet entries are readable by the app" on public.wallet_entries;
create policy "Wallet entries are readable by the app" on public.wallet_entries
    for select using (auth.role() in ('anon', 'authenticated'));

-- Student progress metrics consumed by educator dashboard
create table if not exists public.student_progress (
    id uuid primary key default gen_random_uuid(),
    student_name text not null,
    modules_completed int not null default 0,
    total_modules int not null default 1,
    quizzes_taken int not null default 0,
    average_score numeric(5,2) not null default 0,
    wallet_health_score numeric(5,2) not null default 0,
    parivaar_points int not null default 0,
    employment_applications int not null default 0,
    job_saves int not null default 0,
    wallet_savings int not null default 0,
    alerts int not null default 0,
    role text not null default 'student',
    owner_id uuid references auth.users(id) on delete set null,
    updated_at timestamptz not null default now()
);
alter table public.student_progress enable row level security;
drop policy if exists "Student progress is readable by the app" on public.student_progress;
create policy "Student progress is readable by the app" on public.student_progress
    for select using (auth.role() in ('anon', 'authenticated'));
