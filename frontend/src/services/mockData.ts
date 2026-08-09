export interface LifestyleLog {
  date: string;
  sleepHours: number;
  screenTimeHours: number;
  stressLevel: number; // 1-10
  exerciseMinutes: number;
  studyMinutes: number;
  productivityRating: number; // 1-10
  attendanceRate: number; // %
}

export interface QuizQuestion {
  id: string;
  subject: string;
  concept: string;
  difficulty: "EASY" | "MEDIUM" | "HARD";
  questionText: string;
  options: string[];
  correctOptionIndex: number;
  conceptualExplanation: string;
}

export interface StudentProfile {
  id: string;
  userId?: string;
  fullName: string;
  email: string;
  isCompleted?: boolean;
  institution?: string;
  degree?: string;
  branch?: string;
  course: string;
  semester: number;
  subjects: string[];
  careerGoals: string[];
  preferredStudyHoursPerDay: number;
  currentCgpa: number;
  targetCgpa: number;
  
  consistencyScore: number; // 0-100
  productivityScore: number; // 0-100
  lifestyleScore: number; // 0-100
  learningStyle: string;
  currentStreakCount: number;
  studentGrowthIndex: number; // SGI 0.0 - 10.0
  predictedCgpa: number;
  academicRiskLevel: "LOW" | "MEDIUM" | "HIGH";
  
  conceptMastery: Record<string, number>; // Subject -> %
  weakConcepts: Record<string, string[]>;
  strongConcepts: Record<string, string[]>;
  
  lifestyleHistory: LifestyleLog[];
  badges: { name: string; icon: string; description: string }[];
  completedQuizzesCount: number;
  weeklyGoalCompletion: number; // %
}

// Dynamic Question Bank (starts empty, populated from faculty authoring or API)
export const QUESTION_BANK: QuizQuestion[] = [];

// Default Empty Student Profile Template for New Registered Users
export const EMPTY_STUDENT_PROFILE: StudentProfile = {
  id: "",
  fullName: "",
  email: "",
  institution: "EduPilot Academy",
  degree: "B.Tech",
  branch: "Computer Science & Engineering",
  course: "Computer Science & Engineering",
  semester: 1,
  currentCgpa: 8.0,
  targetCgpa: 8.5,
  subjects: [],
  careerGoals: [],
  preferredStudyHoursPerDay: 0,
  
  consistencyScore: 0,
  productivityScore: 0,
  lifestyleScore: 0,
  learningStyle: "Visual",
  currentStreakCount: 0,
  studentGrowthIndex: 0.0,
  predictedCgpa: 0.0,
  academicRiskLevel: "LOW",
  
  conceptMastery: {},
  weakConcepts: {},
  strongConcepts: {},
  
  lifestyleHistory: [],
  badges: [],
  completedQuizzesCount: 0,
  weeklyGoalCompletion: 0
};

// Class Performance Default Container for Faculty View
export const MOCK_CLASS_DATA = {
  averageSgi: 0.0,
  averageCgpa: 0.0,
  atRiskCount: 0,
  quizCompletionRate: 0,
  subjectBreakdown: [],
  students: [] as Array<{
    id: string;
    name: string;
    sgi: number;
    cgpa: number;
    risk: "LOW" | "MEDIUM" | "HIGH";
    status: string;
    attendance: number;
    primaryIssue?: string;
  }>
};

// Helper methods to read/write state with localStorage fallback
export function getStoredStudentProfile(): StudentProfile {
  if (typeof window === "undefined") return EMPTY_STUDENT_PROFILE;
  const stored = localStorage.getItem("edupilot_student_profile");
  if (!stored) {
    return EMPTY_STUDENT_PROFILE;
  }
  return JSON.parse(stored);
}

export function saveStudentProfile(profile: StudentProfile) {
  if (typeof window !== "undefined") {
    localStorage.setItem("edupilot_student_profile", JSON.stringify(profile));
  }
}

export function calculateLocalSgi(profile: StudentProfile): number {
  const academicScore = ((profile.currentCgpa || 8.0) / 10.0) * 100.0;
  
  const masteryValues = Object.values(profile.conceptMastery || {});
  const avgMastery = masteryValues.length > 0 ? masteryValues.reduce((a,b)=>a+b, 0) / masteryValues.length : 50.0;
  
  const latestLog = profile.lifestyleHistory && profile.lifestyleHistory.length > 0 
    ? profile.lifestyleHistory[profile.lifestyleHistory.length - 1] 
    : { sleepHours: 7.5, stressLevel: 5, exerciseMinutes: 30, screenTimeHours: 4.0, studyMinutes: 180 };
  
  const sleepPoints = 100 - Math.min(Math.abs(latestLog.sleepHours - 8.0) * 20, 100);
  const stressPoints = (10 - latestLog.stressLevel) * 10;
  const exercisePoints = Math.min((latestLog.exerciseMinutes / 30.0) * 100, 100);
  const screenPoints = Math.max(100 - (latestLog.screenTimeHours * 15), 0);
  const studyPoints = Math.min((latestLog.studyMinutes / 240.0) * 100, 100);
  
  const lifestyleScore = (sleepPoints + stressPoints + exercisePoints + screenPoints + studyPoints) / 5.0;
  const consistencyPoints = Math.min((profile.completedQuizzesCount / 10.0) * 100, 100);
  
  const sgi = ((academicScore * 0.40) + (avgMastery * 0.30) + (lifestyleScore * 0.20) + (consistencyPoints * 0.10)) / 10.0;
  return Math.min(Math.max(Number(sgi.toFixed(2)), 0), 10);
}
