import React from "react";

interface SkeletonLoaderProps {
  count?: number;
}

export default function SkeletonLoader({ count = 3 }: SkeletonLoaderProps) {
  return (
    <div className="space-y-4 my-4">
      {Array.from({ length: count }).map((_, i) => (
        <div
          key={i}
          className={`flex gap-3 ${i % 2 === 0 ? "justify-start" : "justify-end"}`}
        >
          {i % 2 === 0 && (
            <div className="h-8 w-8 rounded-2xl bg-slate-800 animate-pulse shrink-0" />
          )}

          <div
            className={`p-4 rounded-2xl border border-white/5 bg-slate-900/60 animate-pulse space-y-2 ${
              i % 2 === 0 ? "w-3/4 rounded-tl-xs" : "w-2/3 rounded-tr-xs bg-purple-950/20 border-purple-500/10"
            }`}
          >
            <div className="h-3 bg-slate-800 rounded w-1/4" />
            <div className="h-3 bg-slate-800/80 rounded w-5/6" />
            <div className="h-3 bg-slate-800/60 rounded w-4/6" />
          </div>

          {i % 2 !== 0 && (
            <div className="h-8 w-8 rounded-2xl bg-slate-800 animate-pulse shrink-0" />
          )}
        </div>
      ))}
    </div>
  );
}
