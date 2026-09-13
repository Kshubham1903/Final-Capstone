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
  Heart,
  Compass,
  Building2,
  Briefcase,
  Code,
  Plus,
  Trash2,
  PlusCircle,
  Check
} from "lucide-react";
import { saveOnboardingStep, onboardStudent, fetchOnboardingStatus, postQuestionnaire, fetchSubjectsByBranchAndSemester, fetchFullProfile, startDiagnosticAssessment, submitDiagnosticAssessment, fetchLatestDiagnosticResult, fetchNextInitialDiagnosticQuestion, submitInitialDiagnosticAnswer } from "../../services/api";

const FORM_B_SECTIONS = [
  {
    id: "B1",
    title: "SECTION B1 — VISUAL & DEMONSTRATION PREFERENCE",
    questions: [
      { id: "B1.1", text: "I understand concepts better when they are explained using diagrams, flowcharts, or visual representations." },
      { id: "B1.2", text: "I prefer learning through videos or demonstrations rather than only reading text." },
      { id: "B1.3", text: "I understand programming concepts better when I see a worked example." },
      { id: "B1.4", text: "I find animations or visual demonstrations helpful when learning difficult concepts." }
    ]
  },
  {
    id: "B2",
    title: "SECTION B2 — READING & EXPLANATION PREFERENCE",
    questions: [
      { id: "B2.1", text: "I prefer written explanations, notes, or textbooks when learning a new concept." },
      { id: "B2.2", text: "I prefer detailed explanations before attempting exercises." },
      { id: "B2.3", text: "I prefer concise explanations rather than long theoretical explanations." },
      { id: "B2.4", text: "I like to read the theory of a concept before seeing examples or demonstrations." }
    ]
  },
  {
    id: "B3",
    title: "SECTION B3 — PRACTICAL & INTERACTIVE LEARNING",
    questions: [
      { id: "B3.1", text: "I learn better by solving problems or coding exercises myself." },
      { id: "B3.2", text: "I prefer practicing a concept immediately after learning its theory." },
      { id: "B3.3", text: "I prefer interactive examples where I can try the concept myself." },
      { id: "B3.4", text: "I enjoy experimenting with code and discovering solutions on my own." }
    ]
  },
  {
    id: "B4",
    title: "SECTION B4 — LEARNING STRUCTURE & PROGRESSION",
    questions: [
      { id: "B4.1", text: "I prefer concepts to be explained in a clear sequence from basic to advanced." },
      { id: "B4.2", text: "I prefer step-by-step activities when learning a difficult concept." },
      { id: "B4.3", text: "I prefer learning through real-world examples and applications." },
      { id: "B4.4", text: "I prefer multiple examples to understand how a concept works in different situations." }
    ]
  },
  {
    id: "B5",
    title: "SECTION B5 — PRACTICE, FEEDBACK & ADAPTATION",
    questions: [
      { id: "B5.1", text: "Short quizzes help me understand whether I have learned a concept correctly." },
      { id: "B5.2", text: "I prefer receiving immediate feedback after answering a question." },
      { id: "B5.3", text: "I prefer practice problems with gradually increasing difficulty." },
      { id: "B5.4", text: "I prefer revisiting a concept when I make repeated mistakes." }
    ]
  }
];

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
  const [collegeName, setCollegeName] = useState("EduPilot Academy");
  const [university, setUniversity] = useState("EduPilot University");
  const [engineeringBranch, setEngineeringBranch] = useState("B.Tech CSE");
  const [currentSemester, setCurrentSemester] = useState(1);
  const [rollNumber, setRollNumber] = useState("EP-2025-001");
  const [admissionYear, setAdmissionYear] = useState(2025);
  const [expectedGraduationYear, setExpectedGraduationYear] = useState(2029);

  // Form A Specific fields
  const [academicYear, setAcademicYear] = useState("2025–26");
  const [semesterRoman, setSemesterRoman] = useState("I");
  const [division, setDivision] = useState("A");
  const [previousProgrammingExperience, setPreviousProgrammingExperience] = useState("None");

  const [previousSemesterSgpa, setPreviousSemesterSgpa] = useState<string>("");
  const [previousSemesterPercentage, setPreviousSemesterPercentage] = useState<string>("");
  const [mathematicsScore, setMathematicsScore] = useState<string>("");
  const [programmingScore, setProgrammingScore] = useState<string>("");
  const [dataStructuresScore, setDataStructuresScore] = useState<string>("");
  const [dbmsScore, setDbmsScore] = useState<string>("");
  const [attendancePercentage, setAttendancePercentage] = useState<string>("");
  const [numberOfBacklogs, setNumberOfBacklogs] = useState<string>("");

  const [programmingConfidence, setProgrammingConfidence] = useState<number>(3);
  const [dataStructuresConfidence, setDataStructuresConfidence] = useState<number>(3);
  const [dbmsConfidence, setDbmsConfidence] = useState<number>(3);
  const [mathematicsConfidence, setMathematicsConfidence] = useState<number>(3);
  const [algorithmsConfidence, setAlgorithmsConfidence] = useState<number>(3);

  // Form B State: Likert scale questionnaire (1 = Strongly Disagree, 5 = Strongly Agree, 0 = Unselected)
  const [learningPreferences, setLearningPreferences] = useState<Record<string, number>>({
    "B1.1": 0, "B1.2": 0, "B1.3": 0, "B1.4": 0,
    "B2.1": 0, "B2.2": 0, "B2.3": 0, "B2.4": 0,
    "B3.1": 0, "B3.2": 0, "B3.3": 0, "B3.4": 0,
    "B4.1": 0, "B4.2": 0, "B4.3": 0, "B4.4": 0,
    "B5.1": 0, "B5.2": 0, "B5.3": 0, "B5.4": 0
  });

  // Step 2: Academic Profile
  const [currentCgpa, setCurrentCgpa] = useState(8.2);
  const [targetCgpa, setTargetCgpa] = useState(9.2);
  const [currentSubjects, setCurrentSubjects] = useState("Data Structures & Algorithms, Database Management Systems, Artificial Intelligence");
  const [catalogSubjects, setCatalogSubjects] = useState<any[]>([]);
  const [customSubjectInput, setCustomSubjectInput] = useState("");
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

  // Form C State Variables: Well-being & Learning Context
  const [consent, setConsent] = useState<string>("");
  const [c2Motivation, setC2Motivation] = useState<number>(0);
  const [c2Focus, setC2Focus] = useState<number>(0);
  const [c2Fatigue, setC2Fatigue] = useState<number>(0);
  const [c2Stress, setC2Stress] = useState<number>(0);
  const [c2Confidence, setC2Confidence] = useState<number>(0);
  const [c2WorkloadComfort, setC2WorkloadComfort] = useState<number>(0);
  const [c2Satisfaction, setC2Satisfaction] = useState<number>(0);
  const [c3SleepDuration, setC3SleepDuration] = useState<string>("");
  const [c3PreferredStudyTime, setC3PreferredStudyTime] = useState<string>("");
  const [c3StudyEnvironment, setC3StudyEnvironment] = useState<string>("");
  const [c3AcademicWorkload, setC3AcademicWorkload] = useState<string>("");
  const [c3PendingAssignments, setC3PendingAssignments] = useState<string>("");
  const [c3AcademicDifficulty, setC3AcademicDifficulty] = useState<string>("");
  const [c3NeedSupport, setC3NeedSupport] = useState<string>("");

  // Form D State Variables: Diagnostic Knowledge Assessment
  const [assessmentSessionId, setAssessmentSessionId] = useState<string>("");
  const [questions, setQuestions] = useState<any[]>([]);
  const [answers, setAnswers] = useState<Record<string, number>>({});
  const [currentQuestionIndex, setCurrentQuestionIndex] = useState<number>(0);
  const [loadingQuestions, setLoadingQuestions] = useState<boolean>(false);
  const [submittingAssessment, setSubmittingAssessment] = useState<boolean>(false);
  const [totalQuestions, setTotalQuestions] = useState<number>(25);
  const [loadingNextQuestion, setLoadingNextQuestion] = useState<boolean>(false);
  const [questionStartTime, setQuestionStartTime] = useState<number>(Date.now());

  // Assessment Results state (summary page)
  const [diagnosticResult, setDiagnosticResult] = useState<any>(null);
  const [diagnosticCompleted, setDiagnosticCompleted] = useState<boolean>(false);

  // Load saved onboarding status on mount
  useEffect(() => {
    async function loadStatus() {
      if (!userId) return;

      try {
        const statusRes = await fetchOnboardingStatus(userId);
        if (statusRes && statusRes.onboardingStatus) {
          const s = statusRes.onboardingStatus;
          if (s.currentStep > 1 && s.currentStep <= 4) {
            setStep(s.currentStep);
          }
        }
      } catch (err) {
        console.warn("Failed to fetch onboarding status:", err);
      }

      try {
        const fullProfileRes = await fetchFullProfile(userId);
        if (fullProfileRes && fullProfileRes.personalInfo) {
          const p = fullProfileRes.personalInfo;
          if (p.fullName) setFullName(p.fullName);
          if (p.email) setEmail(p.email);
          if (p.academicYear) setAcademicYear(p.academicYear);
          if (p.engineeringBranch) setEngineeringBranch(p.engineeringBranch);
          if (p.currentSemester) {
            const romanMap: Record<number, string> = {
              1: "I", 2: "II", 3: "III", 4: "IV", 5: "V", 6: "VI", 7: "VII", 8: "VIII"
            };
            setSemesterRoman(romanMap[p.currentSemester] || "I");
          }
          if (p.division) setDivision(p.division);
          if (p.previousProgrammingExperience) setPreviousProgrammingExperience(p.previousProgrammingExperience);
          if (p.previousSemesterSgpa !== undefined && p.previousSemesterSgpa !== null) setPreviousSemesterSgpa(String(p.previousSemesterSgpa));
          if (p.previousSemesterPercentage !== undefined && p.previousSemesterPercentage !== null) setPreviousSemesterPercentage(String(p.previousSemesterPercentage));
          if (p.mathematicsScore !== undefined && p.mathematicsScore !== null) setMathematicsScore(String(p.mathematicsScore));
          if (p.programmingScore !== undefined && p.programmingScore !== null) setProgrammingScore(String(p.programmingScore));
          if (p.dataStructuresScore !== undefined && p.dataStructuresScore !== null) setDataStructuresScore(String(p.dataStructuresScore));
          if (p.dbmsScore !== undefined && p.dbmsScore !== null) setDbmsScore(String(p.dbmsScore));
          if (p.attendancePercentage !== undefined && p.attendancePercentage !== null) setAttendancePercentage(String(p.attendancePercentage));
          if (p.numberOfBacklogs !== undefined && p.numberOfBacklogs !== null) setNumberOfBacklogs(String(p.numberOfBacklogs));
          if (p.programmingConfidence !== undefined && p.programmingConfidence !== null) setProgrammingConfidence(Number(p.programmingConfidence));
          if (p.dataStructuresConfidence !== undefined && p.dataStructuresConfidence !== null) setDataStructuresConfidence(Number(p.dataStructuresConfidence));
          if (p.dbmsConfidence !== undefined && p.dbmsConfidence !== null) setDbmsConfidence(Number(p.dbmsConfidence));
          if (p.mathematicsConfidence !== undefined && p.mathematicsConfidence !== null) setMathematicsConfidence(Number(p.mathematicsConfidence));
          if (p.algorithmsConfidence !== undefined && p.algorithmsConfidence !== null) setAlgorithmsConfidence(Number(p.algorithmsConfidence));
        }
        if (fullProfileRes && fullProfileRes.learningPreferenceQuestionnaire) {
          const lp = fullProfileRes.learningPreferenceQuestionnaire;
          if (lp.responses) {
            setLearningPreferences(prev => ({ ...prev, ...lp.responses }));
          }
        }
        if (fullProfileRes && fullProfileRes.wellBeingLearningContext) {
          const wb = fullProfileRes.wellBeingLearningContext;
          if (wb.consent !== undefined) {
            setConsent(wb.consent ? "I Agree" : "I Do Not Agree");
          }
          if (wb.motivation) setC2Motivation(wb.motivation);
          if (wb.focus) setC2Focus(wb.focus);
          if (wb.mentalFatigue) setC2Fatigue(wb.mentalFatigue);
          if (wb.academicStress) setC2Stress(wb.academicStress);
          if (wb.currentLearningConfidence) setC2Confidence(wb.currentLearningConfidence);
          if (wb.workloadComfort) setC2WorkloadComfort(wb.workloadComfort);
          if (wb.learningSatisfaction) setC2Satisfaction(wb.learningSatisfaction);
          if (wb.sleepDuration) setC3SleepDuration(wb.sleepDuration);
          if (wb.preferredStudyTime) setC3PreferredStudyTime(wb.preferredStudyTime);
          if (wb.studyEnvironment) setC3StudyEnvironment(wb.studyEnvironment);
          if (wb.currentAcademicWorkload) setC3AcademicWorkload(wb.currentAcademicWorkload);
          if (wb.pendingAssignments) setC3PendingAssignments(wb.pendingAssignments);
          if (wb.majorAcademicDifficulty) setC3AcademicDifficulty(wb.majorAcademicDifficulty);
          if (wb.needsAcademicSupport !== undefined) {
            setC3NeedSupport(wb.needsAcademicSupport ? "Yes" : "No");
          }
        }

        try {
          const latestRes = await fetchLatestDiagnosticResult(userId);
          if (latestRes && latestRes.subjectCode === "DS-LL-PRE01") {
            setDiagnosticResult(latestRes);
            setDiagnosticCompleted(true);
          }
        } catch (err) {
          console.warn("Failed to fetch latest diagnostic result:", err);
        }
      } catch (err) {
        console.warn("Failed to fetch full profile for pre-population:", err);
        const storedName = localStorage.getItem("edupilot_user_name");
        const storedEmail = localStorage.getItem("edupilot_user_email");
        if (storedName) setFullName(storedName);
        if (storedEmail) setEmail(storedEmail);
      }
    }
    loadStatus();
  }, [userId]);

  // Load Diagnostic Assessment Questions for Step 4
  useEffect(() => {
    async function initAssessment() {
      if (step === 4 && questions.length === 0 && !diagnosticCompleted && userId) {
        setLoadingQuestions(true);
        try {
          const session = await startDiagnosticAssessment({
            userId,
            branch: engineeringBranch || "Computer Science & Engineering",
            semester: currentSemester || 3,
            subjectCode: "DS-LL-PRE01",
            questionCount: 25
          });
          if (session && session.questions) {
            setAssessmentSessionId(session.sessionId);
            setQuestions(session.questions);
            if (session.totalQuestions) {
              setTotalQuestions(session.totalQuestions);
            }
            setQuestionStartTime(Date.now());
          }
        } catch (err) {
          console.error("Failed to start assessment session:", err);
          setError("Failed to load assessment questions. Please try refreshing.");
        } finally {
          setLoadingQuestions(false);
        }
      }
    }
    initAssessment();
  }, [step, questions.length, diagnosticCompleted, userId, engineeringBranch, currentSemester]);

  // Auto-save on step progress
  const autoSaveStep = async (nextStep: number) => {
    setSaving(true);
    let payloadData: any = {};
    if (step === 1) {
      const romanToInt: Record<string, number> = {
        "I": 1, "II": 2, "III": 3, "IV": 4, "V": 5, "VI": 6, "VII": 7, "VIII": 8
      };
      const semInt = romanToInt[semesterRoman] || 1;

      payloadData = {
        fullName, email, phone, dateOfBirth, gender, address, city, state, country,
        collegeName, university, engineeringBranch, currentSemester: semInt, rollNumber,
        admissionYear, expectedGraduationYear,
        academicYear,
        division,
        previousProgrammingExperience,
        previousSemesterSgpa: previousSemesterSgpa !== "" ? Number(previousSemesterSgpa) : 0.0,
        previousSemesterPercentage: previousSemesterPercentage !== "" ? Number(previousSemesterPercentage) : 0.0,
        mathematicsScore: mathematicsScore !== "" ? Number(mathematicsScore) : 0.0,
        programmingScore: programmingScore !== "" ? Number(programmingScore) : 0.0,
        dataStructuresScore: dataStructuresScore !== "" ? Number(dataStructuresScore) : 0.0,
        dbmsScore: dbmsScore !== "" ? Number(dbmsScore) : 0.0,
        attendancePercentage: attendancePercentage !== "" ? Number(attendancePercentage) : 0.0,
        numberOfBacklogs: numberOfBacklogs !== "" ? Number(numberOfBacklogs) : 0,
        programmingConfidence,
        dataStructuresConfidence,
        dbmsConfidence,
        mathematicsConfidence,
        algorithmsConfidence
      };
    } else if (step === 2) {
      payloadData = {
        responses: learningPreferences
      };
    } else if (step === 3) {
      if (consent === "I Agree") {
        payloadData = {
          consent: true,
          motivation: c2Motivation,
          focus: c2Focus,
          mentalFatigue: c2Fatigue,
          academicStress: c2Stress,
          currentLearningConfidence: c2Confidence,
          workloadComfort: c2WorkloadComfort,
          learningSatisfaction: c2Satisfaction,
          sleepDuration: c3SleepDuration,
          preferredStudyTime: c3PreferredStudyTime,
          studyEnvironment: c3StudyEnvironment,
          currentAcademicWorkload: c3AcademicWorkload,
          pendingAssignments: c3PendingAssignments,
          majorAcademicDifficulty: c3AcademicDifficulty,
          needsAcademicSupport: c3NeedSupport === "Yes"
        };
      } else {
        payloadData = {
          consent: false
        };
      }
    }

    await saveOnboardingStep(userId, nextStep, payloadData);
    setSaving(false);
  };

  const handleSelectOption = (questionId: string, optionIndex: number) => {
    setAnswers(prev => ({ ...prev, [questionId]: optionIndex }));
  };

  const handleNextQuestion = async () => {
    setError("");
    const activeQuestion = questions[currentQuestionIndex];
    if (!activeQuestion) return;

    const activeQuestionId = activeQuestion.questionId || activeQuestion.id;
    const selectedOpt = answers[activeQuestionId];

    if (selectedOpt === undefined || selectedOpt === null) {
      setError("Please select an answer before proceeding to the next question.");
      return;
    }

    // If next question is already fetched (e.g. user clicked Previous then Next), simply advance index
    if (currentQuestionIndex < questions.length - 1) {
      setCurrentQuestionIndex(prev => prev + 1);
      setQuestionStartTime(Date.now());
      return;
    }

    if (loadingNextQuestion || submittingAssessment) return;

    setLoadingNextQuestion(true);
    const timeTakenSeconds = Math.max(1, Math.round((Date.now() - questionStartTime) / 1000));

    try {
      // 1. Submit current answer to backend initial submit endpoint
      try {
        await submitInitialDiagnosticAnswer({
          sessionId: assessmentSessionId,
          questionId: activeQuestionId,
          selectedOption: selectedOpt,
          responseTimeSeconds: timeTakenSeconds
        });
      } catch (submitErr) {
        console.warn("[Form D] Initial submit warning (continuing to fetch next):", submitErr);
      }

      // 2. Fetch next initial question
      const nextRes = await fetchNextInitialDiagnosticQuestion({ sessionId: assessmentSessionId });

      if (nextRes && nextRes.question) {
        const normalizedQ = {
          ...nextRes.question,
          questionId: nextRes.question.questionId || nextRes.question.id,
          id: nextRes.question.questionId || nextRes.question.id,
          topic: nextRes.question.topic || nextRes.question.concept || "General"
        };

        setQuestions(prev => [...prev, normalizedQ]);
        if (nextRes.totalQuestions) {
          setTotalQuestions(nextRes.totalQuestions);
        }
        setCurrentQuestionIndex(prev => prev + 1);
        setQuestionStartTime(Date.now());
      } else {
        setError(nextRes?.message || "Failed to load the next question. Please try clicking Next Question again.");
      }
    } catch (err: any) {
      console.error("Error transitioning to next diagnostic question:", err);
      setError(err.message || "Failed to submit answer or fetch next question.");
    } finally {
      setLoadingNextQuestion(false);
    }
  };

  const handleSubmitAssessment = async () => {
    setError("");
    const activeQuestion = questions[currentQuestionIndex];
    if (!activeQuestion) return;

    const activeQuestionId = activeQuestion.questionId || activeQuestion.id;
    const selectedOpt = answers[activeQuestionId];

    if (selectedOpt === undefined || selectedOpt === null) {
      setError(`Please select an answer for Question ${currentQuestionIndex + 1} before submitting.`);
      return;
    }

    if (submittingAssessment || loadingNextQuestion) return;

    setSubmittingAssessment(true);
    const timeTakenSeconds = Math.max(1, Math.round((Date.now() - questionStartTime) / 1000));

    try {
      // Submit Q10 answer via initial submit endpoint first
      try {
        await submitInitialDiagnosticAnswer({
          sessionId: assessmentSessionId,
          questionId: activeQuestionId,
          selectedOption: selectedOpt,
          responseTimeSeconds: timeTakenSeconds
        });
      } catch (submitErr) {
        console.warn("[Form D] Initial submit for final question warning:", submitErr);
      }

      // Format all answered questions for final consolidated submission
      const formattedAnswers = questions.map(q => {
        const qId = q.questionId || q.id;
        return {
          questionId: qId,
          selectedOption: answers[qId] !== undefined ? answers[qId] : -1
        };
      });

      const res = await submitDiagnosticAssessment({
        sessionId: assessmentSessionId,
        userId,
        timeTakenSeconds: 300,
        answers: formattedAnswers
      });

      if (res) {
        setDiagnosticResult(res);
        setDiagnosticCompleted(true);
      } else {
        setError("Failed to submit assessment to backend.");
      }
    } catch (err: any) {
      setError(err.message || "Failed to submit assessment.");
    } finally {
      setSubmittingAssessment(false);
    }
  };

  const handleNext = async () => {
    setError("");
    if (step === 1) {
      if (
        previousSemesterSgpa === "" ||
        previousSemesterPercentage === "" ||
        mathematicsScore === "" ||
        programmingScore === "" ||
        dataStructuresScore === "" ||
        dbmsScore === "" ||
        attendancePercentage === "" ||
        numberOfBacklogs === ""
      ) {
        setError("Please fill out all academic performance fields.");
        return;
      }
      const sgpa = Number(previousSemesterSgpa);
      const percentage = Number(previousSemesterPercentage);
      const math = Number(mathematicsScore);
      const prog = Number(programmingScore);
      const ds = Number(dataStructuresScore);
      const dbms = Number(dbmsScore);
      const attendanceVal = Number(attendancePercentage);
      const backlogs = Number(numberOfBacklogs);

      if (isNaN(sgpa) || sgpa < 0 || sgpa > 10) {
        setError("Previous Semester SGPA must be a number between 0.0 and 10.0.");
        return;
      }
      if (isNaN(percentage) || percentage < 0 || percentage > 100) {
        setError("Previous Semester Percentage must be a number between 0 and 100.");
        return;
      }
      if (isNaN(math) || math < 0 || math > 100) {
        setError("Mathematics Score must be a number between 0 and 100.");
        return;
      }
      if (isNaN(prog) || prog < 0 || prog > 100) {
        setError("Programming Score must be a number between 0 and 100.");
        return;
      }
      if (isNaN(ds) || ds < 0 || ds > 100) {
        setError("Data Structures Score must be a number between 0 and 100.");
        return;
      }
      if (isNaN(dbms) || dbms < 0 || dbms > 100) {
        setError("DBMS Score must be a number between 0 and 100.");
        return;
      }
      if (isNaN(attendanceVal) || attendanceVal < 0 || attendanceVal > 100) {
        setError("Attendance Percentage must be a number between 0 and 100.");
        return;
      }
      if (isNaN(backlogs) || backlogs < 0 || !Number.isInteger(backlogs)) {
        setError("Number of Backlogs must be a non-negative integer.");
        return;
      }
    }
    if (step === 2) {
      const unanswered: string[] = [];
      for (let section = 1; section <= 5; section++) {
        for (let q = 1; q <= 4; q++) {
          const key = `B${section}.${q}`;
          if (!learningPreferences[key] || learningPreferences[key] < 1 || learningPreferences[key] > 5) {
            unanswered.push(key);
          }
        }
      }
      if (unanswered.length > 0) {
        setError(`Please answer all questions. Unanswered: ${unanswered.join(", ")}`);
        return;
      }
    }
    if (step === 3) {
      if (!consent) {
        setError("Please select whether you agree or do not agree to the consent form.");
        return;
      }
      if (consent === "I Agree") {
        const unanswered: string[] = [];
        if (c2Motivation === 0) unanswered.push("Motivation");
        if (c2Focus === 0) unanswered.push("Focus");
        if (c2Fatigue === 0) unanswered.push("Mental Fatigue");
        if (c2Stress === 0) unanswered.push("Academic Stress");
        if (c2Confidence === 0) unanswered.push("Learning Confidence");
        if (c2WorkloadComfort === 0) unanswered.push("Workload Comfort");
        if (c2Satisfaction === 0) unanswered.push("Learning Satisfaction");
        if (!c3SleepDuration) unanswered.push("Sleep Duration");
        if (!c3PreferredStudyTime) unanswered.push("Preferred Study Time");
        if (!c3StudyEnvironment) unanswered.push("Study Environment");
        if (!c3AcademicWorkload) unanswered.push("Academic Workload");
        if (!c3PendingAssignments) unanswered.push("Pending Assignments");
        if (!c3AcademicDifficulty) unanswered.push("Academic Difficulty");
        if (!c3NeedSupport) unanswered.push("Academic Support Needed");

        if (unanswered.length > 0) {
          setError(`Please answer all questions. Unanswered: ${unanswered.join(", ")}`);
          return;
        }
      }
    }
    await autoSaveStep(step + 1);
    setStep(prev => Math.min(prev + 1, 4));
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
      const onboardPayload = {
        userId,
        course: engineeringBranch,
        semester: Number(currentSemester),
        subjects: currentSubjects.split(",").map(s => s.trim()),
        careerGoals: [careerGoal],
        preferredStudyHoursPerDay: 2.0,
        targetCgpa: 8.5,
        sleepHours: 7.5,
        stressLevel: 4,
        exerciseMinutes: 30,
        learningStyle: "Visual"
      };

      await onboardStudent(onboardPayload);
      setSubmitting(false);
      navigate("/dashboard");
    } catch (err: any) {
      setSubmitting(false);
      setError(err.message || "Failed to complete onboarding. Please try again.");
    }
  };

  const completionPercentage = Math.round((step / 4) * 100);
  const timeRemaining = Math.max(1, 5 - step);

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
          <span>Step {step} of 4: {getStepTitle(step)}</span>
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
              {/* STEP 1: Form A — Student Profile & Academic Background */}
              {step === 1 && (
                <div className="space-y-6">
                  <div className="border-b border-white/10 pb-4">
                    <h2 className="text-xl font-bold text-main-theme flex items-center gap-2">
                      <User className="h-6 w-6 text-purple-theme" />
                      <span>Form A — Student Profile & Academic Background</span>
                    </h2>
                    <p className="text-xs text-secondary-theme mt-1">Please fill in exactly the required Student Profile & Academic Background details.</p>
                  </div>

                  {/* SECTION 1 — STUDENT & ACADEMIC PROFILE */}
                  <div className="space-y-4">
                    <h3 className="text-sm font-bold text-purple-400 border-b border-white/5 pb-1">SECTION 1 — STUDENT & ACADEMIC PROFILE</h3>
                    <div className="grid grid-cols-1 md:grid-cols-2 gap-4 text-xs">
                      <div>
                        <label className="font-bold text-secondary-theme block mb-1">Student ID (Read-Only)</label>
                        <input type="text" readOnly value={userId || "anonymous_student"} className="w-full p-3 rounded-xl glass-input opacity-70 cursor-not-allowed" />
                      </div>

                      <div>
                        <label className="font-bold text-secondary-theme block mb-1">Academic Year *</label>
                        <select value={academicYear} onChange={e => setAcademicYear(e.target.value)} className="w-full p-3 rounded-xl glass-input bg-black/20">
                          <option value="2025–26">2025–26</option>
                          <option value="2026–27">2026–27</option>
                          <option value="2027–28">2027–28</option>
                          <option value="Other">Other</option>
                        </select>
                      </div>

                      <div>
                        <label className="font-bold text-secondary-theme block mb-1">Program / Course *</label>
                        <select value={engineeringBranch} onChange={e => setEngineeringBranch(e.target.value)} className="w-full p-3 rounded-xl glass-input bg-black/20">
                          <option value="B.Tech Computer Engineering">B.Tech Computer Engineering</option>
                          <option value="B.Tech CSE">B.Tech CSE</option>
                          <option value="Other">Other</option>
                        </select>
                      </div>

                      <div>
                        <label className="font-bold text-secondary-theme block mb-1">Semester *</label>
                        <select value={semesterRoman} onChange={e => setSemesterRoman(e.target.value)} className="w-full p-3 rounded-xl glass-input bg-black/20">
                          <option value="I">I</option>
                          <option value="II">II</option>
                          <option value="III">III</option>
                          <option value="IV">IV</option>
                          <option value="V">V</option>
                          <option value="VI">VI</option>
                          <option value="VII">VII</option>
                          <option value="VIII">VIII</option>
                        </select>
                      </div>

                      <div>
                        <label className="font-bold text-secondary-theme block mb-1">Division *</label>
                        <select value={division} onChange={e => setDivision(e.target.value)} className="w-full p-3 rounded-xl glass-input bg-black/20">
                          <option value="A">A</option>
                          <option value="B">B</option>
                          <option value="C">C</option>
                          <option value="D">D</option>
                        </select>
                      </div>

                      <div>
                        <label className="font-bold text-secondary-theme block mb-1">Previous Programming Experience *</label>
                        <select value={previousProgrammingExperience} onChange={e => setPreviousProgrammingExperience(e.target.value)} className="w-full p-3 rounded-xl glass-input bg-black/20">
                          <option value="None">None</option>
                          <option value="Basic">Basic</option>
                          <option value="Intermediate">Intermediate</option>
                          <option value="Advanced">Advanced</option>
                        </select>
                      </div>
                    </div>
                  </div>

                  {/* SECTION 2 — PREVIOUS ACADEMIC PERFORMANCE */}
                  <div className="space-y-4 pt-2">
                    <h3 className="text-sm font-bold text-purple-400 border-b border-white/5 pb-1">SECTION 2 — PREVIOUS ACADEMIC PERFORMANCE</h3>
                    <div className="grid grid-cols-1 md:grid-cols-2 gap-4 text-xs">
                      <div>
                        <label className="font-bold text-secondary-theme block mb-1">Previous Semester SGPA * (0.0–10.0)</label>
                        <input type="text" placeholder="e.g. 8.25" value={previousSemesterSgpa} onChange={e => setPreviousSemesterSgpa(e.target.value)} className="w-full p-3 rounded-xl glass-input" />
                      </div>

                      <div>
                        <label className="font-bold text-secondary-theme block mb-1">Previous Semester Percentage * (0–100)</label>
                        <input type="text" placeholder="e.g. 78.5" value={previousSemesterPercentage} onChange={e => setPreviousSemesterPercentage(e.target.value)} className="w-full p-3 rounded-xl glass-input" />
                      </div>

                      <div>
                        <label className="font-bold text-secondary-theme block mb-1">Mathematics Grade / Score * (0–100)</label>
                        <input type="text" placeholder="e.g. 85" value={mathematicsScore} onChange={e => setMathematicsScore(e.target.value)} className="w-full p-3 rounded-xl glass-input" />
                      </div>

                      <div>
                        <label className="font-bold text-secondary-theme block mb-1">Programming Grade / Score * (0–100)</label>
                        <input type="text" placeholder="e.g. 90" value={programmingScore} onChange={e => setProgrammingScore(e.target.value)} className="w-full p-3 rounded-xl glass-input" />
                      </div>

                      <div>
                        <label className="font-bold text-secondary-theme block mb-1">Data Structures Grade / Score * (0–100)</label>
                        <input type="text" placeholder="e.g. 88" value={dataStructuresScore} onChange={e => setDataStructuresScore(e.target.value)} className="w-full p-3 rounded-xl glass-input" />
                      </div>

                      <div>
                        <label className="font-bold text-secondary-theme block mb-1">DBMS Grade / Score * (0–100)</label>
                        <input type="text" placeholder="e.g. 82" value={dbmsScore} onChange={e => setDbmsScore(e.target.value)} className="w-full p-3 rounded-xl glass-input" />
                      </div>

                      <div>
                        <label className="font-bold text-secondary-theme block mb-1">Attendance Percentage * (0–100)</label>
                        <input type="text" placeholder="e.g. 92.5" value={attendancePercentage} onChange={e => setAttendancePercentage(e.target.value)} className="w-full p-3 rounded-xl glass-input" />
                      </div>

                      <div>
                        <label className="font-bold text-secondary-theme block mb-1">Number of Backlogs * (Min: 0)</label>
                        <input type="text" placeholder="e.g. 0" value={numberOfBacklogs} onChange={e => setNumberOfBacklogs(e.target.value)} className="w-full p-3 rounded-xl glass-input" />
                      </div>
                    </div>
                  </div>

                  {/* SECTION 3 — ACADEMIC CONFIDENCE */}
                  <div className="space-y-4 pt-2">
                    <div className="flex justify-between items-center border-b border-white/5 pb-1">
                      <h3 className="text-sm font-bold text-purple-400">SECTION 3 — ACADEMIC CONFIDENCE</h3>
                      <span className="text-[10px] text-secondary-theme italic">Scale: 1=Very Low, 2=Low, 3=Moderate, 4=High, 5=Very High</span>
                    </div>
                    <div className="grid grid-cols-1 md:grid-cols-2 gap-4 text-xs">
                      <div>
                        <label className="font-bold text-secondary-theme block mb-1">How confident are you in Programming? *</label>
                        <select value={programmingConfidence} onChange={e => setProgrammingConfidence(Number(e.target.value))} className="w-full p-3 rounded-xl glass-input bg-black/20">
                          <option value={1}>1 = Very Low</option>
                          <option value={2}>2 = Low</option>
                          <option value={3}>3 = Moderate</option>
                          <option value={4}>4 = High</option>
                          <option value={5}>5 = Very High</option>
                        </select>
                      </div>

                      <div>
                        <label className="font-bold text-secondary-theme block mb-1">How confident are you in Data Structures? *</label>
                        <select value={dataStructuresConfidence} onChange={e => setDataStructuresConfidence(Number(e.target.value))} className="w-full p-3 rounded-xl glass-input bg-black/20">
                          <option value={1}>1 = Very Low</option>
                          <option value={2}>2 = Low</option>
                          <option value={3}>3 = Moderate</option>
                          <option value={4}>4 = High</option>
                          <option value={5}>5 = Very High</option>
                        </select>
                      </div>

                      <div>
                        <label className="font-bold text-secondary-theme block mb-1">How confident are you in DBMS? *</label>
                        <select value={dbmsConfidence} onChange={e => setDbmsConfidence(Number(e.target.value))} className="w-full p-3 rounded-xl glass-input bg-black/20">
                          <option value={1}>1 = Very Low</option>
                          <option value={2}>2 = Low</option>
                          <option value={3}>3 = Moderate</option>
                          <option value={4}>4 = High</option>
                          <option value={5}>5 = Very High</option>
                        </select>
                      </div>

                      <div>
                        <label className="font-bold text-secondary-theme block mb-1">How confident are you in Mathematics? *</label>
                        <select value={mathematicsConfidence} onChange={e => setMathematicsConfidence(Number(e.target.value))} className="w-full p-3 rounded-xl glass-input bg-black/20">
                          <option value={1}>1 = Very Low</option>
                          <option value={2}>2 = Low</option>
                          <option value={3}>3 = Moderate</option>
                          <option value={4}>4 = High</option>
                          <option value={5}>5 = Very High</option>
                        </select>
                      </div>

                      <div>
                        <label className="font-bold text-secondary-theme block mb-1">How confident are you in Algorithms? *</label>
                        <select value={algorithmsConfidence} onChange={e => setAlgorithmsConfidence(Number(e.target.value))} className="w-full p-3 rounded-xl glass-input bg-black/20">
                          <option value={1}>1 = Very Low</option>
                          <option value={2}>2 = Low</option>
                          <option value={3}>3 = Moderate</option>
                          <option value={4}>4 = High</option>
                          <option value={5}>5 = Very High</option>
                        </select>
                      </div>
                    </div>
                  </div>
                </div>
              )}
              {/* STEP 2: Form B — Learning Preference Questionnaire */}
              {step === 2 && (
                <div className="space-y-6">
                  <div className="border-b border-white/10 pb-4">
                    <h2 className="text-xl font-bold text-main-theme flex items-center gap-2">
                      <GraduationCap className="h-6 w-6 text-purple-theme" />
                      <span>Form B — Learning Preference Questionnaire</span>
                    </h2>
                    <p className="text-xs text-secondary-theme mt-1">Please indicate how much you agree with each statement. There are no right or wrong answers.</p>
                  </div>

                  <div className="space-y-8 max-h-[60vh] overflow-y-auto pr-2 custom-scrollbar">
                    {FORM_B_SECTIONS.map((sec) => (
                      <div key={sec.id} className="space-y-4 p-5 rounded-2xl bg-white/5 border border-white/10">
                        <h3 className="text-xs font-black text-purple-theme tracking-wider uppercase">{sec.title}</h3>
                        <div className="space-y-6">
                          {sec.questions.map((q) => (
                            <div key={q.id} className="space-y-2">
                              <div className="flex justify-between text-xs font-semibold text-main-theme">
                                <span>{q.id}. {q.text}</span>
                                {learningPreferences[q.id] === 0 && (
                                  <span className="text-[10px] text-pink-400 font-extrabold uppercase animate-pulse">* Required</span>
                                )}
                              </div>

                              {/* Likert Scale Radio Button Group */}
                              <div className="grid grid-cols-5 gap-2">
                                {[1, 2, 3, 4, 5].map((val) => {
                                  const labelMap: Record<number, string> = {
                                    1: "Strongly Disagree",
                                    2: "Disagree",
                                    3: "Neutral",
                                    4: "Agree",
                                    5: "Strongly Agree"
                                  };
                                  const isSelected = learningPreferences[q.id] === val;
                                  return (
                                    <button
                                      key={val}
                                      type="button"
                                      onClick={() => setLearningPreferences(prev => ({ ...prev, [q.id]: val }))}
                                      className={`p-3 rounded-xl border text-[10px] font-bold text-center transition-all cursor-pointer ${
                                        isSelected
                                          ? "bg-purple-600 border-purple-500 text-white shadow-lg shadow-purple-500/20"
                                          : "bg-white/5 border-white/10 hover:bg-white/10 text-secondary-theme"
                                      }`}
                                    >
                                      <div className="text-sm font-black mb-0.5">{val}</div>
                                      <div className="leading-tight opacity-90">{labelMap[val]}</div>
                                    </button>
                                  );
                                })}
                              </div>
                            </div>
                          ))}
                        </div>
                      </div>
                    ))}
                  </div>
                </div>
              )}

              {/* STEP 3: Form C — Well-being & Learning Context */}
              {step === 3 && (
                <div className="space-y-6">
                  <div className="border-b border-white/10 pb-4">
                    <h2 className="text-xl font-bold text-main-theme flex items-center gap-2">
                      <Heart className="h-6 w-6 text-purple-theme" />
                      <span>Form C — Well-being & Learning Context</span>
                    </h2>
                    <p className="text-xs text-secondary-theme mt-1">These questions help us understand your current learning state and study context. There are no right or wrong answers.</p>
                  </div>

                  {/* Consent Section */}
                  <div className="p-5 rounded-2xl bg-white/5 border border-white/10 space-y-4">
                    <h3 className="text-sm font-bold text-purple-theme">SECTION C1 — CONSENT</h3>
                    <p className="text-xs text-main-theme leading-relaxed">
                      I understand that my responses may be used to personalize my learning and for educational research. I understand that participation is voluntary and that my responses will be protected.
                    </p>
                    <div className="flex gap-4">
                      {["I Agree", "I Do Not Agree"].map((opt) => (
                        <button
                          key={opt}
                          type="button"
                          onClick={() => setConsent(opt)}
                          className={`px-6 py-3 rounded-xl border text-xs font-bold transition-all cursor-pointer ${
                            consent === opt
                              ? "bg-purple-600 border-purple-500 text-white shadow-lg shadow-purple-500/20"
                              : "bg-white/5 border-white/10 text-secondary-theme hover:bg-white/10"
                          }`}
                        >
                          {opt}
                        </button>
                      ))}
                    </div>
                  </div>

                  {/* Show questionnaire only if I Agree is selected */}
                  {consent === "I Agree" && (
                    <div className="space-y-8 max-h-[45vh] overflow-y-auto pr-2 custom-scrollbar">

                      {/* Section C2 — Current Learning State */}
                      <div className="space-y-6 p-5 rounded-2xl bg-white/5 border border-white/10">
                        <div className="flex justify-between items-center border-b border-white/5 pb-2">
                          <h3 className="text-xs font-black text-purple-theme tracking-wider uppercase">SECTION C2 — CURRENT LEARNING STATE</h3>
                          <span className="text-[10px] text-secondary-theme italic">Scale: 1=Very Low, 2=Low, 3=Moderate, 4=High, 5=Very High</span>
                        </div>

                        {[
                          { id: "C2.1", label: "How motivated are you to study today?", state: c2Motivation, setter: setC2Motivation },
                          { id: "C2.2", label: "How focused are you during learning?", state: c2Focus, setter: setC2Focus },
                          { id: "C2.3", label: "How mentally tired do you feel? (1=Very Low Fatigue, 5=Very High Fatigue)", state: c2Fatigue, setter: setC2Fatigue },
                          { id: "C2.4", label: "How stressed do you feel about your studies? (1=Very Low Stress, 5=Very High Stress)", state: c2Stress, setter: setC2Stress },
                          { id: "C2.5", label: "How confident are you about your current learning? (General learning confidence)", state: c2Confidence, setter: setC2Confidence },
                          { id: "C2.6", label: "How comfortable are you with your current academic workload? (1=Very Uncomfortable, 5=Very Comfortable)", state: c2WorkloadComfort, setter: setC2WorkloadComfort },
                          { id: "C2.7", label: "How satisfied are you with your learning progress? (1=Very Dissatisfied, 5=Very Satisfied)", state: c2Satisfaction, setter: setC2Satisfaction }
                        ].map((q) => (
                          <div key={q.id} className="space-y-2">
                            <div className="flex justify-between text-xs font-semibold text-main-theme">
                              <span>{q.label}</span>
                              {q.state === 0 && <span className="text-[10px] text-pink-400 font-extrabold uppercase animate-pulse">* Required</span>}
                            </div>
                            <div className="flex gap-2">
                              {[1, 2, 3, 4, 5].map((val) => (
                                <button
                                  key={val}
                                  type="button"
                                  onClick={() => q.setter(val)}
                                  className={`flex-1 py-2.5 rounded-lg border text-xs font-bold text-center transition-all cursor-pointer ${
                                    q.state === val
                                      ? "bg-purple-600 border-purple-500 text-white shadow-md shadow-purple-500/20"
                                      : "bg-white/5 border-white/10 hover:bg-white/10 text-secondary-theme"
                                  }`}
                                >
                                  {val}
                                </button>
                              ))}
                            </div>
                          </div>
                        ))}
                      </div>

                      {/* Section C3 — Learning Context */}
                      <div className="space-y-6 p-5 rounded-2xl bg-white/5 border border-white/10">
                        <h3 className="text-xs font-black text-purple-theme tracking-wider uppercase border-b border-white/5 pb-2">SECTION C3 — LEARNING CONTEXT</h3>
                        <div className="grid grid-cols-1 md:grid-cols-2 gap-4 text-xs">

                          <div>
                            <label className="font-bold text-secondary-theme block mb-1">C3.1 — Sleep Duration *</label>
                            <select value={c3SleepDuration} onChange={e => setC3SleepDuration(e.target.value)} className="w-full p-3 rounded-xl glass-input bg-black/20">
                              <option value="">Select sleep duration...</option>
                              <option value="Less than 5 hours">Less than 5 hours</option>
                              <option value="5–6 hours">5–6 hours</option>
                              <option value="6–7 hours">6–7 hours</option>
                              <option value="7–8 hours">7–8 hours</option>
                              <option value="More than 8 hours">More than 8 hours</option>
                            </select>
                          </div>

                          <div>
                            <label className="font-bold text-secondary-theme block mb-1">C3.2 — Preferred Study Time *</label>
                            <select value={c3PreferredStudyTime} onChange={e => setC3PreferredStudyTime(e.target.value)} className="w-full p-3 rounded-xl glass-input bg-black/20">
                              <option value="">Select study time...</option>
                              <option value="Morning">Morning</option>
                              <option value="Afternoon">Afternoon</option>
                              <option value="Evening">Evening</option>
                              <option value="Night">Night</option>
                            </select>
                          </div>

                          <div>
                            <label className="font-bold text-secondary-theme block mb-1">C3.3 — Study Environment *</label>
                            <select value={c3StudyEnvironment} onChange={e => setC3StudyEnvironment(e.target.value)} className="w-full p-3 rounded-xl glass-input bg-black/20">
                              <option value="">Select environment...</option>
                              <option value="Quiet">Quiet</option>
                              <option value="Moderate">Moderate</option>
                              <option value="Distracting">Distracting</option>
                            </select>
                          </div>

                          <div>
                            <label className="font-bold text-secondary-theme block mb-1">C3.4 — Current Academic Workload *</label>
                            <select value={c3AcademicWorkload} onChange={e => setC3AcademicWorkload(e.target.value)} className="w-full p-3 rounded-xl glass-input bg-black/20">
                              <option value="">Select workload...</option>
                              <option value="Low">Low</option>
                              <option value="Moderate">Moderate</option>
                              <option value="High">High</option>
                            </select>
                          </div>

                          <div>
                            <label className="font-bold text-secondary-theme block mb-1">C3.5 — Pending Assignments *</label>
                            <select value={c3PendingAssignments} onChange={e => setC3PendingAssignments(e.target.value)} className="w-full p-3 rounded-xl glass-input bg-black/20">
                              <option value="">Select pending assignments...</option>
                              <option value="0">0</option>
                              <option value="1">1</option>
                              <option value="2">2</option>
                              <option value="3+">3+</option>
                            </select>
                          </div>

                          <div>
                            <label className="font-bold text-secondary-theme block mb-1">C3.6 — Major Academic Difficulty *</label>
                            <select value={c3AcademicDifficulty} onChange={e => setC3AcademicDifficulty(e.target.value)} className="w-full p-3 rounded-xl glass-input bg-black/20">
                              <option value="">Select difficulty...</option>
                              <option value="Understanding concepts">Understanding concepts</option>
                              <option value="Solving problems">Solving problems</option>
                              <option value="Programming / Coding">Programming / Coding</option>
                              <option value="Managing study time">Managing study time</option>
                              <option value="Keeping up with coursework">Keeping up with coursework</option>
                              <option value="Preparing for assessments">Preparing for assessments</option>
                              <option value="Maintaining concentration">Maintaining concentration</option>
                              <option value="No major difficulty">No major difficulty</option>
                              <option value="Other">Other</option>
                            </select>
                          </div>

                          <div className="md:col-span-2">
                            <label className="font-bold text-secondary-theme block mb-1">C3.7 — Do you currently need academic support? *</label>
                            <div className="flex gap-4">
                              {["Yes", "No"].map((opt) => (
                                <button
                                  key={opt}
                                  type="button"
                                  onClick={() => setC3NeedSupport(opt)}
                                  className={`px-6 py-3 rounded-xl border text-xs font-bold transition-all cursor-pointer ${
                                    c3NeedSupport === opt
                                      ? "bg-purple-600 border-purple-500 text-white shadow-lg"
                                      : "bg-white/5 border-white/10 text-secondary-theme hover:bg-white/10"
                                  }`}
                                >
                                  {opt}
                                </button>
                              ))}
                            </div>
                          </div>

                        </div>
                      </div>

                    </div>
                  )}

                  {consent === "I Do Not Agree" && (
                    <div className="p-4 rounded-xl bg-pink-500/10 border border-pink-500/20 text-pink-400 text-xs">
                      You have selected &quot;I Do Not Agree&quot;. Well-being data will not be collected or saved. You can continue with the onboarding process.
                    </div>
                  )}
                </div>
              )}

              {/* STEP 4: Form D — Diagnostic Knowledge Assessment */}
              {step === 4 && (
                <div className="space-y-6">
                  <div className="border-b border-white/10 pb-4">
                    <h2 className="text-xl font-bold text-main-theme flex items-center gap-2">
                      <BrainCircuit className="h-6 w-6 text-purple-theme" />
                      <span>Form D — Diagnostic Knowledge Assessment</span>
                    </h2>
                    <p className="text-xs text-secondary-theme mt-1">
                      Data Structures &bull; Linked List &bull; Pre-test
                    </p>
                  </div>

                  {loadingQuestions ? (
                    <div className="flex flex-col items-center justify-center py-12 space-y-4">
                      <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-purple-500"></div>
                      <p className="text-xs text-secondary-theme font-medium">Loading assessment questions...</p>
                    </div>
                  ) : !diagnosticCompleted ? (
                    questions.length > 0 ? (
                      <div className="space-y-6">
                        {/* Progress Bar & Question Tracker */}
                        <div className="space-y-2">
                          <div className="flex justify-between text-xs font-bold text-secondary-theme">
                            <span>Question {currentQuestionIndex + 1} of {totalQuestions}</span>
                            <span>{Math.round(((currentQuestionIndex + 1) / totalQuestions) * 100)}% Complete</span>
                          </div>
                          <div className="w-full h-2 rounded-full bg-white/5 overflow-hidden">
                            <div
                              className="h-full bg-gradient-to-r from-purple-500 to-pink-500 transition-all duration-300"
                              style={{ width: `${((currentQuestionIndex + 1) / totalQuestions) * 100}%` }}
                            ></div>
                          </div>
                        </div>

                        {/* Question Content */}
                        <div className="p-6 rounded-2xl bg-white/5 border border-white/10 space-y-6">
                          <div className="space-y-1">
                            <span className="text-[9px] font-black text-purple-theme tracking-widest uppercase">
                              TOPIC: {questions[currentQuestionIndex].topic || questions[currentQuestionIndex].concept || "General"}
                            </span>
                            <h3 className="text-sm font-bold text-main-theme leading-relaxed">
                              {questions[currentQuestionIndex].questionText}
                            </h3>
                          </div>

                          {/* Options grid */}
                          <div className="grid grid-cols-1 gap-3">
                            {questions[currentQuestionIndex].options.map((opt: string, idx: number) => {
                              const letter = ["A", "B", "C", "D"][idx];
                              const activeQId = questions[currentQuestionIndex].questionId || questions[currentQuestionIndex].id;
                              const isSelected = answers[activeQId] === idx;
                              return (
                                <button
                                  key={idx}
                                  type="button"
                                  onClick={() => handleSelectOption(activeQId, idx)}
                                  className={`flex items-start gap-3 p-4 rounded-xl border text-left text-xs font-semibold transition-all cursor-pointer ${
                                    isSelected
                                      ? "bg-purple-600/20 border-purple-500 text-purple-300 shadow-md shadow-purple-500/10"
                                      : "bg-white/5 border-white/10 hover:bg-white/10 text-secondary-theme"
                                  }`}
                                >
                                  <span className={`flex items-center justify-center h-6 w-6 rounded-lg font-black text-[10px] ${
                                    isSelected ? "bg-purple-600 text-white" : "bg-white/10 text-main-theme"
                                  }`}>{letter}</span>
                                  <span className="mt-0.5 leading-relaxed">{opt}</span>
                                </button>
                              );
                            })}
                          </div>
                        </div>

                        {/* Question Navigation */}
                        <div className="flex justify-between items-center">
                          <button
                            type="button"
                            onClick={() => setCurrentQuestionIndex(prev => Math.max(prev - 1, 0))}
                            disabled={currentQuestionIndex === 0 || loadingNextQuestion || submittingAssessment}
                            className={`flex items-center gap-1.5 px-4 py-2 rounded-lg text-xs font-bold transition-all cursor-pointer ${
                              currentQuestionIndex === 0 || loadingNextQuestion || submittingAssessment ? "opacity-30 cursor-not-allowed text-secondary-theme" : "bg-white/5 hover:bg-white/10 text-main-theme"
                            }`}
                          >
                            <ArrowLeft className="h-3.5 w-3.5" />
                            <span>Previous Question</span>
                          </button>

                          {currentQuestionIndex < totalQuestions - 1 ? (
                            <button
                              type="button"
                              onClick={handleNextQuestion}
                              disabled={loadingNextQuestion || submittingAssessment}
                              className="flex items-center gap-1.5 px-5 py-2.5 rounded-xl bg-gradient-to-r from-purple-600 to-pink-600 hover:from-purple-500 hover:to-pink-500 text-white font-bold text-xs shadow-lg shadow-purple-500/20 transition-all cursor-pointer disabled:opacity-50 disabled:cursor-not-allowed"
                            >
                              {loadingNextQuestion ? (
                                <>
                                  <div className="animate-spin rounded-full h-3.5 w-3.5 border-b-2 border-white"></div>
                                  <span>Loading Question {currentQuestionIndex + 2}...</span>
                                </>
                              ) : (
                                <>
                                  <span>Next Question</span>
                                  <ArrowRight className="h-3.5 w-3.5" />
                                </>
                              )}
                            </button>
                          ) : (
                            <button
                              type="button"
                              onClick={handleSubmitAssessment}
                              disabled={submittingAssessment || loadingNextQuestion}
                              className="flex items-center gap-2 px-6 py-2.5 rounded-xl bg-gradient-to-r from-emerald-500 to-teal-500 hover:from-emerald-400 hover:to-teal-400 text-white font-black text-xs shadow-lg shadow-emerald-500/20 transition-all cursor-pointer disabled:opacity-50 disabled:cursor-not-allowed"
                            >
                              {submittingAssessment ? (
                                <span>Submitting...</span>
                              ) : (
                                <>
                                  <Sparkles className="h-4 w-4" />
                                  <span>Submit Assessment</span>
                                </>
                              )}
                            </button>
                          )}
                        </div>
                      </div>
                    ) : (
                      <p className="text-xs text-secondary-theme text-center py-6">No questions available.</p>
                    )
                  ) : (
                    // Summary Result view
                    <div className="space-y-6">
                      <div className="p-6 rounded-2xl bg-white/5 border border-emerald-500/20 space-y-6 text-center">
                        <div className="inline-flex items-center justify-center p-3 rounded-full bg-emerald-500/10 border border-emerald-500/20 text-emerald-400 mb-2">
                          <CheckCircle className="h-8 w-8" />
                        </div>
                        <div className="space-y-1">
                          <h3 className="text-lg font-black text-main-theme">Diagnostic Assessment Complete</h3>
                          <p className="text-xs text-secondary-theme">Your baseline knowledge state has been established.</p>
                        </div>

                        {/* Overall Score */}
                        <div className="grid grid-cols-2 gap-4 max-w-md mx-auto pt-2">
                          <div className="p-4 rounded-xl bg-white/5 border border-white/5 space-y-1">
                            <span className="text-[10px] font-black tracking-wider text-secondary-theme uppercase">Total Score</span>
                            <div className="text-2xl font-black text-purple-theme">
                              {diagnosticResult?.score} <span className="text-sm font-normal text-secondary-theme">/ {diagnosticResult?.totalMarks}</span>
                            </div>
                          </div>
                          <div className="p-4 rounded-xl bg-white/5 border border-white/5 space-y-1">
                            <span className="text-[10px] font-black tracking-wider text-secondary-theme uppercase">Percentage</span>
                            <div className="text-2xl font-black text-emerald-400">
                              {diagnosticResult?.percentage}%
                            </div>
                          </div>
                        </div>

                        {/* Answers breakdown */}
                        <div className="flex justify-center gap-6 text-xs text-secondary-theme pt-2">
                          <div>Correct: <span className="font-bold text-emerald-400">{diagnosticResult?.correctAnswers}</span></div>
                          <div>Incorrect: <span className="font-bold text-pink-400">{diagnosticResult?.incorrectAnswers}</span></div>
                          <div>Unanswered: <span className="font-bold text-main-theme">{diagnosticResult?.skippedQuestions}</span></div>
                        </div>

                        {/* Topic-wise breakdown */}
                        {diagnosticResult?.topicBreakdown && (
                          <div className="text-left space-y-4 pt-4 border-t border-white/10 max-w-lg mx-auto">
                            <h4 className="text-xs font-black text-purple-theme tracking-widest uppercase">Topic-wise Performance</h4>
                            <div className="space-y-3">
                              {Object.entries(diagnosticResult.topicBreakdown).map(([topic, stat]: [string, any]) => (
                                <div key={topic} className="space-y-1 text-xs">
                                  <div className="flex justify-between font-bold text-main-theme">
                                    <span>{topic}</span>
                                    <span>{stat.correct} / {stat.total} ({stat.percentage}%)</span>
                                  </div>
                                  <div className="w-full h-1.5 rounded-full bg-white/5 overflow-hidden">
                                    <div
                                      className="h-full bg-purple-500"
                                      style={{ width: `${stat.percentage}%` }}
                                    ></div>
                                  </div>
                                </div>
                              ))}
                            </div>
                          </div>
                        )}
                      </div>
                    </div>
                  )}
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

            {step < 4 ? (
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
              diagnosticCompleted ? (
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
                      <span>Complete Onboarding</span>
                    </>
                  )}
                </button>
              ) : null
            )}
          </div>

        </div>
      </div>

    </div>
  );
}

function getStepTitle(step: number): string {
  switch (step) {
    case 1: return "Form A — Student Profile & Academic Background";
    case 2: return "Form B — Learning Preference Questionnaire";
    case 3: return "Form C — Well-being & Learning Context";
    case 4: return "Form D — Diagnostic Knowledge Assessment";
    default: return "";
  }
}
