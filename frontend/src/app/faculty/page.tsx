import React, { useState } from "react";
import { Link } from "react-router-dom";
import Layout from "../../components/Layout";
import { 
  Users, 
  Activity, 
  BookOpen, 
  CheckCircle, 
  Mail, 
  Plus, 
  ShieldAlert, 
  UserX, 
  BarChart4, 
  Sparkles,
  Send,
  X
} from "lucide-react";
import { MOCK_CLASS_DATA } from "../../services/mockData";

export default function FacultyDashboard() {
  const [classData, setClassData] = useState(MOCK_CLASS_DATA);
  const [selectedStudent, setSelectedStudent] = useState<any>(null);
  const [interventionText, setInterventionText] = useState("");
  const [showInterventionSent, setShowInterventionSent] = useState(false);

  const handleInterventionSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!interventionText.trim()) return;
    
    setShowInterventionSent(true);
    setTimeout(() => {
      setInterventionText("");
      setShowInterventionSent(false);
      setSelectedStudent(null);
      alert("Intervention notice successfully sent to student!");
    }, 1500);
  };

  return (
    <Layout>
      <div className="space-y-8">
        
        {/* Title */}
        <div>
          <h1 className="text-3xl font-extrabold text-main-theme flex items-center gap-2">
            <Users className="h-8 w-8 text-purple-theme" />
            <span>Faculty Analytics Terminal</span>
          </h1>
          <p className="text-secondary-theme text-sm mt-1 font-medium">
            Monitor class performance distributions, identify students in need of academic intervention, and author quiz sets.
          </p>
        </div>

        {/* Top Class Stats Row */}
        <div className="grid grid-cols-1 md:grid-cols-4 gap-6">
          <div className="glass-panel p-5 rounded-2xl border border-white/5 space-y-1">
            <span className="text-[10px] text-secondary-theme uppercase font-extrabold tracking-wider">Class Average SGI</span>
            <div className="text-2xl font-black text-purple-theme">{classData.averageSgi} <span className="text-xs text-secondary-theme">/ 10</span></div>
            <p className="text-[10px] text-secondary-theme">Holistic study & sleep correlation base.</p>
          </div>

          <div className="glass-panel p-5 rounded-2xl border border-white/5 space-y-1">
            <span className="text-[10px] text-secondary-theme uppercase font-extrabold tracking-wider">Class Average CGPA</span>
            <div className="text-2xl font-black text-cyan-theme">{classData.averageCgpa} <span className="text-xs text-secondary-theme">/ 10</span></div>
            <p className="text-[10px] text-secondary-theme">Aggregated mid-term grades.</p>
          </div>

          <div className="glass-panel p-5 rounded-2xl border border-white/5 space-y-1">
            <span className="text-[10px] text-secondary-theme uppercase font-extrabold tracking-wider">Quiz Completion Rate</span>
            <div className="text-2xl font-black text-pink-theme">{classData.quizCompletionRate}%</div>
            <p className="text-[10px] text-secondary-theme">Participation in diagnostic sets.</p>
          </div>

          <div className="glass-panel p-5 rounded-2xl border border-white/5 space-y-1 bg-pink-500/5">
            <span className="text-[10px] text-pink-theme uppercase font-extrabold tracking-wider">Interventions Recommended</span>
            <div className="text-2xl font-black text-pink-500">{classData.atRiskCount} Enrolled</div>
            <p className="text-[10px] text-secondary-theme">Students with low SGI / High risk profiles.</p>
          </div>
        </div>

        {/* Main Grid: Directory Table & Intervention Drawer */}
        <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
          
          {/* Student Performance Table (2/3 width) */}
          <div className="glass-panel p-6 rounded-2xl border border-white/5 lg:col-span-2 space-y-4">
            <div className="flex justify-between items-center border-b border-white/5 pb-3">
              <h3 className="text-sm font-extrabold tracking-wide">Class Profile Directory</h3>
              
              <Link 
                to="/faculty/quiz-manager"
                className="flex items-center gap-1.5 px-3.5 py-2 rounded-lg bg-purple-600 hover:bg-purple-500 text-white font-bold text-xs shadow-md shadow-purple-500/15 cursor-pointer"
              >
                <Plus className="h-4 w-4" />
                <span>Create Adaptive Question</span>
              </Link>
            </div>

            {/* Table */}
            <div className="overflow-x-auto">
              <table className="w-full text-left text-xs border-collapse">
                <thead>
                  <tr className="border-b border-white/5 text-secondary-theme font-extrabold">
                    <th className="pb-3 pr-2">Student Name</th>
                    <th className="pb-3 px-2">Growth Index (SGI)</th>
                    <th className="pb-3 px-2">GPA (Mocks)</th>
                    <th className="pb-3 px-2">Attendance</th>
                    <th className="pb-3 px-2">Risk Level</th>
                    <th className="pb-3 pl-2 text-right">Actions</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-white/5 font-semibold">
                  {classData.students.map((student) => (
                    <tr key={student.id} className="hover:bg-white/3 transition-colors">
                      <td className="py-3.5 pr-2 font-bold text-main-theme">{student.name}</td>
                      <td className="py-3.5 px-2 text-purple-theme">{student.sgi}</td>
                      <td className="py-3.5 px-2 text-cyan-theme">{student.cgpa}</td>
                      <td className="py-3.5 px-2 text-main-theme">{student.attendance}%</td>
                      <td className="py-3.5 px-2">
                        <span className={`px-2.5 py-0.5 rounded-full text-[9px] font-bold ${
                          student.risk === "LOW" ? "bg-emerald-500/10 text-emerald-theme" :
                          student.risk === "MEDIUM" ? "bg-amber-500/10 text-amber-theme" : "bg-pink-500/10 text-pink-theme"
                        }`}>
                          {student.risk}
                        </span>
                      </td>
                      <td className="py-3.5 pl-2 text-right">
                        {student.risk === "HIGH" && (
                          <button
                            onClick={() => setSelectedStudent(student)}
                            className="px-2.5 py-1 bg-pink-500/10 border border-pink-500/20 text-pink-theme hover:bg-pink-500 hover:text-white rounded-md text-[10px] font-extrabold transition-all cursor-pointer"
                          >
                            Intervene
                          </button>
                        )}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>

          {/* Right Column: AI Action Drawer for Selected At-Risk student */}
          <div className="glass-panel p-6 rounded-2xl border border-white/5 space-y-4">
            <div className="flex items-center gap-2 border-b border-white/5 pb-3">
              <ShieldAlert className="h-5 w-5 text-pink-500" />
              <h3 className="text-sm font-extrabold tracking-wide">AI Intervention Hub</h3>
            </div>

            {!selectedStudent ? (
              <div className="flex flex-col items-center justify-center py-12 text-center text-secondary-theme space-y-3">
                <UserX className="h-10 w-10 text-gray-600" />
                <p className="text-xs max-w-[200px] leading-relaxed">
                  Select <strong>Intervene</strong> next to any HIGH risk student in the directory table to author custom tasks.
                </p>
              </div>
            ) : (
              <form onSubmit={handleInterventionSubmit} className="space-y-4 animate-fade-in">
                <div className="space-y-1">
                  <span className="text-[10px] text-secondary-theme font-bold uppercase">Target Enrollee</span>
                  <div className="p-3 bg-white/5 border border-white/5 rounded-xl">
                    <h4 className="text-xs font-bold text-main-theme">{selectedStudent.name}</h4>
                    <p className="text-[9px] text-pink-theme mt-1 uppercase font-extrabold">
                      Issue: {selectedStudent.primaryIssue}
                    </p>
                  </div>
                </div>

                <div className="space-y-1">
                  <label className="text-[10px] text-secondary-theme font-bold uppercase">Intervention Directives</label>
                  <textarea
                    required
                    rows={4}
                    value={interventionText}
                    onChange={(e) => setInterventionText(e.target.value)}
                    placeholder="Provide recommendations (e.g. adjust study targets, request in-person review, suggest sleep rest models)..."
                    className="w-full p-3 rounded-xl glass-input text-xs leading-relaxed"
                  />
                </div>

                <button
                  type="submit"
                  disabled={showInterventionSent}
                  className="w-full py-3 bg-pink-600 hover:bg-pink-500 disabled:opacity-40 text-white rounded-xl text-xs font-bold flex items-center justify-center gap-1.5 shadow-lg shadow-pink-500/15 cursor-pointer transition-all"
                >
                  <span>Dispatch Intervention Alert</span>
                  <Send className="h-3.5 w-3.5" />
                </button>
              </form>
            )}
          </div>

        </div>

      </div>

      {/* Adaptive Quiz Creator Modal Overlay */}

    </Layout>
  );
}
