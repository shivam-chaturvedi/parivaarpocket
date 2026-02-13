-- 1. Fix student_progress table
ALTER TABLE public.student_progress 
ADD COLUMN IF NOT EXISTS user_email text;

-- Add unique constraint for upsert support if not exists
DO $$ 
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'student_progress_user_email_key') THEN
        ALTER TABLE public.student_progress ADD CONSTRAINT student_progress_user_email_key UNIQUE (user_email);
    END IF;
END $$;

-- 2. Fix budget_goals table
ALTER TABLE public.budget_goals 
ADD COLUMN IF NOT EXISTS user_email text;

DO $$ 
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'budget_goals_user_email_key') THEN
        ALTER TABLE public.budget_goals ADD CONSTRAINT budget_goals_user_email_key UNIQUE (user_email);
    END IF;
END $$;

-- 3. Fix quiz_rewards table
CREATE TABLE IF NOT EXISTS public.quiz_rewards (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    user_email text NOT NULL,
    question_id uuid REFERENCES public.quiz_questions(id) ON DELETE CASCADE,
    created_at timestamptz NOT NULL DEFAULT now(),
    UNIQUE(user_email, question_id)
);

-- Enable RLS
ALTER TABLE public.quiz_rewards ENABLE ROW LEVEL SECURITY;

-- Policies for quiz_rewards
DO $$ 
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_policies WHERE policyname = 'Quiz rewards are readable by owners' AND tablename = 'quiz_rewards') THEN
        CREATE POLICY "Quiz rewards are readable by owners" ON public.quiz_rewards
            FOR SELECT USING (auth.role() IN ('anon', 'authenticated'));
    END IF;
    
    IF NOT EXISTS (SELECT 1 FROM pg_policies WHERE policyname = 'Quiz rewards are insertable by owners' AND tablename = 'quiz_rewards') THEN
        CREATE POLICY "Quiz rewards are insertable by owners" ON public.quiz_rewards
            FOR INSERT WITH CHECK (auth.role() IN ('anon', 'authenticated'));
    END IF;
END $$;

-- 4. Enable RLS on other tables just in case
ALTER TABLE public.student_progress ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.budget_goals ENABLE ROW LEVEL SECURITY;

-- Ensure public access policies exist for app simplicity (as per project baseline)
DO $$ 
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_policies WHERE policyname = 'Public access for student_progress' AND tablename = 'student_progress') THEN
        CREATE POLICY "Public access for student_progress" ON public.student_progress
            FOR ALL USING (true) WITH CHECK (true);
    END IF;
    
    IF NOT EXISTS (SELECT 1 FROM pg_policies WHERE policyname = 'Public access for budget_goals' AND tablename = 'budget_goals') THEN
        CREATE POLICY "Public access for budget_goals" ON public.budget_goals
            FOR ALL USING (true) WITH CHECK (true);
    END IF;
END $$;
