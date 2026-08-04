import { StudentProfile, LifestyleLog, getStoredStudentProfile, saveStudentProfile, calculateLocalSgi, QUESTION_BANK } from "./mockData";

const BACKEND_URL = (import.meta as any).env?.VITE_API_URL || "http://127.0.0.1:8080";

let isBackendOnline = false;

export function getAuthHeaders(): Record<string, string> {
  const token = typeof window !== "undefined" ? localStorage.getItem("edupilot_token") : null;
  const headers: Record<string, string> = { "Content-Type": "application/json" };
  if (token) {
    headers["Authorization"] = `Bearer ${token}`;
  }
  return headers;
}

export async function checkBackendConnection(): Promise<boolean> {
  try {
    const controller = new AbortController();
    const id = setTimeout(() => controller.abort(), 1000);
    const res = await fetch(`${BACKEND_URL}/api/students/health`, {
      method: "GET",
      signal: controller.signal
    });
    clearTimeout(id);
    isBackendOnline = res.ok;
    return isBackendOnline;
  } catch (err) {
    isBackendOnline = false;
    return false;
  }
}

if (typeof window !== "undefined") {
  checkBackendConnection();
}

export async function registerUser(payload: {
  email: string;
  password: string;
  fullName: string;
  role: "STUDENT" | "FACULTY" | "ADMIN";
}): Promise<{ ok: boolean; message?: string; userId?: string; role?: string }> {
  try {
    const res = await fetch(`${BACKEND_URL}/api/auth/register`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(payload)
    });
    const data = await res.json();
    if (!res.ok) {
      return { ok: false, message: data.message || "Registration failed." };
    }
    return { ok: true, message: data.message, userId: data.userId, role: data.role };
  } catch (err) {
    return { ok: false, message: "Network error: Unable to connect to Spring Boot authentication server." };
  }
}

export async function loginUser(payload: {
  email: string;
  password: string;
}): Promise<{ ok: boolean; message?: string; token?: string; role?: string; fullName?: string; email?: string; userId?: string }> {
  try {
    const res = await fetch(`${BACKEND_URL}/api/auth/login`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(payload)
    });
    const data = await res.json();
    if (!res.ok) {
      return { ok: false, message: data.message || "Invalid email or password." };
    }
    return {
      ok: true,
      token: data.token,
      role: data.role,
      fullName: data.fullName,
      email: data.email,
      userId: data.userId
    };
  } catch (err) {
    return { ok: false, message: "Network error: Unable to connect to Spring Boot authentication server." };
  }
}

export async function fetchOnboardingStatus(userId: string): Promise<{
  isCompleted: boolean;
  onboardingStatus?: {
    personalCompleted: boolean;
    academicCompleted: boolean;
    questionnaireCompleted: boolean;
    completionPercentage: number;
    currentStep: number;
  };
}> {
  if (!userId) return { isCompleted: false };
  const online = await checkBackendConnection();
  if (online) {
    try {
      const res = await fetch(`${BACKEND_URL}/api/students/onboarding-status/${userId}`, {
        headers: getAuthHeaders()
      });
      if (res.ok) {
        return await res.json();
      }
    } catch (err) {
      console.warn("Error fetching onboarding status from backend:", err);
    }
  }
  // Local fallback status check
  const storedProfile = getStoredStudentProfile();
  const isComplete = Boolean(storedProfile && storedProfile.isCompleted);
  return {
    isCompleted: isComplete,
    onboardingStatus: {
      personalCompleted: isComplete,
      academicCompleted: isComplete,
      questionnaireCompleted: isComplete,
      completionPercentage: isComplete ? 100 : 0,
      currentStep: isComplete ? 7 : 1
    }
  };
}

export async function saveOnboardingStep(userId: string, step: number, data: any): Promise<any> {
  const online = await checkBackendConnection();
  if (online) {
    try {
      const res = await fetch(`${BACKEND_URL}/api/students/onboard/step`, {
        method: "POST",
        headers: getAuthHeaders(),
        body: JSON.stringify({ userId, step, data })
      });
      if (res.ok) {
        return await res.json();
      }
    } catch (err) {
      console.warn("Error auto-saving onboarding step to backend:", err);
    }
  }
  return { currentStep: step, completionPercentage: Math.min(step * 15, 100) };
}

export async function fetchFullProfile(userId: string): Promise<any> {
  const online = await checkBackendConnection();
  if (online) {
    try {
      const res = await fetch(`${BACKEND_URL}/api/students/profile-full/${userId}`, {
        headers: getAuthHeaders()
      });
      if (res.ok) {
        return await res.json();
      }
    } catch (err) {
      console.warn("Error fetching full profile from backend:", err);
    }
  }
  return { profile: getStoredStudentProfile() };
}

export async function updateFullProfile(userId: string, data: any): Promise<StudentProfile> {
  const online = await checkBackendConnection();
  if (online) {
    try {
      const res = await fetch(`${BACKEND_URL}/api/students/profile/${userId}`, {
        method: "PUT",
        headers: getAuthHeaders(),
        body: JSON.stringify(data)
      });
      if (res.ok) {
        const updated = await res.json();
        saveStudentProfile(updated);
        return updated;
      }
    } catch (err) {
      console.warn("Error updating profile on backend:", err);
    }
  }
  const current = getStoredStudentProfile();
  const updated = { ...current, ...data };
  saveStudentProfile(updated);
  return updated;
}

export async function fetchProfile(userId: string): Promise<StudentProfile> {
  const online = await checkBackendConnection();
  if (online) {
    try {
      const res = await fetch(`${BACKEND_URL}/api/students/profile/${userId}`, {
        headers: getAuthHeaders()
      });
      if (res.ok) {
        return await res.json();
      }
    } catch (err) {
      console.warn("Error fetching profile from backend, falling back to local:", err);
    }
  }
  return getStoredStudentProfile();
}

export async function onboardStudent(payload: any): Promise<StudentProfile> {
  const online = await checkBackendConnection();
  if (online) {
    try {
      const res = await fetch(`${BACKEND_URL}/api/students/onboard`, {
        method: "POST",
        headers: getAuthHeaders(),
        body: JSON.stringify(payload)
      });
      if (res.ok) {
        const profile = await res.json();
        saveStudentProfile(profile);
        return profile;
      }
    } catch (err) {
      console.warn("Error onboarding student on backend:", err);
    }
  }
  
  // Local fallback
  const profile = getStoredStudentProfile();
  profile.course = payload.course;
  profile.semester = payload.semester;
  profile.subjects = payload.subjects;
  profile.careerGoals = payload.careerGoals;
  profile.targetCgpa = payload.targetCgpa;
  profile.preferredStudyHoursPerDay = payload.preferredStudyHoursPerDay;
  profile.learningStyle = payload.learningStyle;
  profile.lifestyleHistory = [
    {
      date: "Today",
      sleepHours: payload.sleepHours || 7.0,
      screenTimeHours: payload.screenTime || 4.5,
      stressLevel: payload.stressLevel || 5,
      exerciseMinutes: payload.exerciseMinutes || 30,
      studyMinutes: payload.preferredStudyHoursPerDay * 60,
      productivityRating: 7.5,
      attendanceRate: 95
    }
  ];
  profile.studentGrowthIndex = calculateLocalSgi(profile);
  saveStudentProfile(profile);
  return profile;
}

export async function postLifestyleLog(profileId: string, log: any): Promise<StudentProfile> {
  const online = await checkBackendConnection();
  if (online) {
    try {
      const res = await fetch(`${BACKEND_URL}/api/students/lifestyle/${profileId}`, {
        method: "POST",
        headers: getAuthHeaders(),
        body: JSON.stringify({
          sleepHours: log.sleepHours,
          screenTimeHours: log.screenTimeHours,
          stressLevel: log.stressLevel,
          exerciseMinutes: log.exerciseMinutes,
          studyMinutes: log.studyMinutes,
          attendanceRate: log.attendanceRate,
          productivityRating: log.productivityRating
        })
      });
      if (res.ok) {
        const updated = await res.json();
        saveStudentProfile(updated);
        return updated;
      }
    } catch (err) {
      console.warn("Error submitting lifestyle to backend:", err);
    }
  }
  
  const profile = getStoredStudentProfile();
  const updatedHistory = [...profile.lifestyleHistory.slice(1), log];
  const updated = {
    ...profile,
    lifestyleHistory: updatedHistory,
    completedQuizzesCount: profile.completedQuizzesCount + 1
  };
  updated.studentGrowthIndex = calculateLocalSgi(updated);
  updated.lifestyleScore = Math.min(Math.round(log.sleepHours * 8 + log.exerciseMinutes * 0.5), 100);
  updated.productivityScore = log.productivityRating * 10;
  saveStudentProfile(updated);
  return updated;
}

export async function postQuestionnaire(profileId: string, data: any): Promise<StudentProfile> {
  const online = await checkBackendConnection();
  if (online) {
    try {
      const res = await fetch(`${BACKEND_URL}/api/students/questionnaire/${profileId}`, {
        method: "POST",
        headers: getAuthHeaders(),
        body: JSON.stringify(data)
      });
      if (res.ok) {
        const updated = await res.json();
        saveStudentProfile(updated);
        return updated;
      }
    } catch (err) {
      console.warn("Error submitting questionnaire to backend:", err);
    }
  }
  
  const profile = getStoredStudentProfile();
  profile.consistencyScore = data.previousScores >= 75 ? 90 : 70;
  profile.productivityScore = data.hoursStudied >= 20 ? 80 : 60;
  profile.lifestyleScore = Math.min(Math.round(data.sleepHours * 8 + data.physicalActivity * 4), 100);
  profile.studentGrowthIndex = calculateLocalSgi(profile);
  saveStudentProfile(profile);
  return profile;
}

export async function getRecommendations(profileId: string): Promise<any> {
  const online = await checkBackendConnection();
  if (online) {
    try {
      const res = await fetch(`${BACKEND_URL}/api/students/recommendations/${profileId}`, {
        headers: getAuthHeaders()
      });
      if (res.ok) {
        return await res.json();
      }
    } catch (err) {
      console.warn("Error getting recommendations from backend:", err);
    }
  }
  
  const profile = getStoredStudentProfile();
  const latestLog = profile.lifestyleHistory[profile.lifestyleHistory.length - 1];
  const recs = [];
  if (profile.studentGrowthIndex < 8.0) {
    recs.push("Focus Set: Attempt 'Binary Search Tree' adaptive assessment to boost DSA mastery.");
  }
  if (latestLog && latestLog.sleepHours < 7.0) {
    recs.push("Sleep Guide: Keep screens off 45m before sleep. Target 7.5 hrs tonight.");
  }
  if (latestLog && latestLog.stressLevel > 6) {
    recs.push("Focus Interval: Activate Pomodoro focus timer with Lofi background beats.");
  }
  recs.push("Career Guidance: Check out target ML Engineering internships recommended for you.");
  
  return {
    predicted_performance_level: profile.studentGrowthIndex >= 7.5 ? "High" : "Medium",
    predicted_cgpa: profile.predictedCgpa,
    academic_risk_level: profile.academicRiskLevel,
    student_growth_index: profile.studentGrowthIndex,
    insights: recs
  };
}

export async function fetchQuizQuestions(subject: string, difficulty: string): Promise<any[]> {
  const online = await checkBackendConnection();
  if (online) {
    try {
      const res = await fetch(`${BACKEND_URL}/api/quizzes/questions?subject=${encodeURIComponent(subject)}&difficulty=${difficulty}`, {
        headers: getAuthHeaders()
      });
      if (res.ok) {
        return await res.json();
      }
    } catch (err) {
      console.warn("Error fetching quiz questions from backend:", err);
    }
  }
  
  // Local fallback: load static seeds + custom local storage questions
  const localBankRaw = typeof window !== "undefined" ? localStorage.getItem("edupilot_custom_questions") : null;
  const localQuestions = localBankRaw ? JSON.parse(localBankRaw) : [];
  const fullBank = [...QUESTION_BANK, ...localQuestions];
  
  const candidates = fullBank.filter(q => q.subject === subject && q.difficulty === difficulty);
  return candidates.length > 0 ? candidates.slice(0, 4) : fullBank.filter(q => q.difficulty === difficulty).slice(0, 4);
}

export async function createQuizQuestion(question: {
  subject: string;
  concept: string;
  difficulty: "EASY" | "MEDIUM" | "HARD";
  questionText: string;
  options: string[];
  correctOptionIndex: number;
  conceptualExplanation: string;
}): Promise<any> {
  const online = await checkBackendConnection();
  if (online) {
    try {
      const res = await fetch(`${BACKEND_URL}/api/quizzes`, {
        method: "POST",
        headers: getAuthHeaders(),
        body: JSON.stringify(question)
      });
      if (res.ok) {
        return await res.json();
      }
    } catch (err) {
      console.warn("Error creating quiz question on backend:", err);
    }
  }

  // Local fallback persistence
  const localBankRaw = localStorage.getItem("edupilot_custom_questions");
  const localBank = localBankRaw ? JSON.parse(localBankRaw) : [];
  localBank.push(question);
  localStorage.setItem("edupilot_custom_questions", JSON.stringify(localBank));
  
  // Push to active memory array for immediate local session use
  QUESTION_BANK.push(question as any);
  return question;
}

export async function submitQuizAnswer(payload: {
  profileId: string;
  subject: string;
  concept: string;
  difficulty: string;
  isCorrect: boolean;
  responseTimeSeconds: number;
}): Promise<any> {
  const online = await checkBackendConnection();
  if (online) {
    try {
      const res = await fetch(`${BACKEND_URL}/api/quizzes/submit`, {
        method: "POST",
        headers: getAuthHeaders(),
        body: JSON.stringify(payload)
      });
      if (res.ok) {
        return await res.json();
      }
    } catch (err) {
      console.warn("Error submitting answer to backend:", err);
    }
  }
  
  const isCorrect = payload.isCorrect;
  const currentDiff = payload.difficulty;
  const time = payload.responseTimeSeconds;
  
  let nextDifficulty = currentDiff;
  let reason = "";

  if (isCorrect) {
    if (time < 15 && currentDiff === "EASY") {
      nextDifficulty = "MEDIUM";
      reason = "Correct in under 15s. Adjusting difficulty to MEDIUM to match quick recall.";
    } else if (time < 20 && currentDiff === "MEDIUM") {
      nextDifficulty = "HARD";
      reason = "Excellent speed. Scaling to HARD to map advanced mastery limits.";
    } else if (currentDiff === "EASY") {
      nextDifficulty = "MEDIUM";
      reason = "Correct response. Scaling up to validation level (MEDIUM).";
    } else if (currentDiff === "MEDIUM") {
      nextDifficulty = "HARD";
      reason = "Mastery confirmed. Moving to highest conceptual tier.";
    } else {
      reason = "Hard difficulty completed. Keeping hard level active.";
    }
  } else {
    if (time > 35 && currentDiff === "HARD") {
      nextDifficulty = "MEDIUM";
      reason = "Incorrect answer & long response time. Dropping difficulty to rebuild confidence.";
    } else if (currentDiff === "HARD") {
      nextDifficulty = "MEDIUM";
      reason = "Incorrect. Reviewing intermediate modules suggested.";
    } else if (currentDiff === "MEDIUM") {
      nextDifficulty = "EASY";
      reason = "Incorrect. Dropping difficulty to EASY to reinforce foundations.";
    } else {
      reason = "Incorrect. Remaining at EASY to review fundamental concepts.";
    }
  }

  return { nextDifficulty, reason };
}
