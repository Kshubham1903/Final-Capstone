import React, { useState } from "react";
import { 
  GraduationCap, Building2, BookOpen, Target, Award, BrainCircuit, Activity, Edit3, X, Check, RefreshCw 
} from "lucide-react";
import { AcademicProfileCardProps } from "./types";
import { updateFullProfile } from "../../services/api";

export default function AcademicProfileCard({ profile, predPerformanceLevel }: AcademicProfileCardProps) {
  const [showEditModal, setShowEditModal] = useState(false);
  const [saving, setSaving] = useState(false);
  const [errorMsg, setErrorMsg] = useState("");

  // Edit form state
  const [nameInput, setNameInput] = useState(profile?.fullName || "");
  const [institutionInput, setInstitutionInput] = useState(profile?.institution || "EduPilot Academy");
  const [degreeInput, setDegreeInput] = useState(profile?.degree || "B.Tech");
  const [branchInput, setBranchInput] = useState(profile?.branch || profile?.course || "Computer Science & Engineering");
  const [semesterInput, setSemesterInput] = useState(profile?.semester ?? 1);
  const [currentCgpaInput, setCurrentCgpaInput] = useState(profile?.currentCgpa ?? 8.0);
  const [targetCgpaInput, setTargetCgpaInput] = useState(profile?.targetCgpa ?? 8.5);

  const studentName = profile?.fullName || "Student";
  const institution = profile?.institution || "EduPilot Academy";
  const degree = profile?.degree || "B.Tech";
  const branch = profile?.branch || profile?.course || "Computer Science & Engineering";
  const semester = profile?.semester ?? 1;
  const currentCgpa = profile?.currentCgpa ?? 8.0;
  const targetCgpa = profile?.targetCgpa ?? 8.5;
  const predictedCgpa = profile?.predictedCgpa ?? 0.0;
  const sgi = profile?.studentGrowthIndex ?? 0.0;
  const riskLevel = profile?.academicRiskLevel || "LOW";
  const subjects = profile?.subjects || ["Data Structures & Algorithms", "Database Management Systems", "Artificial Intelligence"];

  const getRiskBadgeColor = (risk: string) => {
    switch (risk) {
      case "HIGH":
        return "text-pink-theme bg-pink-500/10 border-pink-500/20";
      case "MEDIUM":
        return "text-amber-theme bg-amber-500/10 border-amber-500/20";
      default:
        return "text-emerald-theme bg-emerald-500/10 border-emerald-500/20";
    }
  };

  const handleOpenModal = () => {
    setNameInput(profile?.fullName || "");
    setInstitutionInput(profile?.institution || "EduPilot Academy");
    setDegreeInput(profile?.degree || "B.Tech");
    setBranchInput(profile?.branch || profile?.course || "Computer Science & Engineering");
    setSemesterInput(profile?.semester ?? 1);
    setCurrentCgpaInput(profile?.currentCgpa ?? 8.0);
    setTargetCgpaInput(profile?.targetCgpa ?? 8.5);
    setErrorMsg("");
    setShowEditModal(true);
  };

  const handleSaveAcademicProfile = async (e: React.FormEvent) => {
    e.preventDefault();
    setErrorMsg("");

    // Full Validation Checks
    if (!nameInput.trim()) {
      setErrorMsg("Student Name is required.");
      return;
    }
    if (!institutionInput.trim()) {
      setErrorMsg("Institution name is required.");
      return;
    }
    if (!degreeInput.trim()) {
      setErrorMsg("Degree is required.");
      return;
    }
    if (!branchInput.trim()) {
      setErrorMsg("Branch / Specialization is required.");
      return;
    }
    if (semesterInput < 1 || semesterInput > 12) {
      setErrorMsg("Semester must be between 1 and 12.");
      return;
    }
    if (currentCgpaInput < 0.0 || currentCgpaInput > 10.0) {
      setErrorMsg("Current CGPA must be between 0.0 and 10.0.");
      return;
    }
    if (targetCgpaInput < 0.0 || targetCgpaInput > 10.0) {
      setErrorMsg("Target CGPA must be between 0.0 and 10.0.");
      return;
    }

    setSaving(true);
    const userId = profile?.id || localStorage.getItem("edupilot_user_id") || "";
    
    const updatePayload = {
      fullName: nameInput,
      institution: institutionInput,
      degree: degreeInput,
      branch: branchInput,
      course: branchInput,
      semester: Number(semesterInput),
      currentCgpa: Number(currentCgpaInput),
      targetCgpa: Number(targetCgpaInput)
    };

    try {
      await updateFullProfile(userId, updatePayload);
      setShowEditModal(false);
      window.location.reload(); // Refresh to update full state
    } catch (err: any) {
      setErrorMsg("Failed to save profile changes. Please try again.");
    } finally {
      setSaving(false);
    }
  };

  return (
    <div className="space-y-4">
      
      {/* Primary Academic Identity Banner */}
      <div className="glass-panel p-6 rounded-2xl border border-white/5 bg-gradient-to-r from-purple-900/10 via-transparent to-pink-900/10 space-y-6">
        <div className="flex flex-col md:flex-row md:items-center justify-between gap-4 border-b border-white/5 pb-4">
          <div className="flex items-center gap-3">
            <div className="h-12 w-12 rounded-xl bg-purple-500/10 border border-purple-500/20 flex items-center justify-center text-purple-theme shrink-0">
              <GraduationCap className="h-6 w-6" />
            </div>
            <div>
              <div className="flex items-center gap-2">
                <h3 className="text-base font-extrabold text-main-theme tracking-wide">{studentName}</h3>
                <span className="text-[10px] text-purple-theme font-bold bg-purple-500/10 border border-purple-500/20 px-2 py-0.5 rounded-md">
                  {degree} • Sem {semester}
                </span>
              </div>
              <p className="text-xs text-secondary-theme flex items-center gap-1.5 mt-0.5">
                <Building2 className="h-3.5 w-3.5 text-secondary-theme" />
                <span>{institution}</span>
                <span className="text-white/20">•</span>
                <span className="font-semibold text-main-theme">{branch}</span>
              </p>
            </div>
          </div>

          <button
            onClick={handleOpenModal}
            className="px-3.5 py-2 rounded-xl bg-white/5 hover:bg-white/10 text-main-theme border border-white/10 text-xs font-bold flex items-center gap-1.5 transition-all cursor-pointer self-start md:self-auto"
          >
            <Edit3 className="h-3.5 w-3.5 text-purple-theme" />
            <span>Edit Academic Profile</span>
          </button>
        </div>

        {/* 4 Metric Cards Grid */}
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
          {/* Current CGPA */}
          <div className="glass-panel p-4 rounded-xl border border-white/5 space-y-1">
            <div className="flex justify-between items-center text-[10px] text-secondary-theme uppercase font-extrabold tracking-wider">
              <span>Current CGPA</span>
              <Award className="h-4 w-4 text-purple-theme" />
            </div>
            <div className="flex items-baseline gap-1.5">
              <span className="text-2xl font-black text-main-theme">{currentCgpa.toFixed(1)}</span>
              <span className="text-[10px] text-secondary-theme">/ 10.0</span>
            </div>
            <p className="text-[10px] text-secondary-theme">Verified academic record</p>
          </div>

          {/* Target CGPA */}
          <div className="glass-panel p-4 rounded-xl border border-white/5 space-y-1">
            <div className="flex justify-between items-center text-[10px] text-secondary-theme uppercase font-extrabold tracking-wider">
              <span>Target CGPA</span>
              <Target className="h-4 w-4 text-emerald-theme" />
            </div>
            <div className="flex items-baseline gap-1.5">
              <span className="text-2xl font-black text-emerald-theme">{targetCgpa.toFixed(1)}</span>
              <span className="text-[10px] text-secondary-theme">Goal</span>
            </div>
            <p className="text-[10px] text-secondary-theme">Active target GPA target</p>
          </div>

          {/* Growth Index (SGI) */}
          <div className="glass-panel p-4 rounded-xl border border-white/5 space-y-1">
            <div className="flex justify-between items-center text-[10px] text-secondary-theme uppercase font-extrabold tracking-wider">
              <span>Growth Index (SGI)</span>
              <BrainCircuit className="h-4 w-4 text-cyan-theme" />
            </div>
            <div className="flex items-baseline gap-1.5">
              <span className="text-2xl font-black text-cyan-theme">{sgi.toFixed(1)}</span>
              <span className="text-[10px] text-secondary-theme">/ 10.0</span>
            </div>
            <p className="text-[10px] text-secondary-theme">Composite AI growth score</p>
          </div>

          {/* Academic Risk Level */}
          <div className="glass-panel p-4 rounded-xl border border-white/5 space-y-1">
            <div className="flex justify-between items-center text-[10px] text-secondary-theme uppercase font-extrabold tracking-wider">
              <span>Risk Profile</span>
              <Activity className="h-4 w-4 text-pink-theme" />
            </div>
            <div className="pt-0.5">
              <span className={`px-2.5 py-0.5 rounded-lg border text-xs font-black uppercase ${getRiskBadgeColor(riskLevel)}`}>
                {riskLevel} RISK
              </span>
            </div>
            <p className="text-[10px] text-secondary-theme mt-1">Tier: {predPerformanceLevel}</p>
          </div>
        </div>

        {/* Active Enrolled Subjects Tags */}
        <div className="space-y-2 pt-2 border-t border-white/5">
          <div className="flex items-center gap-2">
            <BookOpen className="h-4 w-4 text-purple-theme" />
            <span className="text-xs font-bold text-main-theme uppercase tracking-wider">Enrolled Subjects</span>
          </div>
          <div className="flex flex-wrap gap-2">
            {subjects.map((subj, idx) => (
              <span key={idx} className="px-3 py-1 rounded-lg bg-white/5 border border-white/10 text-xs font-semibold text-main-theme">
                {subj}
              </span>
            ))}
          </div>
        </div>
      </div>

      {/* Edit Academic Profile Modal */}
      {showEditModal && (
        <div className="fixed inset-0 bg-black/60 backdrop-blur-sm z-50 flex items-center justify-center p-4">
          <div className="glass-panel p-6 rounded-2xl border border-white/10 shadow-2xl w-full max-w-lg space-y-6">
            <div className="flex justify-between items-center border-b border-white/5 pb-3">
              <h3 className="text-base font-extrabold text-gradient-purple flex items-center gap-2">
                <GraduationCap className="h-5 w-5 text-purple-theme" />
                <span>Update Academic Profile & Identity</span>
              </h3>
              <button 
                onClick={() => setShowEditModal(false)} 
                className="text-secondary-theme hover:text-main-theme font-bold cursor-pointer"
              >
                <X className="h-5 w-5" />
              </button>
            </div>

            {errorMsg && (
              <div className="p-3 bg-pink-500/10 border border-pink-500/20 rounded-xl text-xs text-pink-300 font-bold">
                ⚠️ {errorMsg}
              </div>
            )}

            <form onSubmit={handleSaveAcademicProfile} className="space-y-4 text-xs">
              <div className="grid grid-cols-2 gap-4">
                <div className="col-span-2 space-y-1">
                  <label className="font-bold text-secondary-theme uppercase text-[10px]">Student Full Name *</label>
                  <input
                    type="text"
                    required
                    value={nameInput}
                    onChange={(e) => setNameInput(e.target.value)}
                    className="w-full p-2.5 rounded-lg glass-input text-xs"
                    placeholder="e.g. Alex Johnson"
                  />
                </div>

                <div className="col-span-2 space-y-1">
                  <label className="font-bold text-secondary-theme uppercase text-[10px]">Institution / University *</label>
                  <input
                    type="text"
                    required
                    value={institutionInput}
                    onChange={(e) => setInstitutionInput(e.target.value)}
                    className="w-full p-2.5 rounded-lg glass-input text-xs"
                    placeholder="e.g. EduPilot Academy of Technology"
                  />
                </div>

                <div className="space-y-1">
                  <label className="font-bold text-secondary-theme uppercase text-[10px]">Degree Program *</label>
                  <input
                    type="text"
                    required
                    value={degreeInput}
                    onChange={(e) => setDegreeInput(e.target.value)}
                    className="w-full p-2.5 rounded-lg glass-input text-xs"
                    placeholder="e.g. B.Tech"
                  />
                </div>

                <div className="space-y-1">
                  <label className="font-bold text-secondary-theme uppercase text-[10px]">Branch / Specialization *</label>
                  <input
                    type="text"
                    required
                    value={branchInput}
                    onChange={(e) => setBranchInput(e.target.value)}
                    className="w-full p-2.5 rounded-lg glass-input text-xs"
                    placeholder="e.g. Computer Science & Eng"
                  />
                </div>

                <div className="space-y-1">
                  <label className="font-bold text-secondary-theme uppercase text-[10px]">Current Semester (1-12) *</label>
                  <input
                    type="number"
                    min="1"
                    max="12"
                    required
                    value={semesterInput}
                    onChange={(e) => setSemesterInput(Number(e.target.value))}
                    className="w-full p-2.5 rounded-lg glass-input text-xs"
                  />
                </div>

                <div className="space-y-1">
                  <label className="font-bold text-secondary-theme uppercase text-[10px]">Current CGPA (0.0 - 10.0) *</label>
                  <input
                    type="number"
                    step="0.1"
                    min="0"
                    max="10"
                    required
                    value={currentCgpaInput}
                    onChange={(e) => setCurrentCgpaInput(Number(e.target.value))}
                    className="w-full p-2.5 rounded-lg glass-input text-xs"
                  />
                </div>

                <div className="col-span-2 space-y-1">
                  <label className="font-bold text-secondary-theme uppercase text-[10px]">Target CGPA Goal (0.0 - 10.0) *</label>
                  <input
                    type="number"
                    step="0.1"
                    min="0"
                    max="10"
                    required
                    value={targetCgpaInput}
                    onChange={(e) => setTargetCgpaInput(Number(e.target.value))}
                    className="w-full p-2.5 rounded-lg glass-input text-xs"
                  />
                </div>
              </div>

              <div className="pt-2 flex gap-3">
                <button
                  type="button"
                  onClick={() => setShowEditModal(false)}
                  className="w-1/2 py-2.5 bg-white/5 hover:bg-white/10 text-secondary-theme rounded-lg font-bold transition-all"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  disabled={saving}
                  className="w-1/2 py-2.5 bg-gradient-to-r from-purple-600 to-pink-600 hover:from-purple-500 hover:to-pink-500 text-white rounded-lg font-bold shadow-md shadow-purple-600/20 flex items-center justify-center gap-1.5 transition-all cursor-pointer"
                >
                  {saving ? (
                    <>
                      <RefreshCw className="h-4 w-4 animate-spin" />
                      <span>Saving...</span>
                    </>
                  ) : (
                    <>
                      <Check className="h-4 w-4" />
                      <span>Save & Recalculate AI</span>
                    </>
                  )}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

    </div>
  );
}
