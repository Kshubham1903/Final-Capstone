import React, { useState, useEffect } from "react";
import { fetchPendingReassessment, startConceptVerification, submitConceptRemediation, abandonConceptRemediationSession } from "../../services/api";
import { CheckCircle, Award, ArrowRight, HelpCircle, AlertCircle, X, Loader2 } from "lucide-react";

interface PendingKnowledgeCheckCardProps {
  studentId: string;
  selectedSubject?: string;
  onCompleted?: () => void;
}

export const PendingKnowledgeCheckCard: React.FC<PendingKnowledgeCheckCardProps> = ({
  studentId,
  selectedSubject,
  onCompleted
}) => {
  const [pendingCheck, setPendingCheck] = useState<any>(null);
  const [loading, setLoading] = useState<boolean>(true);
  const [modalOpen, setModalOpen] = useState<boolean>(false);
  const [activeSession, setActiveSession] = useState<any>(null);
  const [userAnswers, setUserAnswers] = useState<{ [qId: string]: number }>({});
  const [startingTest, setStartingTest] = useState<boolean>(false);
  const [submitting, setSubmitting] = useState<boolean>(false);
  const [result, setResult] = useState<any>(null);

  const checkPending = async () => {
    if (!studentId) return;
    setLoading(true);
    try {
      const data = await fetchPendingReassessment(studentId, selectedSubject);
      if (data && data.hasPendingCheck) {
        setPendingCheck(data);
      } else {
        setPendingCheck(null);
      }
    } catch (err) {
      console.warn("Failed to check pending reassessment", err);
      setPendingCheck(null);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    checkPending();
  }, [studentId, selectedSubject]);

  const handleStartCheck = async () => {
    if (!pendingCheck) return;
    setStartingTest(true);
    try {
      const sessionData = await startConceptVerification(studentId, pendingCheck.subject, pendingCheck.concept);
      if (sessionData && sessionData.sessionId) {
        setActiveSession(sessionData);
        setUserAnswers({});
        setResult(null);
        setModalOpen(true);
      }
    } catch (err) {
      console.error("Failed to start Knowledge Check session", err);
    } finally {
      setStartingTest(false);
    }
  };

  const handleSelectOption = (qId: string, optionIdx: number) => {
    setUserAnswers(prev => ({ ...prev, [qId]: optionIdx }));
  };

  const handleSubmit = async () => {
    if (!activeSession) return;
    setSubmitting(true);
    try {
      const answerEntries = (activeSession.questions || []).map((q: any) => ({
        questionId: q.id || q.questionId,
        selectedOptionIndex: userAnswers[q.id || q.questionId] !== undefined ? userAnswers[q.id || q.questionId] : -1
      }));

      const res = await submitConceptRemediation(studentId, activeSession.sessionId, answerEntries);
      if (res) {
        setResult(res);
        setPendingCheck(null);
        window.dispatchEvent(new Event("edupilot:assessment-completed"));
        window.dispatchEvent(new Event("edupilot:growth-updated"));
        if (onCompleted) onCompleted();
      }
    } catch (err) {
      console.error("Failed to submit Knowledge Check", err);
    } finally {
      setSubmitting(false);
    }
  };

  const handleAbandon = async () => {
    if (activeSession && activeSession.sessionId) {
      await abandonConceptRemediationSession(activeSession.sessionId);
    }
    setModalOpen(false);
    setActiveSession(null);
    setUserAnswers({});
    setResult(null);
  };

  if (loading) return null;
  if (!pendingCheck && !result) return null;

  return (
    <>
      {/* Compact Dashboard Card */}
      <div className="glass-panel p-4 rounded-xl border border-indigo-500/20 bg-gradient-to-r from-indigo-500/10 via-purple-500/10 to-transparent shadow-lg transition-all hover:border-indigo-500/40">
        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
          <div className="flex items-start gap-3">
            <div className="p-2.5 rounded-lg bg-indigo-500/10 border border-indigo-500/30 text-purple-theme mt-0.5">
              <Award className="w-5 h-5" />
            </div>
            <div>
              <div className="flex items-center gap-2">
                <span className="text-xs font-semibold uppercase tracking-wider text-purple-theme bg-indigo-500/10 px-2 py-0.5 rounded-full border border-indigo-500/20">
                  {pendingCheck?.subject || result?.subject || "Knowledge Check"}
                </span>
                <span className="text-xs text-secondary-theme font-medium">5 Questions</span>
              </div>
              <h4 className="text-sm font-semibold text-main-theme mt-1">
                {result ? "Knowledge Check Completed" : "Knowledge Check Available"}
              </h4>
              <p className="text-xs text-secondary-theme mt-0.5">
                {result
                  ? result.message || `Observed knowledge change: ${result.observedGain >= 0 ? "+" : ""}${result.observedGain} pp`
                  : `5 questions • Measure your current understanding on ${pendingCheck?.concept}`}
              </p>
            </div>
          </div>

          <div className="flex items-center gap-2 self-end sm:self-center">
            {result ? (
              <span className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-lg bg-emerald-500/10 border border-emerald-500/30 text-emerald-theme text-xs font-medium">
                <CheckCircle className="w-3.5 h-3.5" />
                Completed
              </span>
            ) : (
              <button
                onClick={handleStartCheck}
                disabled={startingTest}
                className="inline-flex items-center gap-1.5 px-3.5 py-1.5 rounded-lg bg-indigo-600 hover:bg-indigo-500 text-white text-xs font-semibold transition-colors shadow-md shadow-indigo-600/20 disabled:opacity-50"
              >
                {startingTest ? (
                  <>
                    <Loader2 className="w-3.5 h-3.5 animate-spin" />
                    Starting...
                  </>
                ) : (
                  <>
                    Start Check
                    <ArrowRight className="w-3.5 h-3.5" />
                  </>
                )}
              </button>
            )}
          </div>
        </div>
      </div>

      {/* Verification Check Modal */}
      {modalOpen && activeSession && (
        <div
          onClick={handleAbandon}
          className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/40 backdrop-blur-sm overflow-y-auto"
        >
          <div
            onClick={(e) => e.stopPropagation()}
            className="glass-panel bg-[var(--glass-hover-bg)] relative w-full max-w-2xl rounded-2xl p-6 shadow-2xl space-y-6 my-8 text-main-theme"
          >
            <div className="flex items-center justify-between border-b border-[var(--glass-border)] pb-4">
              <div>
                <span className="text-xs font-semibold text-purple-theme uppercase tracking-wider">
                  {activeSession.subject} Verification Check
                </span>
                <h3 className="text-lg font-bold text-main-theme mt-0.5">
                  Concept: {activeSession.concept}
                </h3>
              </div>
              <button
                onClick={handleAbandon}
                className="p-1.5 rounded-lg text-secondary-theme hover:text-main-theme hover:bg-black/5 transition-colors"
              >
                <X className="w-5 h-5" />
              </button>
            </div>

            {!result ? (
              <>
                <p className="text-xs text-secondary-theme">
                  Answer the following 5 questions to record your observed knowledge change.
                </p>

                <div className="space-y-6 max-h-[60vh] overflow-y-auto pr-2">
                  {(activeSession.questions || []).map((q: any, idx: number) => {
                    const qId = q.id || q.questionId;
                    const selectedIdx = userAnswers[qId];
                    return (
                      <div key={qId || idx} className="p-4 rounded-xl bg-[var(--card-nested-bg)] border border-[var(--glass-border)] space-y-3 shadow-sm">
                        <div className="flex items-start gap-2">
                          <span className="flex-shrink-0 w-6 h-6 rounded-full bg-purple-500/10 border border-purple-500/30 text-purple-theme font-semibold text-xs flex items-center justify-center">
                            {idx + 1}
                          </span>
                          <h4 className="text-sm font-medium text-main-theme">
                            {q.questionText}
                          </h4>
                        </div>
                        <div className="grid grid-cols-1 gap-2 pl-8">
                          {(q.options || []).map((opt: string, optIdx: number) => {
                            const isSelected = selectedIdx === optIdx;
                            return (
                              <button
                                key={optIdx}
                                type="button"
                                onClick={() => handleSelectOption(qId, optIdx)}
                                className={`text-left p-3 rounded-lg text-xs transition-all border ${
                                  isSelected
                                    ? "bg-[var(--sidebar-active-bg)] border-2 border-[var(--accent-purple)] text-main-theme font-semibold shadow-sm"
                                    : "bg-[var(--option-bg)] border-[var(--glass-border)] text-main-theme hover:border-[var(--accent-purple)] hover:bg-[var(--option-hover-bg)]"
                                }`}
                              >
                                <span className={`font-bold mr-2 ${isSelected ? "text-purple-theme" : "text-secondary-theme"}`}>
                                  {String.fromCharCode(65 + optIdx)}.
                                </span>
                                {opt}
                              </button>
                            );
                          })}
                        </div>
                      </div>
                    );
                  })}
                </div>

                <div className="flex items-center justify-between border-t border-[var(--glass-border)] pt-4">
                  <button
                    onClick={handleAbandon}
                    className="px-4 py-2 text-xs text-secondary-theme hover:text-main-theme transition-colors font-medium"
                  >
                    Cancel / Abandon
                  </button>
                  <button
                    onClick={handleSubmit}
                    disabled={submitting || Object.keys(userAnswers).length < (activeSession.questions?.length || 5)}
                    className="inline-flex items-center gap-2 px-5 py-2.5 rounded-lg bg-indigo-600 hover:bg-indigo-500 disabled:opacity-50 text-white text-xs font-semibold transition-colors shadow-lg shadow-indigo-600/30"
                  >
                    {submitting ? (
                      <>
                        <Loader2 className="w-4 h-4 animate-spin" />
                        Submitting...
                      </>
                    ) : (
                      <>
                        Submit Check
                        <CheckCircle className="w-4 h-4" />
                      </>
                    )}
                  </button>
                </div>
              </>
            ) : (
              <div className="text-center py-6 space-y-4">
                <div className="w-12 h-12 rounded-full bg-emerald-500/10 border border-emerald-500/30 text-emerald-theme flex items-center justify-center mx-auto">
                  <CheckCircle className="w-6 h-6" />
                </div>
                <div>
                  <h3 className="text-lg font-bold text-main-theme">Knowledge Check Completed</h3>
                  <p className="text-xs text-secondary-theme mt-1">
                    {result.message || `Observed knowledge change: ${result.observedGain >= 0 ? "+" : ""}${result.observedGain} pp`}
                  </p>
                </div>
                <div className="grid grid-cols-2 gap-4 max-w-sm mx-auto p-4 rounded-xl bg-[var(--card-nested-bg)] border border-[var(--glass-border)] text-left">
                  <div>
                    <span className="text-[10px] text-secondary-theme uppercase tracking-wider block">Previous Knowledge</span>
                    <span className="text-sm font-bold text-main-theme">{result.previousKnowledge} %</span>
                  </div>
                  <div>
                    <span className="text-[10px] text-secondary-theme uppercase tracking-wider block">Current Knowledge</span>
                    <span className="text-sm font-bold text-purple-theme">{result.currentKnowledge} %</span>
                  </div>
                </div>
                <button
                  onClick={() => {
                    setModalOpen(false);
                    setActiveSession(null);
                  }}
                  className="px-6 py-2.5 rounded-lg bg-indigo-600 hover:bg-indigo-500 text-white text-xs font-semibold transition-colors shadow-md"
                >
                  Close Window
                </button>
              </div>
            )}
          </div>
        </div>
      )}
    </>
  );
};
