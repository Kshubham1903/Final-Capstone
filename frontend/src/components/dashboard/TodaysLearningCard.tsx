import React, { useState, useEffect } from "react";
import { 
  Sparkles, CheckCircle, Clock, ArrowRight, RefreshCw, CheckCircle2, Play, Calendar, ListTodo 
} from "lucide-react";
import { TodaysLearningCardProps } from "./types";
import { fetchTodayPlan, completePlannerTask, startStudySession, regeneratePlan } from "../../services/api";
import StudySessionTrackerModal from "./StudySessionTrackerModal";

export default function TodaysLearningCard({ profile }: TodaysLearningCardProps) {
  const [planData, setPlanData] = useState<any>(null);
  const [loading, setLoading] = useState(true);
  const [regenerating, setRegenerating] = useState(false);
  const [activeSession, setActiveSession] = useState<any>(null);

  const userId = profile?.id || localStorage.getItem("edupilot_user_id") || "";

  const loadPlan = async () => {
    setLoading(true);
    if (userId) {
      const data = await fetchTodayPlan(userId);
      setPlanData(data);
    }
    setLoading(false);
  };

  useEffect(() => {
    loadPlan();
  }, [profile]);

  const handleRegenerate = async () => {
    setRegenerating(true);
    if (userId) {
      const data = await regeneratePlan(userId);
      setPlanData(data);
    }
    setRegenerating(false);
  };

  const handleTaskComplete = async (taskId: string) => {
    if (userId) {
      const updated = await completePlannerTask(taskId, userId);
      if (updated) {
        setPlanData(updated);
      } else {
        loadPlan();
      }
    }
  };

  const handleStartSession = async (task: any) => {
    if (!userId) return;
    const sessionRes = await startStudySession({
      userId,
      taskId: task.taskId,
      subjectCode: task.subjectCode || "CS301",
      conceptName: task.conceptName || task.topic || "Core Concept"
    });

    if (sessionRes) {
      setActiveSession({
        sessionId: sessionRes.id,
        taskId: task.taskId,
        subjectCode: task.subjectCode || "CS301",
        conceptName: task.conceptName || task.topic || "Core Concept"
      });
    }
  };

  const tasks = planData?.tasks || [];
  const totalTasks = planData?.totalTasks || tasks.length;
  const completedTasks = planData?.completedTasks || 0;
  const completionPercentage = planData?.completionPercentage || (totalTasks > 0 ? (completedTasks * 100) / totalTasks : 0);

  const getPriorityBadge = (priority: string) => {
    switch (priority) {
      case "CRITICAL":
        return "bg-pink-500/15 text-pink-300 border-pink-500/30";
      case "HIGH":
        return "bg-amber-500/15 text-amber-300 border-amber-500/30";
      case "MEDIUM":
        return "bg-purple-500/15 text-purple-300 border-purple-500/30";
      default:
        return "bg-cyan-500/15 text-cyan-300 border-cyan-500/30";
    }
  };

  return (
    <div className="glass-panel p-6 rounded-2xl border border-white/5 space-y-5 bg-gradient-to-br from-purple-900/10 via-transparent to-emerald-900/10">
      
      {/* Header & Progress Meter */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 border-b border-white/5 pb-4">
        <div className="flex items-center gap-2.5">
          <div className="h-9 w-9 rounded-xl bg-purple-500/10 flex items-center justify-center text-purple-theme border border-purple-500/20">
            <ListTodo className="h-5 w-5" />
          </div>
          <div>
            <h3 className="text-base font-extrabold tracking-wide text-main-theme">Today's Personalized Learning Plan</h3>
            <p className="text-[11px] text-secondary-theme">Adaptive study roadmap generated from Recommendation Engine outputs</p>
          </div>
        </div>

        <div className="flex items-center gap-3 self-start sm:self-auto">
          {/* Progress Badge */}
          <div className="flex items-center gap-2 bg-white/5 px-3 py-1.5 rounded-xl border border-white/10 text-xs">
            <Calendar className="h-3.5 w-3.5 text-emerald-theme" />
            <span className="text-secondary-theme">Progress:</span>
            <span className="font-black text-emerald-theme">{Math.round(completionPercentage)}%</span>
          </div>

          <button
            onClick={handleRegenerate}
            disabled={regenerating}
            className="p-2 rounded-xl bg-white/5 hover:bg-white/10 text-secondary-theme hover:text-main-theme transition-all cursor-pointer"
            title="Regenerate Plan"
          >
            <RefreshCw className={`h-4 w-4 text-purple-theme ${regenerating ? "animate-spin" : ""}`} />
          </button>
        </div>
      </div>

      {/* Progress Bar */}
      <div className="w-full bg-white/5 h-2 rounded-full overflow-hidden border border-white/5">
        <div 
          className="bg-gradient-to-r from-purple-500 via-pink-500 to-emerald-400 h-full transition-all duration-500"
          style={{ width: `${Math.max(5, completionPercentage)}%` }}
        />
      </div>

      {/* Tasks List */}
      {loading ? (
        <div className="p-6 text-center animate-pulse space-y-2">
          <div className="h-4 w-44 bg-white/10 rounded mx-auto" />
          <div className="h-14 bg-white/5 rounded-xl" />
        </div>
      ) : tasks.length > 0 ? (
        <div className="space-y-3">
          {tasks.map((task: any, index: number) => {
            const isCompleted = task.status === "COMPLETED";
            return (
              <div
                key={task.taskId || index}
                className={`p-4 rounded-xl glass-panel border transition-all space-y-2.5 ${
                  isCompleted 
                    ? "bg-emerald-500/5 border-emerald-500/20 opacity-75" 
                    : "border-white/5 hover:border-purple-500/30"
                }`}
              >
                <div className="flex items-center justify-between gap-2">
                  <div className="flex items-center gap-2">
                    <span className={`px-2.5 py-0.5 rounded-md text-[10px] font-black uppercase border ${getPriorityBadge(task.priority)}`}>
                      {task.priority || "MEDIUM"}
                    </span>
                    <span className="text-[10px] font-bold text-secondary-theme bg-white/5 px-2 py-0.5 rounded border border-white/10">
                      {task.subjectCode || "CS301"}
                    </span>
                  </div>

                  <div className="flex items-center gap-2">
                    <span className="text-[10px] text-secondary-theme font-bold flex items-center gap-1">
                      <Clock className="h-3 w-3 text-purple-theme" />
                      {task.estimatedStudyTimeMinutes || 20} mins
                    </span>

                    {!isCompleted ? (
                      <div className="flex items-center gap-1.5">
                        <button
                          onClick={() => handleStartSession(task)}
                          className="px-2.5 py-1 rounded-lg bg-purple-600/30 hover:bg-purple-600/50 text-purple-300 text-[10px] font-bold border border-purple-500/30 flex items-center gap-1 transition-all cursor-pointer"
                        >
                          <Play className="h-3 w-3 fill-current" />
                          <span>Start Study</span>
                        </button>
                        <button
                          onClick={() => handleTaskComplete(task.taskId)}
                          className="p-1 rounded-lg bg-emerald-500/10 hover:bg-emerald-500/20 text-emerald-400 border border-emerald-500/20 transition-all cursor-pointer"
                          title="Mark Task Complete"
                        >
                          <CheckCircle className="h-4 w-4" />
                        </button>
                      </div>
                    ) : (
                      <span className="text-[10px] font-bold text-emerald-400 flex items-center gap-1 bg-emerald-500/10 border border-emerald-500/20 px-2 py-0.5 rounded-md">
                        <CheckCircle2 className="h-3 w-3" />
                        <span>Completed</span>
                      </span>
                    )}
                  </div>
                </div>

                {/* Content */}
                <div>
                  <h4 className={`text-sm font-extrabold ${isCompleted ? "line-through text-secondary-theme" : "text-main-theme"}`}>
                    {task.conceptName || task.topic || "Core Concept Study"}
                  </h4>
                  <p className="text-xs text-emerald-400 font-medium flex items-center gap-1 mt-0.5">
                    <ArrowRight className="h-3.5 w-3.5 shrink-0" />
                    <span>{task.recommendedAction}</span>
                  </p>
                </div>

                {/* Explainability Reason */}
                {task.reason && (
                  <div className="p-2 rounded-lg bg-white/3 border border-white/5 text-[11px] text-secondary-theme italic">
                    {task.reason}
                  </div>
                )}
              </div>
            );
          })}
        </div>
      ) : (
        <div className="p-6 bg-emerald-500/5 border border-emerald-500/15 rounded-xl text-center space-y-2">
          <CheckCircle2 className="h-8 w-8 text-emerald-theme mx-auto opacity-80" />
          <h4 className="font-bold text-xs text-main-theme">All Daily Learning Tasks Completed!</h4>
          <p className="text-[10px] text-secondary-theme max-w-md mx-auto">
            You are fully caught up with today's adaptive roadmap. Great job!
          </p>
        </div>
      )}

      {/* Active Study Session Modal Tracker */}
      {activeSession && (
        <StudySessionTrackerModal
          sessionData={activeSession}
          onClose={() => setActiveSession(null)}
          onSessionFinished={() => loadPlan()}
        />
      )}

    </div>
  );
}
