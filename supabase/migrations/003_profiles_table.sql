-- Stores the email and declared role for each user account
create table if not exists public.profiles (
    id uuid primary key default gen_random_uuid(),
    email text not null unique,
    role text not null default 'student',
    created_at timestamptz not null default now()
);

alter table public.profiles enable row level security;
drop policy if exists "Profiles are readable by the app" on public.profiles;
create policy "Profiles are readable by the app" on public.profiles
    for select using (auth.role() in ('anon', 'authenticated'));
drop policy if exists "Profiles are writable by the app" on public.profiles;
create policy "Profiles are writable by the app" on public.profiles
    for insert with check (auth.role() in ('anon', 'authenticated'));
