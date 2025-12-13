-- Stores the user's current budget and savings target
create table if not exists public.budget_goals (
    id uuid primary key default gen_random_uuid(),
    user_email text not null unique,
    current_budget numeric not null default 0,
    target_savings numeric not null default 0,
    notes text,
    updated_at timestamptz not null default now()
);

alter table public.budget_goals enable row level security;
drop policy if exists "Budget goals are readable" on public.budget_goals;
create policy "Budget goals are readable" on public.budget_goals
    for select using (auth.role() in ('anon', 'authenticated'));
drop policy if exists "Budget goals are writable" on public.budget_goals;
create policy "Budget goals are writable" on public.budget_goals
    for insert with check (auth.role() in ('anon', 'authenticated'));
