import React, { useState } from "react";
import { MessageSquare, Trash2, Edit2, Check, X, Sparkles } from "lucide-react";

interface ConversationCardProps {
  conversation: {
    conversationId: string;
    title?: string;
    learningMode?: string;
    lastUpdated?: string;
    createdAt?: string;
    messages?: any[];
  };
  isActive: boolean;
  onSelect: () => void;
  onDelete: () => void;
  onRename: (newTitle: string) => void;
}

export default function ConversationCard({
  conversation,
  isActive,
  onSelect,
  onDelete,
  onRename
}: ConversationCardProps) {
  const [isEditing, setIsEditing] = useState(false);
  const [editTitle, setEditTitle] = useState(conversation.title || "Untitled Chat");

  const handleSaveRename = (e?: React.FormEvent) => {
    e?.preventDefault();
    if (editTitle.trim()) {
      onRename(editTitle.trim());
    } else {
      setEditTitle(conversation.title || "Untitled Chat");
    }
    setIsEditing(false);
  };

  const handleCancelRename = () => {
    setEditTitle(conversation.title || "Untitled Chat");
    setIsEditing(false);
  };

  const formattedDate = conversation.lastUpdated || conversation.createdAt
    ? new Date(conversation.lastUpdated || conversation.createdAt!).toLocaleDateString(undefined, {
        month: "short",
        day: "numeric",
        hour: "2-digit",
        minute: "2-digit"
      })
    : "Recent";

  return (
    <div
      onClick={() => !isEditing && onSelect()}
      className={`group relative p-3 rounded-xl border transition-all duration-200 cursor-pointer ${
        isActive
          ? "bg-purple-900/20 border-purple-500/40 shadow-lg shadow-purple-950/40"
          : "bg-slate-900/40 border-white/5 hover:bg-slate-900/80 hover:border-white/10"
      }`}
    >
      <div className="flex items-start justify-between gap-2">
        <div className="flex items-center gap-2 min-w-0 flex-1">
          <div
            className={`p-1.5 rounded-lg shrink-0 ${
              isActive
                ? "bg-purple-600/30 text-purple-300 border border-purple-500/30"
                : "bg-white/5 text-slate-400 group-hover:text-slate-200"
            }`}
          >
            <MessageSquare className="h-4 w-4" />
          </div>

          {isEditing ? (
            <form onSubmit={handleSaveRename} className="flex items-center gap-1 flex-1 min-w-0">
              <input
                type="text"
                value={editTitle}
                onChange={(e) => setEditTitle(e.target.value)}
                autoFocus
                className="w-full bg-slate-950 text-white text-xs px-2 py-1 rounded border border-purple-500 focus:outline-none"
              />
              <button
                type="submit"
                className="p-1 text-emerald-400 hover:text-emerald-300 cursor-pointer"
                title="Save"
              >
                <Check className="h-3.5 w-3.5" />
              </button>
              <button
                type="button"
                onClick={handleCancelRename}
                className="p-1 text-slate-400 hover:text-rose-400 cursor-pointer"
                title="Cancel"
              >
                <X className="h-3.5 w-3.5" />
              </button>
            </form>
          ) : (
            <div className="min-w-0 flex-1">
              <h4 className={`text-xs font-semibold truncate ${isActive ? "text-white" : "text-slate-300 group-hover:text-white"}`}>
                {conversation.title || "New Study Session"}
              </h4>
              <div className="flex items-center gap-2 mt-1">
                <span className="text-[10px] text-slate-500">{formattedDate}</span>
                {conversation.learningMode && (
                  <span className="text-[9px] font-bold uppercase px-1.5 py-0.2 rounded bg-purple-500/10 text-purple-400 border border-purple-500/20">
                    {conversation.learningMode}
                  </span>
                )}
              </div>
            </div>
          )}
        </div>

        {/* Action icons on hover */}
        {!isEditing && (
          <div className="flex items-center gap-1 opacity-0 group-hover:opacity-100 transition-opacity">
            <button
              onClick={(e) => {
                e.stopPropagation();
                setIsEditing(true);
              }}
              className="p-1 text-slate-400 hover:text-purple-300 hover:bg-purple-500/10 rounded transition-colors cursor-pointer"
              title="Rename conversation"
            >
              <Edit2 className="h-3.5 w-3.5" />
            </button>

            <button
              onClick={(e) => {
                e.stopPropagation();
                onDelete();
              }}
              className="p-1 text-slate-400 hover:text-rose-400 hover:bg-rose-500/10 rounded transition-colors cursor-pointer"
              title="Delete conversation"
            >
              <Trash2 className="h-3.5 w-3.5" />
            </button>
          </div>
        )}
      </div>
    </div>
  );
}
