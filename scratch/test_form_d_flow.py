import requests
import json
import time

BASE_URL = "http://localhost:8080"
TEST_EMAIL = f"test_formd_{int(time.time())}@edupilot.com"
TEST_PASS = "TestPass123!"

print("=== STARTING FORM D 10-QUESTION E2E VERIFICATION ===", flush=True)

# Step 1: Register student
reg_res = requests.post(f"{BASE_URL}/api/auth/register", json={
    "name": "Form D Tester",
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

# Step 2: Form D Step 4 Init - POST /api/assessment/start
start_res = requests.post(f"{BASE_URL}/api/assessment/start", headers=headers, json={
    "userId": user_id,
    "branch": "Computer Science & Engineering",
    "semester": 3,
    "subjectCode": "DS-LL-PRE01",
    "questionCount": 25
})
assert start_res.status_code == 200, f"Start failed: {start_res.text}"
session = start_res.json()
session_id = session["sessionId"]
total_questions = session.get("totalQuestions", 10)
initial_questions = session.get("questions", [])

print(f"[START SUCCESS] sessionId={session_id}, totalQuestions={total_questions}, initialArrayLength={len(initial_questions)}", flush=True)
assert total_questions == 10, f"Expected 10 total questions, got {total_questions}"
assert len(initial_questions) == 1, f"Expected 1 initial question, got {len(initial_questions)}"

active_q = initial_questions[0]
questions_list = [active_q]
formatted_answers = []

# Loop through questions 1 to 10 simulating Form D handleNextQuestion / handleSubmitAssessment
for current_idx in range(10):
    q = questions_list[current_idx]
    q_id = q.get("questionId") or q.get("id")
    selected_option = 1  # select option B for test
    formatted_answers.append({"questionId": q_id, "selectedOption": selected_option})

    print(f"\n--- Processing Question {current_idx + 1} of {total_questions} ---", flush=True)
    print(f"Question ID: {q_id}", flush=True)
    print(f"Topic: {q.get('topic') or q.get('concept')}", flush=True)
    print(f"Text: {q.get('questionText')[:60]}...", flush=True)

    if current_idx < total_questions - 1:
        # 1. Submit current answer via /initial/submit
        sub_res = requests.post(f"{BASE_URL}/api/assessment/initial/submit", headers=headers, json={
            "adaptiveSessionId": session_id,
            "questionId": q_id,
            "selectedOption": selected_option,
            "responseTimeSeconds": 5
        })
        print(f"[SUBMIT Q{current_idx + 1}] HTTP {sub_res.status_code}", flush=True)
        assert sub_res.status_code == 200, f"Q{current_idx + 1} submit failed: {sub_res.text}"

        # 2. Fetch next question via /initial/next
        next_res = requests.post(f"{BASE_URL}/api/assessment/initial/next", headers=headers, json={
            "adaptiveSessionId": session_id
        })
        print(f"[FETCH Q{current_idx + 2}] HTTP {next_res.status_code}", flush=True)
        assert next_res.status_code == 200, f"Q{current_idx + 2} fetch failed: {next_res.text}"

        next_data = next_res.json()
        assert "question" in next_data, f"Q{current_idx + 2} missing question: {next_data}"
        next_q = next_data["question"]
        questions_list.append(next_q)
        print(f"[RECEIVED Q{current_idx + 2}] Position: {next_data.get('questionNumber')}", flush=True)
    else:
        # Question 10 (Final question)
        # 1. Submit Q10 answer via /initial/submit
        sub_res = requests.post(f"{BASE_URL}/api/assessment/initial/submit", headers=headers, json={
            "adaptiveSessionId": session_id,
            "questionId": q_id,
            "selectedOption": selected_option,
            "responseTimeSeconds": 5
        })
        print(f"[SUBMIT Q10] HTTP {sub_res.status_code}", flush=True)

        # 2. Submit full assessment via /submit
        final_res = requests.post(f"{BASE_URL}/api/assessment/submit", headers=headers, json={
            "sessionId": session_id,
            "userId": user_id,
            "timeTakenSeconds": 300,
            "answers": formatted_answers
        })
        print(f"[FINAL ASSESSMENT SUBMIT] HTTP {final_res.status_code}", flush=True)
        assert final_res.status_code == 200, f"Final submission failed: {final_res.text}"

        result = final_res.json()
        print("\n=== FINAL DIAGNOSTIC ASSESSMENT RESULT ===", flush=True)
        print(f"Total Questions Answered: {result.get('totalQuestions')}", flush=True)
        print(f"Score: {result.get('score')} / {result.get('totalMarks')}", flush=True)
        print(f"Percentage: {result.get('percentage')}%", flush=True)
        print(f"Topic Breakdown: {json.dumps(result.get('topicBreakdown'), indent=2)}", flush=True)

        assert result.get("totalQuestions") == 10, f"Expected 10 questions answered in final result, got {result.get('totalQuestions')}"
        print("\nSUCCESS: All 10 Form D questions generated, served, answered, and processed successfully!", flush=True)
