-- Final Schema Repair Script
-- This script ensures that the student_progress table has the required columns
-- and that the quiz_rewards table exists.

-- 1. Fix student_progress table
ALTER TABLE public.student_progress ADD COLUMN IF NOT EXISTS user_email text;
ALTER TABLE public.student_progress ADD COLUMN IF NOT EXISTS total_modules integer DEFAULT 0;
ALTER TABLE public.student_progress ADD COLUMN IF NOT EXISTS quizzes_taken integer DEFAULT 0;
ALTER TABLE public.student_progress ADD COLUMN IF NOT EXISTS average_score numeric DEFAULT 0;

-- 2. Create quiz_rewards if missing
CREATE TABLE IF NOT EXISTS public.quiz_rewards (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    quiz_id uuid REFERENCES public.quizzes(id) ON DELETE CASCADE,
    question_index integer NOT NULL,
    user_email text NOT NULL,
    points_awarded integer DEFAULT 0,
    awarded_at timestamptz DEFAULT now(),
    UNIQUE(quiz_id, question_index, user_email)
);

-- 3. Fix budget_goals (observed error in logs previously)
ALTER TABLE public.budget_goals ADD COLUMN IF NOT EXISTS user_email text;

-- 4. Enable RLS and add basic policies if they don't exist
ALTER TABLE public.student_progress ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.quiz_rewards ENABLE ROW LEVEL SECURITY;

DO $$ 
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_policies WHERE tablename = 'student_progress' AND policyname = 'Public read') THEN
        CREATE POLICY "Public read" ON public.student_progress FOR SELECT USING (true);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_policies WHERE tablename = 'student_progress' AND policyname = 'Public write') THEN
        CREATE POLICY "Public write" ON public.student_progress FOR ALL USING (true);
    END IF;
    
    IF NOT EXISTS (SELECT 1 FROM pg_policies WHERE tablename = 'quiz_rewards' AND policyname = 'Public read') THEN
        CREATE POLICY "Public read" ON public.quiz_rewards FOR SELECT USING (true);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_policies WHERE tablename = 'quiz_rewards' AND policyname = 'Public write') THEN
        CREATE POLICY "Public write" ON public.quiz_rewards FOR ALL USING (true);
    END IF;
END $$;
