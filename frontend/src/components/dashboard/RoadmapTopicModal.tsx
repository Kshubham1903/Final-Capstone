import React, { useState, useEffect } from "react";
import { 
  X, 
  Sparkles, 
  BookOpen, 
  CheckCircle2, 
  Lock, 
  Award, 
  ExternalLink, 
  Video, 
  FileText, 
  Code, 
  HelpCircle, 
  BrainCircuit, 
  ArrowRight,
  RefreshCw,
  AlertCircle
} from "lucide-react";
import { fetchStudyResources, fetchRoadmapTopicResources } from "../../services/api";

export interface RoadmapTopicModalProps {
  isOpen: boolean;
  onClose: () => void;
  topic: any;
  subjectCode: string;
  subjectName: string;
  onLaunchAssessment: (subjectCode: string, conceptName: string) => void;
}

export default function RoadmapTopicModal({
  isOpen,
  onClose,
  topic,
  subjectCode,
  subjectName,
  onLaunchAssessment
}: RoadmapTopicModalProps) {
  const [resources, setResources] = useState<any[]>([]);
  const [loadingResources, setLoadingResources] = useState(false);

  useEffect(() => {
    if (isOpen && topic && (topic.conceptName || topic.conceptId)) {
      loadResources();
    }
  }, [isOpen, topic]);

  const loadResources = async () => {
    setLoadingResources(true);
    try {
      const activeUserId = typeof window !== "undefined"
        ? localStorage.getItem("edupilot_user_id") || "anonymous_student"
        : "anonymous_student";

      const conceptId = topic.conceptId || topic.conceptName;
      const resData = await fetchRoadmapTopicResources(
        subjectCode,
        conceptId,
        topic.conceptName,
        subjectName,
        activeUserId
      );

      if (resData && resData.resources && Array.isArray(resData.resources)) {
        setResources(resData.resources);
      } else {
        setResources([]);
      }
    } catch (err) {
      console.warn("Failed to load topic study resources:", err);
      setResources([]);
    } finally {
      setLoadingResources(false);
    }
  };

  if (!isOpen || !topic) return null;

  const isCompleted = topic.isCompleted || topic.status === "COMPLETED";
  const isUnlocked = topic.status === "UNLOCKED" || isCompleted;
  const isLocked = topic.status === "LOCKED" && !isCompleted;

  const currentAcc = topic.currentAccuracy || 0;
  const reqAcc = topic.masteryRequiredAccuracy || 70;

  const getResourceIcon = (type: string) => {
    const t = (type || "").toUpperCase();
    if (t.includes("VIDEO")) return <Video className="h-4 w-4 text-purple-theme shrink-0" />;
    if (t.includes("PRACTICE") || t.includes("CODE")) return <Code className="h-4 w-4 text-emerald-theme shrink-0" />;
    if (t.includes("ACADEMIC") || t.includes("PAPER")) return <Award className="h-4 w-4 text-amber-theme shrink-0" />;
    return <FileText className="h-4 w-4 text-cyan-theme shrink-0" />;
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/70 backdrop-blur-sm animate-fade-in">
      <div 
        className="glass-panel w-full max-w-2xl max-h-[90vh] rounded-3xl border border-white/10 flex flex-col overflow-hidden shadow-2xl bg-gradient-to-br from-[#0e1322] via-[#090c15] to-[#12182b]"
        onClick={(e) => e.stopPropagation()}
      >
        
        {/* Header */}
        <div className="p-6 border-b border-white/5 flex items-start justify-between gap-4 bg-white/5">
          <div className="space-y-1.5 flex-1">
            <div className="flex items-center gap-2 flex-wrap">
              <span className="text-[10px] font-extrabold px-2.5 py-0.5 rounded-full bg-purple-500/20 text-purple-theme border border-purple-500/30">
                Topic #{topic.sequence}
              </span>

              {isCompleted ? (
                <span className="text-[10px] font-extrabold px-2.5 py-0.5 rounded-full bg-emerald-500/20 text-emerald-theme border border-emerald-500/30 flex items-center gap-1">
                  <CheckCircle2 className="h-3 w-3" /> Completed
                </span>
              ) : isUnlocked ? (
                <span className="text-[10px] font-extrabold px-2.5 py-0.5 rounded-full bg-cyan-500/20 text-cyan-theme border border-cyan-500/30 flex items-center gap-1">
                  <Sparkles className="h-3 w-3" /> Unlocked / Current
                </span>
              ) : (
                <span className="text-[10px] font-extrabold px-2.5 py-0.5 rounded-full bg-amber-500/20 text-amber-theme border border-amber-500/30 flex items-center gap-1">
                  <Lock className="h-3 w-3" /> Locked Topic
                </span>
              )}
            </div>

            <h2 className="text-xl font-extrabold text-main-theme leading-snug">
              {topic.conceptName}
            </h2>
            <p className="text-xs text-secondary-theme">
              {subjectName} ({subjectCode})
            </p>
          </div>

          <button
            onClick={onClose}
            className="p-1.5 rounded-xl bg-white/5 hover:bg-white/10 text-secondary-theme hover:text-main-theme transition-all cursor-pointer"
          >
            <X className="h-5 w-5" />
          </button>
        </div>

        {/* Scrollable Content Body */}
        <div className="p-6 overflow-y-auto space-y-6 flex-1 text-xs">
          
          {/* Description */}
          {topic.description && (
            <div className="space-y-1.5">
              <h4 className="text-[11px] font-extrabold uppercase tracking-wider text-secondary-theme">Overview</h4>
              <p className="text-main-theme leading-relaxed bg-white/5 p-3.5 rounded-xl border border-white/5">
                {topic.description}
              </p>
            </div>
          )}

          {/* AI Personalization Rationale */}
          {topic.aiRationale && (
            <div className="glass-panel p-4 rounded-2xl border border-purple-500/20 bg-gradient-to-r from-purple-900/15 via-transparent to-pink-900/15 space-y-2">
              <div className="flex items-center gap-2 text-purple-theme font-bold text-xs">
                <Sparkles className="h-4 w-4" />
                <span>Why this topic is recommended for you</span>
              </div>
              <p className="text-secondary-theme leading-relaxed">
                {topic.aiRationale}
              </p>
            </div>
          )}

          {/* Mastery Metrics */}
          <div className="space-y-2 bg-white/5 p-4 rounded-2xl border border-white/5">
            <div className="flex items-center justify-between">
              <span className="font-extrabold text-main-theme flex items-center gap-1.5">
                <Award className="h-4 w-4 text-amber-theme" /> Verified Mastery Accuracy
              </span>
              <span className="font-black text-main-theme">
                {currentAcc.toFixed(1)}% <span className="text-secondary-theme font-normal">/ {reqAcc}% required</span>
              </span>
            </div>
            <div className="h-2 w-full bg-white/10 rounded-full overflow-hidden">
              <div
                className={`h-full rounded-full transition-all duration-500 ${
                  currentAcc >= reqAcc
                    ? "bg-gradient-to-r from-emerald-500 to-teal-400"
                    : "bg-gradient-to-r from-purple-500 to-cyan-400"
                }`}
                style={{ width: `${Math.min(100, Math.max(0, currentAcc))}%` }}
              />
            </div>
          </div>

          {/* Recommended Learning Materials */}
          <div className="space-y-3">
            <div className="flex items-center justify-between">
              <h4 className="text-[11px] font-extrabold uppercase tracking-wider text-secondary-theme flex items-center gap-1.5">
                <BookOpen className="h-3.5 w-3.5 text-cyan-theme" /> Recommended Study Materials
              </h4>
              <button
                onClick={loadResources}
                className="text-[10px] text-purple-theme hover:underline flex items-center gap-1 cursor-pointer"
              >
                <RefreshCw className={`h-3 w-3 ${loadingResources ? "animate-spin" : ""}`} /> Refresh Materials
              </button>
            </div>

            {loadingResources ? (
              <div className="space-y-2 py-2">
                {[1, 2, 3].map((i) => (
                  <div key={i} className="h-12 bg-white/5 rounded-xl animate-pulse" />
                ))}
              </div>
            ) : resources.length === 0 ? (
              <div className="p-4 text-center text-secondary-theme bg-white/5 rounded-xl border border-white/5">
                No specific external resources found for this concept. Study core lecture notes.
              </div>
            ) : (
              <div className="space-y-2">
                {resources.slice(0, 5).map((res, idx) => (
                  <a
                    key={idx}
                    href={res.url || "#"}
                    target="_blank"
                    rel="noopener noreferrer"
                    className="flex items-center justify-between p-3 rounded-xl bg-white/5 hover:bg-white/10 border border-white/5 transition-all group cursor-pointer"
                  >
                    <div className="flex items-center gap-3 min-w-0 pr-2">
                      {getResourceIcon(res.type || res.category)}
                      <div className="min-w-0">
                        <h5 className="font-semibold text-main-theme truncate group-hover:text-purple-theme transition-colors flex items-center gap-1.5 flex-wrap">
                          <span>{res.title || res.name || topic.conceptName}</span>
                          {idx === 0 && (
                            <span className="shrink-0 text-[9px] font-extrabold px-1.5 py-0.5 rounded-full bg-purple-500/20 text-purple-theme border border-purple-500/30 flex items-center gap-1">
                              <Sparkles className="h-2.5 w-2.5 text-purple-theme" /> VARK Preference Match
                            </span>
                          )}
                        </h5>
                        <p className="text-[10px] text-secondary-theme truncate">
                          {res.source || res.type || "External Resource"}
                        </p>
                      </div>
                    </div>
                    <ExternalLink className="h-3.5 w-3.5 text-secondary-theme group-hover:text-purple-theme shrink-0" />
                  </a>
                ))}
              </div>
            )}
          </div>

        </div>

        {/* Modal Footer / Action Button */}
        <div className="p-6 border-t border-white/5 bg-white/5 flex items-center justify-between gap-4">
          <button
            onClick={onClose}
            className="px-4 py-2.5 rounded-xl bg-white/5 hover:bg-white/10 text-secondary-theme text-xs font-semibold transition-all cursor-pointer"
          >
            Close
          </button>

          {isLocked ? (
            <div className="flex items-center gap-2 text-amber-theme bg-amber-500/10 px-4 py-2.5 rounded-xl border border-amber-500/20 text-xs font-semibold">
              <Lock className="h-4 w-4" />
              <span>Complete prerequisite topics to unlock test</span>
            </div>
          ) : (
            <button
              onClick={() => {
                onClose();
                onLaunchAssessment(subjectCode, topic.conceptName);
              }}
              className="px-6 py-2.5 rounded-xl bg-gradient-to-r from-purple-600 to-indigo-600 hover:from-purple-500 hover:to-indigo-500 text-white text-xs font-extrabold shadow-lg shadow-purple-600/20 flex items-center gap-2 transition-all cursor-pointer"
            >
              <BrainCircuit className="h-4 w-4" />
              <span>{isCompleted ? "Retest Topic Mastery" : "Test Topic Mastery"}</span>
              <ArrowRight className="h-4 w-4" />
            </button>
          )}
        </div>

      </div>
    </div>
  );
}
