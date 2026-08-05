import React, { useState, useEffect } from "react";
import { Link } from "react-router-dom";
import { 
  Bot, 
  ArrowLeft, 
  Sparkles, 
  Trash2, 
  Menu, 
  X, 
  AlertOctagon 
} from "lucide-react";
import { 
  fetchConversationHistory, 
  createNewConversation, 
  sendChatMessage, 
  deleteConversation, 
  fetchConversationById, 
  fetchStudentContext,
  fetchLLMProviderInfo
} from "../services/api";

import ContextHeader from "../components/ai/ContextHeader";
import LearningModeSelector, { LearningMode, LEARNING_MODES } from "../components/ai/LearningModeSelector";
import ConversationSidebar from "../components/ai/ConversationSidebar";
import AIChatWindow from "../components/ai/AIChatWindow";
import ChatInput from "../components/ai/ChatInput";
import { MessageData } from "../components/ai/AIMessage";

export default function AITutorPage() {
  const [conversations, setConversations] = useState<any[]>([]);
  const [activeConvId, setActiveConvId] = useState<string | null>(null);
  const [currentConv, setCurrentConv] = useState<any>(null);
  const [studentContext, setStudentContext] = useState<any>(null);
  const [activeMode, setActiveMode] = useState<LearningMode>("LEARN");
  const [inputText, setInputText] = useState("");
  const [loading, setLoading] = useState(false);
  const [sending, setSending] = useState(false);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [showClearModal, setShowClearModal] = useState(false);
  const [showMobileSidebar, setShowMobileSidebar] = useState(false);
  const [runtimeModel, setRuntimeModel] = useState<string>("Google Gemini 2.0 Flash");

  const userId = typeof window !== "undefined" ? (localStorage.getItem("edupilot_user_id") || "user_demo") : "user_demo";

  // Initial load of history & context
  useEffect(() => {
    async function loadData() {
      setLoading(true);
      setErrorMessage(null);
      try {
        const [history, context, providerInfo] = await Promise.all([
          fetchConversationHistory(userId),
          fetchStudentContext(userId),
          fetchLLMProviderInfo()
        ]);

        setConversations(history || []);
        if (context) setStudentContext(context);
        if (providerInfo) {
          const modelDisplay = providerInfo.model.startsWith("Google") ? providerInfo.model : `Google Gemini ${providerInfo.model.replace("gemini-", "").replace("-flash", " Flash")}`;
          setRuntimeModel(modelDisplay);
        }

        if (history && history.length > 0) {
          setActiveConvId(history[0].conversationId);
          setCurrentConv(history[0]);
          if (history[0].learningMode) {
            setActiveMode(history[0].learningMode as LearningMode);
          }
        }
      } catch (err: any) {
        setErrorMessage("Could not sync conversation history with backend.");
      } finally {
        setLoading(false);
      }
    }
    loadData();
  }, [userId]);

  // Load active conversation details when activeConvId changes
  useEffect(() => {
    if (activeConvId) {
      async function loadActive() {
        try {
          const details = await fetchConversationById(activeConvId!);
          if (details) {
            setCurrentConv(details);
            if (details.learningMode) {
              setActiveMode(details.learningMode as LearningMode);
            }
          }
        } catch (err) {
          console.warn("Error fetching thread details:", err);
        }
      }
      loadActive();
    }
  }, [activeConvId]);

  const handleNewConversation = async (concept?: string) => {
    setLoading(true);
    setErrorMessage(null);
    try {
      const modeObj = LEARNING_MODES.find(m => m.key === activeMode);
      const title = concept ? `Study: ${concept}` : `${modeObj?.label || "AI"} Session`;
      const newConv = await createNewConversation(userId, title, undefined, concept, activeMode);
      if (newConv) {
        setConversations((prev) => [newConv, ...prev]);
        setActiveConvId(newConv.conversationId);
        setCurrentConv(newConv);
      }
    } catch (err) {
      setErrorMessage("Could not create new chat thread. Please verify backend connection.");
    } finally {
      setLoading(false);
      setShowMobileSidebar(false);
    }
  };

  const handleSendMessage = async (customPrompt?: string) => {
    const promptToSend = customPrompt || inputText;
    if (!promptToSend.trim() || sending) return;

    if (!customPrompt) setInputText("");
    setSending(true);
    setErrorMessage(null);

    // Optimistic user message append
    const userMsg: MessageData = {
      messageId: "temp_u_" + Date.now(),
      role: "user",
      content: promptToSend,
      timestamp: new Date().toISOString()
    };

    if (currentConv) {
      setCurrentConv((prev: any) => ({
        ...prev,
        messages: [...(prev?.messages || []), userMsg]
      }));
    } else {
      // Create temporary local conversation thread container if empty
      setCurrentConv({
        conversationId: "temp_conv_" + Date.now(),
        title: promptToSend.slice(0, 30),
        learningMode: activeMode,
        messages: [userMsg]
      });
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
        } else {
          // Append assistant message response locally if details fetch pending
          const assistantMsg: MessageData = {
            messageId: res.messageId || "temp_a_" + Date.now(),
            role: "assistant",
            content: res.message || res.aiTextResponse || "Response received.",
            timestamp: new Date().toISOString()
          };
          setCurrentConv((prev: any) => ({
            ...prev,
            messages: [...(prev?.messages || []), assistantMsg]
          }));
        }
      }
    } catch (err: any) {
      setErrorMessage("Failed to receive AI response. Check backend connection or API key status.");
    } finally {
      setSending(false);
    }
  };

  const handleDeleteConv = async (convId: string) => {
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

  const handleRenameConv = (convId: string, newTitle: string) => {
    setConversations((prev) =>
      prev.map((c) => (c.conversationId === convId ? { ...c, title: newTitle } : c))
    );
    if (currentConv && currentConv.conversationId === convId) {
      setCurrentConv((prev: any) => ({ ...prev, title: newTitle }));
    }
  };

  const currentModeObj = LEARNING_MODES.find((m) => m.key === activeMode) || LEARNING_MODES[0];

  return (
    <div className="min-h-screen bg-[#0B0F19] text-slate-100 flex flex-col font-sans selection:bg-purple-500/30">
      {/* Top Header Bar */}
      <header className="border-b border-white/10 bg-slate-950/80 backdrop-blur-xl px-4 md:px-6 py-3.5 flex items-center justify-between sticky top-0 z-30 shadow-md">
        <div className="flex items-center gap-3">
          <button
            onClick={() => setShowMobileSidebar(!showMobileSidebar)}
            className="md:hidden p-2 rounded-xl glass-panel text-slate-400 hover:text-white"
          >
            <Menu className="h-5 w-5" />
          </button>

          <Link
            to="/dashboard"
            className="p-2 rounded-xl glass-panel bg-white/5 hover:bg-white/10 text-slate-400 hover:text-white transition-all"
            title="Return to Dashboard"
          >
            <ArrowLeft className="h-4 w-4" />
          </Link>

          <div className="h-9 w-9 rounded-2xl bg-gradient-to-tr from-purple-600 via-pink-600 to-cyan-600 flex items-center justify-center text-white shadow-lg shadow-purple-500/25">
            <Bot className="h-5 w-5" />
          </div>

          <div>
            <h1 className="text-sm font-extrabold tracking-wide text-white flex items-center gap-2">
              <span>EduPilot AI Tutor</span>
              <span className="text-[9px] font-bold uppercase tracking-wider bg-purple-500/20 text-purple-300 border border-purple-500/30 px-2 py-0.5 rounded-full hidden sm:flex items-center gap-1">
                <Sparkles className="h-3 w-3" />
                Adaptive AI Active
              </span>
            </h1>
            <p className="text-[10px] text-slate-400 hidden sm:block">Production Gemini LLM microservice & multi-turn memory</p>
          </div>
        </div>

        <div className="flex items-center gap-3">
          {activeConvId && (
            <button
              onClick={() => setShowClearModal(true)}
              className="px-3 py-1.5 rounded-xl bg-rose-500/10 hover:bg-rose-500/20 text-rose-300 border border-rose-500/20 text-xs font-bold transition-all flex items-center gap-1.5 cursor-pointer"
            >
              <Trash2 className="h-3.5 w-3.5" />
              <span className="hidden sm:inline">Clear Thread</span>
            </button>
          )}

          <div className="flex items-center gap-2 text-xs font-bold text-slate-300 glass-panel px-3 py-1.5 rounded-xl border border-white/10">
            <span className="h-2 w-2 rounded-full bg-emerald-400 animate-pulse" />
            <span className="hidden sm:inline">Gemini Live</span>
          </div>
        </div>
      </header>

      {/* Student Context Header */}
      <ContextHeader studentContext={studentContext} providerName={runtimeModel} />

      {/* Learning Mode Selector Bar */}
      <LearningModeSelector activeMode={activeMode} onModeChange={setActiveMode} />

      {/* Main Content Layout */}
      <div className="flex-1 flex overflow-hidden relative">
        {/* Desktop Sidebar */}
        <ConversationSidebar
          conversations={conversations}
          activeConvId={activeConvId}
          onSelectConv={(id) => setActiveConvId(id)}
          onNewConv={() => handleNewConversation()}
          onDeleteConv={handleDeleteConv}
          onRenameConv={handleRenameConv}
          className="hidden md:flex"
        />

        {/* Mobile Drawer Sidebar */}
        {showMobileSidebar && (
          <div className="fixed inset-0 z-40 md:hidden flex">
            <div className="fixed inset-0 bg-slate-950/80 backdrop-blur-sm" onClick={() => setShowMobileSidebar(false)} />
            <div className="relative w-80 max-w-[85vw] bg-slate-950 z-50">
              <ConversationSidebar
                conversations={conversations}
                activeConvId={activeConvId}
                onSelectConv={(id) => {
                  setActiveConvId(id);
                  setShowMobileSidebar(false);
                }}
                onNewConv={() => handleNewConversation()}
                onDeleteConv={handleDeleteConv}
                onRenameConv={handleRenameConv}
                className="h-full w-full"
              />
            </div>
          </div>
        )}

        {/* Main Chat Area */}
        <main className="flex-1 flex flex-col justify-between bg-slate-950/40 relative overflow-hidden">
          <AIChatWindow
            messages={currentConv?.messages || []}
            loading={loading}
            sending={sending}
            activeModeLabel={currentModeObj.label}
            studentContext={studentContext}
            errorMessage={errorMessage}
            onClearError={() => setErrorMessage(null)}
            onSelectPrompt={(text) => handleSendMessage(text)}
            onRegenerateLast={() => {
              const msgs = currentConv?.messages || [];
              const lastUser = [...msgs].reverse().find((m: any) => m.role === "user");
              if (lastUser) handleSendMessage(lastUser.content);
            }}
          />

          {/* Bottom Fixed Input Bar */}
          <div className="p-4 border-t border-white/10 bg-slate-950/90 backdrop-blur-xl">
            <ChatInput
              value={inputText}
              onChange={setInputText}
              onSend={() => handleSendMessage()}
              disabled={sending}
              activeModeLabel={currentModeObj.label}
            />
          </div>
        </main>
      </div>

      {/* Clear Thread Confirmation Modal */}
      {showClearModal && activeConvId && (
        <div className="fixed inset-0 bg-slate-950/80 backdrop-blur-md z-50 flex items-center justify-center p-4">
          <div className="glass-panel p-6 rounded-2xl border border-white/10 bg-slate-900 max-w-sm w-full space-y-4 shadow-2xl">
            <div className="h-12 w-12 rounded-xl bg-rose-500/10 border border-rose-500/20 flex items-center justify-center text-rose-400">
              <AlertOctagon className="h-6 w-6" />
            </div>
            <div className="space-y-1">
              <h3 className="text-base font-extrabold text-white">Clear Conversation Thread?</h3>
              <p className="text-xs text-slate-400">
                This will permanently delete this thread from your EduPilot AI profile history.
              </p>
            </div>
            <div className="flex items-center justify-end gap-3 pt-2">
              <button
                onClick={() => setShowClearModal(false)}
                className="px-4 py-2 rounded-xl bg-white/5 hover:bg-white/10 text-xs font-bold text-slate-300 transition-all cursor-pointer"
              >
                Cancel
              </button>
              <button
                onClick={() => {
                  setShowClearModal(false);
                  handleDeleteConv(activeConvId);
                }}
                className="px-4 py-2 rounded-xl bg-rose-600 hover:bg-rose-500 text-white text-xs font-extrabold transition-all shadow-lg shadow-rose-600/20 cursor-pointer"
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
