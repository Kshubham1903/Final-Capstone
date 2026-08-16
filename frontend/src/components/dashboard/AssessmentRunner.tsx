import React, { useState, useEffect } from "react";
import {
  BrainCircuit, X, Clock, CheckCircle2, AlertCircle, ArrowRight, ArrowLeft, Send, Award, RefreshCw, Layers, Check, HelpCircle
} from "lucide-react";
import {
  startDiagnosticAssessment,
  submitDiagnosticAssessment,
  fetchSubjectsByBranchAndSemester,
  startAdaptiveDiagnosticSession,
  fetchNextAdaptiveQuestion,
  submitAdaptiveQuestionAnswer,
  fetchNextInitialDiagnosticQuestion,
  submitInitialDiagnosticAnswer,
  fetchKnowledgeProfile
} from "../../services/api";

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
  const [step, setStep] = useState<"SELECT_SUBJECT" | "TESTING" | "RESULT" | "ADAPTIVE_TESTING" | "ADAPTIVE_RESULT">(
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

  // Adaptive 1-by-1 Test State
  const [adaptiveSessionId, setAdaptiveSessionId] = useState<string | null>(null);
  const [adaptiveQuestion, setAdaptiveQuestion] = useState<any>(null);
  const [adaptiveQuestionNumber, setAdaptiveQuestionNumber] = useState(1);
  const [adaptiveMaxQuestions, setAdaptiveMaxQuestions] = useState(15);
  const [adaptiveSelectedOption, setAdaptiveSelectedOption] = useState<number | null>(null);
  const [adaptiveSubmitting, setAdaptiveSubmitting] = useState(false);
  const [adaptiveFeedback, setAdaptiveFeedback] = useState<any>(null);
  const [adaptiveStartTime, setAdaptiveStartTime] = useState<number>(Date.now());
  const [startingAdaptive, setStartingAdaptive] = useState(false);
  const [finalSkillProfile, setFinalSkillProfile] = useState<any>(null);

  const adaptiveNextRequestInFlightRef = React.useRef<boolean>(false);
  const adaptiveRequestSequenceRef = React.useRef<number>(0);
  const highestQuestionNumberSeenRef = React.useRef<number>(0);
  const displayedAdaptiveFingerprintsRef = React.useRef<Set<string>>(new Set());
  const currentAdaptiveSessionIdRef = React.useRef<string | null>(null);

  // Auto-fetch final consolidated Skill Profile when Stage 2 completes
  useEffect(() => {
    if (step === "ADAPTIVE_RESULT") {
      async function loadFinalSkillProfile() {
        const userId = getStudentUserId();
        if (userId) {
          const profileData = await fetchKnowledgeProfile(userId);
          setFinalSkillProfile(profileData);
        }
      }
      loadFinalSkillProfile();
    }
  }, [step]);

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

  // Helper to resolve student identity
  const getStudentUserId = () => {
    return localStorage.getItem("edupilot_user_id") || localStorage.getItem("edupilot_profile_id") || "";
  };

  // Start assessment session
  const handleStartSession = async (subjCode: string) => {
    setStartingTest(true);
    const userId = getStudentUserId();

    try {
      const sess = await startDiagnosticAssessment({
        userId,
        branch,
        semester,
        subjectCode: subjCode,
        questionCount: 5
      });

      if (sess && sess.sessionId) {
        setSession(sess);
        setSelectedSubjectCode(subjCode);
        setCurrentIdx(0);
        setUserAnswers({});
        setSecondsRemaining(300);

        // Fetch 1-by-1 Groq generated initial diagnostic question
        const nextRes = await fetchNextInitialDiagnosticQuestion({ sessionId: sess.sessionId });
        if (nextRes && nextRes.question) {
          setAdaptiveQuestion(nextRes.question);
          setAdaptiveQuestionNumber(nextRes.questionNumber || 1);
          setAdaptiveMaxQuestions(nextRes.totalQuestions || 10);
          setAdaptiveSelectedOption(null);
          setAdaptiveFeedback(null);
          setAdaptiveStartTime(Date.now());
          setStep("TESTING");
        }
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
          handleSubmitInitialQuestionAnswer(); // Auto-submit active question on timer expiry
          return 0;
        }
        return prev - 1;
      });
    }, 1000);
    return () => clearInterval(timer);
  }, [step, session, adaptiveSelectedOption]);

  const formatTimer = (totalSeconds: number) => {
    const mins = Math.floor(totalSeconds / 60);
    const secs = totalSeconds % 60;
    return `${mins}:${secs < 10 ? "0" : ""}${secs}`;
  };

  const handleSelectOption = (questionId: string, optionIdx: number) => {
    setUserAnswers((prev) => ({ ...prev, [questionId]: optionIdx }));
  };

  const handleSubmitInitialQuestionAnswer = async () => {
    if (adaptiveSelectedOption === null || !adaptiveQuestion || !session) return;
    setSubmitting(true);
    const timeTaken = Math.max(1, Math.round((Date.now() - adaptiveStartTime) / 1000));
    try {
      const submitRes = await submitInitialDiagnosticAnswer({
        sessionId: session.sessionId,
        questionId: adaptiveQuestion.questionId,
        selectedOption: adaptiveSelectedOption,
        responseTimeSeconds: timeTaken
      });
      setAdaptiveFeedback(submitRes);
      if (submitRes && submitRes.completed && submitRes.result) {
        setAssessmentResult(submitRes.result);
      }
    } catch (err) {
      console.error("Failed to submit initial question answer:", err);
    } finally {
      setSubmitting(false);
    }
  };

  const handleNextInitialQuestionStep = async () => {
    if (!session) return;
    if (adaptiveFeedback && adaptiveFeedback.completed) {
      if (adaptiveFeedback.result) {
        setAssessmentResult(adaptiveFeedback.result);
      }
      setStep("RESULT");
      return;
    }

    if (adaptiveNextRequestInFlightRef.current) {
      console.warn(`[AdaptiveRunner] Initial question request already in flight for session ${session.sessionId}. Ignoring duplicate call.`);
      return;
    }

    adaptiveNextRequestInFlightRef.current = true;
    const requestId = ++adaptiveRequestSequenceRef.current;
    setSubmitting(true);

    try {
      const nextRes = await fetchNextInitialDiagnosticQuestion({ sessionId: session.sessionId });

      if (requestId !== adaptiveRequestSequenceRef.current) {
        console.warn(`[AdaptiveRunner] Ignored stale initial response requestId=${requestId}`);
        return;
      }

      if (nextRes && nextRes.completed) {
        if (nextRes.result) {
          setAssessmentResult(nextRes.result);
        }
        setStep("RESULT");
      } else if (nextRes && nextRes.question) {
        const incomingQNum = nextRes.questionNumber || (adaptiveQuestionNumber + 1);

        if (incomingQNum < highestQuestionNumberSeenRef.current) {
          console.warn(`[AdaptiveRunner] Ignored regressive initial questionNumber=${incomingQNum} (highestSeen=${highestQuestionNumberSeenRef.current})`);
          return;
        }

        const qFp = nextRes.question.questionFingerprint || nextRes.question.questionId || nextRes.question.id || nextRes.question.questionText;
        if (qFp && displayedAdaptiveFingerprintsRef.current.has(qFp)) {
          console.warn(`[AdaptiveRunner] Ignored duplicate initial question fingerprint="${qFp}"`);
          return;
        }

        highestQuestionNumberSeenRef.current = incomingQNum;
        if (qFp) displayedAdaptiveFingerprintsRef.current.add(qFp);

        setAdaptiveQuestion(nextRes.question);
        setAdaptiveQuestionNumber(incomingQNum);
        setAdaptiveMaxQuestions(nextRes.totalQuestions || 10);
        setAdaptiveSelectedOption(null);
        setAdaptiveFeedback(null);
        setAdaptiveStartTime(Date.now());
      }
    } catch (err) {
      console.error("Failed to fetch next initial question:", err);
    } finally {
      if (requestId === adaptiveRequestSequenceRef.current) {
        setSubmitting(false);
      }
      adaptiveNextRequestInFlightRef.current = false;
    }
  };

  const handleStartAdaptiveSession = async () => {
    if (!assessmentResult || !assessmentResult.sessionId) return;
    setStartingAdaptive(true);
    try {
      const startRes = await startAdaptiveDiagnosticSession({
        diagnosticSessionId: assessmentResult.sessionId,
        subjectCode: selectedSubjectCode
      });
      if (startRes && startRes.adaptiveSessionId) {
        setAdaptiveSessionId(startRes.adaptiveSessionId);
        setAdaptiveMaxQuestions(startRes.totalQuestions || 10);
        if (startRes.completed) {
          setStep("ADAPTIVE_RESULT");
          setStartingAdaptive(false);
          return;
        }
        const nextRes = await fetchNextAdaptiveQuestion({ adaptiveSessionId: startRes.adaptiveSessionId });
        if (nextRes && nextRes.completed) {
          setStep("ADAPTIVE_RESULT");
        } else if (nextRes && nextRes.question) {
          setAdaptiveQuestion(nextRes.question);
          setAdaptiveQuestionNumber(nextRes.questionNumber || 1);
          setAdaptiveSelectedOption(null);
          setAdaptiveFeedback(null);
          setAdaptiveStartTime(Date.now());
          setStep("ADAPTIVE_TESTING");
        }
      }
    } catch (err) {
      console.error("Failed to start adaptive session:", err);
    } finally {
      setStartingAdaptive(false);
    }
  };

  const handleSubmitAdaptiveAnswer = async () => {
    if (adaptiveSelectedOption === null || !adaptiveQuestion || !adaptiveSessionId) return;
    setAdaptiveSubmitting(true);
    const timeTaken = Math.max(1, Math.round((Date.now() - adaptiveStartTime) / 1000));
    try {
      const submitRes = await submitAdaptiveQuestionAnswer({
        adaptiveSessionId,
        questionId: adaptiveQuestion.questionId,
        selectedOption: adaptiveSelectedOption,
        responseTimeSeconds: timeTaken
      });
      setAdaptiveFeedback(submitRes);
    } catch (err) {
      console.error("Failed to submit adaptive answer:", err);
    } finally {
      setAdaptiveSubmitting(false);
    }
  };

  const handleNextAdaptiveStep = async () => {
    if (!adaptiveSessionId) return;

    if (currentAdaptiveSessionIdRef.current !== adaptiveSessionId) {
      currentAdaptiveSessionIdRef.current = adaptiveSessionId;
      highestQuestionNumberSeenRef.current = 5;
      displayedAdaptiveFingerprintsRef.current.clear();
      adaptiveRequestSequenceRef.current = 0;
    }

    if (adaptiveNextRequestInFlightRef.current) {
      console.warn(`[AdaptiveRunner] Next question request already in flight for session ${adaptiveSessionId}. Ignoring duplicate call.`);
      return;
    }

    adaptiveNextRequestInFlightRef.current = true;
    const requestId = ++adaptiveRequestSequenceRef.current;
    setAdaptiveSubmitting(true);

    try {
      const nextRes = await fetchNextAdaptiveQuestion({ adaptiveSessionId });

      if (requestId !== adaptiveRequestSequenceRef.current) {
        console.warn(`[AdaptiveRunner] Ignored stale response requestId=${requestId}`);
        return;
      }

      if (nextRes && nextRes.completed) {
        setStep("ADAPTIVE_RESULT");
      } else if (nextRes && nextRes.question) {
        const incomingQNum = nextRes.questionNumber || (adaptiveQuestionNumber + 1);

        if (incomingQNum < highestQuestionNumberSeenRef.current) {
          console.warn(`[AdaptiveRunner] Ignored regressive questionNumber=${incomingQNum} (highestSeen=${highestQuestionNumberSeenRef.current})`);
          return;
        }

        const qFp = nextRes.question.questionFingerprint || nextRes.question.questionId || nextRes.question.id || nextRes.question.questionText;
        if (qFp && displayedAdaptiveFingerprintsRef.current.has(qFp)) {
          console.warn(`[AdaptiveRunner] Ignored duplicate question fingerprint="${qFp}"`);
          return;
        }

        highestQuestionNumberSeenRef.current = incomingQNum;
        if (qFp) displayedAdaptiveFingerprintsRef.current.add(qFp);

        setAdaptiveQuestion(nextRes.question);
        setAdaptiveQuestionNumber(incomingQNum);
        setAdaptiveMaxQuestions(nextRes.totalQuestions || 10);
        setAdaptiveSelectedOption(null);
        setAdaptiveFeedback(null);
        setAdaptiveStartTime(Date.now());
      }
    } catch (err) {
      console.error("Failed to fetch next adaptive question:", err);
    } finally {
      if (requestId === adaptiveRequestSequenceRef.current) {
        setAdaptiveSubmitting(false);
      }
      adaptiveNextRequestInFlightRef.current = false;
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

        {/* STEP 2: ACTIVE 1-BY-1 INITIAL DIAGNOSTIC EXECUTION */}
        {step === "TESTING" && adaptiveQuestion && (
          <div className="space-y-6">
            <div className="flex justify-between items-center text-xs text-secondary-theme font-extrabold pb-2 border-b border-white/10">
              <div className="flex items-center gap-2">
                <span className="px-2.5 py-1 rounded-full bg-purple-500/20 text-purple-300 font-bold border border-purple-500/30 uppercase text-[10px]">
                  Stage 1 Diagnostic • Item {adaptiveQuestionNumber} of {adaptiveMaxQuestions}
                </span>
                <span className="px-2.5 py-1 rounded-full bg-cyan-500/20 text-cyan-300 font-bold border border-cyan-500/30 uppercase text-[10px]">
                  {adaptiveQuestion.concept}
                </span>
                <span className="px-2.5 py-1 rounded-full bg-white/10 text-white font-bold border border-white/10 uppercase text-[10px]">
                  {adaptiveQuestion.difficulty}
                </span>
              </div>
              <div className="flex items-center gap-2 font-mono font-bold text-amber-theme bg-amber-500/10 border border-amber-500/20 px-3 py-1 rounded-xl">
                <Clock className="h-4 w-4 animate-pulse" />
                <span>{formatTimer(secondsRemaining)}</span>
              </div>
            </div>

            <div className="glass-panel p-5 rounded-2xl border border-white/10 space-y-2 bg-white/5">
              <h4 className="text-sm font-bold text-main-theme leading-relaxed">
                {adaptiveQuestion.questionText}
              </h4>
            </div>

            <div className="space-y-2.5">
              {adaptiveQuestion.options && adaptiveQuestion.options.map((optText: string, oIdx: number) => {
                const isSelected = adaptiveSelectedOption === oIdx;
                let optionClass = "border-white/10 bg-white/5 text-secondary-theme hover:bg-white/10 hover:border-white/20";
                if (isSelected) {
                  optionClass = "border-purple-500 bg-purple-500/20 text-white font-bold shadow-lg shadow-purple-500/20";
                }
                return (
                  <button
                    key={oIdx}
                    disabled={submitting || adaptiveFeedback !== null}
                    onClick={() => setAdaptiveSelectedOption(oIdx)}
                    className={`w-full p-4 rounded-xl border text-left text-xs transition-all flex items-center justify-between cursor-pointer ${optionClass}`}
                  >
                    <div className="flex items-center gap-3">
                      <span className="h-6 w-6 rounded-full border border-white/20 flex items-center justify-center text-[10px] font-extrabold uppercase bg-black/30">
                        {String.fromCharCode(65 + oIdx)}
                      </span>
                      <span>{optText}</span>
                    </div>
                    {isSelected && <CheckCircle2 className="h-4 w-4 text-purple-400 shrink-0" />}
                  </button>
                );
              })}
            </div>

            {adaptiveFeedback && (
              <div className={`p-4 rounded-xl border text-xs space-y-2 ${adaptiveFeedback.isCorrect ? "bg-emerald-500/10 border-emerald-500/30 text-emerald-300" : "bg-pink-500/10 border-pink-500/30 text-pink-300"}`}>
                <div className="flex items-center gap-2 font-bold uppercase text-[10px]">
                  {adaptiveFeedback.isCorrect ? <CheckCircle2 className="h-4 w-4 text-emerald-400" /> : <AlertCircle className="h-4 w-4 text-pink-400" />}
                  <span>{adaptiveFeedback.isCorrect ? "Correct Answer!" : "Incorrect Answer"}</span>
                  <span className="ml-auto text-[9px] px-2 py-0.5 rounded bg-black/40 font-mono">
                    Concept Status: {adaptiveFeedback.updatedConceptStatus} ({adaptiveFeedback.updatedConceptConfidence}% Conf)
                  </span>
                </div>
                <p className="text-secondary-theme leading-relaxed">
                  {adaptiveFeedback.explanation}
                </p>
              </div>
            )}

            <div className="pt-4 border-t border-white/10 flex justify-between items-center">
              <button
                onClick={onClose}
                className="px-4 py-2 rounded-xl bg-white/5 hover:bg-white/10 text-secondary-theme text-xs font-bold transition-all cursor-pointer"
              >
                Cancel Assessment
              </button>

              {!adaptiveFeedback ? (
                <button
                  disabled={adaptiveSelectedOption === null || submitting}
                  onClick={handleSubmitInitialQuestionAnswer}
                  className="px-6 py-2.5 rounded-xl bg-purple-600 hover:bg-purple-500 disabled:opacity-50 text-white font-extrabold text-xs shadow-lg shadow-purple-600/30 flex items-center gap-2 transition-all cursor-pointer"
                >
                  {submitting ? <RefreshCw className="h-4 w-4 animate-spin" /> : <Send className="h-4 w-4" />}
                  <span>Submit Answer</span>
                </button>
              ) : (
                <button
                  disabled={submitting}
                  onClick={handleNextInitialQuestionStep}
                  className="px-6 py-2.5 rounded-xl bg-gradient-to-r from-purple-600 to-pink-600 hover:from-purple-500 hover:to-pink-500 text-white font-extrabold text-xs shadow-lg shadow-purple-600/30 flex items-center gap-2 transition-all cursor-pointer"
                >
                  {submitting ? <RefreshCw className="h-4 w-4 animate-spin" /> : <ArrowRight className="h-4 w-4" />}
                  <span>Next Question</span>
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

            {/* Adaptive Assessment Handoff Bridge */}
            {assessmentResult.adaptiveEligible && assessmentResult.targetAdaptiveConcepts && assessmentResult.targetAdaptiveConcepts.length > 0 ? (
              <div className="glass-panel p-5 rounded-2xl border border-amber-500/30 bg-amber-500/5 space-y-3">
                <div className="flex items-center gap-2 text-amber-400">
                  <AlertCircle className="h-4 w-4" />
                  <h5 className="text-xs font-extrabold uppercase tracking-wider">Adaptive Verification Required</h5>
                </div>
                <p className="text-xs text-secondary-theme leading-relaxed">
                  The initial diagnostic identified <strong className="text-amber-300">{assessmentResult.targetAdaptiveConcepts.length} concept(s)</strong> requiring targeted adaptive testing to establish a high-confidence mastery profile.
                </p>

                <div className="flex flex-wrap gap-2 pt-1">
                  {assessmentResult.conceptEvaluations ? (
                    assessmentResult.conceptEvaluations.map((evalItem: any) => (
                      <div
                        key={evalItem.concept}
                        className={`px-3 py-1 rounded-xl text-[11px] font-bold border flex items-center gap-2 ${evalItem.requiresAdaptiveTesting
                            ? "bg-amber-500/10 border-amber-500/30 text-amber-300"
                            : "bg-emerald-500/10 border-emerald-500/30 text-emerald-300"
                          }`}
                      >
                        <span>{evalItem.concept}</span>
                        <span className="text-[9px] uppercase px-1.5 py-0.2 rounded bg-black/40 font-mono">
                          {evalItem.status} ({evalItem.confidence}% Conf)
                        </span>
                      </div>
                    ))
                  ) : (
                    assessmentResult.targetAdaptiveConcepts.map((cName: string) => (
                      <span key={cName} className="px-3 py-1 rounded-xl text-[11px] font-bold bg-amber-500/10 border border-amber-500/30 text-amber-300">
                        {cName} (UNCERTAIN)
                      </span>
                    ))
                  )}
                </div>

                <div className="pt-3 border-t border-white/10 flex flex-col sm:flex-row justify-between items-center gap-3">
                  <span className="text-[11px] text-secondary-theme italic">
                    Stage 1 Complete • Ready for Stage 2 Adaptive Assessment
                  </span>
                  <button
                    disabled={startingAdaptive}
                    onClick={handleStartAdaptiveSession}
                    className="w-full sm:w-auto px-5 py-2.5 rounded-xl bg-gradient-to-r from-purple-600 to-pink-600 hover:from-purple-500 hover:to-pink-500 disabled:opacity-50 text-white font-extrabold text-xs shadow-lg shadow-purple-600/30 flex items-center justify-center gap-2 cursor-pointer transition-all"
                  >
                    {startingAdaptive ? <RefreshCw className="h-4 w-4 animate-spin" /> : <BrainCircuit className="h-4 w-4" />}
                    <span>Start Adaptive Assessment</span>
                    <ArrowRight className="h-4 w-4" />
                  </button>
                </div>
              </div>
            ) : (
              <div className="glass-panel p-4 rounded-2xl border border-emerald-500/30 bg-emerald-500/5 flex items-center justify-between text-xs text-emerald-300 font-bold">
                <div className="flex items-center gap-2">
                  <CheckCircle2 className="h-4 w-4 text-emerald-400" />
                  <span>Diagnostic Assessment Complete — All concepts verified with high confidence.</span>
                </div>
              </div>
            )}

            {/* Return Button */}
            <div className="pt-4 border-t border-white/10 flex justify-end">
              <button
                onClick={onClose}
                className="px-6 py-2.5 rounded-xl bg-white/10 hover:bg-white/20 text-white font-extrabold text-xs transition-all cursor-pointer"
              >
                Return to Dashboard
              </button>
            </div>
          </div>
        )}

        {/* STEP 4: 1-BY-1 STAGE 2 ADAPTIVE TESTING */}
        {step === "ADAPTIVE_TESTING" && adaptiveQuestion && (
          <div className="space-y-6">
            <div className="flex justify-between items-center text-xs text-secondary-theme font-extrabold pb-2 border-b border-white/10">
              <div className="flex items-center gap-2">
                <span className="px-2.5 py-1 rounded-full bg-amber-500/20 text-amber-300 font-bold border border-amber-500/30 uppercase text-[10px]">
                  Stage 2 Adaptive • Item {adaptiveQuestionNumber} of {adaptiveMaxQuestions}
                </span>
                <span className="px-2.5 py-1 rounded-full bg-purple-500/20 text-purple-300 font-bold border border-purple-500/30 uppercase text-[10px]">
                  {adaptiveQuestion.concept}
                </span>
                <span className="px-2.5 py-1 rounded-full bg-white/10 text-white font-bold border border-white/10 uppercase text-[10px]">
                  {adaptiveQuestion.difficulty}
                </span>
              </div>
            </div>

            <div className="glass-panel p-5 rounded-2xl border border-white/10 space-y-2 bg-white/5">
              <h4 className="text-sm font-bold text-main-theme leading-relaxed">
                {adaptiveQuestion.questionText}
              </h4>
            </div>

            <div className="space-y-2.5">
              {adaptiveQuestion.options && adaptiveQuestion.options.map((optText: string, oIdx: number) => {
                const isSelected = adaptiveSelectedOption === oIdx;
                let optionClass = "border-white/10 bg-white/5 text-secondary-theme hover:bg-white/10 hover:border-white/20";
                if (isSelected) {
                  optionClass = "border-purple-500 bg-purple-500/20 text-white font-bold shadow-lg shadow-purple-500/20";
                }
                return (
                  <button
                    key={oIdx}
                    disabled={adaptiveSubmitting || adaptiveFeedback !== null}
                    onClick={() => setAdaptiveSelectedOption(oIdx)}
                    className={`w-full p-4 rounded-xl border text-left text-xs transition-all flex items-center justify-between cursor-pointer ${optionClass}`}
                  >
                    <div className="flex items-center gap-3">
                      <span className="h-6 w-6 rounded-full border border-white/20 flex items-center justify-center text-[10px] font-extrabold uppercase bg-black/30">
                        {String.fromCharCode(65 + oIdx)}
                      </span>
                      <span>{optText}</span>
                    </div>
                    {isSelected && <CheckCircle2 className="h-4 w-4 text-purple-400 shrink-0" />}
                  </button>
                );
              })}
            </div>

            {adaptiveFeedback && (
              <div className={`p-4 rounded-xl border text-xs space-y-2 ${adaptiveFeedback.isCorrect ? "bg-emerald-500/10 border-emerald-500/30 text-emerald-300" : "bg-pink-500/10 border-pink-500/30 text-pink-300"}`}>
                <div className="flex items-center gap-2 font-bold uppercase text-[10px]">
                  {adaptiveFeedback.isCorrect ? <CheckCircle2 className="h-4 w-4 text-emerald-400" /> : <AlertCircle className="h-4 w-4 text-pink-400" />}
                  <span>{adaptiveFeedback.isCorrect ? "Correct Answer!" : "Incorrect Answer"}</span>
                  <span className="ml-auto text-[9px] px-2 py-0.5 rounded bg-black/40 font-mono">
                    Concept Status: {adaptiveFeedback.updatedConceptStatus} ({adaptiveFeedback.updatedConceptConfidence}% Conf)
                  </span>
                </div>
                <p className="text-secondary-theme leading-relaxed">
                  {adaptiveFeedback.explanation}
                </p>
              </div>
            )}

            <div className="pt-4 border-t border-white/10 flex justify-between items-center">
              <button
                onClick={onClose}
                className="px-4 py-2 rounded-xl bg-white/5 hover:bg-white/10 text-secondary-theme text-xs font-bold transition-all cursor-pointer"
              >
                Cancel Session
              </button>

              {!adaptiveFeedback ? (
                <button
                  disabled={adaptiveSelectedOption === null || adaptiveSubmitting}
                  onClick={handleSubmitAdaptiveAnswer}
                  className="px-6 py-2.5 rounded-xl bg-purple-600 hover:bg-purple-500 disabled:opacity-50 text-white font-extrabold text-xs shadow-lg shadow-purple-600/30 flex items-center gap-2 transition-all cursor-pointer"
                >
                  {adaptiveSubmitting ? <RefreshCw className="h-4 w-4 animate-spin" /> : <Send className="h-4 w-4" />}
                  <span>Submit Answer</span>
                </button>
              ) : (
                <button
                  disabled={adaptiveSubmitting}
                  onClick={handleNextAdaptiveStep}
                  className="px-6 py-2.5 rounded-xl bg-gradient-to-r from-purple-600 to-pink-600 hover:from-purple-500 hover:to-pink-500 text-white font-extrabold text-xs shadow-lg shadow-purple-600/30 flex items-center gap-2 transition-all cursor-pointer"
                >
                  {adaptiveSubmitting ? <RefreshCw className="h-4 w-4 animate-spin" /> : <ArrowRight className="h-4 w-4" />}
                  <span>Next Adaptive Question</span>
                </button>
              )}
            </div>
          </div>
        )}

        {/* STEP 5: ADAPTIVE ASSESSMENT COMPLETION & FINAL SKILL PROFILE */}
        {step === "ADAPTIVE_RESULT" && (
          <div className="glass-panel p-6 rounded-2xl border border-emerald-500/30 bg-emerald-500/5 text-center space-y-5">
            <div className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full text-xs font-black uppercase border border-emerald-500/30 bg-emerald-500/10 text-emerald-300">
              <Award className="h-4 w-4 text-emerald-400" />
              <span>Adaptive Assessment Completed</span>
            </div>
            <h3 className="text-lg font-black text-white">Consolidated Skill Profile Verified</h3>
            <p className="text-xs text-secondary-theme leading-relaxed max-w-md mx-auto">
              Stage 2 adaptive testing is complete. Below is your final accumulated concept mastery state derived directly from authoritative ConceptMastery records.
            </p>

            {/* Consolidated Skill Profile Grid */}
            <div className="pt-2 text-left space-y-2.5 max-h-[220px] overflow-y-auto pr-1">
              <span className="text-[11px] font-extrabold uppercase text-secondary-theme block pb-1 border-b border-white/10">
                Authoritative Concept Mastery Profile
              </span>
              {finalSkillProfile && finalSkillProfile.conceptEntries && finalSkillProfile.conceptEntries.length > 0 ? (
                finalSkillProfile.conceptEntries.map((cEntry: any) => {
                  const status = cEntry.status || "UNASSESSED";
                  let badgeStyle = "bg-white/10 text-secondary-theme border-white/10";
                  if (status === "STRONG") badgeStyle = "bg-emerald-500/20 text-emerald-300 border-emerald-500/30 font-bold";
                  else if (status === "WEAK") badgeStyle = "bg-pink-500/20 text-pink-300 border-pink-500/30 font-bold";
                  else if (status === "UNCERTAIN") badgeStyle = "bg-amber-500/20 text-amber-300 border-amber-500/30 font-bold";

                  return (
                    <div
                      key={cEntry.conceptName}
                      className="p-3 rounded-xl bg-white/5 border border-white/5 flex items-center justify-between text-xs"
                    >
                      <div>
                        <span className="font-bold text-main-theme block">{cEntry.conceptName}</span>
                        <span className="text-[10px] text-secondary-theme">
                          {cEntry.accuracy}% Acc • {cEntry.confidenceScore}% Evidence ({cEntry.attemptCount} Attempts)
                        </span>
                      </div>
                      <span className={`px-2.5 py-1 rounded-full text-[10px] uppercase border ${badgeStyle}`}>
                        {status}
                      </span>
                    </div>
                  );
                })
              ) : (
                <div className="p-4 text-center text-xs text-secondary-theme">
                  Loading accumulated Concept Mastery Profile...
                </div>
              )}
            </div>

            <div className="pt-4 border-t border-white/10 flex flex-col sm:flex-row justify-center items-center gap-3">
              <button
                onClick={() => {
                  if (typeof window !== "undefined") {
                    const userId = getStudentUserId();
                    window.dispatchEvent(new CustomEvent("edupilot:assessment-completed", {
                      detail: { userId }
                    }));
                  }
                  onClose();
                }}
                className="w-full sm:w-auto px-6 py-2.5 rounded-xl bg-gradient-to-r from-emerald-600 to-teal-600 hover:from-emerald-500 hover:to-teal-500 text-white font-extrabold text-xs shadow-lg shadow-emerald-600/30 flex items-center justify-center gap-2 cursor-pointer transition-all"
              >
                <BrainCircuit className="h-4 w-4" />
                <span>View Personalized Learning Path</span>
                <ArrowRight className="h-4 w-4" />
              </button>
            </div>
          </div>
        )}

      </div>
    </div>
  );
}
