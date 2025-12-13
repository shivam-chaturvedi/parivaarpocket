alter table if exists public.job_opportunities
    add column if not exists job_url text;

update public.job_opportunities
set job_url = 'https://parivaar.org/jobs/mathematics-tutor'
where title ilike 'Mathematics Tutor';
