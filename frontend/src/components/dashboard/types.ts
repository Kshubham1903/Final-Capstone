import { StudentProfile } from "../../services/mockData";

export interface SubjectCatalogItem {
  id: string;
  institution: string;
  degree: string;
  branch: string;
  semester: number;
  subjectCode: string;
  subjectName: string;
  credits: number;
  isActive: boolean;
}

export interface AcademicCatalogCardProps {
  currentBranch?: string;
  currentSemester?: number;
}

export interface DashboardHeaderProps {
  streak?: number;
  isBackendConnected?: boolean;
}

export interface WelcomeCardProps {
  firstName: string;
  profile: StudentProfile | null;
}

export interface AcademicProfileCardProps {
  profile: StudentProfile | null;
  predPerformanceLevel: string;
}

export interface AILearningInsightsCardProps {
  profile: StudentProfile | null;
  insights: string[];
  predPerformanceLevel: string;
  isLoading?: boolean;
  error?: string | null;
  isBackendConnected?: boolean;
  onRetry?: () => void;
}

export interface TodaysLearningCardProps {
  profile: StudentProfile | null;
}

export interface QuickActionsCardProps {
  className?: string;
}

export interface LearningProgressCardProps {
  profile: StudentProfile | null;
  onSelectSubject?: (subjectName: string, currentMastery: number) => void;
}

export interface DiagnosticAssessmentCardProps {
  profile: StudentProfile | null;
}

export interface KnowledgeProgressCardProps {
  profile: StudentProfile | null;
}

export interface RecentActivityCardProps {
  profile: StudentProfile | null;
}

export interface AITutorCardProps {
  className?: string;
}
