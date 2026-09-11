import React, { useState, useEffect } from "react";
import { useNavigate, useParams, useSearchParams } from "react-router-dom";
import { 
  BookOpen, 
  Sparkles, 
  ArrowRight, 
  GraduationCap, 
  Layers, 
  Search, 
  RefreshCw, 
  AlertCircle,
  Brain,
  CheckCircle2,
  Compass,
  Award
} from "lucide-react";
import Layout from "../../../components/Layout";
import { fetchProfile, fetchAllSubjects } from "../../../services/api";
import PersonalizedRoadmapView from "../../../components/dashboard/PersonalizedRoadmapView";

export interface EnrolledSubjectItem {
  subjectCode: string;
  subjectName: string;
  branch?: string;
  semester?: number;
  credits?: number;
  isCustom?: boolean;
}

export default function SubjectsPage() {
  const navigate = useNavigate();
  const { subjectCode } = useParams<{ subjectCode?: string }>();
  const [searchParams] = useSearchParams();
  const subjectNameQuery = searchParams.get("name");
  const [profile, setProfile] = useState<any>(null);
  const [enrolledSubjects, setEnrolledSubjects] = useState<EnrolledSubjectItem[]>([]);
  const [searchQuery, setSearchQuery] = useState("");
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const loadData = async () => {
    setLoading(true);
    setError(null);
    try {
      const activeUserId = typeof window !== "undefined"
        ? localStorage.getItem("edupilot_user_id") || "anonymous_student"
        : "anonymous_student";

      const p = await fetchProfile(activeUserId);
      setProfile(p);

      const rawSubjectsList: string[] = (p && p.subjects && p.subjects.length > 0)
        ? p.subjects
        : ["Database Management Systems", "Data Structures & Algorithms", "Artificial Intelligence & Machine Learning"];

      const catalog = await fetchAllSubjects();
      const catalogMap = new Map<string, any>();
      if (catalog && Array.isArray(catalog)) {
        catalog.forEach((item) => {
          if (item.subjectName) {
            catalogMap.set(item.subjectName.toLowerCase().trim(), item);
          }
          if (item.subjectCode) {
            catalogMap.set(item.subjectCode.toLowerCase().trim(), item);
          }
        });
      }

      const items: EnrolledSubjectItem[] = rawSubjectsList.map((sName) => {
        const cleaned = sName.trim();
        const catalogItem = catalogMap.get(cleaned.toLowerCase());
        if (catalogItem) {
          return {
            subjectCode: catalogItem.subjectCode || deriveSubjectCode(cleaned),
            subjectName: catalogItem.subjectName || cleaned,
            branch: catalogItem.branch || p?.branch || "Computer Science & Engineering",
            semester: catalogItem.semester || p?.semester || 3,
            credits: catalogItem.credits || 4
          };
        }

        return {
          subjectCode: deriveSubjectCode(cleaned),
          subjectName: cleaned,
          branch: p?.branch || "Computer Science & Engineering",
          semester: p?.semester || 3,
          credits: 4,
          isCustom: true
        };
      });

      setEnrolledSubjects(items);
    } catch (err: any) {
      console.error("Failed to load enrolled subjects:", err);
      setError("Failed to load your enrolled subjects. Please check backend connection and retry.");
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadData();
  }, []);

  const deriveSubjectCode = (subjectName: string): string => {
    const lower = subjectName.toLowerCase();
    if (lower.includes("database") || lower.includes("dbms")) return "CS302";
    if (lower.includes("data structure") || lower.includes("dsa")) return "CS301";
    if (lower.includes("operating") || lower.includes("os")) return "CS401";
    if (lower.includes("network") || lower.includes("cn")) return "CS402";
    if (lower.includes("artificial intelligence") || lower.includes("ai")) return "CS601";
    if (lower.includes("c programming") || lower === "c") return "CS101";

    const words = subjectName.split(" ").filter(w => w.length > 2);
    if (words.length >= 2) {
      return (words[0][0] + words[1][0]).toUpperCase() + "301";
    }
    return "CS301";
  };

  const handleSubjectClick = (item: EnrolledSubjectItem) => {
    navigate(`/dashboard/subjects/${encodeURIComponent(item.subjectCode)}?name=${encodeURIComponent(item.subjectName)}`);
  };

  const filteredSubjects = enrolledSubjects.filter((item) =>
    item.subjectName.toLowerCase().includes(searchQuery.toLowerCase()) ||
    item.subjectCode.toLowerCase().includes(searchQuery.toLowerCase())
  );

  if (subjectCode) {
    const matchedSubject = enrolledSubjects.find(
      s => s.subjectCode.toLowerCase() === subjectCode.toLowerCase()
    );
    const resolvedName = subjectNameQuery || matchedSubject?.subjectName || subjectCode;

    return (
      <Layout>
        <PersonalizedRoadmapView
          subjectCode={subjectCode}
          subjectName={resolvedName}
          onBack={() => navigate("/dashboard/subjects")}
        />
      </Layout>
    );
  }

  return (
    <Layout>
      <div className="space-y-6">
        
        {/* Banner Header */}
        <div className="glass-panel p-6 md:p-8 rounded-3xl border border-white/5 bg-gradient-to-r from-purple-900/20 via-indigo-900/10 to-cyan-900/20 relative overflow-hidden">
          <div className="absolute top-0 right-0 p-8 opacity-10 pointer-events-none">
            <Compass className="h-64 w-64 text-purple-theme" />
          </div>

          <div className="relative z-10 max-w-3xl space-y-3">
            <div className="flex items-center gap-2 text-xs font-bold uppercase tracking-widest text-purple-theme">
              <Sparkles className="h-4 w-4" />
              <span>Personalized Adaptive Curriculum</span>
            </div>

            <h1 className="text-2xl md:text-3xl font-extrabold text-main-theme tracking-tight">
              My Enrolled Subjects Roadmap
            </h1>

            <p className="text-sm text-secondary-theme leading-relaxed">
              Explore your tailored subject learning pathways. Select a subject to view your node-by-node mastery roadmap, recommended VARK study materials, and topic completion status.
            </p>

            <div className="pt-2 flex flex-wrap items-center gap-4 text-xs">
              <div className="flex items-center gap-1.5 px-3 py-1.5 rounded-xl bg-white/5 border border-white/10 text-main-theme">
                <GraduationCap className="h-4 w-4 text-purple-theme" />
                <span>{profile?.branch || "Computer Science & Engineering"}</span>
              </div>
              <div className="flex items-center gap-1.5 px-3 py-1.5 rounded-xl bg-white/5 border border-white/10 text-main-theme">
                <Layers className="h-4 w-4 text-cyan-theme" />
                <span>Semester {profile?.semester || 3}</span>
              </div>
              <div className="flex items-center gap-1.5 px-3 py-1.5 rounded-xl bg-white/5 border border-white/10 text-main-theme">
                <Brain className="h-4 w-4 text-emerald-theme" />
                <span>{enrolledSubjects.length} Active Subjects</span>
              </div>
            </div>
          </div>
        </div>

        {/* Filter & Action Toolbar */}
        <div className="flex flex-col sm:flex-row items-center justify-between gap-4">
          <div className="relative w-full sm:w-80">
            <Search className="h-4 w-4 absolute left-3.5 top-1/2 -translate-y-1/2 text-secondary-theme" />
            <input
              type="text"
              placeholder="Search subject or code..."
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              className="w-full pl-10 pr-4 py-2.5 rounded-xl glass-input text-xs"
            />
          </div>

          <div className="flex items-center gap-2 w-full sm:w-auto justify-end">
            <button
              onClick={loadData}
              className="px-3 py-2 rounded-xl bg-white/5 hover:bg-white/10 border border-white/10 text-xs font-semibold text-main-theme flex items-center gap-2 transition-all cursor-pointer"
            >
              <RefreshCw className={`h-3.5 w-3.5 ${loading ? "animate-spin" : ""}`} />
              <span>Refresh Subjects</span>
            </button>
          </div>
        </div>

        {/* Error Alert */}
        {error && (
          <div className="p-4 rounded-xl bg-red-500/10 border border-red-500/20 text-red-400 text-xs flex items-center justify-between">
            <div className="flex items-center gap-2">
              <AlertCircle className="h-4 w-4 shrink-0" />
              <span>{error}</span>
            </div>
            <button
              onClick={loadData}
              className="px-3 py-1 rounded-lg bg-red-500/20 hover:bg-red-500/30 text-white text-xs font-bold"
            >
              Retry
            </button>
          </div>
        )}

        {/* Subjects Grid */}
        {loading ? (
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
            {[1, 2, 3].map((i) => (
              <div key={i} className="glass-panel p-6 rounded-2xl space-y-4 animate-pulse">
                <div className="h-4 w-24 bg-white/10 rounded" />
                <div className="h-6 w-3/4 bg-white/10 rounded" />
                <div className="h-3 w-1/2 bg-white/10 rounded" />
                <div className="h-10 w-full bg-white/10 rounded-xl pt-4" />
              </div>
            ))}
          </div>
        ) : filteredSubjects.length === 0 ? (
          <div className="glass-panel p-12 rounded-3xl border border-white/5 text-center space-y-4 max-w-lg mx-auto">
            <div className="h-12 w-12 rounded-2xl bg-purple-500/10 border border-purple-500/20 flex items-center justify-center text-purple-theme mx-auto">
              <BookOpen className="h-6 w-6" />
            </div>
            <h3 className="text-lg font-bold text-main-theme">No Enrolled Subjects Found</h3>
            <p className="text-xs text-secondary-theme">
              {searchQuery
                ? `No subjects match "${searchQuery}". Try a different query.`
                : "You haven't selected any subjects during onboarding. Complete your profile setup to generate roadmaps."}
            </p>
            {!searchQuery && (
              <button
                onClick={() => navigate("/dashboard/profile")}
                className="px-4 py-2 rounded-xl bg-purple-600 hover:bg-purple-500 text-white text-xs font-bold transition-all shadow-lg shadow-purple-600/20"
              >
                Update Selected Subjects
              </button>
            )}
          </div>
        ) : (
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
            {filteredSubjects.map((item) => (
              <div
                key={item.subjectCode + item.subjectName}
                onClick={() => handleSubjectClick(item)}
                className="glass-panel-interactive p-6 rounded-2xl space-y-5 flex flex-col justify-between cursor-pointer group relative overflow-hidden"
              >
                {/* Subject Header */}
                <div className="space-y-3">
                  <div className="flex items-center justify-between">
                    <span className="text-[10px] font-extrabold px-2.5 py-1 rounded-lg bg-purple-500/15 text-purple-theme border border-purple-500/20 uppercase tracking-wider">
                      {item.subjectCode}
                    </span>
                    <span className="text-[10px] font-semibold text-secondary-theme flex items-center gap-1">
                      <CheckCircle2 className="h-3 w-3 text-emerald-400" /> Roadmap Ready
                    </span>
                  </div>

                  <h3 className="text-lg font-bold text-main-theme group-hover:text-purple-theme transition-colors leading-snug">
                    {item.subjectName}
                  </h3>

                  <div className="flex items-center gap-3 text-xs text-secondary-theme">
                    <span className="flex items-center gap-1">
                      <Layers className="h-3.5 w-3.5 text-cyan-theme" /> Sem {item.semester}
                    </span>
                    <span>•</span>
                    <span className="flex items-center gap-1">
                      <Award className="h-3.5 w-3.5 text-amber-theme" /> {item.credits} Credits
                    </span>
                  </div>
                </div>

                {/* Card Action Footer */}
                <div className="pt-4 border-t border-white/5 flex items-center justify-between">
                  <span className="text-xs font-semibold text-secondary-theme group-hover:text-main-theme transition-colors">
                    View Personalized Roadmap
                  </span>
                  <div className="h-8 w-8 rounded-xl bg-purple-600/20 group-hover:bg-purple-600 text-purple-theme group-hover:text-white flex items-center justify-center transition-all duration-300 transform group-hover:translate-x-1 shadow-md shadow-purple-600/20">
                    <ArrowRight className="h-4 w-4" />
                  </div>
                </div>
              </div>
            ))}
          </div>
        )}

      </div>
    </Layout>
  );
}
