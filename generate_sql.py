import os
import re

courses_dir = "/Users/Shared/shivam/parivaarpocket/courses/Courses x 12"
quizzes_dir = "/Users/Shared/shivam/parivaarpocket/courses/Corresponding Quizzes x 12"

def escape_sql(text):
    return text.replace("'", "''")

def parse_course_file(filename):
    with open(os.path.join(courses_dir, filename), 'r') as f:
        content = f.read().strip()
    
    title_match = re.match(r'(M\d+)_ (.*)', filename.replace(".txt", ""))
    if title_match:
        title = f"{title_match.group(1)}: {title_match.group(2)}"
    else:
        title = filename.replace(".txt", "").replace("_", ":")
    
    difficulty = "Beginner"
    if "Intermediate" in content or "Intermediate" in filename:
        difficulty = "Intermediate"
    elif "Advanced" in content or "Advanced" in filename:
        difficulty = "Advanced"
        
    return {
        "title": title,
        "content": content,
        "difficulty": difficulty,
        "filename_id": filename.split("_")[0]
    }

def parse_quiz_file(filename):
    with open(os.path.join(quizzes_dir, filename), 'r') as f:
        content = f.read().strip()
    
    quiz_id_str = filename.split("_")[0]
    questions = []
    parts = re.split(r'\n\s*\d+\.\s+', "\n" + content)
    for part in parts[1:]:
        lines = part.strip().split('\n')
        if not lines: continue
        
        question_text = lines[0].strip()
        full_options_text = " ".join(lines[1:])
        
        opt_matches = list(re.finditer(r'([A-D])\)\s*(.*?)(?=[A-D]\)|✅|$)', full_options_text))
        options = []
        if opt_matches:
            for match in opt_matches:
                options.append(match.group(2).strip())
        
        ca_match = re.search(r'✅ Correct Answer:\s*([A-D])', full_options_text)
        correct_index = 0
        if ca_match:
            correct_index = ord(ca_match.group(1)) - ord('A')
            
        if question_text and options:
            questions.append({
                "question": question_text,
                "options": options,
                "correct_option": correct_index
            })
            
    return {
        "id_str": quiz_id_str,
        "questions": questions
    }

print(f"Checking courses in {courses_dir}")
courses = []
for f in sorted(os.listdir(courses_dir)):
    if f.endswith(".txt"):
        courses.append(parse_course_file(f))
print(f"Found {len(courses)} courses")

print(f"Checking quizzes in {quizzes_dir}")
quizzes = {}
for f in sorted(os.listdir(quizzes_dir)):
    if f.endswith(".txt"):
        q = parse_quiz_file(f)
        quizzes[q["id_str"].replace("Q", "M")] = q
print(f"Found {len(quizzes)} quizzes")

sql = [
    "-- Final migration to seed all 12 modules and quizzes",
    "ALTER TABLE IF EXISTS public.lessons DROP COLUMN IF EXISTS course_url CASCADE;",
    "",
    "-- Clean up existing modules to avoid duplicates",
    "DELETE FROM public.quiz_questions WHERE quiz_id IN (SELECT id FROM public.quizzes WHERE lesson_id IN (SELECT id FROM public.lessons WHERE title LIKE 'M%: %'));",
    "DELETE FROM public.quizzes WHERE lesson_id IN (SELECT id FROM public.lessons WHERE title LIKE 'M%: %');",
    "DELETE FROM public.lessons WHERE title LIKE 'M%: %';",
    ""
]

for course in courses:
    m_id = course["filename_id"]
    sql.append(f"-- Module {m_id}")
    sql.append(f"INSERT INTO public.lessons (id, title, difficulty, description, progress_percent, quizzes_total)")
    sql.append(f"VALUES (gen_random_uuid(), '{escape_sql(course['title'])}', '{course['difficulty']}', '{escape_sql(course['content'])}', 0, 1);")
    
    quiz = quizzes.get(m_id)
    if quiz:
        sql.append(f"INSERT INTO public.quizzes (lesson_id, title, difficulty, total_marks, passing_marks)")
        sql.append(f"SELECT id, '{escape_sql(course['title'])} Quiz', difficulty, {len(quiz['questions'])}, {max(1, len(quiz['questions'])//2)} FROM public.lessons WHERE title = '{escape_sql(course['title'])}';")
        
        for q in quiz["questions"]:
            options_sql = "ARRAY[" + ", ".join([f"'{escape_sql(opt)}'" for opt in q["options"]]) + "]"
            sql.append(f"INSERT INTO public.quiz_questions (quiz_id, question, options, correct_option, points)")
            sql.append(f"SELECT id, '{escape_sql(q['question'])}', {options_sql}, {q['correct_option']}, 1 FROM public.quizzes WHERE title = '{escape_sql(course['title'])} Quiz';")
    sql.append("")

with open("seed_output.sql", "w") as f:
    f.write("\n".join(sql))

print("SQL generated successfully in seed_output.sql")
