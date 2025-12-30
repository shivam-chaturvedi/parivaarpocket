-- Enable public access for wallet_entries (allow all operations)
drop policy if exists "Wallet entries are readable" on public.wallet_entries;
drop policy if exists "Wallet entries are writable" on public.wallet_entries;
drop policy if exists "Wallet entries are updatable" on public.wallet_entries;
drop policy if exists "Wallet entries are deletable" on public.wallet_entries;

create policy "Public access wallet_entries" on public.wallet_entries
    for all using (true) with check (true);

-- Enable public access for budget_goals (allow all operations)
drop policy if exists "Budget goals are readable" on public.budget_goals;
drop policy if exists "Budget goals are writable" on public.budget_goals;

create policy "Public access budget_goals" on public.budget_goals
    for all using (true) with check (true);
