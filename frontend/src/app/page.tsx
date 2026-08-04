import React, { useState, useEffect } from "react";
import { useNavigate } from "react-router-dom";
import { 
  BrainCircuit, 
  Sparkles, 
  ArrowRight, 
  User, 
  Lock, 
  Shield, 
  Activity, 
  Flame, 
  Target,
  Eye,
  EyeOff
} from "lucide-react";
import { registerUser, loginUser, onboardStudent } from "../services/api";

export default function Home() {
  const navigate = useNavigate();
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [role, setRole] = useState<"STUDENT" | "FACULTY" | "ADMIN">("STUDENT");
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");

  // Registration Mode and Fields
  const [isRegister, setIsRegister] = useState(false);
  const [fullName, setFullName] = useState("");
  const [course, setCourse] = useState("Computer Science");
  const [semester, setSemester] = useState("1");
  const [targetCgpa, setTargetCgpa] = useState("8.5");
  const [department, setDepartment] = useState("Computer Science Department");
  const [designation, setDesignation] = useState("Assistant Professor");
  const [showPassword, setShowPassword] = useState(false);

  // Sync with active theme configuration
  useEffect(() => {
    const storedTheme = localStorage.getItem("edupilot_theme") as "dark" | "light" | null;
    if (storedTheme) {
      if (storedTheme === "light") {
        document.documentElement.classList.add("light");
      } else {
        document.documentElement.classList.remove("light");
      }
    }
  }, []);

  const handleLogin = async (e: React.FormEvent) => {
    e.preventDefault();
    setLoading(true);
    setError("");

    if (isRegister) {
      // REGISTRATION MODE: POST /api/auth/register
      if (!fullName || !email || !password) {
        setError("Please fill in Name, Email, and Password.");
        setLoading(false);
        return;
      }

      const res = await registerUser({
        email,
        password,
        fullName,
        role
      });

      if (!res.ok) {
        setError(res.message || "Registration failed.");
        setLoading(false);
        return;
      }

      // Automatically log in user after successful registration
      const loginRes = await loginUser({ email, password });
      setLoading(false);

      if (loginRes.ok && loginRes.token) {
        localStorage.setItem("edupilot_token", loginRes.token);
        localStorage.setItem("edupilot_user_id", loginRes.userId || res.userId || "");
        localStorage.setItem("edupilot_role", loginRes.role || role);
        localStorage.setItem("edupilot_user", JSON.stringify({
          userId: loginRes.userId || res.userId,
          email: loginRes.email || email,
          fullName: loginRes.fullName || fullName,
          role: loginRes.role || role
        }));
      }

      if (role === "STUDENT") {
        const studentUserId = res.userId || loginRes.userId || "";
        const newStudentProfile = {
          userId: studentUserId,
          course: course,
          semester: parseInt(semester) || 1,
          subjects: ["Data Structures & Algorithms", "Database Management Systems", "Artificial Intelligence"],
          careerGoals: ["Software Engineer at Tech Firm"],
          preferredStudyHoursPerDay: 4,
          targetCgpa: parseFloat(targetCgpa) || 8.5
        };
        await onboardStudent(newStudentProfile);
        navigate("/dashboard");
      } else if (role === "FACULTY") {
        navigate("/faculty");
      } else {
        navigate("/admin");
      }

    } else {
      // LOGIN MODE: POST /api/auth/login
      const res = await loginUser({ email, password });
      setLoading(false);

      if (!res.ok) {
        setError(res.message || "Invalid email or password.");
        return;
      }

      // Store JWT token securely and user session info
      if (res.token) {
        localStorage.setItem("edupilot_token", res.token);
      }
      if (res.userId) {
        localStorage.setItem("edupilot_user_id", res.userId);
      }
      if (res.role) {
        localStorage.setItem("edupilot_role", res.role);
      }
      localStorage.setItem("edupilot_user", JSON.stringify({
        userId: res.userId,
        email: res.email,
        fullName: res.fullName,
        role: res.role
      }));

      // Route based on authenticated role returned by Spring Boot
      const targetRole = res.role || role;
      if (targetRole === "STUDENT") {
        navigate("/dashboard");
      } else if (targetRole === "FACULTY") {
        navigate("/faculty");
      } else if (targetRole === "ADMIN") {
        navigate("/admin");
      }
    }
  };

  return (
    <div className="relative min-h-screen flex flex-col items-center justify-center px-4 md:px-8 py-12 bg-[var(--background-gradient)] overflow-hidden transition-colors duration-300">
      
      {/* Background Neon Glow Rings */}
      <div className="absolute top-[-10%] left-[-20%] w-[60%] h-[60%] rounded-full bg-purple-900/10 blur-[120px] pointer-events-none" />
      <div className="absolute bottom-[-10%] right-[-20%] w-[60%] h-[60%] rounded-full bg-cyan-900/10 blur-[120px] pointer-events-none" />

      {/* Main Container */}
      <div className="w-full max-w-6xl grid grid-cols-1 lg:grid-cols-2 gap-12 items-center z-10">
        
        {/* Left Side: Pitch and Architecture Showcase */}
        <div className="space-y-8 text-left">
          <div className="inline-flex items-center gap-2 px-3 py-1.5 rounded-full bg-purple-500/10 text-purple-theme border border-purple-500/20 text-xs font-semibold">
            <Sparkles className="h-4 w-4" />
            <span>EduPilot AI - Autonomous Growth OS</span>
          </div>

          <h1 className="text-4xl md:text-6xl font-black tracking-tight leading-none">
            Empowering <br />
            <span className="text-gradient-purple">Holistic Student</span> <br />
            <span className="text-gradient-cyan">Development.</span>
          </h1>

          <p className="text-secondary-theme text-base md:text-lg max-w-lg leading-relaxed">
            EduPilot AI replaces static dashboards with a comprehensive growth system. 
            We merge academic performance, diagnostic concept maps, lifestyle logs, and adaptive quizzes into a unified <strong>Student Growth Index (SGI)</strong>.
          </p>

          {/* Core Feature Pillars */}
          <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
            <div className="glass-panel p-4 rounded-xl border border-white/5 space-y-2">
              <Activity className="h-5 w-5 text-cyan-theme" />
              <h3 className="text-sm font-bold">Lifestyle Analytics</h3>
              <p className="text-xs text-secondary-theme">Sleep, stress levels, and focus tracking correlation.</p>
            </div>
            <div className="glass-panel p-4 rounded-xl border border-white/5 space-y-2">
              <Flame className="h-5 w-5 text-amber-500" />
              <h3 className="text-sm font-bold">Adaptive Quizzes</h3>
              <p className="text-xs text-secondary-theme">Dynamic difficulty scaling for conceptual growth.</p>
            </div>
            <div className="glass-panel p-4 rounded-xl border border-white/5 space-y-2">
              <Target className="h-5 w-5 text-emerald-theme" />
              <h3 className="text-sm font-bold">Predictive SGI</h3>
              <p className="text-xs text-secondary-theme">AI predictions of CGPA outputs based on current habits.</p>
            </div>
          </div>
        </div>

        {/* Right Side: Login Panel */}
        <div className="w-full max-w-md mx-auto">
          <div className="glass-panel p-8 rounded-2xl border border-white/10 shadow-2xl relative">
            
            {/* Header logo */}
            <div className="flex items-center gap-3 mb-6 justify-center">
              <BrainCircuit className="h-9 w-9 text-purple-theme" />
              <span className="font-extrabold text-xl tracking-wider text-gradient-purple">EduPilot AI</span>
            </div>

            {/* Auth Mode Toggle Tabs */}
            <div className="flex justify-center border-b border-white/5 pb-3 mb-5 gap-6 text-sm font-bold">
              <button
                type="button"
                onClick={() => {
                  setIsRegister(false);
                  setError("");
                }}
                className={`pb-1 transition-all cursor-pointer ${!isRegister ? "text-purple-theme border-b-2 border-[var(--accent-purple)]" : "text-secondary-theme"}`}
              >
                Sign In
              </button>
              <button
                type="button"
                onClick={() => {
                  setIsRegister(true);
                  setError("");
                  setFullName("");
                }}
                className={`pb-1 transition-all cursor-pointer ${isRegister ? "text-purple-theme border-b-2 border-[var(--accent-purple)]" : "text-secondary-theme"}`}
              >
                Register
              </button>
            </div>

            <form onSubmit={handleLogin} className="space-y-4">
              {/* Role Select Buttons */}
              <div className="space-y-1.5">
                <label className="text-[10px] font-bold text-secondary-theme uppercase tracking-wider block text-center">
                  {role === "ADMIN" ? "System Admin Console Active" : "Select Portal Role"}
                </label>
                <div className="grid grid-cols-2 gap-2 p-1 bg-white/10 rounded-xl border border-white/10 text-xs font-semibold">
                  {(["STUDENT", "FACULTY"] as const).map((r) => (
                    <button
                      type="button"
                      key={r}
                      onClick={() => {
                        setRole(r);
                      }}
                      className={`py-2 rounded-lg text-center transition-all ${role === r ? "bg-[var(--accent-purple)] text-white shadow-lg shadow-[var(--glass-shadow)]" : "text-secondary-theme hover:text-main-theme"}`}
                    >
                      {r}
                    </button>
                  ))}
                </div>
              </div>

              {/* Full Name Input (Register Mode Only) */}
              {isRegister && (
                <div className="space-y-1">
                  <label className="text-xs font-bold text-secondary-theme uppercase tracking-wider">Full Name</label>
                  <div className="relative">
                    <User className="absolute left-3 top-3 h-4 w-4 text-secondary-theme" />
                    <input
                      type="text"
                      required
                      value={fullName}
                      onChange={(e) => setFullName(e.target.value)}
                      className="w-full py-2.5 pl-10 pr-4 rounded-xl glass-input text-xs"
                      placeholder="e.g. John Doe"
                    />
                  </div>
                </div>
              )}

              {/* Email Input */}
              <div className="space-y-1">
                <label className="text-xs font-bold text-secondary-theme uppercase tracking-wider">Email Address</label>
                <div className="relative">
                  <User className="absolute left-3 top-3 h-4 w-4 text-secondary-theme" />
                  <input
                    type="email"
                    required
                    value={email}
                    onChange={(e) => setEmail(e.target.value)}
                    className="w-full py-2.5 pl-10 pr-4 rounded-xl glass-input text-xs"
                    placeholder="name@university.edu"
                  />
                </div>
              </div>

              {/* Password Input */}
              <div className="space-y-1">
                <label className="text-xs font-bold text-secondary-theme uppercase tracking-wider">Password</label>
                <div className="relative">
                  <Lock className="absolute left-3 top-3 h-4 w-4 text-secondary-theme" />
                  <input
                    type={showPassword ? "text" : "password"}
                    required
                    value={password}
                    onChange={(e) => setPassword(e.target.value)}
                    className="w-full py-2.5 pl-10 pr-10 rounded-xl glass-input text-xs"
                    placeholder="********"
                  />
                  <button
                    type="button"
                    onClick={() => setShowPassword(!showPassword)}
                    className="absolute right-3 top-3 text-secondary-theme hover:text-purple-theme transition-all cursor-pointer bg-transparent border-0"
                    title={showPassword ? "Hide Password" : "Show Password"}
                  >
                    {showPassword ? <EyeOff className="h-4 w-4" /> : <Eye className="h-4 w-4" />}
                  </button>
                </div>
              </div>

              {/* STUDENT Specific Registration Inputs */}
              {isRegister && role === "STUDENT" && (
                <>
                  <div className="grid grid-cols-2 gap-4">
                    <div className="space-y-1">
                      <label className="text-xs font-bold text-secondary-theme uppercase tracking-wider">Course / Major</label>
                      <input
                        type="text"
                        required
                        value={course}
                        onChange={(e) => setCourse(e.target.value)}
                        className="w-full py-2.5 px-3 rounded-xl glass-input text-xs"
                        placeholder="Computer Science"
                      />
                    </div>
                    <div className="space-y-1">
                      <label className="text-xs font-bold text-secondary-theme uppercase tracking-wider">Semester</label>
                      <select
                        value={semester}
                        onChange={(e) => setSemester(e.target.value)}
                        className="w-full py-2.5 px-3 rounded-xl glass-input text-xs"
                      >
                        {[1, 2, 3, 4, 5, 6, 7, 8].map(s => (
                          <option key={s} value={s}>Semester {s}</option>
                        ))}
                      </select>
                    </div>
                  </div>

                  <div className="space-y-1">
                    <label className="text-xs font-bold text-secondary-theme uppercase tracking-wider">Target CGPA (out of 10.0)</label>
                    <input
                      type="number"
                      step="0.1"
                      min="1"
                      max="10"
                      required
                      value={targetCgpa}
                      onChange={(e) => setTargetCgpa(e.target.value)}
                      className="w-full py-2.5 px-3 rounded-xl glass-input text-xs"
                      placeholder="8.5"
                    />
                  </div>
                </>
              )}

              {/* FACULTY Specific Registration Inputs */}
              {isRegister && role === "FACULTY" && (
                <div className="grid grid-cols-2 gap-4">
                  <div className="space-y-1">
                    <label className="text-xs font-bold text-secondary-theme uppercase tracking-wider">Department</label>
                    <input
                      type="text"
                      required
                      value={department}
                      onChange={(e) => setDepartment(e.target.value)}
                      className="w-full py-2.5 px-3 rounded-xl glass-input text-xs"
                      placeholder="Computer Science"
                    />
                  </div>
                  <div className="space-y-1">
                    <label className="text-xs font-bold text-secondary-theme uppercase tracking-wider">Designation</label>
                    <input
                      type="text"
                      required
                      value={designation}
                      onChange={(e) => setDesignation(e.target.value)}
                      className="w-full py-2.5 px-3 rounded-xl glass-input text-xs"
                      placeholder="Assistant Professor"
                    />
                  </div>
                </div>
              )}

              {/* Feedback Alert */}
              {error && (
                <div className="p-3 bg-red-500/10 border border-red-500/20 text-red-400 rounded-xl text-xs font-semibold flex items-center gap-2">
                  <Shield className="h-4 w-4" />
                  <span>{error}</span>
                </div>
              )}

              {/* Submit Button & Switch Options */}
              <div className="space-y-3 pt-2">
                <button
                  type="submit"
                  disabled={loading}
                  className="w-full py-3 bg-gradient-to-r from-purple-600 to-pink-600 hover:from-purple-500 hover:to-pink-500 text-white rounded-xl text-xs font-bold flex items-center justify-center gap-2 shadow-lg shadow-purple-600/20 glow-btn transition-all duration-200 cursor-pointer"
                >
                  {loading ? (
                    <div className="h-4 w-4 border-2 border-white border-t-transparent rounded-full animate-spin" />
                  ) : (
                    <>
                      <span>{isRegister ? "Create Account & Enter Portal" : "Enter Portal Dashboard"}</span>
                      <ArrowRight className="h-4 w-4" />
                    </>
                  )}
                </button>

                {/* Sub-Footer Quick Links */}
                <div className="flex justify-between items-center text-[10px] text-secondary-theme pt-2 border-t border-white/5 font-semibold">
                  <button
                    type="button"
                    onClick={() => {
                      setRole("ADMIN");
                    }}
                    className="hover:text-purple-theme transition-all cursor-pointer font-bold uppercase tracking-widest bg-transparent border-0"
                  >
                    System Admin Access
                  </button>

                  <button
                    type="button"
                    onClick={() => {
                      setIsRegister(!isRegister);
                      setError("");
                    }}
                    className="hover:text-purple-theme transition-all cursor-pointer font-bold bg-transparent border-0"
                  >
                    {isRegister ? "Sign In Instead" : "Create Account"}
                  </button>
                </div>
              </div>
            </form>
          </div>
        </div>

      </div>
    </div>
  );
}
