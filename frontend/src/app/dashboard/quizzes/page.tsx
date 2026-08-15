import React, { useState, useEffect, useRef } from "react";
import Layout from "../../../components/Layout";
import { 
  GraduationCap, 
  BrainCircuit, 
  HelpCircle, 
  ArrowRight, 
  Check, 
  X, 
  AlertCircle, 
  Sparkles, 
  CheckCircle2, 
  ArrowLeft,
  Timer
} from "lucide-react";
import { StudentProfile } from "../../../services/mockData";
import { 
  fetchQuizQuestions, 
  submitQuizAnswer, 
  fetchProfile, 
  checkBackendConnection, 
  generateAiQuizQuestions, 
  fetchStudentRecommendations, 
  startConceptRemediation, 
  submitConceptRemediation,
  startDiagnosticAssessment,
  fetchNextInitialDiagnosticQuestion,
  submitInitialDiagnosticAnswer,
  startAdaptiveDiagnosticSession,
  fetchNextAdaptiveQuestion,
  submitAdaptiveQuestionAnswer,
  fetchKnowledgeProfile
} from "../../../services/api";

export default function Quizzes() {
  const [profile, setProfile] = useState<StudentProfile | null>(null);
  
  // Synchronous URL search parameter parsing as source of truth for initialization
  const urlSearch = typeof window !== "undefined" ? window.location.search : "";
  const urlParams = new URLSearchParams(urlSearch);
  const urlSubject = urlParams.get("subject") || "";
  const urlTargetConcept = urlParams.get("targetConcept") || urlParams.get("concept") || "";
  const urlIsVerification = urlParams.get("isVerification") === "true" || !!urlTargetConcept;

  // State Management
  const [activeSubject, setActiveSubject] = useState(urlSubject);
  const [quizStarted, setQuizStarted] = useState(false);
  const [currentDiff, setCurrentDiff] = useState<"EASY" | "MEDIUM" | "HARD">("EASY");
  const [questionCount, setQuestionCount] = useState(0);
  const [correctAnswers, setCorrectAnswers] = useState(0);
  
  // Active Question
  const [quizQuestions, setQuizQuestions] = useState<any[]>([]);
  const [activeQuestion, setActiveQuestion] = useState<any>(null);
  const [selectedOption, setSelectedOption] = useState<number | null>(null);
  const [isAnswered, setIsAnswered] = useState(false);
  const [secondsSpent, setSecondsSpent] = useState(0);
  const [quizFinished, setQuizFinished] = useState(false);
  const [isGeneratingAi, setIsGeneratingAi] = useState(false);
  const [generatingSubject, setGeneratingSubject] = useState<string | null>(null);
  const [aiGenerationError, setAiGenerationError] = useState<string | null>(null);
  const [recommendations, setRecommendations] = useState<any[]>([]);

  // Diagnostic log
  const [diagnosticLog, setDiagnosticLog] = useState<{ difficulty: string; correct: boolean; reason: string }[]>([]);

  // 1-by-1 Assessment Engine State
  const [assessmentStage, setAssessmentStage] = useState<"INITIAL" | "ADAPTIVE">("INITIAL");
  const [diagnosticSessionId, setDiagnosticSessionId] = useState<string | null>(null);
  const [adaptiveSessionId, setAdaptiveSessionId] = useState<string | null>(null);
  const [maxQuestions, setMaxQuestions] = useState<number>(5);
  const [submittingAnswer, setSubmittingAnswer] = useState<boolean>(false);
  const [questionFeedback, setQuestionFeedback] = useState<any | null>(null);
  const [groqError, setGroqError] = useState<string | null>(null);
  const [retryAction, setRetryAction] = useState<(() => void) | null>(null);
  const [finalSkillProfile, setFinalSkillProfile] = useState<any | null>(null);

  const [targetConcept, setTargetConcept] = useState(urlTargetConcept);
  const [isVerification, setIsVerification] = useState(urlIsVerification);

  useEffect(() => {
    async function load() {
      const activeUserId = typeof window !== "undefined" ? (localStorage.getItem("edupilot_user_id") || "") : "";
      const active = await fetchProfile(activeUserId);
      setProfile(active);
      if (activeUserId) {
        const recs = await fetchStudentRecommendations(activeUserId);
        setRecommendations(recs || []);
      }
    }
    load();
  }, []);

  const displayTargetConcept = targetConcept || urlTargetConcept;
  const isVerificationMode = isVerification || urlIsVerification || !!displayTargetConcept;

  const [verificationLoading, setVerificationLoading] = useState(urlIsVerification);
  const [verificationError, setVerificationError] = useState<string | null>(null);
  const [remediationSessionId, setRemediationSessionId] = useState<string | null>(null);
  const [remediationResult, setRemediationResult] = useState<any | null>(null);
  const [userAnswers, setUserAnswers] = useState<Array<{ questionId: string; selectedOptionIndex: number }>>([]);
  const [seenQuestionIds, setSeenQuestionIds] = useState<string[]>([]);
  const [isExhausted, setIsExhausted] = useState(false);

  const adaptiveNextRequestInFlightRef = useRef<boolean>(false);
  const adaptiveRequestSequenceRef = useRef<number>(0);
  const highestQuestionNumberSeenRef = useRef<number>(0);
  const displayedAdaptiveFingerprintsRef = useRef<Set<string>>(new Set());
  const currentAdaptiveSessionIdRef = useRef<string | null>(null);

  const getStorageKey = (subj: string) => {
    const studentId = profile ? (profile.id || "default_student") : "default_student";
    return `quiz_seen_${studentId}_${subj}`;
  };

  const getPersistedSeenIds = (subj: string): string[] => {
    if (typeof window === "undefined") return [];
    try {
      const raw = localStorage.getItem(getStorageKey(subj));
      return raw ? JSON.parse(raw) : [];
    } catch {
      return [];
    }
  };

  const savePersistedSeenId = (subj: string, questionKey: string) => {
    if (typeof window === "undefined" || !questionKey) return;
    try {
      const current = getPersistedSeenIds(subj);
      if (!current.includes(questionKey)) {
        const updated = [...current, questionKey];
        const trimmed = updated.length > 60 ? updated.slice(updated.length - 40) : updated;
        localStorage.setItem(getStorageKey(subj), JSON.stringify(trimmed));
      }
    } catch (e) {
      console.warn("Could not persist seen question ID", e);
    }
  };

  const resolveSubjectCode = (subjName: string): string => {
    if (!subjName) return "CS301";
    const name = subjName.toLowerCase();
    if (name.includes("database") || name.includes("dbms")) return "CS302";
    if (name.includes("java") || name.includes("object oriented") || name.includes("oop")) return "CS303";
    if (name.includes("network") || name.includes("cn")) return "CS304";
    if (name.includes("operating") || name.includes("os")) return "CS305";
    return "CS301";
  };

  const startAiQuiz = async (subj: string) => {
    setGeneratingSubject(subj);
    setIsGeneratingAi(true);
    setAiGenerationError(null);
    setGroqError(null);
    setRetryAction(null);

    highestQuestionNumberSeenRef.current = 0;
    displayedAdaptiveFingerprintsRef.current.clear();
    adaptiveRequestSequenceRef.current = 0;
    adaptiveNextRequestInFlightRef.current = false;
    currentAdaptiveSessionIdRef.current = null;

    try {
      setActiveSubject(subj);
      setDiagnosticSessionId(null);
      setAdaptiveSessionId(null);
      setIsVerification(false);
      setTargetConcept("");
      setQuizStarted(true);
      setQuestionCount(0);
      setCorrectAnswers(0);
      setQuizFinished(false);
      setIsExhausted(false);
      setSelectedOption(null);
      setIsAnswered(false);
      setSecondsSpent(0);
      setDiagnosticLog([]);
      setVerificationError(null);
      setVerificationLoading(false);
      setAssessmentStage("INITIAL");
      setQuestionFeedback(null);

      const userId = profile?.userId || profile?.id || (typeof window !== "undefined" ? localStorage.getItem("edupilot_user_id") : "") || "";
      const branch = profile?.branch || "Computer Science & Engineering";
      const semester = profile?.semester || 5;
      const subjectCode = resolveSubjectCode(subj);

      const startRes = await startDiagnosticAssessment({
        userId,
        branch,
        semester,
        subjectCode,
        subjectName: subj,
        questionCount: 5
      });
      if (!startRes || !startRes.sessionId) {
        setGroqError("Failed to initialize diagnostic session. Please check connection.");
        return;
      }

      setDiagnosticSessionId(startRes.sessionId);
      await loadNextInitialQuestion(startRes.sessionId);
    } catch (err: any) {
      setGroqError(err.message || "Diagnostic session setup failed.");
    } finally {
      setIsGeneratingAi(false);
      setGeneratingSubject(null);
    }
  };

  const loadNextInitialQuestion = async (sessId: string) => {
    if (!sessId) return;

    if (adaptiveNextRequestInFlightRef.current) {
      console.warn(`[AdaptiveQuiz] Next initial question request already in flight for session ${sessId}. Ignoring duplicate call.`);
      return;
    }

    adaptiveNextRequestInFlightRef.current = true;
    const requestId = ++adaptiveRequestSequenceRef.current;
    setIsGeneratingAi(true);
    setGroqError(null);

    try {
      const res = await fetchNextInitialDiagnosticQuestion({ sessionId: sessId });

      if (requestId !== adaptiveRequestSequenceRef.current) {
        console.warn(`[AdaptiveQuiz] Ignored stale initial response requestId=${requestId}`);
        return;
      }

      if (res.error || !res.question) {
        setGroqError(res.message || "Groq question generation failed.");
        setRetryAction(() => () => loadNextInitialQuestion(sessId));
        return;
      }

      const incomingQuestionNumber = res.questionNumber || (questionCount + 1);

      if (incomingQuestionNumber < highestQuestionNumberSeenRef.current) {
        console.warn(`[AdaptiveQuiz] Ignored regressive initial questionNumber=${incomingQuestionNumber} (highestSeen=${highestQuestionNumberSeenRef.current})`);
        return;
      }

      const qFp = res.question.questionFingerprint || res.question.questionId || res.question.id || res.question.questionText;
      if (qFp && displayedAdaptiveFingerprintsRef.current.has(qFp)) {
        console.warn(`[AdaptiveQuiz] Ignored duplicate initial question fingerprint="${qFp}"`);
        return;
      }

      highestQuestionNumberSeenRef.current = incomingQuestionNumber;
      if (qFp) displayedAdaptiveFingerprintsRef.current.add(qFp);

      console.log(`[AdaptiveQuiz] Accepted initial question requestId=${requestId}, questionNumber=${incomingQuestionNumber}, fingerprint=${qFp}`);

      setActiveQuestion(res.question);
      setCurrentDiff((res.question.difficulty as "EASY" | "MEDIUM" | "HARD") || "MEDIUM");
      setQuestionCount(incomingQuestionNumber - 1);
      setMaxQuestions(res.totalQuestions || 10);
      setSelectedOption(null);
      setIsAnswered(false);
      setSecondsSpent(0);
    } catch (err: any) {
      if (requestId === adaptiveRequestSequenceRef.current) {
        setGroqError(err.message || "Failed to fetch diagnostic question.");
        setRetryAction(() => () => loadNextInitialQuestion(sessId));
      }
    } finally {
      if (requestId === adaptiveRequestSequenceRef.current) {
        setIsGeneratingAi(false);
      }
      adaptiveNextRequestInFlightRef.current = false;
    }
  };

  const loadNextAdaptiveQuestion = async (adapSessId: string) => {
    if (!adapSessId) return;

    if (currentAdaptiveSessionIdRef.current !== adapSessId) {
      currentAdaptiveSessionIdRef.current = adapSessId;
      highestQuestionNumberSeenRef.current = 5;
      displayedAdaptiveFingerprintsRef.current.clear();
      adaptiveRequestSequenceRef.current = 0;
    }

    if (adaptiveNextRequestInFlightRef.current) {
      console.warn(`[AdaptiveQuiz] Next adaptive question request already in flight for session ${adapSessId}. Ignoring duplicate call.`);
      return;
    }

    adaptiveNextRequestInFlightRef.current = true;
    const requestId = ++adaptiveRequestSequenceRef.current;
    setIsGeneratingAi(true);
    setGroqError(null);

    console.log(`[AdaptiveQuiz] Dispatched requestId=${requestId}, sessionId=${adapSessId}, highestSeen=${highestQuestionNumberSeenRef.current}`);

    try {
      const res = await fetchNextAdaptiveQuestion({ adaptiveSessionId: adapSessId });

      if (requestId !== adaptiveRequestSequenceRef.current) {
        console.warn(`[AdaptiveQuiz] Ignored stale response requestId=${requestId} (latest is ${adaptiveRequestSequenceRef.current})`);
        return;
      }

      if (res.completed) {
        console.log(`[AdaptiveQuiz] Session ${adapSessId} completed on requestId=${requestId}`);
        await finishDiagnosticSession();
        return;
      }

      if (res.error || !res.question) {
        setGroqError(res.message || "Groq adaptive question generation failed.");
        setRetryAction(() => () => loadNextAdaptiveQuestion(adapSessId));
        return;
      }

      const incomingQuestionNumber = res.questionNumber || (questionCount + 1);

      if (incomingQuestionNumber < highestQuestionNumberSeenRef.current) {
        console.warn(`[AdaptiveQuiz] Ignored stale/regressive questionNumber=${incomingQuestionNumber} (highestSeen=${highestQuestionNumberSeenRef.current}) on requestId=${requestId}`);
        return;
      }

      const qFp = res.question.questionFingerprint || res.question.questionId || res.question.id || res.question.questionText;
      if (qFp && displayedAdaptiveFingerprintsRef.current.has(qFp)) {
        console.warn(`[AdaptiveQuiz] Ignored duplicate question fingerprint="${qFp}" on requestId=${requestId}`);
        return;
      }

      highestQuestionNumberSeenRef.current = incomingQuestionNumber;
      if (qFp) displayedAdaptiveFingerprintsRef.current.add(qFp);

      console.log(`[AdaptiveQuiz] Accepted question requestId=${requestId}, questionNumber=${incomingQuestionNumber}, fingerprint=${qFp}`);

      setActiveQuestion(res.question);
      setCurrentDiff((res.question.difficulty as "EASY" | "MEDIUM" | "HARD") || "MEDIUM");
      setQuestionCount(incomingQuestionNumber - 1);
      setMaxQuestions(res.totalQuestions || 10);
      setSelectedOption(null);
      setIsAnswered(false);
      setSecondsSpent(0);
    } catch (err: any) {
      if (requestId === adaptiveRequestSequenceRef.current) {
        setGroqError(err.message || "Failed to fetch adaptive question.");
        setRetryAction(() => () => loadNextAdaptiveQuestion(adapSessId));
      }
    } finally {
      if (requestId === adaptiveRequestSequenceRef.current) {
        setIsGeneratingAi(false);
      }
      adaptiveNextRequestInFlightRef.current = false;
    }
  };

  const finishDiagnosticSession = async () => {
    setQuizFinished(true);
    if (typeof window !== "undefined") {
      window.dispatchEvent(new CustomEvent("edupilot:assessment-completed"));
    }
    const userId = profile?.userId || profile?.id || (typeof window !== "undefined" ? localStorage.getItem("edupilot_user_id") : "") || "";
    if (userId) {
      const updatedKp = await fetchKnowledgeProfile(userId);
      setFinalSkillProfile(updatedKp);
      const updatedProf = await fetchProfile(userId);
      setProfile(updatedProf);
      const recs = await fetchStudentRecommendations(userId);
      setRecommendations(recs || []);
    }
  };

  // ISOLATED VERIFICATION QUIZ FLOW
  const startVerificationQuiz = async (subject: string, concept: string) => {
    const subj = subject || activeSubject || urlSubject;
    const conc = concept || targetConcept || urlTargetConcept;

    setActiveSubject(subj);
    setTargetConcept(conc);
    setIsVerification(true);
    setQuizStarted(true);
    setVerificationLoading(true);
    setVerificationError(null);

    setQuestionCount(0);
    setCorrectAnswers(0);
    setQuizFinished(false);
    setIsExhausted(false);
    setSelectedOption(null);
    setIsAnswered(false);
    setSecondsSpent(0);
    setDiagnosticLog([]);

    if (!subj || !conc) {
      setVerificationError("Missing subject or target concept in verification parameters.");
      setVerificationLoading(false);
      return;
    }

    try {
      const activeUserId = profile?.id || (typeof window !== "undefined" ? localStorage.getItem("edupilot_user_id") : "") || "";
      const res = await startConceptRemediation(activeUserId, subj, conc);
      if (!res || !res.questions || !Array.isArray(res.questions) || res.questions.length === 0) {
        setVerificationError("No verification questions available for this concept.");
        setVerificationLoading(false);
        return;
      }
      setRemediationSessionId(res.sessionId || null);
      setRemediationResult(null);
      setUserAnswers([]);
      setQuizQuestions(res.questions);
      setActiveQuestion(res.questions[0]);
      setVerificationLoading(false);
    } catch (err: any) {
      console.error("[startVerificationQuiz] Error starting remediation test:", err);
      const errMsg = err?.message || String(err);
      if (errMsg.includes("AUTH_ERROR")) {
        setVerificationError("Authentication error: Please log in again to attempt verification.");
      } else if (errMsg.includes("TIMEOUT_ERROR")) {
        setVerificationError("Timeout error: Verification quiz loading timed out.");
      } else if (errMsg.includes("SERVER_ERROR") || errMsg.includes("NETWORK_ERROR")) {
        setVerificationError("Backend / Network error: Unable to connect to verification quiz service.");
      } else {
        setVerificationError(`Unable to load Verification Quiz: ${errMsg}`);
      }
      setVerificationLoading(false);
    }
  };

  // NORMAL ADAPTIVE QUIZ FLOW (UNCHANGED)
  const startQuiz = async (subj: string) => {
    setActiveSubject(subj);
    setIsVerification(false);
    setTargetConcept("");
    setQuizStarted(true);
    setCurrentDiff("EASY");
    setQuestionCount(0);
    setCorrectAnswers(0);
    setQuizFinished(false);
    setIsExhausted(false);
    setVerificationError(null);
    setVerificationLoading(false);
    setSelectedOption(null);
    setIsAnswered(false);
    setSecondsSpent(0);
    setDiagnosticLog([]);

    const persistedSeen = getPersistedSeenIds(subj);
    const cumulativeExclusions = [...persistedSeen];
    
    // Fetch initial targeted question pool from backend
    let sessionPool: any[] = [];
    try {
      const easyBatch = await fetchQuizQuestions(subj, "EASY", cumulativeExclusions);
      for (const q of easyBatch || []) {
        const key = q?.id || q?.questionText;
        if (key && !cumulativeExclusions.includes(key)) {
          cumulativeExclusions.push(key);
          sessionPool.push(q);
        }
      }

      if (sessionPool.length < 10) {
        for (const d of ["MEDIUM", "HARD", "EASY"]) {
          if (sessionPool.length >= 10) break;
          try {
            const extraBatch = await fetchQuizQuestions(subj, d, cumulativeExclusions);
            let addedNew = false;
            for (const q of extraBatch || []) {
              if (sessionPool.length >= 10) break;
              const key = q?.id || q?.questionText;
              if (key && !cumulativeExclusions.includes(key)) {
                cumulativeExclusions.push(key);
                sessionPool.push(q);
                addedNew = true;
              }
            }
            if (!addedNew) break;
          } catch (loopErr) {
            console.warn("Extra difficulty fetch loop warning:", loopErr);
            break;
          }
        }
      }
    } catch (err: any) {
      console.error("Error fetching adaptive quiz questions:", err);
      setIsExhausted(true);
      return;
    }

    if (sessionPool.length === 0) {
      setIsExhausted(true);
      return;
    }

    const finalQuestions = sessionPool.slice(0, 10);
    setQuizQuestions(finalQuestions);
    setSeenQuestionIds(finalQuestions.map(q => q?.id || q?.questionText).filter(Boolean));
    setActiveQuestion(finalQuestions[0]);
  };

  const getRecommendationForSubject = (subj: string) => {
    if (!recommendations || recommendations.length === 0) return null;

    const rec = recommendations.find(r => 
      (r.subjectName && r.subjectName.toLowerCase() === subj.toLowerCase()) || 
      (r.subjectCode && r.subjectCode.toLowerCase() === subj.toLowerCase())
    );

    if (!rec) return null;

    const rawPriority = rec.priority || "MEDIUM";
    const priority = rawPriority.charAt(0) + rawPriority.substring(1).toLowerCase();

    return {
      topic: rec.topic || rec.conceptName || "General Concepts",
      priority: priority,
      reason: rec.reason || "Practice concepts to improve mastery."
    };
  };

  useEffect(() => {
    async function load() {
      const activeUserId = typeof window !== "undefined" ? (localStorage.getItem("edupilot_user_id") || "") : "";
      const active = await fetchProfile(activeUserId);
      setProfile(active);

      if (typeof window !== "undefined") {
        const params = new URLSearchParams(window.location.search);
        const subjParam = params.get("subject") || "";
        const targetParam = params.get("targetConcept") || params.get("concept") || "";
        const isVerifParam = params.get("isVerification") === "true" || !!targetParam;

        if (isVerifParam) {
          setIsVerification(true);
          if (targetParam) setTargetConcept(targetParam);
          if (subjParam) setActiveSubject(subjParam);
          startVerificationQuiz(subjParam, targetParam);
        } else if (subjParam) {
          startQuiz(subjParam);
        }
      }
    }
    load();
  }, []);

  // Timer loop when quiz is active
  useEffect(() => {
    let timer: any;
    if (quizStarted && !isAnswered && !quizFinished) {
      timer = setInterval(() => {
        setSecondsSpent(prev => prev + 1);
      }, 1000);
    }
    return () => clearInterval(timer);
  }, [quizStarted, isAnswered, quizFinished]);

  if (!profile) return null;

  const handleSubmitAnswer = async () => {
    if (selectedOption === null || submittingAnswer || !activeQuestion) return;
    setSubmittingAnswer(true);

    try {
      if (diagnosticSessionId && assessmentStage === "INITIAL") {
        const res = await submitInitialDiagnosticAnswer({
          sessionId: diagnosticSessionId,
          questionId: activeQuestion.questionId || activeQuestion.id,
          selectedOption,
          responseTimeSeconds: secondsSpent
        });

        const isCorrect = res.isCorrect;
        if (isCorrect) setCorrectAnswers(prev => prev + 1);

        setQuestionFeedback(res);
        setIsAnswered(true);

        setDiagnosticLog(prev => [...prev, {
          difficulty: currentDiff,
          correct: isCorrect,
          reason: res.explanation || (isCorrect ? "Correct answer!" : "Incorrect option selected.")
        }]);

        setQuestionCount(prev => prev + 1);

      } else if (adaptiveSessionId && assessmentStage === "ADAPTIVE") {
        const res = await submitAdaptiveQuestionAnswer({
          adaptiveSessionId: adaptiveSessionId,
          questionId: activeQuestion.questionId || activeQuestion.id,
          selectedOption,
          responseTimeSeconds: secondsSpent
        });

        const isCorrect = res.isCorrect;
        if (isCorrect) setCorrectAnswers(prev => prev + 1);

        setQuestionFeedback(res);
        setIsAnswered(true);
        if (res.nextDifficulty) {
          setCurrentDiff(res.nextDifficulty as "EASY" | "MEDIUM" | "HARD");
        }

        setDiagnosticLog(prev => [...prev, {
          difficulty: currentDiff,
          correct: isCorrect,
          reason: res.explanation || (isCorrect ? "Correct answer!" : "Incorrect option selected.")
        }]);

        setQuestionCount(prev => prev + 1);

      } else {
        // Fallback for isolated legacy verification quiz
        const isCorrect = selectedOption === activeQuestion.correctOptionIndex;
        if (isCorrect) setCorrectAnswers(prev => prev + 1);

        const qId = activeQuestion.questionId || activeQuestion.id || `q_${questionCount}`;
        setUserAnswers(prev => [...prev, { questionId: qId, selectedOptionIndex: selectedOption }]);

        const payload = {
          profileId: profile.id || "",
          subject: activeSubject,
          concept: activeQuestion.concept,
          difficulty: currentDiff,
          isCorrect: isCorrect,
          responseTimeSeconds: secondsSpent,
          isVerification: isVerificationMode,
          targetConcept: displayTargetConcept || undefined
        };

        const result = await submitQuizAnswer(payload);
        const nextDifficulty = result.nextDifficulty as "EASY" | "MEDIUM" | "HARD";
        const reasonText = result.reason;

        setCurrentDiff(nextDifficulty);

        setDiagnosticLog(prev => [...prev, {
          difficulty: currentDiff,
          correct: isCorrect,
          reason: reasonText
        }]);

        setQuestionCount(prev => prev + 1);
        setIsAnswered(true);
      }
    } catch (err: any) {
      console.error("Error submitting answer:", err);
    } finally {
      setSubmittingAnswer(false);
    }
  };

  const handleNextStep = async () => {
    if (diagnosticSessionId || adaptiveSessionId) {
      const isCompleted = questionFeedback && questionFeedback.completed;

      console.log("[QUIZ DEBUG]", {
        currentQuestionNumber: questionCount + 1,
        currentQuestionIndex: questionCount,
        questionId: activeQuestion?.id || activeQuestion?.questionId,
        isLastQuestion: questionCount + 1 >= maxQuestions,
        action: "handleNextStep",
        assessmentStage,
        isCompleted
      });

      if (assessmentStage === "INITIAL") {
        if (isCompleted) {
          console.log("[QUIZ DEBUG] 10-question initial assessment batch completed. Transitioning directly to results/profile.");
          await finishDiagnosticSession();
        } else {
          setQuestionFeedback(null);
          setSelectedOption(null);
          setIsAnswered(false);
          await loadNextInitialQuestion(diagnosticSessionId!);
        }
      } else if (assessmentStage === "ADAPTIVE") {
        if (isCompleted) {
          console.log("[QUIZ DEBUG] Adaptive stage completed. Transitioning to results/profile.");
          await finishDiagnosticSession();
        } else {
          setQuestionFeedback(null);
          setSelectedOption(null);
          setIsAnswered(false);
          await loadNextAdaptiveQuestion(adaptiveSessionId!);
        }
      }
      return;
    }

    // Legacy fallback next step
    const totalSet = quizQuestions.length;
    if (questionCount >= totalSet || questionCount >= 10) {
      if (isVerificationMode && remediationSessionId) {
        const activeUserId = profile?.id || (typeof window !== "undefined" ? localStorage.getItem("edupilot_user_id") : "") || "";
        const remRes = await submitConceptRemediation(activeUserId, remediationSessionId, userAnswers);
        setRemediationResult(remRes);
      }

      setQuizFinished(true);

      if (typeof window !== "undefined") {
        window.dispatchEvent(new CustomEvent("edupilot:assessment-completed"));
      }
      
      const conn = await checkBackendConnection();
      if (conn) {
        const updated = await fetchProfile(profile.id || "");
        setProfile(updated);
        const recs = await fetchStudentRecommendations(profile.id || "");
        setRecommendations(recs || []);
      } else {
        applyResultsToProfileLocal();
      }
    } else {
      const nextIndex = questionCount;
      let nextQ = quizQuestions[nextIndex];

      if (!nextQ) {
        if (isVerificationMode) {
          setQuizFinished(true);
        } else {
          setIsExhausted(true);
        }
        return;
      }

      const key = nextQ.id || nextQ.questionText;
      savePersistedSeenId(activeSubject, key);
      setActiveQuestion(nextQ);

      setSelectedOption(null);
      setIsAnswered(false);
      setSecondsSpent(0);
    }
  };

  const applyResultsToProfileLocal = () => {
    const accuracy = correctAnswers / 10;
    const masteryChange = accuracy >= 0.75 ? 8.0 : accuracy >= 0.5 ? 4.0 : -2.0;
    
    const updatedMastery = { ...profile.conceptMastery };
    const currentVal = updatedMastery[activeSubject] || 50;
    updatedMastery[activeSubject] = Math.min(Math.max(currentVal + masteryChange, 0), 100);

    const updatedProfile = {
      ...profile,
      conceptMastery: updatedMastery,
      completedQuizzesCount: profile.completedQuizzesCount + 1
    };

    // Calculate local SGI (simulate locally)
    const mockData = require("../../../services/mockData");
    updatedProfile.studentGrowthIndex = mockData.calculateLocalSgi(updatedProfile);
    
    if (accuracy >= 0.75) {
      const subjStrongs = updatedProfile.strongConcepts[activeSubject] || [];
      const newConcept = activeQuestion.concept;
      if (!subjStrongs.includes(newConcept)) {
        updatedProfile.strongConcepts[activeSubject] = [...subjStrongs, newConcept];
      }
      const subjWeaks = updatedProfile.weakConcepts[activeSubject] || [];
      updatedProfile.weakConcepts[activeSubject] = subjWeaks.filter(c => c !== newConcept);
    } else {
      const subjWeaks = updatedProfile.weakConcepts[activeSubject] || [];
      const newConcept = activeQuestion.concept;
      if (!subjWeaks.includes(newConcept)) {
        updatedProfile.weakConcepts[activeSubject] = [...subjWeaks, newConcept];
      }
    }

    mockData.saveStudentProfile(updatedProfile);
    setProfile(updatedProfile);
  };

  return (
    <Layout>
      <div className="space-y-8">
        
        {/* Header Title */}
        <div>
          <h1 className="text-3xl font-extrabold text-main-theme flex items-center gap-2">
            <GraduationCap className="h-8 w-8 text-purple-theme" />
            <span>{displayTargetConcept ? `Mastery Verification Quiz: ${displayTargetConcept}` : "Adaptive Diagnostic Hub"}</span>
          </h1>
          <p className="text-secondary-theme text-sm mt-1">
            {displayTargetConcept
              ? `Focused verification assessment on ${displayTargetConcept}. Complete this set to verify conceptual mastery and update your learning plan.`
              : "EduPilot quizzes scale question difficulty in real-time based on conceptual accuracy and speed across 10-question diagnostic sessions."}
          </p>
        </div>

        {/* NOT IN QUIZ: Select Subject Selection */}
        {!quizStarted && !isVerificationMode && (
          profile.subjects.length === 0 ? (
            <div className="glass-panel p-8 rounded-2xl border border-white/5 text-center space-y-4">
              <p className="text-secondary-theme text-sm">No enrolled subjects found. Please complete your academic profile configuration.</p>
            </div>
          ) : (
            <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
              {profile.subjects.map((subj) => {
                const rec = getRecommendationForSubject(subj);
                const isThisSubjectLoading = isGeneratingAi && generatingSubject === subj;
                
                return (
                  <div key={subj} className="glass-panel p-6 rounded-2xl border border-white/5 flex flex-col justify-between space-y-5 relative overflow-hidden">
                    {/* Dynamic Loader Overlay */}
                    {isThisSubjectLoading && (
                      <div className="absolute inset-0 bg-black/70 backdrop-blur-xs flex flex-col items-center justify-center space-y-2 z-10">
                        <div className="animate-spin rounded-full h-7 w-7 border-b-2 border-purple-500"></div>
                        <span className="text-[10px] font-bold text-purple-theme uppercase tracking-wider">Structuring Test...</span>
                      </div>
                    )}
                    
                    <div className="space-y-4">
                      {/* Header Info & Mastery */}
                      <div className="flex items-start justify-between">
                        <div className="space-y-1">
                          <h3 className="text-sm font-bold text-main-theme leading-snug">{subj}</h3>
                          <p className="text-xs text-secondary-theme">
                            Current Mastery Score: <strong className="text-purple-theme">{Math.round(profile.conceptMastery[subj] || 50)}%</strong>
                          </p>
                        </div>
                        <div className="h-8 w-8 bg-purple-500/10 rounded-lg flex items-center justify-center border border-purple-500/20 shrink-0">
                          <BrainCircuit className="h-4.5 w-4.5 text-purple-theme" />
                        </div>
                      </div>

                      {/* Dynamic Recommended Section */}
                      {rec ? (
                        <div className="bg-purple-950/20 border border-purple-500/10 rounded-xl p-3.5 space-y-2">
                          <div className="flex justify-between items-center">
                            <span className="text-[9px] font-bold text-purple-theme tracking-wide uppercase">Recommended for You</span>
                            <span className={`text-[9px] font-bold px-2 py-0.5 rounded-full ${
                              rec.priority === "High" ? "bg-red-500/10 text-red-400 border border-red-500/10" :
                              rec.priority === "Medium" ? "bg-amber-500/10 text-amber-400 border border-amber-500/10" :
                              "bg-emerald-500/10 text-emerald-400 border border-emerald-500/10"
                            }`}>
                              {rec.priority} Priority
                            </span>
                          </div>
                          <div className="space-y-0.5">
                            <h4 className="text-xs font-bold text-main-theme">{rec.topic}</h4>
                            <p className="text-[10px] text-secondary-theme leading-relaxed">{rec.reason}</p>
                          </div>
                        </div>
                      ) : (
                        <div className="bg-white/5 border border-white/5 rounded-xl p-3 text-center">
                          <span className="text-[10px] text-secondary-theme font-medium">No active recommendations available</span>
                        </div>
                      )}
                    </div>

                    {/* Navigation Actions */}
                    <div className="space-y-2 pt-2">
                      <button
                        onClick={() => startAiQuiz(subj)}
                        disabled={isGeneratingAi}
                        className="group w-full py-3 bg-gradient-to-r from-purple-600 to-indigo-600 hover:from-purple-500 hover:to-indigo-500 text-white rounded-xl text-xs font-bold transition-all duration-300 flex items-center justify-center gap-1.5 cursor-pointer disabled:opacity-50 disabled:cursor-not-allowed shadow-[0_0_15px_rgba(168,85,247,0.5)] hover:shadow-[0_0_25px_rgba(168,85,247,0.8)] active:scale-[0.98] border border-purple-500/30 hover:border-purple-400/50"
                      >
                        <Sparkles className="h-4 w-4 text-white/90 animate-pulse" />
                        <span>{isGeneratingAi ? "Generating..." : "Start Test"}</span>
                        <ArrowRight className="h-4 w-4 text-white/90 transition-transform duration-300 group-hover:translate-x-1" />
                      </button>

                      {activeSubject === subj && aiGenerationError && (
                        <p className="text-xs text-red-400 text-center mt-1">{aiGenerationError}</p>
                      )}
                    </div>
                  </div>
                );
              })}
            </div>
          )
        )}

        {/* LOADING VERIFICATION QUIZ PANEL */}
        {quizStarted && !activeQuestion && !isExhausted && !verificationError && !groqError && !quizFinished && (
          <div className="glass-panel p-12 rounded-2xl border border-white/10 text-center space-y-4 max-w-lg mx-auto">
            <div className="animate-spin h-10 w-10 border-4 border-purple-500 border-t-transparent rounded-full mx-auto" />
            <h3 className="text-lg font-bold text-main-theme">Generating Groq Diagnostic Question...</h3>
            <p className="text-xs text-secondary-theme">
              Groq AI (<span className="text-purple-400 font-mono">llama-3.3-70b-versatile</span>) is constructing a dynamic question for <strong className="text-purple-theme">{displayTargetConcept || activeSubject || "Subject"}</strong>
            </p>
          </div>
        )}

        {/* GROQ ERROR & RETRY CARD */}
        {quizStarted && groqError && (
          <div className="glass-panel p-8 rounded-2xl border border-red-500/30 bg-red-500/5 text-center space-y-5 max-w-xl mx-auto">
            <div className="h-12 w-12 rounded-full bg-red-500/10 border border-red-500/20 flex items-center justify-center mx-auto text-red-400">
              <AlertCircle className="h-6 w-6" />
            </div>
            <div className="space-y-1.5">
              <h3 className="text-lg font-extrabold text-white">Groq Diagnostic Generation Error</h3>
              <p className="text-xs text-secondary-theme leading-relaxed">
                {groqError}
              </p>
            </div>
            <div className="p-3 bg-black/30 rounded-xl border border-red-500/20 text-left space-y-1">
              <span className="text-[10px] font-bold uppercase text-red-400 block tracking-wider">Absolute No-Fallback Rule Enforcement</span>
              <p className="text-[11px] text-red-200/90 leading-normal">
                No attempt was consumed. Per product requirements, static DB templates are forbidden. Please click retry below to attempt Groq API generation again.
              </p>
            </div>
            <div className="pt-2 flex justify-center gap-3">
              <button
                onClick={() => {
                  setGroqError(null);
                  if (retryAction) {
                    retryAction();
                  } else if (diagnosticSessionId) {
                    loadNextInitialQuestion(diagnosticSessionId);
                  }
                }}
                className="px-6 py-2.5 bg-gradient-to-r from-purple-600 to-indigo-600 hover:from-purple-500 hover:to-indigo-500 text-white rounded-xl text-xs font-bold transition-all shadow-lg shadow-purple-600/30 cursor-pointer"
              >
                Retry Generation with Groq
              </button>
              <button
                onClick={() => {
                  setQuizStarted(false);
                  setGroqError(null);
                }}
                className="px-4 py-2.5 bg-white/5 hover:bg-white/10 text-secondary-theme border border-white/10 rounded-xl text-xs font-bold transition-all cursor-pointer"
              >
                Return to Hub
              </button>
            </div>
          </div>
        )}

        {/* EXHAUSTED / ERROR POOL PANEL */}
        {quizStarted && (isExhausted || !!verificationError) && (
          <div className="glass-panel p-8 rounded-2xl border border-white/10 space-y-6 text-center max-w-xl mx-auto">
            <div className="h-12 w-12 bg-amber-500/10 rounded-2xl flex items-center justify-center border border-amber-500/20 mx-auto">
              <AlertCircle className="h-6 w-6 text-amber-400" />
            </div>
            <div className="space-y-2">
              <h3 className="text-xl font-bold text-main-theme">
                {isVerificationMode ? "Unable to load Verification Quiz" : "No New Questions Available"}
              </h3>
              <p className="text-xs text-secondary-theme leading-relaxed">
                {verificationError || (isVerificationMode
                  ? `Could not load targeted verification questions for concept "${displayTargetConcept}" in subject "${activeSubject || urlSubject}". Please verify system connection.`
                  : "No new questions are available for this topic right now. You can restart the quiz or choose another topic.")}
              </p>
            </div>
            <div className="flex gap-4 justify-center pt-2">
              <button
                onClick={() => {
                  setVerificationError(null);
                  setIsExhausted(false);
                  if (isVerificationMode) {
                    startVerificationQuiz(activeSubject || urlSubject, displayTargetConcept);
                  } else {
                    startQuiz(activeSubject || urlSubject);
                  }
                }}
                className="px-5 py-2.5 bg-purple-600 hover:bg-purple-500 text-white rounded-xl text-xs font-bold transition-all cursor-pointer"
              >
                Retry Loading
              </button>
              <a
                href="/dashboard"
                className="px-5 py-2.5 bg-white/5 hover:bg-white/10 text-main-theme border border-white/10 rounded-xl text-xs font-bold transition-all cursor-pointer inline-block"
              >
                Return to Dashboard
              </a>
            </div>
          </div>
        )}

        {/* IN QUIZ PANEL */}
        {quizStarted && !quizFinished && !isExhausted && activeQuestion && (
          <div className="grid grid-cols-1 lg:grid-cols-3 gap-6 items-start">
            
            {/* Left Column: Active Question Form (2/3 width) */}
            <div className="lg:col-span-2 glass-panel p-8 rounded-2xl border border-white/10 space-y-6">
              
              {/* Question Header Status */}
              <div className="flex justify-between items-center border-b border-white/5 pb-4">
                <span className="text-[10px] font-bold text-secondary-theme uppercase tracking-widest">
                  Question {questionCount + 1} of {quizQuestions.length > 0 ? quizQuestions.length : 10}
                </span>
                
                <div className="flex items-center gap-3">
                  <span className={`text-[10px] font-bold px-2.5 py-1 rounded-full ${
                    currentDiff === "EASY" ? "bg-emerald-500/10 text-emerald-theme" :
                    currentDiff === "MEDIUM" ? "bg-cyan-500/10 text-cyan-theme" : "bg-pink-500/10 text-pink-theme"
                  }`}>
                    {currentDiff} DIFFICULTY
                  </span>
                  <span className="text-xs text-secondary-theme flex items-center gap-1.5">
                    <Timer className="h-4 w-4 text-purple-theme" />
                    <span>{secondsSpent}s</span>
                  </span>
                </div>
              </div>

              {/* Progress Bar */}
              <div className="w-full bg-white/5 rounded-full h-1.5 overflow-hidden">
                <div 
                  className="bg-gradient-to-r from-purple-500 to-pink-500 h-full transition-all duration-300"
                  style={{ width: `${((questionCount + 1) / (quizQuestions.length > 0 ? quizQuestions.length : 10)) * 100}%` }}
                />
              </div>

              {/* Question Text */}
              <div className="space-y-4">
                <span className="text-xs text-purple-theme font-bold tracking-wide block uppercase">
                  Concept: {activeQuestion.concept}
                </span>
                <p className="text-base font-medium text-main-theme leading-relaxed">
                  {activeQuestion.questionText}
                </p>
              </div>

              {/* Option Selection List */}
              <div className="space-y-3">
                {activeQuestion.options.map((option: string, idx: number) => {
                  const isSelected = selectedOption === idx;
                  const targetCorrectIdx = questionFeedback?.correctOptionIndex !== undefined && questionFeedback?.correctOptionIndex !== null
                    ? questionFeedback.correctOptionIndex
                    : activeQuestion?.correctOptionIndex;

                  const isOptionCorrect = targetCorrectIdx !== undefined && targetCorrectIdx !== null && idx === targetCorrectIdx;

                  let cardStyle = "bg-white/5 border-white/5 text-main-theme hover:bg-white/10";
                  let badgeLabel = null;

                  if (isAnswered) {
                    if (isOptionCorrect) {
                      cardStyle = "bg-emerald-500/10 border-emerald-500/50 text-emerald-400 font-bold shadow-[0_0_15px_rgba(16,185,129,0.15)]";
                      badgeLabel = (
                        <span className="flex items-center gap-1 text-[11px] font-bold text-emerald-400 bg-emerald-500/20 px-2.5 py-0.5 rounded-md border border-emerald-500/30">
                          <Check className="h-3.5 w-3.5" />
                          <span>Correct Answer</span>
                        </span>
                      );
                    } else if (isSelected) {
                      cardStyle = "bg-red-500/10 border-red-500/50 text-red-400 font-bold shadow-[0_0_15px_rgba(239,68,68,0.15)]";
                      badgeLabel = (
                        <span className="flex items-center gap-1 text-[11px] font-bold text-red-400 bg-red-500/20 px-2.5 py-0.5 rounded-md border border-red-500/30">
                          <X className="h-3.5 w-3.5" />
                          <span>Your Answer</span>
                        </span>
                      );
                    } else {
                      cardStyle = "bg-white/3 border-white/5 opacity-50 text-secondary-theme";
                    }
                  } else if (isSelected) {
                    cardStyle = "bg-purple-600/20 border-purple-500/50 text-purple-theme font-bold";
                  }

                  return (
                    <button
                      key={idx}
                      disabled={isAnswered}
                      onClick={() => setSelectedOption(idx)}
                      className={`w-full p-4 rounded-xl border text-xs font-semibold text-left transition-all flex items-center justify-between gap-3 ${cardStyle}`}
                    >
                      <span className="flex-1">{option}</span>
                      {badgeLabel}
                    </button>
                  );
                })}
              </div>

              {/* Conceptual Review Explanation */}
              {isAnswered && (
                <div className={`p-4 rounded-xl text-xs space-y-2 border ${
                  (questionFeedback?.isCorrect ?? (selectedOption === activeQuestion?.correctOptionIndex))
                    ? "bg-emerald-500/5 border-emerald-500/20"
                    : "bg-red-500/5 border-red-500/20"
                }`}>
                  <div className="flex items-center justify-between">
                    <div className="flex items-center gap-1.5 font-bold">
                      {(questionFeedback?.isCorrect ?? (selectedOption === activeQuestion?.correctOptionIndex)) ? (
                        <span className="text-emerald-400 flex items-center gap-1.5">
                          <CheckCircle2 className="h-4 w-4" />
                          <span>Correct!</span>
                        </span>
                      ) : (
                        <span className="text-red-400 flex items-center gap-1.5">
                          <AlertCircle className="h-4 w-4" />
                          <span>
                            Incorrect. Correct Answer:{" "}
                            <strong>
                              Option {String.fromCharCode(65 + (questionFeedback?.correctOptionIndex ?? activeQuestion?.correctOptionIndex ?? 0))}: {" "}
                              {activeQuestion.options[questionFeedback?.correctOptionIndex ?? activeQuestion?.correctOptionIndex ?? 0]}
                            </strong>
                          </span>
                        </span>
                      )}
                    </div>
                  </div>
                  <div className="pt-1 text-secondary-theme leading-relaxed">
                    <strong className="text-main-theme block mb-0.5">Conceptual Explanation:</strong>
                    <p>{questionFeedback?.explanation || activeQuestion?.conceptualExplanation}</p>
                  </div>
                </div>
              )}

              {/* Submission Control */}
              <div className="flex justify-end pt-4 border-t border-white/5">
                {!isAnswered ? (
                  <button
                    disabled={selectedOption === null}
                    onClick={handleSubmitAnswer}
                    className="px-6 py-3 bg-purple-600 disabled:opacity-40 hover:bg-purple-500 text-white text-xs font-bold rounded-xl transition-all shadow-md shadow-purple-500/20 cursor-pointer"
                  >
                    Submit Answer
                  </button>
                ) : (
                  <button
                    onClick={handleNextStep}
                    className="px-6 py-3 bg-gradient-to-r from-purple-600 to-pink-600 hover:from-purple-500 hover:to-pink-500 text-white text-xs font-bold rounded-xl transition-all shadow-md shadow-purple-500/20 flex items-center gap-1 cursor-pointer"
                  >
                    <span>{questionCount >= quizQuestions.length ? "Complete Profile Update" : "Advance Question"}</span>
                    <ArrowRight className="h-4 w-4" />
                  </button>
                )}
              </div>
            </div>

            {/* Right Column: AI Adaptation Decision Logs */}
            <div className="glass-panel p-6 rounded-2xl border border-white/5 space-y-4">
              <div className="flex items-center gap-2 border-b border-white/5 pb-3">
                <BrainCircuit className="h-5 w-5 text-cyan-theme" />
                <h3 className="text-xs font-bold uppercase tracking-wider text-main-theme">AI Adaptive Tracer</h3>
              </div>

              <div className="space-y-3 max-h-[500px] overflow-y-auto pr-1">
                {diagnosticLog.length === 0 ? (
                  <p className="text-[10px] text-secondary-theme text-center py-4 leading-relaxed">
                    Submit answers to monitor real-time difficulty adaptation triggers across all 10 questions.
                  </p>
                ) : (
                  diagnosticLog.map((log, index) => (
                    <div key={index} className="p-3 bg-white/5 border border-white/5 rounded-xl space-y-1.5">
                      <div className="flex justify-between text-[10px] font-bold">
                        <span className="text-secondary-theme">Q{index + 1}: {log.difficulty}</span>
                        <span className={log.correct ? "text-emerald-theme" : "text-pink-theme"}>
                          {log.correct ? "CORRECT" : "INCORRECT"}
                        </span>
                      </div>
                      <p className="text-[10px] text-secondary-theme leading-relaxed">{log.reason}</p>
                    </div>
                  ))
                )}
              </div>
            </div>

          </div>
        )}

        {/* QUIZ COMPLETION SUMMARY */}
        {quizStarted && quizFinished && (
          <div className="w-full max-w-3xl mx-auto glass-panel p-8 rounded-2xl border border-white/10 space-y-6 text-center">
            <div className={`h-16 w-16 rounded-full flex items-center justify-center mx-auto border ${
              isVerificationMode 
                ? (remediationResult?.passed ? "bg-emerald-500/10 border-emerald-500/20 text-emerald-theme" : "bg-amber-500/10 border-amber-500/20 text-amber-400")
                : "bg-emerald-500/10 border-emerald-500/20 text-emerald-theme"
            }`}>
              {isVerificationMode && !remediationResult?.passed ? (
                <AlertCircle className="h-8 w-8 text-amber-400" />
              ) : (
                <CheckCircle2 className="h-8 w-8 text-emerald-theme" />
              )}
            </div>

            <div className="space-y-2">
              <h2 className={`text-2xl font-bold tracking-wider ${
                isVerificationMode && !remediationResult?.passed ? "text-amber-400" : "text-gradient-purple"
              }`}>
                {isVerificationMode 
                  ? (remediationResult?.passed ? "Concept Successfully Remediated!" : "Remediation Test Complete") 
                  : "10-Question Diagnostic Complete"}
              </h2>
              <p className="text-xs text-secondary-theme">
                You correctly answered <strong className="text-purple-theme font-bold">{correctAnswers} out of {quizQuestions.length > 0 ? quizQuestions.length : 5} questions</strong> for:
              </p>
              <p className="text-base font-bold text-main-theme">
                {activeSubject} {displayTargetConcept ? `— ${displayTargetConcept}` : ""}
              </p>
              {isVerificationMode && remediationResult?.message && (
                <p className={`text-xs font-semibold max-w-md mx-auto pt-1 leading-relaxed ${
                  remediationResult.passed ? "text-emerald-400" : "text-amber-400"
                }`}>
                  {remediationResult.message}
                </p>
              )}
            </div>

            {/* Diagnostic Indicators */}
            <div className="grid grid-cols-2 gap-4 pt-2">
              <div className="p-4 bg-white/5 rounded-xl border border-white/5">
                <span className="text-[10px] text-secondary-theme block uppercase">Status Result</span>
                <span className={`text-lg font-bold ${isVerificationMode ? (remediationResult?.passed ? "text-emerald-400" : "text-amber-400") : "text-purple-theme"}`}>
                  {isVerificationMode ? (remediationResult?.passed ? "REMEDIATED" : "PRACTICE NEEDED") : `+${correctAnswers >= 7 ? "0.4" : "0.1"} Growth`}
                </span>
              </div>
              <div className="p-4 bg-white/5 rounded-xl border border-white/5">
                <span className="text-[10px] text-secondary-theme block uppercase">Accuracy Rate</span>
                <span className="text-lg font-bold text-cyan-theme">{((correctAnswers / (quizQuestions.length > 0 ? quizQuestions.length : 5)) * 100).toFixed(0)}%</span>
              </div>
            </div>

            {/* Full Questions Results Breakdown */}
            <div className="space-y-4 text-left pt-4 border-t border-white/10">
              <h3 className="text-sm font-bold text-main-theme uppercase tracking-wider flex items-center gap-2">
                <BrainCircuit className="h-4 w-4 text-purple-theme" />
                <span>Session Results Breakdown</span>
              </h3>

              <div className="space-y-3 max-h-[400px] overflow-y-auto pr-2">
                {diagnosticLog.map((item, idx) => (
                  <div key={idx} className="p-4 bg-white/5 border border-white/5 rounded-xl space-y-2">
                    <div className="flex justify-between items-center">
                      <span className="text-xs font-bold text-purple-theme">Question {idx + 1} of {quizQuestions.length > 0 ? quizQuestions.length : 5} ({item.difficulty})</span>
                      <span className={`text-[10px] font-bold px-2 py-0.5 rounded-full ${item.correct ? "bg-emerald-500/10 text-emerald-theme border border-emerald-500/20" : "bg-pink-500/10 text-pink-theme border border-pink-500/20"}`}>
                        {item.correct ? "CORRECT" : "INCORRECT"}
                      </span>
                    </div>
                    <p className="text-xs text-secondary-theme leading-relaxed">{item.reason}</p>
                  </div>
                ))}
              </div>
            </div>

            <a
              href="/dashboard"
              className="px-6 py-3 bg-purple-600 hover:bg-purple-500 text-white text-xs font-bold rounded-xl shadow-lg shadow-purple-500/20 inline-block cursor-pointer"
            >
              Return to Dashboard
            </a>
          </div>
        )}

      </div>
    </Layout>
  );
}