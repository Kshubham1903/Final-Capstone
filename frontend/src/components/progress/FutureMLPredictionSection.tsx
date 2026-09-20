import React from "react";
import { Sparkles, Binary, Lock, Database, ArrowRight } from "lucide-react";

export const FutureMLPredictionSection: React.FC = () => {
  return (
    <div className="bg-slate-900 rounded-2xl border border-slate-800 p-6 shadow-md text-slate-100 relative overflow-hidden">
      {/* Background Decorative Gradient Grid */}
      <div className="absolute -right-10 -bottom-10 w-64 h-64 bg-indigo-500/10 rounded-full blur-3xl pointer-events-none" />
      <div className="absolute -left-10 -top-10 w-64 h-64 bg-purple-500/10 rounded-full blur-3xl pointer-events-none" />

      <div className="relative z-10">
        <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-2 mb-6">
          <div>
            <h3 className="text-lg font-bold text-white flex items-center gap-2">
              <Sparkles className="w-5 h-5 text-indigo-400" />
              Machine Learning Growth Prediction Model
            </h3>
            <p className="text-xs text-slate-400 mt-1">
              Architectural Pipeline &amp; Feature Vector Specification for Predictive Outcome Modeling.
            </p>
          </div>
          <span className="text-xs font-mono font-semibold px-3 py-1 bg-indigo-950 text-indigo-300 rounded-full border border-indigo-800 flex items-center gap-1.5">
            <Lock className="w-3.5 h-3.5 text-indigo-400" />
            Phase 2 ML Pipeline Ready
          </span>
        </div>

        <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
          {/* Feature Vector Card */}
          <div className="p-4 rounded-xl bg-slate-800/60 border border-slate-700/80">
            <h4 className="font-bold text-slate-200 text-xs uppercase tracking-wider mb-2 flex items-center gap-1.5">
              <Binary className="w-4 h-4 text-indigo-400" />
              1. Input Feature Vector ($X$)
            </h4>
            <div className="bg-slate-950/80 p-3 rounded-lg border border-slate-800 text-xs font-mono text-slate-300 space-y-1.5">
              <div><span className="text-indigo-400 font-bold">K</span>: Concept Masteries [K0...Kt]</div>
              <div><span className="text-emerald-400 font-bold">P</span>: Pace &amp; Preference Parameters</div>
              <div><span className="text-amber-400 font-bold">E</span>: Session Frequency &amp; Completion</div>
              <div><span className="text-rose-400 font-bold">W</span>: Cognitive Fatigue &amp; Stress</div>
              <div><span className="text-cyan-400 font-bold">T</span>: Assessment Trajectory Deltas</div>
            </div>
            <p className="text-[11px] text-slate-400 mt-2">
              Extractable via backend endpoint <code className="text-slate-300 bg-slate-900 px-1 py-0.5 rounded">GET /api/student-growth/&#123;userId&#125;/ml-dataset</code>.
            </p>
          </div>

          {/* Model Architecture Card */}
          <div className="p-4 rounded-xl bg-slate-800/60 border border-slate-700/80">
            <h4 className="font-bold text-slate-200 text-xs uppercase tracking-wider mb-2 flex items-center gap-1.5">
              <Database className="w-4 h-4 text-purple-400" />
              2. Target Output ($Y$)
            </h4>
            <div className="bg-slate-950/80 p-3 rounded-lg border border-slate-800 text-xs font-mono text-slate-300 space-y-1.5">
              <div><span className="text-purple-400 font-bold">Y_growth</span>: Predicted Gain (pp)</div>
              <div><span className="text-purple-400 font-bold">Y_retention</span>: Concept Decay Prob</div>
              <div><span className="text-purple-400 font-bold">Y_opt_diff</span>: Recommended Diff</div>
            </div>
            <p className="text-[11px] text-slate-400 mt-2">
              Models continuous outcome trajectory without requiring artificial post-tests.
            </p>
          </div>

          {/* Status & Training Readiness */}
          <div className="p-4 rounded-xl bg-slate-800/60 border border-slate-700/80 flex flex-col justify-between">
            <div>
              <h4 className="font-bold text-slate-200 text-xs uppercase tracking-wider mb-2 flex items-center gap-1.5">
                <ArrowRight className="w-4 h-4 text-emerald-400" />
                3. Training Readiness
              </h4>
              <p className="text-xs text-slate-300 leading-relaxed">
                The growth pipeline is fully instrumented to collect authentic feature vectors. Synthetic ML predictions are intentionally omitted until empirical sample threshold ($N \ge 100$) is satisfied.
              </p>
            </div>

            <div className="mt-4 pt-3 border-t border-slate-700/60 flex items-center justify-between text-[11px] text-slate-400">
              <span>Status: <strong className="text-emerald-400 font-normal">Data Collection Active</strong></span>
              <span className="font-mono text-slate-400">Model: XGBoost / LSTM</span>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};
