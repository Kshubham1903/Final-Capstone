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
import { fetchQuizQuestions, submitQuizAnswer, fetchProfile, checkBackendConnection } from "../../../services/api";

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

  if (!profile) return null;

  const startQuiz = async (subj: string) => {
    setActiveSubject(subj);
    setQuizStarted(true);
    setCurrentDiff("EASY");
    setQuestionCount(0);
    setCorrectAnswers(0);
    setQuizFinished(false);
    setSelectedOption(null);
    setIsAnswered(false);
    setSecondsSpent(0);
    setDiagnosticLog([]);
    setSeenQuestionIds([]);
    
    // Fetch adaptive questions
    const questions = await fetchQuizQuestions(subj, "EASY");
    setQuizQuestions(questions);
    const firstQ = questions[0] || null;
    setActiveQuestion(firstQ);
    if (firstQ) {
      const firstKey = firstQ.id || firstQ.questionText;
      setSeenQuestionIds([firstKey]);
    }
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
    if (questionCount >= 4) {
      setQuizFinished(true);
      
      const conn = await checkBackendConnection();
      if (conn) {
        const updated = await fetchProfile(profile.id || "");
        setProfile(updated);
      } else {
        applyResultsToProfileLocal();
      }
    } else {
      const nextDiff = currentDiff;
      const nextQuestions = await fetchQuizQuestions(activeSubject, nextDiff);
      
      // Exclude questions already seen in this session
      const unseen = nextQuestions.filter((q: any) => {
        const key = q.id || q.questionText;
        return !seenQuestionIds.includes(key);
      });

      let selectedQ: any = null;
      if (unseen.length > 0) {
        selectedQ = unseen[0];
      } else {
        const fallbackUnseen = quizQuestions.filter((q: any) => {
          const key = q.id || q.questionText;
          return !seenQuestionIds.includes(key);
        });
        selectedQ = fallbackUnseen.length > 0 ? fallbackUnseen[0] : (nextQuestions[0] || activeQuestion);
      }

      if (selectedQ) {
        const key = selectedQ.id || selectedQ.questionText;
        setSeenQuestionIds(prev => [...prev, key]);
        setActiveQuestion(selectedQ);
      }

      setSelectedOption(null);
      setIsAnswered(false);
      setSecondsSpent(0);
    }
  };

  const applyResultsToProfileLocal = () => {
    const accuracy = correctAnswers / 4;
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
            EduPilot quizzes scale question difficulty in real-time based on conceptual accuracy and speed to bypass rote memorization testing.
          </p>
        </div>

        {/* NOT IN QUIZ: Select Subject Selection */}
        {!quizStarted && (
          <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
            {profile.subjects.map((subj) => (
              <div key={subj} className="glass-panel p-6 rounded-2xl border border-white/5 flex flex-col justify-between space-y-6">
                <div className="space-y-2">
                  <div className="h-10 w-10 bg-purple-500/10 rounded-xl flex items-center justify-center border border-purple-500/20">
                    <BrainCircuit className="h-5 w-5 text-purple-theme" />
                  </div>
                  <h3 className="text-base font-bold text-main-theme leading-snug">{subj}</h3>
                  <p className="text-xs text-secondary-theme">
                    Current Mastery Score: <strong className="text-purple-theme">{Math.round(profile.conceptMastery[subj] || 50)}%</strong>
                  </p>
                </div>

                <button
                  onClick={() => startQuiz(subj)}
                  className="w-full py-3 bg-white/5 hover:bg-purple-600/20 border border-white/5 hover:border-purple-500/30 rounded-xl text-xs font-bold text-main-theme hover:text-white transition-all flex items-center justify-center gap-1.5 cursor-pointer"
                >
                  <span>Launch Diagnostic Set</span>
                  <ArrowRight className="h-4 w-4" />
                </button>
              </div>
            ))}
          </div>
        )}

        {/* IN QUIZ PANEL */}
        {quizStarted && !quizFinished && activeQuestion && (
          <div className="grid grid-cols-1 lg:grid-cols-3 gap-6 items-start">
            
            {/* Left Column: Active Question Form (2/3 width) */}
            <div className="lg:col-span-2 glass-panel p-8 rounded-2xl border border-white/10 space-y-6">
              
              {/* Question Header Status */}
              <div className="flex justify-between items-center border-b border-white/5 pb-4">
                <span className="text-[10px] font-bold text-secondary-theme uppercase tracking-widest">
                  Question {questionCount + 1} of 4
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
                    <span>{questionCount >= 4 ? "Complete Profile Update" : "Advance Question"}</span>
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

              <div className="space-y-3">
                {diagnosticLog.length === 0 ? (
                  <p className="text-[10px] text-secondary-theme text-center py-4 leading-relaxed">
                    Submit answers to monitor real-time difficulty adaptation triggers.
                  </p>
                ) : (
                  diagnosticLog.map((log, index) => (
                    <div key={index} className="p-3 bg-white/5 border border-white/5 rounded-xl space-y-1.5">
                      <div className="flex justify-between text-[10px] font-bold">
                        <span className="text-secondary-theme">Step {index + 1}: {log.difficulty}</span>
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
          <div className="w-full max-w-xl mx-auto glass-panel p-8 rounded-2xl border border-white/10 text-center space-y-6">
            <div className="h-16 w-16 bg-emerald-500/10 rounded-full flex items-center justify-center mx-auto border border-emerald-500/20">
              <CheckCircle2 className="h-8 w-8 text-emerald-theme" />
            </div>

            <div className="space-y-2">
              <h2 className="text-xl font-bold tracking-wider text-gradient-purple">Diagnostic Complete</h2>
              <p className="text-xs text-secondary-theme">
                You correctly answered {correctAnswers} out of 4 questions for:
              </p>
              <p className="text-sm font-bold text-main-theme">{activeSubject}</p>
            </div>

            {/* Diagnostic Indicators */}
            <div className="grid grid-cols-2 gap-4 pt-2">
              <div className="p-4 bg-white/5 rounded-xl border border-white/5">
                <span className="text-[10px] text-secondary-theme block uppercase">Diagnostics SGI</span>
                <span className="text-lg font-bold text-purple-theme">+{correctAnswers >= 3 ? "0.4" : "0.1"} Growth</span>
              </div>
              <div className="p-4 bg-white/5 rounded-xl border border-white/5">
                <span className="text-[10px] text-secondary-theme block uppercase">Accuracy Rate</span>
                <span className="text-lg font-bold text-cyan-theme">{((correctAnswers / 4) * 100).toFixed(0)}%</span>
              </div>
            </div>

            <div className="p-3.5 bg-purple-500/5 border border-purple-500/15 rounded-xl text-xs text-secondary-theme leading-relaxed">
              **Knowledge Tracing Map:** The recommender engine has noted your conceptual masteries and updated your dashboard recommendations list accordingly.
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
