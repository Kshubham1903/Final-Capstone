import React, { useState, useEffect } from "react";
import { 
  BrainCircuit, X, Clock, CheckCircle2, AlertCircle, ArrowRight, ArrowLeft, Send, Award, RefreshCw, Layers, Check, HelpCircle
} from "lucide-react";
import { startDiagnosticAssessment, submitDiagnosticAssessment, fetchSubjectsByBranchAndSemester } from "../../services/api";

interface AssessmentRunnerProps {
  onClose: () => void;
  branch?: string;
  semester?: number;
  initialSubjectCode?: string;
}

export default function AssessmentRunner({
  onClose,
  branch = "Computer Science & Engineering",
  semester = 3,
  initialSubjectCode = "CS301"
}: AssessmentRunnerProps) {
  const [step, setStep] = useState<"SELECT_SUBJECT" | "TESTING" | "RESULT">(
    initialSubjectCode ? "TESTING" : "SELECT_SUBJECT"
  );
  
  const [selectedSubjectCode, setSelectedSubjectCode] = useState(initialSubjectCode);
  const [availableSubjects, setAvailableSubjects] = useState<any[]>([]);
  const [loadingSubjects, setLoadingSubjects] = useState(false);

  // Test state
  const [session, setSession] = useState<any>(null);
  const [currentIdx, setCurrentIdx] = useState(0);
  const [userAnswers, setUserAnswers] = useState<Record<string, number>>({});
  const [secondsRemaining, setSecondsRemaining] = useState(300); // 5 minute timer
  const [startingTest, setStartingTest] = useState(false);
  const [submitting, setSubmitting] = useState(false);

  // Result state
  const [assessmentResult, setAssessmentResult] = useState<any>(null);

  // Fetch subjects for branch & semester if needed
  useEffect(() => {
    async function loadCatalogSubjects() {
      setLoadingSubjects(true);
      const items = await fetchSubjectsByBranchAndSemester(branch, semester);
      setAvailableSubjects(items || []);
      setLoadingSubjects(false);
    }
    loadCatalogSubjects();
  }, [branch, semester]);

  // Start assessment session
  const handleStartSession = async (subjCode: string) => {
    setStartingTest(true);
    const userId = localStorage.getItem("edupilot_user_id") || "anonymous_student";
    
    try {
      const sess = await startDiagnosticAssessment({
        userId,
        branch,
        semester,
        subjectCode: subjCode,
        questionCount: 5
      });

      if (sess && sess.questions && sess.questions.length > 0) {
        setSession(sess);
        setSelectedSubjectCode(subjCode);
        setCurrentIdx(0);
        setUserAnswers({});
        setSecondsRemaining(300);
        setStep("TESTING");
      }
    } catch (err) {
      console.error("Failed to start assessment session:", err);
    } finally {
      setStartingTest(false);
    }
  };

  useEffect(() => {
    if (initialSubjectCode && !session && !startingTest) {
      handleStartSession(initialSubjectCode);
    }
  }, [initialSubjectCode]);

  // Timer interval during testing step
  useEffect(() => {
    if (step !== "TESTING") return;
    const timer = setInterval(() => {
      setSecondsRemaining((prev) => {
        if (prev <= 1) {
          clearInterval(timer);
          handleSubmitAssessment(); // Auto-submit on timer expiry
          return 0;
        }
        return prev - 1;
      });
    }, 1000);
    return () => clearInterval(timer);
  }, [step, session, userAnswers]);

  const formatTimer = (totalSeconds: number) => {
    const mins = Math.floor(totalSeconds / 60);
    const secs = totalSeconds % 60;
    return `${mins}:${secs < 10 ? "0" : ""}${secs}`;
  };

  const handleSelectOption = (questionId: string, optionIdx: number) => {
    setUserAnswers((prev) => ({ ...prev, [questionId]: optionIdx }));
  };

  const handleSubmitAssessment = async () => {
    if (!session || submitting) return;
    setSubmitting(true);

    const userId = localStorage.getItem("edupilot_user_id") || "anonymous_student";
    const answersList = (session.questions || []).map((q: any) => ({
      questionId: q.questionId,
      selectedOption: userAnswers[q.questionId] !== undefined ? userAnswers[q.questionId] : -1
    }));

    const timeTaken = 300 - secondsRemaining;

    try {
      const res = await submitDiagnosticAssessment({
        sessionId: session.sessionId,
        userId,
        timeTakenSeconds: timeTaken > 0 ? timeTaken : 30,
        answers: answersList
      });

      setAssessmentResult(res);
      setStep("RESULT");
    } catch (err) {
      console.error("Error submitting assessment:", err);
    } finally {
      setSubmitting(false);
    }
  };

  const questions = session?.questions || [];
  const currentQuestion = questions[currentIdx];

  const getMasteryBadgeClass = (level: string) => {
    switch (level) {
      case "MASTER":
      case "PROFICIENT":
        return "bg-emerald-500/10 text-emerald-400 border-emerald-500/20";
      case "INTERMEDIATE":
        return "bg-amber-500/10 text-amber-400 border-amber-500/20";
      default:
        return "bg-pink-500/10 text-pink-400 border-pink-500/20";
    }
  };

  return (
    <div className="fixed inset-0 bg-black/75 backdrop-blur-md z-50 flex items-center justify-center p-4 overflow-y-auto">
      <div className="glass-panel p-6 rounded-3xl border border-white/10 shadow-2xl w-full max-w-3xl space-y-6 my-auto max-h-[90vh] overflow-y-auto">
        
        {/* Top Header */}
        <div className="flex justify-between items-center border-b border-white/5 pb-4">
          <div className="flex items-center gap-3">
            <div className="h-10 w-10 rounded-xl bg-purple-500/10 border border-purple-500/20 flex items-center justify-center text-purple-theme">
              <BrainCircuit className="h-5 w-5" />
            </div>
            <div>
              <h3 className="text-base font-extrabold text-main-theme">Diagnostic Assessment Engine</h3>
              <p className="text-xs text-secondary-theme">
                {branch} • Semester {semester}
              </p>
            </div>
          </div>

          <button 
            onClick={onClose}
            className="p-1.5 rounded-lg bg-white/5 hover:bg-white/10 text-secondary-theme hover:text-main-theme font-bold cursor-pointer"
          >
            <X className="h-5 w-5" />
          </button>
        </div>

        {/* STEP 1: SELECT SUBJECT */}
        {step === "SELECT_SUBJECT" && (
          <div className="space-y-6 text-xs">
            <div className="space-y-1">
              <h4 className="text-sm font-extrabold text-main-theme">Select Assessment Subject</h4>
              <p className="text-secondary-theme">
                Choose a subject from your active Academic Catalog to evaluate your conceptual mastery.
              </p>
            </div>

            {loadingSubjects ? (
              <div className="p-8 text-center animate-pulse">
                <RefreshCw className="h-6 w-6 text-purple-theme animate-spin mx-auto mb-2" />
                <span className="text-secondary-theme font-bold">Loading Academic Catalog Subjects...</span>
              </div>
            ) : availableSubjects.length > 0 ? (
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                {availableSubjects.map((subj) => (
                  <div
                    key={subj.subjectCode}
                    onClick={() => handleStartSession(subj.subjectCode)}
                    className="glass-panel p-5 rounded-2xl border border-white/5 hover:border-purple-500/40 hover:bg-purple-500/5 transition-all cursor-pointer space-y-3 group"
                  >
                    <div className="flex justify-between items-center">
                      <span className="text-[10px] font-black uppercase text-purple-theme bg-purple-500/10 px-2 py-0.5 rounded border border-purple-500/20">
                        {subj.subjectCode}
                      </span>
                      <span className="text-[10px] font-bold text-emerald-theme bg-emerald-500/10 px-2 py-0.5 rounded border border-emerald-500/20">
                        {subj.credits || 4} Credits
                      </span>
                    </div>
                    <div>
                      <h5 className="font-extrabold text-sm text-main-theme group-hover:text-purple-theme transition-colors">
                        {subj.subjectName}
                      </h5>
                    </div>
                    <div className="pt-2 border-t border-white/5 flex justify-between items-center text-[10px] text-secondary-theme">
                      <span>5 Diagnostic Questions</span>
                      <span className="text-purple-theme font-bold flex items-center gap-1 group-hover:translate-x-1 transition-transform">
                        <span>Start Test</span>
                        <ArrowRight className="h-3 w-3" />
                      </span>
                    </div>
                  </div>
                ))}
              </div>
            ) : (
              <div className="p-6 bg-white/5 rounded-2xl text-center space-y-3">
                <p className="text-secondary-theme">No custom subjects found for this branch. Starting standard Computer Science diagnostic test.</p>
                <button
                  onClick={() => handleStartSession("CS301")}
                  className="px-6 py-2.5 rounded-xl bg-purple-600 hover:bg-purple-500 text-white font-bold"
                >
                  Start CS301 Assessment
                </button>
              </div>
            )}
          </div>
        )}

        {/* STEP 2: ACTIVE TEST EXECUTION */}
        {step === "TESTING" && (
          <div className="space-y-6">
            {/* Session Header Bar */}
            <div className="flex flex-wrap items-center justify-between gap-3 bg-white/5 p-4 rounded-2xl border border-white/5 text-xs">
              <div>
                <span className="text-[10px] font-black uppercase text-purple-theme bg-purple-500/10 px-2 py-0.5 rounded border border-purple-500/20 mr-2">
                  {session?.subjectCode}
                </span>
                <span className="font-bold text-main-theme">{session?.subjectName}</span>
              </div>

              {/* Countdown Timer */}
              <div className="flex items-center gap-2 font-mono font-bold text-amber-theme bg-amber-500/10 border border-amber-500/20 px-3 py-1 rounded-xl">
                <Clock className="h-4 w-4 animate-pulse" />
                <span>{formatTimer(secondsRemaining)}</span>
              </div>
            </div>

            {/* Question Navigator Pills */}
            <div className="flex items-center gap-2 overflow-x-auto pb-1">
              {questions.map((q: any, idx: number) => {
                const isAnswered = userAnswers[q.questionId] !== undefined;
                const isCurrent = idx === currentIdx;
                return (
                  <button
                    key={q.questionId}
                    onClick={() => setCurrentIdx(idx)}
                    className={`h-9 w-9 rounded-xl font-extrabold text-xs flex items-center justify-center transition-all cursor-pointer ${
                      isCurrent
                        ? "bg-purple-600 text-white shadow-lg shadow-purple-600/30 scale-105"
                        : isAnswered
                        ? "bg-emerald-500/20 text-emerald-400 border border-emerald-500/30"
                        : "bg-white/5 text-secondary-theme hover:bg-white/10"
                    }`}
                  >
                    {idx + 1}
                  </button>
                );
              })}
            </div>

            {/* Active Question Card */}
            {currentQuestion ? (
              <div className="space-y-5 bg-white/3 p-6 rounded-2xl border border-white/5">
                <div className="flex justify-between items-center text-xs">
                  <div className="flex items-center gap-2">
                    <span className="text-[10px] font-black uppercase text-cyan-theme bg-cyan-500/10 px-2.5 py-0.5 rounded-md border border-cyan-500/20">
                      Topic: {currentQuestion.topic || "Core Concept"}
                    </span>
                    <span className="text-[10px] font-bold text-secondary-theme">
                      {currentQuestion.difficulty} • {currentQuestion.marks} Marks
                    </span>
                  </div>
                  <span className="text-xs font-extrabold text-secondary-theme">
                    Question {currentIdx + 1} of {questions.length}
                  </span>
                </div>

                <h4 className="text-sm font-extrabold text-main-theme leading-relaxed">
                  {currentQuestion.questionText}
                </h4>

                {/* Options List */}
                <div className="space-y-2.5 pt-2">
                  {currentQuestion.options?.map((opt: string, optIdx: number) => {
                    const isSelected = userAnswers[currentQuestion.questionId] === optIdx;
                    return (
                      <div
                        key={optIdx}
                        onClick={() => handleSelectOption(currentQuestion.questionId, optIdx)}
                        className={`p-3.5 rounded-xl border text-xs font-semibold flex items-center justify-between cursor-pointer transition-all ${
                          isSelected
                            ? "bg-purple-500/20 border-purple-500/60 text-white shadow-md shadow-purple-500/10"
                            : "bg-white/5 border-white/5 text-secondary-theme hover:bg-white/10 hover:text-main-theme"
                        }`}
                      >
                        <div className="flex items-center gap-3">
                          <span className={`h-6 w-6 rounded-full font-black text-[10px] flex items-center justify-center ${
                            isSelected ? "bg-purple-500 text-white" : "bg-white/10 text-secondary-theme"
                          }`}>
                            {String.fromCharCode(65 + optIdx)}
                          </span>
                          <span>{opt}</span>
                        </div>
                        {isSelected && <Check className="h-4 w-4 text-purple-400" />}
                      </div>
                    );
                  })}
                </div>
              </div>
            ) : null}

            {/* Bottom Controls */}
            <div className="flex justify-between items-center pt-2">
              <button
                disabled={currentIdx === 0}
                onClick={() => setCurrentIdx((prev) => Math.max(0, prev - 1))}
                className="px-4 py-2 rounded-xl bg-white/5 hover:bg-white/10 disabled:opacity-30 text-xs font-bold text-main-theme flex items-center gap-1.5 cursor-pointer"
              >
                <ArrowLeft className="h-4 w-4" />
                <span>Previous</span>
              </button>

              {currentIdx < questions.length - 1 ? (
                <button
                  onClick={() => setCurrentIdx((prev) => Math.min(questions.length - 1, prev + 1))}
                  className="px-6 py-2.5 rounded-xl bg-purple-600 hover:bg-purple-500 text-white text-xs font-extrabold flex items-center gap-1.5 shadow-lg shadow-purple-600/20 cursor-pointer"
                >
                  <span>Next Question</span>
                  <ArrowRight className="h-4 w-4" />
                </button>
              ) : (
                <button
                  disabled={submitting}
                  onClick={handleSubmitAssessment}
                  className="px-6 py-2.5 rounded-xl bg-gradient-to-r from-emerald-600 to-teal-600 hover:from-emerald-500 hover:to-teal-500 text-white text-xs font-extrabold flex items-center gap-1.5 shadow-lg shadow-emerald-600/20 cursor-pointer"
                >
                  {submitting ? (
                    <>
                      <RefreshCw className="h-4 w-4 animate-spin" />
                      <span>Evaluating Results...</span>
                    </>
                  ) : (
                    <>
                      <Send className="h-4 w-4" />
                      <span>Submit Assessment</span>
                    </>
                  )}
                </button>
              )}
            </div>
          </div>
        )}

        {/* STEP 3: ASSESSMENT RESULT EVALUATION */}
        {step === "RESULT" && assessmentResult && (
          <div className="space-y-6">
            
            {/* Hero Result Banner */}
            <div className="glass-panel p-6 rounded-2xl border border-white/10 bg-gradient-to-r from-purple-900/20 via-transparent to-emerald-900/20 text-center space-y-3">
              <div className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full text-xs font-black uppercase border mb-1 border-white/10 bg-white/5">
                <Award className="h-4 w-4 text-purple-400" />
                <span>Diagnostic Assessment Result</span>
              </div>

              <div className="flex justify-center items-baseline gap-2">
                <span className="text-4xl font-black text-transparent bg-clip-text bg-gradient-to-r from-purple-400 via-pink-400 to-emerald-400">
                  {assessmentResult.percentage}%
                </span>
                <span className="text-xs text-secondary-theme">
                  ({assessmentResult.score} / {assessmentResult.totalMarks} Marks)
                </span>
              </div>

              <div className="flex justify-center items-center gap-3 text-xs pt-1">
                <span className={`px-3 py-1 rounded-full border text-xs font-black uppercase ${getMasteryBadgeClass(assessmentResult.masteryLevel)}`}>
                  {assessmentResult.masteryLevel} MASTERY
                </span>
                <span className="text-secondary-theme">
                  Accuracy: <strong className="text-emerald-400">{assessmentResult.accuracy}%</strong>
                </span>
              </div>
            </div>

            {/* 3 Metric Cards */}
            <div className="grid grid-cols-3 gap-3 text-center text-xs">
              <div className="p-3.5 bg-white/5 rounded-xl border border-white/5 space-y-1">
                <span className="text-[10px] text-secondary-theme uppercase font-extrabold">Correct</span>
                <div className="text-lg font-black text-emerald-400">{assessmentResult.correctAnswers}</div>
              </div>
              <div className="p-3.5 bg-white/5 rounded-xl border border-white/5 space-y-1">
                <span className="text-[10px] text-secondary-theme uppercase font-extrabold">Incorrect</span>
                <div className="text-lg font-black text-pink-400">{assessmentResult.incorrectAnswers}</div>
              </div>
              <div className="p-3.5 bg-white/5 rounded-xl border border-white/5 space-y-1">
                <span className="text-[10px] text-secondary-theme uppercase font-extrabold">Time Taken</span>
                <div className="text-lg font-black text-purple-400">{formatTimer(assessmentResult.timeTakenSeconds)}</div>
              </div>
            </div>

            {/* Topic Breakdown */}
            {assessmentResult.topicBreakdown && Object.keys(assessmentResult.topicBreakdown).length > 0 && (
              <div className="space-y-2">
                <h5 className="text-xs font-extrabold text-main-theme uppercase tracking-wider">Topic Mastery Breakdown</h5>
                <div className="grid grid-cols-1 sm:grid-cols-2 gap-2">
                  {Object.entries(assessmentResult.topicBreakdown).map(([topName, stat]: [string, any]) => (
                    <div key={topName} className="p-3 rounded-xl bg-white/5 border border-white/5 flex justify-between items-center text-xs">
                      <span className="font-semibold text-secondary-theme">{topName}</span>
                      <span className="font-extrabold text-emerald-400">{stat.percentage}%</span>
                    </div>
                  ))}
                </div>
              </div>
            )}

            {/* Return Button */}
            <div className="pt-4 border-t border-white/10 flex justify-end">
              <button
                onClick={onClose}
                className="px-6 py-2.5 rounded-xl bg-purple-600 hover:bg-purple-500 text-white font-extrabold text-xs shadow-lg shadow-purple-600/20 transition-all cursor-pointer"
              >
                Return to Dashboard
              </button>
            </div>
          </div>
        )}

      </div>
    </div>
  );
}
