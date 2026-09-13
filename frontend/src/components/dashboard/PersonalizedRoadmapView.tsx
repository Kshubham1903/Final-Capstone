import React, { useState, useEffect } from "react";
import { 
  Sparkles, 
  CheckCircle2, 
  Lock, 
  BookOpen, 
  ArrowRight, 
  ArrowLeft, 
  RefreshCw, 
  Award, 
  BrainCircuit, 
  Compass, 
  AlertCircle,
  HelpCircle,
  Play
} from "lucide-react";
import { fetchSubjectRoadmap, generateSubjectRoadmap } from "../../services/api";
import RoadmapTopicModal from "./RoadmapTopicModal";
import AssessmentRunner from "./AssessmentRunner";

export interface PersonalizedRoadmapViewProps {
  subjectCode: string;
  subjectName: string;
  onBack?: () => void;
}

export default function PersonalizedRoadmapView({
  subjectCode,
  subjectName,
  onBack
}: PersonalizedRoadmapViewProps) {
  const [roadmap, setRoadmap] = useState<any>(null);
  const [loading, setLoading] = useState(true);
  const [generating, setGenerating] = useState(false);
  const [error, setError] = useState<string | null>(null);

  // Modal & Assessment states
  const [selectedTopic, setSelectedTopic] = useState<any>(null);
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [showAssessment, setShowAssessment] = useState(false);

  const loadRoadmap = async () => {
    setLoading(true);
    setError(null);
    try {
      const activeUserId = typeof window !== "undefined"
        ? localStorage.getItem("edupilot_user_id") || "anonymous_student"
        : "anonymous_student";

      let rm = await fetchSubjectRoadmap(subjectCode, activeUserId, subjectName);

      if (!rm || !rm.topics || rm.topics.length === 0) {
        setGenerating(true);
        rm = await generateSubjectRoadmap({
          subjectCode,
          subjectName,
          userId: activeUserId
        });
        setGenerating(false);
      }

      setRoadmap(rm);
    } catch (err: any) {
      console.error("Error loading subject roadmap:", err);
      setError("Failed to load your personalized roadmap. Please try again.");
    } finally {
      setLoading(false);
    }
  };

  const handleRegenerate = async () => {
    setGenerating(true);
    setError(null);
    try {
      const activeUserId = typeof window !== "undefined"
        ? localStorage.getItem("edupilot_user_id") || "anonymous_student"
        : "anonymous_student";

      const rm = await generateSubjectRoadmap({
        subjectCode,
        subjectName,
        userId: activeUserId
      });
      setRoadmap(rm);
    } catch (err) {
      setError("Failed to regenerate roadmap. Using previous active roadmap.");
    } finally {
      setGenerating(false);
    }
  };

  useEffect(() => {
    loadRoadmap();
  }, [subjectCode]);

  const handleTopicClick = (topic: any) => {
    setSelectedTopic(topic);
    setIsModalOpen(true);
  };

  const handleLaunchAssessmentFromModal = (sCode: string, conceptName: string) => {
    setShowAssessment(true);
  };

  const handleAssessmentClose = () => {
    setShowAssessment(false);
    loadRoadmap(); // Refresh roadmap state after test completion
  };

  const topics = roadmap?.topics || [];
  const completedCount = topics.filter((t: any) => t.isCompleted || t.status === "COMPLETED").length;
  const progressPct = topics.length > 0 ? Math.round((completedCount / topics.length) * 100) : 0;

  return (
    <div className="space-y-6">
      
      {/* Top Header Toolbar */}
      <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4">
        <div className="flex items-center gap-3">
          {onBack && (
            <button
              onClick={onBack}
              className="p-2 rounded-xl bg-white/5 hover:bg-white/10 border border-white/10 text-secondary-theme hover:text-main-theme transition-all cursor-pointer"
              title="Back to Enrolled Subjects"
            >
              <ArrowLeft className="h-4 w-4" />
            </button>
          )}
          <div>
            <div className="flex items-center gap-2">
              <span className="text-[10px] font-extrabold px-2.5 py-0.5 rounded-full bg-purple-500/20 text-purple-theme border border-purple-500/30">
                {subjectCode}
              </span>
              <span className="text-xs text-secondary-theme font-semibold">Personalized Learning Pathway</span>
            </div>
            <h1 className="text-2xl font-extrabold text-main-theme tracking-tight mt-0.5">
              {subjectName} Roadmap
            </h1>
          </div>
        </div>

        <div className="flex items-center gap-2">
          <button
            onClick={handleRegenerate}
            disabled={generating || loading}
            className="px-3.5 py-2 rounded-xl bg-purple-600/20 hover:bg-purple-600/30 border border-purple-500/30 text-purple-theme hover:text-white text-xs font-bold transition-all flex items-center gap-2 cursor-pointer disabled:opacity-50"
          >
            <RefreshCw className={`h-3.5 w-3.5 ${generating ? "animate-spin" : ""}`} />
            <span>{generating ? "Regenerating..." : "Regenerate Roadmap"}</span>
          </button>
        </div>
      </div>

      {/* Progress & Overview Card */}
      <div className="glass-panel p-6 rounded-3xl border border-white/5 bg-gradient-to-br from-purple-900/15 via-transparent to-cyan-900/15 flex flex-col md:flex-row items-center justify-between gap-6">
        <div className="space-y-2 flex-1">
          <div className="flex items-center gap-2 text-xs font-bold text-purple-theme uppercase tracking-wider">
            <Sparkles className="h-4 w-4" />
            <span>Subject Completion Velocity</span>
          </div>
          <div className="flex items-baseline gap-3">
            <h2 className="text-3xl font-black text-main-theme">{progressPct}%</h2>
            <span className="text-xs text-secondary-theme">
              {completedCount} of {topics.length} topics mastered
            </span>
          </div>
          <div className="h-2.5 w-full bg-white/10 rounded-full overflow-hidden border border-white/5">
            <div
              className="h-full rounded-full bg-gradient-to-r from-purple-500 via-cyan-400 to-emerald-400 transition-all duration-700"
              style={{ width: `${progressPct}%` }}
            />
          </div>
        </div>

        <div className="flex items-center gap-4 border-t md:border-t-0 md:border-l border-white/5 pt-4 md:pt-0 md:pl-6 text-xs text-secondary-theme">
          <div className="text-center space-y-1">
            <span className="block font-black text-lg text-emerald-400">{completedCount}</span>
            <span className="text-[10px] font-extrabold uppercase">Mastered</span>
          </div>
          <div className="h-8 w-px bg-white/5" />
          <div className="text-center space-y-1">
            <span className="block font-black text-lg text-purple-400">
              {topics.filter((t: any) => t.status === "UNLOCKED" && !t.isCompleted).length}
            </span>
            <span className="text-[10px] font-extrabold uppercase">Unlocked</span>
          </div>
          <div className="h-8 w-px bg-white/5" />
          <div className="text-center space-y-1">
            <span className="block font-black text-lg text-amber-400">
              {topics.filter((t: any) => t.status === "LOCKED").length}
            </span>
            <span className="text-[10px] font-extrabold uppercase">Locked</span>
          </div>
        </div>
      </div>

      {/* Error Alert */}
      {error && (
        <div className="p-4 rounded-xl bg-red-500/10 border border-red-500/20 text-red-400 text-xs flex items-center justify-between">
          <div className="flex items-center gap-2">
            <AlertCircle className="h-4 w-4 shrink-0" />
            <span>{error}</span>
          </div>
          <button
            onClick={loadRoadmap}
            className="px-3 py-1 rounded-lg bg-red-500/20 hover:bg-red-500/30 text-white text-xs font-bold"
          >
            Retry
          </button>
        </div>
      )}

      {/* Loading Skeleton */}
      {(loading || generating) ? (
        <div className="space-y-6 py-4">
          {[1, 2, 3, 4].map((i) => (
            <div key={i} className="glass-panel p-6 rounded-2xl animate-pulse space-y-3">
              <div className="h-4 w-32 bg-white/10 rounded" />
              <div className="h-6 w-2/3 bg-white/10 rounded" />
              <div className="h-3 w-1/2 bg-white/10 rounded" />
            </div>
          ))}
        </div>
      ) : topics.length === 0 ? (
        <div className="glass-panel p-12 rounded-3xl border border-white/5 text-center space-y-4 max-w-lg mx-auto">
          <BrainCircuit className="h-12 w-12 text-purple-theme mx-auto animate-pulse" />
          <h3 className="text-lg font-bold text-main-theme">Preparing Your Personalized Roadmap</h3>
          <p className="text-xs text-secondary-theme leading-relaxed">
            Our AI engine is generating your customized curriculum node sequence. Click below to initialize.
          </p>
          <button
            onClick={handleRegenerate}
            className="px-6 py-2.5 rounded-xl bg-purple-600 hover:bg-purple-500 text-white text-xs font-bold transition-all shadow-lg shadow-purple-600/20"
          >
            Generate Roadmap Now
          </button>
        </div>
      ) : (
        /* Vertical Roadmap Nodes Path */
        <div className="relative space-y-8 pl-4 md:pl-8 pt-2">
          
          {/* Background Connecting Line */}
          <div className="absolute left-[31px] md:left-[47px] top-6 bottom-6 w-1 bg-gradient-to-b from-emerald-500 via-purple-500 to-white/10 rounded-full -z-0 opacity-40" />

          {topics.map((node: any, idx: number) => {
            const isCompleted = node.isCompleted || node.status === "COMPLETED";
            const isUnlocked = node.status === "UNLOCKED" || isCompleted;
            const isLocked = node.status === "LOCKED" && !isCompleted;

            return (
              <div
                key={node.conceptId || idx}
                className="relative z-10 flex items-start gap-4 md:gap-6 group"
              >
                
                {/* Node Status Marker Icon */}
                <div
                  className={`h-10 w-10 md:h-12 md:w-12 rounded-2xl flex items-center justify-center font-bold text-sm shrink-0 border transition-all duration-300 shadow-xl ${
                    isCompleted
                      ? "bg-emerald-500/20 text-emerald-400 border-emerald-500/40 shadow-emerald-500/10"
                      : isUnlocked
                      ? "bg-purple-600 text-white border-purple-400 shadow-purple-600/30 animate-pulse"
                      : "bg-white/5 text-secondary-theme border-white/10"
                  }`}
                >
                  {isCompleted ? (
                    <CheckCircle2 className="h-5 w-5 md:h-6 md:w-6" />
                  ) : isUnlocked ? (
                    <Sparkles className="h-5 w-5 md:h-6 md:w-6" />
                  ) : (
                    <Lock className="h-4 w-4 md:h-5 md:w-5" />
                  )}
                </div>

                {/* Node Content Card */}
                <div
                  onClick={() => handleTopicClick(node)}
                  className={`flex-1 glass-panel-interactive p-5 md:p-6 rounded-2xl border transition-all duration-300 space-y-3 cursor-pointer ${
                    isCompleted
                      ? "border-emerald-500/20 bg-gradient-to-r from-emerald-900/10 via-transparent to-transparent"
                      : isUnlocked
                      ? "border-purple-500/40 bg-gradient-to-r from-purple-900/20 via-transparent to-pink-900/10 shadow-lg shadow-purple-500/5"
                      : "border-white/5 opacity-75"
                  }`}
                >
                  {/* Topic Metadata Header */}
                  <div className="flex items-center justify-between gap-2 flex-wrap">
                    <div className="flex items-center gap-2">
                      <span className="text-[10px] font-extrabold px-2.5 py-0.5 rounded-md bg-white/5 text-secondary-theme border border-white/10 uppercase tracking-wider">
                        Topic #{node.sequence}
                      </span>

                      {isCompleted ? (
                        <span className="text-[10px] font-extrabold px-2.5 py-0.5 rounded-md bg-emerald-500/20 text-emerald-theme border border-emerald-500/30">
                          Mastered ({node.currentAccuracy?.toFixed(0)}%)
                        </span>
                      ) : isUnlocked ? (
                        <span className="text-[10px] font-extrabold px-2.5 py-0.5 rounded-md bg-purple-500/20 text-purple-theme border border-purple-500/30 flex items-center gap-1">
                          <Play className="h-2.5 w-2.5 fill-current" /> Active Learning Node
                        </span>
                      ) : (
                        <span className="text-[10px] font-extrabold px-2.5 py-0.5 rounded-md bg-amber-500/15 text-amber-theme border border-amber-500/20">
                          Locked Topic
                        </span>
                      )}
                    </div>

                    <span className="text-[11px] font-semibold text-secondary-theme">
                      Target: {node.masteryRequiredAccuracy || 70}%
                    </span>
                  </div>

                  {/* Concept Title & Rationale */}
                  <div className="space-y-1">
                    <h3 className="text-base md:text-lg font-bold text-main-theme group-hover:text-purple-theme transition-colors leading-snug">
                      {node.conceptName}
                    </h3>
                    <p className="text-xs text-secondary-theme leading-relaxed line-clamp-2">
                      {node.aiRationale || node.description}
                    </p>
                  </div>

                  {/* Action Footer */}
                  <div className="pt-2 border-t border-white/5 flex items-center justify-between text-xs">
                    <span className={`font-bold flex items-center gap-1 ${
                      isCompleted
                        ? "text-emerald-theme"
                        : isUnlocked
                        ? "text-purple-theme"
                        : "text-secondary-theme"
                    }`}>
                      {isCompleted ? "Review Topic & Resources" : isUnlocked ? "Open Learning Node & Test" : "Prerequisites Pending"}
                    </span>

                    <div className="flex items-center gap-1 text-secondary-theme group-hover:text-main-theme transition-colors">
                      <span className="text-[10px] font-semibold uppercase tracking-wider">Details</span>
                      <ArrowRight className="h-3.5 w-3.5 group-hover:translate-x-1 transition-transform" />
                    </div>
                  </div>

                </div>

              </div>
            );
          })}
        </div>
      )}

      {/* Topic Detail Modal */}
      {selectedTopic && (
        <RoadmapTopicModal
          isOpen={isModalOpen}
          onClose={() => setIsModalOpen(false)}
          topic={selectedTopic}
          subjectCode={subjectCode}
          subjectName={subjectName}
          onLaunchAssessment={handleLaunchAssessmentFromModal}
        />
      )}

      {/* Existing Assessment Runner Integration */}
      {showAssessment && (
        <AssessmentRunner
          onClose={handleAssessmentClose}
          initialSubjectCode={subjectCode}
        />
      )}

    </div>
  );
}
