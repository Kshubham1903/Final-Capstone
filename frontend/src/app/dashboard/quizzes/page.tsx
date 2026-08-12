import React, { useState, useEffect } from "react";
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
import { fetchQuizQuestions, submitQuizAnswer, fetchProfile, checkBackendConnection, generateAiQuizQuestions } from "../../../services/api";

export default function Quizzes() {
  const [profile, setProfile] = useState<StudentProfile | null>(null);
  
  // State Management
  const [activeSubject, setActiveSubject] = useState("");
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

  // Diagnostic log
  const [diagnosticLog, setDiagnosticLog] = useState<{ difficulty: string; correct: boolean; reason: string }[]>([]);

  useEffect(() => {
    async function load() {
      const activeUserId = typeof window !== "undefined" ? (localStorage.getItem("edupilot_user_id") || "") : "";
      const active = await fetchProfile(activeUserId);
      setProfile(active);
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

  const [seenQuestionIds, setSeenQuestionIds] = useState<string[]>([]);
  const [isExhausted, setIsExhausted] = useState(false);

  if (!profile) return null;

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


  const startAiQuiz = async (subj: string) => {
    setAiGenerationError(null);
    setGeneratingSubject(subj);
    setIsGeneratingAi(true);

    try {
      const aiQuestions = await generateAiQuizQuestions(profile!.id || "", subj, 10);

      if (!aiQuestions || aiQuestions.length === 0) {
        setAiGenerationError("AI couldn't generate questions right now. Try again in a moment.");
        return;
      }

      setActiveSubject(subj);
      setQuizStarted(true);
      setCurrentDiff((aiQuestions[0].difficulty as "EASY" | "MEDIUM" | "HARD") || "MEDIUM");
      setQuestionCount(0);
      setCorrectAnswers(0);
      setQuizFinished(false);
      setIsExhausted(false);
      setSelectedOption(null);
      setIsAnswered(false);
      setSecondsSpent(0);
      setDiagnosticLog([]);

      setQuizQuestions(aiQuestions);
      setSeenQuestionIds(aiQuestions.map(q => q.id));
      setActiveQuestion(aiQuestions[0]);
    } catch (err: any) {
      setAiGenerationError(err.message || "AI quiz generation failed.");
    } finally {
      setIsGeneratingAi(false);
      setGeneratingSubject(null);
    }
  };

  const getRecommendationForSubject = (subj: string) => {
    if (!profile) return null;
    
    const mastery = profile.conceptMastery[subj] ?? 50;
    const weakList = profile.weakConcepts?.[subj] || [];
    const strongList = profile.strongConcepts?.[subj] || [];

    let recommendedTopic = "";
    if (weakList.length > 0) {
      recommendedTopic = weakList[0];
    } else {
      const lowerSubj = subj.toLowerCase();
      let candidates: string[] = [];
      if (lowerSubj.includes("data structure") || lowerSubj.includes("algorithm") || lowerSubj.includes("dsa")) {
        candidates = ["Recursion", "Trees", "Graphs", "Sorting"];
      } else if (lowerSubj.includes("database") || lowerSubj.includes("dbms")) {
        candidates = ["Normalization", "Indexing", "SQL Queries", "Transactions"];
      } else if (lowerSubj.includes("discrete") || lowerSubj.includes("math")) {
        candidates = ["Set Theory", "Graph Theory", "Combinatorics", "Propositional Logic"];
      } else if (lowerSubj.includes("blockchain")) {
        candidates = ["Consensus Mechanisms", "Smart Contracts", "Proof of Stake", "Cryptographic Linking"];
      } else if (lowerSubj.includes("cloud")) {
        candidates = ["Identity & Access Management", "Shared Responsibility Model", "Virtual Firewalls", "Zero Trust Security"];
      } else {
        candidates = ["Core Concepts", "Advanced Principles", "Optimization"];
      }
      recommendedTopic = candidates.find(c => !strongList.includes(c)) || candidates[0];
    }

    let priorityLevel: "High" | "Medium" | "Low" = "Medium";
    if (mastery < 40) {
      priorityLevel = "High";
    } else if (mastery >= 70) {
      priorityLevel = "Low";
    }

    let reason = "";
    if (weakList.includes(recommendedTopic)) {
      reason = `Your recent performance indicates this concept needs attention.`;
    } else if (mastery < 45) {
      reason = `Establishing foundational understanding of ${recommendedTopic} will significantly boost your subject grade.`;
    } else if (mastery < 75) {
      reason = `Practice this topic to build consistency and advance your conceptual mastery.`;
    } else {
      reason = `Targeting advanced concepts to secure perfect scores.`;
    }

    return {
      topic: recommendedTopic,
      priority: priorityLevel,
      reason: reason
    };
  };

  const handleSubmitAnswer = async () => {
    if (selectedOption === null) return;
    setIsAnswered(true);

    const isCorrect = selectedOption === activeQuestion.correctOptionIndex;
    if (isCorrect) setCorrectAnswers(prev => prev + 1);

    const payload = {
      profileId: profile.id || "",
      subject: activeSubject,
      concept: activeQuestion.concept,
      difficulty: currentDiff,
      isCorrect: isCorrect,
      responseTimeSeconds: secondsSpent
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
  };

  const handleNextStep = async () => {
    if (questionCount >= quizQuestions.length) {
      setQuizFinished(true);
      
      const conn = await checkBackendConnection();
      if (conn) {
        const updated = await fetchProfile(profile.id || "");
        setProfile(updated);
      } else {
        applyResultsToProfileLocal();
      }
    } else {
      const nextIndex = questionCount; // questionCount is now 1 for Q2, 2 for Q3... 9 for Q10
      let nextQ = quizQuestions[nextIndex];

      if (!nextQ) {
        // Fallback fetch if pool was shorter than 10
        const extraQuestions = await fetchQuizQuestions(activeSubject, currentDiff, seenQuestionIds);
        const unseen = (extraQuestions || []).filter((q: any) => {
          const key = q.id || q.questionText;
          return !seenQuestionIds.includes(key);
        });
        if (unseen.length > 0) {
          nextQ = unseen[0];
          const key = nextQ.id || nextQ.questionText;
          setSeenQuestionIds(prev => [...prev, key]);
          setQuizQuestions(prev => [...prev, nextQ]);
        }
      }

      if (!nextQ) {
        setIsExhausted(true);
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
            <span>Adaptive Diagnostic Hub</span>
          </h1>
          <p className="text-secondary-theme text-sm mt-1">
            EduPilot quizzes scale question difficulty in real-time based on conceptual accuracy and speed across 10-question diagnostic sessions.
          </p>
        </div>

        {/* NOT IN QUIZ: Select Subject Selection */}
        {!quizStarted && (
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

        {/* EXHAUSTED POOL PANEL */}
        {quizStarted && isExhausted && (
          <div className="glass-panel p-8 rounded-2xl border border-white/10 space-y-6 text-center max-w-xl mx-auto">
            <div className="h-12 w-12 bg-amber-500/10 rounded-2xl flex items-center justify-center border border-amber-500/20 mx-auto">
              <AlertCircle className="h-6 w-6 text-amber-400" />
            </div>
            <div className="space-y-2">
              <h3 className="text-xl font-bold text-main-theme">No New Questions Available</h3>
              <p className="text-xs text-secondary-theme leading-relaxed">
                No new questions are available for this topic right now. You can restart the quiz or choose another topic.
              </p>
            </div>
            <div className="flex gap-4 justify-center pt-2">
              <button
                onClick={() => {
                  if (typeof window !== "undefined") {
                    localStorage.removeItem(getStorageKey(activeSubject));
                  }
                  startAiQuiz(activeSubject);
                }}
                className="px-5 py-2.5 bg-purple-600 hover:bg-purple-500 text-white rounded-xl text-xs font-bold transition-all cursor-pointer"
              >
                Reset Topic History & Restart
              </button>
              <button
                onClick={() => {
                  setQuizStarted(false);
                  setIsExhausted(false);
                }}
                className="px-5 py-2.5 bg-white/5 hover:bg-white/10 text-main-theme border border-white/10 rounded-xl text-xs font-bold transition-all cursor-pointer"
              >
                Choose Another Subject
              </button>
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
                  Question {isAnswered ? questionCount : questionCount + 1} of {quizQuestions.length}
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

              {/* Progress Bar (1-10) */}
              <div className="w-full bg-white/5 rounded-full h-1.5 overflow-hidden">
                <div 
                  className="bg-gradient-to-r from-purple-500 to-pink-500 h-full transition-all duration-300"
                  style={{ width: `${((isAnswered ? questionCount : questionCount + 1) / quizQuestions.length) * 100}%` }}
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
                  const isCorrect = idx === activeQuestion.correctOptionIndex;
                  let cardStyle = "bg-white/5 border-white/5 text-main-theme hover:bg-white/10";
                  
                  if (isAnswered) {
                    if (isCorrect) {
                      cardStyle = "bg-emerald-500/10 border-emerald-500/50 text-emerald-theme font-bold";
                    } else if (isSelected) {
                      cardStyle = "bg-pink-500/10 border-pink-500/50 text-pink-theme font-bold";
                    } else {
                      cardStyle = "bg-white/3 border-white/5 opacity-55 text-secondary-theme";
                    }
                  } else if (isSelected) {
                    cardStyle = "bg-purple-600/20 border-purple-500/50 text-purple-theme font-bold";
                  }

                  return (
                    <button
                      key={idx}
                      disabled={isAnswered}
                      onClick={() => setSelectedOption(idx)}
                      className={`w-full p-4 rounded-xl border text-xs font-semibold text-left transition-all flex items-center justify-between ${cardStyle}`}
                    >
                      <span>{option}</span>
                      {isAnswered && isCorrect && <Check className="h-4 w-4 text-emerald-theme" />}
                      {isAnswered && isSelected && !isCorrect && <X className="h-4 w-4 text-pink-theme" />}
                    </button>
                  );
                })}
              </div>

              {/* Conceptual Review Explanation */}
              {isAnswered && (
                <div className="p-4 rounded-xl bg-purple-500/5 border border-purple-500/15 text-xs space-y-2">
                  <div className="flex items-center gap-1.5 text-purple-theme font-bold">
                    <AlertCircle className="h-4 w-4" />
                    <span>AI Conceptual Feedback</span>
                  </div>
                  <p className="text-secondary-theme leading-relaxed">{activeQuestion.conceptualExplanation}</p>
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

        {/* QUIZ COMPLETION SUMMARY & FULL 10-QUESTION REVIEW */}
        {quizStarted && quizFinished && (
          <div className="w-full max-w-3xl mx-auto glass-panel p-8 rounded-2xl border border-white/10 space-y-6 text-center">
            <div className="h-16 w-16 bg-emerald-500/10 rounded-full flex items-center justify-center mx-auto border border-emerald-500/20">
              <CheckCircle2 className="h-8 w-8 text-emerald-theme" />
            </div>

            <div className="space-y-2">
              <h2 className="text-2xl font-bold tracking-wider text-gradient-purple">10-Question Diagnostic Complete</h2>
              <p className="text-xs text-secondary-theme">
                You correctly answered <strong className="text-purple-theme font-bold">{correctAnswers} out of 10 questions</strong> for:
              </p>
              <p className="text-base font-bold text-main-theme">{activeSubject}</p>
            </div>

            {/* Diagnostic Indicators */}
            <div className="grid grid-cols-2 gap-4 pt-2">
              <div className="p-4 bg-white/5 rounded-xl border border-white/5">
                <span className="text-[10px] text-secondary-theme block uppercase">Diagnostics SGI</span>
                <span className="text-lg font-bold text-purple-theme">+{correctAnswers >= 7 ? "0.4" : "0.1"} Growth</span>
              </div>
              <div className="p-4 bg-white/5 rounded-xl border border-white/5">
                <span className="text-[10px] text-secondary-theme block uppercase">Accuracy Rate</span>
                <span className="text-lg font-bold text-cyan-theme">{((correctAnswers / 10) * 100).toFixed(0)}%</span>
              </div>
            </div>

            {/* Full 10-Question Results Breakdown */}
            <div className="space-y-4 text-left pt-4 border-t border-white/10">
              <h3 className="text-sm font-bold text-main-theme uppercase tracking-wider flex items-center gap-2">
                <BrainCircuit className="h-4 w-4 text-purple-theme" />
                <span>All 10 Questions Session Results</span>
              </h3>

              <div className="space-y-3 max-h-[400px] overflow-y-auto pr-2">
                {diagnosticLog.map((item, idx) => (
                  <div key={idx} className="p-4 bg-white/5 border border-white/5 rounded-xl space-y-2">
                    <div className="flex justify-between items-center">
                      <span className="text-xs font-bold text-purple-theme">Question {idx + 1} of 10 ({item.difficulty})</span>
                      <span className={`text-[10px] font-bold px-2 py-0.5 rounded-full ${item.correct ? "bg-emerald-500/10 text-emerald-theme border border-emerald-500/20" : "bg-pink-500/10 text-pink-theme border border-pink-500/20"}`}>
                        {item.correct ? "CORRECT" : "INCORRECT"}
                      </span>
                    </div>
                    <p className="text-xs text-secondary-theme leading-relaxed">{item.reason}</p>
                  </div>
                ))}
              </div>
            </div>

            <div className="p-3.5 bg-purple-500/5 border border-purple-500/15 rounded-xl text-xs text-secondary-theme leading-relaxed">
              **Knowledge Tracing Map:** The recommender engine has noted your conceptual masteries across all 10 questions and updated your dashboard recommendations list accordingly.
            </div>

            <button
              onClick={() => setQuizStarted(false)}
              className="px-6 py-3 bg-purple-600 hover:bg-purple-500 text-white text-xs font-bold rounded-xl shadow-lg shadow-purple-500/20 cursor-pointer"
            >
              Return to Subject Hub
            </button>
          </div>
        )}

      </div>
    </Layout>
  );
}
