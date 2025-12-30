-- Stores individual wallet transactions (income, expense, savings)
create table if not exists public.wallet_entries (
    id uuid primary key default gen_random_uuid(),
    owner_email text not null,
    entry_type text not null,
    category text,
    amount numeric not null,
    note text,
    entry_date date not null,
    created_at timestamptz not null default now()
);

-- Enable RLS
alter table public.wallet_entries enable row level security;

-- Policies
-- Allowing broad access for demo purposes, consistent with budget_goals migration.
-- In production, these should be restricted to `owner_email = auth.jwt() ->> 'email'`.

drop policy if exists "Wallet entries are readable" on public.wallet_entries;
create policy "Wallet entries are readable" on public.wallet_entries
    for select using (auth.role() in ('anon', 'authenticated'));

drop policy if exists "Wallet entries are writable" on public.wallet_entries;
create policy "Wallet entries are writable" on public.wallet_entries
    for insert with check (auth.role() in ('anon', 'authenticated'));

drop policy if exists "Wallet entries are updatable" on public.wallet_entries;
create policy "Wallet entries are updatable" on public.wallet_entries
    for update using (auth.role() in ('anon', 'authenticated'));

drop policy if exists "Wallet entries are deletable" on public.wallet_entries;
create policy "Wallet entries are deletable" on public.wallet_entries
    for delete using (auth.role() in ('anon', 'authenticated'));
