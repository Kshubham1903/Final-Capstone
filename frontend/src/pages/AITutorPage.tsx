import React, { useState, useEffect, useRef } from "react";
import { 
  Bot, Send, Plus, Trash2, MessageSquare, Sparkles, User, Loader2, ArrowLeft, Lightbulb, BookOpen, ChevronRight, Activity, AlertCircle, CheckCircle2,
  BrainCircuit, Dumbbell, History, AlertOctagon, Briefcase, Code, Copy, Check, RefreshCw, AlertTriangle, X 
} from "lucide-react";
import { Link } from "react-router-dom";
import { 
  fetchConversationHistory, createNewConversation, sendChatMessage, deleteConversation, fetchConversationById, fetchStudentContext 
} from "../services/api";

type LearningMode = "LEARN" | "PRACTICE" | "REVISION" | "EXPLAIN_MISTAKES" | "INTERVIEW" | "CODING";

interface ModeOption {
  key: LearningMode;
  label: string;
  icon: any;
  description: string;
  color: string;
}

const LEARNING_MODES: ModeOption[] = [
  { key: "LEARN", label: "Learn", icon: BrainCircuit, description: "Step-by-step conceptual explanations & analogies", color: "from-purple-600 to-indigo-600" },
  { key: "PRACTICE", label: "Practice", icon: Dumbbell, description: "Interactive drill questions & hints", color: "from-purple-600 to-pink-600" },
  { key: "REVISION", label: "Revision", icon: History, description: "High-yield summary notes & memory tricks", color: "from-amber-600 to-orange-600" },
  { key: "EXPLAIN_MISTAKES", label: "Explain Mistakes", icon: AlertOctagon, description: "Diagnostic assessment error breakdown", color: "from-rose-600 to-red-600" },
  { key: "INTERVIEW", label: "Interview", icon: Briefcase, description: "Technical interview & system design simulator", color: "from-emerald-600 to-teal-600" },
  { key: "CODING", label: "Coding", icon: Code, description: "Complexity analysis & code implementation", color: "from-cyan-600 to-blue-600" }
];

export default function AITutorPage() {
  const [conversations, setConversations] = useState<any[]>([]);
  const [activeConvId, setActiveConvId] = useState<string | null>(null);
  const [currentConv, setCurrentConv] = useState<any>(null);
  const [studentContext, setStudentContext] = useState<any>(null);
  const [activeMode, setActiveMode] = useState<LearningMode>("LEARN");
  const [inputText, setInputText] = useState("");
  const [loading, setLoading] = useState(false);
  const [sending, setSending] = useState(false);
  const [copiedIndex, setCopiedIndex] = useState<string | null>(null);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [showClearModal, setShowClearModal] = useState(false);

  const messagesEndRef = useRef<HTMLDivElement>(null);
  const userId = localStorage.getItem("edupilot_user_id") || "user_demo";

  const scrollToBottom = () => {
    messagesEndRef.current?.scrollIntoView({ behavior: "smooth" });
  };

  useEffect(() => {
    async function loadData() {
      setLoading(true);
      setErrorMessage(null);
      try {
        const [history, context] = await Promise.all([
          fetchConversationHistory(userId),
          fetchStudentContext(userId)
        ]);

        setConversations(history || []);
        if (context) setStudentContext(context);

        if (history && history.length > 0) {
          setActiveConvId(history[0].conversationId);
          setCurrentConv(history[0]);
          if (history[0].learningMode) {
            setActiveMode(history[0].learningMode);
          }
        }
      } catch (err: any) {
        setErrorMessage("Network error connecting to AI Tutor service. Retrying background sync...");
      } finally {
        setLoading(false);
      }
    }
    loadData();
  }, [userId]);

  useEffect(() => {
    if (activeConvId) {
      async function loadActive() {
        try {
          const details = await fetchConversationById(activeConvId!);
          if (details) {
            setCurrentConv(details);
            if (details.learningMode) {
              setActiveMode(details.learningMode);
            }
          }
        } catch (err) {
          console.warn("Error loading conversation thread:", err);
        }
      }
      loadActive();
    }
  }, [activeConvId]);

  useEffect(() => {
    scrollToBottom();
  }, [currentConv, sending]);

  const handleNewConversation = async (concept?: string) => {
    setLoading(true);
    setErrorMessage(null);
    try {
      const newConv = await createNewConversation(userId, concept ? `Study: ${concept}` : "New AI Session", undefined, concept, activeMode);
      if (newConv) {
        setConversations((prev) => [newConv, ...prev]);
        setActiveConvId(newConv.conversationId);
        setCurrentConv(newConv);
      }
    } catch (err) {
      setErrorMessage("Could not start new chat session. Please check connection.");
    } finally {
      setLoading(false);
    }
  };

  const handleSendMessage = async (customPrompt?: string) => {
    const promptToSend = customPrompt || inputText;
    if (!promptToSend.trim() || sending) return;

    if (!customPrompt) setInputText("");
    setSending(true);
    setErrorMessage(null);

    if (currentConv) {
      const optimisticMsg = {
        messageId: "temp_" + Date.now(),
        role: "user",
        content: promptToSend,
        timestamp: new Date().toISOString()
      };
      setCurrentConv((prev: any) => ({
        ...prev,
        messages: [...(prev.messages || []), optimisticMsg]
      }));
    }

    try {
      const res = await sendChatMessage({
        studentId: userId,
        conversationId: activeConvId || undefined,
        message: promptToSend,
        learningMode: activeMode
      });

      if (res) {
        if (!activeConvId && res.conversationId) {
          setActiveConvId(res.conversationId);
        }
        const updatedConv = await fetchConversationById(res.conversationId || activeConvId!);
        if (updatedConv) {
          setCurrentConv(updatedConv);
        }
      }
    } catch (err: any) {
      setErrorMessage("Failed to receive AI response. Check backend connection or API key status.");
    } finally {
      setSending(false);
    }
  };

  const handleDeleteConv = async (convId: string, e: React.MouseEvent) => {
    e.stopPropagation();
    await deleteConversation(convId);
    setConversations((prev) => prev.filter((c) => c.conversationId !== convId));
    if (activeConvId === convId) {
      const remaining = conversations.filter((c) => c.conversationId !== convId);
      if (remaining.length > 0) {
        setActiveConvId(remaining[0].conversationId);
        setCurrentConv(remaining[0]);
      } else {
        setActiveConvId(null);
        setCurrentConv(null);
      }
    }
  };

  const handleCopyCode = (text: string, id: string) => {
    navigator.clipboard.writeText(text);
    setCopiedIndex(id);
    setTimeout(() => setCopiedIndex(null), 2000);
  };

  const weakConceptTag = studentContext?.weakConcepts?.[0] || "AVL Trees";
  const focusTaskTag = studentContext?.todayFocusTask || "Review BST Rotations";
  const healthScore = studentContext?.learningHealthScore || 75.0;
  const activeSubject = studentContext?.activeSubject || "Data Structures & Algorithms";

  const currentModeObj = LEARNING_MODES.find(m => m.key === activeMode) || LEARNING_MODES[0];

  return (
    <div className="min-h-screen bg-[#0B0F19] text-main-theme flex flex-col font-sans selection:bg-purple-500/30">
      
      {/* Top Navigation Header */}
      <header className="border-b border-white/5 bg-slate-900/60 backdrop-blur-xl px-6 py-4 flex items-center justify-between sticky top-0 z-30">
        <div className="flex items-center gap-3">
          <Link to="/dashboard" className="p-2 rounded-xl glass-panel bg-white/5 hover:bg-white/10 text-secondary-theme hover:text-white transition-all">
            <ArrowLeft className="h-4 w-4" />
          </Link>
          <div className="h-10 w-10 rounded-2xl bg-gradient-to-tr from-purple-600 via-pink-600 to-indigo-600 flex items-center justify-center text-white shadow-lg shadow-purple-500/25">
            <Bot className="h-5 w-5" />
          </div>
          <div>
            <h1 className="text-base font-black tracking-wide text-main-theme flex items-center gap-2">
              <span>EduPilot AI Tutor</span>
              <span className="text-[10px] font-bold uppercase tracking-wider bg-purple-500/15 text-purple-300 border border-purple-500/30 px-2.5 py-0.5 rounded-full flex items-center gap-1 shadow-sm">
                <Sparkles className="h-3 w-3" />
                Adaptive AI Active
              </span>
            </h1>
            <p className="text-[11px] text-secondary-theme">Contextual prompt intelligence & adaptive learning modes</p>
          </div>
        </div>

        <div className="flex items-center gap-3">
          {activeConvId && (
            <button
              onClick={() => setShowClearModal(true)}
              className="px-3 py-1.5 rounded-xl glass-panel bg-red-500/10 hover:bg-red-500/20 text-red-300 border border-red-500/20 text-xs font-bold transition-all flex items-center gap-1.5 cursor-pointer"
            >
              <Trash2 className="h-3.5 w-3.5" />
              <span>Clear Thread</span>
            </button>
          )}

          <div className="flex items-center gap-2 text-xs font-bold text-secondary-theme glass-panel px-3 py-1.5 rounded-xl border border-white/5">
            <span className="h-2 w-2 rounded-full bg-emerald-400 animate-pulse" />
            <span>LLM Service Connected</span>
          </div>
        </div>
      </header>

      {/* Live Context Header Bar */}
      <div className="glass-panel border-b border-white/5 bg-slate-900/40 backdrop-blur-md px-6 py-2.5 flex flex-wrap items-center justify-between gap-3 text-xs">
        <div className="flex flex-wrap items-center gap-3">
          <div className="flex items-center gap-1.5 bg-purple-500/10 text-purple-300 border border-purple-500/20 px-3 py-1 rounded-xl">
            <BookOpen className="h-3.5 w-3.5 text-purple-400" />
            <span className="text-[10px] text-slate-400 font-bold uppercase">Studying:</span>
            <span className="font-extrabold">{activeSubject}</span>
          </div>

          <div className="flex items-center gap-1.5 bg-emerald-500/10 text-emerald-300 border border-emerald-500/20 px-3 py-1 rounded-xl">
            <CheckCircle2 className="h-3.5 w-3.5 text-emerald-400" />
            <span className="text-[10px] text-slate-400 font-bold uppercase">Current Plan:</span>
            <span className="font-extrabold">{focusTaskTag}</span>
          </div>

          <div className="flex items-center gap-1.5 bg-pink-500/10 text-pink-300 border border-pink-500/20 px-3 py-1 rounded-xl">
            <AlertCircle className="h-3.5 w-3.5 text-pink-400" />
            <span className="text-[10px] text-slate-400 font-bold uppercase">Weak Concept:</span>
            <span className="font-extrabold">{weakConceptTag}</span>
          </div>
        </div>

        <div className="flex items-center gap-2 bg-white/5 px-3 py-1 rounded-xl border border-white/10">
          <Activity className="h-3.5 w-3.5 text-emerald-400" />
          <span className="text-[10px] text-slate-400 uppercase font-bold">Learning Health:</span>
          <span className="font-black text-emerald-400">{healthScore}%</span>
        </div>
      </div>

      {/* Interactive Learning Mode Selector Bar */}
      <div className="bg-slate-900/80 border-b border-white/10 px-6 py-3 flex items-center justify-between gap-4 overflow-x-auto">
        <span className="text-[10px] font-black uppercase tracking-wider text-slate-400 shrink-0">
          Active Learning Mode:
        </span>
        <div className="flex items-center gap-2 overflow-x-auto pb-1 scrollbar-none">
          {LEARNING_MODES.map((mode) => {
            const Icon = mode.icon;
            const isSelected = mode.key === activeMode;
            return (
              <button
                key={mode.key}
                onClick={() => setActiveMode(mode.key)}
                className={`px-3.5 py-2 rounded-xl text-xs font-black transition-all flex items-center gap-2 cursor-pointer shrink-0 border ${
                  isSelected
                    ? `bg-gradient-to-r ${mode.color} text-white border-transparent shadow-lg shadow-purple-500/20 scale-[1.02]`
                    : "glass-panel bg-white/3 hover:bg-white/8 text-slate-400 hover:text-white border-white/5"
                }`}
                title={mode.description}
              >
                <Icon className="h-4 w-4" />
                <span>{mode.label}</span>
              </button>
            );
          })}
        </div>
      </div>

      {/* Error Alert Banner */}
      {errorMessage && (
        <div className="mx-6 mt-4 p-3.5 rounded-2xl bg-rose-500/10 border border-rose-500/30 text-rose-300 text-xs flex items-center justify-between shadow-lg">
          <div className="flex items-center gap-2.5">
            <AlertTriangle className="h-4 w-4 text-rose-400 shrink-0" />
            <span>{errorMessage}</span>
          </div>
          <button onClick={() => setErrorMessage(null)} className="p-1 hover:text-white transition-all">
            <X className="h-4 w-4" />
          </button>
        </div>
      )}

      {/* Main Layout Grid */}
      <div className="flex-1 flex overflow-hidden">
        
        {/* Sidebar */}
        <aside className="w-80 border-r border-white/5 bg-slate-900/30 p-4 flex flex-col gap-4 hidden md:flex">
          <button
            onClick={() => handleNewConversation()}
            className="w-full py-3 px-4 rounded-xl bg-gradient-to-r from-purple-600 to-pink-600 hover:from-purple-500 hover:to-pink-500 text-white font-extrabold text-xs flex items-center justify-center gap-2 shadow-lg shadow-purple-600/20 transition-all cursor-pointer"
          >
            <Plus className="h-4 w-4" />
            <span>New Chat Session</span>
          </button>

          <div className="flex-1 overflow-y-auto space-y-2 pr-1">
            <span className="text-[10px] uppercase font-black tracking-wider text-slate-500 px-2">
              Conversation Threads ({conversations.length})
            </span>

            {conversations.map((conv) => {
              const isActive = conv.conversationId === activeConvId;
              return (
                <div
                  key={conv.conversationId}
                  onClick={() => setActiveConvId(conv.conversationId)}
                  className={`p-3.5 rounded-2xl border text-xs cursor-pointer flex items-center justify-between group transition-all ${
                    isActive
                      ? "glass-panel bg-purple-600/20 border-purple-500/40 text-white font-bold shadow-md shadow-purple-500/10"
                      : "glass-panel bg-white/2 border-white/5 text-slate-400 hover:bg-white/5 hover:text-slate-200"
                  }`}
                >
                  <div className="flex items-center gap-2.5 min-w-0 pr-2">
                    <MessageSquare className={`h-4 w-4 shrink-0 ${isActive ? "text-purple-400" : "text-slate-500"}`} />
                    <span className="truncate">{conv.title || "AI Discussion"}</span>
                  </div>

                  <button
                    onClick={(e) => handleDeleteConv(conv.conversationId, e)}
                    className="opacity-0 group-hover:opacity-100 p-1 text-slate-500 hover:text-pink-400 transition-all"
                    title="Delete Thread"
                  >
                    <Trash2 className="h-3.5 w-3.5" />
                  </button>
                </div>
              );
            })}
          </div>
        </aside>

        {/* Chat Main Window */}
        <main className="flex-1 flex flex-col bg-slate-950/40 relative overflow-hidden">
          
          {/* Active Conversation Title Header */}
          {currentConv && (
            <div className="p-3 px-6 border-b border-white/5 bg-slate-900/20 flex items-center justify-between text-xs text-slate-400">
              <div className="flex items-center gap-2">
                <Sparkles className="h-4 w-4 text-purple-400" />
                <span className="font-extrabold text-white">{currentConv.title}</span>
                <span className="bg-purple-500/20 text-purple-300 border border-purple-500/30 px-2.5 py-0.5 rounded-full text-[10px] font-bold">
                  Mode: {currentModeObj.label}
                </span>
              </div>
              <span className="text-[10px] text-slate-500 font-medium">Provider: Gemini 1.5 Flash</span>
            </div>
          )}

          {/* Messages Stream */}
          <div className="flex-1 overflow-y-auto p-6 space-y-4">
            {currentConv && currentConv.messages && currentConv.messages.length > 0 ? (
              currentConv.messages.map((msg: any, idx: number) => {
                const isUser = msg.role === "user";
                const msgId = msg.messageId || `msg_${idx}`;
                return (
                  <div
                    key={msgId}
                    className={`flex items-start gap-3 ${isUser ? "flex-row-reverse" : "flex-row"}`}
                  >
                    <div
                      className={`h-8 w-8 rounded-xl flex items-center justify-center text-xs font-bold shrink-0 ${
                        isUser
                          ? "bg-purple-600 text-white shadow-md shadow-purple-600/20"
                          : "bg-gradient-to-tr from-cyan-600 to-emerald-500 text-white border border-white/20 shadow-md"
                      }`}
                    >
                      {isUser ? <User className="h-4 w-4" /> : <Bot className="h-4 w-4" />}
                    </div>

                    <div
                      className={`max-w-[80%] sm:max-w-[70%] p-4.5 rounded-2xl text-xs leading-relaxed ${
                        isUser
                          ? "bg-purple-600 text-white rounded-tr-none shadow-md shadow-purple-600/20"
                          : "glass-panel border border-white/10 text-slate-200 rounded-tl-none bg-slate-900/80 backdrop-blur-md shadow-lg"
                      }`}
                    >
                      <p className="whitespace-pre-wrap">{msg.content}</p>

                      {/* Code Snippet Copy Toolbar */}
                      {!isUser && msg.content.includes("```") && (
                        <div className="mt-3 pt-2 border-t border-white/10 flex items-center justify-between text-[10px] text-slate-400">
                          <span className="font-mono">Code Snippet</span>
                          <button
                            onClick={() => handleCopyCode(msg.content, msgId)}
                            className="flex items-center gap-1 hover:text-white transition-all cursor-pointer"
                          >
                            {copiedIndex === msgId ? (
                              <>
                                <Check className="h-3 w-3 text-emerald-400" />
                                <span className="text-emerald-400">Copied!</span>
                              </>
                            ) : (
                              <>
                                <Copy className="h-3 w-3" />
                                <span>Copy Code</span>
                              </>
                            )}
                          </button>
                        </div>
                      )}
                    </div>
                  </div>
                );
              })
            ) : (
              /* Empty State Prompts */
              <div className="h-full flex flex-col items-center justify-center text-center max-w-lg mx-auto p-6 space-y-6">
                <div className="h-16 w-16 rounded-2xl bg-purple-500/10 border border-purple-500/20 flex items-center justify-center text-purple-400 shadow-xl shadow-purple-500/10">
                  <Bot className="h-8 w-8" />
                </div>
                <div className="space-y-2">
                  <h3 className="text-lg font-extrabold text-white">Active Mode: {currentModeObj.label}</h3>
                  <p className="text-xs text-slate-400 leading-relaxed">
                    {currentModeObj.description}
                  </p>
                </div>

                <div className="w-full space-y-2 text-left">
                  {[
                    `Explain ${weakConceptTag} in ${currentModeObj.label} mode`,
                    `Practice question for ${weakConceptTag}`,
                    `Code implementation for ${weakConceptTag}`
                  ].map((prompt, i) => (
                    <button
                      key={i}
                      onClick={() => handleSendMessage(prompt)}
                      className="w-full p-3.5 glass-panel bg-white/3 hover:bg-white/8 border border-white/10 rounded-2xl text-xs text-slate-300 hover:text-white flex items-center justify-between transition-all cursor-pointer"
                    >
                      <span className="flex items-center gap-2.5">
                        <Lightbulb className="h-4 w-4 text-purple-400 shrink-0" />
                        <span>{prompt}</span>
                      </span>
                      <ChevronRight className="h-4 w-4 text-slate-500" />
                    </button>
                  ))}
                </div>
              </div>
            )}

            {/* Typing Indicator */}
            {sending && (
              <div className="flex items-center gap-3">
                <div className="h-8 w-8 rounded-xl bg-gradient-to-tr from-cyan-600 to-emerald-500 flex items-center justify-center text-white shrink-0 shadow-md">
                  <Bot className="h-4 w-4" />
                </div>
                <div className="glass-panel p-3 px-4 rounded-2xl rounded-tl-none border border-white/10 text-xs text-purple-300 flex items-center gap-2 bg-slate-900/90 shadow-lg">
                  <Loader2 className="h-3.5 w-3.5 animate-spin text-purple-400" />
                  <span>AI Tutor is preparing response in {currentModeObj.label} mode...</span>
                </div>
              </div>
            )}

            <div ref={messagesEndRef} />
          </div>

          {/* Input Controls Bar */}
          <div className="p-4 border-t border-white/10 bg-slate-900/80 backdrop-blur-xl">
            <div className="max-w-4xl mx-auto flex items-center gap-2">
              <input
                type="text"
                value={inputText}
                onChange={(e) => setInputText(e.target.value)}
                onKeyDown={(e) => e.key === "Enter" && handleSendMessage()}
                placeholder={`Ask in ${currentModeObj.label} mode (e.g. "Teach me Trees")...`}
                className="flex-1 glass-panel bg-slate-900/90 border border-white/10 rounded-xl px-4 py-3.5 text-xs text-white placeholder:text-slate-500 focus:outline-none focus:border-purple-500/50 transition-all"
              />
              <button
                onClick={() => handleSendMessage()}
                disabled={sending || !inputText.trim()}
                className="py-3.5 px-5 bg-gradient-to-r from-purple-600 to-pink-600 hover:from-purple-500 hover:to-pink-500 disabled:opacity-50 text-white font-extrabold text-xs rounded-xl shadow-lg shadow-purple-600/20 flex items-center gap-2 transition-all cursor-pointer shrink-0"
              >
                <span>Send</span>
                <Send className="h-3.5 w-3.5" />
              </button>
            </div>
          </div>

        </main>
      </div>

      {/* Clear Thread Confirmation Modal */}
      {showClearModal && activeConvId && (
        <div className="fixed inset-0 bg-slate-950/80 backdrop-blur-md z-50 flex items-center justify-center p-4">
          <div className="glass-panel p-6 rounded-2xl border border-white/10 bg-slate-900 max-w-sm w-full space-y-4 shadow-2xl">
            <div className="h-12 w-12 rounded-xl bg-red-500/10 border border-red-500/20 flex items-center justify-center text-red-400">
              <AlertOctagon className="h-6 w-6" />
            </div>
            <div className="space-y-1">
              <h3 className="text-base font-extrabold text-white">Clear Conversation Thread?</h3>
              <p className="text-xs text-slate-400">
                This will permanently delete this discussion thread from your AI Tutor history.
              </p>
            </div>
            <div className="flex items-center justify-end gap-3 pt-2">
              <button
                onClick={() => setShowClearModal(false)}
                className="px-4 py-2 rounded-xl bg-white/5 hover:bg-white/10 text-xs font-bold text-slate-300 transition-all"
              >
                Cancel
              </button>
              <button
                onClick={(e) => {
                  setShowClearModal(false);
                  handleDeleteConv(activeConvId, e as any);
                }}
                className="px-4 py-2 rounded-xl bg-red-600 hover:bg-red-500 text-white text-xs font-extrabold transition-all shadow-lg shadow-red-600/20"
              >
                Delete Thread
              </button>
            </div>
          </div>
        </div>
      )}

    </div>
  );
}
