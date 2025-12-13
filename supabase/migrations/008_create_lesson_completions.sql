-- Stores which lessons each student has completed
create table if not exists public.lesson_completions (
    id uuid primary key default gen_random_uuid(),
    lesson_id uuid references public.lessons(id) on delete cascade,
    user_email text not null,
    quiz_attempt_id uuid references public.quiz_attempts(id) on delete set null,
    completed_at timestamptz not null default now()
);

alter table public.lesson_completions enable row level security;
drop policy if exists "Lesson completions are readable" on public.lesson_completions;
create policy "Lesson completions are readable" on public.lesson_completions
    for select using (auth.role() in ('anon', 'authenticated'));
drop policy if exists "Lesson completions are writable" on public.lesson_completions;
create policy "Lesson completions are writable" on public.lesson_completions
    for insert with check (auth.role() in ('anon', 'authenticated'));
