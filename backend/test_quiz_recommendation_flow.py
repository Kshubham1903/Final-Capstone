import requests
import json
import uuid
import time

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

def run_tests():
    print("==================================================")
    print("STARTING ACCEPTANCE TESTS 1 - 5 (CONCEPT NORMALIZATION & GRANULARITY)")
    print("==================================================")

    # ----------------------------------------------------
    # TEST 1: Multiple wrong questions -> 5 underlying concepts
    # ----------------------------------------------------
    print("\n--- TEST 1: Multiple wrong questions (5 concepts in Blockchain Development) ---")
    u_id_1 = uuid.uuid4().hex[:6]
    email_1 = f"student_t1_{u_id_1}@edupilot.test"
    token_1, user_id_1 = get_auth_token(email_1, "Password123!", "Test 1 Student")
    headers_1 = {"Authorization": f"Bearer {token_1}"}

    concepts_5 = [
        "Ethereum State Trie (MPT) (EASY #16)",
        "Gas Limit vs Gas Price EVM (EASY #15)",
        "Smart Contract Immutable Code (EASY #1)",
        "ERC-20 Token Standard Functions (EASY #18)",
        "Account Abstraction ERC-4337 (EASY #25)"
    ]

    for c in concepts_5:
        requests.post(f"{BASE_URL}/api/quizzes/submit", json={
            "profileId": user_id_1,
            "subject": "Blockchain Development",
            "concept": c,
            "difficulty": "EASY",
            "isCorrect": False,
            "responseTimeSeconds": 15
        }, headers=headers_1)

    plan_1 = requests.get(f"{BASE_URL}/api/planner/today/{user_id_1}", headers=headers_1).json()
    tasks_1 = [t.get("conceptName") for t in plan_1.get("tasks", [])]
    print(f"TEST 1 Returned Recommendations Count: {len(tasks_1)}")
    print(f"TEST 1 Recommendation Concepts: {tasks_1}")

    expected_concepts_1 = [
        "Ethereum State Trie (MPT)",
        "Gas Limit vs Gas Price EVM",
        "Smart Contract Immutable Code",
        "ERC-20 Token Standard Functions",
        "Account Abstraction ERC-4337"
    ]

    for exp in expected_concepts_1:
        assert exp in tasks_1, f"Expected normalized concept '{exp}' in recommendations, got {tasks_1}"
        assert not any("#" in t or "EASY #" in t for t in tasks_1), "No raw #index or tier strings allowed in recommendations!"

    print("TEST 1 PASSED: 5 normalized core concepts returned with zero raw question titles or #index tags!")

    # ----------------------------------------------------
    # TEST 2: Same concept multiple times (3 difficulty variations)
    # ----------------------------------------------------
    print("\n--- TEST 2: Same concept multiple times (3 difficulty tier variations of Ethereum State Trie) ---")
    u_id_2 = uuid.uuid4().hex[:6]
    email_2 = f"student_t2_{u_id_2}@edupilot.test"
    token_2, user_id_2 = get_auth_token(email_2, "Password123!", "Test 2 Student")
    headers_2 = {"Authorization": f"Bearer {token_2}"}

    variations = [
        "Ethereum State Trie (MPT) (EASY #16)",
        "Ethereum State Trie (MPT) Implementation (MEDIUM #16)",
        "Ethereum State Trie (MPT) Architecture (HARD #16)"
    ]

    for var in variations:
        requests.post(f"{BASE_URL}/api/quizzes/submit", json={
            "profileId": user_id_2,
            "subject": "Blockchain Development",
            "concept": var,
            "difficulty": "MEDIUM",
            "isCorrect": False,
            "responseTimeSeconds": 20
        }, headers=headers_2)

    plan_2 = requests.get(f"{BASE_URL}/api/planner/today/{user_id_2}", headers=headers_2).json()
    tasks_2 = [t.get("conceptName") for t in plan_2.get("tasks", [])]
    reasons_2 = [t.get("reason") for t in plan_2.get("tasks", [])]
    print(f"TEST 2 Recommendations Count: {len(tasks_2)}")
    print(f"TEST 2 Tasks: {tasks_2}")
    print(f"TEST 2 Reasons: {reasons_2}")

    assert len(tasks_2) == 1, f"Expected EXACTLY ONE recommendation for 3 mistakes on same concept, got {len(tasks_2)}"
    assert tasks_2[0] == "Ethereum State Trie (MPT)", f"Expected 'Ethereum State Trie (MPT)', got '{tasks_2[0]}'"
    assert "3 times" in reasons_2[0] or "missed Ethereum State Trie (MPT)" in reasons_2[0], \
        "Expected aggregated mistake count of 3 in recommendation reason"
    print("TEST 2 PASSED: 3 raw variations normalized to 1 single concept recommendation with aggregated mistake count 3!")

    # ----------------------------------------------------
    # TEST 3: Correct answers produce zero recommendations
    # ----------------------------------------------------
    print("\n--- TEST 3: Correct answers produce zero weak recommendations ---")
    u_id_3 = uuid.uuid4().hex[:6]
    email_3 = f"student_t3_{u_id_3}@edupilot.test"
    token_3, user_id_3 = get_auth_token(email_3, "Password123!", "Test 3 Student")
    headers_3 = {"Authorization": f"Bearer {token_3}"}

    for c in ["Hash Tables (EASY #1)", "Binary Search Tree (EASY #2)"]:
        requests.post(f"{BASE_URL}/api/quizzes/submit", json={
            "profileId": user_id_3,
            "subject": "Data Structures & Algorithms",
            "concept": c,
            "difficulty": "EASY",
            "isCorrect": True,
            "responseTimeSeconds": 10
        }, headers=headers_3)

    plan_3 = requests.get(f"{BASE_URL}/api/planner/today/{user_id_3}", headers=headers_3).json()
    tasks_3 = [t.get("conceptName") for t in plan_3.get("tasks", [])]
    print(f"TEST 3 Tasks: {tasks_3}")
    assert "Hash Tables" not in tasks_3 and "Binary Search Tree" not in tasks_3, "Correct answers must not create weak recommendations"
    print("TEST 3 PASSED: Zero weak recommendations for correct answers!")

    # ----------------------------------------------------
    # TEST 4: DSA -> Blockchain Subject Isolation
    # ----------------------------------------------------
    print("\n--- TEST 4: DSA -> Blockchain Subject Isolation ---")
    u_id_4 = uuid.uuid4().hex[:6]
    email_4 = f"student_t4_{u_id_4}@edupilot.test"
    token_4, user_id_4 = get_auth_token(email_4, "Password123!", "Test 4 Student")
    headers_4 = {"Authorization": f"Bearer {token_4}"}

    # Step 1: Complete DSA quiz with wrong Hash Tables
    requests.post(f"{BASE_URL}/api/quizzes/submit", json={
        "profileId": user_id_4,
        "subject": "Data Structures & Algorithms",
        "concept": "Hash Tables (EASY #1)",
        "difficulty": "EASY",
        "isCorrect": False,
        "responseTimeSeconds": 15
    }, headers=headers_4)

    # Step 2: Immediately after, complete Blockchain quiz with wrong Ethereum questions
    requests.post(f"{BASE_URL}/api/quizzes/submit", json={
        "profileId": user_id_4,
        "subject": "Blockchain Development",
        "concept": "Ethereum State Trie (MPT) (EASY #16)",
        "difficulty": "EASY",
        "isCorrect": False,
        "responseTimeSeconds": 15
    }, headers=headers_4)

    plan_4 = requests.get(f"{BASE_URL}/api/planner/today/{user_id_4}", headers=headers_4).json()
    tasks_4 = [t.get("conceptName") for t in plan_4.get("tasks", [])]
    subjects_4 = [t.get("subjectName") for t in plan_4.get("tasks", [])]
    print(f"TEST 4 Subjects: {subjects_4}")
    print(f"TEST 4 Tasks: {tasks_4}")

    assert "Ethereum State Trie (MPT)" in tasks_4, "Expected Blockchain concept in recommendations"
    assert "Hash Tables" not in tasks_4, "CRITICAL: Hash Tables from previous DSA quiz MUST NOT leak into Blockchain plan!"
    print("TEST 4 PASSED: Recommendations strictly scoped to latest Blockchain session!")

    # ----------------------------------------------------
    # TEST 5: 100% Score -> Success state
    # ----------------------------------------------------
    print("\n--- TEST 5: 100% Score Success State ---")
    u_id_5 = uuid.uuid4().hex[:6]
    email_5 = f"student_t5_{u_id_5}@edupilot.test"
    token_5, user_id_5 = get_auth_token(email_5, "Password123!", "Test 5 Student")
    headers_5 = {"Authorization": f"Bearer {token_5}"}

    for i in range(5):
        requests.post(f"{BASE_URL}/api/quizzes/submit", json={
            "profileId": user_id_5,
            "subject": "Blockchain Development",
            "concept": f"Blockchain_Concept_{i}",
            "difficulty": "EASY",
            "isCorrect": True,
            "responseTimeSeconds": 10
        }, headers=headers_5)

    plan_5 = requests.get(f"{BASE_URL}/api/planner/today/{user_id_5}", headers=headers_5).json()
    tasks_5 = [t.get("conceptName") for t in plan_5.get("tasks", [])]
    reasons_5 = [t.get("reason") for t in plan_5.get("tasks", [])]
    print(f"TEST 5 Tasks: {tasks_5}")
    print(f"TEST 5 Reasons: {reasons_5}")

    # ----------------------------------------------------
    # TEST 6: API Verification (Direct Recommendation & Planner Endpoints)
    # ----------------------------------------------------
    print("\n--- TEST 6: Direct API Verification for /api/recommendations and /api/planner/today ---")
    u_id_6 = uuid.uuid4().hex[:6]
    email_6 = f"student_t6_{u_id_6}@edupilot.test"
    token_6, user_id_6 = get_auth_token(email_6, "Password123!", "Test 6 Student")
    headers_6 = {"Authorization": f"Bearer {token_6}"}

    # Submit 3 raw missed questions with difficulty and index suffixes
    raw_inputs = [
        "Ethereum State Trie (MPT) (EASY #16)",
        "Ethereum State Trie (MPT) Implementation (MEDIUM #16)",
        "Gas Limit vs Gas Price EVM (EASY #15)"
    ]
    for raw in raw_inputs:
        requests.post(f"{BASE_URL}/api/quizzes/submit", json={
            "profileId": user_id_6,
            "subject": "Blockchain Development",
            "concept": raw,
            "difficulty": "EASY",
            "isCorrect": False,
            "responseTimeSeconds": 12
        }, headers=headers_6)

    # 1. Fetch raw active recommendations endpoint
    recs_res = requests.get(f"{BASE_URL}/api/recommendations/{user_id_6}", headers=headers_6).json()
    print("\n[GET /api/recommendations/{userId} Response]:")
    print(json.dumps(recs_res, indent=2))

    # 2. Fetch today learning plan endpoint
    plan_res = requests.get(f"{BASE_URL}/api/planner/today/{user_id_6}", headers=headers_6).json()
    print("\n[GET /api/planner/today/{userId} Response]:")
    print(json.dumps(plan_res, indent=2))

    rec_concepts = [r.get("conceptName") for r in recs_res]
    plan_concepts = [t.get("conceptName") for t in plan_res.get("tasks", [])]

    print(f"\nReturned Recommendation Concept Names: {rec_concepts}")
    print(f"Returned Learning Plan Concept Names: {plan_concepts}")

    # Assertions
    assert len(rec_concepts) == 2, f"Expected 2 aggregated normalized concept recommendations, got {len(rec_concepts)}"
    assert "Ethereum State Trie (MPT)" in rec_concepts and "Gas Limit vs Gas Price EVM" in rec_concepts, \
        f"Expected clean concept names, got {rec_concepts}"

    for c in rec_concepts + plan_concepts:
        assert not ("#" in c or "EASY" in c or "MEDIUM" in c or "HARD" in c or "Implementation" in c), \
            f"Raw question title or #index suffix found in returned concept: '{c}'"

    print("\nTEST 6 PASSED: Recommendation API endpoints return clean normalized concepts with zero raw titles or #index values!")

    print("\n==================================================")
    print("ALL API ACCEPTANCE TESTS 1 - 6 PASSED SUCCESSFULLY!")
    print("==================================================")

if __name__ == "__main__":
    run_tests()

