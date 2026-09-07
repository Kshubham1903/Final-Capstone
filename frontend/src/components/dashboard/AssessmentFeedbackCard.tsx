import React, { useState, useEffect } from "react";
import { Sparkles, Brain, CheckCircle2, RefreshCw, Star } from "lucide-react";
import { fetchStudentState, fetchStudentRecommendations, postStudentSatisfaction } from "../../services/api";

export interface AssessmentFeedbackCardProps {
  studentId: string;
  topic: string;
  score: number;
  totalQuestions: number;
  percentage?: number;
  customFeedback?: string;
  className?: string;
}

export default function AssessmentFeedbackCard({
  studentId,
  topic,
  score,
  totalQuestions,
  percentage,
  customFeedback,
  className = ""
}: AssessmentFeedbackCardProps) {
  const [masteryPct, setMasteryPct] = useState<number | null>(null);
  const [statusLabel, setStatusLabel] = useState<string>("STRONG");
  const [nextRec, setNextRec] = useState<string | null>(null);
  const [loading, setLoading] = useState<boolean>(true);
  const [selectedRating, setSelectedRating] = useState<number | null>(null);
  const [hoverRating, setHoverRating] = useState<number | null>(null);
  const [feedbackComment, setFeedbackComment] = useState<string>("");
  const [ratingSubmitted, setRatingSubmitted] = useState<boolean>(false);
  const [submittingRating, setSubmittingRating] = useState<boolean>(false);

  const handleRatingSubmit = async (ratingVal: number) => {
    setSelectedRating(ratingVal);
    const activeId = studentId || (typeof window !== "undefined" ? localStorage.getItem("edupilot_user_id") || "" : "");
    if (!activeId) return;

    setSubmittingRating(true);
    try {
      await postStudentSatisfaction(activeId, ratingVal, "LEARNING_ACTIVITY", feedbackComment || undefined);
      setRatingSubmitted(true);
    } catch (err) {
      console.warn("Failed to submit student satisfaction rating:", err);
    } finally {
      setSubmittingRating(false);
    }
  };

  const calculatedPct = percentage !== undefined && percentage !== null 
    ? Math.round(percentage) 
    : Math.round((score / (totalQuestions > 0 ? totalQuestions : 1)) * 100);

  const incorrectCount = Math.max(0, totalQuestions - score);

  useEffect(() => {
    async function loadData() {
      setLoading(true);
      const activeId = studentId || (typeof window !== "undefined" ? localStorage.getItem("edupilot_user_id") || "" : "");
      if (!activeId) {
        setLoading(false);
        return;
      }

      try {
        // 1. Fetch Student State to get exact updated concept mastery
        const stateData = await fetchStudentState(activeId);
        let foundMastery: number | null = null;

        if (stateData && stateData.knowledge) {
          const kMap = stateData.knowledge;
          for (const subjKey of Object.keys(kMap)) {
            const subjObj = kMap[subjKey];
            if (typeof subjObj === "object" && subjObj !== null) {
              for (const conceptKey of Object.keys(subjObj)) {
                if (conceptKey.toLowerCase().includes(topic.toLowerCase()) || topic.toLowerCase().includes(conceptKey.toLowerCase())) {
                  const val = subjObj[conceptKey];
                  if (typeof val === "number") {
                    foundMastery = val > 1 ? val : val * 100;
                  } else if (val && typeof val.mastery === "number") {
                    foundMastery = val.mastery > 1 ? val.mastery : val.mastery * 100;
                  }
                  break;
                }
              }
            }
          }
        }

        if (foundMastery !== null) {
          const m = Math.round(foundMastery);
          setMasteryPct(m);
          if (m >= 80) setStatusLabel("STRONG");
          else if (m >= 60) setStatusLabel("MODERATE");
          else setStatusLabel("NEED_PRACTICE");
        } else {
          setMasteryPct(calculatedPct);
          if (calculatedPct >= 80) setStatusLabel("STRONG");
          else if (calculatedPct >= 60) setStatusLabel("MODERATE");
          else setStatusLabel("NEED_PRACTICE");
        }

        // 2. Fetch updated recommendations after assessment
        const recs = await fetchStudentRecommendations(activeId);
        if (recs && recs.length > 0) {
          const topRec = recs[0];
          const recText = topRec.actionTitle 
            ? `${topRec.actionTitle}${topRec.targetConcept ? ` (${topRec.targetConcept})` : ""}`
            : topRec.rationale || `Continue with ${topRec.targetConcept || "next targeted topic"}`;
          setNextRec(recText);
        }
      } catch (err) {
        console.warn("Failed to load feedback context from StudentState:", err);
      } finally {
        setLoading(false);
      }
    }

    loadData();
  }, [studentId, topic, score, totalQuestions]);

  const feedbackMessage = customFeedback || (
    calculatedPct >= 70
      ? "Good progress. Your mastery has improved."
      : "Requires review. Practice recommended to improve concept retention."
  );

  return (
    <div className={`glass-panel p-6 rounded-2xl border border-purple-500/20 bg-gradient-to-br from-purple-900/10 via-transparent to-pink-900/10 space-y-5 text-left ${className}`}>
      
      {/* Header Badge */}
      <div className="flex items-center justify-between border-b border-white/10 pb-3">
        <div className="flex items-center gap-2">
          <Brain className="h-5 w-5 text-purple-theme" />
          <h4 className="text-xs font-black text-main-theme uppercase tracking-wider">
            Assessment Complete
          </h4>
        </div>
        <span className="text-[10px] font-extrabold px-2.5 py-1 rounded-full bg-purple-500/20 text-purple-300 border border-purple-500/30 flex items-center gap-1">
          <Sparkles className="h-3 w-3" /> State Updated
        </span>
      </div>

      {/* Primary Metrics Grid */}
      <div className="grid grid-cols-2 md:grid-cols-4 gap-3">
        
        {/* Score & Correctness */}
        <div className="p-3.5 rounded-xl bg-white/5 border border-white/5 space-y-1">
          <span className="text-[10px] text-secondary-theme font-bold uppercase tracking-wider block">Score</span>
          <div className="flex items-baseline gap-1">
            <span className="text-xl font-black text-emerald-theme">{score}</span>
            <span className="text-xs text-secondary-theme">/ {totalQuestions}</span>
          </div>
          <span className="text-[10px] text-secondary-theme block font-medium">
            {score} Correct • {incorrectCount} Incorrect
          </span>
        </div>

        {/* Assessed Topic */}
        <div className="p-3.5 rounded-xl bg-white/5 border border-white/5 space-y-1">
          <span className="text-[10px] text-secondary-theme font-bold uppercase tracking-wider block">Topic</span>
          <span className="text-sm font-extrabold text-main-theme block truncate" title={topic}>
            {topic}
          </span>
        </div>

        {/* Current Mastery */}
        <div className="p-3.5 rounded-xl bg-white/5 border border-white/5 space-y-1">
          <span className="text-[10px] text-secondary-theme font-bold uppercase tracking-wider block">Current Mastery</span>
          <div className="flex items-baseline gap-1">
            <span className="text-xl font-black text-purple-theme">
              {loading ? "..." : `${masteryPct}%`}
            </span>
          </div>
        </div>

        {/* Status */}
        <div className="p-3.5 rounded-xl bg-white/5 border border-white/5 space-y-1">
          <span className="text-[10px] text-secondary-theme font-bold uppercase tracking-wider block">Status</span>
          <span className={`text-xs font-black uppercase inline-block px-2 py-0.5 rounded ${
            statusLabel === "STRONG" ? "bg-emerald-500/20 text-emerald-400 border border-emerald-500/30" :
            statusLabel === "MODERATE" ? "bg-cyan-500/20 text-cyan-400 border border-cyan-500/30" :
            "bg-amber-500/20 text-amber-400 border border-amber-500/30"
          }`}>
            {statusLabel}
          </span>
        </div>
      </div>

      {/* Feedback Message */}
      <div className="p-4 rounded-xl bg-purple-500/10 border border-purple-500/20 space-y-1">
        <span className="text-[10px] font-bold text-purple-theme uppercase tracking-wider block flex items-center gap-1.5">
          <CheckCircle2 className="h-3.5 w-3.5" /> Feedback
        </span>
        <p className="text-xs text-main-theme font-medium leading-relaxed">
          "{feedbackMessage}"
        </p>
      </div>

      {/* Adaptation & Next Recommendation */}
      <div className="p-4 rounded-xl bg-cyan-500/10 border border-cyan-500/20 space-y-2">
        <div className="flex items-center gap-1.5 text-cyan-theme text-xs font-bold">
          <Sparkles className="h-4 w-4" />
          <span>Your learning plan has been updated based on this assessment.</span>
        </div>

        {nextRec && (
          <div className="pt-2 border-t border-cyan-500/20 flex items-start gap-2">
            <span className="text-[10px] font-extrabold uppercase px-2 py-0.5 rounded bg-cyan-500/20 text-cyan-300 border border-cyan-500/30 shrink-0">
              Next Action
            </span>
            <p className="text-xs text-main-theme font-semibold leading-snug">
              {nextRec}
            </p>
          </div>
        )}
      </div>

      {/* Interactive Student Satisfaction Rating */}
      <div className="p-4 rounded-xl bg-amber-500/10 border border-amber-500/20 space-y-2">
        <div className="flex items-center justify-between">
          <span className="text-[10px] font-bold text-amber-400 uppercase tracking-wider flex items-center gap-1.5">
            <Star className="h-3.5 w-3.5 fill-amber-400 text-amber-400" /> Student Satisfaction
          </span>
          <span className="text-[10px] text-amber-400/80 font-medium">Rate your experience</span>
        </div>

        <p className="text-xs text-main-theme font-medium">
          How helpful was this learning activity?
        </p>

        {ratingSubmitted ? (
          <div className="text-xs text-emerald-400 font-semibold flex items-center gap-1.5 pt-1">
            <CheckCircle2 className="h-4 w-4" /> Thank you! Rating of {selectedRating}/5 recorded.
          </div>
        ) : (
          <div className="space-y-2 pt-1">
            <div className="flex items-center gap-1">
              {[1, 2, 3, 4, 5].map((star) => (
                <button
                  key={star}
                  type="button"
                  disabled={submittingRating}
                  onClick={() => handleRatingSubmit(star)}
                  onMouseEnter={() => setHoverRating(star)}
                  onMouseLeave={() => setHoverRating(null)}
                  className="p-1 text-amber-400 hover:scale-110 transition-transform cursor-pointer disabled:opacity-50"
                  title={`Rate ${star} star${star > 1 ? "s" : ""}`}
                >
                  <Star
                    className={`h-5 w-5 ${
                      (hoverRating !== null ? star <= hoverRating : (selectedRating !== null && star <= selectedRating))
                        ? "fill-amber-400 text-amber-400"
                        : "text-amber-400/40"
                    }`}
                  />
                </button>
              ))}
              {hoverRating !== null && (
                <span className="text-xs font-bold text-amber-300 ml-2">
                  {hoverRating} / 5
                </span>
              )}
            </div>
          </div>
        )}
      </div>

    </div>
  );
}
