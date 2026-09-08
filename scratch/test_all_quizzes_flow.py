import requests
import json
import time

BASE_URL = "http://localhost:8080"
TEST_EMAIL = f"test_all_quizzes_{int(time.time())}@edupilot.com"
TEST_PASS = "TestPass123!"

print("=== STARTING LIVE END-TO-END VERIFICATION FOR ALL QUIZ FLOWS ===", flush=True)

# Register Student
reg_res = requests.post(f"{BASE_URL}/api/auth/register", json={
    "name": "All Quizzes Tester",
    "email": TEST_EMAIL,
    "password": TEST_PASS,
    "role": "STUDENT"
})
assert reg_res.status_code in [200, 201], f"Registration failed: {reg_res.text}"
reg_data = reg_res.json()
token = reg_data.get("token")
user_id = reg_data.get("userId") or reg_data.get("id")
headers = {"Authorization": f"Bearer {token}", "Content-Type": "application/json"}
print(f"[AUTH SUCCESS] user_id={user_id}", flush=True)

# ------------------------------------------------------------------------
# 1. Form D Diagnostic Assessment Flow
# ------------------------------------------------------------------------
print("\n--- 1. Testing Form D Diagnostic Assessment Flow ---", flush=True)
d_start = requests.post(f"{BASE_URL}/api/assessment/start", headers=headers, json={
    "userId": user_id,
    "branch": "Computer Science & Engineering",
    "semester": 3,
    "subjectCode": "DS-LL-PRE01",
    "questionCount": 25
})
assert d_start.status_code == 200, f"Form D start failed: {d_start.text}"
d_sess = d_start.json()
d_sess_id = d_sess["sessionId"]
print(f"[FORM D START PASS] sessionId={d_sess_id}, totalQuestions={d_sess.get('totalQuestions')}", flush=True)

d_answers = []
current_q = d_sess["questions"][0]
for idx in range(10):
    q_id = current_q.get("questionId") or current_q.get("id")
    d_answers.append({"questionId": q_id, "selectedOption": 1})
    
    sub = requests.post(f"{BASE_URL}/api/assessment/initial/submit", headers=headers, json={
        "adaptiveSessionId": d_sess_id,
        "questionId": q_id,
        "selectedOption": 1,
        "responseTimeSeconds": 5
    })
    assert sub.status_code == 200, f"Q{idx+1} submit failed: {sub.text}"
    
    if idx < 9:
        nxt = requests.post(f"{BASE_URL}/api/assessment/initial/next", headers=headers, json={
            "adaptiveSessionId": d_sess_id
        })
        assert nxt.status_code == 200, f"Q{idx+2} fetch failed: {nxt.text}"
        current_q = nxt.json()["question"]

d_final = requests.post(f"{BASE_URL}/api/assessment/submit", headers=headers, json={
    "sessionId": d_sess_id,
    "userId": user_id,
    "timeTakenSeconds": 300,
    "answers": d_answers
})
assert d_final.status_code == 200, f"Form D final submit failed: {d_final.text}"
print(f"[FORM D E2E PASS] totalQuestions={d_final.json().get('totalQuestions')}, score={d_final.json().get('score')}", flush=True)

# ------------------------------------------------------------------------
# 2. Adaptive Assessment Flow (Stage 1 + Stage 2)
# ------------------------------------------------------------------------
print("\n--- 2. Testing Adaptive Assessment Flow (Stage 1 & 2) ---", flush=True)
a_start = requests.post(f"{BASE_URL}/api/assessment/start", headers=headers, json={
    "userId": user_id,
    "branch": "Computer Science & Engineering",
    "semester": 3,
    "subjectCode": "CS301",
    "questionCount": 10
})
assert a_start.status_code == 200
a_sess = a_start.json()
a_sess_id = a_sess["sessionId"]

# Stage 2 Adaptive Session
adap_start = requests.post(f"{BASE_URL}/api/assessment/adaptive/start", headers=headers, json={
    "diagnosticSessionId": a_sess_id,
    "subjectCode": "CS301",
    "userId": user_id
})
assert adap_start.status_code == 200, f"Adaptive start failed: {adap_start.text}"
adap_sess = adap_start.json()
adap_sess_id = adap_sess["adaptiveSessionId"]
print(f"[ADAPTIVE START PASS] adaptiveSessionId={adap_sess_id}", flush=True)

# Fetch and answer next adaptive question
adap_next = requests.post(f"{BASE_URL}/api/assessment/adaptive/next", headers=headers, json={
    "adaptiveSessionId": adap_sess_id
})
assert adap_next.status_code == 200, f"Adaptive next failed: {adap_next.text}"
adap_q = adap_next.json()["question"]
adap_q_id = adap_q.get("questionId") or adap_q.get("id")

adap_sub = requests.post(f"{BASE_URL}/api/assessment/adaptive/submit", headers=headers, json={
    "adaptiveSessionId": adap_sess_id,
    "questionId": adap_q_id,
    "selectedOption": 1,
    "responseTimeSeconds": 4
})
assert adap_sub.status_code == 200, f"Adaptive submit failed: {adap_sub.text}"
print(f"[ADAPTIVE Q SUBMIT PASS] isCorrect={adap_sub.json().get('isCorrect')}, nextDifficulty={adap_sub.json().get('nextDifficulty')}", flush=True)

# ------------------------------------------------------------------------
# 3. Concept Remediation Test Flow
# ------------------------------------------------------------------------
print("\n--- 3. Testing Concept Remediation Test Flow ---", flush=True)
rem_start = requests.post(f"{BASE_URL}/api/concept-remediation/start", headers=headers, json={
    "studentId": user_id,
    "subject": "Data Structures & Algorithms",
    "concept": "Arrays & Linked Lists"
})
assert rem_start.status_code == 200, f"Remediation start failed: {rem_start.text}"
rem_sess = rem_start.json()
rem_sess_id = rem_sess["sessionId"]
rem_qs = rem_sess["questions"]
print(f"[REMEDIATION START PASS] sessionId={rem_sess_id}, questionCount={len(rem_qs)}", flush=True)

rem_answers = [{"questionId": q["questionId"], "selectedOptionIndex": 1} for q in rem_qs]
rem_sub = requests.post(f"{BASE_URL}/api/concept-remediation/submit", headers=headers, json={
    "studentId": user_id,
    "sessionId": rem_sess_id,
    "answers": rem_answers
})
assert rem_sub.status_code == 200, f"Remediation submit failed: {rem_sub.text}"
print(f"[REMEDIATION SUBMIT PASS] passed={rem_sub.json().get('passed')}, score={rem_sub.json().get('correctCount')}/{rem_sub.json().get('totalQuestions')}", flush=True)

# ------------------------------------------------------------------------
# 4. Subject Knowledge Baseline Check (Dashboard Test)
# ------------------------------------------------------------------------
print("\n--- 4. Testing Subject Knowledge Baseline Check Flow ---", flush=True)
dash_gen = requests.post(f"{BASE_URL}/api/dashboard-test/generate", headers=headers, json={
    "studentId": user_id
})
assert dash_gen.status_code == 200, f"Dashboard test generate failed: {dash_gen.text}"
dash_sess = dash_gen.json()
dash_sess_id = dash_sess["sessionId"]
dash_qs = dash_sess["questions"]
print(f"[DASHBOARD TEST GEN PASS] sessionId={dash_sess_id}, questionsCount={len(dash_qs)}", flush=True)

dash_answers = [{"questionId": q["questionId"], "selectedOptionIndex": 1} for q in dash_qs]
dash_sub = requests.post(f"{BASE_URL}/api/dashboard-test/submit", headers=headers, json={
    "studentId": user_id,
    "sessionId": dash_sess_id,
    "answers": dash_answers
})
assert dash_sub.status_code == 200, f"Dashboard test submit failed: {dash_sub.text}"
print(f"[DASHBOARD TEST SUBMIT PASS] score={dash_sub.json().get('totalScorePercentage')}%", flush=True)

latest_dash = requests.get(f"{BASE_URL}/api/dashboard-test/latest-result/{user_id}", headers=headers)
assert latest_dash.status_code == 200, f"Latest result fetch failed: {latest_dash.text}"
print(f"[DASHBOARD TEST LATEST RESULT PASS] score={latest_dash.json().get('totalScorePercentage')}%", flush=True)

# ------------------------------------------------------------------------
# 5. Practice Quiz Pool & Question Creation Flow
# ------------------------------------------------------------------------
print("\n--- 5. Testing Practice Quiz Pool & Faculty Question Creation Flow ---", flush=True)
new_q_payload = {
    "subject": "Data Structures & Algorithms",
    "concept": "Binary Search Trees",
    "difficulty": "EASY",
    "questionText": f"Automated Test Question {int(time.time())}: What is the search time complexity in a BST?",
    "options": ["O(1)", "O(log N)", "O(N^2)", "O(N log N)"],
    "correctOptionIndex": 1,
    "conceptualExplanation": "BST search time complexity is bounded by tree height O(log N)."
}
create_q = requests.post(f"{BASE_URL}/api/quizzes", headers=headers, json=new_q_payload)
assert create_q.status_code == 200, f"Create question failed: {create_q.text}"
created_q = create_q.json()
print(f"[CREATE QUESTION PASS] id={created_q.get('id')}", flush=True)

get_qs = requests.get(f"{BASE_URL}/api/quizzes/questions?subject=Data%20Structures%20%26%20Algorithms&difficulty=EASY", headers=headers)
assert get_qs.status_code == 200, f"Fetch quiz questions failed: {get_qs.text}"
print(f"[FETCH QUIZ QUESTIONS PASS] count={len(get_qs.json())}", flush=True)

quiz_sub = requests.post(f"{BASE_URL}/api/quizzes/submit", headers=headers, json={
    "profileId": user_id,
    "subject": "Data Structures & Algorithms",
    "concept": "Binary Search Trees",
    "difficulty": "EASY",
    "isCorrect": True,
    "responseTimeSeconds": 5.0
})
assert quiz_sub.status_code == 200, f"Quiz single submit failed: {quiz_sub.text}"
print(f"[QUIZ SINGLE SUBMIT PASS] nextDifficulty={quiz_sub.json().get('nextDifficulty')}", flush=True)

print("\n========================================================================")
print("SUCCESS: ALL 5 QUIZ & ASSESSMENT FLOWS VERIFIED END-TO-END SUCCESSFULLY!")
print("========================================================================", flush=True)
