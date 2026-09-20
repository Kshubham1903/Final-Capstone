import React from "react";
import { BarChart2 } from "lucide-react";
import { LineChart, Line, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer } from "recharts";
import { TrajectoryPoint } from "../../services/api";

export interface LearningTrajectoryChartProps {
  trajectory: TrajectoryPoint[];
  recentGain?: number;
}

export default function LearningTrajectoryChart({ trajectory, recentGain }: LearningTrajectoryChartProps) {
  const chartData = (trajectory || []).map((point, index) => ({
    eventLabel: `T${point.assessmentIndex ?? (index + 1)}`,
    type: point.sessionType ? point.sessionType.replace("_", " ") : "Event",
    score: point.scorePercentage != null ? Math.round(point.scorePercentage * 10) / 10 : 0,
    cumulativeGrowthPp: point.cumulativeGrowthPp != null ? point.cumulativeGrowthPp : 0,
    recentGainPp: point.recentGainPp != null ? point.recentGainPp : 0,
    conceptsCount: point.conceptsCovered ? point.conceptsCovered.length : 0,
    date: point.timestamp ? new Date(point.timestamp).toLocaleDateString(undefined, { month: "short", day: "numeric", hour: "2-digit", minute: "2-digit" }) : `T${point.assessmentIndex ?? (index + 1)}`
  }));

  const latestGain = recentGain != null
    ? recentGain
    : trajectory && trajectory.length > 0
    ? trajectory[trajectory.length - 1].recentGainPp ?? 0
    : 0;

  return (
    <div className="bg-white rounded-2xl border border-slate-200 p-6 shadow-sm space-y-4">
      <div className="flex items-center justify-between border-b border-slate-100 pb-3">
        <div className="flex items-center space-x-2">
          <BarChart2 className="w-5 h-5 text-indigo-600" />
          <h3 className="text-base font-bold text-slate-800">
            Authentic Learning Trajectory ($T_0 \rightarrow T_n$)
          </h3>
        </div>
        <div className="flex items-center space-x-2">
          <span className="text-xs text-slate-500 font-medium">
            Recent Gain:{" "}
            <span className={`font-bold ${latestGain >= 0 ? "text-emerald-600" : "text-rose-600"}`}>
              {latestGain >= 0 ? `+${latestGain.toFixed(1)} pp` : `${latestGain.toFixed(1)} pp`}
            </span>
          </span>
        </div>
      </div>

      {chartData.length > 0 ? (
        <div className="h-64 w-full">
          <ResponsiveContainer width="100%" height="100%">
            <LineChart data={chartData} margin={{ top: 10, right: 20, left: -20, bottom: 0 }}>
              <CartesianGrid strokeDasharray="3 3" stroke="#f1f5f9" />
              <XAxis dataKey="eventLabel" stroke="#64748b" fontSize={12} />
              <YAxis domain={[0, 100]} stroke="#64748b" fontSize={12} />
              <Tooltip
                contentStyle={{ backgroundColor: "#0f172a", borderColor: "#334155", borderRadius: "0.75rem" }}
                labelStyle={{ color: "#f8fafc", fontWeight: "bold" }}
                formatter={(value: any, name: any, props: any) => [
                  `${value}% Score (Growth: ${props.payload.cumulativeGrowthPp > 0 ? '+' : ''}${props.payload.cumulativeGrowthPp.toFixed(1)} pp)`,
                  `${props.payload.type} (${props.payload.conceptsCount} Concepts)`
                ]}
              />
              <Line
                type="monotone"
                dataKey="score"
                stroke="#4f46e5"
                strokeWidth={3}
                dot={{ r: 5, fill: "#4f46e5" }}
                activeDot={{ r: 7, fill: "#6366f1" }}
              />
            </LineChart>
          </ResponsiveContainer>
        </div>
      ) : (
        <div className="py-12 text-center text-xs text-slate-400 italic">
          No assessment trajectory points recorded yet. Complete an initial diagnostic to establish $T_0$.
        </div>
      )}
    </div>
  );
}
