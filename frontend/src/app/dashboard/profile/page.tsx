import React, { useState, useEffect } from "react";
import Layout from "../../../components/Layout";
import { 
  User, 
  GraduationCap, 
  Activity, 
  Save, 
  Flame, 
  Award, 
  RefreshCw, 
  CheckCircle,
  AlertCircle,
  Building2
} from "lucide-react";
import { fetchFullProfile, updateFullProfile } from "../../../services/api";

export default function ProfilePage() {
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [successMsg, setSuccessMsg] = useState("");
  const [errorMsg, setErrorMsg] = useState("");

  const userId = typeof window !== "undefined" ? (localStorage.getItem("edupilot_user_id") || "") : "";

  // Academic Profile State
  const [fullName, setFullName] = useState("");
  const [email, setEmail] = useState("");
  const [institution, setInstitution] = useState("EduPilot Academy");
  const [degree, setDegree] = useState("B.Tech");
  const [branch, setBranch] = useState("Computer Science & Engineering");
  const [semester, setSemester] = useState(1);
  const [currentCgpa, setCurrentCgpa] = useState(8.0);
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
        setInstitution(p.institution || "EduPilot Academy");
        setDegree(p.degree || "B.Tech");
        setBranch(p.branch || p.course || "Computer Science & Engineering");
        setSemester(p.semester || 1);
        setCurrentCgpa(p.currentCgpa || 8.0);
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
    setErrorMsg("");
    setSuccessMsg("");

    // Validations
    if (!fullName.trim()) {
      setErrorMsg("Student Name is required.");
      return;
    }
    if (!institution.trim()) {
      setErrorMsg("Institution name is required.");
      return;
    }
    if (!degree.trim()) {
      setErrorMsg("Degree program is required.");
      return;
    }
    if (!branch.trim()) {
      setErrorMsg("Branch / Specialization is required.");
      return;
    }
    if (semester < 1 || semester > 12) {
      setErrorMsg("Semester must be between 1 and 12.");
      return;
    }
    if (currentCgpa < 0.0 || currentCgpa > 10.0) {
      setErrorMsg("Current CGPA must be between 0.0 and 10.0.");
      return;
    }
    if (targetCgpa < 0.0 || targetCgpa > 10.0) {
      setErrorMsg("Target CGPA must be between 0.0 and 10.0.");
      return;
    }

    setSaving(true);

    const updatePayload = {
      fullName,
      institution,
      degree,
      branch,
      course: branch,
      semester: Number(semester),
      currentCgpa: Number(currentCgpa),
      targetCgpa: Number(targetCgpa),
      preferredStudyHoursPerDay: Number(preferredStudyHours),
      learningStyle,
      careerGoals: careerGoals.split(",").map(s => s.trim())
    };

    try {
      const updated = await updateFullProfile(userId, updatePayload);
      if (updated) {
        setSgi(updated.studentGrowthIndex || sgi);
        setPredictedCgpa(updated.predictedCgpa || predictedCgpa);
        setRiskLevel(updated.academicRiskLevel || riskLevel);
        setSuccessMsg("Academic profile updated successfully! Python AI recalculated your Student Growth Index.");
      }
    } catch (err: any) {
      setErrorMsg("Failed to save academic profile changes.");
    } finally {
      setSaving(false);
    }

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
              <span>Academic Profile & Student Identity</span>
            </h1>
            <p className="text-secondary-theme text-sm mt-1">
              Primary academic identity powering adaptive diagnostic assessments, AI recommendations, and tutoring.
            </p>
          </div>
        </div>

        {errorMsg && (
          <div className="p-4 rounded-2xl bg-pink-500/10 border border-pink-500/20 text-pink-300 text-xs flex items-center gap-2">
            <AlertCircle className="h-5 w-5" />
            <span className="font-bold">{errorMsg}</span>
          </div>
        )}

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
            <span className="text-[10px] text-secondary-theme uppercase font-extrabold tracking-wider">Current CGPA</span>
            <div className="text-3xl font-black text-purple-theme">{currentCgpa.toFixed(1)}</div>
            <p className="text-[10px] text-secondary-theme">Verified academic record.</p>
          </div>

          <div className="glass-panel p-6 rounded-3xl border border-white/10 space-y-2">
            <span className="text-[10px] text-secondary-theme uppercase font-extrabold tracking-wider">Target CGPA</span>
            <div className="text-3xl font-black text-emerald-theme">{targetCgpa.toFixed(1)}</div>
            <p className="text-[10px] text-secondary-theme">Target GPA goal.</p>
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
              <h3 className="text-lg font-extrabold tracking-wide text-main-theme">Academic Profile Identity Details</h3>
              <p className="text-xs text-secondary-theme mt-0.5">Saving modifications automatically triggers the Python AI microservice to recalculate your SGI.</p>
            </div>
          </div>

          <form onSubmit={handleSaveProfile} className="space-y-6 text-xs">
            <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
              <div>
                <label className="font-bold text-secondary-theme block mb-1">Student Full Name *</label>
                <input 
                  type="text" 
                  required
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
                <label className="font-bold text-secondary-theme block mb-1">Institution / University Name *</label>
                <input 
                  type="text" 
                  required
                  value={institution} 
                  onChange={e => setInstitution(e.target.value)} 
                  className="w-full p-3 rounded-xl glass-input" 
                  placeholder="e.g. EduPilot Academy of Technology"
                />
              </div>

              <div>
                <label className="font-bold text-secondary-theme block mb-1">Degree Program *</label>
                <input 
                  type="text" 
                  required
                  value={degree} 
                  onChange={e => setDegree(e.target.value)} 
                  className="w-full p-3 rounded-xl glass-input" 
                  placeholder="e.g. B.Tech"
                />
              </div>

              <div>
                <label className="font-bold text-secondary-theme block mb-1">Branch / Specialization *</label>
                <input 
                  type="text" 
                  required
                  value={branch} 
                  onChange={e => setBranch(e.target.value)} 
                  className="w-full p-3 rounded-xl glass-input" 
                  placeholder="e.g. Computer Science & Engineering"
                />
              </div>

              <div>
                <label className="font-bold text-secondary-theme block mb-1">Current Semester (1-12) *</label>
                <input 
                  type="number" 
                  min="1" 
                  max="12" 
                  required
                  value={semester} 
                  onChange={e => setSemester(Number(e.target.value))} 
                  className="w-full p-3 rounded-xl glass-input" 
                />
              </div>

              <div>
                <label className="font-bold text-secondary-theme block mb-1">Current CGPA (0.0 - 10.0) *</label>
                <input 
                  type="number" 
                  step="0.1" 
                  min="0" 
                  max="10" 
                  required
                  value={currentCgpa} 
                  onChange={e => setCurrentCgpa(Number(e.target.value))} 
                  className="w-full p-3 rounded-xl glass-input" 
                />
              </div>

              <div>
                <label className="font-bold text-secondary-theme block mb-1">Target CGPA Goal (0.0 - 10.0) *</label>
                <input 
                  type="number" 
                  step="0.1" 
                  min="0" 
                  max="10" 
                  required
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

              <div className="md:col-span-2">
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
                    <span>Save Academic Profile & Recalculate AI SGI</span>
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
