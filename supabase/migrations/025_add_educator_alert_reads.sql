-- Create educator_alert_reads table
CREATE TABLE IF NOT EXISTS public.educator_alert_reads (
    id UUID DEFAULT uuid_generate_v4() PRIMARY KEY,
    educator_email TEXT NOT NULL,
    alert_id UUID NOT NULL REFERENCES public.alerts(id) ON DELETE CASCADE,
    read_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    UNIQUE(educator_email, alert_id)
);

-- Enable RLS
ALTER TABLE public.educator_alert_reads ENABLE ROW LEVEL SECURITY;

-- Create policies
CREATE POLICY "Educators can view their own read receipts"
    ON public.educator_alert_reads FOR SELECT
    USING (educator_email = current_user);

CREATE POLICY "Educators can insert their own read receipts"
    ON public.educator_alert_reads FOR INSERT
    WITH CHECK (educator_email = current_user);

CREATE POLICY "Educators can update their own read receipts"
    ON public.educator_alert_reads FOR UPDATE
    USING (educator_email = current_user);

-- Allow public access for now as per other tables
CREATE POLICY "Public read educator_alert_reads"
    ON public.educator_alert_reads FOR SELECT
    USING (true);

CREATE POLICY "Public insert educator_alert_reads"
    ON public.educator_alert_reads FOR INSERT
    WITH CHECK (true);

CREATE POLICY "Public update educator_alert_reads"
    ON public.educator_alert_reads FOR UPDATE
    USING (true);
