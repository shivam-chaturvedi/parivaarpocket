-- Migration: create alerts table for budget and wallet issues
create table if not exists public.alerts (
    id uuid primary key default gen_random_uuid(),
    user_email text not null,
    category text not null,
    severity text not null default 'info',
    message text not null,
    metadata jsonb default '{}'::jsonb,
    created_at timestamptz not null default now()
);

alter table public.alerts enable row level security;

drop policy if exists "Public alerts access" on public.alerts;
create policy "Public alerts access" on public.alerts
    for all using (true) with check (true);

comment on table public.alerts is 'Alerts created when wallet/budget conditions require educator attention';
comment on column public.alerts.category is 'Logical grouping (e.g., Budget, Wallet)';
comment on column public.alerts.severity is 'Severity label such as info, warning, danger';
comment on column public.alerts.message is 'Human-readable alert summary';
comment on column public.alerts.metadata is 'Structured data containing additional context';
