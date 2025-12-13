alter table if exists public.lessons
    add column if not exists course_url text;

update public.lessons
set course_url = 'https://parivaar.org/courses/budgeting-basics'
where title ilike 'Budgeting Basics';

update public.lessons
set course_url = 'https://parivaar.org/courses/smart-saving'
where title ilike 'Smart Saving';
