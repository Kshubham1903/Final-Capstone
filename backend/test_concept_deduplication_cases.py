import requests
import json
import uuid

BASE_URL = "http://localhost:8080"

def get_auth_token(email, password, full_name):
    reg_payload = {
        "email": email,
        "password": password,
        "fullName": full_name,
        "role": "STUDENT"
    }
    requests.post(f"{BASE_URL}/api/auth/register", json=reg_payload)

    login_payload = {
        "email": email,
        "password": password
    }
    res = requests.post(f"{BASE_URL}/api/auth/login", json=login_payload)
    if res.status_code == 200:
        data = res.json()
        return data.get("token"), data.get("userId")
    else:
        raise Exception(f"Login failed: {res.text}")

def run_targeted_deduplication_tests():
    print("=" * 50)
    print("RUNNING TARGETED CONCEPT-LEVEL DEDUPLICATION TESTS")
    print("=" * 50)

    u_id = uuid.uuid4().hex[:6]
    email = f"student_dedup_{u_id}@edupilot.test"
    token, uid = get_auth_token(email, "Password123!", "Deduplication Student")
    headers = {"Authorization": f"Bearer {token}", "Content-Type": "application/json"}

    # Case 1: Q1, Q2, Q3 wrong for Artificial Intelligence + Game Theory & Constraint Satisfaction
    print("\n--- CASE 1: 3 Different wrong questions for SAME subject + SAME concept ---")
    questions_c1 = [
        {"profileId": uid, "subject": "Artificial Intelligence", "concept": "Game Theory & Constraint Satisfaction", "difficulty": "EASY", "isCorrect": False, "responseTimeSeconds": 10, "questionId": "q101", "questionText": "What is Minimax in Game Theory?"},
        {"profileId": uid, "subject": "Artificial Intelligence", "concept": "Game Theory & Constraint Satisfaction", "difficulty": "MEDIUM", "isCorrect": False, "responseTimeSeconds": 12, "questionId": "q102", "questionText": "What is Alpha-Beta Pruning?"},
        {"profileId": uid, "subject": "Artificial Intelligence", "concept": "Game Theory & Constraint Satisfaction", "difficulty": "HARD", "isCorrect": False, "responseTimeSeconds": 15, "questionId": "q103", "questionText": "What is CSP Backtracking?"}
    ]

    for q in questions_c1:
        res = requests.post(f"{BASE_URL}/api/quizzes/submit", json=q, headers=headers)
        assert res.status_code == 200, f"Submit failed: {res.text}"

    recs1 = requests.get(f"{BASE_URL}/api/recommendations/{uid}", headers=headers).json()
    plan1 = requests.get(f"{BASE_URL}/api/planner/today/{uid}", headers=headers).json()
    tasks1 = plan1.get("tasks", [])

    print(f"Active Recommendations Count: {len(recs1)}")
    print(f"Today's Plan Tasks Count: {len(tasks1)}")
    print(f"Tasks: {[t.get('conceptName') for t in tasks1]}")
    assert len(recs1) == 1, f"Expected 1 active recommendation, got {len(recs1)}"
    assert len(tasks1) == 1, f"Expected 1 plan task, got {len(tasks1)}"
    assert tasks1[0].get("conceptName") == "Game Theory & Constraint Satisfaction"
    print("CASE 1 PASSED: 3 wrong questions for same concept produced EXACTLY 1 card!")

    # Case 2: Repeat quiz attempt with 2 more wrong questions for SAME concept
    print("\n--- CASE 2: Repeat attempt with more wrong questions for SAME concept ---")
    questions_c2 = [
        {"profileId": uid, "subject": "Artificial Intelligence", "concept": "Game Theory & Constraint Satisfaction", "difficulty": "EASY", "isCorrect": False, "responseTimeSeconds": 10, "questionId": "q104", "questionText": "What is Arc Consistency in CSP?"},
        {"profileId": uid, "subject": "Artificial Intelligence", "concept": "Game Theory & Constraint Satisfaction", "difficulty": "MEDIUM", "isCorrect": False, "responseTimeSeconds": 11, "questionId": "q105", "questionText": "What is Nash Equilibrium?"}
    ]

    for q in questions_c2:
        res = requests.post(f"{BASE_URL}/api/quizzes/submit", json=q, headers=headers)
        assert res.status_code == 200

    recs2 = requests.get(f"{BASE_URL}/api/recommendations/{uid}", headers=headers).json()
    plan2 = requests.get(f"{BASE_URL}/api/planner/today/{uid}", headers=headers).json()
    tasks2 = plan2.get("tasks", [])

    print(f"Active Recommendations Count: {len(recs2)}")
    print(f"Today's Plan Tasks Count: {len(tasks2)}")
    print(f"Updated Task Reason: {tasks2[0].get('reason')}")
    assert len(recs2) == 1, f"Expected 1 active recommendation after repeat attempt, got {len(recs2)}"
    assert len(tasks2) == 1, f"Expected 1 plan task after repeat attempt, got {len(tasks2)}"
    print("CASE 2 PASSED: Repeat quiz attempt updated existing recommendation and kept STILL EXACTLY 1 card!")

    # Case 3: Two DIFFERENT concepts in Artificial Intelligence
    print("\n--- CASE 3: Two DIFFERENT concepts in SAME subject ---")
    q_diff_concept = {"profileId": uid, "subject": "Artificial Intelligence", "concept": "Uninformed & Heuristic Search", "difficulty": "EASY", "isCorrect": False, "responseTimeSeconds": 10}
    requests.post(f"{BASE_URL}/api/quizzes/submit", json=q_diff_concept, headers=headers)

    plan3 = requests.get(f"{BASE_URL}/api/planner/today/{uid}", headers=headers).json()
    tasks3 = plan3.get("tasks", [])
    task_concepts3 = [t.get("conceptName") for t in tasks3]

    print(f"Today's Plan Tasks Count: {len(tasks3)}")
    print(f"Tasks: {task_concepts3}")
    assert len(tasks3) == 2, f"Expected 2 plan tasks for 2 different concepts, got {len(tasks3)}"
    assert "Game Theory & Constraint Satisfaction" in task_concepts3
    assert "Uninformed & Heuristic Search" in task_concepts3
    print("CASE 3 PASSED: 2 different concepts produced 2 separate plan tasks!")

    print("\n" + "=" * 50)
    print("ALL TARGETED DEDUPLICATION TESTS PASSED 100%!")
    print("=" * 50)

if __name__ == "__main__":
    run_targeted_deduplication_tests()
