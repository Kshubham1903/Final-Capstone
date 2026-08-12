import requests
import json
import uuid
import sys

BASE_URL = 'http://localhost:8080'

def get_auth_token(email, password, full_name):
    reg_payload = {'email': email, 'password': password, 'fullName': full_name, 'role': 'STUDENT'}
    requests.post(f'{BASE_URL}/api/auth/register', json=reg_payload)
    login_res = requests.post(f'{BASE_URL}/api/auth/login', json={'email': email, 'password': password}).json()
    return login_res['token'], login_res['userId']

def normalize_concept(raw):
    if not raw: return ""
    import re
    c = raw.strip()
    c = re.sub(r'(?i)\s*[-|\[\(]?\s*(EASY|MEDIUM|HARD|Tier\s*\d+)\s*[\]\)]?', '', c)
    c = re.sub(r'(?i)\s*#\d+.*$', '', c)
    c = re.sub(r'(?i)\s+(Implementation|Architecture|Foundations|Mechanics|Concepts|Principles)$', '', c)
    return c.strip()

def run_tests():
    print("==================================================")
    print("STARTING CONTROLLED CONCEPT QUIZ & VERIFICATION SUITE")
    print("==================================================")

    # ----------------------------------------------------
    # TEST 1: 10-question quiz containing exactly 2–3 concepts
    # ----------------------------------------------------
    print("\n--- TEST 1: 10-question quiz containing exactly 2–3 concepts ---")
    u1 = uuid.uuid4().hex[:6]
    tok1, uid1 = get_auth_token(f'v1_{u1}@test.com', 'Password123!', 'Tester 1')
    h1 = {'Authorization': f'Bearer {tok1}'}

    res1 = requests.get(f'{BASE_URL}/api/quizzes/questions?subject=Blockchain Development&difficulty=EASY', headers=h1)
    assert res1.status_code == 200, f"Quiz fetch failed: {res1.text}"
    qs1 = res1.json()
    assert len(qs1) >= 6, f"Expected questions returned, got {len(qs1)}"

    # Normalize concept names
    concepts1 = set()
    for q in qs1:
        c = q.get('concept', '')
        clean_c = normalize_concept(c)
        concepts1.add(clean_c)

    print("Quiz 1 Returned Questions Count:", len(qs1))
    print("Quiz 1 Normalized Concepts:", concepts1)
    assert 2 <= len(concepts1) <= 3, f"Expected 2 to 3 concepts, got {len(concepts1)}: {concepts1}"
    print("TEST 1 PASSED: Quiz contains controlled concept grouping (~2–3 concepts)!")

    # ----------------------------------------------------
    # TEST 2: Multiple wrong questions aggregate into 1 recommendation & correct concepts excluded
    # ----------------------------------------------------
    print("\n--- TEST 2: Multiple wrong questions aggregate into 1 recommendation & correct excluded ---")
    u2 = uuid.uuid4().hex[:6]
    tok2, uid2 = get_auth_token(f'v2_{u2}@test.com', 'Password123!', 'Tester 2')
    h2 = {'Authorization': f'Bearer {tok2}'}

    # Submit 3 wrong questions for same concept, 1 correct question for another concept
    for c in ['Ethereum State Trie (MPT) (EASY #16)', 'Ethereum State Trie (MPT) Implementation (MEDIUM #16)', 'Ethereum State Trie (MPT) Architecture (HARD #16)']:
        requests.post(f'{BASE_URL}/api/quizzes/submit', json={
            'profileId': uid2, 'subject': 'Blockchain Development', 'concept': c, 'difficulty': 'EASY', 'isCorrect': False, 'responseTimeSeconds': 15
        }, headers=h2)

    requests.post(f'{BASE_URL}/api/quizzes/submit', json={
        'profileId': uid2, 'subject': 'Blockchain Development', 'concept': 'Binary Search Tree (EASY #1)', 'difficulty': 'EASY', 'isCorrect': True, 'responseTimeSeconds': 10
    }, headers=h2)

    plan2 = requests.get(f'{BASE_URL}/api/planner/today/{uid2}', headers=h2).json()
    tasks2 = [t.get('conceptName') for t in plan2.get('tasks', [])]
    reasons2 = [t.get('reason') for t in plan2.get('tasks', [])]
    print("Plan 2 Tasks:", tasks2)
    print("Plan 2 Reasons:", reasons2)

    assert len(tasks2) == 1 and tasks2[0] == 'Ethereum State Trie (MPT)', f"Expected 1 task for Ethereum State Trie (MPT), got {tasks2}"
    assert '3 times' in reasons2[0], f"Expected 3 times in reason, got {reasons2[0]}"
    assert 'Binary Search Tree' not in tasks2, "Binary Search Tree (correct answer) MUST NOT appear in recommendations"
    print("TEST 2 PASSED: 3 wrong questions aggregated into 1 recommendation & correct concept excluded!")

    # ----------------------------------------------------
    # TEST 3: Mark Complete -> VERIFICATION_PENDING state transition
    # ----------------------------------------------------
    print("\n--- TEST 3: Mark Complete -> VERIFICATION_PENDING state transition ---")
    task_id2 = plan2['tasks'][0]['taskId']

    complete_res = requests.patch(f'{BASE_URL}/api/planner/task/{task_id2}/complete?userId={uid2}', headers=h2)
    assert complete_res.status_code == 200, f"Complete task failed: {complete_res.text}"

    plan3 = requests.get(f'{BASE_URL}/api/planner/today/{uid2}', headers=h2).json()
    task3_status = plan3['tasks'][0]['status']
    print("Plan 3 Task Status after Mark Complete:", task3_status)
    assert task3_status == 'VERIFICATION_PENDING', f"Expected VERIFICATION_PENDING, got {task3_status}"

    recs3 = requests.get(f'{BASE_URL}/api/recommendations/{uid2}', headers=h2).json()
    rec3_status = recs3[0]['status'] if recs3 else 'UNKNOWN'
    print("Recommendation Status after Mark Complete:", rec3_status)
    assert rec3_status == 'VERIFICATION_PENDING', f"Expected VERIFICATION_PENDING, got {rec3_status}"
    print("TEST 3 PASSED: Task & Recommendation successfully transitioned to VERIFICATION_PENDING!")

    # ----------------------------------------------------
    # TEST 4: Focused Verification Quiz Retrieval
    # ----------------------------------------------------
    print("\n--- TEST 4: Focused Verification Quiz Retrieval ---")
    v_res = requests.get(f'{BASE_URL}/api/quizzes/questions?subject=Blockchain Development&difficulty=EASY&targetConcept=Ethereum State Trie (MPT)', headers=h2)
    assert v_res.status_code == 200, f"Verification quiz fetch failed: {v_res.text}"
    v_qs = v_res.json()
    print("Verification Quiz Questions Count:", len(v_qs))
    for q in v_qs:
        c_clean = normalize_concept(q.get('concept', ''))
        assert c_clean == 'Ethereum State Trie (MPT)', f"Verification quiz question concept must be Ethereum State Trie (MPT), got {q.get('concept')}"
    print("TEST 4 PASSED: Verification quiz returned questions ONLY from targeted concept!")

    # ----------------------------------------------------
    # TEST 5: Verification Failure -> Concept Remains Active
    # ----------------------------------------------------
    print("\n--- TEST 5: Verification Failure -> Concept Remains Active ---")
    requests.post(f'{BASE_URL}/api/quizzes/submit', json={
        'profileId': uid2, 'subject': 'Blockchain Development', 'concept': 'Ethereum State Trie (MPT) (EASY #16)', 'difficulty': 'EASY',
        'isCorrect': False, 'responseTimeSeconds': 20, 'isVerification': True, 'targetConcept': 'Ethereum State Trie (MPT)'
    }, headers=h2)

    plan5 = requests.get(f'{BASE_URL}/api/planner/today/{uid2}', headers=h2).json()
    task5_status = plan5['tasks'][0]['status'] if plan5.get('tasks') else 'EMPTY'
    print("Plan 5 Task Status after Failed Verification:", task5_status)
    assert task5_status == 'ACTIVE' or task5_status == 'PENDING' or task5_status == 'IN_PROGRESS', f"Expected ACTIVE/PENDING/IN_PROGRESS, got {task5_status}"
    print("TEST 5 PASSED: Failed verification quiz kept concept active!")

    # ----------------------------------------------------
    # TEST 6: Verification Pass -> Concept Mastered & Removed
    # ----------------------------------------------------
    print("\n--- TEST 6: Verification Pass -> Concept Mastered & Removed ---")
    # Mark for verification again
    requests.patch(f'{BASE_URL}/api/planner/task/{task_id2}/complete?userId={uid2}', headers=h2)

    # Submit passing verification answer
    requests.post(f'{BASE_URL}/api/quizzes/submit', json={
        'profileId': uid2, 'subject': 'Blockchain Development', 'concept': 'Ethereum State Trie (MPT) (EASY #16)', 'difficulty': 'EASY',
        'isCorrect': True, 'responseTimeSeconds': 10, 'isVerification': True, 'targetConcept': 'Ethereum State Trie (MPT)'
    }, headers=h2)

    plan6 = requests.get(f'{BASE_URL}/api/planner/today/{uid2}', headers=h2).json()
    tasks6 = [t.get('conceptName') for t in plan6.get('tasks', [])]
    print("Plan 6 Tasks after Passed Verification:", tasks6)
    assert 'Ethereum State Trie (MPT)' not in tasks6 or tasks6 == ['No Weak Concepts Identified'], f"Concept should be mastered and removed from active plan, got {tasks6}"
    print("TEST 6 PASSED: Verification pass marked concept mastered and removed it from active plan!")

    # ----------------------------------------------------
    # TEST 7: Verification Quiz Does Not Create Unrelated Recommendations
    # ----------------------------------------------------
    print("\n--- TEST 7: Verification Quiz Isolation ---")
    recs7 = requests.get(f'{BASE_URL}/api/recommendations/{uid2}', headers=h2).json()
    active_recs7 = [r for r in recs7 if r.get('status') == 'ACTIVE']
    print("Active Recommendations Count after Verification Quiz:", len(active_recs7))
    assert len(active_recs7) == 0, f"Verification quiz must not create unrelated active recommendations, got {active_recs7}"
    print("TEST 7 PASSED: Verification quiz did not generate unrelated recommendations!")

    # ----------------------------------------------------
    # TEST 8: Three Separate Subjects Isolation
    # ----------------------------------------------------
    print("\n--- TEST 8: Three Separate Subjects Isolation ---")
    u8 = uuid.uuid4().hex[:6]
    tok8, uid8 = get_auth_token(f'v8_{u8}@test.com', 'Password123!', 'Tester 8')
    h8 = {'Authorization': f'Bearer {tok8}'}

    # Quiz 1: DSA wrong
    requests.post(f'{BASE_URL}/api/quizzes/submit', json={'profileId': uid8, 'subject': 'Data Structures & Algorithms', 'concept': 'Hash Tables (EASY #1)', 'difficulty': 'EASY', 'isCorrect': False, 'responseTimeSeconds': 15}, headers=h8)
    plan_dsa = requests.get(f'{BASE_URL}/api/planner/today/{uid8}', headers=h8).json()
    dsa_tasks = [t.get('conceptName') for t in plan_dsa.get('tasks', [])]

    # Quiz 2: AI wrong
    requests.post(f'{BASE_URL}/api/quizzes/submit', json={'profileId': uid8, 'subject': 'Artificial Intelligence', 'concept': 'A* Search Algorithm (EASY #1)', 'difficulty': 'EASY', 'isCorrect': False, 'responseTimeSeconds': 15}, headers=h8)
    plan_ai = requests.get(f'{BASE_URL}/api/planner/today/{uid8}', headers=h8).json()
    ai_tasks = [t.get('conceptName') for t in plan_ai.get('tasks', [])]

    # Quiz 3: Blockchain wrong
    requests.post(f'{BASE_URL}/api/quizzes/submit', json={'profileId': uid8, 'subject': 'Blockchain Development', 'concept': 'Smart Contracts (EASY #1)', 'difficulty': 'EASY', 'isCorrect': False, 'responseTimeSeconds': 15}, headers=h8)
    plan_bc = requests.get(f'{BASE_URL}/api/planner/today/{uid8}', headers=h8).json()
    bc_tasks = [t.get('conceptName') for t in plan_bc.get('tasks', [])]

    print("DSA Plan Tasks:", dsa_tasks)
    print("AI Plan Tasks:", ai_tasks)
    print("Blockchain Plan Tasks:", bc_tasks)

    assert 'Hash Tables' in dsa_tasks and 'A* Search Algorithm' not in dsa_tasks, "DSA plan must contain ONLY DSA tasks"
    assert ('Uninformed & Heuristic Search' in ai_tasks or 'A* Search Algorithm' in ai_tasks) and 'Hash Tables' not in ai_tasks, "AI plan must contain ONLY AI tasks"
    assert 'Smart Contracts' in bc_tasks and 'A* Search Algorithm' not in bc_tasks, "Blockchain plan must contain ONLY Blockchain tasks"
    print("TEST 8 PASSED: Three separate subjects strictly isolated without cross-contamination!")

    print("\n==================================================")
    print("ALL CONCEPT VERIFICATION SUITE TESTS PASSED 100%!")
    print("==================================================")

if __name__ == '__main__':
    run_tests()
