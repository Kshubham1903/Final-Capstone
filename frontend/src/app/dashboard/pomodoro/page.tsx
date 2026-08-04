import React, { useState, useEffect, useRef } from "react";
import Layout from "../../../components/Layout";
import { 
  Play, 
  Pause, 
  RotateCcw, 
  Volume2, 
  VolumeX, 
  Sparkles, 
  Music, 
  CheckCircle,
  Plus,
  Compass,
  Watch
} from "lucide-react";
import { saveStudentProfile, calculateLocalSgi, StudentProfile } from "../../../services/mockData";
import { fetchProfile } from "../../../services/api";

export default function Pomodoro() {
  const [profile, setProfile] = useState<StudentProfile | null>(null);
  
  // Timer States
  const [mode, setMode] = useState<"WORK" | "SHORT" | "LONG">("WORK");
  const [timeLeft, setTimeLeft] = useState(25 * 60);
  const [isRunning, setIsRunning] = useState(false);
  
  // Sound Mock States
  const [soundEnabled, setSoundEnabled] = useState(false);
  const [soundTrack, setSoundTrack] = useState("Lo-Fi Beats");
  
  // Tasks list
  const [tasks, setTasks] = useState([
    { id: 1, text: "Revise Normalization forms (1NF/2NF/3NF)", completed: false },
    { id: 2, text: "Practice BST In-order Traversals", completed: true },
    { id: 3, text: "Verify neural networks learning curves", completed: false }
  ]);
  const [newTaskText, setNewTaskText] = useState("");

  const timerRef = useRef<any>(null);

  useEffect(() => {
    async function load() {
      const userId = localStorage.getItem("edupilot_user_id") || "";
      if (userId) {
        const p = await fetchProfile(userId);
        setProfile(p);
      }
    }
    load();
  }, []);

  useEffect(() => {
    if (isRunning) {
      timerRef.current = setInterval(() => {
        setTimeLeft(prev => {
          if (prev <= 1) {
            handleTimerComplete();
            return 0;
          }
          return prev - 1;
        });
      }, 1000);
    } else {
      clearInterval(timerRef.current);
    }

    return () => clearInterval(timerRef.current);
  }, [isRunning, mode]);

  const handleTimerComplete = () => {
    setIsRunning(false);
    
    if (mode === "WORK") {
      // Completed work session - boost productivity metrics
      if (profile) {
        const updated = {
          ...profile,
          productivityScore: Math.min(profile.productivityScore + 4, 100)
        };
        // Add a focus log entry to productivity minutes
        const historyCopy = [...updated.lifestyleHistory];
        if (historyCopy.length > 0) {
          historyCopy[historyCopy.length - 1].studyMinutes += 25;
        }
        updated.lifestyleHistory = historyCopy;
        updated.studentGrowthIndex = calculateLocalSgi(updated);
        
        saveStudentProfile(updated);
        setProfile(updated);
      }
      alert("ðŸŽ‰ Great focus block completed! Take a quick rest.");
      setMode("SHORT");
      setTimeLeft(5 * 60);
    } else {
      alert("â±ï¸ Rest finished! Ready to step back in?");
      setMode("WORK");
      setTimeLeft(25 * 60);
    }
  };

  const toggleTimer = () => {
    setIsRunning(!isRunning);
  };

  const resetTimer = () => {
    setIsRunning(false);
    if (mode === "WORK") setTimeLeft(25 * 60);
    else if (mode === "SHORT") setTimeLeft(5 * 60);
    else setTimeLeft(15 * 60);
  };

  const handleModeChange = (newMode: "WORK" | "SHORT" | "LONG") => {
    setIsRunning(false);
    setMode(newMode);
    if (newMode === "WORK") setTimeLeft(25 * 60);
    else if (newMode === "SHORT") setTimeLeft(5 * 60);
    else setTimeLeft(15 * 60);
  };

  const formatTime = (seconds: number) => {
    const mins = Math.floor(seconds / 60);
    const secs = seconds % 60;
    return `${mins.toString().padStart(2, "0")}:${secs.toString().padStart(2, "0")}`;
  };

  const handleAddTask = (e: React.FormEvent) => {
    e.preventDefault();
    if (!newTaskText.trim()) return;
    setTasks([...tasks, {
      id: Date.now(),
      text: newTaskText,
      completed: false
    }]);
    setNewTaskText("");
  };

  const toggleTaskComplete = (id: number) => {
    setTasks(tasks.map(t => t.id === id ? { ...t, completed: !t.completed } : t));
  };

  return (
    <Layout>
      <div className="space-y-8">
        
        {/* Title */}
        <div>
          <h1 className="text-3xl font-extrabold text-main-theme flex items-center gap-2">
            <Watch className="h-8 w-8 text-purple-theme" />
            <span>Focus Pomodoro Timer</span>
          </h1>
          <p className="text-secondary-theme text-sm mt-1">
            Increase your daily consistency score by locking in study blocks. Includes integrated cognitive sound loops.
          </p>
        </div>

        {/* Layout Grid */}
        <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
          
          {/* Left Column: Circular Timer (2/3 width) */}
          <div className="lg:col-span-2 glass-panel p-8 rounded-2xl border border-white/10 flex flex-col items-center justify-center space-y-8 relative overflow-hidden">
            <div className="absolute top-0 right-0 h-48 w-48 bg-purple-500/5 rounded-full blur-[80px] pointer-events-none" />
            
            {/* Mode Select Buttons */}
            <div className="flex gap-2 bg-white/5 p-1.5 rounded-xl border border-white/5 text-xs font-semibold">
              <button
                onClick={() => handleModeChange("WORK")}
                className={`px-4 py-2 rounded-lg transition-all ${mode === "WORK" ? "bg-purple-600 text-white" : "text-secondary-theme hover:text-white"}`}
              >
                Focus Session (25m)
              </button>
              <button
                onClick={() => handleModeChange("SHORT")}
                className={`px-4 py-2 rounded-lg transition-all ${mode === "SHORT" ? "bg-purple-600 text-white" : "text-secondary-theme hover:text-white"}`}
              >
                Short Rest (5m)
              </button>
              <button
                onClick={() => handleModeChange("LONG")}
                className={`px-4 py-2 rounded-lg transition-all ${mode === "LONG" ? "bg-purple-600 text-white" : "text-secondary-theme hover:text-white"}`}
              >
                Long Rest (15m)
              </button>
            </div>

            {/* Countdown Digits */}
            <div className="flex flex-col items-center space-y-2">
              <div className="text-7xl md:text-8xl font-black font-mono tracking-tight text-transparent bg-clip-text bg-gradient-to-r from-purple-400 via-pink-400 to-cyan-400">
                {formatTime(timeLeft)}
              </div>
              <span className="text-[10px] text-secondary-theme uppercase tracking-widest font-bold">
                {mode === "WORK" ? "Study Period Active" : "Cool Down Period"}
              </span>
            </div>

            {/* Controls */}
            <div className="flex items-center gap-4">
              <button
                onClick={resetTimer}
                className="p-3 bg-white/5 hover:bg-white/10 text-main-theme rounded-full border border-white/5 transition-all cursor-pointer"
              >
                <RotateCcw className="h-5 w-5" />
              </button>

              <button
                onClick={toggleTimer}
                className="px-8 py-3.5 bg-gradient-to-r from-purple-600 to-pink-600 hover:from-purple-500 hover:to-pink-500 text-white rounded-full text-xs font-bold shadow-lg shadow-purple-500/20 glow-btn flex items-center gap-2 cursor-pointer transition-all"
              >
                {isRunning ? (
                  <>
                    <Pause className="h-4 w-4 fill-white" />
                    <span>Pause Session</span>
                  </>
                ) : (
                  <>
                    <Play className="h-4 w-4 fill-white" />
                    <span>Start Session</span>
                  </>
                )}
              </button>

              <button
                onClick={() => setSoundEnabled(!soundEnabled)}
                className={`p-3 rounded-full border transition-all cursor-pointer ${soundEnabled ? "bg-cyan-500/10 border-cyan-500/30 text-cyan-theme" : "bg-white/5 border-white/5 text-secondary-theme"}`}
              >
                {soundEnabled ? <Volume2 className="h-5 w-5" /> : <VolumeX className="h-5 w-5" />}
              </button>
            </div>

            {/* Sound options panel */}
            {soundEnabled && (
              <div className="flex items-center gap-3 p-3 bg-white/5 rounded-xl border border-white/5 text-xs text-secondary-theme">
                <Music className="h-4 w-4 text-cyan-theme" />
                <span>Ambient Flow:</span>
                {["Lo-Fi Beats", "Rainforest", "Cosy Cafe"].map(track => (
                  <button
                    key={track}
                    onClick={() => setSoundTrack(track)}
                    className={`px-2.5 py-1 rounded-md text-[10px] font-bold ${soundTrack === track ? "bg-cyan-500/20 text-cyan-200" : "hover:text-white"}`}
                  >
                    {track}
                  </button>
                ))}
              </div>
            )}

          </div>

          {/* Right Column: Focus Tasks Checklist (1/3 width) */}
          <div className="glass-panel p-6 rounded-2xl border border-white/5 flex flex-col justify-between space-y-6">
            
            {/* Checklist Header */}
            <div className="space-y-4">
              <div className="flex items-center gap-2 border-b border-white/5 pb-3">
                <Compass className="h-5 w-5 text-cyan-theme" />
                <h3 className="text-sm font-extrabold tracking-wide">Target Session Topics</h3>
              </div>

              {/* Task list render */}
              <div className="space-y-3 max-h-64 overflow-y-auto">
                {tasks.map(task => (
                  <div
                    key={task.id}
                    onClick={() => toggleTaskComplete(task.id)}
                    className={`flex items-center gap-3 p-3 rounded-xl border text-xs cursor-pointer transition-all ${task.completed ? "bg-emerald-500/5 border-emerald-500/20 text-secondary-theme line-through" : "bg-white/5 border-white/5 text-main-theme hover:border-white/10"}`}
                  >
                    <div className={`h-4.5 w-4.5 rounded-md border flex items-center justify-center ${task.completed ? "bg-emerald-500 border-emerald-500 text-white" : "border-white/20"}`}>
                      {task.completed && <CheckCircle className="h-3 w-3" />}
                    </div>
                    <span>{task.text}</span>
                  </div>
                ))}
              </div>
            </div>

            {/* Add new task form */}
            <form onSubmit={handleAddTask} className="flex gap-2">
              <input
                type="text"
                placeholder="Add active study topic..."
                value={newTaskText}
                onChange={(e) => setNewTaskText(e.target.value)}
                className="flex-1 py-2 px-3 rounded-lg glass-input text-xs"
              />
              <button
                type="submit"
                className="p-2.5 bg-purple-600 hover:bg-purple-500 text-white rounded-lg text-xs font-bold cursor-pointer"
              >
                <Plus className="h-4 w-4" />
              </button>
            </form>

          </div>

        </div>

      </div>
    </Layout>
  );
}
