import { StudentProfile } from "../../services/mockData";

export interface DashboardHeaderProps {
  streak?: number;
  isBackendConnected?: boolean;
}

export interface WelcomeCardProps {
  firstName: string;
  profile: StudentProfile | null;
}

export interface TodaysLearningCardProps {
  profile: StudentProfile | null;
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
