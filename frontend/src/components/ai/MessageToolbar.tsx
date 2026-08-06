import React, { useState } from "react";
import { Copy, Check, ThumbsUp, ThumbsDown, RotateCcw, Volume2, VolumeX } from "lucide-react";

interface MessageToolbarProps {
  content: string;
  role: "user" | "assistant" | "system";
  onRegenerate?: () => void;
}

export default function MessageToolbar({ content, role, onRegenerate }: MessageToolbarProps) {
  const [copied, setCopied] = useState(false);
  const [liked, setLiked] = useState<boolean | null>(null);
  const [speaking, setSpeaking] = useState(false);

  const handleCopy = () => {
    navigator.clipboard.writeText(content);
    setCopied(true);
    setTimeout(() => setCopied(false), 2000);
  };

  const handleSpeak = () => {
    if (typeof window === "undefined" || !("speechSynthesis" in window)) return;

    if (speaking) {
      window.speechSynthesis.cancel();
      setSpeaking(false);
    } else {
      window.speechSynthesis.cancel();
      const utterance = new SpeechSynthesisUtterance(content.replace(/```[\s\S]*?```/g, "Code snippet omitted."));
      utterance.onend = () => setSpeaking(false);
      utterance.onerror = () => setSpeaking(false);
      window.speechSynthesis.speak(utterance);
      setSpeaking(true);
    }
  };

  return (
    <div className="flex items-center gap-1 mt-2 text-[11px] text-slate-400">
      {/* Copy button */}
      <button
        onClick={handleCopy}
        className="p-1 px-2 rounded-lg bg-white/5 hover:bg-white/10 hover:text-white transition-all flex items-center gap-1 cursor-pointer"
        title="Copy response text"
      >
        {copied ? (
          <>
            <Check className="h-3 w-3 text-emerald-400" />
            <span className="text-emerald-400 font-semibold">Copied</span>
          </>
        ) : (
          <>
            <Copy className="h-3 w-3" />
            <span>Copy</span>
          </>
        )}
      </button>

      {/* Speak text */}
      <button
        onClick={handleSpeak}
        className={`p-1 px-2 rounded-lg transition-all flex items-center gap-1 cursor-pointer ${
          speaking ? "bg-purple-500/20 text-purple-300 font-bold border border-purple-500/30" : "bg-white/5 hover:bg-white/10 hover:text-white"
        }`}
        title="Listen to audio narration"
      >
        {speaking ? (
          <>
            <VolumeX className="h-3 w-3 text-purple-400" />
            <span>Stop Audio</span>
          </>
        ) : (
          <>
            <Volume2 className="h-3 w-3" />
            <span>Read Aloud</span>
          </>
        )}
      </button>

      {/* Feedback & Regenerate for assistant messages */}
      {role === "assistant" && (
        <>
          <span className="text-slate-700 mx-0.5">•</span>

          <button
            onClick={() => setLiked(liked === true ? null : true)}
            className={`p-1 rounded-lg transition-colors cursor-pointer ${
              liked === true ? "text-emerald-400 bg-emerald-500/10" : "hover:text-emerald-400 hover:bg-white/5"
            }`}
            title="Helpful response"
          >
            <ThumbsUp className="h-3 w-3" />
          </button>

          <button
            onClick={() => setLiked(liked === false ? null : false)}
            className={`p-1 rounded-lg transition-colors cursor-pointer ${
              liked === false ? "text-rose-400 bg-rose-500/10" : "hover:text-rose-400 hover:bg-white/5"
            }`}
            title="Not helpful"
          >
            <ThumbsDown className="h-3 w-3" />
          </button>

          {onRegenerate && (
            <button
              onClick={onRegenerate}
              className="p-1 px-2 rounded-lg bg-white/5 hover:bg-white/10 hover:text-white transition-all flex items-center gap-1 cursor-pointer ml-auto"
              title="Regenerate response"
            >
              <RotateCcw className="h-3 w-3" />
              <span>Retry</span>
            </button>
          )}
        </>
      )}
    </div>
  );
}
