-- Add read status tracking to alerts table
ALTER TABLE public.alerts ADD COLUMN IF NOT EXISTS read boolean DEFAULT false;

-- Create index for faster queries on unread alerts
CREATE INDEX IF NOT EXISTS idx_alerts_read ON public.alerts(read);
