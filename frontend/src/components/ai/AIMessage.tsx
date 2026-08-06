import React from "react";
import { Bot, User, Sparkles } from "lucide-react";
import MarkdownRenderer from "./MarkdownRenderer";
import MessageToolbar from "./MessageToolbar";

export interface MessageData {
  messageId?: string;
  id?: string;
  role: "user" | "assistant" | "system";
  content: string;
  timestamp?: string;
  metadata?: any;
}

interface AIMessageProps {
  message: MessageData;
  onRegenerate?: () => void;
}

export default function AIMessage({ message, onRegenerate }: AIMessageProps) {
  const isUser = message.role === "user";

  const formattedTime = message.timestamp
    ? new Date(message.timestamp).toLocaleTimeString([], { hour: "2-digit", minute: "2-digit" })
    : new Date().toLocaleTimeString([], { hour: "2-digit", minute: "2-digit" });

  return (
    <div className={`flex gap-3 my-4 group ${isUser ? "justify-end" : "justify-start"}`}>
      {/* Avatar for assistant */}
      {!isUser && (
        <div className="h-8 w-8 rounded-2xl bg-gradient-to-tr from-purple-600 via-indigo-600 to-cyan-600 flex items-center justify-center text-white shrink-0 shadow-lg shadow-purple-500/20 border border-purple-400/30">
          <Bot className="h-4 w-4" />
        </div>
      )}

      {/* Message Content Bubble */}
      <div className={`max-w-[85%] sm:max-w-[78%] ${isUser ? "order-1" : "order-2"}`}>
        <div className="flex items-center gap-2 mb-1 px-1 text-[11px] text-secondary-theme">
          <span className="font-bold text-main-theme flex items-center gap-1">
            {isUser ? "You" : "EduPilot AI"}
            {!isUser && (
              <Sparkles className="h-3 w-3 text-purple-theme inline" />
            )}
          </span>
          <span>•</span>
          <span>{formattedTime}</span>
        </div>

        <div
          className={`p-4 rounded-2xl border shadow-lg ${
            isUser
              ? "bg-gradient-to-r from-purple-700 to-indigo-700 text-white border-purple-500/30 rounded-tr-xs"
              : "glass-panel bg-[var(--glass-bg)] text-main-theme border-[var(--glass-border)] rounded-tl-xs"
          }`}
        >
          {isUser ? (
            <p className="text-xs sm:text-sm font-sans whitespace-pre-wrap leading-relaxed">{message.content}</p>
          ) : (
            <MarkdownRenderer content={message.content} />
          )}

          <MessageToolbar content={message.content} role={message.role} onRegenerate={onRegenerate} />
        </div>
      </div>

      {/* User Avatar */}
      {isUser && (
        <div className="h-8 w-8 rounded-2xl bg-slate-800 border border-white/10 flex items-center justify-center text-slate-300 shrink-0 order-2">
          <User className="h-4 w-4" />
        </div>
      )}
    </div>
  );
}
