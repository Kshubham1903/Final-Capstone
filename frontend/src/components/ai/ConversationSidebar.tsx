import React, { useState } from "react";
import { Plus, Search, MessageSquare, History, Bot, ChevronLeft, ChevronRight, X } from "lucide-react";
import ConversationCard from "./ConversationCard";

interface ConversationSidebarProps {
  conversations: any[];
  activeConvId: string | null;
  onSelectConv: (id: string) => void;
  onNewConv: () => void;
  onDeleteConv: (id: string) => void;
  onRenameConv: (id: string, newTitle: string) => void;
  className?: string;
  collapsed?: boolean;
  onToggleCollapse?: () => void;
}

export default function ConversationSidebar({
  conversations,
  activeConvId,
  onSelectConv,
  onNewConv,
  onDeleteConv,
  onRenameConv,
  className = "",
  collapsed = false,
  onToggleCollapse
}: ConversationSidebarProps) {
  const [searchTerm, setSearchTerm] = useState("");

  const filteredConversations = conversations.filter((c) =>
    (c.title || "Untitled Chat").toLowerCase().includes(searchTerm.toLowerCase())
  );

  /* ── COLLAPSED STATE: narrow icon-only strip ── */
  if (collapsed) {
    return (
      <aside
        className={`relative flex flex-col items-center gap-2 py-3 border-r border-[var(--glass-border)] bg-[var(--glass-bg)] backdrop-blur-xl transition-all duration-300 ${className}`}
        style={{ width: 52 }}
      >
        {/* Expand toggle */}
        <button
          onClick={onToggleCollapse}
          title="Expand threads panel"
          className="p-2 rounded-xl bg-white/5 hover:bg-purple-500/20 border border-[var(--glass-border)] text-secondary-theme hover:text-purple-300 transition-all cursor-pointer"
        >
          <ChevronRight className="h-4 w-4" />
        </button>

        {/* New thread icon */}
        <button
          onClick={onNewConv}
          title="New AI Study Thread"
          className="p-2 rounded-xl bg-gradient-to-br from-purple-600 to-indigo-600 text-white shadow-lg shadow-purple-500/20 hover:scale-105 transition-all cursor-pointer border border-white/20"
        >
          <Plus className="h-4 w-4" />
        </button>

        <div className="w-full border-t border-[var(--glass-border)] my-1" />

        {/* Thread dots */}
        <div className="flex flex-col items-center gap-1.5 flex-1 overflow-hidden w-full px-2">
          {conversations.slice(0, 10).map((conv) => (
            <button
              key={conv.conversationId}
              onClick={() => onSelectConv(conv.conversationId)}
              title={conv.title || "Study Session"}
              className={`w-7 h-7 rounded-lg flex items-center justify-center transition-all cursor-pointer text-[10px] font-bold shrink-0 ${
                activeConvId === conv.conversationId
                  ? "bg-purple-600/40 text-purple-200 border border-purple-500/50"
                  : "bg-white/5 text-secondary-theme hover:bg-white/10 hover:text-main-theme border border-transparent"
              }`}
            >
              {(conv.title || "S").charAt(0).toUpperCase()}
            </button>
          ))}
        </div>

        {/* Online indicator */}
        <div className="flex flex-col items-center gap-1 pb-1">
          <Bot className="h-3.5 w-3.5 text-purple-theme opacity-60" />
          <span className="h-1.5 w-1.5 rounded-full bg-emerald-400 animate-pulse" />
        </div>
      </aside>
    );
  }

  /* ── EXPANDED STATE: full panel ── */
  return (
    <aside
      className={`flex flex-col h-full border-r border-[var(--glass-border)] bg-[var(--glass-bg)] backdrop-blur-xl transition-all duration-300 ${className}`}
      style={{ width: 272 }}
    >
      {/* Header with collapse toggle */}
      <div className="p-3 space-y-2.5 border-b border-[var(--glass-border)] shrink-0">
        <div className="flex items-center justify-between gap-2">
          <button
            onClick={onNewConv}
            className="flex-1 py-2 px-3 rounded-xl bg-gradient-to-r from-purple-600 via-indigo-600 to-cyan-600 hover:from-purple-500 hover:to-cyan-500 text-white font-bold text-[11px] shadow-lg shadow-purple-500/20 transition-all flex items-center justify-center gap-1.5 cursor-pointer border border-white/20 hover:scale-[1.01]"
          >
            <Plus className="h-3.5 w-3.5" />
            <span>New Study Thread</span>
          </button>

          {onToggleCollapse && (
            <button
              onClick={onToggleCollapse}
              title="Collapse threads panel"
              className="p-2 rounded-xl bg-white/5 hover:bg-white/10 border border-[var(--glass-border)] text-secondary-theme hover:text-main-theme transition-all cursor-pointer shrink-0"
            >
              <ChevronLeft className="h-4 w-4" />
            </button>
          )}
        </div>

        {/* Search */}
        <div className="relative">
          <Search className="h-3.5 w-3.5 absolute left-3 top-1/2 -translate-y-1/2 text-secondary-theme pointer-events-none" />
          <input
            type="text"
            placeholder="Search threads..."
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
            className="w-full glass-input rounded-xl pl-8 pr-8 py-1.5 text-[11px] text-main-theme placeholder-secondary-theme focus:outline-none focus:border-purple-500/50"
          />
          {searchTerm && (
            <button
              onClick={() => setSearchTerm("")}
              className="absolute right-2.5 top-1/2 -translate-y-1/2 text-secondary-theme hover:text-main-theme cursor-pointer"
            >
              <X className="h-3 w-3" />
            </button>
          )}
        </div>
      </div>

      {/* Thread list */}
      <div className="flex-1 overflow-y-auto p-2.5 space-y-1.5 scrollbar-thin scrollbar-thumb-slate-800">
        <div className="flex items-center justify-between px-1 pb-1 text-[10px] uppercase font-bold tracking-wider text-secondary-theme">
          <span className="flex items-center gap-1">
            <History className="h-3 w-3 text-purple-theme" />
            Threads ({filteredConversations.length})
          </span>
        </div>

        {filteredConversations.length === 0 ? (
          <div className="p-6 text-center space-y-2">
            <MessageSquare className="h-8 w-8 text-secondary-theme mx-auto opacity-40" />
            <p className="text-[11px] text-secondary-theme font-medium">
              {searchTerm ? "No matching threads." : "No saved threads yet."}
            </p>
            <p className="text-[10px] text-secondary-theme opacity-70">
              Start a new study session above!
            </p>
          </div>
        ) : (
          filteredConversations.map((conv) => (
            <ConversationCard
              key={conv.conversationId}
              conversation={conv}
              isActive={activeConvId === conv.conversationId}
              onSelect={() => onSelectConv(conv.conversationId)}
              onDelete={() => onDeleteConv(conv.conversationId)}
              onRename={(newTitle) => onRenameConv(conv.conversationId, newTitle)}
            />
          ))
        )}
      </div>

      {/* Footer */}
      <div className="px-3 py-2.5 border-t border-[var(--glass-border)] bg-[var(--glass-hover-bg)] flex items-center justify-between text-[11px] text-secondary-theme shrink-0">
        <div className="flex items-center gap-1.5">
          <Bot className="h-3.5 w-3.5 text-purple-theme" />
          <span>Gemini Microservice</span>
        </div>
        <span className="text-emerald-theme font-bold flex items-center gap-1">
          <span className="h-1.5 w-1.5 rounded-full bg-emerald-400 animate-pulse" />
          Online
        </span>
      </div>
    </aside>
  );
}
