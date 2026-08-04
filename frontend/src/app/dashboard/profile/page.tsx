import React, { useState, useEffect } from "react";
import Layout from "../../../components/Layout";
import { 
  User, 
  GraduationCap, 
  Activity, 
  Sparkles, 
  Save, 
  ShieldCheck, 
  Flame, 
  Award, 
  RefreshCw, 
  CheckCircle,
  FileText,
  Clock,
  BookOpen
} from "lucide-react";
import { fetchFullProfile, updateFullProfile } from "../../../services/api";

export default function ProfilePage() {
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [successMsg, setSuccessMsg] = useState("");

  const userId = typeof window !== "undefined" ? (localStorage.getItem("edupilot_user_id") || "") : "";

  // Aggregate State
  const [fullName, setFullName] = useState("");
  const [email, setEmail] = useState("");
  const [course, setCourse] = useState("");
  const [semester, setSemester] = useState(1);
  const [targetCgpa, setTargetCgpa] = useState(8.5);
  const [preferredStudyHours, setPreferredStudyHours] = useState(4.0);
  const [learningStyle, setLearningStyle] = useState("Visual");
  const [careerGoals, setCareerGoals] = useState("");

  // AI Calculated Metrics
  const [sgi, setSgi] = useState(0.0);
  const [predictedCgpa, setPredictedCgpa] = useState(0.0);
  const [riskLevel, setRiskLevel] = useState("LOW");
  const [streakCount, setStreakCount] = useState(0);

  useEffect(() => {
    async function loadData() {
      if (!userId) {
        setLoading(false);
        return;
      }
      const data = await fetchFullProfile(userId);
      if (data && data.profile) {
        const p = data.profile;
        setFullName(p.fullName || localStorage.getItem("edupilot_user_name") || "");
        setEmail(p.email || localStorage.getItem("edupilot_user_email") || "");
        setCourse(p.course || "Computer Science & Engineering");
        setSemester(p.semester || 1);
        setTargetCgpa(p.targetCgpa || 8.5);
        setPreferredStudyHours(p.preferredStudyHoursPerDay || 4.0);
        setLearningStyle(p.learningStyle || "Visual");
        setCareerGoals(p.careerGoals ? p.careerGoals.join(", ") : "Software Engineer");

        setSgi(p.studentGrowthIndex || 0.0);
        setPredictedCgpa(p.predictedCgpa || 0.0);
        setRiskLevel(p.academicRiskLevel || "LOW");
        setStreakCount(p.currentStreakCount || 0);
      }
      setLoading(false);
    }
    loadData();
  }, [userId]);

  const handleSaveProfile = async (e: React.FormEvent) => {
    e.preventDefault();
    setSaving(true);
    setSuccessMsg("");

    const updatePayload = {
      fullName,
      course,
      semester: Number(semester),
      targetCgpa: Number(targetCgpa),
      preferredStudyHoursPerDay: Number(preferredStudyHours),
      learningStyle,
      careerGoals: careerGoals.split(",").map(s => s.trim())
    };

    const updated = await updateFullProfile(userId, updatePayload);
    if (updated) {
      setSgi(updated.studentGrowthIndex || sgi);
      setPredictedCgpa(updated.predictedCgpa || predictedCgpa);
      setRiskLevel(updated.academicRiskLevel || riskLevel);
      setSuccessMsg("Profile updated successfully! AI predictions recalculation completed.");
    }
    setSaving(false);

    setTimeout(() => {
      setSuccessMsg("");
    }, 4000);
  };

  if (loading) {
    return (
      <Layout>
        <div className="min-h-[60vh] flex items-center justify-center">
          <div className="animate-spin rounded-full h-10 w-10 border-b-2 border-purple-500" />
        </div>
      </Layout>
    );
  }

  return (
    <Layout>
      <div className="space-y-8">
        
        {/* Title */}
        <div className="flex justify-between items-center">
          <div>
            <h1 className="text-3xl font-extrabold text-main-theme flex items-center gap-2">
              <User className="h-8 w-8 text-purple-theme" />
              <span>Student Profile & Analytics Center</span>
            </h1>
            <p className="text-secondary-theme text-sm mt-1">
              Manage your academic parameters, view your Student Growth Index (SGI), and trigger live AI recalculations.
            </p>
          </div>
        </div>

        {successMsg && (
          <div className="p-4 rounded-2xl bg-emerald-500/10 border border-emerald-500/20 text-emerald-400 text-xs flex items-center gap-2 animate-bounce">
            <CheckCircle className="h-5 w-5" />
            <span className="font-bold">{successMsg}</span>
          </div>
        )}

        {/* AI Analytics Hero Banner */}
        <div className="grid grid-cols-1 md:grid-cols-4 gap-6">
          <div className="glass-panel p-6 rounded-3xl border border-white/10 space-y-2 bg-gradient-to-br from-purple-900/20 to-pink-900/10">
            <span className="text-[10px] text-secondary-theme uppercase font-extrabold tracking-wider">Student Growth Index (SGI)</span>
            <div className="text-3xl font-black text-transparent bg-clip-text bg-gradient-to-r from-purple-400 to-pink-500">
              {sgi.toFixed(1)} <span className="text-xs text-secondary-theme">/ 10</span>
            </div>
            <p className="text-[10px] text-secondary-theme">Live AI computed growth velocity.</p>
          </div>

          <div className="glass-panel p-6 rounded-3xl border border-white/10 space-y-2">
            <span className="text-[10px] text-secondary-theme uppercase font-extrabold tracking-wider">Predicted CGPA</span>
            <div className="text-3xl font-black text-cyan-theme">{predictedCgpa.toFixed(2)}</div>
            <p className="text-[10px] text-secondary-theme">Random Forest model prediction.</p>
          </div>

          <div className="glass-panel p-6 rounded-3xl border border-white/10 space-y-2">
            <span className="text-[10px] text-secondary-theme uppercase font-extrabold tracking-wider">Academic Risk Level</span>
            <div className="flex items-center gap-2 pt-1">
              <span className={`px-3 py-1 rounded-full text-xs font-black uppercase ${
                riskLevel === "LOW" ? "bg-emerald-500/20 text-emerald-400 border border-emerald-500/30" :
                riskLevel === "MEDIUM" ? "bg-amber-500/20 text-amber-400 border border-amber-500/30" :
                "bg-pink-500/20 text-pink-400 border border-pink-500/30"
              }`}>
                {riskLevel} RISK
              </span>
            </div>
            <p className="text-[10px] text-secondary-theme">Automated diagnostic status.</p>
          </div>

          <div className="glass-panel p-6 rounded-3xl border border-white/10 space-y-2 bg-amber-500/5">
            <span className="text-[10px] text-amber-theme uppercase font-extrabold tracking-wider">Current Streak</span>
            <div className="text-3xl font-black text-amber-theme flex items-center gap-1">
              <Flame className="h-6 w-6 fill-amber-500" />
              <span>{streakCount} Days</span>
            </div>
            <p className="text-[10px] text-secondary-theme">Continuous learning activity.</p>
          </div>
        </div>

        {/* Edit Profile Form Card */}
        <div className="glass-panel p-8 rounded-3xl border border-white/10 space-y-6">
          <div className="border-b border-white/10 pb-4 flex justify-between items-center">
            <div>
              <h3 className="text-lg font-extrabold tracking-wide text-main-theme">Edit Profile & Academic Parameters</h3>
              <p className="text-xs text-secondary-theme mt-0.5">Saving modifications automatically triggers the Python AI microservice to recalculate your SGI.</p>
            </div>
          </div>

          <form onSubmit={handleSaveProfile} className="space-y-6 text-xs">
            <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
              <div>
                <label className="font-bold text-secondary-theme block mb-1">Full Name</label>
                <input 
                  type="text" 
                  value={fullName} 
                  onChange={e => setFullName(e.target.value)} 
                  className="w-full p-3 rounded-xl glass-input" 
                />
              </div>

              <div>
                <label className="font-bold text-secondary-theme block mb-1">Email Address</label>
                <input 
                  type="email" 
                  disabled 
                  value={email} 
                  className="w-full p-3 rounded-xl glass-input opacity-60 cursor-not-allowed" 
                />
              </div>

              <div>
                <label className="font-bold text-secondary-theme block mb-1">Engineering Branch / Program</label>
                <input 
                  type="text" 
                  value={course} 
                  onChange={e => setCourse(e.target.value)} 
                  className="w-full p-3 rounded-xl glass-input" 
                />
              </div>

              <div>
                <label className="font-bold text-secondary-theme block mb-1">Current Semester</label>
                <input 
                  type="number" 
                  min="1" 
                  max="8" 
                  value={semester} 
                  onChange={e => setSemester(Number(e.target.value))} 
                  className="w-full p-3 rounded-xl glass-input" 
                />
              </div>

              <div>
                <label className="font-bold text-secondary-theme block mb-1">Target CGPA Goal (0.0 - 10.0)</label>
                <input 
                  type="number" 
                  step="0.1" 
                  value={targetCgpa} 
                  onChange={e => setTargetCgpa(Number(e.target.value))} 
                  className="w-full p-3 rounded-xl glass-input" 
                />
              </div>

              <div>
                <label className="font-bold text-secondary-theme block mb-1">Preferred Daily Study Hours</label>
                <input 
                  type="number" 
                  step="0.5" 
                  value={preferredStudyHours} 
                  onChange={e => setPreferredStudyHours(Number(e.target.value))} 
                  className="w-full p-3 rounded-xl glass-input" 
                />
              </div>

              <div>
                <label className="font-bold text-secondary-theme block mb-1">Learning Style Preference</label>
                <select 
                  value={learningStyle} 
                  onChange={e => setLearningStyle(e.target.value)} 
                  className="w-full p-3 rounded-xl glass-input"
                >
                  <option value="Kinesthetic (Coding-first)">Kinesthetic (Coding-first)</option>
                  <option value="Visual (Diagrams & Flowcharts)">Visual (Diagrams & Flowcharts)</option>
                  <option value="Auditory (Lectures & Discussions)">Auditory (Lectures & Discussions)</option>
                  <option value="Reading / Writing">Reading / Writing</option>
                </select>
              </div>

              <div>
                <label className="font-bold text-secondary-theme block mb-1">Target Career Roles (Comma Separated)</label>
                <input 
                  type="text" 
                  value={careerGoals} 
                  onChange={e => setCareerGoals(e.target.value)} 
                  className="w-full p-3 rounded-xl glass-input" 
                />
              </div>
            </div>

            <div className="pt-4 border-t border-white/10 flex justify-end">
              <button
                type="submit"
                disabled={saving}
                className="flex items-center gap-2 px-8 py-3 rounded-xl bg-gradient-to-r from-purple-600 to-pink-600 hover:from-purple-500 hover:to-pink-500 text-white font-extrabold text-xs shadow-lg shadow-purple-500/20 transition-all cursor-pointer"
              >
                {saving ? (
                  <>
                    <RefreshCw className="h-4 w-4 animate-spin" />
                    <span>Recalculating AI Models...</span>
                  </>
                ) : (
                  <>
                    <Save className="h-4 w-4" />
                    <span>Save Profile & Recalculate AI SGI</span>
                  </>
                )}
              </button>
            </div>
          </form>
        </div>

      </div>
    </Layout>
  );
}
