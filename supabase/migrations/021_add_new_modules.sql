-- Final migration to seed all 12 modules and quizzes
ALTER TABLE IF EXISTS public.lessons DROP COLUMN IF EXISTS course_url CASCADE;

-- Clean up existing modules to avoid duplicates
DELETE FROM public.quiz_questions WHERE quiz_id IN (SELECT id FROM public.quizzes WHERE lesson_id IN (SELECT id FROM public.lessons WHERE title LIKE 'M%: %'));
DELETE FROM public.quizzes WHERE lesson_id IN (SELECT id FROM public.lessons WHERE title LIKE 'M%: %');
DELETE FROM public.lessons WHERE title LIKE 'M%: %';

-- Module M10
INSERT INTO public.lessons (id, title, difficulty, description, progress_percent, quizzes_total)
VALUES (gen_random_uuid(), 'M10: Understanding Loans', 'Beginner', 'A loan is money borrowed that must be repaid with interest over time. Loans can be helpful, but if misused, they can trap families in long-term debt cycles.
Common Types of Loans
     Education loans  -  for studies and skill development
     Personal loans  -  high interest, flexible use
     Microloans  -  small loans, often used by low-income households
Key loan terms:
     Principal: Amount borrowed
     Interest: Cost of borrowing
     EMI (Equated Monthly Instalment): Fixed monthly repayment
For example, borrowing ₹50,000 at high interest from a moneylender may result in repaying much more than the original amount.
Gd borrowing practices include:
     Borrowing only for productive purposes (education, business)
     Comparing interest rates before borrowing
     Ensuring EMIs fit within the household budget
Bad borrowing leads to:
     Missed repayments
     Penalties and stress
     Reduced future borrowing ability
ParivaarPocket teaches responsible credit usage by showing repayment schedules and interest impact visually.
Financial Tip
👉 If you cannot afford the EMI today, you cannot afford the loan.', 0, 1);
INSERT INTO public.quizzes (lesson_id, title, difficulty, total_marks, passing_marks)
SELECT id, 'M10: Understanding Loans Quiz', difficulty, 5, 2 FROM public.lessons WHERE title = 'M10: Understanding Loans';
INSERT INTO public.quiz_questions (quiz_id, question, options, correct_option, points)
SELECT id, 'What is interest and why do lenders charge it?', ARRAY['Extra money you pay for convenience', 'Money paid to the lender for borrowing funds', 'A government tax on loans', 'A discount on repayments'], 1, 1 FROM public.quizzes WHERE title = 'M10: Understanding Loans Quiz';
INSERT INTO public.quiz_questions (quiz_id, question, options, correct_option, points)
SELECT id, 'Name two types of loans commonly used by families.', ARRAY['Car loan and lottery loan', 'Home loan and personal loan', 'Credit card loan and casino loan', 'Education loan and free loan'], 1, 1 FROM public.quizzes WHERE title = 'M10: Understanding Loans Quiz';
INSERT INTO public.quiz_questions (quiz_id, question, options, correct_option, points)
SELECT id, 'Why are high-interest loans dangerous for low-income households?', ARRAY['They are fun to pay back', 'They help build wealth quickly', 'They can trap households in debt because repayments are high', 'They reduce expenses automatically'], 2, 1 FROM public.quizzes WHERE title = 'M10: Understanding Loans Quiz';
INSERT INTO public.quiz_questions (quiz_id, question, options, correct_option, points)
SELECT id, 'True or False: Loans should be taken for luxury items. Explain.', ARRAY['True', 'False'], 1, 1 FROM public.quizzes WHERE title = 'M10: Understanding Loans Quiz';
INSERT INTO public.quiz_questions (quiz_id, question, options, correct_option, points)
SELECT id, 'Explain how EMI affects monthly household budgeting.', ARRAY['EMI doesn''t affect budgeting', 'EMIs are fixed monthly payments that reduce available money for other expenses', 'EMIs increase your total income', 'EMIs are optional payments that can be skipped anytime'], 1, 1 FROM public.quizzes WHERE title = 'M10: Understanding Loans Quiz';

-- Module M11
INSERT INTO public.lessons (id, title, difficulty, description, progress_percent, quizzes_total)
VALUES (gen_random_uuid(), 'M11: Financial Goal Setting', 'Beginner', 'In many families, older children help manage household money. Understanding household finances means helping with big expenses and savings, not just your own.
Key concepts and examples:
     Household Income and Expenses: Together, a family has income (salaries, farm income, business) and expenses (fd, rent, schl fees, healthcare). By learning budgeting and saving, you can suggest how to reduce unnecessary costs. For example, comparing electricity providers, or deciding to ck at home rather than eat outside can save money.
     Sharing Responsibilities: If you have earned or saved money, talk with family about using it for important needs. For example, Priya pled her savings with her mother to buy ingredients in bulk, which reduced the grocery bill by 10%.
     Decision-Making: Families can make decisions like "we will save ₹500 this month for a new water pump". You can help by tracking this savings in a notebk. Participation teaches financial planning.
     Emergency Planning: Families sometimes face crises (illness, crop failure). Discussing an emergency fund or how to cut costs helps everyone. For example, if a family sets aside a small fund each month for emergencies, they won''t have to take high-interest loans later.
Financial Tip: Suggest a regular family meeting to discuss money goals. Even once a month, talk about what you''ve spent, earned, and plan to save. Working together makes financial plans stronger.', 0, 1);
INSERT INTO public.quizzes (lesson_id, title, difficulty, total_marks, passing_marks)
SELECT id, 'M11: Financial Goal Setting Quiz', difficulty, 5, 2 FROM public.lessons WHERE title = 'M11: Financial Goal Setting';
INSERT INTO public.quiz_questions (quiz_id, question, options, correct_option, points)
SELECT id, 'How can a student help reduce household expenses? Give one example', ARRAY['Buying snacks every day', 'Sharing siblings'' clothes', 'Suggesting to save on electricity or water', 'Buying unnecessary gifts.'], 2, 1 FROM public.quizzes WHERE title = 'M11: Financial Goal Setting Quiz';
INSERT INTO public.quiz_questions (quiz_id, question, options, correct_option, points)
SELECT id, 'Why is it gd for a family to have an emergency fund?', ARRAY['For holidays', 'To avoid borrowing in a crisis', 'To lend to friends', 'For no reason.'], 1, 1 FROM public.quizzes WHERE title = 'M11: Financial Goal Setting Quiz';
INSERT INTO public.quiz_questions (quiz_id, question, options, correct_option, points)
SELECT id, 'In a family budget, which of these is usually an essential expense?', ARRAY['Rent or house loan', 'Buying the latest video game', 'Impulse shopping', 'None are essential.'], 0, 1 FROM public.quizzes WHERE title = 'M11: Financial Goal Setting Quiz';
INSERT INTO public.quiz_questions (quiz_id, question, options, correct_option, points)
SELECT id, 'True or False: It is helpful to talk openly about money goals with your family.', ARRAY['True', 'False'], 0, 1 FROM public.quizzes WHERE title = 'M11: Financial Goal Setting Quiz';
INSERT INTO public.quiz_questions (quiz_id, question, options, correct_option, points)
SELECT id, 'If a family decides to save ₹1000 in 10 months for a water filter, how much should they save each month?', ARRAY['₹50', '₹100', '₹100', '₹10'], 2, 1 FROM public.quizzes WHERE title = 'M11: Financial Goal Setting Quiz';

-- Module M12
INSERT INTO public.lessons (id, title, difficulty, description, progress_percent, quizzes_total)
VALUES (gen_random_uuid(), 'M12: Retirement Planning Basics', 'Beginner', 'Financial planning means thinking ahead to long-term goals: higher education, a home, or a family business. Starting early gives you a head start.
Key concepts and examples:
     Education and Career Plans: Decide what you want to study or do after schl. Save or seek help (scholarships) to reach those goals. Example: Kiran wants to attend college in another city; knowing it costs ₹50,000 total, her family started saving a small amount each month early.
     Inflation and Costs: Prices tend to rise over time (inflation). What costs ₹100 today might cost ₹110 next year. When planning future expenses (like college fees, marriage expenses), remember this. It means saving early is even more important.
     Continuing to Learn: Stay informed about money (bks, trustworthy websites) and adjust your plans as needed. The world of work changes (new jobs in technology, etc.). Be flexible and willing to learn new skills.
     Supporting Economic Security: Your personal planning contributes to your family''s security. If you get a gd job or manage money well, you break cycles of debt and help the next generation.
Financial Tip: Set at least one big goal (e.g., college admission, starting a small business) and list steps to achieve it (save money, study hard, learn skills). Review your progress yearly and adjust as needed.', 0, 1);
INSERT INTO public.quizzes (lesson_id, title, difficulty, total_marks, passing_marks)
SELECT id, 'M12: Retirement Planning Basics Quiz', difficulty, 5, 2 FROM public.lessons WHERE title = 'M12: Retirement Planning Basics';
INSERT INTO public.quiz_questions (quiz_id, question, options, correct_option, points)
SELECT id, 'What is inflation and why should it matter when you save money for 5 - 10 years from now?', ARRAY['sudden increase in pocket money', 'When prices stay the same over time', 'When prices increase over time, reducing the value of your savings', 'Free gifts from the government'], 2, 1 FROM public.quizzes WHERE title = 'M12: Retirement Planning Basics Quiz';
INSERT INTO public.quiz_questions (quiz_id, question, options, correct_option, points)
SELECT id, 'Give one example of a long-term financial goal a student might have. How could you start working towards it now?', ARRAY['Buying candy daily', 'Saving for a new phone', 'Saving for college fees or higher education', 'Buying a car immediately'], 2, 1 FROM public.quizzes WHERE title = 'M12: Retirement Planning Basics Quiz';
INSERT INTO public.quiz_questions (quiz_id, question, options, correct_option, points)
SELECT id, 'True or False: Earning a little extra now (from a part-time job or skills) can help you reach future goals faster.', ARRAY['True', 'False'], 0, 1 FROM public.quizzes WHERE title = 'M12: Retirement Planning Basics Quiz';
INSERT INTO public.quiz_questions (quiz_id, question, options, correct_option, points)
SELECT id, 'Why is ongoing education (learning new skills) considered a form of financial planning?', ARRAY['It is fun and makes time pass quickly', 'It can increase your earning potential in the future', 'It reduces current expenses automatically', 'It guarantees free money from government'], 1, 1 FROM public.quizzes WHERE title = 'M12: Retirement Planning Basics Quiz';
INSERT INTO public.quiz_questions (quiz_id, question, options, correct_option, points)
SELECT id, 'Name one way to find help or information for planning your education or career in India (such as scholarships, counseling centers, etc.).', ARRAY['Social media trends', 'Friends only', 'Career counseling centers, scholarship websites, or schl guidance', 'Buying lottery tickets'], 2, 1 FROM public.quizzes WHERE title = 'M12: Retirement Planning Basics Quiz';

-- Module M1
INSERT INTO public.lessons (id, title, difficulty, description, progress_percent, quizzes_total)
VALUES (gen_random_uuid(), 'M1: Introduction to Personal Finance', 'Beginner', 'Learning about personal finance means understanding how to manage your money wisely. This includes knowing how to earn money (income), spend it, save it, and plan for the future. In India today, many families struggle with money decisions  -  only about a quarter of Indian adults truly understand basic money matters. As students, learning these skills now can help you avoid stress later. For example, if you learn to save even a small part of your pocket money, you can help pay for schl bks or support your family''s needs.
Key concepts in personal finance include:
     Income: Money you receive, such as pocket money, gifts, or earnings from small jobs (tutoring, farming help, etc.). Every rupee you get can be used or planned.
     Spending: Money you use to buy things. Some things are needs (essential: fd, schl fees, medicines) and some are wants (non-essentials: snacks, games). Learning the difference helps you spend wisely.
     Saving: Setting aside a portion of your income instead of spending it all. Even saving ₹10 a week can grow into a useful amount. For instance, Rahul saved a small part of his ₹100 weekly pocket money and after a few months he had enough to buy important study supplies. Banks and even simple piggy banks can help you save.
     Budgeting: Making a simple plan for your money. A budget lists your expected income and expenses so you can see how much to spend on needs, wants, and savings. A common rule is to split money into parts: for example, using about 50% for needs, 30% for wants, and 20% for savings. This 50/30/20 split is a guideline to help you cover essentials first.
Financial Tip: Try keeping a money diary or use a mobile app (if you have a phone) to record every rupee you spend and save. This will show you where your money goes and help you make better plans.', 0, 1);
INSERT INTO public.quizzes (lesson_id, title, difficulty, total_marks, passing_marks)
SELECT id, 'M1: Introduction to Personal Finance Quiz', difficulty, 5, 2 FROM public.lessons WHERE title = 'M1: Introduction to Personal Finance';
INSERT INTO public.quiz_questions (quiz_id, question, options, correct_option, points)
SELECT id, 'What does personal finance primarily involve?', ARRAY['Learning history', 'Managing money (income, spending, saving)', 'Physical exercise', 'Cking.'], 1, 1 FROM public.quizzes WHERE title = 'M1: Introduction to Personal Finance Quiz';
INSERT INTO public.quiz_questions (quiz_id, question, options, correct_option, points)
SELECT id, 'Give one example of a need and one example of a want for a student.', ARRAY['Need: Video games, Want: Schl bks', 'Need: Schl uniform, Want: New headphones', 'Need: Movie tickets, Want: Lunch', 'Need: Chocolates, Want: Water'], 1, 1 FROM public.quizzes WHERE title = 'M1: Introduction to Personal Finance Quiz';
INSERT INTO public.quiz_questions (quiz_id, question, options, correct_option, points)
SELECT id, 'Why is it important to save money?', ARRAY['To spend more on shopping', 'To impress others', 'To prepare for emergencies and future needs', 'To avoid earning income'], 2, 1 FROM public.quizzes WHERE title = 'M1: Introduction to Personal Finance Quiz';
INSERT INTO public.quiz_questions (quiz_id, question, options, correct_option, points)
SELECT id, 'If you have ₹200 and save 20% for later, how much do you save and how much can you spend now?', ARRAY['Save ₹20, Spend ₹180', 'Save ₹30, Spend ₹170', 'Save ₹40, Spend ₹160', 'Save ₹50, Spend ₹150'], 2, 1 FROM public.quizzes WHERE title = 'M1: Introduction to Personal Finance Quiz';
INSERT INTO public.quiz_questions (quiz_id, question, options, correct_option, points)
SELECT id, 'True or False: Making a budget helps you plan your money and avoid running out of cash.', ARRAY['True', 'False', 'Only for adults', 'Only for rich people'], 0, 1 FROM public.quizzes WHERE title = 'M1: Introduction to Personal Finance Quiz';

-- Module M2
INSERT INTO public.lessons (id, title, difficulty, description, progress_percent, quizzes_total)
VALUES (gen_random_uuid(), 'M2: Budgeting Essentials', 'Beginner', 'Managing day-to-day money starts with making a budget, or a simple money plan. Budgeting means writing down how much money you expect (income) and what you need to spend it on. For a student, income might be pocket money or help from family; expenses include fd, schl supplies, bus fare, and maybe a bit for fun. By planning ahead, you know if your money will last the month.
Key concepts and examples:
     List Income and Expenses: Write down all money you have coming in (e.g., ₹300 monthly pocket money) and all expenses. For example, Rani lists ₹150 for notebks, ₹50 for phone calls, and ₹100 for other needs. This way she sees her money clearly.
     Track Needs vs Wants: First pay for needs. If you have ₹300, paying ₹150 for notebks and ₹100 for lunch (₹250 total) covers needs. Then think carefully about wants  -  maybe ₹50 for a movie ticket. Any more spending could hurt savings.
     Balance Your Budget: Ensure you don''t plan to spend more than you have. If expenses exceed income, you must cut back on wants or find more income. For example, if Rani plans to spend ₹350 on everything with only ₹300 income, her budget is unbalanced and she must adjust.
     Use Simple Budget Tls: You can use a notebk or a phone app (many free apps exist) to list and add up numbers. Even a simple table on paper helps. Seeing your budget written down makes money feel real, not just guesses.
Financial Tip: At the start of each month, decide on a savings goal. For example, put aside 10% of your expected income as savings. This way you automatically save before deciding on extra treats or wants.', 0, 1);
INSERT INTO public.quizzes (lesson_id, title, difficulty, total_marks, passing_marks)
SELECT id, 'M2: Budgeting Essentials Quiz', difficulty, 5, 2 FROM public.lessons WHERE title = 'M2: Budgeting Essentials';
INSERT INTO public.quiz_questions (quiz_id, question, options, correct_option, points)
SELECT id, 'Why should you list your expenses in a budget before wants?', ARRAY['Because wants are more important', 'To make sure you can afford essentials (needs) first', 'So you spend money quickly', 'It''s more fun.'], 1, 1 FROM public.quizzes WHERE title = 'M2: Budgeting Essentials Quiz';
INSERT INTO public.quiz_questions (quiz_id, question, options, correct_option, points)
SELECT id, 'Raju has ₹500 for the month. He spends ₹200 on tuition, ₹100 on fd, and ₹150 on clothes. How much is left? Is his budget balanced if he wanted to save ₹100?', ARRAY['₹100 left; Yes, it is balanced', '₹50 left; No, it is not balanced', '₹150 left; Yes, it is balanced', '₹50 left; Yes, it is balanced'], 1, 1 FROM public.quizzes WHERE title = 'M2: Budgeting Essentials Quiz';
INSERT INTO public.quiz_questions (quiz_id, question, options, correct_option, points)
SELECT id, 'If you plan to save ₹50 but end up needing ₹50 extra for schl fees, what should you do? Give one example of cutting a non-essential expense.', ARRAY['Spend more on entertainment', 'Borrow money without planning', 'Cut a non-essential expense, like eating out or buying snacks', 'Stop budgeting completely'], 2, 1 FROM public.quizzes WHERE title = 'M2: Budgeting Essentials Quiz';
INSERT INTO public.quiz_questions (quiz_id, question, options, correct_option, points)
SELECT id, 'Which of these is a gd budgeting method?', ARRAY['Spend freely and save whatever is left', 'Decide on savings first, then plan for needs and wants', 'Not keeping track of money', 'Borrow money when needed.'], 1, 1 FROM public.quizzes WHERE title = 'M2: Budgeting Essentials Quiz';
INSERT INTO public.quiz_questions (quiz_id, question, options, correct_option, points)
SELECT id, 'What is one thing you can do if your planned spending is more than your income?', ARRAY['Ignore the problem', 'Increase spending', 'Reduce expenses or adjust your budget', 'Stop tracking money'], 2, 1 FROM public.quizzes WHERE title = 'M2: Budgeting Essentials Quiz';

-- Module M3
INSERT INTO public.lessons (id, title, difficulty, description, progress_percent, quizzes_total)
VALUES (gen_random_uuid(), 'M3: Saving and Financial Goals', 'Beginner', 'Saving means keeping part of your money for the future. It helps you reach bigger goals and handle emergencies. For example, saving ₹100 each month for a year gives ₹1200, which could buy a bicycle or help with exam fees. Having savings makes you and your family safer if something unexpected happens.
Key concepts and examples:
     Set Clear Goals: Decide why you are saving. Common goals for students: paying exam fees, buying a textbk, or even helping buy medicines. Tara wanted a new backpack costing ₹500; by saving just ₹50 per month, she had the money in 10 months.
     Start Small, Save Regularly: You don''t need a lot to start. Even putting aside ₹20 each week is gd practice. Treat saving like a regular expense. For example, if you earn ₹200 from a weekend job, immediately keep ₹50 in a separate place.
     Use a Safe Place: Keep savings where it won''t be spent easily. A piggy bank or an old jar at home works. A better way is a bank account (see Module 4)  -  banks often give interest on your savings, so your money grows a little. For instance, banks pay some extra money (interest) on your savings balance.
     Emergency Fund: Besides goals, keep a small emergency fund. This money is only used for urgent needs like a medical expense or sudden travel. It prevents you or your family from borrowing at high interest.
Financial Tip: Make saving a habit by treating it like a bill you must pay yourself. For example, if you get ₹1000, first "pay" ₹100 to your saving fund before using the rest.', 0, 1);
INSERT INTO public.quizzes (lesson_id, title, difficulty, total_marks, passing_marks)
SELECT id, 'M3: Saving and Financial Goals Quiz', difficulty, 5, 2 FROM public.lessons WHERE title = 'M3: Saving and Financial Goals';
INSERT INTO public.quiz_questions (quiz_id, question, options, correct_option, points)
SELECT id, 'Why is it important to have a saving goal? Give one example of a saving goal for a student.', ARRAY['So you can spend money faster', 'To know what you are saving for and stay motivated', 'To avoid using money', 'To impress others'], 1, 1 FROM public.quizzes WHERE title = 'M3: Saving and Financial Goals Quiz';
INSERT INTO public.quiz_questions (quiz_id, question, options, correct_option, points)
SELECT id, 'If you save ₹30 each week, how much will you save in 10 weeks? Is this enough for a ₹300 schl bag?', ARRAY['₹300; Yes, it is enough', '₹200; No, it is not enough', '₹250; No, it is not enough', '₹300; No, it is not enough'], 0, 1 FROM public.quizzes WHERE title = 'M3: Saving and Financial Goals Quiz';
INSERT INTO public.quiz_questions (quiz_id, question, options, correct_option, points)
SELECT id, 'What is an emergency fund? Why might it be useful for your family?', ARRAY['Money for shopping and entertainment', 'Extra money kept for unexpected expenses like medical bills', 'Money borrowed from others', 'Money used only for festivals'], 1, 1 FROM public.quizzes WHERE title = 'M3: Saving and Financial Goals Quiz';
INSERT INTO public.quiz_questions (quiz_id, question, options, correct_option, points)
SELECT id, 'Chse the best way to increase your savings:', ARRAY['Spend all your money now and hope to save later', 'Save a fixed part of any money you get', 'Only save if you have leftovers', 'None of these.'], 1, 1 FROM public.quizzes WHERE title = 'M3: Saving and Financial Goals Quiz';
INSERT INTO public.quiz_questions (quiz_id, question, options, correct_option, points)
SELECT id, 'True or False: Keeping money in a piggy bank or a bank account can earn extra money through interest.', ARRAY['True', 'False', 'Only piggy banks earn interest', 'Only adults earn interest'], 0, 1 FROM public.quizzes WHERE title = 'M3: Saving and Financial Goals Quiz';

-- Module M4
INSERT INTO public.lessons (id, title, difficulty, description, progress_percent, quizzes_total)
VALUES (gen_random_uuid(), 'M4: Understanding Credit', 'Beginner', 'Sometimes people need to borrow money (credit), for example, for education or emergencies. However, borrowing means debt  -  you must pay back more than you tk because of interest. Debt can be useful (like a small emergency loan from a bank with low interest), but it can also be dangerous if misused (like expensive high-interest loans).
Key concepts and examples:
     Borrowing Smartly: Only borrow for important reasons (college fees, health emergency). If you borrow ₹1000 at 10% interest, you must repay ₹1100. High-interest loans (like from some moneylenders) can trap people. For instance, if a shop owner loans money at 20% interest, you owe ₹1200 on a ₹1000 loan after a year. This can be hard to repay if income is low.
     Credit Cards and Small Loans: Adults sometimes use credit cards or small loans (EMI) for purchases. These should be used carefully  -  always plan how to repay them on time. If not, penalties and extra charges add up.
     Avoiding Bad Debt: Beware of schemes promising easy money, like pyramid scams or lottery frauds  -  these are common traps. Also, avoid spending borrowed money on wants; it should go to needs or investment (education, business).
     Reaching Financial Security: Gd credit (repaying loans on time) helps secure your family''s future. It can mean better loan terms later for things like a family business or home.
Financial Tip: Never borrow money from unreliable sources. If your family needs a loan, check if your bank offers small loans at reasonable interest, or lk for government schemes. Always ask: "Can I really repay this?" before borrowing.', 0, 1);
INSERT INTO public.quizzes (lesson_id, title, difficulty, total_marks, passing_marks)
SELECT id, 'M4: Understanding Credit Quiz', difficulty, 5, 2 FROM public.lessons WHERE title = 'M4: Understanding Credit';
INSERT INTO public.quiz_questions (quiz_id, question, options, correct_option, points)
SELECT id, 'What is interest in terms of borrowing?', ARRAY['Free money from the bank', 'The extra cost you pay when borrowing', 'A fee for saving money', 'A tax from the government.'], 1, 1 FROM public.quizzes WHERE title = 'M4: Understanding Credit Quiz';
INSERT INTO public.quiz_questions (quiz_id, question, options, correct_option, points)
SELECT id, 'If Mohan borrows ₹500 at 10% annual interest, how much must he pay back after one year?', ARRAY['₹500', '₹510', '₹550', '₹600'], 2, 1 FROM public.quizzes WHERE title = 'M4: Understanding Credit Quiz';
INSERT INTO public.quiz_questions (quiz_id, question, options, correct_option, points)
SELECT id, 'Which choice is the safest way to borrow money?', ARRAY['Borrow from a bank at low interest', 'Borrow from a loan shark at high interest', 'Borrow and not plan to repay', 'Borrow to gamble.'], 0, 1 FROM public.quizzes WHERE title = 'M4: Understanding Credit Quiz';
INSERT INTO public.quiz_questions (quiz_id, question, options, correct_option, points)
SELECT id, 'Name one reason why borrowing money can be risky for your family.', ARRAY['It always increases income', 'It can create debt that is hard to repay', 'It makes budgeting easier', 'It guarantees profit'], 1, 1 FROM public.quizzes WHERE title = 'M4: Understanding Credit Quiz';
INSERT INTO public.quiz_questions (quiz_id, question, options, correct_option, points)
SELECT id, 'True or False: It''s okay to buy lottery tickets with money you borrowed, because the lottery is a quick way to earn more.', ARRAY['True', 'False', 'Only for adults', 'Only if you borrow a small amount'], 1, 1 FROM public.quizzes WHERE title = 'M4: Understanding Credit Quiz';

-- Module M5
INSERT INTO public.lessons (id, title, difficulty, description, progress_percent, quizzes_total)
VALUES (gen_random_uuid(), 'M5: Banking Basics', 'Beginner', 'A bank is a safe place to keep your money. It also helps you with transactions (sending/receiving money) and can pay you a little interest on your savings. In India, the government''s Jan-Dhan Yojana lets any person (even teens) open a free basic bank account with zero minimum balance. This means you can put your savings in the bank and earn interest without having to pay fees.
Key concepts and examples:
     Types of Accounts: The most common is a savings account. You deposit money and can withdraw it when needed. Some banks also offer special student accounts or no-frills accounts (like Jan-Dhan). Example: Anu opened a Jan-Dhan account with ₹100 deposit; later, her friends transfer her earned money to this account via a phone app.
     Debit Card and Mobile Banking: Banks often give a debit card and let you do mobile or online banking. With these, you can pay bills or buy online without cash. Always keep your card/PIN secret. For example, Varun pays his schl fees online through mobile banking, which is safer than carrying cash.
     Interest on Savings: Banks reward you for keeping money by adding a small percentage as interest. For example, if the bank gives 4% per year, ₹100 saved becomes ₹104 in a year. While small, this means your money grows without extra effort.
     Avoid Risk: Unlike hiding cash at home (which can be lost or stolen), a bank keeps money secure. Also, if you must borrow later, a gd banking history helps you qualify for small loans at lower interest rates (see Module 5).
Financial Tip: Open a basic bank account if you can (ask an adult for help). Whenever you get money, deposit a part of it instead of keeping all cash. This way, your savings earn interest safely.', 0, 1);
INSERT INTO public.quizzes (lesson_id, title, difficulty, total_marks, passing_marks)
SELECT id, 'M5: Banking Basics Quiz', difficulty, 5, 2 FROM public.lessons WHERE title = 'M5: Banking Basics';
INSERT INTO public.quiz_questions (quiz_id, question, options, correct_option, points)
SELECT id, 'Name one benefit of keeping your money in a bank account instead of at home.', ARRAY['Money can be lost or stolen easily', 'Banks help keep money safe and secure', 'Money cannot be withdrawn when needed', 'Banks charge money for keeping cash'], 1, 1 FROM public.quizzes WHERE title = 'M5: Banking Basics Quiz';
INSERT INTO public.quiz_questions (quiz_id, question, options, correct_option, points)
SELECT id, 'What does "no minimum balance" mean in a bank account? Why is this helpful? (Hint: see PMJDY rules.)', ARRAY['You must keep a fixed amount of money always', 'You cannot withdraw money', 'You can keep even ₹0 in the account without penalty', 'The bank controls your spending'], 2, 1 FROM public.quizzes WHERE title = 'M5: Banking Basics Quiz';
INSERT INTO public.quiz_questions (quiz_id, question, options, correct_option, points)
SELECT id, 'If a bank pays 5% interest per year and you save ₹500, how much money (approximately) will you have after one year?', ARRAY['₹500', '₹505', '₹525', '₹550'], 2, 1 FROM public.quizzes WHERE title = 'M5: Banking Basics Quiz';
INSERT INTO public.quiz_questions (quiz_id, question, options, correct_option, points)
SELECT id, 'Give an example of a secure transaction with a debit card or mobile banking that a student might use.', ARRAY['Sharing your ATM PIN with friends', 'Paying at a shop using a debit card with PIN verification', 'Writing your password in public', 'Using public Wi-Fi to share bank details'], 1, 1 FROM public.quizzes WHERE title = 'M5: Banking Basics Quiz';
INSERT INTO public.quiz_questions (quiz_id, question, options, correct_option, points)
SELECT id, 'True or False: Paying utility bills (electricity, phone) through bank mobile app can be safer than paying in cash.', ARRAY['True', 'False', 'Only for large payments', 'Only for adults'], 0, 1 FROM public.quizzes WHERE title = 'M5: Banking Basics Quiz';

-- Module M6
INSERT INTO public.lessons (id, title, difficulty, description, progress_percent, quizzes_total)
VALUES (gen_random_uuid(), 'M6: Managing Expenses', 'Beginner', 'Managing expenses means controlling where money goes after it is earned. For many Parivaar families, income is limited and irregular, which makes expense management essential for financial survival. When expenses are not tracked, money is often spent on short-term wants instead of long-term needs, leading to debt and instability.
Expenses fall into two main categories:
  1.   Fixed expenses  -  costs that remain mostly the same each month (schl fees, rent, electricity minimum charges).
  2.   Variable expenses  -  costs that change depending on usage or choices (fd, transport, phone data, snacks).
A common problem is leakage spending  --  small daily expenses that seem insignificant but add up over time. For example, spending ₹20 daily on snacks results in ₹600 per month, which could otherwise cover schl materials or savings.
Effective expense management involves:
     Recording every expense, even small ones.
     Comparing expenses to income to ensure spending does not exceed earnings.
     Identifying unnecessary spending and redirecting money to savings or essential needs.
From an economic security perspective, managing expenses reduces reliance on borrowing and improves household resilience. When families control spending, they can allocate funds toward education, healthcare, and emergencies  --  breaking cycles of poverty.
Financial Tip
Before making any purchase, pause and ask: "Is this a need today, or can it wait?" Delaying even small purchases often reveals they were unnecessary.', 0, 1);
INSERT INTO public.quizzes (lesson_id, title, difficulty, total_marks, passing_marks)
SELECT id, 'M6: Managing Expenses Quiz', difficulty, 5, 2 FROM public.lessons WHERE title = 'M6: Managing Expenses';
INSERT INTO public.quiz_questions (quiz_id, question, options, correct_option, points)
SELECT id, 'Explain the difference between fixed and variable expenses using one household example of each.', ARRAY['Fixed: Snacks, Variable: Rent', 'Fixed: Electricity bill, Variable: Schl fees', 'Fixed: House rent, Variable: Monthly groceries', 'Fixed: Movie tickets, Variable: Mobile phone'], 2, 1 FROM public.quizzes WHERE title = 'M6: Managing Expenses Quiz';
INSERT INTO public.quiz_questions (quiz_id, question, options, correct_option, points)
SELECT id, 'If a student earns ₹1,200 per month and spends ₹1,300, what financial problem does this create and why?', ARRAY['Profit, because spending more increases income', 'Balanced budget, because amounts are close', 'Deficit, because expenses are higher than income', 'Savings, because money is left over'], 2, 1 FROM public.quizzes WHERE title = 'M6: Managing Expenses Quiz';
INSERT INTO public.quiz_questions (quiz_id, question, options, correct_option, points)
SELECT id, 'How can tracking daily expenses help prevent long-term financial stress?', ARRAY['By encouraging more spending', 'By helping identify where money is wasted and control spending', 'By avoiding income completely', 'By borrowing more money'], 1, 1 FROM public.quizzes WHERE title = 'M6: Managing Expenses Quiz';
INSERT INTO public.quiz_questions (quiz_id, question, options, correct_option, points)
SELECT id, 'Identify two examples of leakage spending common among students.', ARRAY['Paying schl fees and buying textbks', 'Saving money and investing', 'Frequent snacks and unnecessary mobile data packs', 'Paying rent and electricity bills'], 2, 1 FROM public.quizzes WHERE title = 'M6: Managing Expenses Quiz';
INSERT INTO public.quiz_questions (quiz_id, question, options, correct_option, points)
SELECT id, 'Why is managing expenses more important for low-income households than high-income households?', ARRAY['They earn unlimited money', 'Small overspending can cause serious financial problems', 'They do not need budgets', 'They spend only on luxury items'], 1, 1 FROM public.quizzes WHERE title = 'M6: Managing Expenses Quiz';

-- Module M7
INSERT INTO public.lessons (id, title, difficulty, description, progress_percent, quizzes_total)
VALUES (gen_random_uuid(), 'M7: Emergency Funds', 'Beginner', 'An emergency fund is money set aside specifically for unexpected situations, such as medical emergencies, sudden job loss, urgent home repairs, or schl-related expenses. For families with limited income, emergencies can quickly push households into high-interest debt if no savings are available.
Emergency funds protect families from:
     Borrowing from moneylenders
     Selling essential assets
     Missing schl fees or rent payments
A strong emergency fund creates financial resilience, meaning the ability to recover from shocks without long-term damage.
Financial experts recommend saving 3 - 6 months of essential expenses, but for low-income households, even ₹5,000 - ₹10,000 can make a significant difference. The key is consistency, not amount.
Emergency funds should be:
     Liquid: Easily accessible (bank savings account)
     Separate: Not mixed with daily spending money
     Safe: Not invested in risky assets like stocks
For example, if a parent falls sick and cannot work for two weeks, an emergency fund can cover groceries and transport without borrowing.
ParivaarPocket encourages emergency saving by:
     Setting savings goals
     Locking funds digitally
     Rewarding consistency with points
Financial Tip
👉 Automate saving: Set aside a small fixed amount immediately when money is received, even before spending.', 0, 1);
INSERT INTO public.quizzes (lesson_id, title, difficulty, total_marks, passing_marks)
SELECT id, 'M7: Emergency Funds Quiz', difficulty, 5, 2 FROM public.lessons WHERE title = 'M7: Emergency Funds';
INSERT INTO public.quiz_questions (quiz_id, question, options, correct_option, points)
SELECT id, 'What is an emergency fund and why is it important for low-income households?', ARRAY['Money kept for festivals and celebrations', 'Money saved for daily shopping', 'Money set aside for unexpected expenses like illness or job loss', 'Money borrowed from others'], 2, 1 FROM public.quizzes WHERE title = 'M7: Emergency Funds Quiz';
INSERT INTO public.quiz_questions (quiz_id, question, options, correct_option, points)
SELECT id, 'Give two examples of situations where an emergency fund would be used.', ARRAY['Buying new clothes and gadgets', 'Paying schl fees late and shopping online', 'Medical emergencies and urgent house repairs', 'Vacations and entertainment'], 2, 1 FROM public.quizzes WHERE title = 'M7: Emergency Funds Quiz';
INSERT INTO public.quiz_questions (quiz_id, question, options, correct_option, points)
SELECT id, 'Why should emergency funds be kept separate from daily spending money?', ARRAY['To spend it faster', 'To avoid using it for regular expenses', 'To show others how much money you have', 'To increase daily spending'], 1, 1 FROM public.quizzes WHERE title = 'M7: Emergency Funds Quiz';
INSERT INTO public.quiz_questions (quiz_id, question, options, correct_option, points)
SELECT id, 'True or False: Emergency funds should be invested in stocks for higher returns. Explain.', ARRAY['True', 'False', 'Only for rich families', 'Only for adults'], 1, 1 FROM public.quizzes WHERE title = 'M7: Emergency Funds Quiz';
INSERT INTO public.quiz_questions (quiz_id, question, options, correct_option, points)
SELECT id, 'If a family saves ₹500 per month, how long will it take to build a ₹6,000 emergency fund?', ARRAY['6 months', '10 months', '12 months', '15 months'], 2, 1 FROM public.quizzes WHERE title = 'M7: Emergency Funds Quiz';

-- Module M8
INSERT INTO public.lessons (id, title, difficulty, description, progress_percent, quizzes_total)
VALUES (gen_random_uuid(), 'M8: Investments Overview', 'Beginner', 'Investing means using money today to try to earn more in future. For students, investments can be simple: a bank Fixed Deposit (FD) or even education (investing time and money in learning new skills to increase future income).
Key concepts and examples:
     Safe Investments: The simplest is a Fixed Deposit (FD) at a bank. You deposit money for a fixed time (say 1 year) and the bank pays a bit more interest (often higher than a savings account). For instance, depositing ₹1000 in a 1-year FD at 6% returns ₹1060 after a year.
     Caution with Risk: Some ask you to invest in stocks or schemes promising fast money. These are risky. Always ask: "Do I really understand this?" For students, it''s safer to stick to known methods (bank accounts, gold, government bonds if any) until you learn more.
     Invest in Yourself: The best investment is often education. Spending money on bks or learning a new skill can pay off later with higher earning jobs. For example, taking a short computer course might cost ₹2000 but could help you earn more in tuitions or jobs later.
     Avoid Hasty Decisions: Never put all your money into one thing. Start small. If a neighbor suggests you "double your money in one week" scheme, it''s probably a scam.
Financial Tip: Even as a student, ask a bank about small Recurring Deposits (RD) where you can deposit ₹100 - 200 every month. This forces you to save and earns interest on the total.', 0, 1);
INSERT INTO public.quizzes (lesson_id, title, difficulty, total_marks, passing_marks)
SELECT id, 'M8: Investments Overview Quiz', difficulty, 5, 2 FROM public.lessons WHERE title = 'M8: Investments Overview';
INSERT INTO public.quiz_questions (quiz_id, question, options, correct_option, points)
SELECT id, 'What is a Fixed Deposit (FD) in a bank?', ARRAY['Taking a loan', 'A savings account with a fixed interest rate', 'Free money from government', 'Buying stocks.'], 1, 1 FROM public.quizzes WHERE title = 'M8: Investments Overview Quiz';
INSERT INTO public.quiz_questions (quiz_id, question, options, correct_option, points)
SELECT id, 'If you put ₹500 in a 1-year FD at 8% interest, how much will you have after 1 year? (Ignore compounding)', ARRAY['₹504', '₹540', '₹580', '₹600'], 0, 1 FROM public.quizzes WHERE title = 'M8: Investments Overview Quiz';
INSERT INTO public.quiz_questions (quiz_id, question, options, correct_option, points)
SELECT id, 'Which is generally safer for a first-time investor?', ARRAY['Gambling', 'Bank FD or RD', 'A stranger''s get-rich-quick plan', 'None of these.'], 0, 1 FROM public.quizzes WHERE title = 'M8: Investments Overview Quiz';
INSERT INTO public.quiz_questions (quiz_id, question, options, correct_option, points)
SELECT id, 'True or False: Spending money on bks and education can be considered an investment.', ARRAY['True', 'False'], 0, 1 FROM public.quizzes WHERE title = 'M8: Investments Overview Quiz';
INSERT INTO public.quiz_questions (quiz_id, question, options, correct_option, points)
SELECT id, 'Name one thing you should always check before investing money (hint: risk, knowledge, time).', ARRAY['The color of the company logo', 'The risk involved', 'How popular the investment sounds', 'Whether your friends are investing'], 0, 1 FROM public.quizzes WHERE title = 'M8: Investments Overview Quiz';

-- Module M9
INSERT INTO public.lessons (id, title, difficulty, description, progress_percent, quizzes_total)
VALUES (gen_random_uuid(), 'M9: Stocks and Bonds', 'Beginner', 'Stocks and bonds are investment tls that allow individuals to grow their money over time, but they work very differently.
Stocks
Buying a stock means owning a small part of a company. If the company grows and becomes more profitable, the value of the stock increases. However, if the company performs prly, the value can fall.
     Higher risk
     Higher potential returns
     Prices change daily
Bonds
Bonds are loans given to governments or companies. When you buy a bond, you receive regular interest payments, and your money is returned after a fixed period.
     Lower risk
     Stable returns
     Suitable for cautious investors
For underprivileged families, capital protection is important. This means not risking money needed for fd, rent, or education. Stocks are better for long-term wealth creation, while bonds help preserve money.
A balanced investor uses both:
     Stocks for growth
     Bonds for stability
ParivaarPocket introduces investing concepts carefully, ensuring students understand risk before reward.
Financial Tip
👉 Never invest money you may need within the next year.', 0, 1);
INSERT INTO public.quizzes (lesson_id, title, difficulty, total_marks, passing_marks)
SELECT id, 'M9: Stocks and Bonds Quiz', difficulty, 5, 2 FROM public.lessons WHERE title = 'M9: Stocks and Bonds';
INSERT INTO public.quiz_questions (quiz_id, question, options, correct_option, points)
SELECT id, 'What does owning a stock represent?', ARRAY['You have lent money to a company', 'You own a small part of the company', 'You are guaranteed a fixed income', 'You are the company CEO'], 0, 1 FROM public.quizzes WHERE title = 'M9: Stocks and Bonds Quiz';
INSERT INTO public.quiz_questions (quiz_id, question, options, correct_option, points)
SELECT id, 'State one key difference between stocks and bonds.', ARRAY['Stocks are loans, bonds are ownership', 'Stocks represent ownership; bonds are loans to the company/government', 'Stocks are risk-free; bonds are risky', 'Stocks and bonds are the same'], 0, 1 FROM public.quizzes WHERE title = 'M9: Stocks and Bonds Quiz';
INSERT INTO public.quiz_questions (quiz_id, question, options, correct_option, points)
SELECT id, 'Why are bonds considered less risky than stocks?', ARRAY['They are always profitable', 'They can be sold anytime', 'They provide fixed interest and principal repayment', 'They are cheaper than stocks'], 0, 1 FROM public.quizzes WHERE title = 'M9: Stocks and Bonds Quiz';
INSERT INTO public.quiz_questions (quiz_id, question, options, correct_option, points)
SELECT id, 'True or False: Stock prices always increase over time. Explain.', ARRAY['True', 'False'], 0, 1 FROM public.quizzes WHERE title = 'M9: Stocks and Bonds Quiz';
INSERT INTO public.quiz_questions (quiz_id, question, options, correct_option, points)
SELECT id, 'Which investment is more suitable for emergency savings: stocks or bonds? Why?', ARRAY['Stocks  -  because they give higher returns', 'Bonds', 'Both are equally suitable', 'None  -  keep cash under the mattress'], 0, 1 FROM public.quizzes WHERE title = 'M9: Stocks and Bonds Quiz';
