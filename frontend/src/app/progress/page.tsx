import React, { useState, useEffect } from "react";
import { Link } from "react-router-dom";
import Layout from "../../components/Layout";
import { fetchStudentGrowth, fetchStudentState, StudentGrowthData } from "../../services/api";
import StudentGrowthHeader from "../../components/progress/StudentGrowthHeader";
import GrowthSummaryCards from "../../components/progress/GrowthSummaryCards";
import LearningTrajectoryChart from "../../components/progress/LearningTrajectoryChart";
import { SubjectProgressSection } from "../../components/progress/SubjectProgressSection";
import { ConceptImprovementTable } from "../../components/progress/ConceptImprovementTable";
import { LearningJourneyTimeline } from "../../components/progress/LearningJourneyTimeline";
import { CurrentStateKPEWCard } from "../../components/progress/CurrentStateKPEWCard";
import { AdaptiveInsightsSection } from "../../components/progress/AdaptiveInsightsSection";
import { FutureMLPredictionSection } from "../../components/progress/FutureMLPredictionSection";
import { Loader2, AlertCircle, PlayCircle, RefreshCw } from "lucide-react";

export default function StudentProgressPage() {
  const [growthData, setGrowthData] = useState<StudentGrowthData | null>(null);
  const [stateData, setStateData] = useState<any | null>(null);
  const [loading, setLoading] = useState<boolean>(true);
  const [error, setError] = useState<string | null>(null);

  const loadData = async () => {
    setLoading(true);
    setError(null);
    try {
      const activeUserId = typeof window !== "undefined" ? (localStorage.getItem("edupilot_user_id") || "") : "";
      
      const [growthRes, stateRes] = await Promise.all([
        fetchStudentGrowth(activeUserId),
        fetchStudentState(activeUserId)
      ]);

      setGrowthData(growthRes);
      setStateData(stateRes);
    } catch (err: any) {
      console.error("Failed to load student progress data:", err);
      setError("Failed to fetch growth trajectory. Please check your backend connection.");
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadData();
  }, []);

  return (
    <Layout>
      <div className="space-y-8 max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-6">
        {/* Header */}
        <StudentGrowthHeader
          hasDiagnostic={growthData?.hasDiagnostic || false}
          totalAssessments={growthData?.totalAssessmentsCount || 0}
          lastUpdated={growthData?.lastAssessmentTimestamp || null}
        />

        {/* Loading State */}
        {loading && (
          <div className="flex flex-col items-center justify-center py-16 bg-white rounded-2xl border border-slate-200 shadow-sm">
            <Loader2 className="w-10 h-10 text-blue-600 animate-spin mb-3" />
            <p className="text-sm font-semibold text-slate-700">Synthesizing Authentic Student Growth Trajectory...</p>
            <p className="text-xs text-slate-400 mt-1">Comparing Baseline ($K_0$) vs Current Knowledge ($K_t$)</p>
          </div>
        )}

        {/* Error State */}
        {!loading && error && (
          <div className="bg-rose-50 border border-rose-200 rounded-2xl p-6 flex flex-col sm:flex-row items-center justify-between gap-4">
            <div className="flex items-center gap-3">
              <AlertCircle className="w-6 h-6 text-rose-600 shrink-0" />
              <div>
                <h4 className="font-bold text-rose-900 text-sm">Unable to Load Progress Data</h4>
                <p className="text-xs text-rose-700 mt-0.5">{error}</p>
              </div>
            </div>
            <button
              onClick={loadData}
              className="px-4 py-2 bg-rose-600 hover:bg-rose-700 text-white rounded-lg text-xs font-semibold flex items-center gap-1.5 transition-colors shrink-0"
            >
              <RefreshCw className="w-3.5 h-3.5" />
              Retry Connection
            </button>
          </div>
        )}

        {/* No Diagnostic / No Data State */}
        {!loading && !error && growthData && !growthData.hasDiagnostic && (
          <div className="bg-amber-50 border border-amber-200 rounded-2xl p-8 text-center max-w-2xl mx-auto my-8">
            <div className="w-12 h-12 rounded-full bg-amber-100 text-amber-700 flex items-center justify-center mx-auto mb-4">
              <PlayCircle className="w-6 h-6" />
            </div>
            <h3 className="text-lg font-bold text-slate-900">Initial Diagnostic Required</h3>
            <p className="text-xs text-slate-600 mt-2 leading-relaxed">
              Longitudinal student growth calculation ($K_0 \rightarrow K_t$) requires an immutable baseline ($K_0$) established through an initial diagnostic test. Please complete your initial diagnostic to activate growth analytics.
            </p>
            <div className="mt-6">
              <Link
                to="/dashboard/quizzes"
                className="inline-flex items-center gap-2 px-5 py-2.5 bg-blue-600 hover:bg-blue-700 text-white text-xs font-bold rounded-xl transition-all shadow-md hover:shadow-lg"
              >
                Take Initial Diagnostic Quiz
              </Link>
            </div>
          </div>
        )}

        {/* Main Content Dashboard */}
        {!loading && !error && growthData && (
          <>
            {/* Top KPI Cards */}
            <GrowthSummaryCards growthData={growthData} />

            {/* Main Trajectory Chart */}
            <LearningTrajectoryChart trajectory={growthData.trajectory} />

            {/* Subject-Level Progress */}
            <SubjectProgressSection subjects={growthData.subjectProgress} />

            {/* Concept Improvement Table */}
            <ConceptImprovementTable concepts={growthData.conceptMasteries} />

            {/* Multi-Dimensional State Vector [K, P, E, W] */}
            <CurrentStateKPEWCard stateData={stateData} />

            {/* Authentic Learning Journey Timeline */}
            <LearningJourneyTimeline timeline={growthData.timeline} />

            {/* Adaptive Insights & Next Steps */}
            <AdaptiveInsightsSection growthData={growthData} />

            {/* Reserved ML Growth Prediction Model Placeholder */}
            <FutureMLPredictionSection />
          </>
        )}
      </div>
    </Layout>
  );
}
