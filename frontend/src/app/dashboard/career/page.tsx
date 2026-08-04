import React, { useState } from "react";
import Layout from "../../../components/Layout";
import { 
  Briefcase, 
  FileText, 
  Sparkles, 
  Video, 
  CheckCircle, 
  AlertCircle, 
  MapPin, 
  Calendar,
  Send,
  Loader2
} from "lucide-react";

export default function CareerCenter() {
  
  // Resume Review States
  const [resumeScore, setResumeScore] = useState<number | null>(null);
  const [analyzingResume, setAnalyzingResume] = useState(false);
  const [resumeFeedback, setResumeFeedback] = useState<string[]>([]);
  const [selectedFile, setSelectedFile] = useState<string | null>(null);

  // Mock Interview States
  const [activeRole, setActiveRole] = useState("Software Engineer");
  const [interviewQuestion, setInterviewQuestion] = useState(
    "Explain how you would handle database locking in a concurrent environment where multiple transactions modify the same table?"
  );
  const [answerInput, setAnswerInput] = useState("");
  const [interviewFeedback, setInterviewFeedback] = useState<string | null>(null);
  const [analyzingAnswer, setAnalyzingAnswer] = useState(false);

  // Internship & Hackathon listings
  const listings = [
    { type: "Internship", title: "ML Engineering Intern", company: "Apple Inc.", location: "Cupertino, CA (Remote)", date: "Apply by Sep 10" },
    { type: "Internship", title: "Software Development Intern", company: "Linear App", location: "San Francisco, CA", date: "Apply by Aug 25" },
    { type: "Hackathon", title: "Autonomous Agents Buildathon", company: "DeepMind", location: "Virtual", date: "Starts Oct 05" }
  ];

  const handleResumeSimulate = () => {
    if (!selectedFile) {
      alert("Please choose a file or use the mock template.");
      return;
    }
    setAnalyzingResume(true);
    setResumeScore(null);
    setResumeFeedback([]);

    setTimeout(() => {
      setAnalyzingResume(false);
      setResumeScore(84);
      setResumeFeedback([
        "Strong presentation of project architecture utilizing React + Vite & Spring Boot.",
        "Add more action verbs: replace 'helped build' with 'orchestrated implementation of'.",
        "Missing index/search-related keywords: suggest adding 'Redis caching mechanisms'."
      ]);
    }, 2000);
  };

  const handleInterviewSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!answerInput.trim()) return;
    
    setAnalyzingAnswer(true);
    setInterviewFeedback(null);

    setTimeout(() => {
      setAnalyzingAnswer(false);
      
      const containsKeywords = ["lock", "optimistic", "pessimistic", "isolation", "deadlock"].some(kw => 
        answerInput.toLowerCase().includes(kw)
      );

      if (containsKeywords) {
        setInterviewFeedback(
          "âœ… Excellent conceptual explanation! You correctly touched upon locking methodologies (optimistic/pessimistic) and transaction isolation criteria. Suggest structuring your answers with structural flow charts in real loops."
        );
      } else {
        setInterviewFeedback(
          "âš ï¸ Good attempt, but you missed crucial keywords. For databases, always reference Optimistic vs Pessimistic locking paradigms and JDBC isolation levels. Review 'Concurrency Control' in DBMS module."
        );
      }
    }, 2200);
  };

  const handleRoleSwap = (role: string, qText: string) => {
    setActiveRole(role);
    setInterviewQuestion(qText);
    setAnswerInput("");
    setInterviewFeedback(null);
  };

  return (
    <Layout>
      <div className="space-y-8">
        
        {/* Title */}
        <div>
          <h1 className="text-3xl font-extrabold text-main-theme flex items-center gap-2">
            <Briefcase className="h-8 w-8 text-purple-theme" />
            <span>Placement & Career Guidance</span>
          </h1>
          <p className="text-secondary-theme text-sm mt-1">
            Analyze your resume parameters against standard tech companies and simulate mock interviews with AI.
          </p>
        </div>

        {/* Layout Grid */}
        <div className="grid grid-cols-1 lg:grid-cols-2 gap-8">
          
          {/* Left Column: Resume Review Center */}
          <div className="glass-panel p-6 rounded-2xl border border-white/5 space-y-6 flex flex-col justify-between">
            <div className="space-y-4">
              <div className="flex items-center gap-2 border-b border-white/5 pb-3">
                <FileText className="h-5 w-5 text-purple-theme" />
                <h3 className="text-sm font-extrabold tracking-wide">Resume AI Parser</h3>
              </div>

              {/* Upload Drag zone mock */}
              <div className="border border-dashed border-white/10 rounded-xl p-8 text-center bg-white/3 flex flex-col items-center justify-center space-y-3">
                <FileText className="h-10 w-10 text-secondary-theme" />
                <div>
                  <button 
                    onClick={() => setSelectedFile("Student_Resume.pdf")}
                    className="text-xs text-purple-theme hover:text-purple-300 font-bold"
                  >
                    Upload Student_Resume.pdf
                  </button>
                  <p className="text-[10px] text-secondary-theme mt-1">PDF, DOCX up to 10MB</p>
                </div>
                {selectedFile && (
                  <span className="text-[10px] text-emerald-theme font-bold">Selected: {selectedFile}</span>
                )}
              </div>

              {/* Feedback lists */}
              {analyzingResume && (
                <div className="flex items-center gap-2 text-xs text-secondary-theme justify-center">
                  <Loader2 className="h-4 w-4 animate-spin text-purple-theme" />
                  <span>AI Parser parsing text streams...</span>
                </div>
              )}

              {resumeScore !== null && (
                <div className="space-y-4 animate-fade-in">
                  <div className="flex justify-between items-center bg-white/5 p-3 rounded-xl border border-white/5">
                    <span className="text-xs font-bold text-main-theme">Resume Strength Score:</span>
                    <span className="text-lg font-black text-purple-theme">{resumeScore} / 100</span>
                  </div>
                  <div className="space-y-2">
                    {resumeFeedback.map((f, i) => (
                      <div key={i} className="text-xs p-2.5 rounded-lg bg-white/5 border-l-2 border-purple-500 flex items-start gap-2">
                        {i === 0 ? <CheckCircle className="h-4 w-4 text-emerald-theme shrink-0 mt-0.5" /> : <AlertCircle className="h-4 w-4 text-purple-theme shrink-0 mt-0.5" />}
                        <span>{f}</span>
                      </div>
                    ))}
                  </div>
                </div>
              )}
            </div>

            <button
              onClick={handleResumeSimulate}
              disabled={analyzingResume}
              className="w-full py-3 bg-purple-600 hover:bg-purple-500 disabled:opacity-40 text-white text-xs font-bold rounded-xl transition-all shadow-md shadow-purple-500/15 cursor-pointer mt-4"
            >
              Analyze Resume Keywords
            </button>
          </div>

          {/* Right Column: Placement prep & Mock Interview */}
          <div className="glass-panel p-6 rounded-2xl border border-white/5 space-y-6">
            
            {/* Header / Selector */}
            <div className="flex justify-between items-center border-b border-white/5 pb-3">
              <div className="flex items-center gap-2">
                <Video className="h-5 w-5 text-cyan-theme" />
                <h3 className="text-sm font-extrabold tracking-wide">Interview Prep</h3>
              </div>

              <div className="flex gap-2">
                <button
                  onClick={() => handleRoleSwap("Software Engineer", "Explain how you would handle database locking in a concurrent environment where multiple transactions modify the same table?")}
                  className={`px-2.5 py-1 rounded-md text-[10px] font-bold ${activeRole === "Software Engineer" ? "bg-cyan-600 text-white" : "bg-white/5 text-secondary-theme"}`}
                >
                  SE (System)
                </button>
                <button
                  onClick={() => handleRoleSwap("ML Scientist", "What is the vanishing gradient problem in deep neural networks, and how do residual skip-connections resolve it?")}
                  className={`px-2.5 py-1 rounded-md text-[10px] font-bold ${activeRole === "ML Scientist" ? "bg-cyan-600 text-white" : "bg-white/5 text-secondary-theme"}`}
                >
                  ML (Algorithm)
                </button>
              </div>
            </div>

            {/* Form */}
            <form onSubmit={handleInterviewSubmit} className="space-y-4">
              <div className="space-y-2">
                <span className="text-[10px] text-secondary-theme font-bold uppercase block">AI Recruiter Prompt:</span>
                <p className="text-xs font-semibold text-main-theme bg-white/5 p-3 rounded-xl border border-white/5 leading-relaxed">
                  {interviewQuestion}
                </p>
              </div>

              <div className="space-y-1">
                <label className="text-[10px] text-secondary-theme font-bold uppercase">Your Conceptual Explanation:</label>
                <textarea
                  required
                  rows={4}
                  value={answerInput}
                  onChange={(e) => setAnswerInput(e.target.value)}
                  placeholder="Type your explanation here. Touch upon core algorithms and design parameters..."
                  className="w-full p-3 rounded-xl glass-input text-xs leading-relaxed"
                />
              </div>

              {/* Feedback logs */}
              {analyzingAnswer && (
                <div className="flex items-center gap-2 text-xs text-secondary-theme justify-center">
                  <Loader2 className="h-4 w-4 animate-spin text-cyan-theme" />
                  <span>AI evaluator grading responses...</span>
                </div>
              )}

              {interviewFeedback && (
                <div className="p-3.5 bg-white/5 border border-white/5 rounded-xl text-xs leading-relaxed">
                  {interviewFeedback}
                </div>
              )}

              <button
                type="submit"
                disabled={analyzingAnswer || !answerInput.trim()}
                className="w-full py-3 bg-cyan-600 hover:bg-cyan-500 text-white disabled:opacity-40 text-xs font-bold rounded-xl transition-all shadow-md shadow-cyan-500/15 flex items-center justify-center gap-1.5 cursor-pointer"
              >
                <span>Evaluate Response</span>
                <Send className="h-4 w-4" />
              </button>
            </form>

          </div>

        </div>

        {/* Hackathon Recommendations lists */}
        <div className="glass-panel p-6 rounded-2xl border border-white/5 space-y-4">
          <div className="flex items-center gap-2 border-b border-white/5 pb-3">
            <Sparkles className="h-5 w-5 text-amber-theme" />
            <h3 className="text-sm font-extrabold tracking-wide">Target Internship & Hackathon Recommendations</h3>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
            {listings.map((item) => (
              <div key={item.title} className="p-4 bg-white/5 border border-white/5 rounded-xl hover:border-purple-500/30 transition-all flex flex-col justify-between space-y-3">
                <div className="space-y-1">
                  <span className={`text-[9px] font-bold px-2 py-0.5 rounded-full ${item.type === "Internship" ? "bg-purple-500/10 text-purple-theme" : "bg-cyan-500/10 text-cyan-theme"}`}>
                    {item.type}
                  </span>
                  <h4 className="text-xs font-bold text-main-theme pt-1">{item.title}</h4>
                  <span className="text-[10px] text-secondary-theme flex items-center gap-1">
                    <MapPin className="h-3 w-3" />
                    <span>{item.company} â€¢ {item.location}</span>
                  </span>
                </div>
                
                <span className="text-[10px] text-secondary-theme flex items-center gap-1">
                  <Calendar className="h-3 w-3" />
                  <span>{item.date}</span>
                </span>
              </div>
            ))}
          </div>
        </div>

      </div>
    </Layout>
  );
}
