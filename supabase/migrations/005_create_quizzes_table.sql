-- Defines the quiz set for each lesson
create table if not exists public.quizzes (
    id uuid primary key default gen_random_uuid(),
    lesson_id uuid references public.lessons(id) on delete cascade,
    title text not null,
    difficulty text not null default 'Beginner',
    passing_marks int not null default 0,
    total_marks int not null default 0,
    created_at timestamptz not null default now()
);

alter table public.quizzes enable row level security;
drop policy if exists "Quizzes are readable by the app" on public.quizzes;
create policy "Quizzes are readable by the app" on public.quizzes
    for select using (auth.role() in ('anon', 'authenticated'));
drop policy if exists "Quizzes are writable by the app" on public.quizzes;
create policy "Quizzes are writable by the app" on public.quizzes
    for insert with check (auth.role() in ('anon', 'authenticated'));

insert into public.quizzes (lesson_id, title, difficulty, passing_marks, total_marks)
select id, 'Budgeting Basics Check-in', 'Beginner', 3, 5
from public.lessons
where title ilike 'Budgeting Basics'
limit 1;
