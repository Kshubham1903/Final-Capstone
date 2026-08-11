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

export function handleAuthError(res: Response): void {
  if (res.status === 401 || res.status === 403) {
    console.warn(`[Security Alert] HTTP ${res.status} Unauthorized / Forbidden received. Clearing invalid session token.`);
    if (typeof window !== "undefined") {
      localStorage.removeItem("edupilot_token");
    }
  }
}

export async function checkBackendConnection(): Promise<boolean> {
  try {
    const controller = new AbortController();
    const id = setTimeout(() => controller.abort(), 3000);
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

export async function fetchQuizQuestions(subject: string, difficulty: string, excludeIds?: string[], targetConcept?: string): Promise<any[]> {
  const isVerificationRequest = !!(targetConcept && targetConcept.trim().length > 0);
  const online = await checkBackendConnection();
  if (online || typeof window !== "undefined") {
    const controller = new AbortController();
    const timeoutId = setTimeout(() => controller.abort(), 8000);
    try {
      const params = new URLSearchParams({
        subject: subject,
        difficulty: difficulty
      });
      if (excludeIds && excludeIds.length > 0) {
        params.append("exclude", excludeIds.join(","));
      }
      if (isVerificationRequest) {
        params.append("targetConcept", targetConcept);
      }
      const fetchUrl = `${BACKEND_URL}/api/quizzes/questions?${params.toString()}`;
      const authHeaders = getAuthHeaders();
      const res = await fetch(fetchUrl, {
        headers: authHeaders,
        signal: controller.signal
      });
      clearTimeout(timeoutId);

      if (res.ok) {
        isBackendOnline = true;
        const data = await res.json();
        return data;
      }

      // Explicit Auth Error Handling: Do NOT silently hide 401/403 auth errors
      if (res.status === 401 || res.status === 403) {
        handleAuthError(res);
        throw new Error("AUTH_ERROR: Authentication required or invalid session token. Please log in again.");
      }

      if (isVerificationRequest) {
        throw new Error(`SERVER_ERROR: Backend server returned HTTP ${res.status}`);
      }
    } catch (err: any) {
      clearTimeout(timeoutId);
      if (err.name === "AbortError") {
        throw new Error("TIMEOUT_ERROR: Verification quiz loading timed out.");
      }
      if (err.message && (err.message.includes("AUTH_ERROR") || err.message.includes("SERVER_ERROR"))) {
        throw err;
      }
      if (isVerificationRequest) {
        throw new Error(`NETWORK_ERROR: ${err.message || "Failed to reach backend server"}`);
      }
      console.warn("Error fetching quiz questions from backend:", err);
    }
  }
  
  if (isVerificationRequest) {
    throw new Error("NO_QUESTIONS: Targeted verification questions unavailable in local bank.");
  }

  const localBankRaw = typeof window !== "undefined" ? localStorage.getItem("edupilot_custom_questions") : null;
  const localQuestions = localBankRaw ? JSON.parse(localBankRaw) : [];
  const fullBank = [...QUESTION_BANK, ...localQuestions];
  
  const candidates = fullBank.filter(q => q.subject === subject && q.difficulty === difficulty);
  if (candidates.length > 0) return candidates.slice(0, 10);

  const subjectCandidates = fullBank.filter(q => q.subject === subject);
  if (subjectCandidates.length > 0) return subjectCandidates.slice(0, 10);

  const subjLower = subject.trim().toLowerCase();
  let rawFallback: any[] = [];
  if (subjLower.includes("blockchain")) {
    rawFallback = [
      {
        subject: subject,
        concept: "Consensus Mechanisms",
        difficulty: difficulty,
        questionText: "What core problem does a blockchain consensus mechanism solve in a decentralized network?",
        options: [
          "Double-spending prevention and Byzantine fault tolerance agreement",
          "High physical network latency reduction",
          "Relational database storage capacity expansion",
          "Centralized server cluster hardware recovery"
        ],
        correctOptionIndex: 0,
        conceptualExplanation: "Consensus mechanisms enable distributed nodes to reach agreement on transaction validity without trusting a central authority."
      },
      {
        subject: subject,
        concept: "Smart Contracts",
        difficulty: difficulty,
        questionText: "What is the primary function of a smart contract on Ethereum?",
        options: [
          "Hardware accelerator for cryptographic mining validation",
          "Self-executing on-chain code that runs when conditions are met",
          "Encrypted peer-to-peer instant messaging protocol client",
          "Scanned physical paper contract stored in network storage"
        ],
        correctOptionIndex: 1,
        conceptualExplanation: "Smart contracts are immutable programs deployed on EVM that execute deterministic logic when triggered by transactions."
      },
      {
        subject: subject,
        concept: "Proof of Stake",
        difficulty: difficulty,
        questionText: "How does Proof of Stake (PoS) differ fundamentally from Proof of Work (PoW)?",
        options: [
          "PoS requires higher hardware electricity consumption than PoW",
          "PoS eliminates all transaction gas fees across the network",
          "PoS selects validators based on staked capital instead of hash power",
          "PoW does not utilize any cryptographic hash calculations"
        ],
        correctOptionIndex: 2,
        conceptualExplanation: "PoS replaces energy-intensive hash mining with capital commitment (staking) to secure network consensus."
      },
      {
        subject: subject,
        concept: "Cryptographic Linking",
        difficulty: difficulty,
        questionText: "In blockchain data structures, how are consecutive blocks cryptographically linked?",
        options: [
          "Blocks connect via static IP address routing tables",
          "Blocks use relational database primary key foreign key links",
          "Blocks merge into a single centralized flat log file",
          "Each block header contains the hash of the preceding block header"
        ],
        correctOptionIndex: 3,
        conceptualExplanation: "Including the previous block's SHA-256/Keccak hash creates an append-only chain where altering past blocks invalidates all subsequent hashes."
      }
    ];
  } else if (subjLower.includes("cloud security") || subjLower.includes("cloud")) {
    rawFallback = [
      {
        subject: subject,
        concept: "Identity & Access Management",
        difficulty: difficulty,
        questionText: "Which IAM principle ensures users receive only the permissions required for their specific job tasks?",
        options: [
          "Principle of Least Privilege access control policy",
          "Role-Based Overdrive execution privileges scheme",
          "Implicit Allow Access default authorization mode",
          "Shared Root Credentials administrative delegation"
        ],
        correctOptionIndex: 0,
        conceptualExplanation: "The Principle of Least Privilege dictates granting users the minimum necessary access required to complete designated assignments."
      },
      {
        subject: subject,
        concept: "Shared Responsibility Model",
        difficulty: difficulty,
        questionText: "In the Cloud Shared Responsibility Model, which security layer is managed primarily by the cloud provider?",
        options: [
          "Customer application source code security vulnerability fixes",
          "Physical data center infrastructure and host facility hardware security",
          "User password complexity policies and rotation schedules",
          "S3 storage bucket public access permission configuration flags"
        ],
        correctOptionIndex: 1,
        conceptualExplanation: "Cloud providers manage security 'of' the cloud (hardware, facilities, host OS), while customers secure data 'in' the cloud."
      },
      {
        subject: subject,
        concept: "Virtual Firewalls",
        difficulty: difficulty,
        questionText: "What is the function of an AWS Security Group in cloud networking?",
        options: [
          "Customer database management server for user records",
          "Automated backup scheduler for Elastic Block Storage volumes",
          "Stateful virtual firewall controlling instance inbound and outbound traffic",
          "Domain Name System registrar and public routing manager"
        ],
        correctOptionIndex: 2,
        conceptualExplanation: "Security Groups operate statefully at the ENI/instance level, automatically allowing return traffic for outbound requests."
      },
      {
        subject: subject,
        concept: "Zero Trust Security",
        difficulty: difficulty,
        questionText: "What is the primary architectural pillar behind Zero Trust Security in cloud environments?",
        options: [
          "Trusting all internal network traffic behind perimeter firewalls",
          "Disabling TLS encryption protocols for internal service calls",
          "Granting root permissions to all internal subnet IP ranges",
          "Never trust, always verify every request regardless of origin"
        ],
        correctOptionIndex: 3,
        conceptualExplanation: "Zero Trust assumes threats exist inside and outside the network, enforcing strict authentication, authorization, and continuous validation."
      }
    ];
  } else {
    rawFallback = [
      {
        subject: subject,
        concept: `${subject} Architecture`,
        difficulty: difficulty,
        questionText: `What is a fundamental design principle when building scalable software modules in ${subject}?`,
        options: [
          `Ensuring loose coupling and modular separation of concerns across components`,
          `Tightly coupling business logic and data persistence into a single file`,
          `Disabling error handling and input validation during execution cycles`,
          `Hardcoding production system parameters into public binary files`
        ],
        correctOptionIndex: 0,
        conceptualExplanation: `Modular separation of concerns ensures maintainability and clean component isolation in ${subject} architectures.`
      },
      {
        subject: subject,
        concept: `${subject} Performance Optimization`,
        difficulty: difficulty,
        questionText: `When optimizing latency and throughput in ${subject}, which approach yields the most reliable performance gains?`,
        options: [
          `Executing unbounded recursive loops without defined termination criteria`,
          `Profiling execution bottlenecks and optimizing critical execution paths`,
          `Disabling caching layers across all service API interface endpoints`,
          `Increasing server hardware specs without profiling underlying bottlenecks`
        ],
        correctOptionIndex: 1,
        conceptualExplanation: `Targeted profiling pinpoints exact bottlenecks, allowing data-driven optimization in ${subject} workflows.`
      },
      {
        subject: subject,
        concept: `${subject} Error Resilience`,
        difficulty: difficulty,
        questionText: `How should high-availability systems in ${subject} handle transient fault conditions?`,
        options: [
          `Ignoring system errors and returning empty unvalidated response payloads`,
          `Terminating host server processes upon encountering non-fatal warnings`,
          `Implementing exponential backoff retry strategies with circuit breakers`,
          `Bypassing input validation and boundary security checks under heavy load`
        ],
        correctOptionIndex: 2,
        conceptualExplanation: `Exponential backoff and circuit breakers prevent cascading failures and handle transient disruptions resiliently in ${subject}.`
      },
      {
        subject: subject,
        concept: `${subject} Security & Data Integrity`,
        difficulty: difficulty,
        questionText: `What practice is critical for protecting data integrity and access control within ${subject} workflows?`,
        options: [
          `Storing API secret credentials in client-side open source assets`,
          `Disabling TLS encryption protocols across internal microservices`,
          `Granting full admin permissions to all default user access tokens`,
          `Validating and sanitizing all inputs at system boundaries before processing`
        ],
        correctOptionIndex: 3,
        conceptualExplanation: `Input validation and boundary sanitization prevent injection vulnerabilities and secure ${subject} operations.`
      }
    ];
  }

  // Shuffle options and update correctOptionIndex for frontend fallback safety
  return rawFallback.map(item => {
    const originalOptions = [...item.options];
    const origIdx = item.correctOptionIndex;
    const correctText = originalOptions[origIdx];
    
    // Simple deterministic/random shuffle
    const paired = originalOptions.map((opt, idx) => ({ opt, isCorrect: idx === origIdx }));
    for (let i = paired.length - 1; i > 0; i--) {
      const j = Math.floor(Math.random() * (i + 1));
      [paired[i], paired[j]] = [paired[j], paired[i]];
    }
    const newOptions = paired.map(p => p.opt);
    const newCorrectIdx = paired.findIndex(p => p.isCorrect);
    return {
      ...item,
      options: newOptions,
      correctOptionIndex: newCorrectIdx
    };
  });
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

  const localBankRaw = localStorage.getItem("edupilot_custom_questions");
  const localBank = localBankRaw ? JSON.parse(localBankRaw) : [];
  localBank.push(question);
  localStorage.setItem("edupilot_custom_questions", JSON.stringify(localBank));
  
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
  isVerification?: boolean;
  targetConcept?: string;
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

// Academic Catalog Master API Services

export async function fetchSubjectBranches(): Promise<string[]> {
  const online = await checkBackendConnection();
  if (online) {
    try {
      const res = await fetch(`${BACKEND_URL}/api/subjects/branches`, {
        headers: getAuthHeaders()
      });
      if (res.ok) {
        return await res.json();
      }
    } catch (err) {
      console.warn("Error fetching branches from backend:", err);
    }
  }
  return [
    "Computer Science & Engineering",
    "Artificial Intelligence & Data Science",
    "Information Technology"
  ];
}

export async function fetchSubjectsByBranchAndSemester(branch: string, semester: number): Promise<any[]> {
  const online = await checkBackendConnection();
  if (online) {
    try {
      const res = await fetch(`${BACKEND_URL}/api/subjects/branches/${encodeURIComponent(branch)}/semesters/${semester}`, {
        headers: getAuthHeaders()
      });
      if (res.ok) {
        return await res.json();
      }
    } catch (err) {
      console.warn("Error fetching subjects by branch & semester:", err);
    }
  }
  
  return [
    { id: "1", subjectCode: "CS301", subjectName: "Data Structures & Algorithms", credits: 4, branch, semester, isActive: true },
    { id: "2", subjectCode: "CS302", subjectName: "Database Management Systems", credits: 4, branch, semester, isActive: true },
    { id: "3", subjectCode: "CS303", subjectName: "Discrete Mathematical Structures", credits: 3, branch, semester, isActive: true }
  ];
}

export async function fetchAllSubjects(): Promise<any[]> {
  const online = await checkBackendConnection();
  if (online) {
    try {
      const res = await fetch(`${BACKEND_URL}/api/subjects`, {
        headers: getAuthHeaders()
      });
      if (res.ok) {
        return await res.json();
      }
    } catch (err) {
      console.warn("Error fetching all subjects:", err);
    }
  }
  return [];
}

// Diagnostic Assessment Engine API Services

export async function startDiagnosticAssessment(payload: {
  userId: string;
  branch: string;
  semester: number;
  subjectCode: string;
  questionCount?: number;
}): Promise<any> {
  const online = await checkBackendConnection();
  if (online) {
    try {
      const res = await fetch(`${BACKEND_URL}/api/assessment/start`, {
        method: "POST",
        headers: getAuthHeaders(),
        body: JSON.stringify(payload)
      });
      if (res.ok) {
        return await res.json();
      }
    } catch (err) {
      console.warn("Error starting diagnostic assessment on backend:", err);
    }
  }

  // Fallback diagnostic session
  return {
    sessionId: "sess_local_" + Date.now(),
    branch: payload.branch,
    semester: payload.semester,
    subjectCode: payload.subjectCode,
    subjectName: "Data Structures & Algorithms",
    totalQuestions: 3,
    totalMarks: 6,
    questions: [
      {
        questionId: "q1",
        topic: "Binary Search Trees",
        questionText: "What is the worst-case time complexity of searching in an unbalanced Binary Search Tree?",
        options: ["O(1)", "O(log N)", "O(N)", "O(N log N)"],
        marks: 2,
        difficulty: "MEDIUM"
      },
      {
        questionId: "q2",
        topic: "Sorting Algorithms",
        questionText: "Which sorting algorithm is guaranteed O(N log N) time in worst case and is stable?",
        options: ["Quick Sort", "Merge Sort", "Heap Sort", "Selection Sort"],
        marks: 2,
        difficulty: "EASY"
      },
      {
        questionId: "q3",
        topic: "Graph Theory",
        questionText: "Which graph traversal algorithm uses a Queue data structure?",
        options: ["Depth First Search (DFS)", "Breadth First Search (BFS)", "Dijkstra Algorithm", "Kruskal Algorithm"],
        marks: 2,
        difficulty: "EASY"
      }
    ]
  };
}

export async function submitDiagnosticAssessment(payload: {
  sessionId: string;
  userId: string;
  timeTakenSeconds: number;
  answers: Array<{ questionId: string; selectedOption: number }>;
}): Promise<any> {
  const online = await checkBackendConnection();
  if (online) {
    try {
      const res = await fetch(`${BACKEND_URL}/api/assessment/submit`, {
        method: "POST",
        headers: getAuthHeaders(),
        body: JSON.stringify(payload)
      });
      if (res.ok) {
        return await res.json();
      }
    } catch (err) {
      console.warn("Error submitting assessment to backend:", err);
    }
  }

  // Fallback result
  return {
    id: "res_local_" + Date.now(),
    sessionId: payload.sessionId,
    userId: payload.userId,
    subjectCode: "CS301",
    subjectName: "Data Structures & Algorithms",
    totalQuestions: payload.answers.length,
    correctAnswers: Math.max(1, payload.answers.length - 1),
    incorrectAnswers: 1,
    skippedQuestions: 0,
    score: 4,
    totalMarks: 6,
    percentage: 66.7,
    accuracy: 66.7,
    timeTakenSeconds: payload.timeTakenSeconds,
    masteryLevel: "PROFICIENT",
    topicBreakdown: {
      "Binary Search Trees": { correct: 1, total: 1, percentage: 100.0 },
      "Sorting Algorithms": { correct: 1, total: 1, percentage: 100.0 }
    },
    userAnswers: []
  };
}

export async function fetchLatestDiagnosticResult(userId: string): Promise<any> {
  const online = await checkBackendConnection();
  if (online) {
    try {
      const res = await fetch(`${BACKEND_URL}/api/assessment/latest/${userId}`, {
        headers: getAuthHeaders()
      });
      if (res.ok) {
        return await res.json();
      }
    } catch (err) {
      console.warn("Error fetching latest diagnostic result:", err);
    }
  }
  return null;
}

// Knowledge Intelligence Engine Master APIs

export async function fetchKnowledgeProfile(userId: string): Promise<any> {
  const online = await checkBackendConnection();
  if (online) {
    try {
      const res = await fetch(`${BACKEND_URL}/api/knowledge/profile/${userId}`, {
        headers: getAuthHeaders()
      });
      if (res.ok) {
        return await res.json();
      }
    } catch (err) {
      console.warn("Error fetching Knowledge Profile from backend:", err);
    }
  }
  return null;
}

export async function fetchWeakConcepts(userId: string): Promise<any[]> {
  const online = await checkBackendConnection();
  if (online) {
    try {
      const res = await fetch(`${BACKEND_URL}/api/knowledge/weak-concepts/${userId}`, {
        headers: getAuthHeaders()
      });
      if (res.ok) {
        return await res.json();
      }
    } catch (err) {
      console.warn("Error fetching weak concepts from backend:", err);
    }
  }
  return [];
}

export async function fetchStrongConcepts(userId: string): Promise<any[]> {
  const online = await checkBackendConnection();
  if (online) {
    try {
      const res = await fetch(`${BACKEND_URL}/api/knowledge/strong-concepts/${userId}`, {
        headers: getAuthHeaders()
      });
      if (res.ok) {
        return await res.json();
      }
    } catch (err) {
      console.warn("Error fetching strong concepts from backend:", err);
    }
  }
  return [];
}

// Personalized Recommendation Engine Master API Services

export async function fetchStudentRecommendations(userId: string): Promise<any[]> {
  const online = await checkBackendConnection();
  if (online) {
    try {
      const res = await fetch(`${BACKEND_URL}/api/recommendations/${userId}`, {
        headers: getAuthHeaders()
      });
      if (res.ok) {
        return await res.json();
      }
    } catch (err) {
      console.warn("Error fetching recommendations from backend:", err);
    }
  }
  return [];
}

export async function fetchHighPriorityRecommendations(userId: string): Promise<any[]> {
  const online = await checkBackendConnection();
  if (online) {
    try {
      const res = await fetch(`${BACKEND_URL}/api/recommendations/high-priority/${userId}`, {
        headers: getAuthHeaders()
      });
      if (res.ok) {
        return await res.json();
      }
    } catch (err) {
      console.warn("Error fetching high priority recommendations:", err);
    }
  }
  return [];
}

export async function regenerateStudentRecommendations(userId: string): Promise<any[]> {
  const online = await checkBackendConnection();
  if (online) {
    try {
      const res = await fetch(`${BACKEND_URL}/api/recommendations/regenerate/${userId}`, {
        method: "POST",
        headers: getAuthHeaders()
      });
      if (res.ok) {
        return await res.json();
      }
    } catch (err) {
      console.warn("Error regenerating recommendations on backend:", err);
    }
  }
  return [];
}

export async function completeRecommendation(id: string): Promise<any> {
  const online = await checkBackendConnection();
  if (online) {
    try {
      const res = await fetch(`${BACKEND_URL}/api/recommendations/${id}/complete`, {
        method: "PATCH",
        headers: getAuthHeaders()
      });
      if (res.ok) {
        return await res.json();
      }
    } catch (err) {
      console.warn("Error marking recommendation complete on backend:", err);
    }
  }
  return null;
}

// Personalized Learning Planner Master API Services

export async function fetchTodayPlan(userId: string): Promise<any> {
  const online = await checkBackendConnection();
  if (online) {
    try {
      const res = await fetch(`${BACKEND_URL}/api/planner/today/${userId}`, {
        headers: getAuthHeaders()
      });
      if (res.ok) {
        return await res.json();
      }
    } catch (err) {
      console.warn("Error fetching today's learning plan:", err);
    }
  }
  return null;
}

export async function fetchWeekPlan(userId: string): Promise<any[]> {
  const online = await checkBackendConnection();
  if (online) {
    try {
      const res = await fetch(`${BACKEND_URL}/api/planner/week/${userId}`, {
        headers: getAuthHeaders()
      });
      if (res.ok) {
        return await res.json();
      }
    } catch (err) {
      console.warn("Error fetching weekly learning plan:", err);
    }
  }
  return [];
}

export async function regeneratePlan(userId: string): Promise<any> {
  const online = await checkBackendConnection();
  if (online) {
    try {
      const res = await fetch(`${BACKEND_URL}/api/planner/regenerate/${userId}`, {
        method: "POST",
        headers: getAuthHeaders()
      });
      if (res.ok) {
        return await res.json();
      }
    } catch (err) {
      console.warn("Error regenerating learning plan:", err);
    }
  }
  return null;
}

export async function completePlannerTask(taskId: string, userId: string): Promise<any> {
  const online = await checkBackendConnection();
  if (online) {
    try {
      const res = await fetch(`${BACKEND_URL}/api/planner/task/${taskId}/complete?userId=${encodeURIComponent(userId)}`, {
        method: "PATCH",
        headers: getAuthHeaders()
      });
      if (res.ok) {
        return await res.json();
      }
    } catch (err) {
      console.warn("Error completing planner task:", err);
    }
  }
  return null;
}

export async function startStudySession(payload: { userId: string; taskId: string; subjectCode: string; conceptName: string }): Promise<any> {
  const online = await checkBackendConnection();
  if (online) {
    try {
      const res = await fetch(`${BACKEND_URL}/api/planner/session/start`, {
        method: "POST",
        headers: getAuthHeaders(),
        body: JSON.stringify(payload)
      });
      if (res.ok) {
        return await res.json();
      }
    } catch (err) {
      console.warn("Error starting study session:", err);
    }
  }
  return null;
}

export async function endStudySession(payload: { sessionId: string; actualDurationMinutes: number; pausedDurationMinutes: number; completionNotes: string }): Promise<any> {
  const online = await checkBackendConnection();
  if (online) {
    try {
      const res = await fetch(`${BACKEND_URL}/api/planner/session/end`, {
        method: "POST",
        headers: getAuthHeaders(),
        body: JSON.stringify(payload)
      });
      if (res.ok) {
        return await res.json();
      }
    } catch (err) {
      console.warn("Error ending study session:", err);
    }
  }
  return null;
}

// AI Tutor & Conversational LLM Infrastructure API Services

export async function fetchLLMProviderInfo(): Promise<{ provider: string; model: string }> {
  try {
    const res = await fetch(`${BACKEND_URL}/api/ai/provider-info`, {
      headers: getAuthHeaders()
    });
    if (res.ok) {
      return await res.json();
    }
  } catch (err) {
    console.warn("Error fetching LLM provider info:", err);
  }
  return { provider: "Google Gemini", model: "gemini-2.0-flash" };
}

export async function sendChatMessage(payload: { studentId: string; conversationId?: string; message: string; learningPlanTaskId?: string; referencedConcept?: string; learningMode?: string }): Promise<any> {
  const online = await checkBackendConnection();
  if (online) {
    try {
      const res = await fetch(`${BACKEND_URL}/api/ai/chat`, {
        method: "POST",
        headers: getAuthHeaders(),
        body: JSON.stringify(payload)
      });
      if (res.ok) {
        return await res.json();
      }
      const text = await res.text();
      return {
        conversationId: payload.conversationId || "conv_error",
        messageId: "msg_err_" + Date.now(),
        role: "assistant",
        content: text || JSON.stringify({
          success: false,
          provider: "Gemini",
          errorType: "HTTP_ERROR_" + res.status,
          message: `Backend returned HTTP ${res.status}`,
          suggestion: "Please check backend logs."
        }),
        timestamp: new Date().toISOString()
      };
    } catch (err) {
      console.warn("Error sending AI Tutor chat message:", err);
    }
  }
  return {
    conversationId: payload.conversationId || "conv_error",
    messageId: "msg_err_" + Date.now(),
    role: "assistant",
    content: JSON.stringify({
      success: false,
      provider: "Gemini",
      errorType: "NETWORK_ERROR",
      message: "Unable to connect to Spring Boot backend API service.",
      suggestion: "Please check backend service health on port 8080."
    }),
    timestamp: new Date().toISOString()
  };
}

export async function createNewConversation(studentId: string, title?: string, taskId?: string, concept?: string, learningMode?: string): Promise<any> {
  const online = await checkBackendConnection();
  if (online) {
    try {
      const query = new URLSearchParams({ studentId });
      if (title) query.append("title", title);
      if (taskId) query.append("taskId", taskId);
      if (concept) query.append("concept", concept);
      if (learningMode) query.append("mode", learningMode);

      const res = await fetch(`${BACKEND_URL}/api/ai/new-conversation?${query.toString()}`, {
        method: "POST",
        headers: getAuthHeaders()
      });
      if (res.ok) {
        return await res.json();
      }
    } catch (err) {
      console.warn("Error creating new AI conversation:", err);
    }
  }
  return null;
}

export async function fetchConversationHistory(studentId: string): Promise<any[]> {
  const online = await checkBackendConnection();
  if (online) {
    try {
      const res = await fetch(`${BACKEND_URL}/api/ai/history/${studentId}`, {
        headers: getAuthHeaders()
      });
      if (res.ok) {
        return await res.json();
      }
    } catch (err) {
      console.warn("Error fetching conversation history:", err);
    }
  }
  return [];
}

export async function fetchConversationById(conversationId: string): Promise<any> {
  const online = await checkBackendConnection();
  if (online) {
    try {
      const res = await fetch(`${BACKEND_URL}/api/ai/conversation/${conversationId}`, {
        headers: getAuthHeaders()
      });
      if (res.ok) {
        return await res.json();
      }
    } catch (err) {
      console.warn("Error fetching conversation details:", err);
    }
  }
  return null;
}

export async function deleteConversation(conversationId: string): Promise<boolean> {
  const online = await checkBackendConnection();
  if (online) {
    try {
      const res = await fetch(`${BACKEND_URL}/api/ai/conversation/${conversationId}`, {
        method: "DELETE",
        headers: getAuthHeaders()
      });
      return res.ok;
    } catch (err) {
      console.warn("Error deleting conversation:", err);
    }
  }
  return false;
}

export async function fetchStudentContext(studentId: string, concept?: string): Promise<any> {
  const online = await checkBackendConnection();
  if (online) {
    try {
      const query = concept ? `?concept=${encodeURIComponent(concept)}` : "";
      const res = await fetch(`${BACKEND_URL}/api/ai/context/${studentId}${query}`, {
        headers: getAuthHeaders()
      });
      if (res.ok) {
        return await res.json();
      }
    } catch (err) {
      console.warn("Error fetching student learning context:", err);
    }
  }
  return null;
}
