import React, { useRef, useEffect } from "react";
import { Send, Sparkles, CornerDownLeft, Eraser } from "lucide-react";

interface ChatInputProps {
  value: string;
  onChange: (val: string) => void;
  onSend: () => void;
  disabled?: boolean;
  activeModeLabel?: string;
}

export default function ChatInput({
  value,
  onChange,
  onSend,
  disabled = false,
  activeModeLabel
}: ChatInputProps) {
  const textareaRef = useRef<HTMLTextAreaElement>(null);

  // Auto resize textarea height based on content
  useEffect(() => {
    if (textareaRef.current) {
      textareaRef.current.style.height = "auto";
      textareaRef.current.style.height = `${Math.min(textareaRef.current.scrollHeight, 180)}px`;
    }
  }, [value]);

  const handleKeyDown = (e: React.KeyboardEvent<HTMLTextAreaElement>) => {
    if (e.key === "Enter" && !e.shiftKey) {
      e.preventDefault();
      if (value.trim() && !disabled) {
        onSend();
      }
    }
  };

  return (
    <div className="max-w-4xl mx-auto space-y-2">
      {/* Input Box Container */}
      <div className={`relative rounded-2xl border transition-all duration-200 glass-panel bg-[var(--glass-bg)] backdrop-blur-xl ${
        disabled
          ? "border-[var(--glass-border)] opacity-60"
          : "border-purple-500/30 focus-within:border-purple-500 focus-within:ring-2 focus-within:ring-purple-500/20 shadow-xl shadow-purple-950/30"
      }`}>
        <textarea
          ref={textareaRef}
          value={value}
          onChange={(e) => onChange(e.target.value)}
          onKeyDown={handleKeyDown}
          placeholder={`Ask EduPilot AI anything about your course... (${activeModeLabel || "Adaptive"} mode)`}
          disabled={disabled}
          rows={1}
          className="w-full bg-transparent text-main-theme text-xs sm:text-sm placeholder-secondary-theme px-4 py-3.5 pr-24 focus:outline-none resize-none max-h-44"
        />

        {/* Action icons right-side */}
        <div className="absolute right-2.5 bottom-2.5 flex items-center gap-1.5">
          {value.trim() && (
            <button
              onClick={() => onChange("")}
              disabled={disabled}
              className="p-1.5 text-secondary-theme hover:text-main-theme hover:bg-white/10 rounded-xl transition-colors cursor-pointer"
              title="Clear text"
            >
              <Eraser className="h-4 w-4" />
            </button>
          )}

          <button
            onClick={() => onSend()}
            disabled={!value.trim() || disabled}
            className={`p-2 rounded-xl transition-all duration-200 flex items-center justify-center cursor-pointer ${
              value.trim() && !disabled
                ? "bg-gradient-to-r from-purple-600 via-indigo-600 to-cyan-600 text-white shadow-lg shadow-purple-500/30 hover:scale-105"
                : "bg-white/5 text-secondary-theme opacity-50 cursor-not-allowed"
            }`}
            title="Send message (Enter)"
          >
            <Send className="h-4 w-4" />
          </button>
        </div>
      </div>

      {/* Keyboard hints footer */}
      <div className="flex items-center justify-between px-2 text-[10px] text-secondary-theme">
        <span className="flex items-center gap-1">
          <Sparkles className="h-3 w-3 text-purple-theme" />
          <span>Active Mode: <strong className="text-purple-theme">{activeModeLabel || "Adaptive Mastery"}</strong></span>
        </span>

        <span className="hidden sm:flex items-center gap-1 font-mono text-secondary-theme">
          <span>Press</span>
          <kbd className="px-1.5 py-0.5 rounded bg-white/5 border border-[var(--glass-border)] text-main-theme font-sans">Enter ↵</kbd>
          <span>to send,</span>
          <kbd className="px-1.5 py-0.5 rounded bg-white/5 border border-[var(--glass-border)] text-main-theme font-sans">Shift + Enter</kbd>
          <span>for new line</span>
        </span>
      </div>
    </div>
  );
}
