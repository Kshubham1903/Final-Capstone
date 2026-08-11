package com.edupilot.service;

import com.edupilot.model.StudentProfile;
import com.edupilot.model.LifestyleData;
import com.edupilot.model.LifestyleQuestionnaire;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.edupilot.model.QuizQuestion;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
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

        Set<String> stopWords = new HashSet<>(Arrays.asList("regarding", "fundamental", "principles", "what", "is", "the", "a", "an", "in", "of", "to", "and", "or", "for", "with", "by", "how", "which", "does", "do", "when", "why", "where", "are"));
        Set<String> words1 = new HashSet<>();
        for (String w : norm1.split(" ")) {
            if (!w.isBlank() && !stopWords.contains(w)) words1.add(w);
        }
        Set<String> words2 = new HashSet<>();
        for (String w : norm2.split(" ")) {
            if (!w.isBlank() && !stopWords.contains(w)) words2.add(w);
        }

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
            "what is the main goal of",
            "fundamental design principle when building scalable modules in",
            "optimizing latency and throughput in",
            "how should high-availability systems in",
            "protecting data integrity and access control within",
            "automated unit and integration test suites vital when iterating on",
            "maintains deterministic state transitions across complex workflows in"
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
        request.put("count", 20);

        try {
            List<Map<String, Object>> response = restTemplate.postForObject(endpoint, request, List.class);
            if (response != null && !response.isEmpty()) {
                List<QuizQuestion> result = new ArrayList<>();
                for (Map<String, Object> item : response) {
                    String qText = (String) item.get("questionText");
                    List<String> opts = (List<String>) item.get("options");
                    int correctIdx = item.get("correctOptionIndex") != null ? ((Number) item.get("correctOptionIndex")).intValue() : 0;
                    String explanation = (String) item.getOrDefault("conceptualExplanation", "Explanation for " + subject);
                    String concept = (String) item.getOrDefault("concept", subject + " Core");

                    QuizQuestion q = new QuizQuestion();
                    q.setSubject(subject);
                    q.setConcept(concept);
                    q.setDifficulty(difficulty != null ? difficulty : QuizQuestion.Difficulty.EASY);
                    q.setQuestionText(qText);
                    q.setOptions(opts);
                    q.setCorrectOptionIndex(correctIdx);
                    q.setConceptualExplanation(explanation);
                    q.setGenerationVersion(3);
                    q.setConceptId((String) item.get("conceptId"));
                    q.setTemplateFamilyId((String) item.get("templateFamilyId"));
                    q.setQuestionFingerprint((String) item.get("questionFingerprint"));
                    q.setQuestionSource(item.get("conceptId") != null && ((String) item.get("conceptId")).startsWith("dyn_") ? "DYNAMIC_V2" : "CURATED_BANK");
                    q.setQualityValidated(true);

                    if (validateQuestionQuality(q, excludeQuestions)) {
                        result.add(q);
                    }
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

    public static boolean validateQuestionQuality(QuizQuestion q, List<String> excludeQuestions) {
        if (q == null) return false;

        if (q.getQuestionText() == null || q.getQuestionText().trim().length() < 15) {
            return false;
        }

        if (q.getOptions() == null || q.getOptions().size() != 4) {
            return false;
        }

        Set<String> uniqueOptions = new HashSet<>();
        for (String opt : q.getOptions()) {
            if (opt == null || opt.trim().isEmpty()) {
                return false;
            }
            uniqueOptions.add(opt.trim().toLowerCase());
        }
        if (uniqueOptions.size() < 4) {
            return false;
        }

        if (q.getCorrectOptionIndex() < 0 || q.getCorrectOptionIndex() >= 4) {
            return false;
        }

        if (q.getConceptualExplanation() == null || q.getConceptualExplanation().trim().length() < 10) {
            return false;
        }

        if (isGenericTemplateQuestion(q.getQuestionText())) {
            return false;
        }

        for (String opt : q.getOptions()) {
            if (isGenericTemplateQuestion(opt)) {
                return false;
            }
        }

        if (excludeQuestions != null) {
            for (String exc : excludeQuestions) {
                if (isDuplicateQuestion(q.getQuestionText(), exc)) {
                    return false;
                }
            }
        }

        return true;
    }

    private List<QuizQuestion> createFallbackQuestionsForSubject(String subject, QuizQuestion.Difficulty difficulty, List<String> excludeQuestions) {
        List<QuizQuestion> list = new ArrayList<>();
        QuizQuestion.Difficulty diff = difficulty != null ? difficulty : QuizQuestion.Difficulty.EASY;
        String subjLower = subject.trim().toLowerCase();

        if (subjLower.contains("blockchain")) {
            list.add(new QuizQuestion(null, subject, "Consensus Mechanisms", diff,
                    "What core problem does a blockchain consensus mechanism solve in a decentralized network?",
                    List.of("Double-spending prevention and Byzantine fault tolerance agreement", "High physical network latency reduction", "Relational database storage capacity expansion", "Centralized server cluster hardware recovery"),
                    0, "Consensus mechanisms enable distributed nodes to reach agreement without a central authority."));
            list.add(new QuizQuestion(null, subject, "Smart Contracts", diff,
                    "What is the primary function of a smart contract on Ethereum?",
                    List.of("Hardware accelerator for cryptographic mining validation", "Self-executing on-chain code that runs when conditions are met", "Encrypted peer-to-peer instant messaging protocol client", "Scanned physical paper contract stored in network storage"),
                    1, "Smart contracts are immutable programs executing on EVM when triggered by transactions."));
            list.add(new QuizQuestion(null, subject, "Proof of Stake", diff,
                    "How does Proof of Stake (PoS) differ fundamentally from Proof of Work (PoW)?",
                    List.of("PoS requires higher hardware electricity consumption than PoW", "PoS eliminates all transaction gas fees across the network", "PoS selects validators based on staked capital instead of hash power", "PoW does not utilize any cryptographic hash calculations"),
                    2, "PoS replaces energy-intensive hash mining with capital commitment (staking)."));
            list.add(new QuizQuestion(null, subject, "Cryptographic Linking", diff,
                    "In blockchain data structures, how are consecutive blocks cryptographically linked?",
                    List.of("Blocks connect via static IP address routing tables", "Blocks use relational database primary key foreign key links", "Blocks merge into a single centralized flat log file", "Each block header contains the hash of the preceding block header"),
                    3, "Including the previous block hash creates an immutable append-only chain."));
        } else if (subjLower.contains("cloud security") || subjLower.contains("cloud")) {
            list.add(new QuizQuestion(null, subject, "Access Control", diff,
                    "Which IAM principle ensures users receive only the permissions required for their specific job tasks?",
                    List.of("Principle of Least Privilege access control policy", "Role-Based Overdrive execution privileges scheme", "Implicit Allow Access default authorization mode", "Shared Root Credentials administrative delegation"),
                    0, "The Least Privilege principle mandates granting minimum necessary access required for tasks."));
            list.add(new QuizQuestion(null, subject, "Shared Responsibility Model", diff,
                    "In the Cloud Shared Responsibility Model, which security layer is managed by the cloud provider?",
                    List.of("Customer application source code security vulnerability fixes", "Physical data center infrastructure and host facility hardware security", "User password complexity policies and rotation schedules", "S3 storage bucket public access permission configuration flags"),
                    1, "Cloud providers manage security 'of' the cloud (facilities, hardware, host OS)."));
            list.add(new QuizQuestion(null, subject, "Virtual Firewalls", diff,
                    "What is the function of an AWS Security Group in cloud networking?",
                    List.of("Customer database management server for user records", "Automated backup scheduler for Elastic Block Storage volumes", "Stateful virtual firewall controlling instance inbound and outbound traffic", "Domain Name System registrar and public routing manager"),
                    2, "Security Groups operate statefully at the instance level."));
            list.add(new QuizQuestion(null, subject, "Zero Trust Security", diff,
                    "What is the core pillar behind Zero Trust Architecture in cloud security?",
                    List.of("Trusting all internal network traffic behind perimeter firewalls", "Disabling TLS encryption protocols for internal service calls", "Granting root permissions to all internal subnet IP ranges", "Never trust, always verify every request regardless of origin"),
                    3, "Zero Trust treats internal and external networks as equally untrusted."));
        } else if (subjLower.contains("digital marketing") || subjLower.contains("marketing")) {
            list.add(new QuizQuestion(null, subject, "SEO Canonical Tags", diff,
                    "What is the primary function of a canonical URL tag in Search Engine Optimization?",
                    List.of("Specifying the master authoritative page URL to avoid duplicate penalties", "Accelerating mobile page rendering and browser caching performance", "Hiding internal admin pages from search engine indexing crawlers", "Generating pay-per-click ad copy variants automatically"),
                    0, "Canonical tags tell search crawlers which URL is the main authoritative copy."));
            list.add(new QuizQuestion(null, subject, "PPC CTR Calculation", diff,
                    "In digital pay-per-click (PPC) advertising, how is Click-Through Rate (CTR) calculated?",
                    List.of("Total Conversions divided by Total Clicks multiplied by 100", "Total Clicks divided by Total Impressions multiplied by 100", "Total Spend divided by Total Revenue generated from sales", "Total Impressions divided by Bounce Rate percentage calculation"),
                    1, "CTR measures ad engagement ratio: `(Clicks / Impressions) * 100`."));
            list.add(new QuizQuestion(null, subject, "CAC Metric", diff,
                    "What metric measures the average marketing expense incurred to acquire a single paying customer?",
                    List.of("Return on Ad Spend (ROAS) financial return ratio", "Lifetime Value (LTV) long-term customer revenue metric", "Customer Acquisition Cost (CAC) total marketing acquisition expense", "Cost Per Mille (CPM) baseline cost per thousand impressions"),
                    2, "CAC equals total marketing/sales spend divided by new customers acquired."));
            list.add(new QuizQuestion(null, subject, "A/B Testing", diff,
                    "When conducting an A/B split test on a landing page, what is a fundamental rule for valid statistical results?",
                    List.of("Changing headline, promotional offer, and page layout simultaneously", "Stopping the experiment immediately after receiving 10 page visits", "Running Variant A exclusively during business hours on weekdays", "Testing a single isolated variable change at a time for clear attribution"),
                    3, "Isolating a single variable ensures observed conversion rate differences are attributed accurately."));
        } else {
            list.add(new QuizQuestion(null, subject, subject + " Architecture", diff,
                    "How does software architecture maintain clean modular isolation in " + subject + "?",
                    List.of("Ensuring loose coupling and modular separation of concerns across components", "Tightly coupling business logic and data persistence into a single file", "Disabling error handling and input validation during execution cycles", "Hardcoding production system parameters into public binary files"),
                    0, "Modular separation of concerns ensures maintainability and clean component isolation in " + subject + "."));
            list.add(new QuizQuestion(null, subject, subject + " Performance Optimization", diff,
                    "What performance optimization technique yields high throughput in " + subject + "?",
                    List.of("Executing unbounded recursive loops without defined termination criteria", "Profiling execution bottlenecks and optimizing critical execution paths", "Disabling caching layers across all service API interface endpoints", "Increasing server hardware specs without profiling underlying bottlenecks"),
                    1, "Targeted profiling pinpoints exact bottlenecks, allowing data-driven optimization in " + subject + "."));
            list.add(new QuizQuestion(null, subject, subject + " Error Resilience", diff,
                    "Which pattern handles fault tolerance and system recovery in " + subject + "?",
                    List.of("Ignoring system errors and returning empty unvalidated response payloads", "Terminating host server processes upon encountering non-fatal warnings", "Implementing exponential backoff retry strategies with circuit breakers", "Bypassing input validation and boundary security checks under heavy load"),
                    2, "Exponential backoff and circuit breakers prevent cascading failures in " + subject + "."));
            list.add(new QuizQuestion(null, subject, subject + " Security & Data Integrity", diff,
                    "How are data integrity and authorization enforced across " + subject + " components?",
                    List.of("Storing API secret credentials in client-side open source assets", "Disabling TLS encryption protocols across internal microservices", "Granting full admin permissions to all default user access tokens", "Validating and sanitizing all inputs at system boundaries before processing"),
                    3, "Input validation and boundary sanitization prevent injection vulnerabilities in " + subject + "."));
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
                result.add(shuffleQuestionOptions(q));
            }
        }

        return result;
    }

    public static QuizQuestion shuffleQuestionOptions(QuizQuestion q) {
        if (q == null || q.getOptions() == null || q.getOptions().size() < 2) {
            return q;
        }
        List<String> originalOptions = new ArrayList<>(q.getOptions());
        int originalIdx = q.getCorrectOptionIndex();
        if (originalIdx < 0 || originalIdx >= originalOptions.size()) {
            originalIdx = 0;
        }
        String correctText = originalOptions.get(originalIdx);

        List<Map.Entry<String, Boolean>> paired = new ArrayList<>();
        for (int i = 0; i < originalOptions.size(); i++) {
            paired.add(new AbstractMap.SimpleEntry<>(originalOptions.get(i), i == originalIdx));
        }

        Collections.shuffle(paired);

        List<String> shuffledOptions = new ArrayList<>();
        int newCorrectIdx = 0;
        for (int i = 0; i < paired.size(); i++) {
            shuffledOptions.add(paired.get(i).getKey());
            if (Boolean.TRUE.equals(paired.get(i).getValue())) {
                newCorrectIdx = i;
            }
        }

        if (!shuffledOptions.get(newCorrectIdx).equals(correctText)) {
            System.err.println("CRITICAL ERROR: Option shuffle integrity check failed!");
            return q;
        }

        QuizQuestion copy = new QuizQuestion();
        copy.setId(q.getId());
        copy.setSubject(q.getSubject());
        copy.setConcept(q.getConcept());
        copy.setDifficulty(q.getDifficulty());
        copy.setQuestionText(q.getQuestionText());
        copy.setOptions(shuffledOptions);
        copy.setCorrectOptionIndex(newCorrectIdx);
        copy.setConceptualExplanation(q.getConceptualExplanation());
        copy.setGenerationVersion(q.getGenerationVersion());
        copy.setQuestionSource(q.getQuestionSource());
        copy.setConceptId(q.getConceptId());
        copy.setTemplateFamilyId(q.getTemplateFamilyId());
        copy.setQuestionFingerprint(q.getQuestionFingerprint());
        copy.setQualityValidated(q.isQualityValidated());
        copy.setCreatedAt(q.getCreatedAt());

        return copy;
    }
}

