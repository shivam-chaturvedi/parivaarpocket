-- Tracks every quiz attempt by students
create table if not exists public.quiz_attempts (
    id uuid primary key default gen_random_uuid(),
    quiz_id uuid references public.quizzes(id) on delete set null,
    user_email text not null,
    score int not null default 0,
    max_score int not null default 0,
    passed boolean not null default false,
    responses jsonb,
    created_at timestamptz not null default now()
);

alter table public.quiz_attempts enable row level security;
drop policy if exists "Quiz attempts are readable" on public.quiz_attempts;
create policy "Quiz attempts are readable" on public.quiz_attempts
    for select using (auth.role() in ('anon', 'authenticated'));
drop policy if exists "Quiz attempts are writable" on public.quiz_attempts;
create policy "Quiz attempts are writable" on public.quiz_attempts
    for insert with check (auth.role() in ('anon', 'authenticated'));
