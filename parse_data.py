import os
import json
import re

courses_dir = "/Users/Shared/shivam/parivaarpocket/courses/Courses x 12"
quizzes_dir = "/Users/Shared/shivam/parivaarpocket/courses/Corresponding Quizzes x 12"

def parse_course(content, filename):
    title = filename.replace(".txt", "")
    return {"title": title, "content": content.strip()}

def parse_quiz(content, filename):
    title = filename.replace(".txt", "")
    questions = []
    
    # Split by item numbers (e.g., 1. , 2. )
    parts = re.split(r'\n\s*\d+\.\s+', "\n" + content)
    for part in parts[1:]:
        lines = part.strip().split('\n')
        question_text = lines[0].strip()
        options = []
        correct_answer = None
        
        full_line = " ".join(lines[1:])
        # Find options A), B), C), D)
        opt_matches = re.findall(r'([A-D])\)\s*([^A-D✅\n]+)', full_line)
        if opt_matches:
            options = [m[1].strip() for m in opt_matches]
        
        # Find correct answer
        ca_match = re.search(r'✅ Correct Answer:\s*([A-D])', full_line)
        if ca_match:
            correct_answer = ca_match.group(1)
            # Map A, B, C, D to 0, 1, 2, 3
            correct_index = ord(correct_answer) - ord('A')
        else:
            correct_index = 0
            
        questions.append({
            "question": question_text,
            "options": options,
            "correct_option": correct_index
        })
    return {"title": title, "questions": questions}

data = {"lessons": [], "quizzes": []}

for f in sorted(os.listdir(courses_dir)):
    if f.endswith(".txt"):
        with open(os.path.join(courses_dir, f), 'r') as file:
            data["lessons"].append(parse_course(file.read(), f))

for f in sorted(os.listdir(quizzes_dir)):
    if f.endswith(".txt"):
        with open(os.path.join(quizzes_dir, f), 'r') as file:
            data["quizzes"].append(parse_quiz(file.read(), f))

print(json.dumps(data))
