import React, { useState, useEffect } from "react";
import { Link, useLocation, useNavigate } from "react-router-dom";
import { 
  LayoutDashboard, 
  Timer, 
  GraduationCap, 
  Briefcase, 
  Users, 
  Settings, 
  Bell, 
  Flame, 
  ShieldAlert, 
  LogOut,
  BrainCircuit,
  Menu,
  X,
  Sun,
  Moon,
  User,
  Bot
} from "lucide-react";
import { getStoredStudentProfile } from "../services/mockData";
import { fetchProfile } from "../services/api";

export default function Layout({ children }: { children: React.ReactNode }) {
  const location = useLocation();
  const pathname = location.pathname;
  const navigate = useNavigate();
  const [mobileOpen, setMobileOpen] = useState(false);
  const [currentRole, setCurrentRole] = useState<"STUDENT" | "FACULTY" | "ADMIN">("STUDENT");
  const [streak, setStreak] = useState(0);
  const [userName, setUserName] = useState("User");
  const [notifications, setNotifications] = useState<Array<{ id: number; text: string; type: string }>>([]);
  const [showNotifications, setShowNotifications] = useState(false);
  const [theme, setTheme] = useState<"dark" | "light">("dark");

  useEffect(() => {
    const storedTheme = localStorage.getItem("edupilot_theme") as "dark" | "light" | null;
    if (storedTheme) {
      setTheme(storedTheme);
      if (storedTheme === "light") {
        document.documentElement.classList.add("light");
      } else {
        document.documentElement.classList.remove("light");
      }
    }
  }, []);

  const toggleTheme = () => {
    const nextTheme = theme === "dark" ? "light" : "dark";
    setTheme(nextTheme);
    localStorage.setItem("edupilot_theme", nextTheme);
    if (nextTheme === "light") {
      document.documentElement.classList.add("light");
    } else {
      document.documentElement.classList.remove("light");
    }
  };

  useEffect(() => {
    async function syncProfile() {
      const userId = localStorage.getItem("edupilot_user_id") || "";
      let name = localStorage.getItem("edupilot_user_name") || "";
      let currentStreak = 0;

      if (userId) {
        const p = await fetchProfile(userId);
        if (p) {
          if (p.fullName && p.fullName.trim()) name = p.fullName;
          currentStreak = p.currentStreakCount || 0;
        }
      }

      if (name && name.trim()) setUserName(name);
      setStreak(currentStreak);
    }
    syncProfile();

    // Sync role based on pathname
    if (pathname.includes("/faculty")) {
      setCurrentRole("FACULTY");
    } else if (pathname.includes("/admin")) {
      setCurrentRole("ADMIN");
    } else {
      setCurrentRole("STUDENT");
    }
  }, [pathname]);

  const handleRoleChange = (role: "STUDENT" | "FACULTY" | "ADMIN") => {
    setCurrentRole(role);
    if (role === "STUDENT") {
      navigate("/dashboard");
    } else if (role === "FACULTY") {
      navigate("/faculty");
    } else if (role === "ADMIN") {
      navigate("/admin");
    }
  };

  const navItems = {
    STUDENT: [
      { name: "Dashboard", href: "/dashboard", icon: LayoutDashboard },
      { name: "My Profile", href: "/dashboard/profile", icon: User },
      { name: "Pomodoro Focus", href: "/dashboard/pomodoro", icon: Timer },
      { name: "Adaptive Quizzes", href: "/dashboard/quizzes", icon: GraduationCap },
      { name: "Career Guidance", href: "/dashboard/career", icon: Briefcase },
      { name: "AI Tutor", href: "/dashboard/ai-tutor", icon: Bot },
    ],
    FACULTY: [
      { name: "Class Performance", href: "/faculty", icon: Users },
      { name: "Quiz Manager", href: "/faculty/quiz-manager", icon: GraduationCap },
    ],
    ADMIN: [
      { name: "System Control", href: "/admin", icon: Settings },
      { name: "All Dashboards", href: "/dashboard", icon: LayoutDashboard },
    ]
  };

  const activeItems = navItems[currentRole] || navItems.STUDENT;

  return (
    <div className="min-h-screen flex flex-col md:flex-row text-main-theme">
      
      {/* Mobile Top Navbar */}
      <div className="md:hidden flex justify-between items-center px-4 py-3 glass-panel border-b border-white/5 sticky top-0 z-50">
        <div className="flex items-center gap-2">
          <BrainCircuit className="h-6 w-6 text-purple-theme" />
          <span className="font-bold text-lg tracking-wider text-gradient-purple">EduPilot AI</span>
        </div>
        <button onClick={() => setMobileOpen(!mobileOpen)} className="p-1 text-secondary-theme hover:text-main-theme">
          {mobileOpen ? <X className="h-6 w-6" /> : <Menu className="h-6 w-6" />}
        </button>
      </div>

      {/* Sidebar Navigation */}
      <aside className={`fixed inset-y-0 left-0 z-40 w-64 glass-panel border-r border-white/5 flex flex-col justify-between transform transition-transform duration-300 ease-in-out md:translate-x-0 md:static ${mobileOpen ? "translate-x-0" : "-translate-x-full"}`}>
        
        {/* Upper Sidebar */}
        <div>
          {/* Logo Section */}
          <div className="hidden md:flex items-center gap-3 px-6 py-6">
            <div className="h-10 w-10 rounded-xl bg-purple-600/20 flex items-center justify-center border border-purple-500/30 shadow-lg shadow-purple-500/10">
              <BrainCircuit className="h-6 w-6 text-purple-theme" />
            </div>
            <div>
              <h1 className="font-extrabold text-lg tracking-wide text-transparent bg-clip-text bg-gradient-to-r from-purple-400 to-pink-500">EduPilot AI</h1>
              <span className="text-[10px] text-secondary-theme uppercase tracking-widest font-bold">Self-Growth OS</span>
            </div>
          </div>

          {/* Navigation Links */}
          <nav className="px-4 py-4 space-y-1">
            <div className="px-3 mb-2 text-[10px] uppercase font-bold text-secondary-theme tracking-wider">Navigation</div>
            {activeItems.map((item) => {
              const Icon = item.icon;
              const isActive = pathname === item.href;
              return (
                <Link
                  key={item.name}
                  to={item.href}
                  onClick={() => setMobileOpen(false)}
                  className={`flex items-center gap-3 px-4 py-3 rounded-xl transition-all duration-200 ${isActive ? "bg-[var(--sidebar-active-bg)] text-purple-theme border-l-4 border-[var(--accent-purple)] font-bold" : "text-secondary-theme hover:bg-white/5 hover:text-main-theme"}`}
                >
                  <Icon className={`h-5 w-5 ${isActive ? "text-purple-theme" : "text-secondary-theme"}`} />
                  <span className="text-sm font-medium">{item.name}</span>
                </Link>
              );
            })}
          </nav>

        </div>

        {/* Lower Sidebar / Profile */}
        <div className="p-4 border-t border-white/5">
          <div className="flex items-center gap-3 p-2 rounded-xl bg-white/10 border border-white/15">
            <div className="h-9 w-9 rounded-full bg-gradient-to-tr from-purple-500 to-pink-500 flex items-center justify-center font-bold text-sm text-white">
              {userName && userName !== "User" ? userName.split(" ").map(n => n[0]).join("").toUpperCase().slice(0, 2) : "EP"}
            </div>
            <div className="flex-1 min-w-0">
              <h2 className="text-xs font-semibold truncate text-main-theme">{userName}</h2>
              <span className="text-[10px] text-secondary-theme truncate block font-bold">{currentRole} Profile</span>
            </div>
            <Link to="/" onClick={() => {
              localStorage.removeItem("edupilot_user_id");
              localStorage.removeItem("edupilot_user_name");
              localStorage.removeItem("edupilot_user_email");
            }} className="text-secondary-theme hover:text-red-400">
              <LogOut className="h-4 w-4" />
            </Link>
          </div>
        </div>
      </aside>

      {/* Main Content Area */}
      <div className="flex-1 flex flex-col min-w-0">
        
        {/* Header Bar */}
        <header className="hidden md:flex justify-between items-center px-8 py-4 bg-white/5 backdrop-blur-md border-b border-white/5 relative z-30">
          <div className="flex items-center gap-4">
            <h2 className="text-lg font-semibold tracking-wide capitalize text-main-theme">
              {pathname === "/dashboard/ai-tutor"
                ? "AI Tutor"
                : (pathname.split("/").pop() || "Portal").replace(/-/g, " ")}
            </h2>
            {currentRole === "STUDENT" && (
              <div className="flex items-center gap-1.5 bg-amber-500/10 text-amber-theme border border-amber-500/20 px-3 py-1 rounded-full text-xs font-semibold shadow-inner">
                <Flame className="h-4 w-4 fill-[var(--accent-amber)]" />
                <span>{streak} Day Streak</span>
              </div>
            )}
          </div>
          
          <div className="flex items-center gap-4 relative">
            
            {/* Theme Toggle Button */}
            <button
              onClick={toggleTheme}
              className="p-2 rounded-xl bg-white/5 border border-white/5 text-secondary-theme hover:text-main-theme transition-all cursor-pointer"
              title="Toggle Theme"
            >
              {theme === "dark" ? <Sun className="h-5 w-5 text-amber-theme" /> : <Moon className="h-5 w-5 text-indigo-400" />}
            </button>
            
            {/* Notification Bell */}
            <button 
              onClick={() => setShowNotifications(!showNotifications)}
              className="relative p-2 rounded-xl bg-white/5 border border-white/5 text-secondary-theme hover:text-main-theme transition-all cursor-pointer"
            >
              <Bell className="h-5 w-5" />
              {notifications.length > 0 && (
                <span className="absolute top-1 right-1 h-2 w-2 rounded-full bg-pink-500 animate-ping" />
              )}
            </button>

            {/* Notification Dropdown */}
            {showNotifications && (
              <div className="absolute right-0 top-12 w-80 bg-[var(--glass-hover-bg)] rounded-xl p-4 border border-[var(--glass-border)] shadow-2xl z-50">
                <div className="flex justify-between items-center mb-3">
                  <h3 className="text-xs font-bold text-secondary-theme uppercase tracking-wider">AI Copilot Notifications</h3>
                  <button 
                    onClick={() => setNotifications([])} 
                    className="text-[10px] text-purple-theme hover:text-purple-300"
                  >
                    Clear All
                  </button>
                </div>
                <div className="space-y-2">
                  {notifications.length === 0 ? (
                    <div className="text-xs text-secondary-theme text-center py-4">No new system alerts</div>
                  ) : (
                    notifications.map((n) => (
                      <div key={n.id} className="text-xs p-2.5 rounded-lg bg-white/10 border-l-4 border-purple-500 text-main-theme shadow-sm">
                        {n.text}
                      </div>
                    ))
                  )}
                </div>
              </div>
            )}
            
            {/* System Status Indicators */}
            <div className="flex items-center gap-2 bg-emerald-500/10 text-emerald-theme border border-emerald-500/20 px-3 py-1 rounded-full text-xs font-semibold">
              <span className="h-1.5 w-1.5 rounded-full bg-emerald-400" />
              <span>AI Core Live</span>
            </div>
          </div>
        </header>

        {/* Content container */}
        <main className="flex-1 p-6 md:p-8 overflow-y-auto max-h-[calc(100vh-68px)]">
          {children}
        </main>
      </div>

    </div>
  );
}
