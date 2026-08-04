import React, { useState, useEffect } from "react";
import Layout from "../../../components/Layout";
import { 
  BookOpen, 
  Plus, 
  GraduationCap, 
  HelpCircle, 
  CheckCircle,
  FileText,
  AlertCircle
} from "lucide-react";
import { createQuizQuestion, fetchQuizQuestions } from "../../../services/api";

export default function QuizManagerDashboard() {
  // Filters
  const [filterSubject, setFilterSubject] = useState("Data Structures & Algorithms");
  const [filterDifficulty, setFilterDifficulty] = useState<"EASY" | "MEDIUM" | "HARD">("EASY");
  const [activeQuestions, setActiveQuestions] = useState<any[]>([]);
  const [loadingPool, setLoadingPool] = useState(false);

  // Form states
  const [subjInput, setSubjInput] = useState("Data Structures & Algorithms");
  const [conceptInput, setConceptInput] = useState("");
  const [difficultyInput, setDifficultyInput] = useState<"EASY" | "MEDIUM" | "HARD">("EASY");
  const [questionText, setQuestionText] = useState("");
  const [optionsInput, setOptionsInput] = useState(["", "", "", ""]);
  const [correctIdx, setCorrectIdx] = useState(0);
  const [explanationInput, setExplanationInput] = useState("");
  const [submitting, setSubmitting] = useState(false);

  // Load questions based on filters
  const loadPool = async () => {
    setLoadingPool(true);
    try {
      const qList = await fetchQuizQuestions(filterSubject, filterDifficulty);
      setActiveQuestions(qList);
    } catch (err) {
      console.error("Error loading quiz pool:", err);
    } finally {
      setLoadingPool(false);
    }
  };

  useEffect(() => {
    loadPool();
  }, [filterSubject, filterDifficulty]);

  const handleOptionChange = (idx: number, val: string) => {
    const nextOpts = [...optionsInput];
    nextOpts[idx] = val;
    setOptionsInput(nextOpts);
  };

  const handleAddQuestion = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!conceptInput.trim() || !questionText.trim()) {
      alert("Please fill all core fields.");
      return;
    }

    const cleanedOptions = optionsInput.filter(o => o.trim() !== "");
    if (cleanedOptions.length < 2) {
      alert("Please enter at least two options.");
      return;
    }

    setSubmitting(true);
    const newQ = {
      subject: subjInput,
      concept: conceptInput,
      difficulty: difficultyInput,
      questionText,
      options: cleanedOptions,
      correctOptionIndex: correctIdx,
      conceptualExplanation: explanationInput
    };

    try {
      await createQuizQuestion(newQ);
      alert("Question successfully saved and registered!");
      
      // Reset form
      setConceptInput("");
      setQuestionText("");
      setOptionsInput(["", "", "", ""]);
      setExplanationInput("");
      
      // Reload active list
      loadPool();
    } catch (err) {
      alert("Failed to submit question. Check backend connection.");
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <Layout>
      <div className="space-y-8">
        
        {/* Title Block */}
        <div>
          <h1 className="text-3xl font-extrabold text-main-theme flex items-center gap-2">
            <GraduationCap className="h-8 w-8 text-purple-theme" />
            <span>Adaptive Quiz Pools Manager</span>
          </h1>
          <p className="text-secondary-theme text-sm mt-1 font-medium">
            Create new challenge sets and review active questions in the database to map targeted student development.
          </p>
        </div>

        {/* Main Grid */}
        <div className="grid grid-cols-1 lg:grid-cols-12 gap-8 items-start">
          
          {/* Left Column: Author Form (5/12 width) */}
          <div className="lg:col-span-5 glass-panel p-6 rounded-2xl border border-white/5 space-y-6">
            <div className="border-b border-white/5 pb-3">
              <h3 className="text-base font-extrabold text-gradient-purple flex items-center gap-2">
                <Plus className="h-5 w-5 text-purple-theme" />
                <span>Author New Quiz Question</span>
              </h3>
              <p className="text-[11px] text-secondary-theme mt-1">Design conceptual items for adaptive assessment paths.</p>
            </div>

            <form onSubmit={handleAddQuestion} className="space-y-4">
              <div className="grid grid-cols-2 gap-4">
                <div className="space-y-1">
                  <label className="text-[10px] font-bold text-secondary-theme uppercase tracking-wider">Subject Topic</label>
                  <select
                    value={subjInput}
                    onChange={(e) => setSubjInput(e.target.value)}
                    className="w-full p-2.5 rounded-lg glass-input text-xs focus:bg-[#0d0f1e]"
                  >
                    <option value="Data Structures & Algorithms">Data Structures & Algorithms</option>
                    <option value="Database Management Systems">Database Management Systems</option>
                    <option value="Artificial Intelligence">Artificial Intelligence</option>
                  </select>
                </div>

                <div className="space-y-1">
                  <label className="text-[10px] font-bold text-secondary-theme uppercase tracking-wider">Target Concept</label>
                  <input
                    type="text"
                    required
                    placeholder="e.g. Red-Black Tree"
                    value={conceptInput}
                    onChange={(e) => setConceptInput(e.target.value)}
                    className="w-full p-2.5 rounded-lg glass-input text-xs"
                  />
                </div>

                <div className="space-y-1">
                  <label className="text-[10px] font-bold text-secondary-theme uppercase tracking-wider">Difficulty Tier</label>
                  <select
                    value={difficultyInput}
                    onChange={(e) => setDifficultyInput(e.target.value as any)}
                    className="w-full p-2.5 rounded-lg glass-input text-xs focus:bg-[#0d0f1e]"
                  >
                    <option value="EASY">EASY</option>
                    <option value="MEDIUM">MEDIUM</option>
                    <option value="HARD">HARD</option>
                  </select>
                </div>

                <div className="space-y-1">
                  <label className="text-[10px] font-bold text-secondary-theme uppercase tracking-wider">Correct Index (0-3)</label>
                  <input
                    type="number"
                    min="0"
                    max="3"
                    required
                    value={correctIdx}
                    onChange={(e) => setCorrectIdx(Number(e.target.value))}
                    className="w-full p-2.5 rounded-lg glass-input text-xs"
                  />
                </div>
              </div>

              <div className="space-y-1">
                <label className="text-[10px] font-bold text-secondary-theme uppercase tracking-wider">Question Body</label>
                <textarea
                  required
                  rows={3}
                  value={questionText}
                  onChange={(e) => setQuestionText(e.target.value)}
                  placeholder="Type question prompt or details..."
                  className="w-full p-2.5 rounded-lg glass-input text-xs"
                />
              </div>

              <div className="grid grid-cols-2 gap-3">
                {optionsInput.map((opt, idx) => (
                  <div key={idx} className="space-y-1">
                    <label className="text-[9px] font-bold text-secondary-theme uppercase tracking-wider">Option {idx}</label>
                    <input
                      type="text"
                      required
                      placeholder={`Option Choice ${idx}`}
                      value={opt}
                      onChange={(e) => handleOptionChange(idx, e.target.value)}
                      className="w-full p-2 rounded-lg glass-input text-xs"
                    />
                  </div>
                ))}
              </div>

              <div className="space-y-1">
                <label className="text-[10px] font-bold text-secondary-theme uppercase tracking-wider">AI Conceptual Explanation</label>
                <input
                  type="text"
                  required
                  placeholder="Explain why correct index is correct..."
                  value={explanationInput}
                  onChange={(e) => setExplanationInput(e.target.value)}
                  className="w-full p-2.5 rounded-lg glass-input text-xs"
                />
              </div>

              <button
                type="submit"
                disabled={submitting}
                className="w-full py-3 bg-gradient-to-r from-purple-600 to-pink-600 hover:from-purple-500 hover:to-pink-500 text-white rounded-xl text-xs font-bold transition-all shadow-md shadow-purple-600/15 cursor-pointer flex items-center justify-center gap-1.5"
              >
                {submitting ? (
                  <div className="h-4 w-4 border-2 border-white border-t-transparent rounded-full animate-spin" />
                ) : (
                  <>
                    <Plus className="h-4 w-4" />
                    <span>Add Question to Adaptive Pools</span>
                  </>
                )}
              </button>
            </form>
          </div>

          {/* Right Column: Pool Viewer (7/12 width) */}
          <div className="lg:col-span-7 space-y-6">
            
            {/* Filter Panel */}
            <div className="glass-panel p-5 rounded-2xl border border-white/5 grid grid-cols-1 md:grid-cols-2 gap-4">
              <div className="space-y-1">
                <label className="text-[10px] font-bold text-secondary-theme uppercase tracking-widest block">Filter Subject</label>
                <select
                  value={filterSubject}
                  onChange={(e) => setFilterSubject(e.target.value)}
                  className="w-full p-2.5 rounded-lg glass-input text-xs focus:bg-[#0d0f1e]"
                >
                  <option value="Data Structures & Algorithms">Data Structures & Algorithms</option>
                  <option value="Database Management Systems">Database Management Systems</option>
                  <option value="Artificial Intelligence">Artificial Intelligence</option>
                </select>
              </div>

              <div className="space-y-1">
                <label className="text-[10px] font-bold text-secondary-theme uppercase tracking-widest block">Filter Difficulty</label>
                <div className="grid grid-cols-3 gap-2 bg-white/5 rounded-lg p-1 border border-white/5 text-xs font-bold">
                  {(["EASY", "MEDIUM", "HARD"] as const).map((diff) => (
                    <button
                      key={diff}
                      type="button"
                      onClick={() => setFilterDifficulty(diff)}
                      className={`py-1.5 rounded-md text-center transition-all ${filterDifficulty === diff ? "bg-[var(--accent-purple)] text-white" : "text-secondary-theme hover:text-main-theme"}`}
                    >
                      {diff}
                    </button>
                  ))}
                </div>
              </div>
            </div>

            {/* Questions Pool list */}
            <div className="glass-panel p-6 rounded-2xl border border-white/5 space-y-4">
              <div className="flex justify-between items-center border-b border-white/5 pb-3">
                <h3 className="text-sm font-extrabold tracking-wide flex items-center gap-2">
                  <FileText className="h-5 w-5 text-cyan-theme" />
                  <span>Active Question Pool ({activeQuestions.length})</span>
                </h3>
                <span className="text-[10px] px-2 py-0.5 rounded-full bg-purple-500/10 border border-purple-500/20 text-purple-theme font-bold uppercase tracking-widest">
                  {filterDifficulty} Level
                </span>
              </div>

              {loadingPool ? (
                <div className="flex flex-col items-center justify-center py-16 space-y-2">
                  <div className="h-8 w-8 border-4 border-purple-500 border-t-transparent rounded-full animate-spin" />
                  <span className="text-xs text-secondary-theme font-semibold">Fetching pool questions...</span>
                </div>
              ) : activeQuestions.length === 0 ? (
                <div className="text-center py-16 space-y-2">
                  <AlertCircle className="h-10 w-10 text-secondary-theme mx-auto opacity-40" />
                  <p className="text-xs text-secondary-theme font-bold">No questions found matching your filter criteria.</p>
                  <p className="text-[10px] text-secondary-theme max-w-xs mx-auto">Author a question in the left panel to populate this category.</p>
                </div>
              ) : (
                <div className="space-y-4 max-h-[500px] overflow-y-auto pr-1">
                  {activeQuestions.map((q, idx) => (
                    <div key={q.id || idx} className="p-4 rounded-xl bg-white/5 border border-white/5 space-y-3">
                      <div className="flex justify-between items-start">
                        <span className="text-xs font-bold text-main-theme pr-4">{q.questionText}</span>
                        <span className="text-[9px] font-extrabold uppercase px-1.5 py-0.5 rounded bg-white/10 text-secondary-theme tracking-wide whitespace-nowrap">
                          {q.concept}
                        </span>
                      </div>

                      {/* Options */}
                      <div className="grid grid-cols-1 md:grid-cols-2 gap-2">
                        {q.options?.map((opt: string, optIdx: number) => {
                          const isCorrect = optIdx === q.correctOptionIndex;
                          return (
                            <div 
                              key={optIdx} 
                              className={`p-2 rounded-lg text-xs flex items-center justify-between border ${isCorrect ? "bg-emerald-500/10 border-emerald-500/30 text-emerald-400 font-bold" : "bg-white/5 border-white/5 text-secondary-theme"}`}
                            >
                              <span>{optIdx}. {opt}</span>
                              {isCorrect && <CheckCircle className="h-3.5 w-3.5 text-emerald-400" />}
                            </div>
                          );
                        })}
                      </div>

                      {/* Explanation */}
                      {q.conceptualExplanation && (
                        <div className="p-2.5 rounded-lg bg-purple-500/5 border border-purple-500/10 text-[10px] text-purple-300">
                          <strong>Conceptual Explanation:</strong> {q.conceptualExplanation}
                        </div>
                      )}
                    </div>
                  ))}
                </div>
              )}

            </div>

          </div>

        </div>

      </div>
    </Layout>
  );
}
