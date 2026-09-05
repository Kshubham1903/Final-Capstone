import React, { useState, useEffect } from "react";
import { 
  CheckCircle2, 
  HelpCircle, 
  Play, 
  RotateCcw, 
  Sparkles, 
  Award, 
  BookOpen, 
  ArrowRight, 
  Check, 
  AlertCircle,
  Loader2
} from "lucide-react";
import { 
  StudentProfile 
} from "../../services/mockData";
import { 
  generateDashboardTest, 
  submitDashboardTest, 
  getLatestDashboardTestResult, 
  DashboardTestQuestionDTO, 
  DashboardTestResultDTO 
} from "../../services/api";

interface SubjectKnowledgeTestProps {
  profile: StudentProfile | null;
}

export default function SubjectKnowledgeTest({ profile }: SubjectKnowledgeTestProps) {
  const [latestResult, setLatestResult] = useState<DashboardTestResultDTO | null>(null);
  const [isLoadingResult, setIsLoadingResult] = useState<boolean>(true);
  
  // Test execution states
  const [viewState, setViewState] = useState<"SUMMARY" | "TEST" | "RESULT">("SUMMARY");
  const [isGenerating, setIsGenerating] = useState<boolean>(false);
  const [isSubmitting, setIsSubmitting] = useState<boolean>(false);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  const [sessionId, setSessionId] = useState<string>("");
  const [questions, setQuestions] = useState<DashboardTestQuestionDTO[]>([]);
  const [currentIndex, setCurrentIndex] = useState<number>(0);
  const [selectedAnswers, setSelectedAnswers] = useState<Record<string, number>>({});
  const [currentResult, setCurrentResult] = useState<DashboardTestResultDTO | null>(null);

  const studentId = profile?.id || (typeof window !== "undefined" ? localStorage.getItem("edupilot_user_id") || "" : "");
  const subjects = profile?.subjects && profile.subjects.length > 0
    ? profile.subjects
    : ["Data Structures & Algorithms", "Database Management Systems", "Operating Systems", "Computer Networks", "Software Engineering"];

  const loadLatestResult = async () => {
    if (!studentId) {
      setIsLoadingResult(false);
      return;
    }
    setIsLoadingResult(true);
    try {
      const res = await getLatestDashboardTestResult(studentId);
      setLatestResult(res);
    } catch (err) {
      console.warn("Failed to load initial test results:", err);
    } finally {
      setIsLoadingResult(false);
    }
  };

  useEffect(() => {
    loadLatestResult();
  }, [studentId]);

  const handleStartTest = async () => {
    if (!studentId) {
      setErrorMessage("Student ID not found. Please log in again.");
      return;
    }
    setIsGenerating(true);
    setErrorMessage(null);
    try {
      const res = await generateDashboardTest(studentId);
      if (res.ok && res.sessionId && res.questions && res.questions.length > 0) {
        setSessionId(res.sessionId);
        setQuestions(res.questions);
        setCurrentIndex(0);
        setSelectedAnswers({});
        setViewState("TEST");
      } else {
        setErrorMessage(res.message || "Failed to generate test. Please try again.");
      }
    } catch (err: any) {
      setErrorMessage("Error starting knowledge test: " + err.message);
    } finally {
      setIsGenerating(false);
    }
  };

  const handleOptionSelect = (optionIndex: number) => {
    const currentQ = questions[currentIndex];
    if (!currentQ) return;
    setSelectedAnswers(prev => ({
      ...prev,
      [currentQ.questionId]: optionIndex
    }));
  };

  const handleNextQuestion = () => {
    if (currentIndex < questions.length - 1) {
      setCurrentIndex(prev => prev + 1);
    }
  };

  const handlePrevQuestion = () => {
    if (currentIndex > 0) {
      setCurrentIndex(prev => prev - 1);
    }
  };

  const handleSubmitTest = async () => {
    if (!sessionId || !studentId) return;
    setIsSubmitting(true);
    setErrorMessage(null);

    const answersList = questions.map(q => ({
      questionId: q.questionId,
      selectedOptionIndex: selectedAnswers[q.questionId] ?? -1
    }));

    try {
      const res = await submitDashboardTest({
        studentId,
        sessionId,
        answers: answersList
      });

      if (res.ok && res.result) {
        setCurrentResult(res.result);
        setLatestResult(res.result);
        setViewState("RESULT");
      } else {
        setErrorMessage(res.message || "Failed to evaluate submission.");
      }
    } catch (err: any) {
      setErrorMessage("Submission error: " + err.message);
    } finally {
      setIsSubmitting(false);
    }
  };

  const currentQ = questions[currentIndex];
  const totalQuestions = questions.length;
  const answeredCount = Object.keys(selectedAnswers).length;

  return (
    <div className="glass-panel p-6 rounded-2xl border border-white/5 space-y-5 bg-gradient-to-br from-purple-900/10 via-transparent to-pink-900/10 animate-fade-in">
      
      {/* Header Bar */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 border-b border-white/5 pb-4">
        <div className="flex items-center gap-2.5">
          <div className="h-9 w-9 rounded-xl bg-purple-500/10 border border-purple-500/20 flex items-center justify-center text-purple-theme">
            <Award className="h-5 w-5" />
          </div>
          <div>
            <h3 className="text-sm font-black text-main-theme uppercase tracking-wider flex items-center gap-2">
              <span>Subject Knowledge Check</span>
              <span className="text-[9px] px-2 py-0.5 rounded-full bg-purple-500/20 text-purple-300 font-bold tracking-normal uppercase border border-purple-500/30">
                5 Easy / Subject
              </span>
            </h3>
            <p className="text-[11px] text-secondary-theme leading-tight mt-0.5">
              Baseline knowledge test generated via Groq AI across your profile subjects.
            </p>
          </div>
        </div>

        {viewState === "SUMMARY" && (
          <button
            onClick={handleStartTest}
            disabled={isGenerating}
            className="px-4 py-2.5 rounded-xl bg-gradient-to-r from-purple-500 to-pink-500 text-white font-extrabold text-xs tracking-wider shadow-lg shadow-purple-500/20 hover:scale-105 active:scale-95 transition-all cursor-pointer flex items-center justify-center gap-2 disabled:opacity-50 shrink-0"
          >
            {isGenerating ? (
              <>
                <Loader2 className="h-4 w-4 animate-spin" />
                <span>Generating Test...</span>
              </>
            ) : (
              <>
                <Play className="h-3.5 w-3.5 fill-current" />
                <span>{latestResult ? "Retake Knowledge Check" : "Start Knowledge Check"}</span>
              </>
            )}
          </button>
        )}

        {viewState === "RESULT" && (
          <button
            onClick={() => setViewState("SUMMARY")}
            className="px-4 py-2 rounded-xl bg-white/5 hover:bg-white/10 border border-white/10 text-main-theme font-bold text-xs transition-all flex items-center gap-1.5 cursor-pointer"
          >
            <RotateCcw className="h-3.5 w-3.5" />
            <span>View Summary</span>
          </button>
        )}
      </div>

      {errorMessage && (
        <div className="p-3.5 rounded-xl bg-red-500/10 border border-red-500/20 text-red-300 text-xs flex items-center gap-2">
          <AlertCircle className="h-4 w-4 shrink-0" />
          <span>{errorMessage}</span>
        </div>
      )}

      {/* ====================================================================
          VIEW 1: SUMMARY / DASHBOARD WIDGET VIEW
          ==================================================================== */}
      {viewState === "SUMMARY" && (
        <div className="space-y-4">
          {/* Latest Result Banner if available */}
          {latestResult ? (
            <div className="p-4 rounded-xl bg-purple-500/10 border border-purple-500/20 flex flex-col sm:flex-row items-start sm:items-center justify-between gap-3">
              <div>
                <span className="text-[10px] text-secondary-theme uppercase font-bold tracking-wider block">Latest Overall Score</span>
                <div className="flex items-baseline gap-2">
                  <span className="text-2xl font-black text-emerald-400">{latestResult.overallPercentage.toFixed(1)}%</span>
                  <span className="text-xs text-secondary-theme font-medium">({latestResult.totalCorrect} / {latestResult.totalQuestions} correct)</span>
                </div>
              </div>
              <div className="text-[10px] text-secondary-theme bg-white/5 px-3 py-1.5 rounded-lg border border-white/5">
                Completed: {latestResult.createdAt ? new Date(latestResult.createdAt).toLocaleDateString() : "Recently"}
              </div>
            </div>
          ) : (
            <div className="p-4 rounded-xl bg-white/5 border border-white/5 text-center space-y-1">
              <Sparkles className="h-5 w-5 text-purple-400 mx-auto" />
              <p className="text-xs text-main-theme font-bold">No Baseline Test Conducted Yet</p>
              <p className="text-[11px] text-secondary-theme">Run your first knowledge check to log per-subject readiness scores.</p>
            </div>
          )}

          {/* Subject Scores List */}
          <div className="space-y-2.5">
            <span className="text-[10px] text-secondary-theme uppercase font-bold tracking-wider block">Your Enrolled Subjects</span>
            <div className="grid grid-cols-1 md:grid-cols-2 gap-3">
              {subjects.map((subject, idx) => {
                const pct = latestResult?.subjectScorePercentage?.[subject];
                const correctCount = latestResult?.correctCountPerSubject?.[subject];
                const hasScore = typeof pct === "number";

                return (
                  <div key={idx} className="glass-panel p-3.5 rounded-xl border border-white/5 flex items-center justify-between gap-3 hover:border-purple-500/20 transition-all">
                    <div className="flex items-center gap-2.5 min-w-0">
                      <div className="h-8 w-8 rounded-lg bg-white/5 border border-white/10 flex items-center justify-center text-purple-300 font-black text-xs shrink-0">
                        {idx + 1}
                      </div>
                      <div className="min-w-0">
                        <span className="text-xs font-bold text-main-theme block truncate">{subject}</span>
                        <span className="text-[10px] text-secondary-theme block">
                          {hasScore ? `${correctCount ?? Math.round((pct / 100) * 5)} / 5 Questions Correct` : "5 Baseline Questions"}
                        </span>
                      </div>
                    </div>

                    <div className="shrink-0 text-right">
                      {hasScore ? (
                        <span className={`text-sm font-black px-2.5 py-1 rounded-lg border ${
                          pct >= 80 ? "bg-emerald-500/10 text-emerald-400 border-emerald-500/20" :
                          pct >= 60 ? "bg-cyan-500/10 text-cyan-400 border-cyan-500/20" :
                          "bg-amber-500/10 text-amber-400 border-amber-500/20"
                        }`}>
                          {pct.toFixed(0)}%
                        </span>
                      ) : (
                        <span className="text-[10px] text-secondary-theme font-medium px-2 py-1 rounded-lg bg-white/5">
                          Pending
                        </span>
                      )}
                    </div>
                  </div>
                );
              })}
            </div>
          </div>
        </div>
      )}

      {/* ====================================================================
          VIEW 2: SEQUENTIAL TEST EXECUTION
          ==================================================================== */}
      {viewState === "TEST" && currentQ && (
        <div className="space-y-5 animate-fade-in">
          {/* Progress Header */}
          <div className="flex items-center justify-between text-xs text-secondary-theme">
            <span className="font-bold text-main-theme">Question {currentIndex + 1} of {totalQuestions}</span>
            <span>Answered {answeredCount} / {totalQuestions}</span>
          </div>

          <div className="w-full bg-white/5 h-2 rounded-full overflow-hidden">
            <div 
              className="bg-gradient-to-r from-purple-500 to-pink-500 h-full transition-all duration-300"
              style={{ width: `${((currentIndex + 1) / totalQuestions) * 100}%` }}
            />
          </div>

          {/* Question Card */}
          <div className="glass-panel p-5 rounded-2xl border border-purple-500/20 bg-purple-950/10 space-y-4">
            <div className="flex flex-wrap items-center justify-between gap-2 border-b border-white/5 pb-3">
              <span className="text-xs font-black text-purple-300 uppercase tracking-wider px-3 py-1 rounded-lg bg-purple-500/10 border border-purple-500/20">
                📚 {currentQ.subject}
              </span>
              <span className="text-[10px] font-bold text-secondary-theme">
                Concept: {currentQ.concept}
              </span>
            </div>

            <h4 className="text-sm md:text-base font-extrabold text-main-theme leading-relaxed">
              {currentQ.questionText}
            </h4>

            {/* Options */}
            <div className="space-y-2.5 pt-2">
              {currentQ.options?.map((opt, oIdx) => {
                const isSelected = selectedAnswers[currentQ.questionId] === oIdx;
                return (
                  <button
                    key={oIdx}
                    onClick={() => handleOptionSelect(oIdx)}
                    className={`w-full p-3.5 rounded-xl border text-left text-xs font-medium transition-all flex items-center justify-between gap-3 cursor-pointer ${
                      isSelected
                        ? "bg-purple-500/20 border-purple-500/50 text-white font-bold shadow-lg shadow-purple-500/10"
                        : "bg-white/5 border-white/5 hover:bg-white/10 text-secondary-theme"
                    }`}
                  >
                    <div className="flex items-center gap-3">
                      <span className={`h-6 w-6 rounded-lg flex items-center justify-center font-bold text-[11px] border ${
                        isSelected ? "bg-purple-500 text-white border-purple-400" : "bg-white/5 border-white/10 text-secondary-theme"
                      }`}>
                        {String.fromCharCode(65 + oIdx)}
                      </span>
                      <span>{opt}</span>
                    </div>

                    {isSelected && <Check className="h-4 w-4 text-purple-400 shrink-0" />}
                  </button>
                );
              })}
            </div>
          </div>

          {/* Navigation Controls */}
          <div className="flex items-center justify-between pt-2">
            <button
              onClick={handlePrevQuestion}
              disabled={currentIndex === 0}
              className="px-4 py-2 rounded-xl bg-white/5 border border-white/10 text-xs font-bold text-main-theme hover:bg-white/10 disabled:opacity-30 disabled:cursor-not-allowed cursor-pointer"
            >
              Previous
            </button>

            {currentIndex < totalQuestions - 1 ? (
              <button
                onClick={handleNextQuestion}
                className="px-5 py-2.5 rounded-xl bg-gradient-to-r from-purple-500 to-pink-500 text-white text-xs font-extrabold flex items-center gap-1.5 shadow-lg shadow-purple-500/20 hover:scale-105 cursor-pointer"
              >
                <span>Next Question</span>
                <ArrowRight className="h-4 w-4" />
              </button>
            ) : (
              <button
                onClick={handleSubmitTest}
                disabled={isSubmitting}
                className="px-6 py-2.5 rounded-xl bg-emerald-500 hover:bg-emerald-600 text-white text-xs font-extrabold flex items-center gap-2 shadow-lg shadow-emerald-500/20 hover:scale-105 disabled:opacity-50 cursor-pointer"
              >
                {isSubmitting ? (
                  <>
                    <Loader2 className="h-4 w-4 animate-spin" />
                    <span>Evaluating Results...</span>
                  </>
                ) : (
                  <>
                    <CheckCircle2 className="h-4 w-4" />
                    <span>Submit & Finish Test</span>
                  </>
                )}
              </button>
            )}
          </div>
        </div>
      )}

      {/* ====================================================================
          VIEW 3: TEST RESULTS DISPLAY
          ==================================================================== */}
      {viewState === "RESULT" && currentResult && (
        <div className="space-y-5 animate-fade-in">
          <div className="p-6 rounded-2xl bg-gradient-to-r from-purple-500/20 to-pink-500/20 border border-purple-500/30 text-center space-y-2">
            <Award className="h-10 w-10 text-purple-300 mx-auto" />
            <h3 className="text-base font-black text-main-theme">Knowledge Check Complete!</h3>
            <div className="text-3xl font-black text-emerald-400">
              {currentResult.overallPercentage.toFixed(1)}% Overall Score
            </div>
            <p className="text-xs text-secondary-theme">
              {currentResult.totalCorrect} out of {currentResult.totalQuestions} questions answered correctly across {Object.keys(currentResult.subjectScorePercentage || {}).length} subjects.
            </p>
          </div>

          <div className="space-y-3">
            <h4 className="text-xs font-extrabold text-main-theme uppercase tracking-wider">Per-Subject Performance Breakdown</h4>
            <div className="grid grid-cols-1 md:grid-cols-2 gap-3">
              {Object.entries(currentResult.subjectScorePercentage || {}).map(([sub, pct], idx) => {
                const correctCount = currentResult.correctCountPerSubject?.[sub] ?? Math.round((pct / 100) * 5);
                return (
                  <div key={idx} className="glass-panel p-4 rounded-xl border border-white/5 space-y-2">
                    <div className="flex items-center justify-between text-xs">
                      <span className="font-extrabold text-main-theme truncate">{sub}</span>
                      <span className="font-black text-purple-300">{pct.toFixed(0)}% ({correctCount}/5)</span>
                    </div>
                    <div className="w-full bg-white/5 h-2 rounded-full overflow-hidden">
                      <div 
                        className={`h-full rounded-full transition-all duration-500 ${
                          pct >= 80 ? "bg-emerald-400" : pct >= 60 ? "bg-cyan-400" : "bg-amber-400"
                        }`}
                        style={{ width: `${pct}%` }}
                      />
                    </div>
                  </div>
                );
              })}
            </div>
          </div>

          <div className="flex justify-end pt-2">
            <button
              onClick={() => setViewState("SUMMARY")}
              className="px-5 py-2.5 rounded-xl bg-purple-500 hover:bg-purple-600 text-white font-extrabold text-xs shadow-lg transition-all cursor-pointer"
            >
              Return to Dashboard
            </button>
          </div>
        </div>
      )}
    </div>
  );
}
