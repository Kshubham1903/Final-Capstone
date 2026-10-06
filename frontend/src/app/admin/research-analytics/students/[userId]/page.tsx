import React, { useState, useEffect } from "react";
import { useParams, useNavigate } from "react-router-dom";
import Layout from "../../../../../components/Layout";
import {
  ArrowLeft,
  RefreshCw,
  ShieldAlert,
  User,
  GraduationCap,
  BookOpen,
  Compass,
  Sparkles,
  TrendingUp,
  CheckCircle2,
  AlertTriangle,
  Clock,
  Layers,
  Award,
  Calendar,
  BarChart2,
  FileText,
  HelpCircle,
  Minus
} from "lucide-react";
import {
  fetchAdminStudentAnalytics,
  AdminStudentAnalyticsDTO
} from "../../../../../services/api";

export default function IndividualStudentAnalyticsPage() {
  const { userId } = useParams<{ userId: string }>();
  const navigate = useNavigate();

  const [analytics, setAnalytics] = useState<AdminStudentAnalyticsDTO | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const loadData = async () => {
    if (!userId) return;
    setLoading(true);
    setError(null);
    try {
      const data = await fetchAdminStudentAnalytics(userId);
      if (data) {
        setAnalytics(data);
      } else {
        setError("Unable to retrieve research analytics for this student. Verify the student ID and ensure you have ADMIN privileges.");
      }
    } catch (err) {
      setError("Network error while loading student research analytics.");
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadData();
  }, [userId]);

  return (
    <Layout>
      <div className="space-y-8 max-w-7xl mx-auto pb-12">
        
        {/* Navigation & Header */}
        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
          <button
            onClick={() => navigate("/admin/student-directory")}
            className="flex items-center gap-2 px-3.5 py-2 rounded-xl bg-white/5 hover:bg-white/10 border border-white/10 text-xs font-bold text-main-theme transition-all cursor-pointer w-fit"
          >
            <ArrowLeft className="h-4 w-4" />
            <span>Back to Student Directory</span>
          </button>

          <button
            onClick={loadData}
            disabled={loading}
            className="flex items-center gap-2 px-4 py-2 rounded-xl bg-white/5 hover:bg-white/10 border border-white/10 text-xs font-bold text-main-theme transition-all cursor-pointer w-fit"
          >
            <RefreshCw className={`h-4 w-4 text-purple-theme ${loading ? "animate-spin" : ""}`} />
            <span>{loading ? "Refreshing..." : "Refresh Student Data"}</span>
          </button>
        </div>

        {/* Error State */}
        {error && (
          <div className="p-5 rounded-2xl bg-amber-500/10 border border-amber-500/20 text-amber-theme flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4 text-sm">
            <div className="flex items-center gap-3">
              <ShieldAlert className="h-5 w-5 shrink-0" />
              <span>{error}</span>
            </div>
            <button
              onClick={loadData}
              className="px-4 py-1.5 rounded-lg bg-amber-500/20 hover:bg-amber-500/30 text-amber-300 font-bold text-xs"
            >
              Retry
            </button>
          </div>
        )}

        {/* Loading Skeleton */}
        {loading && !analytics && (
          <div className="space-y-6 animate-pulse">
            <div className="glass-panel p-6 rounded-2xl border border-white/5 h-36 bg-white/5" />
            <div className="grid grid-cols-1 md:grid-cols-4 gap-5">
              {[...Array(4)].map((_, i) => (
                <div key={i} className="glass-panel p-5 rounded-2xl border border-white/5 h-28 bg-white/5" />
              ))}
            </div>
            <div className="glass-panel p-6 rounded-2xl border border-white/5 h-72 bg-white/5" />
          </div>
        )}

        {/* Loaded Content */}
        {analytics && (
          <>
            {/* 1. STUDENT PROFILE HEADER */}
            <div className="glass-panel p-6 sm:p-8 rounded-3xl border border-white/5 relative overflow-hidden space-y-6">
              <div className="flex flex-col md:flex-row md:items-center justify-between gap-6">
                <div className="flex items-start gap-4">
                  <div className="h-14 w-14 rounded-2xl bg-purple-600/20 border border-purple-500/30 flex items-center justify-center text-purple-300 shrink-0">
                    <User className="h-7 w-7" />
                  </div>
                  <div className="space-y-1">
                    <div className="flex items-center gap-3 flex-wrap">
                      <h1 className="text-2xl font-black text-main-theme">
                        {analytics.student.fullName || "Student Profile"}
                      </h1>
                      {analytics.knowledge.hasAuthenticBaseline ? (
                        <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-[10px] font-bold bg-purple-500/10 text-purple-300 border border-purple-500/20">
                          <CheckCircle2 className="h-3 w-3 text-purple-400" />
                          Verified Baseline Diagnostic
                        </span>
                      ) : (
                        <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-[10px] font-bold bg-zinc-500/10 text-secondary-theme border border-zinc-500/20">
                          <Minus className="h-3 w-3" />
                          No Authentic Baseline
                        </span>
                      )}
                    </div>
                    <p className="text-xs text-secondary-theme font-mono">{analytics.student.email}</p>
                    <p className="text-[11px] text-purple-300/70 font-mono">User ID: {analytics.student.userId}</p>
                  </div>
                </div>

                {/* Activity Status Badge */}
                <div className="flex items-center gap-2 self-start md:self-center">
                  <span className="text-xs text-secondary-theme font-semibold">Activity Status:</span>
                  {analytics.activity.status === "ACTIVE" && (
                    <span className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full text-xs font-extrabold bg-emerald-500/10 text-emerald-400 border border-emerald-500/20">
                      <span className="w-2 h-2 rounded-full bg-emerald-400 animate-pulse" />
                      ACTIVE
                    </span>
                  )}
                  {analytics.activity.status === "AT_RISK" && (
                    <span className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full text-xs font-extrabold bg-amber-500/10 text-amber-400 border border-amber-500/20">
                      <span className="w-2 h-2 rounded-full bg-amber-400" />
                      AT RISK
                    </span>
                  )}
                  {analytics.activity.status === "INACTIVE" && (
                    <span className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full text-xs font-extrabold bg-rose-500/10 text-rose-400 border border-rose-500/20">
                      <span className="w-2 h-2 rounded-full bg-rose-400" />
                      INACTIVE
                    </span>
                  )}
                  {analytics.activity.status === "NO_ACTIVITY" && (
                    <span className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full text-xs font-extrabold bg-zinc-500/10 text-zinc-400 border border-zinc-500/20">
                      <span className="w-2 h-2 rounded-full bg-zinc-400" />
                      NO ACTIVITY
                    </span>
                  )}
                </div>
              </div>

              {/* Profile Details Grid */}
              <div className="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-6 gap-3 pt-4 border-t border-white/5 text-xs">
                <div className="p-3 rounded-xl bg-white/[0.02] border border-white/5 space-y-0.5">
                  <span className="text-[10px] text-secondary-theme uppercase font-extrabold tracking-wider">Branch</span>
                  <div className="font-bold text-main-theme">{analytics.student.branch || "N/A"}</div>
                </div>

                <div className="p-3 rounded-xl bg-white/[0.02] border border-white/5 space-y-0.5">
                  <span className="text-[10px] text-secondary-theme uppercase font-extrabold tracking-wider">Semester</span>
                  <div className="font-bold text-main-theme">{analytics.student.semester ? `Semester ${analytics.student.semester}` : "N/A"}</div>
                </div>

                <div className="p-3 rounded-xl bg-white/[0.02] border border-white/5 space-y-0.5">
                  <span className="text-[10px] text-secondary-theme uppercase font-extrabold tracking-wider">Degree</span>
                  <div className="font-bold text-main-theme">{analytics.student.degree || "N/A"}</div>
                </div>

                <div className="p-3 rounded-xl bg-white/[0.02] border border-white/5 space-y-0.5">
                  <span className="text-[10px] text-secondary-theme uppercase font-extrabold tracking-wider">Institution</span>
                  <div className="font-bold text-main-theme truncate" title={analytics.student.institution || ""}>
                    {analytics.student.institution || "N/A"}
                  </div>
                </div>

                <div className="p-3 rounded-xl bg-white/[0.02] border border-white/5 space-y-0.5">
                  <span className="text-[10px] text-secondary-theme uppercase font-extrabold tracking-wider">Career Goals</span>
                  <div className="font-bold text-main-theme truncate" title={analytics.student.careerGoals || ""}>
                    {analytics.student.careerGoals || "N/A"}
                  </div>
                </div>

                <div className="p-3 rounded-xl bg-white/[0.02] border border-white/5 space-y-0.5">
                  <span className="text-[10px] text-secondary-theme uppercase font-extrabold tracking-wider">Learning Style</span>
                  <div className="font-bold text-main-theme">{analytics.student.learningStyle || "N/A"}</div>
                </div>
              </div>
            </div>

            {/* 2. KNOWLEDGE & GROWTH DIMENSIONS */}
            <div className="space-y-3">
              <div className="flex items-center gap-2">
                <TrendingUp className="h-4 w-4 text-purple-theme" />
                <h2 className="text-xs uppercase font-extrabold tracking-wider text-secondary-theme">Knowledge & Learning Gain</h2>
              </div>

              <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-5">
                {/* Baseline Knowledge K0 */}
                <div className="glass-panel p-5 rounded-2xl border border-white/5 space-y-1">
                  <div className="flex items-center justify-between">
                    <span className="text-[10px] text-secondary-theme uppercase font-extrabold tracking-wider">Baseline Knowledge (K₀)</span>
                    <BarChart2 className="h-4 w-4 text-indigo-400" />
                  </div>
                  <div className="text-2xl font-black text-indigo-400">
                    {analytics.knowledge.baselineKnowledge != null ? `${analytics.knowledge.baselineKnowledge.toFixed(1)}%` : "N/A"}
                  </div>
                  <p className="text-[10px] text-secondary-theme">
                    {analytics.knowledge.hasAuthenticBaseline ? "Initial verified diagnostic score." : "No diagnostic baseline completed."}
                  </p>
                </div>

                {/* Current Knowledge Kt */}
                <div className="glass-panel p-5 rounded-2xl border border-white/5 space-y-1">
                  <div className="flex items-center justify-between">
                    <span className="text-[10px] text-secondary-theme uppercase font-extrabold tracking-wider">Current Knowledge (Kₜ)</span>
                    <TrendingUp className="h-4 w-4 text-emerald-400" />
                  </div>
                  <div className="text-2xl font-black text-emerald-theme">
                    {analytics.knowledge.currentKnowledge != null ? `${analytics.knowledge.currentKnowledge.toFixed(1)}%` : "N/A"}
                  </div>
                  <p className="text-[10px] text-secondary-theme">
                    Comparable baseline concept set accuracy.
                  </p>
                </div>

                {/* Growth Delta K */}
                <div className="glass-panel p-5 rounded-2xl border border-white/5 space-y-1">
                  <div className="flex items-center justify-between">
                    <span className="text-[10px] text-secondary-theme uppercase font-extrabold tracking-wider">Growth (ΔK = Kₜ − K₀)</span>
                    <Sparkles className="h-4 w-4 text-purple-400" />
                  </div>
                  <div className="text-2xl font-black">
                    {analytics.knowledge.growthPp != null ? (
                      <span className={analytics.knowledge.growthPp >= 0 ? "text-emerald-400" : "text-rose-400"}>
                        {analytics.knowledge.growthPp >= 0 ? `+${analytics.knowledge.growthPp.toFixed(1)} pp` : `${analytics.knowledge.growthPp.toFixed(1)} pp`}
                      </span>
                    ) : (
                      <span className="text-secondary-theme">N/A</span>
                    )}
                  </div>
                  <p className="text-[10px] text-secondary-theme">Percentage points gained since baseline.</p>
                </div>

                {/* Normalized Gain g */}
                <div className="glass-panel p-5 rounded-2xl border border-white/5 space-y-1">
                  <div className="flex items-center justify-between">
                    <span className="text-[10px] text-secondary-theme uppercase font-extrabold tracking-wider">Normalized Gain (g)</span>
                    <Award className="h-4 w-4 text-emerald-400" />
                  </div>
                  <div className="text-2xl font-black text-emerald-400">
                    {analytics.knowledge.normalizedLearningGain != null ? analytics.knowledge.normalizedLearningGain.toFixed(2) : "N/A"}
                  </div>
                  <p className="text-[10px] text-secondary-theme">Hake's normalized gain (0.00 – 1.00 scale).</p>
                </div>
              </div>
            </div>

            {/* 3. SUBJECT PERFORMANCE */}
            <div className="space-y-3">
              <div className="flex items-center gap-2">
                <BookOpen className="h-4 w-4 text-purple-theme" />
                <h2 className="text-xs uppercase font-extrabold tracking-wider text-secondary-theme">Subject Breakdown & Roadmap Progress</h2>
              </div>

              {analytics.subjectPerformance.length > 0 ? (
                <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-5">
                  {analytics.subjectPerformance.map((subj, idx) => (
                    <div key={idx} className="glass-panel p-5 rounded-2xl border border-white/5 space-y-4">
                      <div className="flex items-start justify-between">
                        <div>
                          <span className="text-[10px] font-mono text-purple-400 font-bold">{subj.subjectCode || "SUBJ"}</span>
                          <h4 className="text-sm font-bold text-main-theme">{subj.subjectName}</h4>
                        </div>
                        {subj.weakConcepts > 0 ? (
                          <span className="px-2 py-0.5 rounded-full text-[10px] font-bold bg-amber-500/10 text-amber-400 border border-amber-500/20">
                            {subj.weakConcepts} Weak
                          </span>
                        ) : (
                          <span className="px-2 py-0.5 rounded-full text-[10px] font-bold bg-emerald-500/10 text-emerald-400 border border-emerald-500/20">
                            All Solid
                          </span>
                        )}
                      </div>

                      <div className="grid grid-cols-3 gap-2 text-center text-xs">
                        <div className="p-2 rounded-xl bg-white/[0.02] border border-white/5">
                          <span className="text-[10px] text-secondary-theme block">Baseline</span>
                          <span className="font-mono font-bold text-main-theme">
                            {subj.baselineScore != null ? `${subj.baselineScore.toFixed(1)}%` : "N/A"}
                          </span>
                        </div>
                        <div className="p-2 rounded-xl bg-white/[0.02] border border-white/5">
                          <span className="text-[10px] text-secondary-theme block">Current</span>
                          <span className="font-mono font-bold text-emerald-400">
                            {subj.currentScore != null ? `${subj.currentScore.toFixed(1)}%` : "N/A"}
                          </span>
                        </div>
                        <div className="p-2 rounded-xl bg-white/[0.02] border border-white/5">
                          <span className="text-[10px] text-secondary-theme block">Gain (g)</span>
                          <span className="font-mono font-bold text-purple-300">
                            {subj.normalizedGain != null ? subj.normalizedGain.toFixed(2) : "N/A"}
                          </span>
                        </div>
                      </div>

                      {subj.roadmapProgressPercentage != null && (
                        <div className="space-y-1.5 pt-1">
                          <div className="flex justify-between text-[11px]">
                            <span className="text-secondary-theme">Roadmap Progress</span>
                            <span className="font-bold text-main-theme">{subj.roadmapProgressPercentage.toFixed(0)}%</span>
                          </div>
                          <div className="h-1.5 w-full bg-white/10 rounded-full overflow-hidden">
                            <div
                              className="h-full bg-purple-500 rounded-full transition-all duration-500"
                              style={{ width: `${subj.roadmapProgressPercentage}%` }}
                            />
                          </div>
                        </div>
                      )}
                    </div>
                  ))}
                </div>
              ) : (
                <div className="glass-panel p-8 rounded-2xl border border-white/5 text-center text-xs text-secondary-theme">
                  No subject performance data available for this student.
                </div>
              )}
            </div>

            {/* 5. ASSESSMENT ANALYTICS & HISTORY */}
            <div className="glass-panel p-6 rounded-3xl border border-white/5 space-y-5">
              <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 border-b border-white/5 pb-4">
                <div>
                  <h3 className="text-sm font-extrabold text-main-theme flex items-center gap-2">
                    <FileText className="h-4 w-4 text-purple-theme" />
                    <span>Assessment History</span>
                  </h3>
                  <p className="text-xs text-secondary-theme mt-0.5">Chronological diagnostic and benchmark evaluations</p>
                </div>

                <div className="flex items-center gap-3 text-xs">
                  <div className="px-3 py-1.5 rounded-xl bg-white/5 border border-white/5">
                    <span className="text-secondary-theme">Total: </span>
                    <span className="font-bold text-main-theme">{analytics.assessmentSummary.totalAssessments}</span>
                  </div>
                  <div className="px-3 py-1.5 rounded-xl bg-purple-500/10 border border-purple-500/20 text-purple-300 font-bold">
                    <span>Completed: {analytics.assessmentSummary.completedAssessments}</span>
                  </div>
                </div>
              </div>

              {analytics.assessmentHistory.length > 0 ? (
                <div className="overflow-x-auto">
                  <table className="w-full text-left text-xs">
                    <thead>
                      <tr className="border-b border-white/5 text-[10px] uppercase font-extrabold text-secondary-theme">
                        <th className="pb-3">Subject</th>
                        <th className="pb-3">Type</th>
                        <th className="pb-3">Status</th>
                        <th className="pb-3 text-right">Score</th>
                        <th className="pb-3 text-right">Percentage</th>
                        <th className="pb-3 text-right">Accuracy</th>
                        <th className="pb-3 text-center">Mastery Level</th>
                        <th className="pb-3 text-right">Date</th>
                      </tr>
                    </thead>
                    <tbody className="divide-y divide-white/5">
                      {analytics.assessmentHistory.map((ar, idx) => (
                        <tr key={ar.id || idx} className="hover:bg-white/[0.02]">
                          <td className="py-3 font-semibold text-main-theme">{ar.subjectName || ar.subjectCode || "Assessment"}</td>
                          <td className="py-3 text-secondary-theme">{ar.moduleType}</td>
                          <td className="py-3">
                            <span className="px-2 py-0.5 rounded-full text-[10px] font-bold bg-emerald-500/10 text-emerald-400 border border-emerald-500/20">
                              {ar.status}
                            </span>
                          </td>
                          <td className="py-3 text-right font-mono text-main-theme">{ar.score} / {ar.totalMarks}</td>
                          <td className="py-3 text-right font-mono font-bold text-purple-300">{ar.percentage.toFixed(1)}%</td>
                          <td className="py-3 text-right font-mono text-emerald-400">{ar.accuracy.toFixed(1)}%</td>
                          <td className="py-3 text-center">
                            <span className="px-2 py-0.5 rounded-full text-[10px] font-bold bg-white/5 border border-white/10 text-main-theme">
                              {ar.masteryLevel}
                            </span>
                          </td>
                          <td className="py-3 text-right text-secondary-theme">
                            {new Date(ar.createdAt).toLocaleDateString()}
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              ) : (
                <div className="py-8 text-center text-xs text-secondary-theme">
                  No assessment records found.
                </div>
              )}
            </div>

            {/* 6. QUIZ ANALYTICS & HISTORY */}
            <div className="glass-panel p-6 rounded-3xl border border-white/5 space-y-5">
              <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 border-b border-white/5 pb-4">
                <div>
                  <h3 className="text-sm font-extrabold text-main-theme flex items-center gap-2">
                    <HelpCircle className="h-4 w-4 text-purple-theme" />
                    <span>Quiz Analytics</span>
                  </h3>
                  <p className="text-xs text-secondary-theme mt-0.5">Practice, adaptive, and verification quiz sessions</p>
                </div>

                <div className="flex items-center gap-3 text-xs">
                  <div className="px-3 py-1.5 rounded-xl bg-white/5 border border-white/5">
                    <span className="text-secondary-theme">Quizzes: </span>
                    <span className="font-bold text-main-theme">{analytics.quizSummary.totalCompletedQuizzes}</span>
                  </div>
                  <div className="px-3 py-1.5 rounded-xl bg-white/5 border border-white/5">
                    <span className="text-secondary-theme">Accuracy: </span>
                    <span className="font-bold text-emerald-400">{analytics.quizSummary.accuracy.toFixed(1)}%</span>
                  </div>
                </div>
              </div>

              {analytics.quizHistory.length > 0 ? (
                <div className="overflow-x-auto">
                  <table className="w-full text-left text-xs">
                    <thead>
                      <tr className="border-b border-white/5 text-[10px] uppercase font-extrabold text-secondary-theme">
                        <th className="pb-3">Subject / Target Concept</th>
                        <th className="pb-3">Module Type</th>
                        <th className="pb-3">Verification</th>
                        <th className="pb-3 text-right">Questions</th>
                        <th className="pb-3 text-right">Correct</th>
                        <th className="pb-3 text-right">Accuracy</th>
                        <th className="pb-3 text-right">Date</th>
                      </tr>
                    </thead>
                    <tbody className="divide-y divide-white/5">
                      {analytics.quizHistory.map((q, idx) => (
                        <tr key={q.id || idx} className="hover:bg-white/[0.02]">
                          <td className="py-3">
                            <div className="font-semibold text-main-theme">{q.subjectName || "Subject"}</div>
                            {q.targetConcept && <div className="text-[10px] text-secondary-theme">{q.targetConcept}</div>}
                          </td>
                          <td className="py-3 text-secondary-theme">{q.moduleType}</td>
                          <td className="py-3">
                            {q.isVerificationQuiz ? (
                              <span className="px-2 py-0.5 rounded-full text-[10px] font-bold bg-cyan-500/10 text-cyan-300 border border-cyan-500/20">
                                Verification
                              </span>
                            ) : (
                              <span className="text-secondary-theme text-[11px]">—</span>
                            )}
                          </td>
                          <td className="py-3 text-right font-mono">{q.totalQuestions}</td>
                          <td className="py-3 text-right font-mono text-emerald-400">{q.correctCount}</td>
                          <td className="py-3 text-right font-mono font-bold text-main-theme">{q.accuracy.toFixed(1)}%</td>
                          <td className="py-3 text-right text-secondary-theme">
                            {q.lastAnswerTime ? new Date(q.lastAnswerTime).toLocaleDateString() : "N/A"}
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              ) : (
                <div className="py-8 text-center text-xs text-secondary-theme">
                  No quiz sessions found.
                </div>
              )}
            </div>

            {/* 7. CONCEPT MASTERY TABLE */}
            <div className="glass-panel p-6 rounded-3xl border border-white/5 space-y-4">
              <div className="border-b border-white/5 pb-3">
                <h3 className="text-sm font-extrabold text-main-theme flex items-center gap-2">
                  <Layers className="h-4 w-4 text-purple-theme" />
                  <span>Concept Mastery Records</span>
                </h3>
                <p className="text-xs text-secondary-theme mt-0.5">Observed concept accuracy and mastery levels</p>
              </div>

              {analytics.conceptMastery.length > 0 ? (
                <div className="overflow-x-auto">
                  <table className="w-full text-left text-xs">
                    <thead>
                      <tr className="border-b border-white/5 text-[10px] uppercase font-extrabold text-secondary-theme">
                        <th className="pb-3">Subject</th>
                        <th className="pb-3">Concept / Topic</th>
                        <th className="pb-3">Status</th>
                        <th className="pb-3">Mastery Level</th>
                        <th className="pb-3 text-right">Accuracy</th>
                        <th className="pb-3 text-right">Confidence</th>
                        <th className="pb-3 text-right">Attempts</th>
                        <th className="pb-3 text-right">Correct / Wrong</th>
                      </tr>
                    </thead>
                    <tbody className="divide-y divide-white/5">
                      {analytics.conceptMastery.map((cm, idx) => (
                        <tr key={cm.id || idx} className="hover:bg-white/[0.02]">
                          <td className="py-3 font-semibold text-main-theme">{cm.subjectName || "Subject"}</td>
                          <td className="py-3">
                            <div className="text-main-theme font-medium">{cm.conceptName || cm.topic}</div>
                            {cm.topic && cm.topic !== cm.conceptName && (
                              <div className="text-[10px] text-secondary-theme">{cm.topic}</div>
                            )}
                          </td>
                          <td className="py-3">
                            {cm.status === "STRONG" && (
                              <span className="px-2 py-0.5 rounded-full text-[10px] font-bold bg-emerald-500/10 text-emerald-400 border border-emerald-500/20">
                                Strong
                              </span>
                            )}
                            {cm.status === "WEAK" && (
                              <span className="px-2 py-0.5 rounded-full text-[10px] font-bold bg-rose-500/10 text-rose-400 border border-rose-500/20">
                                Weak
                              </span>
                            )}
                            {cm.status === "UNASSESSED" && (
                              <span className="px-2 py-0.5 rounded-full text-[10px] font-bold bg-white/5 text-secondary-theme border border-white/10">
                                Unassessed
                              </span>
                            )}
                          </td>
                          <td className="py-3 font-semibold text-purple-300">{cm.masteryLevel}</td>
                          <td className="py-3 text-right font-mono font-bold text-main-theme">{cm.accuracy.toFixed(1)}%</td>
                          <td className="py-3 text-right font-mono text-cyan-400">{cm.confidenceScore.toFixed(1)}%</td>
                          <td className="py-3 text-right font-mono">{cm.attemptCount}</td>
                          <td className="py-3 text-right font-mono text-secondary-theme">
                            <span className="text-emerald-400">{cm.correctCount}</span> / <span className="text-rose-400">{cm.wrongCount}</span>
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              ) : (
                <div className="py-8 text-center text-xs text-secondary-theme">
                  No concept mastery records recorded.
                </div>
              )}
            </div>
          </>
        )}

      </div>
    </Layout>
  );
}
