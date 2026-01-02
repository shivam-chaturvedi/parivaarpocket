-- Migration to add granular filter support to the jobs table
alter table public.jobs 
add column if not exists category text,
add column if not exists required_skills text[],
add column if not exists working_hours text;

-- Update RLS if necessary (though existing policy search based on anon/auth should cover these)
comment on column public.jobs.category is 'Categorization: Tutoring, Delivery, Retail, Internship, General';
comment on column public.jobs.required_skills is 'Array of skills extracted from title/description';
comment on column public.jobs.working_hours is 'Working hour details (e.g., Full-time, Part-time)';
