import React, { useState, useEffect } from "react";
import { 
  Sparkles, 
  X, 
  CheckCircle2, 
  XCircle, 
  Loader2, 
  ArrowRight, 
  Target,
  ShieldCheck
} from "lucide-react";
import { startConceptRemediation, submitConceptRemediation } from "../../services/api";

interface ConceptRemediationModalProps {
  studentId: string;
  task: any;
  onClose: () => void;
  onFinished: (remediated: boolean) => void;
}

export default function ConceptRemediationModal({
  studentId,
  task,
  onClose,
  onFinished
}: ConceptRemediationModalProps) {
  const [loading, setLoading] = useState(true);
  const [sessionData, setSessionData] = useState<any>(null);
  const [currentIndex, setCurrentIndex] = useState(0);
  const [selectedAnswers, setSelectedAnswers] = useState<Record<string, number>>({});
  const [submitting, setSubmitting] = useState(false);
  const [result, setResult] = useState<any>(null);

  const subject = task.subjectName || task.subject || "Computer Science";
  const concept = task.conceptName || task.topic || "Core Concept";

  useEffect(() => {
    let isMounted = true;
    const initTest = async () => {
      setLoading(true);
      const data = await startConceptRemediation(studentId, subject, concept);
      if (isMounted) {
        if (data && data.questions && data.questions.length > 0) {
          setSessionData(data);
        }
        setLoading(false);
      }
    };
    if (studentId && subject && concept) {
      initTest();
    }
    return () => {
      isMounted = false;
    };
  }, [studentId, subject, concept]);

  const questions = sessionData?.questions || [];
  const currentQuestion = questions[currentIndex];

  const handleSelectOption = (optIdx: number) => {
    if (!currentQuestion) return;
    setSelectedAnswers(prev => ({
      ...prev,
      [currentQuestion.questionId]: optIdx
    }));
  };

  const handleSubmit = async () => {
    if (!sessionData?.sessionId) return;
    setSubmitting(true);

    const answersPayload = questions.map((q: any) => ({
      questionId: q.questionId,
      selectedOptionIndex: selectedAnswers[q.questionId] ?? 0
    }));

    const res = await submitConceptRemediation(studentId, sessionData.sessionId, answersPayload);
    setSubmitting(false);

    if (res) {
      setResult(res);
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/80 backdrop-blur-sm animate-fade-in">
      <div className="w-full max-[#0d0e17] max-w-xl glass-panel p-6 rounded-2xl border border-purple-500/30 bg-[#0d0e17] text-main-theme shadow-2xl space-y-5 relative">
        
        {/* Header */}
        <div className="flex items-center justify-between border-b border-white/10 pb-4">
          <div className="flex items-center gap-2.5">
            <div className="h-9 w-9 rounded-xl bg-purple-500/20 text-purple-300 flex items-center justify-center border border-purple-500/30">
              <Target className="h-5 w-5" />
            </div>
            <div>
              <div className="flex items-center gap-2">
                <h3 className="text-sm font-black uppercase tracking-wider text-main-theme">
                  Concept Remediation Test
                </h3>
                <span className="text-[10px] font-bold px-2 py-0.5 rounded bg-purple-500/20 text-purple-300 border border-purple-500/30">
                  {task.subjectCode || "CS301"}
                </span>
              </div>
              <p className="text-xs text-purple-300 font-bold mt-0.5">
                Target Concept: {concept}
              </p>
            </div>
          </div>

          <button
            onClick={onClose}
            className="p-1.5 rounded-lg bg-white/5 hover:bg-white/10 text-secondary-theme hover:text-main-theme transition-colors cursor-pointer"
          >
            <X className="h-4 w-4" />
          </button>
        </div>

        {/* Body Content */}
        {loading ? (
          <div className="py-12 flex flex-col items-center justify-center space-y-3">
            <Loader2 className="h-8 w-8 text-purple-400 animate-spin" />
            <p className="text-xs font-bold text-main-theme">Generating 5 Concept-Targeted Questions...</p>
            <p className="text-[10px] text-secondary-theme">Tailoring questions specifically for {concept}</p>
          </div>
        ) : result ? (
          /* Result View */
          <div className="py-6 text-center space-y-5">
            <div className="flex justify-center">
              {result.passed ? (
                <div className="h-16 w-16 rounded-full bg-emerald-500/20 border-2 border-emerald-500 flex items-center justify-center text-emerald-400">
                  <ShieldCheck className="h-10 w-10" />
                </div>
              ) : (
                <div className="h-16 w-16 rounded-full bg-amber-500/20 border-2 border-amber-500 flex items-center justify-center text-amber-400">
                  <XCircle className="h-10 w-10" />
                </div>
              )}
            </div>

            <div className="space-y-1">
              <h4 className={`text-lg font-black ${result.passed ? "text-emerald-400" : "text-amber-400"}`}>
                {result.passed ? "Concept Successfully Remediated!" : "Remediation Needs Further Practice"}
              </h4>
              <p className="text-sm font-extrabold text-main-theme">
                Score: {result.correctCount} / {result.totalQuestions} ({Math.round(result.percentage)}%)
              </p>
              <p className="text-xs text-secondary-theme max-w-sm mx-auto pt-1 leading-relaxed">
                {result.message}
              </p>
            </div>

            <button
              onClick={() => {
                onFinished(result.remediated || result.passed);
                onClose();
              }}
              className="w-full py-2.5 rounded-xl bg-purple-600 hover:bg-purple-500 text-white font-extrabold text-xs transition-all cursor-pointer shadow-lg shadow-purple-600/30"
            >
              Return to Dashboard
            </button>
          </div>
        ) : questions.length > 0 ? (
          /* Question Flow View */
          <div className="space-y-4">
            {/* Progress Bar */}
            <div className="space-y-1">
              <div className="flex justify-between text-[10px] font-bold text-secondary-theme uppercase tracking-wider">
                <span>Question {currentIndex + 1} of {questions.length}</span>
                <span>{Math.round(((currentIndex + 1) / questions.length) * 100)}% Complete</span>
              </div>
              <div className="h-1.5 w-full bg-white/5 rounded-full overflow-hidden">
                <div 
                  className="h-full bg-gradient-to-r from-purple-500 to-emerald-400 transition-all duration-300"
                  style={{ width: `${((currentIndex + 1) / questions.length) * 100}%` }}
                />
              </div>
            </div>

            {/* Question Text */}
            <div className="p-4 rounded-xl bg-white/5 border border-white/10">
              <p className="text-sm font-bold text-main-theme leading-relaxed">
                {currentQuestion.questionText}
              </p>
            </div>

            {/* Options */}
            <div className="space-y-2">
              {currentQuestion.options.map((opt: string, optIdx: number) => {
                const isSelected = selectedAnswers[currentQuestion.questionId] === optIdx;
                return (
                  <button
                    key={optIdx}
                    onClick={() => handleSelectOption(optIdx)}
                    className={`w-full p-3 rounded-xl border text-left text-xs font-semibold transition-all flex items-center justify-between cursor-pointer ${
                      isSelected
                        ? "bg-purple-600/30 border-purple-500 text-purple-200 shadow-md"
                        : "bg-white/3 border-white/5 text-secondary-theme hover:bg-white/5 hover:text-main-theme"
                    }`}
                  >
                    <span>{opt}</span>
                    <div className={`h-4 w-4 rounded-full border flex items-center justify-center shrink-0 ${
                      isSelected ? "border-purple-400 bg-purple-500 text-white" : "border-white/20"
                    }`}>
                      {isSelected && <div className="h-1.5 w-1.5 rounded-full bg-white" />}
                    </div>
                  </button>
                );
              })}
            </div>

            {/* Navigation Controls */}
            <div className="flex items-center justify-between pt-3 border-t border-white/10">
              <button
                onClick={() => setCurrentIndex(prev => Math.max(0, prev - 1))}
                disabled={currentIndex === 0}
                className="px-3 py-1.5 rounded-lg bg-white/5 hover:bg-white/10 text-xs font-bold text-secondary-theme disabled:opacity-30 cursor-pointer"
              >
                Previous
              </button>

              {currentIndex < questions.length - 1 ? (
                <button
                  onClick={() => setCurrentIndex(prev => Math.min(questions.length - 1, prev + 1))}
                  className="px-4 py-1.5 rounded-lg bg-purple-600 hover:bg-purple-500 text-white text-xs font-extrabold flex items-center gap-1 transition-all cursor-pointer shadow-md"
                >
                  <span>Next Question</span>
                  <ArrowRight className="h-3.5 w-3.5" />
                </button>
              ) : (
                <button
                  onClick={handleSubmit}
                  disabled={submitting}
                  className="px-5 py-2 rounded-lg bg-gradient-to-r from-emerald-500 to-purple-600 hover:from-emerald-400 hover:to-purple-500 text-white text-xs font-black flex items-center gap-1.5 transition-all cursor-pointer shadow-lg shadow-emerald-500/20"
                >
                  {submitting ? (
                    <>
                      <Loader2 className="h-3.5 w-3.5 animate-spin" />
                      <span>Grading Test...</span>
                    </>
                  ) : (
                    <>
                      <CheckCircle2 className="h-4 w-4" />
                      <span>Submit Remediation</span>
                    </>
                  )}
                </button>
              )}
            </div>
          </div>
        ) : (
          <div className="py-8 text-center space-y-2">
            <XCircle className="h-8 w-8 text-pink-400 mx-auto" />
            <p className="text-xs font-bold text-main-theme">Failed to load remediation questions.</p>
            <button
              onClick={onClose}
              className="px-4 py-1.5 bg-white/10 text-xs font-bold rounded-lg cursor-pointer mt-2"
            >
              Close
            </button>
          </div>
        )}

      </div>
    </div>
  );
}
