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

-- Sample data so the UI shows something immediately
insert into public.lessons (title, difficulty, description, progress_percent, quizzes_completed, quizzes_total)
values
  ('Budgeting Basics', 'Beginner', 'Understand income, expenses, and how to build a safe budget.', 70, 3, 5),
  ('Smart Saving', 'Intermediate', 'Set savings goals and track them with alerts and reminders.', 40, 2, 4);

insert into public.quiz_results (title, score, difficulty, coins_awarded)
values
  ('Budget Planner Quiz', 92, 'Intermediate', 120),
  ('Job Readiness', 84, 'Advanced', 140);

insert into public.job_opportunities (title, company, location, category, hours, pay_range, required_skills, safety_notes, contact, suitability_score)
values
  ('Mathematics Tutor', 'Learn & Grow Academy', 'Kolkata', 'Tutoring', 'Part-time (10-15 hrs/week)', '₹3,000 - ₹5,000/month',
            array['Grade 8-10 maths','Communication'], 'Work only with school references', 'call: 90000-00001', 86);

insert into public.notifications (title, description, severity, notify_date)
values
  ('Wallet Alert', 'Spending on transport exceeded the weekly budget.', 'warning', current_date - 1),
  ('Quiz Reminder', 'Practice "Savings Safety" quiz to unlock ParivaarCoins.', 'info', current_date - 2);

insert into public.student_progress (student_name, modules_completed, total_modules, quizzes_taken, average_score, wallet_health_score, parivaar_points, employment_applications, job_saves, wallet_savings, alerts)
values
  ('Rajesh Kumar', 8, 12, 15, 82, 78, 1250, 3, 5, 3200, 1);

insert into public.wallet_entries (owner_email, entry_type, category, amount, note, entry_date)
values
  ('student@parivaar.org', 'INCOME', 'Scholarship', 4500, 'Monthly stipend', current_date - 12),
  ('student@parivaar.org', 'EXPENSE', 'Food', 1800, 'Groceries and snacks', current_date - 3),
  ('student@parivaar.org', 'SAVINGS', 'Emergency', 3200, 'Emergency fund', current_date - 7);
