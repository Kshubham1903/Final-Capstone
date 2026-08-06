import React, { useRef, useEffect } from "react";
import { AlertTriangle, RefreshCw, X } from "lucide-react";
import AIMessage, { MessageData } from "./AIMessage";
import TypingIndicator from "./TypingIndicator";
import SkeletonLoader from "./SkeletonLoader";
import SuggestionChips from "./SuggestionChips";

interface AIChatWindowProps {
  messages: MessageData[];
  loading: boolean;
  sending: boolean;
  activeModeLabel: string;
  studentContext?: any;
  errorMessage?: string | null;
  onClearError: () => void;
  onSelectPrompt: (text: string) => void;
  onRegenerateLast: () => void;
}

export default function AIChatWindow({
  messages,
  loading,
  sending,
  activeModeLabel,
  studentContext,
  errorMessage,
  onClearError,
  onSelectPrompt,
  onRegenerateLast
}: AIChatWindowProps) {
  const bottomRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    bottomRef.current?.scrollIntoView({ behavior: "smooth" });
  }, [messages, sending]);

  return (
    <div className="flex-1 overflow-y-auto p-4 md:p-6 space-y-4 scrollbar-thin scrollbar-thumb-slate-800">
      {/* Error Banner */}
      {errorMessage && (
        <div className="max-w-3xl mx-auto p-3.5 rounded-2xl bg-rose-500/10 border border-rose-500/30 text-rose-200 text-xs flex items-center justify-between gap-3 shadow-lg">
          <div className="flex items-center gap-2">
            <AlertTriangle className="h-4 w-4 text-rose-400 shrink-0" />
            <span>{errorMessage}</span>
          </div>
          <button
            onClick={onClearError}
            className="p-1 text-rose-400 hover:text-white rounded-lg hover:bg-rose-500/20 cursor-pointer"
          >
            <X className="h-4 w-4" />
          </button>
        </div>
      )}

      {/* Loading state */}
      {loading ? (
        <SkeletonLoader count={4} />
      ) : messages.length === 0 ? (
        <SuggestionChips activeModeLabel={activeModeLabel} onSelectPrompt={onSelectPrompt} />
      ) : (
        messages.map((msg, index) => (
          <AIMessage
            key={msg.messageId || msg.id || index}
            message={msg}
            onRegenerate={index === messages.length - 1 && msg.role === "assistant" ? onRegenerateLast : undefined}
          />
        ))
      )}

      {/* Sending thinking indicator */}
      {sending && <TypingIndicator modeLabel={activeModeLabel} />}

      <div ref={bottomRef} />
    </div>
  );
}
