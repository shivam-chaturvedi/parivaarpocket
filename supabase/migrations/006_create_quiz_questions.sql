-- Stores MCQ options for each quiz
create table if not exists public.quiz_questions (
    id uuid primary key default gen_random_uuid(),
    quiz_id uuid references public.quizzes(id) on delete cascade,
    question text not null,
    options text[] not null default array[]::text[],
    correct_option int not null default 0,
    points int not null default 1,
    created_at timestamptz not null default now()
);

alter table public.quiz_questions enable row level security;
drop policy if exists "Quiz questions read" on public.quiz_questions;
create policy "Quiz questions read" on public.quiz_questions
    for select using (auth.role() in ('anon', 'authenticated'));
drop policy if exists "Quiz questions insert" on public.quiz_questions;
create policy "Quiz questions insert" on public.quiz_questions
    for insert with check (auth.role() in ('anon', 'authenticated'));

insert into public.quiz_questions (quiz_id, question, options, correct_option, points)
select q.id, 'Which of these is a core piece of a safe budget?', array['Track income and savings','Ignore receipts','Spend first, save later','Borrow heavily'], 0, 1
from public.quizzes q
join public.lessons l on l.id = q.lesson_id
where l.title ilike 'Budgeting Basics'
limit 1;

insert into public.quiz_questions (quiz_id, question, options, correct_option, points)
select q.id, 'What should you do when you notice expenses creeping above your plan?', array['Adjust the plan and monitor','Ignore the overspend','Remove all savings','Take a high-interest loan'], 0, 1
from public.quizzes q
join public.lessons l on l.id = q.lesson_id
where l.title ilike 'Budgeting Basics'
limit 1;
