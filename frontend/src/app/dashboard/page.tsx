import React, { useState, useEffect } from "react";
import { Link } from "react-router-dom";
import Layout from "../../components/Layout";
import { 
  AreaChart, Area, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer,
  BarChart, Bar, RadarChart, PolarGrid, PolarAngleAxis, PolarRadiusAxis, Radar
} from "recharts";
import { 
  Flame, Award, CheckCircle, BrainCircuit, Activity, Calendar, Compass, 
  Plus, Sparkles, UserCheck, Zap, Moon, Watch, Brain, X, HelpCircle, GraduationCap
} from "lucide-react";
import { StudentProfile, getStoredStudentProfile } from "../../services/mockData";
import { fetchProfile, postLifestyleLog, postQuestionnaire, getRecommendations, checkBackendConnection } from "../../services/api";

export default function StudentDashboard() {
  const [profile, setProfile] = useState<StudentProfile | null>(null);
  const [chartType, setChartType] = useState<"lifestyle" | "mastery">("lifestyle");
  
  // Daily Track inputs
  const [showLogModal, setShowLogModal] = useState(false);
  const [sleepInput, setSleepInput] = useState("7.5");
  const [studyInput, setStudyInput] = useState("240");
  const [stressInput, setStressInput] = useState("4");
  const [screenInput, setScreenInput] = useState("4.0");
  const [exerciseInput, setExerciseInput] = useState("30");
  const [productivityInput, setProductivityInput] = useState("8");



  const [aiInsights, setAiInsights] = useState<string[]>([]);
  const [predPerformanceLevel, setPredPerformanceLevel] = useState("Medium");
  const [isBackendConnected, setIsBackendConnected] = useState(false);

  useEffect(() => {
    async function loadData() {
      const activeUserId = typeof window !== "undefined" ? (localStorage.getItem("edupilot_user_id") || "") : "";
      const activeProfile = await fetchProfile(activeUserId);
      setProfile(activeProfile);

      const conn = await checkBackendConnection();
      setIsBackendConnected(conn);

      const recsResult = await getRecommendations(activeProfile?.id || activeUserId);
      if (recsResult && recsResult.insights) {
        setAiInsights(recsResult.insights);
        if (recsResult.predicted_performance_level) {
          setPredPerformanceLevel(recsResult.predicted_performance_level);
        }
      }
    }
    loadData();
  }, []);

  if (!profile) {
    return (
      <div className="min-h-screen flex items-center justify-center bg-[#05060b]">
        <div className="animate-spin rounded-full h-10 w-10 border-b-2 border-purple-500" />
      </div>
    );
  }

  // Handle new daily log submission
  const handleLogSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    
    const newLog = {
      date: ["Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"][new Date().getDay() - 1] || "Today",
      sleepHours: Number(sleepInput),
      screenTimeHours: Number(screenInput),
      stressLevel: Number(stressInput),
      exerciseMinutes: Number(exerciseInput),
      studyMinutes: Number(studyInput),
      productivityRating: Number(productivityInput),
      attendanceRate: 98
    };

    const updated = await postLifestyleLog(profile?.id || "", newLog);
    setProfile(updated);
    setShowLogModal(false);

    // Refresh recommendations
    const recsResult = await getRecommendations(profile?.id || "");
    if (recsResult && recsResult.insights) {
      setAiInsights(recsResult.insights);
    }
  };

  // Convert conceptMastery to chart data format with safe defensive check
  const masteryData = Object.entries(profile?.conceptMastery || {}).map(([subj, val]) => ({
    subject: subj.length > 15 ? subj.substring(0, 15) + "..." : subj,
    Mastery: Math.round(val)
  }));

  // Dynamic real data binding (no fake mock fallbacks)
  const lifestyleHistoryData = profile?.lifestyleHistory || [];
  const badgesList = profile?.badges || [];

  const strongConceptsList = Object.entries(profile?.strongConcepts || {}).flatMap(([subject, list]) => 
    (list || []).map((c) => ({ subject, concept: c }))
  );
  const weakConceptsList = Object.entries(profile?.weakConcepts || {}).flatMap(([subject, list]) => 
    (list || []).map((c) => ({ subject, concept: c }))
  );

  const getFirstName = () => {
    // read edupilot_user
    let fullName = "";
    if (typeof window !== "undefined") {
      // parse safely
      try {
        const userStr = localStorage.getItem("edupilot_user");
        if (userStr) {
          const userObj = JSON.parse(userStr);
          if (userObj && typeof userObj.fullName === "string" && userObj.fullName.trim() !== "") {
            fullName = userObj.fullName;
          }
        }
      } catch (err) {
        console.warn("Failed to parse edupilot_user from localStorage:", err);
      }
    }

    // fallback to profile.fullName
    if (!fullName && profile && typeof profile.fullName === "string" && profile.fullName.trim() !== "") {
      fullName = profile.fullName;
    }

    // fallback to Student
    if (!fullName) {
      fullName = "Student";
    }

    // return first name
    return fullName.split(" ")[0] || fullName;
  };

  const firstName = getFirstName();

  return (
    <Layout>
      <div className="space-y-8 animate-fade-in">
        
        {/* Welcome Section & Daily Tracker Trigger */}
        <div className="flex flex-col md:flex-row md:items-center justify-between gap-4">
          <div>
            <h1 className="text-3xl font-extrabold text-main-theme flex items-center gap-2.5">
              <span>Welcome back, {firstName}</span>
              <Sparkles className="h-6 w-6 text-purple-theme animate-pulse" />
            </h1>
            <p className="text-secondary-theme text-sm mt-1">On track for {profile?.course || "Computer Science"} | Semester {profile?.semester ?? 1}</p>
          </div>

          <div className="flex items-center gap-2">
            <button
              onClick={() => setShowLogModal(true)}
              className="flex items-center gap-2 px-5 py-3 rounded-xl bg-purple-600 hover:bg-purple-500 text-white font-bold text-xs shadow-lg shadow-purple-500/20 cursor-pointer transition-all duration-200"
            >
              <Plus className="h-4 w-4" />
              <span>Log Habits</span>
            </button>
          </div>
        </div>

        {/* Dashboard Grid 1: AI Analytics Insights (SGI, Risk, Streaks) */}
        <div className="grid grid-cols-1 md:grid-cols-4 gap-6">
          
          {/* Student Growth Index Ring */}
          <div className="glass-panel p-6 rounded-2xl border border-white/5 flex flex-col justify-between relative overflow-hidden">
            <div className="absolute top-0 right-0 h-24 w-24 bg-purple-500/5 rounded-full blur-xl pointer-events-none" />
            <div>
              <div className="flex items-center justify-between mb-3">
                <span className="text-[10px] text-secondary-theme uppercase font-extrabold tracking-wider">Growth Index (SGI)</span>
                <BrainCircuit className="h-5 w-5 text-purple-theme" />
              </div>
              <div className="flex items-baseline gap-2">
                <span className="text-4xl font-black text-purple-theme">
                  {profile?.studentGrowthIndex ?? 0.0}
                </span>
                <span className="text-xs text-secondary-theme">/ 10.0</span>
              </div>
              <p className="text-[10px] text-secondary-theme mt-2">Aggregated from academic, habit, and quiz scores.</p>
            </div>
            
            <div className="mt-4 pt-3 border-t border-white/5 flex items-center justify-between text-xs text-secondary-theme font-bold">
              <span>Goal CGPA: {profile?.targetCgpa ?? 0.0}</span>
              <span className="text-emerald-theme font-semibold">Pred GPA: {profile?.predictedCgpa ?? 0.0}</span>
            </div>
          </div>

          {/* Academic Risk Level Card */}
          <div className="glass-panel p-6 rounded-2xl border border-white/5 flex flex-col justify-between relative overflow-hidden">
            <div className="absolute top-0 right-0 h-24 w-24 bg-cyan-500/5 rounded-full blur-xl pointer-events-none" />
            <div>
              <div className="flex items-center justify-between mb-3">
                <span className="text-[10px] text-secondary-theme uppercase font-extrabold tracking-wider">Risk Profile</span>
                <Activity className="h-5 w-5 text-cyan-theme" />
              </div>
              <div className="flex items-center gap-2">
                <span className={`text-2xl font-black ${(profile?.academicRiskLevel || "LOW") === "LOW" ? "text-emerald-theme" : (profile?.academicRiskLevel || "LOW") === "MEDIUM" ? "text-amber-theme" : "text-pink-theme"}`}>
                  {profile?.academicRiskLevel || "LOW"} RISK
                </span>
              </div>
              <p className="text-[10px] text-secondary-theme mt-2">Class attendance, test performance, and workload predictions.</p>
            </div>

            <div className="mt-4 pt-3 border-t border-white/5 text-xs text-secondary-theme flex justify-between font-bold">
              <span>Performance Level:</span>
              <span className="text-cyan-theme font-bold">{predPerformanceLevel}</span>
            </div>
          </div>

          {/* Study Preferences & Productivity */}
          <div className="glass-panel p-6 rounded-2xl border border-white/5 flex flex-col justify-between">
            <div>
              <div className="flex items-center justify-between mb-3">
                <span className="text-[10px] text-secondary-theme uppercase font-extrabold tracking-wider">Productivity Index</span>
                <Zap className="h-5 w-5 text-amber-theme" />
              </div>
              <div className="flex items-baseline gap-2">
                <span className="text-3xl font-extrabold text-amber-theme">{profile?.productivityScore ?? 0}</span>
                <span className="text-xs text-secondary-theme">/ 100</span>
              </div>
              <p className="text-[10px] text-secondary-theme mt-2">Focus time duration and self-rated active intervals.</p>
            </div>

            <div className="mt-4 pt-3 border-t border-white/5 text-xs text-secondary-theme flex justify-between font-bold">
              <span>Lifestyle Score:</span>
              <span className="text-amber-theme font-semibold">{profile?.lifestyleScore ?? 0}%</span>
            </div>
          </div>

          {/* Onboarding Goals Tracker */}
          <div className="glass-panel p-6 rounded-2xl border border-white/5 flex flex-col justify-between">
            <div>
              <div className="flex items-center justify-between mb-3">
                <span className="text-[10px] text-secondary-theme uppercase font-extrabold tracking-wider">Target Focus</span>
                <Compass className="h-5 w-5 text-pink-theme" />
              </div>
              <div className="space-y-1">
                <span className="text-xs font-bold text-main-theme truncate block">
                  {(profile?.careerGoals && profile.careerGoals[0]) || "General Onboarding"}
                </span>
                <span className="text-[10px] text-secondary-theme block">Active milestone</span>
              </div>
            </div>

            <div className="mt-4 pt-3 border-t border-white/5 text-xs text-secondary-theme flex justify-between font-bold">
              <span>Quiz Completes:</span>
              <span className="text-pink-theme font-bold">{profile?.completedQuizzesCount ?? 0}</span>
            </div>
          </div>

        </div>

        {/* Dashboard Grid 2: Charts and Streaks */}
        <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
          
          {/* Left Chart (2/3 width) */}
          <div className="glass-panel p-6 rounded-2xl border border-white/5 lg:col-span-2 space-y-4">
            <div className="flex justify-between items-center border-b border-white/5 pb-3">
              <div className="flex items-center gap-2">
                <Activity className="h-5 w-5 text-purple-theme" />
                <h3 className="text-sm font-extrabold tracking-wide text-main-theme">Interactive Analytics</h3>
              </div>

              <div className="flex gap-2">
                <button
                  onClick={() => setChartType("lifestyle")}
                  className={`px-3 py-1.5 rounded-lg text-xs font-semibold ${chartType === "lifestyle" ? "bg-purple-600 text-white" : "bg-white/10 text-secondary-theme hover:text-main-theme"}`}
                >
                  Lifestyle Logs
                </button>
                <button
                  onClick={() => setChartType("mastery")}
                  className={`px-3 py-1.5 rounded-lg text-xs font-semibold ${chartType === "mastery" ? "bg-purple-600 text-white" : "bg-white/10 text-secondary-theme hover:text-main-theme"}`}
                >
                  Subject Mastery
                </button>
              </div>
            </div>

            {/* Render Recharts */}
            <div className="h-64 w-full text-xs flex items-center justify-center">
              {chartType === "lifestyle" ? (
                lifestyleHistoryData.length > 0 ? (
                  <ResponsiveContainer width="100%" height="100%">
                    <AreaChart data={lifestyleHistoryData}>
                      <defs>
                        <linearGradient id="colorSleep" x1="0" y1="0" x2="0" y2="1">
                          <stop offset="5%" stopColor="var(--accent-purple)" stopOpacity={0.4}/>
                          <stop offset="95%" stopColor="var(--accent-purple)" stopOpacity={0}/>
                        </linearGradient>
                        <linearGradient id="colorStudy" x1="0" y1="0" x2="0" y2="1">
                          <stop offset="5%" stopColor="var(--accent-cyan)" stopOpacity={0.4}/>
                          <stop offset="95%" stopColor="var(--accent-cyan)" stopOpacity={0}/>
                        </linearGradient>
                      </defs>
                      <CartesianGrid strokeDasharray="3 3" stroke="var(--glass-border)" />
                      <XAxis dataKey="date" stroke="var(--text-secondary)" />
                      <YAxis stroke="var(--text-secondary)" />
                      <Tooltip contentStyle={{ backgroundColor: "var(--glass-bg)", color: "var(--text-main)", border: "1px solid var(--glass-border)", borderRadius: "8px" }} />
                      <Area type="monotone" dataKey="sleepHours" name="Sleep (Hours)" stroke="var(--accent-purple)" fillOpacity={1} fill="url(#colorSleep)" />
                      <Area type="monotone" dataKey="productivityRating" name="Productivity (Scale)" stroke="var(--accent-cyan)" fillOpacity={1} fill="url(#colorStudy)" />
                    </AreaChart>
                  </ResponsiveContainer>
                ) : (
                  <div className="flex flex-col items-center justify-center p-6 text-center space-y-2">
                    <Activity className="h-8 w-8 text-purple-theme opacity-50" />
                    <h4 className="font-bold text-xs text-main-theme">No Habit Logs Recorded Yet</h4>
                    <p className="text-[10px] text-secondary-theme max-w-xs">Click 'Log Habits' above to record your daily study time, sleep, and focus metrics.</p>
                    <button
                      onClick={() => setShowLogModal(true)}
                      className="mt-2 px-3 py-1.5 rounded-lg bg-purple-600/20 hover:bg-purple-600/30 text-purple-theme text-[10px] font-bold border border-purple-500/30"
                    >
                      Log First Habit
                    </button>
                  </div>
                )
              ) : (
                <ResponsiveContainer width="100%" height="100%">
                  <BarChart data={masteryData}>
                    <CartesianGrid strokeDasharray="3 3" stroke="var(--glass-border)" />
                    <XAxis dataKey="subject" stroke="var(--text-secondary)" />
                    <YAxis stroke="var(--text-secondary)" />
                    <Tooltip contentStyle={{ backgroundColor: "var(--glass-bg)", color: "var(--text-main)", border: "1px solid var(--glass-border)", borderRadius: "8px" }} />
                    <Bar dataKey="Mastery" fill="var(--accent-pink)" radius={[4, 4, 0, 0]} />
                  </BarChart>
                </ResponsiveContainer>
              )}
            </div>
          </div>

          {/* Right Pillar: Daily AI Recommendations */}
          <div className="glass-panel p-6 rounded-2xl border border-white/5 space-y-4">
            <div className="flex items-center gap-2 border-b border-white/5 pb-3">
              <Brain className="h-5 w-5 text-cyan-theme" />
              <h3 className="text-sm font-extrabold tracking-wide text-main-theme">Daily AI Engine Guides</h3>
            </div>

            <div className="space-y-3 max-h-[250px] overflow-y-auto pr-1">
              {(aiInsights && aiInsights.length > 0) ? (
                aiInsights.map((insight, idx) => (
                  <div key={idx} className="p-3.5 rounded-xl bg-white/10 border border-white/5 hover:border-purple-500/30 transition-all flex items-start gap-3">
                    <div className="h-6 w-6 rounded bg-purple-500/10 flex items-center justify-center mt-0.5 shrink-0">
                      <Sparkles className="h-3.5 w-3.5 text-purple-theme" />
                    </div>
                    <p className="text-xs text-main-theme leading-relaxed">{insight}</p>
                  </div>
                ))
              ) : (
                <div className="flex flex-col items-center justify-center p-6 text-center space-y-2 bg-white/3 rounded-xl border border-white/5">
                  <Sparkles className="h-6 w-6 text-cyan-theme opacity-50 animate-pulse" />
                  <h4 className="font-bold text-xs text-main-theme">AI Study Copilot Ready</h4>
                  <p className="text-[10px] text-secondary-theme leading-relaxed">Your AI Copilot is analyzing your learning habits. Take an adaptive quiz or log habits to generate personalized study recommendations.</p>
                </div>
              )}
            </div>
          </div>

        </div>

        {/* Dashboard Grid 3: Mastery Matrix & Gamification */}
        <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
          
          {/* Weak / Strong Concepts lists */}
          <div className="glass-panel p-6 rounded-2xl border border-white/5 space-y-4 md:col-span-2">
            <h3 className="text-sm font-extrabold tracking-wide border-b border-white/5 pb-3 text-main-theme">
              Conceptual Mastery Mapping
            </h3>

            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              {/* Strong Concepts */}
              <div className="space-y-2.5">
                <div className="text-xs font-bold text-emerald-theme uppercase tracking-wider flex items-center gap-1.5">
                  <CheckCircle className="h-4 w-4" />
                  <span>Strong Concepts</span>
                </div>
                <div className="space-y-2">
                  {strongConceptsList.length > 0 ? (
                    strongConceptsList.map((item, idx) => (
                      <div key={`${item.subject}-${item.concept}-${idx}`} className="p-2.5 bg-emerald-500/5 border border-emerald-500/15 rounded-lg text-xs flex justify-between">
                        <span className="font-semibold text-main-theme">{item.concept}</span>
                        <span className="text-[10px] text-secondary-theme">{item.subject.length > 15 ? item.subject.substring(0, 15) + "..." : item.subject}</span>
                      </div>
                    ))
                  ) : (
                    <div className="p-4 bg-white/3 rounded-xl border border-white/5 text-center text-[10px] text-secondary-theme">
                      No strong concepts recorded yet. Answer quiz questions correctly to build concept mastery.
                    </div>
                  )}
                </div>
              </div>

              {/* Weak Concepts */}
              <div className="space-y-2.5">
                <div className="text-xs font-bold text-pink-theme uppercase tracking-wider flex items-center gap-1.5">
                  <Activity className="h-4 w-4 animate-pulse" />
                  <span>Weak Concepts (Need Review)</span>
                </div>
                <div className="space-y-2">
                  {weakConceptsList.length > 0 ? (
                    weakConceptsList.map((item, idx) => (
                      <div key={`${item.subject}-${item.concept}-${idx}`} className="p-2.5 bg-pink-500/5 border border-pink-500/15 rounded-lg text-xs flex justify-between items-center">
                        <div>
                          <span className="font-semibold text-main-theme block">{item.concept}</span>
                          <span className="text-[10px] text-secondary-theme block">{item.subject.length > 15 ? item.subject.substring(0, 15) + "..." : item.subject}</span>
                        </div>
                        <Link 
                          to="/dashboard/quizzes" 
                          className="px-2 py-1 bg-pink-500/20 hover:bg-pink-500/30 text-pink-300 text-[10px] font-bold rounded"
                        >
                          Practice
                        </Link>
                      </div>
                    ))
                  ) : (
                    <div className="p-4 bg-white/3 rounded-xl border border-white/5 text-center text-[10px] text-secondary-theme space-y-2">
                      <p>No weak concepts identified yet!</p>
                      <Link 
                        to="/dashboard/quizzes" 
                        className="inline-block px-3 py-1.5 bg-purple-600/20 hover:bg-purple-600/30 text-purple-theme text-[10px] font-bold rounded border border-purple-500/30"
                      >
                        Take Adaptive Quiz
                      </Link>
                    </div>
                  )}
                </div>
              </div>
            </div>
          </div>

          {/* Gamification Badge Hub */}
          <div className="glass-panel p-6 rounded-2xl border border-white/5 space-y-4">
            <div className="flex items-center gap-2 border-b border-white/5 pb-3">
              <Award className="h-5 w-5 text-amber-theme" />
              <h3 className="text-sm font-extrabold tracking-wide text-main-theme">Unlocked Badges</h3>
            </div>

            <div className="grid grid-cols-1 gap-3">
              {badgesList.length > 0 ? (
                badgesList.map((b) => (
                  <div key={b.name} className="flex items-center gap-3 p-2 bg-white/10 rounded-xl border border-white/5">
                    <div className="text-2xl h-10 w-10 bg-amber-500/10 rounded-lg flex items-center justify-center">
                      {b.icon}
                    </div>
                    <div>
                      <h4 className="text-xs font-bold text-main-theme">{b.name}</h4>
                      <p className="text-[10px] text-secondary-theme mt-0.5">{b.description}</p>
                    </div>
                  </div>
                ))
              ) : (
                <div className="p-6 bg-white/3 rounded-xl border border-white/5 text-center text-[10px] text-secondary-theme space-y-2">
                  <Award className="h-8 w-8 text-amber-theme opacity-40 mx-auto" />
                  <p className="font-bold text-xs text-main-theme">No Badges Earned Yet</p>
                  <p>Complete daily habit logs and adaptive quizzes to unlock your first achievement badge!</p>
                </div>
              )}
            </div>
          </div>

        </div>

      </div>

      {/* Habit Logger Modal Overlay */}
      {showLogModal && (
        <div className="fixed inset-0 bg-black/60 backdrop-blur-sm z-50 flex items-center justify-center p-4">
          <div className="glass-panel p-6 rounded-2xl border border-white/10 shadow-2xl w-full max-w-md space-y-6">
            <div className="flex justify-between items-center border-b border-white/5 pb-3">
              <h3 className="text-base font-extrabold text-gradient-purple flex items-center gap-2">
                <Plus className="h-5 w-5 text-purple-theme" />
                <span>Log Daily Lifestyle Metrics</span>
              </h3>
              <button 
                onClick={() => setShowLogModal(false)} 
                className="text-secondary-theme hover:text-main-theme font-bold cursor-pointer"
              >
                <X className="h-5 w-5" />
              </button>
            </div>

            <form onSubmit={handleLogSubmit} className="space-y-4">
              <div className="grid grid-cols-2 gap-4">
                <div className="space-y-1">
                  <label className="text-[10px] font-bold text-secondary-theme uppercase">Sleep Hours</label>
                  <input
                    type="number"
                    step="0.1"
                    required
                    value={sleepInput}
                    onChange={(e) => setSleepInput(e.target.value)}
                    className="w-full p-2.5 rounded-lg glass-input text-xs"
                  />
                </div>
                <div className="space-y-1">
                  <label className="text-[10px] font-bold text-secondary-theme uppercase">Study Minutes</label>
                  <input
                    type="number"
                    required
                    value={studyInput}
                    onChange={(e) => setStudyInput(e.target.value)}
                    className="w-full p-2.5 rounded-lg glass-input text-xs"
                  />
                </div>
                <div className="space-y-1">
                  <label className="text-[10px] font-bold text-secondary-theme uppercase">Stress Level (1-10)</label>
                  <input
                    type="number"
                    min="1"
                    max="10"
                    required
                    value={stressInput}
                    onChange={(e) => setStressInput(e.target.value)}
                    className="w-full p-2.5 rounded-lg glass-input text-xs"
                  />
                </div>
                <div className="space-y-1">
                  <label className="text-[10px] font-bold text-secondary-theme uppercase">Screen Time (hrs)</label>
                  <input
                    type="number"
                    step="0.1"
                    required
                    value={screenInput}
                    onChange={(e) => setScreenInput(e.target.value)}
                    className="w-full p-2.5 rounded-lg glass-input text-xs"
                  />
                </div>
                <div className="space-y-1">
                  <label className="text-[10px] font-bold text-secondary-theme uppercase">Exercise (mins)</label>
                  <input
                    type="number"
                    required
                    value={exerciseInput}
                    onChange={(e) => setExerciseInput(e.target.value)}
                    className="w-full p-2.5 rounded-lg glass-input text-xs"
                  />
                </div>
                <div className="space-y-1">
                  <label className="text-[10px] font-bold text-secondary-theme uppercase">Productivity (1-10)</label>
                  <input
                    type="number"
                    min="1"
                    max="10"
                    required
                    value={productivityInput}
                    onChange={(e) => setProductivityInput(e.target.value)}
                    className="w-full p-2.5 rounded-lg glass-input text-xs"
                  />
                </div>
              </div>

              <div className="p-3 bg-purple-500/5 border border-purple-500/10 rounded-xl text-[10px] text-secondary-theme">
                ⚡ Submit details to immediately run diagnostic models and adjust active recommender flows.
              </div>

              <button
                type="submit"
                className="w-full py-2.5 bg-gradient-to-r from-purple-600 to-pink-600 hover:from-purple-500 hover:to-pink-500 text-white rounded-lg text-xs font-bold transition-all shadow-md shadow-purple-600/15"
              >
                Log Metrics & Run Predictive Engines
              </button>
            </form>
          </div>
        </div>
      )}

    </Layout>
  );
}
