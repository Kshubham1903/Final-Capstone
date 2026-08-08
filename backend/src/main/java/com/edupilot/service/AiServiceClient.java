package com.edupilot.service;

import com.edupilot.model.StudentProfile;
import com.edupilot.model.LifestyleData;
import com.edupilot.model.LifestyleQuestionnaire;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.edupilot.model.QuizQuestion;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class AiServiceClient {

    @Value("${ai.service.url:http://localhost:8000}")
    private String aiServiceUrl;

    private final RestTemplate restTemplate = new RestTemplate();

    public static String normalizeText(String text) {
        if (text == null) return "";
        return text.toLowerCase()
                .replaceAll("[^a-z0-9]", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    public static boolean isDuplicateQuestion(String q1, String q2) {
        String norm1 = normalizeText(q1);
        String norm2 = normalizeText(q2);
        if (norm1.equals(norm2)) return true;

        Set<String> words1 = new HashSet<>(Arrays.asList(norm1.split(" ")));
        Set<String> words2 = new HashSet<>(Arrays.asList(norm2.split(" ")));
        words1.remove("");
        words2.remove("");

        if (words1.isEmpty() || words2.isEmpty()) return false;

        Set<String> intersection = new HashSet<>(words1);
        intersection.retainAll(words2);

        Set<String> union = new HashSet<>(words1);
        union.addAll(words2);

        double similarity = (double) intersection.size() / union.size();
        return similarity > 0.80;
    }

    private static final List<String> GENERIC_PATTERNS = List.of(
            "primary objective of understanding core concepts",
            "recommended diagnostic step",
            "importance of understanding",
            "common challenge in",
            "baseline principles",
            "software implementations in",
            "verification standards in",
            "performance metrics during",
            "what is an important concept in",
            "what is the main goal of"
    );

    public static boolean isGenericTemplateQuestion(String text) {
        if (text == null) return true;
        String low = text.toLowerCase();
        for (String pat : GENERIC_PATTERNS) {
            if (low.contains(pat)) return true;
        }
        return false;
    }

    public Map<String, Object> predictPerformance(StudentProfile profile, LifestyleData lifestyle, int quizCount) {
        String endpoint = aiServiceUrl + "/api/ai/predict-performance";
        
        Map<String, Object> request = new HashMap<>();
        request.put("student_id", profile.getId());
        request.put("target_cgpa", profile.getTargetCgpa());
        request.put("current_cgpa", profile.getTargetCgpa() - 0.5);
        request.put("concept_mastery", profile.getConceptMastery());
        request.put("completed_quizzes_count", quizCount);
        
        Map<String, Object> lifestyleMap = new HashMap<>();
        lifestyleMap.put("sleep_hours", lifestyle != null ? lifestyle.getSleepHours() : 7.0);
        lifestyleMap.put("screen_time_hours", lifestyle != null ? lifestyle.getScreenTimeHours() : 4.0);
        lifestyleMap.put("stress_level", lifestyle != null ? lifestyle.getStressLevel() : 5);
        lifestyleMap.put("exercise_minutes", lifestyle != null ? lifestyle.getExerciseMinutes() : 20.0);
        lifestyleMap.put("study_minutes", lifestyle != null ? lifestyle.getStudyMinutes() : 180.0);
        lifestyleMap.put("attendance_rate", lifestyle != null ? lifestyle.getAttendanceRate() : 85.0);
        lifestyleMap.put("productivity_rating", lifestyle != null ? lifestyle.getProductivityRating() : 7.0);
        
        request.put("lifestyle", lifestyleMap);

        try {
            return restTemplate.postForObject(endpoint, request, Map.class);
        } catch (Exception ex) {
            Map<String, Object> fallback = new HashMap<>();
            fallback.put("predicted_cgpa", profile.getTargetCgpa() - 0.1);
            fallback.put("academic_risk_level", "LOW");
            fallback.put("student_growth_index", 7.2);
            fallback.put("improvement_rate", 0.05);
            fallback.put("insights", List.of(
                "[AI Fallback] Maintain structured study times to ensure conceptual clarity.",
                "[AI Fallback] Consider boosting physical exercise to mitigate stress peaks."
            ));
            return fallback;
        }
    }

    public Map<String, Object> predictStudentDevelopment(StudentProfile profile, LifestyleQuestionnaire questionnaire) {
        String endpoint = aiServiceUrl + "/api/ai/predict-student-development";
        
        Map<String, Object> request = new HashMap<>();
        request.put("student_id", profile.getId());
        request.put("hours_studied", questionnaire.getHoursStudied());
        request.put("attendance", questionnaire.getAttendance());
        request.put("parental_involvement", questionnaire.getParentalInvolvement());
        request.put("access_to_resources", questionnaire.getAccessToResources());
        request.put("extracurricular_activities", questionnaire.getExtracurricularActivities());
        request.put("sleep_hours", questionnaire.getSleepHours());
        request.put("previous_scores", questionnaire.getPreviousScores());
        request.put("motivation_level", questionnaire.getMotivationLevel());
        request.put("internet_access", questionnaire.getInternetAccess());
        request.put("tutoring_sessions", questionnaire.getTutoringSessions());
        request.put("family_income", questionnaire.getFamilyIncome());
        request.put("teacher_quality", questionnaire.getTeacherQuality());
        request.put("school_type", questionnaire.getSchoolType());
        request.put("peer_influence", questionnaire.getPeerInfluence());
        request.put("physical_activity", questionnaire.getPhysicalActivity());
        request.put("learning_disabilities", questionnaire.getLearningDisabilities());
        request.put("parental_education_level", questionnaire.getParentalEducationLevel());
        request.put("distance_from_home", questionnaire.getDistanceFromHome());
        request.put("gender", questionnaire.getGender());

        try {
            return restTemplate.postForObject(endpoint, request, Map.class);
        } catch (Exception ex) {
            Map<String, Object> fallback = new HashMap<>();
            fallback.put("predicted_performance_level", questionnaire.getPreviousScores() >= 75 ? "High" : questionnaire.getPreviousScores() >= 50 ? "Medium" : "Low");
            fallback.put("predicted_cgpa", Math.min(10.0, 5.0 + (questionnaire.getPreviousScores() / 100.0) * 4.5));
            fallback.put("academic_risk_level", questionnaire.getPreviousScores() >= 75 ? "LOW" : questionnaire.getPreviousScores() >= 50 ? "MEDIUM" : "HIGH");
            
            double attendanceFactor = questionnaire.getAttendance() / 100.0;
            double studyFactor = Math.min(questionnaire.getHoursStudied() / 40.0, 1.0);
            double sleepFactor = Math.max(0, 1.0 - Math.abs(questionnaire.getSleepHours() - 8.0) * 0.15);
            double composite = (questionnaire.getPreviousScores() * 0.40) + (attendanceFactor * 25) + (studyFactor * 20) + (sleepFactor * 15);
            fallback.put("student_growth_index", Math.round((composite / 10.0) * 100.0) / 100.0);
            
            fallback.put("consistency_score", questionnaire.getPreviousScores() >= 75 ? 90 : questionnaire.getPreviousScores() >= 50 ? 75 : 50);
            fallback.put("productivity_score", questionnaire.getHoursStudied() >= 20 ? 85 : questionnaire.getHoursStudied() >= 10 ? 70 : 45);
            fallback.put("lifestyle_score", (int) (questionnaire.getSleepHours() * 8 + questionnaire.getPhysicalActivity() * 4));
            
            fallback.put("insights", List.of(
                "[AI Fallback] Increase weekly study hours to reinforce intermediate concepts.",
                "[AI Fallback] Maintain structured sleep timing to reduce cognitive fatigue."
            ));
            return fallback;
        }
    }

    public Map<String, Object> adjustQuizDifficulty(String concept, String currentDifficulty, boolean isCorrect, double responseTimeSeconds) {
        String endpoint = aiServiceUrl + "/api/ai/adaptive-quiz";
        
        Map<String, Object> request = new HashMap<>();
        request.put("concept", concept);
        request.put("current_difficulty", currentDifficulty);
        request.put("is_correct", isCorrect);
        request.put("response_time_seconds", responseTimeSeconds);

        try {
            return restTemplate.postForObject(endpoint, request, Map.class);
        } catch (Exception ex) {
            Map<String, Object> fallback = new HashMap<>();
            fallback.put("next_difficulty", isCorrect ? "HARD" : "EASY");
            fallback.put("reason", "[AI Fallback] Simple logic fallback response.");
            return fallback;
        }
    }

    public List<QuizQuestion> generateQuestionsForSubject(String subject, QuizQuestion.Difficulty difficulty) {
        return generateQuestionsForSubject(subject, difficulty, List.of());
    }

    public List<QuizQuestion> generateQuestionsForSubject(String subject, QuizQuestion.Difficulty difficulty, List<String> excludeQuestions) {
        String diffStr = difficulty != null ? difficulty.name() : "EASY";
        String endpoint = aiServiceUrl + "/api/ai/generate-questions";

        Map<String, Object> request = new HashMap<>();
        request.put("subject", subject);
        request.put("difficulty", diffStr);
        request.put("exclude_questions", excludeQuestions != null ? excludeQuestions : List.of());
        request.put("count", 4);

        try {
            List<Map<String, Object>> response = restTemplate.postForObject(endpoint, request, List.class);
            if (response != null && !response.isEmpty()) {
                List<QuizQuestion> result = new ArrayList<>();
                for (Map<String, Object> item : response) {
                    String qText = (String) item.get("questionText");
                    if (isGenericTemplateQuestion(qText)) {
                        continue;
                    }
                    boolean dup = false;
                    if (excludeQuestions != null) {
                        for (String exc : excludeQuestions) {
                            if (isDuplicateQuestion(qText, exc)) {
                                dup = true;
                                break;
                            }
                        }
                    }
                    if (dup) continue;

                    QuizQuestion q = new QuizQuestion();
                    q.setSubject(subject);
                    q.setConcept((String) item.getOrDefault("concept", "Core " + subject));
                    q.setDifficulty(difficulty);
                    q.setQuestionText(qText);
                    q.setOptions((List<String>) item.get("options"));
                    q.setCorrectOptionIndex(((Number) item.get("correctOptionIndex")).intValue());
                    q.setConceptualExplanation((String) item.getOrDefault("conceptualExplanation", "Explanation for " + subject));
                    result.add(q);
                }
                if (!result.isEmpty()) {
                    return result;
                }
            }
        } catch (Exception ex) {
            System.err.println("AI microservice question generation endpoint error: " + ex.getMessage());
        }

        return createFallbackQuestionsForSubject(subject, difficulty, excludeQuestions);
    }

    private List<QuizQuestion> createFallbackQuestionsForSubject(String subject, QuizQuestion.Difficulty difficulty, List<String> excludeQuestions) {
        List<QuizQuestion> list = new ArrayList<>();
        QuizQuestion.Difficulty diff = difficulty != null ? difficulty : QuizQuestion.Difficulty.EASY;
        String subjLower = subject.trim().toLowerCase();

        if (subjLower.contains("blockchain")) {
            list.add(new QuizQuestion(null, subject, "Consensus Mechanisms", diff,
                    "What core problem does a blockchain consensus mechanism solve in a decentralized network?",
                    List.of("Double-spending and Byzantine Generals Problem", "High network latency", "Database storage limit", "Centralized server failure"),
                    0, "Consensus mechanisms enable distributed nodes to reach agreement without a central authority."));
            list.add(new QuizQuestion(null, subject, "Smart Contracts", diff,
                    "What is the primary function of a smart contract on Ethereum?",
                    List.of("Self-executing code deployed on-chain that runs automatically when conditions are met", "Hardware accelerator for mining", "Encrypted messaging client", "Paper legal contract scan"),
                    0, "Smart contracts are immutable programs executing on EVM when triggered by transactions."));
            list.add(new QuizQuestion(null, subject, "Proof of Stake", diff,
                    "How does Proof of Stake (PoS) differ fundamentally from Proof of Work (PoW)?",
                    List.of("PoS selects block validators based on staked capital rather than hashing power", "PoS uses 100x more electricity", "PoS eliminates transaction fees", "PoW has no hashing"),
                    0, "PoS replaces energy-intensive hash mining with capital commitment (staking)."));
            list.add(new QuizQuestion(null, subject, "Cryptographic Linking", diff,
                    "In blockchain data structures, how are consecutive blocks cryptographically linked?",
                    List.of("Each block header contains the hash of the preceding block header", "Blocks connect via IP addresses", "Blocks use relational database primary key IDs", "Blocks merge into one flat file"),
                    0, "Including the previous block hash creates an immutable append-only chain."));
        } else if (subjLower.contains("cloud security") || subjLower.contains("cloud")) {
            list.add(new QuizQuestion(null, subject, "Access Control", diff,
                    "Which IAM principle ensures users receive only the permissions required for their specific job tasks?",
                    List.of("Principle of Least Privilege", "Role-Based Overdrive", "Implicit Allow Access", "Root Credentials Sharing"),
                    0, "The Least Privilege principle mandates granting minimum necessary access required for tasks."));
            list.add(new QuizQuestion(null, subject, "Shared Responsibility Model", diff,
                    "In the Cloud Shared Responsibility Model, which security layer is managed by the cloud provider?",
                    List.of("Physical data center infrastructure and host facility security", "User password strength policies", "Application code bug fixes", "S3 bucket public access flags"),
                    0, "Cloud providers manage security 'of' the cloud (facilities, hardware, host OS)."));
            list.add(new QuizQuestion(null, subject, "Virtual Firewalls", diff,
                    "What is the function of an AWS Security Group in cloud networking?",
                    List.of("A stateful virtual firewall controlling inbound/outbound traffic at instance ENI level", "A user database", "An EBS backup scheduler", "A DNS registrar"),
                    0, "Security Groups operate statefully at the instance level."));
            list.add(new QuizQuestion(null, subject, "Zero Trust Security", diff,
                    "What is the core pillar behind Zero Trust Architecture in cloud security?",
                    List.of("Never trust, always verify every access request regardless of network origin", "Trust all internal network traffic", "Disable TLS for internal APIs", "Grant root rights to internal IP subnets"),
                    0, "Zero Trust treats internal and external networks as equally untrusted."));
        } else if (subjLower.contains("digital marketing") || subjLower.contains("marketing")) {
            list.add(new QuizQuestion(null, subject, "SEO Canonical Tags", diff,
                    "What is the primary function of a canonical URL tag in Search Engine Optimization?",
                    List.of("Prevents duplicate content penalties by specifying the preferred master page URL", "Speeds up mobile rendering", "Hides content from crawlers", "Generates PPC ad copy"),
                    0, "Canonical tags tell search crawlers which URL is the main authoritative copy."));
            list.add(new QuizQuestion(null, subject, "PPC CTR Calculation", diff,
                    "In digital pay-per-click (PPC) advertising, how is Click-Through Rate (CTR) calculated?",
                    List.of("Total Clicks divided by Total Impressions multiplied by 100", "Total Conversions divided by Clicks", "Total Spend divided by Sales", "Impressions divided by Bounce Rate"),
                    0, "CTR measures ad engagement ratio: `(Clicks / Impressions) * 100`."));
            list.add(new QuizQuestion(null, subject, "CAC Metric", diff,
                    "What metric measures the average marketing expense incurred to acquire a single paying customer?",
                    List.of("Customer Acquisition Cost (CAC)", "Return on Ad Spend (ROAS)", "Lifetime Value (LTV)", "Cost Per Mille (CPM)"),
                    0, "CAC equals total marketing/sales spend divided by new customers acquired."));
            list.add(new QuizQuestion(null, subject, "A/B Testing", diff,
                    "When conducting an A/B split test on a landing page, what is a fundamental rule for valid statistical results?",
                    List.of("Test only one single variable change (such as headline or CTA button color) at a time", "Change headline, offer, and layout simultaneously", "Stop test after 10 visits", "Run Variant A on weekdays only"),
                    0, "Isolating a single variable ensures observed conversion rate differences are attributed accurately."));
        } else {
            list.add(new QuizQuestion(null, subject, subject + " Architecture", diff,
                    "What is a fundamental design principle when building scalable software modules in " + subject + "?",
                    List.of("Ensuring loose coupling and modular separation of concerns across " + subject + " components", "Tightly coupling all business logic into a single script", "Disabling exception handling during load", "Hardcoding production parameters"),
                    0, "Modular separation of concerns ensures maintainability and clean component isolation in " + subject + "."));
            list.add(new QuizQuestion(null, subject, subject + " Performance Optimization", diff,
                    "When optimizing latency and throughput in " + subject + ", which approach yields the most reliable performance gains?",
                    List.of("Profiling execution bottlenecks and optimizing critical paths in " + subject, "Unbounded recursive calls without exit criteria", "Disabling caching layers", "Increasing hardware without profiling"),
                    0, "Targeted profiling pinpoints exact bottlenecks, allowing data-driven optimization in " + subject + "."));
            list.add(new QuizQuestion(null, subject, subject + " Error Resilience", diff,
                    "How should high-availability systems in " + subject + " handle transient fault conditions?",
                    List.of("Implementing exponential backoff retry strategies with circuit breakers in " + subject, "Ignoring errors and returning empty unvalidated payloads", "Terminating host process on non-fatal warnings", "Bypassing input validation"),
                    0, "Exponential backoff and circuit breakers prevent cascading failures in " + subject + "."));
            list.add(new QuizQuestion(null, subject, subject + " Security & Data Integrity", diff,
                    "What practice is critical for protecting data integrity and access control within " + subject + " workflows?",
                    List.of("Validating and sanitizing all inputs at system boundaries before processing in " + subject, "Storing API keys in public client assets", "Disabling TLS for microservices", "Granting default admin permissions"),
                    0, "Input validation and boundary sanitization prevent injection vulnerabilities in " + subject + "."));
        }

        List<QuizQuestion> result = new ArrayList<>();
        for (QuizQuestion q : list) {
            if (isGenericTemplateQuestion(q.getQuestionText())) continue;
            boolean dup = false;
            if (excludeQuestions != null) {
                for (String exc : excludeQuestions) {
                    if (isDuplicateQuestion(q.getQuestionText(), exc)) {
                        dup = true;
                        break;
                    }
                }
            }
            if (!dup) {
                result.add(q);
            }
        }

        return result;
    }
}

