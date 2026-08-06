import React, { useRef, useEffect } from "react";
import { AlertTriangle, X } from "lucide-react";
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

  const hasMessages = messages.length > 0;

  return (
    <div className="flex-1 flex flex-col min-h-0 overflow-hidden">
      {/* Error Banner — always on top */}
      {errorMessage && (
        <div className="shrink-0 mx-4 mt-3 p-3 rounded-2xl bg-rose-500/10 border border-rose-500/30 text-rose-200 text-xs flex items-center justify-between gap-3 shadow-lg">
          <div className="flex items-center gap-2">
            <AlertTriangle className="h-4 w-4 text-rose-400 shrink-0" />
            <span>{errorMessage}</span>
          </div>
          <button
            onClick={onClearError}
            className="p-1 text-rose-400 hover:text-white rounded-lg hover:bg-rose-500/20 cursor-pointer shrink-0"
          >
            <X className="h-4 w-4" />
          </button>
        </div>
      )}

      {/* ── Welcome / Messages area ── */}
      {loading ? (
        <div className="flex-1 overflow-y-auto p-4 scrollbar-thin scrollbar-thumb-slate-800">
          <SkeletonLoader count={4} />
        </div>
      ) : !hasMessages ? (
        /* Welcome state — SuggestionChips handles its own scroll */
        <SuggestionChips
          activeModeLabel={activeModeLabel}
          onSelectPrompt={onSelectPrompt}
          studentContext={studentContext}
        />
      ) : (
        /* Chat messages list */
        <div className="flex-1 overflow-y-auto px-4 py-4 space-y-1 scrollbar-thin scrollbar-thumb-slate-800">
          {messages.map((msg, index) => (
            <AIMessage
              key={msg.messageId || msg.id || index}
              message={msg}
              onRegenerate={
                index === messages.length - 1 && msg.role === "assistant"
                  ? onRegenerateLast
                  : undefined
              }
            />
          ))}

          {sending && <TypingIndicator modeLabel={activeModeLabel} />}

          <div ref={bottomRef} />
        </div>
      )}

      {/* Scroll anchor when loading */}
      {(loading || !hasMessages) && <div ref={bottomRef} />}
    </div>
  );
}
