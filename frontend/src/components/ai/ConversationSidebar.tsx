import React, { useState } from "react";
import { Plus, Search, MessageSquare, Sparkles, History, Bot } from "lucide-react";
import ConversationCard from "./ConversationCard";

interface ConversationSidebarProps {
  conversations: any[];
  activeConvId: string | null;
  onSelectConv: (id: string) => void;
  onNewConv: () => void;
  onDeleteConv: (id: string) => void;
  onRenameConv: (id: string, newTitle: string) => void;
  className?: string;
}

export default function ConversationSidebar({
  conversations,
  activeConvId,
  onSelectConv,
  onNewConv,
  onDeleteConv,
  onRenameConv,
  className = ""
}: ConversationSidebarProps) {
  const [searchTerm, setSearchTerm] = useState("");

  const filteredConversations = conversations.filter((c) =>
    (c.title || "Untitled Chat").toLowerCase().includes(searchTerm.toLowerCase())
  );

  return (
    <aside className={`w-80 border-r border-white/10 bg-slate-950/80 backdrop-blur-xl flex flex-col h-full ${className}`}>
      {/* Header & New Chat Button */}
      <div className="p-4 space-y-3 border-b border-white/10">
        <button
          onClick={onNewConv}
          className="w-full py-2.5 px-4 rounded-xl bg-gradient-to-r from-purple-600 via-indigo-600 to-cyan-600 hover:from-purple-500 hover:to-cyan-500 text-white font-bold text-xs shadow-lg shadow-purple-500/20 transition-all flex items-center justify-center gap-2 cursor-pointer border border-white/20 hover:scale-[1.01]"
        >
          <Plus className="h-4 w-4" />
          <span>New AI Study Thread</span>
        </button>

        {/* Search input */}
        <div className="relative">
          <Search className="h-3.5 w-3.5 absolute left-3 top-1/2 -translate-y-1/2 text-slate-400" />
          <input
            type="text"
            placeholder="Search chat history..."
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
            className="w-full bg-slate-900 border border-white/10 rounded-xl pl-9 pr-3 py-1.5 text-xs text-white placeholder-slate-500 focus:outline-none focus:border-purple-500/50"
          />
        </div>
      </div>

      {/* Conversations List */}
      <div className="flex-1 overflow-y-auto p-3 space-y-2 scrollbar-thin scrollbar-thumb-slate-800">
        <div className="flex items-center justify-between px-1 text-[10px] uppercase font-bold tracking-wider text-slate-400">
          <span className="flex items-center gap-1">
            <History className="h-3 w-3 text-purple-400" />
            Previous Threads ({filteredConversations.length})
          </span>
        </div>

        {filteredConversations.length === 0 ? (
          <div className="p-6 text-center space-y-2">
            <MessageSquare className="h-8 w-8 text-slate-600 mx-auto" />
            <p className="text-xs text-slate-400 font-medium">
              {searchTerm ? "No matching conversations found." : "No saved chat history yet."}
            </p>
            <p className="text-[11px] text-slate-400">Start a new study session to begin asking questions!</p>
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

      {/* Footer Info */}
      <div className="p-3 border-t border-white/10 bg-slate-900/60 flex items-center justify-between text-[11px] text-slate-400">
        <div className="flex items-center gap-1.5">
          <Bot className="h-3.5 w-3.5 text-purple-400" />
          <span>Gemini Pro Microservice</span>
        </div>
        <span className="text-emerald-400 font-bold flex items-center gap-1">
          <span className="h-1.5 w-1.5 rounded-full bg-emerald-400 animate-pulse" />
          Online
        </span>
      </div>
    </aside>
  );
}
