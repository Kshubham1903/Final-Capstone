import React, { useState } from "react";
import { Check, Copy, Code } from "lucide-react";

interface MarkdownRendererProps {
  content: string;
  className?: string;
}

export default function MarkdownRenderer({ content, className = "" }: MarkdownRendererProps) {
  const [copiedCodeIndex, setCopiedCodeIndex] = useState<number | null>(null);

  const handleCopyCode = (codeText: string, index: number) => {
    navigator.clipboard.writeText(codeText);
    setCopiedCodeIndex(index);
    setTimeout(() => setCopiedCodeIndex(null), 2000);
  };

  // Process code blocks vs standard markdown text
  const parseBlocks = (raw: string) => {
    const codeBlockRegex = /```([a-zA-Z0-9_-]*)\n([\s\S]*?)```/g;
    const blocks: Array<{ type: "text" | "code"; content: string; language?: string }> = [];
    let lastIndex = 0;
    let match;

    while ((match = codeBlockRegex.exec(raw)) !== null) {
      if (match.index > lastIndex) {
        blocks.push({
          type: "text",
          content: raw.slice(lastIndex, match.index)
        });
      }
      blocks.push({
        type: "code",
        language: match[1] || "text",
        content: match[2].trimEnd()
      });
      lastIndex = match.index + match[0].length;
    }

    if (lastIndex < raw.length) {
      blocks.push({
        type: "text",
        content: raw.slice(lastIndex)
      });
    }

    return blocks;
  };

  // Helper to render formatted inline text (bold, inline code, bullet points, headers)
  const renderFormattedText = (text: string) => {
    const lines = text.split("\n");
    return lines.map((line, idx) => {
      // Header check
      if (line.startsWith("### ")) {
        return (
          <h3 key={idx} className="text-sm font-bold text-purple-theme mt-3 mb-1.5 flex items-center gap-1.5">
            {renderInline(line.replace("### ", ""))}
          </h3>
        );
      }
      if (line.startsWith("## ")) {
        return (
          <h2 key={idx} className="text-base font-extrabold text-purple-theme mt-4 mb-2 flex items-center gap-2 border-b border-purple-500/20 pb-1">
            {renderInline(line.replace("## ", ""))}
          </h2>
        );
      }
      if (line.startsWith("# ")) {
        return (
          <h1 key={idx} className="text-lg font-black text-main-theme mt-4 mb-2">
            {renderInline(line.replace("# ", ""))}
          </h1>
        );
      }
      // Bullet list
      if (line.trim().startsWith("- ") || line.trim().startsWith("* ")) {
        const bulletText = line.trim().substring(2);
        return (
          <li key={idx} className="ml-4 list-disc text-main-theme my-0.5">
            {renderInline(bulletText)}
          </li>
        );
      }
      // Numbered list
      const numMatch = line.trim().match(/^(\d+)\.\s+(.*)/);
      if (numMatch) {
        return (
          <li key={idx} className="ml-5 list-decimal text-main-theme my-0.5">
            {renderInline(numMatch[2])}
          </li>
        );
      }
      // Blockquote
      if (line.trim().startsWith("> ")) {
        return (
          <blockquote key={idx} className="border-l-4 border-purple-500/50 bg-purple-500/10 px-3 py-1.5 rounded-r-lg text-secondary-theme italic my-2 text-xs">
            {renderInline(line.trim().substring(2))}
          </blockquote>
        );
      }
      // Empty line
      if (!line.trim()) {
        return <div key={idx} className="h-2" />;
      }

      return (
        <p key={idx} className="my-1 leading-relaxed text-main-theme">
          {renderInline(line)}
        </p>
      );
    });
  };

  // Simple inline parser for **bold** and `code`
  const renderInline = (str: string) => {
    const parts: React.ReactNode[] = [];
    const regex = /(\*\*.*?\*\*|`.*?`)/g;
    let last = 0;
    let m;

    while ((m = regex.exec(str)) !== null) {
      if (m.index > last) {
        parts.push(str.slice(last, m.index));
      }
      const token = m[0];
      if (token.startsWith("**") && token.endsWith("**")) {
        parts.push(
          <strong key={m.index} className="font-extrabold text-main-theme">
            {token.slice(2, -2)}
          </strong>
        );
      } else if (token.startsWith("`") && token.endsWith("`")) {
        parts.push(
          <code key={m.index} className="px-1.5 py-0.5 rounded bg-purple-500/15 border border-purple-500/30 text-purple-theme font-mono text-[11px]">
            {token.slice(1, -1)}
          </code>
        );
      }
      last = m.index + token.length;
    }
    if (last < str.length) {
      parts.push(str.slice(last));
    }
    return parts.length > 0 ? parts : str;
  };

  const blocks = parseBlocks(content);

  return (
    <div className={`space-y-2 text-xs sm:text-sm font-sans leading-relaxed ${className}`}>
      {blocks.map((block, idx) => {
        if (block.type === "code") {
          const isCopied = copiedCodeIndex === idx;
          return (
            // Code blocks intentionally keep dark background for syntax readability in both themes
            <div key={idx} className="my-3 rounded-xl border border-[var(--glass-border)] bg-slate-950 overflow-hidden shadow-xl">
              {/* Code block header bar */}
              <div className="flex items-center justify-between px-3 py-1.5 bg-slate-900 border-b border-white/10 text-[11px] font-mono">
                <span className="flex items-center gap-1.5 text-purple-theme font-bold">
                  <Code className="h-3.5 w-3.5" />
                  {block.language || "code"}
                </span>
                <button
                  onClick={() => handleCopyCode(block.content, idx)}
                  className="flex items-center gap-1 px-2 py-0.5 rounded bg-white/5 hover:bg-white/10 text-secondary-theme transition-colors cursor-pointer"
                  title="Copy code snippet"
                >
                  {isCopied ? (
                    <>
                      <Check className="h-3 w-3 text-emerald-400" />
                      <span className="text-emerald-theme font-bold">Copied!</span>
                    </>
                  ) : (
                    <>
                      <Copy className="h-3 w-3" />
                      <span>Copy</span>
                    </>
                  )}
                </button>
              </div>
              {/* Code content — dark background kept intentionally for syntax highlighting readability */}
              <pre className="p-3.5 text-xs font-mono text-cyan-200 overflow-x-auto scrollbar-thin scrollbar-thumb-slate-800 leading-relaxed bg-[#0B0F19]">
                <code>{block.content}</code>
              </pre>
            </div>
          );
        }

        return <div key={idx}>{renderFormattedText(block.content)}</div>;
      })}
    </div>
  );
}
