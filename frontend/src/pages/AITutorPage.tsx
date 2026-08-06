import React, { useState, useEffect } from "react";
import {
  Bot,
  Sparkles,
  Trash2,
  Menu,
  X,
  AlertOctagon,
  Maximize2,
  Minimize2,
  BookOpen,
  ChevronDown,
  ChevronUp
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

import Layout from "../components/Layout";
import ContextHeader from "../components/ai/ContextHeader";
import LearningModeSelector, { LearningMode, LEARNING_MODES } from "../components/ai/LearningModeSelector";
import ConversationSidebar from "../components/ai/ConversationSidebar";
import AIChatWindow from "../components/ai/AIChatWindow";
import ChatInput from "../components/ai/ChatInput";
import { MessageData } from "../components/ai/AIMessage";

export default function AITutorPage() {
  /* ── Data state (unchanged from original) ── */
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

  /* ── UI layout state (new) ── */
  const [threadCollapsed, setThreadCollapsed] = useState(false);
  const [focusMode, setFocusMode] = useState(false);
  const [contextExpanded, setContextExpanded] = useState(false);
  const [modeSelectorVisible, setModeSelectorVisible] = useState(true);

  const userId =
    typeof window !== "undefined"
      ? localStorage.getItem("edupilot_user_id") || "user_demo"
      : "user_demo";

  /* ── Initial load (unchanged logic) ── */
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
          const modelDisplay = providerInfo.model.startsWith("Google")
            ? providerInfo.model
            : `Google Gemini ${providerInfo.model.replace("gemini-", "").replace("-flash", " Flash")}`;
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

  /* ── Load active conversation details (unchanged logic) ── */
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

  /* ── Handlers (unchanged business logic) ── */
  const handleNewConversation = async (concept?: string) => {
    setLoading(true);
    setErrorMessage(null);
    try {
      const modeObj = LEARNING_MODES.find((m: any) => m.key === activeMode);
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

  const currentModeObj = LEARNING_MODES.find((m: any) => m.key === activeMode) || LEARNING_MODES[0];
  const hasMessages = (currentConv?.messages || []).length > 0;

  /* ── Focus mode toggles ── */
  const enterFocusMode = () => {
    setFocusMode(true);
    setThreadCollapsed(true);
  };
  const exitFocusMode = () => {
    setFocusMode(false);
    setThreadCollapsed(false);
  };

  /* ────────────────────────────────────────────────
     RENDER
  ──────────────────────────────────────────────── */
  return (
    <Layout>
      {/*
        Root container fills the Layout's main content area.
        h-[calc(100vh-68px)] matches Layout's max-h on the <main> tag.
      */}
      <div className="flex flex-col h-[calc(100vh-68px)] -m-6 md:-m-8 overflow-hidden">

        {/* ══════════════════════════════
            MAIN WORKSPACE ROW
            Left: Thread panel  |  Right: Chat
        ══════════════════════════════ */}
        <div className="flex flex-1 overflow-hidden">

          {/* ── Thread Panel (Desktop) ── */}
          <div className="hidden md:flex">
            <ConversationSidebar
              conversations={conversations}
              activeConvId={activeConvId}
              onSelectConv={(id: string) => setActiveConvId(id)}
              onNewConv={() => handleNewConversation()}
              onDeleteConv={handleDeleteConv}
              onRenameConv={handleRenameConv}
              collapsed={threadCollapsed || focusMode}
              onToggleCollapse={() => {
                if (focusMode) {
                  exitFocusMode();
                } else {
                  setThreadCollapsed((p) => !p);
                }
              }}
            />
          </div>

          {/* ── Chat Workspace (RIGHT) ── */}
          <div className="flex-1 flex flex-col min-w-0 overflow-hidden bg-[var(--glass-bg)]">

            {/* ── Chat Header Bar ── */}
            <div className="shrink-0 border-b border-[var(--glass-border)] bg-[var(--glass-bg)] backdrop-blur-xl px-4 py-3 space-y-2">

              {/* Title row */}
              <div className="flex items-center justify-between gap-3">
                {/* Left: mobile menu + title */}
                <div className="flex items-center gap-3 min-w-0">
                  {/* Mobile hamburger */}
                  <button
                    onClick={() => setShowMobileSidebar(true)}
                    className="md:hidden p-2 rounded-xl bg-white/5 border border-[var(--glass-border)] text-secondary-theme hover:text-main-theme transition-all cursor-pointer shrink-0"
                    title="Toggle Thread History"
                  >
                    <Menu className="h-4 w-4" />
                  </button>

                  {/* AI Avatar */}
                  <div className="h-9 w-9 rounded-2xl bg-gradient-to-tr from-purple-600 via-pink-600 to-cyan-600 flex items-center justify-center text-white shadow-lg shadow-purple-500/25 shrink-0 border border-white/10">
                    <Bot className="h-5 w-5" />
                  </div>

                  <div className="min-w-0">
                    <h1 className="text-sm font-extrabold tracking-wide text-main-theme flex items-center gap-2 flex-wrap">
                      <span>AI Tutor Companion</span>
                      <span className="hidden sm:inline-flex items-center gap-1 text-[9px] font-bold uppercase tracking-wider bg-purple-500/20 text-purple-300 border border-purple-500/30 px-2 py-0.5 rounded-full">
                        <Sparkles className="h-2.5 w-2.5 text-purple-400" />
                        Adaptive AI Active
                      </span>
                    </h1>
                    <p className="text-[10px] text-secondary-theme hidden sm:block truncate">
                      Gemini microservice · multi-turn memory · {currentModeObj.label}
                    </p>
                  </div>
                </div>

                {/* Right: actions */}
                <div className="flex items-center gap-2 shrink-0">
                  {/* Gemini live badge */}
                  <div className="hidden sm:flex items-center gap-1.5 text-[11px] font-bold text-main-theme glass-panel px-2.5 py-1 rounded-xl border border-[var(--glass-border)]">
                    <span className="h-1.5 w-1.5 rounded-full bg-emerald-400 animate-pulse" />
                    <span>Gemini Live</span>
                  </div>

                  {/* Mode selector toggle on mobile */}
                  <button
                    onClick={() => setModeSelectorVisible((p) => !p)}
                    title="Toggle learning mode strip"
                    className="sm:hidden p-2 rounded-xl bg-white/5 border border-[var(--glass-border)] text-secondary-theme hover:text-main-theme transition-all cursor-pointer"
                  >
                    <BookOpen className="h-4 w-4" />
                  </button>

                  {/* Context toggle */}
                  <button
                    onClick={() => setContextExpanded((p) => !p)}
                    title={contextExpanded ? "Hide student context" : "Show student context"}
                    className="p-2 rounded-xl bg-white/5 border border-[var(--glass-border)] text-secondary-theme hover:text-main-theme transition-all cursor-pointer"
                  >
                    {contextExpanded ? (
                      <ChevronUp className="h-4 w-4" />
                    ) : (
                      <ChevronDown className="h-4 w-4" />
                    )}
                  </button>

                  {/* Focus mode toggle */}
                  <button
                    onClick={focusMode ? exitFocusMode : enterFocusMode}
                    title={focusMode ? "Exit Focus Mode" : "Enter Focus Mode — expand chat"}
                    className={`p-2 rounded-xl border transition-all cursor-pointer ${
                      focusMode
                        ? "bg-purple-500/20 border-purple-500/40 text-purple-300 hover:bg-purple-500/30"
                        : "bg-white/5 border-[var(--glass-border)] text-secondary-theme hover:text-main-theme"
                    }`}
                  >
                    {focusMode ? (
                      <Minimize2 className="h-4 w-4" />
                    ) : (
                      <Maximize2 className="h-4 w-4" />
                    )}
                  </button>

                  {/* Clear thread */}
                  {activeConvId && (
                    <button
                      onClick={() => setShowClearModal(true)}
                      className="px-3 py-1.5 rounded-xl bg-rose-500/10 hover:bg-rose-500/20 text-rose-300 border border-rose-500/20 text-[11px] font-bold transition-all flex items-center gap-1.5 cursor-pointer"
                    >
                      <Trash2 className="h-3.5 w-3.5" />
                      <span className="hidden sm:inline">Clear Thread</span>
                    </button>
                  )}
                </div>
              </div>

              {/* ── Collapsible Student Context Strip ── */}
              {contextExpanded && (
                <ContextHeader
                  studentContext={studentContext}
                  providerName={runtimeModel}
                  compact
                />
              )}

              {/* ── Learning Mode Chips ── */}
              {(!hasMessages || modeSelectorVisible) && (
                <div
                  className={`transition-all duration-200 overflow-hidden ${
                    hasMessages && !modeSelectorVisible ? "max-h-0 opacity-0" : "max-h-20 opacity-100"
                  }`}
                >
                  <LearningModeSelector activeMode={activeMode} onModeChange={setActiveMode} />
                </div>
              )}

              {/* Show chips toggle when messages exist */}
              {hasMessages && (
                <button
                  onClick={() => setModeSelectorVisible((p) => !p)}
                  className="hidden sm:flex items-center gap-1 text-[10px] text-secondary-theme hover:text-main-theme transition-colors cursor-pointer w-full justify-center py-0.5"
                >
                  {modeSelectorVisible ? (
                    <>
                      <ChevronUp className="h-3 w-3" />
                      <span>Hide mode selector</span>
                    </>
                  ) : (
                    <>
                      <ChevronDown className="h-3 w-3" />
                      <span>Show mode selector · Active: <strong className="text-purple-theme">{currentModeObj.label}</strong></span>
                    </>
                  )}
                </button>
              )}
            </div>

            {/* ── Chat Area ── */}
            <AIChatWindow
              messages={currentConv?.messages || []}
              loading={loading}
              sending={sending}
              activeModeLabel={currentModeObj.label}
              studentContext={studentContext}
              errorMessage={errorMessage}
              onClearError={() => setErrorMessage(null)}
              onSelectPrompt={(text: string) => handleSendMessage(text)}
              onRegenerateLast={() => {
                const msgs = currentConv?.messages || [];
                const lastUser = [...msgs].reverse().find((m: any) => m.role === "user");
                if (lastUser) handleSendMessage(lastUser.content);
              }}
            />

            {/* ── Sticky Input Bar ── */}
            <div className="shrink-0 border-t border-[var(--glass-border)] bg-[var(--glass-bg)] backdrop-blur-xl p-3">
              <ChatInput
                value={inputText}
                onChange={setInputText}
                onSend={() => handleSendMessage()}
                disabled={sending}
                activeModeLabel={currentModeObj.label}
              />
            </div>
          </div>
        </div>

        {/* ══════════════════════════════
            MOBILE DRAWER SIDEBAR
        ══════════════════════════════ */}
        {showMobileSidebar && (
          <div className="fixed inset-0 z-50 md:hidden flex">
            <div
              className="fixed inset-0 bg-black/60 backdrop-blur-sm"
              onClick={() => setShowMobileSidebar(false)}
            />
            <div className="relative w-80 max-w-[85vw] glass-panel bg-[var(--glass-hover-bg)] z-50 h-full flex flex-col">
              {/* Drawer close button */}
              <div className="flex items-center justify-between px-4 py-3 border-b border-[var(--glass-border)] shrink-0">
                <span className="text-xs font-bold text-main-theme flex items-center gap-2">
                  <Bot className="h-4 w-4 text-purple-theme" />
                  Study Threads
                </span>
                <button
                  onClick={() => setShowMobileSidebar(false)}
                  className="p-1.5 rounded-xl bg-white/5 text-secondary-theme hover:text-main-theme cursor-pointer"
                >
                  <X className="h-4 w-4" />
                </button>
              </div>
              <ConversationSidebar
                conversations={conversations}
                activeConvId={activeConvId}
                onSelectConv={(id: string) => {
                  setActiveConvId(id);
                  setShowMobileSidebar(false);
                }}
                onNewConv={() => handleNewConversation()}
                onDeleteConv={handleDeleteConv}
                onRenameConv={handleRenameConv}
                className="flex-1 overflow-hidden"
              />
            </div>
          </div>
        )}

        {/* ══════════════════════════════
            CLEAR THREAD MODAL
        ══════════════════════════════ */}
        {showClearModal && activeConvId && (
          <div className="fixed inset-0 bg-black/70 backdrop-blur-md z-50 flex items-center justify-center p-4">
            <div className="glass-panel p-6 rounded-2xl border border-[var(--glass-border)] max-w-sm w-full space-y-4 shadow-2xl">
              <div className="h-12 w-12 rounded-xl bg-rose-500/10 border border-rose-500/20 flex items-center justify-center text-rose-400">
                <AlertOctagon className="h-6 w-6" />
              </div>
              <div className="space-y-1">
                <h3 className="text-base font-extrabold text-main-theme">Clear Conversation Thread?</h3>
                <p className="text-xs text-secondary-theme">
                  This will permanently delete this thread from your EduPilot AI profile history.
                </p>
              </div>
              <div className="flex items-center justify-end gap-3 pt-2">
                <button
                  onClick={() => setShowClearModal(false)}
                  className="px-4 py-2 rounded-xl bg-white/5 hover:bg-white/10 text-xs font-bold text-secondary-theme hover:text-main-theme transition-all cursor-pointer"
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
    </Layout>
  );
}
