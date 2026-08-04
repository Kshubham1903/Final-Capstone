import React, { useState, useEffect } from "react";
import { useNavigate } from "react-router-dom";
import { motion, AnimatePresence } from "framer-motion";
import { 
  BrainCircuit, 
  Sparkles, 
  ArrowRight, 
  ArrowLeft, 
  User, 
  GraduationCap, 
  Activity, 
  BookOpen, 
  CheckCircle, 
  ShieldAlert, 
  Clock, 
  Save, 
  HeartHandshake, 
  Compass, 
  Building2, 
  Briefcase, 
  Code 
} from "lucide-react";
import { saveOnboardingStep, onboardStudent, fetchOnboardingStatus, postQuestionnaire } from "../../services/api";

export default function Onboarding() {
  const navigate = useNavigate();
  const [step, setStep] = useState(1);
  const [saving, setSaving] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState("");

  const userId = typeof window !== "undefined" ? (localStorage.getItem("edupilot_user_id") || "") : "";

  // Step 1: Personal Information
  const [fullName, setFullName] = useState("");
  const [email, setEmail] = useState("");
  const [phone, setPhone] = useState("");
  const [dateOfBirth, setDateOfBirth] = useState("2003-05-15");
  const [gender, setGender] = useState("Male");
  const [address, setAddress] = useState("");
  const [city, setCity] = useState("San Francisco");
  const [state, setState] = useState("CA");
  const [country, setCountry] = useState("USA");
  const [collegeName, setCollegeName] = useState("Institute of Technology");
  const [university, setUniversity] = useState("State University");
  const [engineeringBranch, setEngineeringBranch] = useState("Computer Science & Engineering");
  const [currentSemester, setCurrentSemester] = useState(5);
  const [rollNumber, setRollNumber] = useState("CS-2023-042");
  const [admissionYear, setAdmissionYear] = useState(2022);
  const [expectedGraduationYear, setExpectedGraduationYear] = useState(2026);

  // Step 2: Academic Profile
  const [currentCgpa, setCurrentCgpa] = useState(8.2);
  const [targetCgpa, setTargetCgpa] = useState(9.2);
  const [currentSubjects, setCurrentSubjects] = useState("Data Structures & Algorithms, Database Management Systems, Artificial Intelligence");
  const [weakSubjects, setWeakSubjects] = useState("Dynamic Programming, Graph Theory");
  const [strongSubjects, setStrongSubjects] = useState("Binary Search Tree, SQL Joins");
  const [careerGoal, setCareerGoal] = useState("Machine Learning Engineer / AI Researcher");
  const [dreamCompany, setDreamCompany] = useState("Google DeepMind / Apple");
  const [programmingLanguages, setProgrammingLanguages] = useState("Python, Java, TypeScript, C++");
  const [frameworks, setFrameworks] = useState("React, Spring Boot, FastAPI, TensorFlow");
  const [githubUrl, setGithubUrl] = useState("https://github.com/student");
  const [linkedInUrl, setLinkedInUrl] = useState("https://linkedin.com/in/student");
  const [weeklyCodingHours, setWeeklyCodingHours] = useState(15);
  const [preferredLearningStyle, setPreferredLearningStyle] = useState("Kinesthetic (Coding-first)");

  // Step 3-7: 19 ML Features (Lifestyle Assessment)
  // Section 1: Academic
  const [hoursStudied, setHoursStudied] = useState(20);
  const [attendance, setAttendance] = useState(90);
  const [previousScores, setPreviousScores] = useState(85);
  const [tutoringSessions, setTutoringSessions] = useState(1);

  // Section 2: Environment
  const [accessToResources, setAccessToResources] = useState("High");
  const [internetAccess, setInternetAccess] = useState("Yes");
  const [schoolType, setSchoolType] = useState("Public");
  const [teacherQuality, setTeacherQuality] = useState("Medium");

  // Section 3: Lifestyle
  const [sleepHours, setSleepHours] = useState(7.5);
  const [physicalActivity, setPhysicalActivity] = useState(3.5);
  const [extracurricularActivities, setExtracurricularActivities] = useState("Yes");

  // Section 4: Motivation & Support
  const [motivationLevel, setMotivationLevel] = useState("High");
  const [parentalInvolvement, setParentalInvolvement] = useState("Medium");
  const [familyIncome, setFamilyIncome] = useState("Medium");
  const [peerInfluence, setPeerInfluence] = useState("Positive");

  // Section 5: Demographics & Personal
  const [parentalEducationLevel, setParentalEducationLevel] = useState("College");
  const [distanceFromHome, setDistanceFromHome] = useState("Near");
  const [learningDisabilities, setLearningDisabilities] = useState("No");

  // Load saved onboarding status on mount
  useEffect(() => {
    async function loadStatus() {
      if (!userId) return;
      const statusRes = await fetchOnboardingStatus(userId);
      if (statusRes && statusRes.onboardingStatus) {
        const s = statusRes.onboardingStatus;
        if (s.currentStep > 1 && s.currentStep <= 7 && !s.personalCompleted) {
          setStep(s.currentStep);
        }
      }
      const storedName = localStorage.getItem("edupilot_user_name");
      const storedEmail = localStorage.getItem("edupilot_user_email");
      if (storedName) setFullName(storedName);
      if (storedEmail) setEmail(storedEmail);
    }
    loadStatus();
  }, [userId]);

  // Auto-save on step progress
  const autoSaveStep = async (nextStep: number) => {
    setSaving(true);
    let payloadData: any = {};
    if (step === 1) {
      payloadData = {
        fullName, email, phone, dateOfBirth, gender, address, city, state, country,
        collegeName, university, engineeringBranch, currentSemester, rollNumber,
        admissionYear, expectedGraduationYear
      };
    } else if (step === 2) {
      payloadData = {
        engineeringBranch, semester: currentSemester, currentCgpa, targetCgpa,
        currentSubjects: currentSubjects.split(",").map(s => s.trim()),
        weakSubjects: weakSubjects.split(",").map(s => s.trim()),
        strongSubjects: strongSubjects.split(",").map(s => s.trim()),
        careerGoal, dreamCompany,
        programmingLanguages: programmingLanguages.split(",").map(s => s.trim()),
        frameworks: frameworks.split(",").map(s => s.trim()),
        githubUrl, linkedInUrl, weeklyCodingHours, preferredLearningStyle
      };
    } else if (step === 3) {
      payloadData = { hoursStudied, attendance, previousScores, tutoringSessions };
    } else if (step === 4) {
      payloadData = { accessToResources, internetAccess, schoolType, teacherQuality };
    } else if (step === 5) {
      payloadData = { sleepHours, physicalActivity, extracurricularActivities };
    } else if (step === 6) {
      payloadData = { motivationLevel, parentalInvolvement, familyIncome, peerInfluence };
    } else if (step === 7) {
      payloadData = { parentalEducationLevel, distanceFromHome, learningDisabilities, gender };
    }

    await saveOnboardingStep(userId, nextStep, payloadData);
    setSaving(false);
  };

  const handleNext = async () => {
    setError("");
    if (step === 1 && (!fullName.trim() || !collegeName.trim())) {
      setError("Please fill out your full name and college name.");
      return;
    }
    await autoSaveStep(step + 1);
    setStep(prev => Math.min(prev + 1, 7));
  };

  const handleBack = () => {
    setError("");
    setStep(prev => Math.max(prev - 1, 1));
  };

  const handleCompleteOnboarding = async (e: React.FormEvent) => {
    e.preventDefault();
    setSubmitting(true);
    setError("");

    try {
      // 1. Submit Questionnaire 19 ML attributes
      const questPayload: any = {
        studentProfileId: userId,
        hoursStudied: Number(hoursStudied),
        attendance: Number(attendance),
        parentalInvolvement,
        accessToResources,
        extracurricularActivities,
        sleepHours: Number(sleepHours),
        previousScores: Number(previousScores),
        motivationLevel,
        internetAccess,
        tutoringSessions: Number(tutoringSessions),
        familyIncome,
        teacherQuality,
        schoolType,
        peerInfluence,
        physicalActivity: Number(physicalActivity),
        learningDisabilities,
        parentalEducationLevel,
        distanceFromHome,
        gender
      };
      await postQuestionnaire(userId, questPayload);

      // 2. Submit Master Onboarding Completion
      const onboardPayload = {
        userId,
        course: engineeringBranch,
        semester: Number(currentSemester),
        subjects: currentSubjects.split(",").map(s => s.trim()),
        careerGoals: [careerGoal],
        preferredStudyHoursPerDay: Number(hoursStudied) / 7.0,
        targetCgpa: Number(targetCgpa),
        sleepHours: Number(sleepHours),
        stressLevel: 4,
        exerciseMinutes: Number(physicalActivity) * 60 / 7.0,
        learningStyle: preferredLearningStyle
      };

      await onboardStudent(onboardPayload);
      setSubmitting(false);
      navigate("/dashboard");
    } catch (err: any) {
      setSubmitting(false);
      setError(err.message || "Failed to complete onboarding. Please try again.");
    }
  };

  const completionPercentage = Math.round((step / 7) * 100);
  const timeRemaining = Math.max(1, 8 - step);

  return (
    <div className="min-h-screen bg-[#05060b] text-main-theme flex flex-col justify-between p-4 md:p-8 relative overflow-hidden">
      
      {/* Background glow effects */}
      <div className="absolute top-0 left-1/4 w-96 h-96 bg-purple-600/10 rounded-full blur-3xl pointer-events-none" />
      <div className="absolute bottom-0 right-1/4 w-96 h-96 bg-pink-600/10 rounded-full blur-3xl pointer-events-none" />

      {/* Header */}
      <div className="max-w-4xl mx-auto w-full flex justify-between items-center z-10 py-4">
        <div className="flex items-center gap-3">
          <div className="h-10 w-10 rounded-xl bg-purple-600/20 flex items-center justify-center border border-purple-500/30">
            <BrainCircuit className="h-6 w-6 text-purple-theme" />
          </div>
          <div>
            <h1 className="font-extrabold text-lg tracking-wide text-transparent bg-clip-text bg-gradient-to-r from-purple-400 to-pink-500">
              EduPilot AI
            </h1>
            <span className="text-[10px] text-secondary-theme uppercase font-bold tracking-widest">Diagnostic Onboarding Wizard</span>
          </div>
        </div>

        <div className="flex items-center gap-4 text-xs font-semibold text-secondary-theme">
          <div className="flex items-center gap-1 bg-white/5 border border-white/10 px-3 py-1.5 rounded-full">
            <Clock className="h-4 w-4 text-purple-theme" />
            <span>~{timeRemaining} min remaining</span>
          </div>
          {saving && (
            <div className="flex items-center gap-1 text-emerald-theme animate-pulse">
              <Save className="h-3.5 w-3.5" />
              <span className="text-[10px] uppercase font-bold">Auto-saving...</span>
            </div>
          )}
        </div>
      </div>

      {/* Progress Bar Container */}
      <div className="max-w-4xl mx-auto w-full space-y-2 z-10 my-4">
        <div className="flex justify-between text-xs font-bold text-secondary-theme uppercase tracking-wider">
          <span>Step {step} of 7: {getStepTitle(step)}</span>
          <span className="text-purple-theme">{completionPercentage}% Completed</span>
        </div>
        <div className="w-full h-2 bg-white/10 rounded-full overflow-hidden">
          <motion.div 
            className="h-full bg-gradient-to-r from-purple-500 to-pink-500"
            initial={{ width: 0 }}
            animate={{ width: `${completionPercentage}%` }}
            transition={{ duration: 0.3 }}
          />
        </div>
      </div>

      {/* Main Form Card */}
      <div className="max-w-4xl mx-auto w-full z-10 flex-1 flex flex-col justify-center my-4">
        <div className="glass-panel p-6 md:p-10 rounded-3xl border border-white/10 shadow-2xl relative">
          
          {error && (
            <div className="mb-6 p-3 rounded-xl bg-pink-500/10 border border-pink-500/20 text-pink-400 text-xs flex items-center gap-2">
              <ShieldAlert className="h-4 w-4" />
              <span>{error}</span>
            </div>
          )}

          <AnimatePresence mode="wait">
            <motion.div
              key={step}
              initial={{ opacity: 0, x: 20 }}
              animate={{ opacity: 1, x: 0 }}
              exit={{ opacity: 0, x: -20 }}
              transition={{ duration: 0.25 }}
            >
              {/* STEP 1: Personal Information */}
              {step === 1 && (
                <div className="space-y-6">
                  <div className="border-b border-white/10 pb-4">
                    <h2 className="text-xl font-bold text-main-theme flex items-center gap-2">
                      <User className="h-6 w-6 text-purple-theme" />
                      <span>Step 1: Personal & Demographic Information</span>
                    </h2>
                    <p className="text-xs text-secondary-theme mt-1">Tell us about yourself and your academic institution.</p>
                  </div>

                  <div className="grid grid-cols-1 md:grid-cols-2 gap-4 text-xs">
                    <div>
                      <label className="font-bold text-secondary-theme block mb-1">Full Name *</label>
                      <input type="text" value={fullName} onChange={e => setFullName(e.target.value)} className="w-full p-3 rounded-xl glass-input" placeholder="e.g. Alex Wright" />
                    </div>

                    <div>
                      <label className="font-bold text-secondary-theme block mb-1">Email Address *</label>
                      <input type="email" value={email} onChange={e => setEmail(e.target.value)} className="w-full p-3 rounded-xl glass-input" placeholder="alex@university.edu" />
                    </div>

                    <div>
                      <label className="font-bold text-secondary-theme block mb-1">Phone Number</label>
                      <input type="text" value={phone} onChange={e => setPhone(e.target.value)} className="w-full p-3 rounded-xl glass-input" placeholder="+1 (555) 019-2834" />
                    </div>

                    <div>
                      <label className="font-bold text-secondary-theme block mb-1">Gender</label>
                      <select value={gender} onChange={e => setGender(e.target.value)} className="w-full p-3 rounded-xl glass-input">
                        <option value="Male">Male</option>
                        <option value="Female">Female</option>
                        <option value="Other">Other</option>
                      </select>
                    </div>

                    <div>
                      <label className="font-bold text-secondary-theme block mb-1">College / Institute Name *</label>
                      <input type="text" value={collegeName} onChange={e => setCollegeName(e.target.value)} className="w-full p-3 rounded-xl glass-input" placeholder="e.g. Institute of Technology" />
                    </div>

                    <div>
                      <label className="font-bold text-secondary-theme block mb-1">University</label>
                      <input type="text" value={university} onChange={e => setUniversity(e.target.value)} className="w-full p-3 rounded-xl glass-input" placeholder="e.g. State University" />
                    </div>

                    <div>
                      <label className="font-bold text-secondary-theme block mb-1">Engineering Branch</label>
                      <select value={engineeringBranch} onChange={e => setEngineeringBranch(e.target.value)} className="w-full p-3 rounded-xl glass-input">
                        <option value="Computer Science & Engineering">Computer Science & Engineering</option>
                        <option value="Artificial Intelligence & Data Science">Artificial Intelligence & Data Science</option>
                        <option value="Information Technology">Information Technology</option>
                        <option value="Electronics & Communication">Electronics & Communication</option>
                        <option value="Electrical Engineering">Electrical Engineering</option>
                        <option value="Mechanical Engineering">Mechanical Engineering</option>
                      </select>
                    </div>

                    <div>
                      <label className="font-bold text-secondary-theme block mb-1">Current Semester</label>
                      <input type="number" min="1" max="8" value={currentSemester} onChange={e => setCurrentSemester(Number(e.target.value))} className="w-full p-3 rounded-xl glass-input" />
                    </div>
                  </div>
                </div>
              )}

              {/* STEP 2: Academic Profile & Stream */}
              {step === 2 && (
                <div className="space-y-6">
                  <div className="border-b border-white/10 pb-4">
                    <h2 className="text-xl font-bold text-main-theme flex items-center gap-2">
                      <GraduationCap className="h-6 w-6 text-purple-theme" />
                      <span>Step 2: Academic Profile & Career Goals</span>
                    </h2>
                    <p className="text-xs text-secondary-theme mt-1">Configure your CGPA benchmarks, subjects, and engineering stack targets.</p>
                  </div>

                  <div className="grid grid-cols-1 md:grid-cols-2 gap-4 text-xs">
                    <div>
                      <label className="font-bold text-secondary-theme block mb-1">Current CGPA (0.0 - 10.0)</label>
                      <input type="number" step="0.1" value={currentCgpa} onChange={e => setCurrentCgpa(Number(e.target.value))} className="w-full p-3 rounded-xl glass-input" />
                    </div>

                    <div>
                      <label className="font-bold text-secondary-theme block mb-1">Target CGPA (0.0 - 10.0)</label>
                      <input type="number" step="0.1" value={targetCgpa} onChange={e => setTargetCgpa(Number(e.target.value))} className="w-full p-3 rounded-xl glass-input" />
                    </div>

                    <div className="md:col-span-2">
                      <label className="font-bold text-secondary-theme block mb-1">Current Semester Subjects (Comma Separated)</label>
                      <input type="text" value={currentSubjects} onChange={e => setCurrentSubjects(e.target.value)} className="w-full p-3 rounded-xl glass-input" />
                    </div>

                    <div>
                      <label className="font-bold text-secondary-theme block mb-1">Perceived Weak Topics / Subjects</label>
                      <input type="text" value={weakSubjects} onChange={e => setWeakSubjects(e.target.value)} className="w-full p-3 rounded-xl glass-input" />
                    </div>

                    <div>
                      <label className="font-bold text-secondary-theme block mb-1">Strong Mastery Topics</label>
                      <input type="text" value={strongSubjects} onChange={e => setStrongSubjects(e.target.value)} className="w-full p-3 rounded-xl glass-input" />
                    </div>

                    <div>
                      <label className="font-bold text-secondary-theme block mb-1">Target Career Role</label>
                      <input type="text" value={careerGoal} onChange={e => setCareerGoal(e.target.value)} className="w-full p-3 rounded-xl glass-input" placeholder="e.g. AI Specialist" />
                    </div>

                    <div>
                      <label className="font-bold text-secondary-theme block mb-1">Dream Target Companies</label>
                      <input type="text" value={dreamCompany} onChange={e => setDreamCompany(e.target.value)} className="w-full p-3 rounded-xl glass-input" placeholder="e.g. Google, Apple" />
                    </div>

                    <div>
                      <label className="font-bold text-secondary-theme block mb-1">Weekly Coding Hours</label>
                      <input type="number" value={weeklyCodingHours} onChange={e => setWeeklyCodingHours(Number(e.target.value))} className="w-full p-3 rounded-xl glass-input" />
                    </div>

                    <div>
                      <label className="font-bold text-secondary-theme block mb-1">Preferred Learning Style</label>
                      <select value={preferredLearningStyle} onChange={e => setPreferredLearningStyle(e.target.value)} className="w-full p-3 rounded-xl glass-input">
                        <option value="Kinesthetic (Coding-first)">Kinesthetic (Coding-first)</option>
                        <option value="Visual (Diagrams & Flowcharts)">Visual (Diagrams & Flowcharts)</option>
                        <option value="Auditory (Lectures & Discussions)">Auditory (Lectures & Discussions)</option>
                        <option value="Reading / Writing">Reading / Writing</option>
                      </select>
                    </div>
                  </div>
                </div>
              )}

              {/* STEP 3: Lifestyle Assessment - Section 1 (Academic) */}
              {step === 3 && (
                <div className="space-y-6">
                  <div className="border-b border-white/10 pb-4">
                    <h2 className="text-xl font-bold text-main-theme flex items-center gap-2">
                      <BookOpen className="h-6 w-6 text-purple-theme" />
                      <span>Step 3: Lifestyle Assessment - Section 1 (Academic Habits)</span>
                    </h2>
                    <p className="text-xs text-secondary-theme mt-1">Core study frequency and attendance metrics used by the ML model.</p>
                  </div>

                  <div className="grid grid-cols-1 md:grid-cols-2 gap-6 text-xs">
                    <div className="space-y-2">
                      <label className="font-bold text-secondary-theme block">Weekly Study Hours: {hoursStudied} hrs/week</label>
                      <input type="range" min="0" max="60" value={hoursStudied} onChange={e => setHoursStudied(Number(e.target.value))} className="w-full accent-purple-500" />
                    </div>

                    <div className="space-y-2">
                      <label className="font-bold text-secondary-theme block">Class Attendance Rate: {attendance}%</label>
                      <input type="range" min="40" max="100" value={attendance} onChange={e => setAttendance(Number(e.target.value))} className="w-full accent-purple-500" />
                    </div>

                    <div className="space-y-2">
                      <label className="font-bold text-secondary-theme block">Previous Exam Score Percentage: {previousScores}%</label>
                      <input type="range" min="35" max="100" value={previousScores} onChange={e => setPreviousScores(Number(e.target.value))} className="w-full accent-purple-500" />
                    </div>

                    <div className="space-y-2">
                      <label className="font-bold text-secondary-theme block">Tutoring / Mentorship Sessions: {tutoringSessions} per week</label>
                      <input type="number" min="0" max="10" value={tutoringSessions} onChange={e => setTutoringSessions(Number(e.target.value))} className="w-full p-3 rounded-xl glass-input" />
                    </div>
                  </div>
                </div>
              )}

              {/* STEP 4: Lifestyle Assessment - Section 2 (Study Environment) */}
              {step === 4 && (
                <div className="space-y-6">
                  <div className="border-b border-white/10 pb-4">
                    <h2 className="text-xl font-bold text-main-theme flex items-center gap-2">
                      <Building2 className="h-6 w-6 text-purple-theme" />
                      <span>Step 4: Lifestyle Assessment - Section 2 (Study Environment)</span>
                    </h2>
                    <p className="text-xs text-secondary-theme mt-1">Resource access and educational quality indicators.</p>
                  </div>

                  <div className="grid grid-cols-1 md:grid-cols-2 gap-4 text-xs">
                    <div>
                      <label className="font-bold text-secondary-theme block mb-1">Access to Educational Resources</label>
                      <select value={accessToResources} onChange={e => setAccessToResources(e.target.value)} className="w-full p-3 rounded-xl glass-input">
                        <option value="High">High (Abundant books, labs, cloud compute)</option>
                        <option value="Medium">Medium (Standard campus access)</option>
                        <option value="Low">Low (Restricted access)</option>
                      </select>
                    </div>

                    <div>
                      <label className="font-bold text-secondary-theme block mb-1">High-Speed Internet Access</label>
                      <select value={internetAccess} onChange={e => setInternetAccess(e.target.value)} className="w-full p-3 rounded-xl glass-input">
                        <option value="Yes">Yes</option>
                        <option value="No">No</option>
                      </select>
                    </div>

                    <div>
                      <label className="font-bold text-secondary-theme block mb-1">School / Institution Type</label>
                      <select value={schoolType} onChange={e => setSchoolType(e.target.value)} className="w-full p-3 rounded-xl glass-input">
                        <option value="Public">Public / State University</option>
                        <option value="Private">Private Autonomous College</option>
                      </select>
                    </div>

                    <div>
                      <label className="font-bold text-secondary-theme block mb-1">Perceived Teacher Quality</label>
                      <select value={teacherQuality} onChange={e => setTeacherQuality(e.target.value)} className="w-full p-3 rounded-xl glass-input">
                        <option value="High">High</option>
                        <option value="Medium">Medium</option>
                        <option value="Low">Low</option>
                      </select>
                    </div>
                  </div>
                </div>
              )}

              {/* STEP 5: Lifestyle Assessment - Section 3 (Physical & Mental Wellness) */}
              {step === 5 && (
                <div className="space-y-6">
                  <div className="border-b border-white/10 pb-4">
                    <h2 className="text-xl font-bold text-main-theme flex items-center gap-2">
                      <Activity className="h-6 w-6 text-purple-theme" />
                      <span>Step 5: Lifestyle Assessment - Section 3 (Sleep & Physical Wellness)</span>
                    </h2>
                    <p className="text-xs text-secondary-theme mt-1">Sleep patterns and physical exercise baseline.</p>
                  </div>

                  <div className="grid grid-cols-1 md:grid-cols-2 gap-6 text-xs">
                    <div className="space-y-2">
                      <label className="font-bold text-secondary-theme block">Average Daily Sleep Hours: {sleepHours} hours/night</label>
                      <input type="range" min="4" max="12" step="0.5" value={sleepHours} onChange={e => setSleepHours(Number(e.target.value))} className="w-full accent-purple-500" />
                    </div>

                    <div className="space-y-2">
                      <label className="font-bold text-secondary-theme block">Weekly Physical Activity: {physicalActivity} hours/week</label>
                      <input type="range" min="0" max="14" step="0.5" value={physicalActivity} onChange={e => setPhysicalActivity(Number(e.target.value))} className="w-full accent-purple-500" />
                    </div>

                    <div className="md:col-span-2">
                      <label className="font-bold text-secondary-theme block mb-1">Participation in Extracurricular Activities / Clubs</label>
                      <select value={extracurricularActivities} onChange={e => setExtracurricularActivities(e.target.value)} className="w-full p-3 rounded-xl glass-input">
                        <option value="Yes">Yes (Active member in Hackathon / Robotics / Tech clubs)</option>
                        <option value="No">No</option>
                      </select>
                    </div>
                  </div>
                </div>
              )}

              {/* STEP 6: Lifestyle Assessment - Section 4 (Motivation & Social Support) */}
              {step === 6 && (
                <div className="space-y-6">
                  <div className="border-b border-white/10 pb-4">
                    <h2 className="text-xl font-bold text-main-theme flex items-center gap-2">
                      <HeartHandshake className="h-6 w-6 text-purple-theme" />
                      <span>Step 6: Lifestyle Assessment - Section 4 (Motivation & Peer Support)</span>
                    </h2>
                    <p className="text-xs text-secondary-theme mt-1">Social circle dynamics and drive indicators.</p>
                  </div>

                  <div className="grid grid-cols-1 md:grid-cols-2 gap-4 text-xs">
                    <div>
                      <label className="font-bold text-secondary-theme block mb-1">Current Academic Motivation Level</label>
                      <select value={motivationLevel} onChange={e => setMotivationLevel(e.target.value)} className="w-full p-3 rounded-xl glass-input">
                        <option value="High">High (Extremely driven)</option>
                        <option value="Medium">Medium (Balanced)</option>
                        <option value="Low">Low (Experiencing burnout)</option>
                      </select>
                    </div>

                    <div>
                      <label className="font-bold text-secondary-theme block mb-1">Parental Involvement in Studies</label>
                      <select value={parentalInvolvement} onChange={e => setParentalInvolvement(e.target.value)} className="w-full p-3 rounded-xl glass-input">
                        <option value="High">High</option>
                        <option value="Medium">Medium</option>
                        <option value="Low">Low</option>
                      </select>
                    </div>

                    <div>
                      <label className="font-bold text-secondary-theme block mb-1">Family Income Bracket</label>
                      <select value={familyIncome} onChange={e => setFamilyIncome(e.target.value)} className="w-full p-3 rounded-xl glass-input">
                        <option value="High">High</option>
                        <option value="Medium">Medium</option>
                        <option value="Low">Low</option>
                      </select>
                    </div>

                    <div>
                      <label className="font-bold text-secondary-theme block mb-1">Peer Group Influence</label>
                      <select value={peerInfluence} onChange={e => setPeerInfluence(e.target.value)} className="w-full p-3 rounded-xl glass-input">
                        <option value="Positive">Positive (Collaborative study environment)</option>
                        <option value="Neutral">Neutral</option>
                        <option value="Negative">Negative (Distractive environment)</option>
                      </select>
                    </div>
                  </div>
                </div>
              )}

              {/* STEP 7: Lifestyle Assessment - Section 5 (Demographic Baseline) */}
              {step === 7 && (
                <div className="space-y-6">
                  <div className="border-b border-white/10 pb-4">
                    <h2 className="text-xl font-bold text-main-theme flex items-center gap-2">
                      <Sparkles className="h-6 w-6 text-purple-theme" />
                      <span>Step 7: Final Demographic Verification & AI Execution</span>
                    </h2>
                    <p className="text-xs text-secondary-theme mt-1">Final background attributes before launching Random Forest model inference.</p>
                  </div>

                  <div className="grid grid-cols-1 md:grid-cols-2 gap-4 text-xs">
                    <div>
                      <label className="font-bold text-secondary-theme block mb-1">Parental Education Level</label>
                      <select value={parentalEducationLevel} onChange={e => setParentalEducationLevel(e.target.value)} className="w-full p-3 rounded-xl glass-input">
                        <option value="Postgraduate">Postgraduate</option>
                        <option value="College">College Graduate</option>
                        <option value="High School">High School</option>
                      </select>
                    </div>

                    <div>
                      <label className="font-bold text-secondary-theme block mb-1">Distance from Home to Campus</label>
                      <select value={distanceFromHome} onChange={e => setDistanceFromHome(e.target.value)} className="w-full p-3 rounded-xl glass-input">
                        <option value="Near">Near (&lt; 5 km)</option>
                        <option value="Moderate">Moderate (5 - 15 km)</option>
                        <option value="Far">Far (&gt; 15 km / Hostel)</option>
                      </select>
                    </div>

                    <div className="md:col-span-2">
                      <label className="font-bold text-secondary-theme block mb-1">Learning Disabilities / Special Accommodations</label>
                      <select value={learningDisabilities} onChange={e => setLearningDisabilities(e.target.value)} className="w-full p-3 rounded-xl glass-input">
                        <option value="No">No</option>
                        <option value="Yes">Yes</option>
                      </select>
                    </div>
                  </div>
                </div>
              )}
            </motion.div>
          </AnimatePresence>

          {/* Bottom Action Controls */}
          <div className="flex justify-between items-center mt-8 pt-6 border-t border-white/10">
            <button
              type="button"
              onClick={handleBack}
              disabled={step === 1 || submitting}
              className={`flex items-center gap-2 px-5 py-2.5 rounded-xl font-bold text-xs transition-all ${
                step === 1 ? "opacity-30 cursor-not-allowed text-secondary-theme" : "bg-white/5 hover:bg-white/10 text-main-theme"
              }`}
            >
              <ArrowLeft className="h-4 w-4" />
              <span>Back</span>
            </button>

            {step < 7 ? (
              <button
                type="button"
                onClick={handleNext}
                disabled={submitting}
                className="flex items-center gap-2 px-6 py-2.5 rounded-xl bg-gradient-to-r from-purple-600 to-pink-600 hover:from-purple-500 hover:to-pink-500 text-white font-bold text-xs shadow-lg shadow-purple-500/20 transition-all cursor-pointer"
              >
                <span>Continue</span>
                <ArrowRight className="h-4 w-4" />
              </button>
            ) : (
              <button
                type="button"
                onClick={handleCompleteOnboarding}
                disabled={submitting}
                className="flex items-center gap-2 px-8 py-3 rounded-xl bg-gradient-to-r from-emerald-500 to-teal-500 hover:from-emerald-400 hover:to-teal-400 text-white font-black text-xs shadow-xl shadow-emerald-500/20 transition-all cursor-pointer animate-pulse"
              >
                {submitting ? (
                  <span>Executing AI Analysis...</span>
                ) : (
                  <>
                    <Sparkles className="h-4 w-4" />
                    <span>Complete Onboarding & Compute SGI</span>
                  </>
                )}
              </button>
            )}
          </div>

        </div>
      </div>

    </div>
  );
}

function getStepTitle(step: number): string {
  switch (step) {
    case 1: return "Personal Information";
    case 2: return "Academic Profile & Stream";
    case 3: return "Academic Habits";
    case 4: return "Study Environment";
    case 5: return "Sleep & Physical Wellness";
    case 6: return "Motivation & Social Circle";
    case 7: return "Demographics & AI Analysis Launch";
    default: return "";
  }
}
