import React, { useState, useEffect } from "react";
import Layout from "../../../components/Layout";
import { AdminCohortComparisonView } from "../../../components/admin/AdminCohortComparisonView";
import { fetchAllSubjects, fetchAdminSubjectAnalytics } from "../../../services/api";
import { GitCompare, Sparkles } from "lucide-react";

export default function ResearchAnalyticsPage() {
  const [availableBranches, setAvailableBranches] = useState<string[]>([
    "Computer Science & Engineering",
    "Information Technology",
    "Electronics & Communication",
    "Mechanical Engineering",
    "Civil Engineering",
    "Electrical Engineering"
  ]);
  const [availableSubjects, setAvailableSubjects] = useState<Array<{ code: string; name: string }>>([
    { code: "DBMS", name: "Database Management Systems" },
    { code: "DSA", name: "Data Structures & Algorithms" },
    { code: "AIML", name: "Artificial Intelligence & Machine Learning" },
    { code: "CN", name: "Computer Networks" },
    { code: "OS", name: "Operating Systems" },
    { code: "SE", name: "Software Engineering" }
  ]);

  useEffect(() => {
    async function loadMetadata() {
      try {
        const [subjectsCatalog, subjectAnalytics] = await Promise.allSettled([
          fetchAllSubjects(),
          fetchAdminSubjectAnalytics()
        ]);

        const subjectMap = new Map<string, string>();

        if (subjectsCatalog.status === "fulfilled" && Array.isArray(subjectsCatalog.value)) {
          subjectsCatalog.value.forEach((sub: any) => {
            const code = sub.subjectCode || sub.code;
            const name = sub.subjectName || sub.name;
            if (code && name) {
              subjectMap.set(code, name);
            }
          });
        }

        if (subjectAnalytics.status === "fulfilled" && subjectAnalytics.value?.subjects) {
          subjectAnalytics.value.subjects.forEach((sub: any) => {
            if (sub.subjectCode && sub.subjectName) {
              subjectMap.set(sub.subjectCode, sub.subjectName);
            }
          });
        }

        if (subjectMap.size > 0) {
          setAvailableSubjects(
            Array.from(subjectMap.entries()).map(([code, name]) => ({ code, name }))
          );
        }
      } catch (err) {
        console.warn("Error fetching comparison metadata:", err);
      }
    }

    loadMetadata();
  }, []);

  return (
    <Layout>
      <div className="space-y-6 max-w-7xl mx-auto pb-12">
        {/* Page Header */}
        <div className="flex flex-col md:flex-row md:items-center justify-between gap-4">
          <div>
            <div className="flex items-center gap-2 text-xs font-bold uppercase tracking-wider text-purple-400 mb-1">
              <Sparkles className="h-4 w-4 text-purple-400" />
              <span>Institutional Research & Observational Telemetry</span>
            </div>
            <h1 className="text-2xl sm:text-3xl font-black text-main-theme tracking-tight flex items-center gap-3">
              <GitCompare className="h-7 w-7 text-purple-500 shrink-0" />
              Cohort Comparison
            </h1>
            <p className="text-sm text-secondary-theme mt-1 max-w-2xl">
              Conduct dual-cohort comparative analysis across branches, semesters, subjects, activity profiles, and diagnostic baseline cohorts.
            </p>
          </div>
        </div>

        {/* Cohort Comparison View Component */}
        <AdminCohortComparisonView
          availableBranches={availableBranches}
          availableSubjects={availableSubjects}
        />
      </div>
    </Layout>
  );
}
