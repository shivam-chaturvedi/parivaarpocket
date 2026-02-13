-- Ensure dependencies exist (lessons, quizzes and quiz_questions)
-- These should have been created by 001, 005 and 006, but we ensure them here for robustness
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

create table if not exists public.quizzes (
    id uuid primary key default gen_random_uuid(),
    lesson_id uuid references public.lessons(id) on delete cascade,
    title text not null,
    difficulty text not null default 'Beginner',
    passing_marks int not null default 0,
    total_marks int not null default 0,
    created_at timestamptz not null default now()
);

create table if not exists public.quiz_questions (
    id uuid primary key default gen_random_uuid(),
    quiz_id uuid references public.quizzes(id) on delete cascade,
    question text not null,
    options text[] not null default array[]::text[],
    correct_option int not null default 0,
    points int not null default 1,
    created_at timestamptz not null default now()
);

-- Tracks which quiz questions have already rewarded coins to a user
create table if not exists public.quiz_rewards (
    id uuid primary key default gen_random_uuid(),
    user_email text not null,
    question_id uuid references public.quiz_questions(id) on delete cascade,
    created_at timestamptz not null default now(),
    unique(user_email, question_id)
);

-- Enable RLS
alter table public.quiz_rewards enable row level security;

-- Policies
do $$ 
begin
    if not exists (select 1 from pg_policies where policyname = 'Quiz rewards are readable by owners' and tablename = 'quiz_rewards') then
        create policy "Quiz rewards are readable by owners" on public.quiz_rewards
            for select using (auth.role() in ('anon', 'authenticated'));
    end if;
    
    if not exists (select 1 from pg_policies where policyname = 'Quiz rewards are insertable by owners' and tablename = 'quiz_rewards') then
        create policy "Quiz rewards are insertable by owners" on public.quiz_rewards
            for insert with check (auth.role() in ('anon', 'authenticated'));
    end if;
end $$;
