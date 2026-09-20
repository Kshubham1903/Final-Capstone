import React from "react";
import { TimelineEvent } from "../../services/api";
import { Clock, Calendar, FileCheck, Award, Activity } from "lucide-react";

interface LearningJourneyTimelineProps {
  timeline: TimelineEvent[];
}

export const LearningJourneyTimeline: React.FC<LearningJourneyTimelineProps> = ({ timeline }) => {
  if (!timeline || timeline.length === 0) {
    return (
      <div className="bg-white rounded-2xl border border-slate-200 p-6 shadow-sm">
        <h3 className="text-lg font-bold text-slate-800 flex items-center gap-2 mb-3">
          <Clock className="w-5 h-5 text-indigo-600" />
          Authentic Assessment Timeline
        </h3>
        <p className="text-sm text-slate-500 italic">
          No assessment events recorded yet. Complete diagnostic or practice sessions to generate timeline events.
        </p>
      </div>
    );
  }

  const getEventIcon = (type: string) => {
    switch (type.toUpperCase()) {
      case "DIAGNOSTIC":
        return <Award className="w-4 h-4 text-purple-600" />;
      case "PROGRESS_ASSESSMENT":
        return <Activity className="w-4 h-4 text-emerald-600" />;
      case "QUIZ":
      case "PRACTICE":
        return <FileCheck className="w-4 h-4 text-blue-600" />;
      default:
        return <Activity className="w-4 h-4 text-emerald-600" />;
    }
  };

  const getEventBadgeColor = (type: string) => {
    switch (type.toUpperCase()) {
      case "DIAGNOSTIC":
        return "bg-purple-100 text-purple-800 border-purple-200";
      case "PROGRESS_ASSESSMENT":
        return "bg-emerald-100 text-emerald-800 border-emerald-200";
      case "QUIZ":
      case "PRACTICE":
        return "bg-blue-100 text-blue-800 border-blue-200";
      default:
        return "bg-slate-100 text-slate-700 border-slate-200";
    }
  };

  return (
    <div className="bg-white rounded-2xl border border-slate-200 p-6 shadow-sm">
      <div className="flex justify-between items-center mb-6">
        <div>
          <h3 className="text-lg font-bold text-slate-800 flex items-center gap-2">
            <Clock className="w-5 h-5 text-indigo-600" />
            Authentic Assessment Timeline
          </h3>
          <p className="text-xs text-slate-500 mt-1">
            Chronological audit log of valid student assessment sessions ($T_0, T_1, T_2, \dots$).
          </p>
        </div>
        <span className="text-xs font-medium text-slate-500 bg-slate-100 px-3 py-1 rounded-full">
          {timeline.length} Recorded Events
        </span>
      </div>

      <div className="relative pl-6 border-l-2 border-slate-200 space-y-6">
        {timeline.map((event, idx) => {
          const typeUpper = event.type ? event.type.toUpperCase() : "";
          const isDiagnostic = typeUpper === "DIAGNOSTIC";
          const isProgress = typeUpper === "PROGRESS_ASSESSMENT";

          return (
            <div key={event.id || idx} className="relative group">
              {/* Dot icon */}
              <div
                className={`absolute -left-[31px] top-0.5 w-6 h-6 rounded-full border-2 bg-white flex items-center justify-center shadow-sm ${
                  isDiagnostic ? "border-purple-600" : isProgress ? "border-emerald-600" : "border-blue-600"
                }`}
              >
                {getEventIcon(event.type)}
              </div>

              <div className="bg-slate-50 hover:bg-slate-100/80 transition-colors p-4 rounded-xl border border-slate-200/80">
                <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-2 mb-2">
                  <div className="flex items-center gap-2">
                    <span className="font-bold text-slate-800 text-sm">{event.title}</span>
                    <span
                      className={`text-[10px] font-bold px-2 py-0.5 rounded border uppercase tracking-wider ${getEventBadgeColor(
                        event.type
                      )}`}
                    >
                      {event.subtitle || event.type}
                    </span>
                  </div>

                  <div className="flex items-center gap-3 text-xs text-slate-500">
                    <span className="flex items-center gap-1 font-mono">
                      <Calendar className="w-3.5 h-3.5 text-slate-400" />
                      {event.timestamp ? new Date(event.timestamp).toLocaleString() : "Date N/A"}
                    </span>
                    {event.scorePercentage !== null && (
                      <span className="font-mono font-bold text-slate-800 bg-white px-2.5 py-0.5 rounded border border-slate-200">
                        {event.scorePercentage.toFixed(1)}%
                      </span>
                    )}
                  </div>
                </div>

                <p className="text-xs text-slate-600 font-mono bg-white p-2.5 rounded-lg border border-slate-200/60">
                  {event.details}
                </p>
              </div>
            </div>
          );
        })}
      </div>
    </div>
  );
};
