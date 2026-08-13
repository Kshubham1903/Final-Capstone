package com.edupilot.service;

import com.edupilot.model.*;
import com.edupilot.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class StudentService {

    @Autowired
    private StudentProfileRepository profileRepository;

    @Autowired
    private LifestyleDataRepository lifestyleRepository;

    @Autowired
    private LifestyleQuestionnaireRepository questionnaireRepository;

    @Autowired
    private PersonalInformationRepository personalRepository;

    @Autowired
    private AcademicProfileRepository academicRepository;

    @Autowired
    private OnboardingStatusRepository statusRepository;

    @Autowired
    private AiServiceClient aiServiceClient;

    /**
     * Helper to resolve canonical student userId from user ID or email address.
     */
    public String resolveUserId(String emailOrId) {
        if (emailOrId == null || emailOrId.trim().isEmpty() || "anonymous_student".equalsIgnoreCase(emailOrId)) {
            return "anonymous_student";
        }
        Optional<StudentProfile> opt = profileRepository.findByUserId(emailOrId);
        if (opt.isPresent() && opt.get().getUserId() != null) {
            return opt.get().getUserId();
        }
        opt = profileRepository.findByEmail(emailOrId);
        if (opt.isPresent()) {
            return opt.get().getUserId() != null ? opt.get().getUserId() : opt.get().getId();
        }
        opt = profileRepository.findById(emailOrId);
        if (opt.isPresent()) {
            return opt.get().getUserId() != null ? opt.get().getUserId() : opt.get().getId();
        }
        return emailOrId;
    }

    /**
     * Defensive helper to find profile by MongoDB document ID or User ID, or auto-create if missing.
     */
    public StudentProfile findOrCreateProfile(String idOrUserId) {
        if (idOrUserId == null || idOrUserId.trim().isEmpty()) {
            idOrUserId = "anonymous_student";
        }
        Optional<StudentProfile> opt = profileRepository.findById(idOrUserId);
        if (opt.isPresent()) {
            return ensureSubjectMastery(opt.get());
        }
        opt = profileRepository.findByUserId(idOrUserId);
        if (opt.isPresent()) {
            return ensureSubjectMastery(opt.get());
        }

        // Initialize new StudentProfile for this userId
        StudentProfile newProfile = StudentProfile.builder()
                .userId(idOrUserId)
                .institution("EduPilot Academy")
                .degree("B.Tech")
                .branch("Computer Science & Engineering")
                .course("Computer Science & Engineering")
                .semester(1)
                .currentCgpa(8.0)
                .targetCgpa(8.5)
                .subjects(List.of("Data Structures & Algorithms", "Database Management Systems", "Artificial Intelligence"))
                .careerGoals(List.of("Software Engineer"))
                .preferredStudyHoursPerDay(4.0)
                .consistencyScore(70)
                .productivityScore(60)
                .lifestyleScore(65)
                .learningStyle("Visual")
                .currentStreakCount(1)
                .studentGrowthIndex(5.5)
                .conceptMastery(new HashMap<>())
                .weakConcepts(new HashMap<>())
                .strongConcepts(new HashMap<>())
                .build();

        return profileRepository.save(newProfile);
    }

    /**
     * Updates and validates the user's daily streak.
     * Increments if active exactly one day after the last activity.
     * Remains unchanged if already updated on the same day.
     * Resets to 1 if there is a gap of more than one day.
     */
    public synchronized StudentProfile updateStreak(String userId) {
        if (userId == null || userId.trim().isEmpty() || "anonymous_student".equals(userId)) {
            return null;
        }
        StudentProfile profile = findOrCreateProfile(userId);
        LocalDate today = LocalDate.now();

        LocalDate lastUpdate = profile.getLastStreakUpdate();
        if (lastUpdate == null) {
            profile.setCurrentStreakCount(1);
            profile.setLastStreakUpdate(today);
            profileRepository.save(profile);
        } else {
            long daysBetween = java.time.temporal.ChronoUnit.DAYS.between(lastUpdate, today);
            if (daysBetween == 1) {
                profile.setCurrentStreakCount(profile.getCurrentStreakCount() + 1);
                profile.setLastStreakUpdate(today);
                profileRepository.save(profile);
            } else if (daysBetween > 1) {
                profile.setCurrentStreakCount(1);
                profile.setLastStreakUpdate(today);
                profileRepository.save(profile);
            }
        }
        return profile;
    }

    /**
     * Backend Source of Truth: Check if student has completed onboarding.
     */
    public Map<String, Object> checkOnboardingStatus(String userId) {
        Map<String, Object> response = new HashMap<>();
        response.put("userId", userId);

        Optional<OnboardingStatus> statusOpt = statusRepository.findByUserId(userId);
        if (statusOpt.isEmpty()) {
            OnboardingStatus newStatus = new OnboardingStatus();
            newStatus.setUserId(userId);
            newStatus.setCurrentStep(1);
            newStatus.setCompletionPercentage(0);
            newStatus.setLastSavedAt(LocalDateTime.now());
            statusRepository.save(newStatus);

            response.put("isCompleted", false);
            response.put("onboardingStatus", newStatus);
            return response;
        }

        OnboardingStatus status = statusOpt.get();
        boolean personalDone = personalRepository.findByUserId(userId).isPresent();
        boolean academicDone = academicRepository.findByUserId(userId).isPresent();
        boolean questionnaireDone = questionnaireRepository.findByStudentProfileId(userId).isPresent() 
                || questionnaireRepository.findAll().stream().anyMatch(q -> userId.equals(q.getStudentProfileId()));

        status.setPersonalCompleted(personalDone);
        status.setAcademicCompleted(academicDone);
        status.setQuestionnaireCompleted(questionnaireDone);

        boolean fullyComplete = status.isCompleted() || (personalDone && academicDone && questionnaireDone);
        status.setCompleted(fullyComplete);

        int completedCount = 0;
        if (personalDone) completedCount += 33;
        if (academicDone) completedCount += 33;
        if (questionnaireDone) completedCount += 34;
        status.setCompletionPercentage(completedCount);

        statusRepository.save(status);

        response.put("isCompleted", fullyComplete);
        response.put("onboardingStatus", status);
        return response;
    }

    /**
     * Auto-save onboarding step progress to MongoDB with safe type parsing.
     */
    public OnboardingStatus saveOnboardingStep(String userId, int step, Map<String, Object> payload) {
        if (payload == null) payload = Collections.emptyMap();

        OnboardingStatus status = statusRepository.findByUserId(userId)
                .orElseGet(() -> {
                    OnboardingStatus s = new OnboardingStatus();
                    s.setUserId(userId);
                    return s;
                });

        status.setCurrentStep(step);
        status.setLastSavedAt(LocalDateTime.now());

        if (step == 1) { // Personal Information
            PersonalInformation personal = personalRepository.findByUserId(userId)
                    .orElseGet(() -> {
                        PersonalInformation p = new PersonalInformation();
                        p.setUserId(userId);
                        return p;
                    });

            if (payload.containsKey("fullName")) personal.setFullName(parseString(payload.get("fullName")));
            if (payload.containsKey("email")) personal.setEmail(parseString(payload.get("email")));
            if (payload.containsKey("phone")) personal.setPhone(parseString(payload.get("phone")));
            if (payload.containsKey("dateOfBirth")) personal.setDateOfBirth(parseString(payload.get("dateOfBirth")));
            if (payload.containsKey("gender")) personal.setGender(parseString(payload.get("gender")));
            if (payload.containsKey("address")) personal.setAddress(parseString(payload.get("address")));
            if (payload.containsKey("city")) personal.setCity(parseString(payload.get("city")));
            if (payload.containsKey("state")) personal.setState(parseString(payload.get("state")));
            if (payload.containsKey("country")) personal.setCountry(parseString(payload.get("country")));
            if (payload.containsKey("collegeName")) personal.setCollegeName(parseString(payload.get("collegeName")));
            if (payload.containsKey("university")) personal.setUniversity(parseString(payload.get("university")));
            if (payload.containsKey("engineeringBranch")) personal.setEngineeringBranch(parseString(payload.get("engineeringBranch")));
            if (payload.containsKey("currentSemester")) personal.setCurrentSemester(parseInt(payload.get("currentSemester"), 1));
            if (payload.containsKey("rollNumber")) personal.setRollNumber(parseString(payload.get("rollNumber")));
            if (payload.containsKey("admissionYear")) personal.setAdmissionYear(parseInt(payload.get("admissionYear"), 2022));
            if (payload.containsKey("expectedGraduationYear")) personal.setExpectedGraduationYear(parseInt(payload.get("expectedGraduationYear"), 2026));
            personal.setUpdatedAt(LocalDateTime.now());

            personalRepository.save(personal);
            status.setPersonalCompleted(true);

        } else if (step == 2) { // Academic Profile
            AcademicProfile academic = academicRepository.findByUserId(userId)
                    .orElseGet(() -> {
                        AcademicProfile a = new AcademicProfile();
                        a.setUserId(userId);
                        return a;
                    });

            if (payload.containsKey("institution")) academic.setInstitution(parseString(payload.get("institution")));
            if (payload.containsKey("degree")) academic.setDegree(parseString(payload.get("degree")));
            if (payload.containsKey("engineeringBranch")) academic.setEngineeringBranch(parseString(payload.get("engineeringBranch")));
            if (payload.containsKey("semester")) academic.setSemester(parseInt(payload.get("semester"), 1));
            if (payload.containsKey("currentCgpa")) academic.setCurrentCgpa(parseDouble(payload.get("currentCgpa"), 8.0));
            if (payload.containsKey("targetCgpa")) academic.setTargetCgpa(parseDouble(payload.get("targetCgpa"), 9.0));
            if (payload.containsKey("currentSubjects")) academic.setCurrentSubjects(parseList(payload.get("currentSubjects")));
            if (payload.containsKey("weakSubjects")) academic.setWeakSubjects(parseList(payload.get("weakSubjects")));
            if (payload.containsKey("strongSubjects")) academic.setStrongSubjects(parseList(payload.get("strongSubjects")));
            if (payload.containsKey("careerGoal")) academic.setCareerGoal(parseString(payload.get("careerGoal")));
            if (payload.containsKey("dreamCompany")) academic.setDreamCompany(parseString(payload.get("dreamCompany")));
            if (payload.containsKey("programmingLanguages")) academic.setProgrammingLanguages(parseList(payload.get("programmingLanguages")));
            if (payload.containsKey("frameworks")) academic.setFrameworks(parseList(payload.get("frameworks")));
            if (payload.containsKey("githubUrl")) academic.setGithubUrl(parseString(payload.get("githubUrl")));
            if (payload.containsKey("linkedInUrl")) academic.setLinkedInUrl(parseString(payload.get("linkedInUrl")));
            if (payload.containsKey("leetcodeUrl")) academic.setLeetcodeUrl(parseString(payload.get("leetcodeUrl")));
            if (payload.containsKey("weeklyCodingHours")) academic.setWeeklyCodingHours(parseDouble(payload.get("weeklyCodingHours"), 10.0));
            if (payload.containsKey("preferredLearningStyle")) academic.setPreferredLearningStyle(parseString(payload.get("preferredLearningStyle")));
            academic.setUpdatedAt(LocalDateTime.now());

            academicRepository.save(academic);
            status.setAcademicCompleted(true);

        } else if (step >= 3 && step <= 7) { // Lifestyle Assessment Sections
            LifestyleQuestionnaire quest = questionnaireRepository.findByStudentProfileId(userId)
                    .orElseGet(() -> {
                        LifestyleQuestionnaire q = new LifestyleQuestionnaire();
                        q.setStudentProfileId(userId);
                        return q;
                    });

            if (payload.containsKey("hoursStudied")) quest.setHoursStudied(parseDouble(payload.get("hoursStudied"), 20.0));
            if (payload.containsKey("attendance")) quest.setAttendance(parseDouble(payload.get("attendance"), 90.0));
            if (payload.containsKey("previousScores")) quest.setPreviousScores(parseDouble(payload.get("previousScores"), 85.0));
            if (payload.containsKey("tutoringSessions")) quest.setTutoringSessions(parseDouble(payload.get("tutoringSessions"), 1.0));
            if (payload.containsKey("accessToResources")) quest.setAccessToResources(parseString(payload.get("accessToResources")));
            if (payload.containsKey("internetAccess")) quest.setInternetAccess(parseString(payload.get("internetAccess")));
            if (payload.containsKey("schoolType")) quest.setSchoolType(parseString(payload.get("schoolType")));
            if (payload.containsKey("teacherQuality")) quest.setTeacherQuality(parseString(payload.get("teacherQuality")));
            if (payload.containsKey("sleepHours")) quest.setSleepHours(parseDouble(payload.get("sleepHours"), 7.5));
            if (payload.containsKey("physicalActivity")) quest.setPhysicalActivity(parseDouble(payload.get("physicalActivity"), 3.5));
            if (payload.containsKey("extracurricularActivities")) quest.setExtracurricularActivities(parseString(payload.get("extracurricularActivities")));
            if (payload.containsKey("motivationLevel")) quest.setMotivationLevel(parseString(payload.get("motivationLevel")));
            if (payload.containsKey("parentalInvolvement")) quest.setParentalInvolvement(parseString(payload.get("parentalInvolvement")));
            if (payload.containsKey("familyIncome")) quest.setFamilyIncome(parseString(payload.get("familyIncome")));
            if (payload.containsKey("peerInfluence")) quest.setPeerInfluence(parseString(payload.get("peerInfluence")));
            if (payload.containsKey("parentalEducationLevel")) quest.setParentalEducationLevel(parseString(payload.get("parentalEducationLevel")));
            if (payload.containsKey("distanceFromHome")) quest.setDistanceFromHome(parseString(payload.get("distanceFromHome")));
            if (payload.containsKey("learningDisabilities")) quest.setLearningDisabilities(parseString(payload.get("learningDisabilities")));
            if (payload.containsKey("gender")) quest.setGender(parseString(payload.get("gender")));

            questionnaireRepository.save(quest);

            if (step == 7) {
                status.setQuestionnaireCompleted(true);
            }
        }

        int percentage = 0;
        if (status.isPersonalCompleted()) percentage += 33;
        if (status.isAcademicCompleted()) percentage += 33;
        if (status.isQuestionnaireCompleted()) percentage += 34;
        status.setCompletionPercentage(percentage);

        return statusRepository.save(status);
    }

    /**
     * Onboard / Complete Onboarding for a student.
     */
    public StudentProfile onboardStudent(String userId, String course, int semester, 
                                         List<String> subjects, List<String> goals, 
                                         double studyHours, double targetCgpa,
                                         double sleepHours, double stressLevel,
                                         int exerciseMinutes, String learningStyle) {
        
        StudentProfile profile = findOrCreateProfile(userId);

        profile.setCourse(course != null ? course : "Computer Science & Engineering");
        profile.setBranch(course != null ? course : "Computer Science & Engineering");
        profile.setSemester(semester > 0 ? semester : 1);
        profile.setSubjects(subjects != null ? subjects : List.of("Data Structures & Algorithms", "Database Management Systems", "Artificial Intelligence"));
        profile.setCareerGoals(goals != null ? goals : List.of("Software Engineer"));
        profile.setPreferredStudyHoursPerDay(studyHours > 0 ? studyHours : 4.0);
        profile.setTargetCgpa(targetCgpa > 0 ? targetCgpa : 8.5);
        profile.setLearningStyle(learningStyle != null ? learningStyle : "Visual");
        profile.setCompleted(true);

        // Mastery & weak/strong concepts baseline
        Map<String, Double> defaultMastery = profile.getConceptMastery() != null ? profile.getConceptMastery() : new HashMap<>();
        Map<String, List<String>> weakConcepts = profile.getWeakConcepts() != null ? profile.getWeakConcepts() : new HashMap<>();
        Map<String, List<String>> strongConcepts = profile.getStrongConcepts() != null ? profile.getStrongConcepts() : new HashMap<>();

        if (subjects != null) {
            for (String subject : subjects) {
                defaultMastery.putIfAbsent(subject, 50.0);
                weakConcepts.putIfAbsent(subject, new ArrayList<>());
                strongConcepts.putIfAbsent(subject, new ArrayList<>());
            }
        }
        profile.setConceptMastery(defaultMastery);
        profile.setWeakConcepts(weakConcepts);
        profile.setStrongConcepts(strongConcepts);

        StudentProfile saved = profileRepository.save(profile);

        // Update OnboardingStatus in MongoDB
        OnboardingStatus status = statusRepository.findByUserId(userId).orElseGet(() -> {
            OnboardingStatus s = new OnboardingStatus();
            s.setUserId(userId);
            return s;
        });
        status.setPersonalCompleted(true);
        status.setAcademicCompleted(true);
        status.setQuestionnaireCompleted(true);
        status.setCompleted(true);
        status.setCompletionPercentage(100);
        status.setCompletedAt(LocalDateTime.now());
        statusRepository.save(status);

        // Run background prediction
        runSilentBackgroundPrediction(saved);
        return profileRepository.save(saved);
    }

    public StudentProfile updateLifestyleAndTriggerAnalytics(String idOrUserId, LifestyleData inputData) {
        StudentProfile profile = findOrCreateProfile(idOrUserId);
        inputData.setStudentProfileId(profile.getId());
        inputData.setDate(LocalDate.now());

        lifestyleRepository.save(inputData);

        profile.setCompletedQuizzesCount(profile.getCompletedQuizzesCount() + 1);

        double lifestyleScore = Math.min((inputData.getSleepHours() * 8.0) + (inputData.getExerciseMinutes() * 0.5), 100.0);
        double productivityScore = inputData.getProductivityRating() * 10.0;

        profile.setLifestyleScore((int) Math.round(lifestyleScore));
        profile.setProductivityScore((int) Math.round(productivityScore));

        runSilentBackgroundPrediction(profile);
        return profileRepository.save(profile);
    }

    public StudentProfile submitQuestionnaireAndRunAnalytics(String idOrUserId, LifestyleQuestionnaire questionnaireInput) {
        StudentProfile profile = findOrCreateProfile(idOrUserId);
        
        questionnaireInput.setStudentProfileId(profile.getId());
        
        Optional<LifestyleQuestionnaire> existingOpt = questionnaireRepository.findByStudentProfileId(profile.getId());
        if (existingOpt.isEmpty()) {
            existingOpt = questionnaireRepository.findByStudentProfileId(idOrUserId);
        }
        if (existingOpt.isPresent()) {
            questionnaireInput.setId(existingOpt.get().getId());
        }
        questionnaireRepository.save(questionnaireInput);
        
        runSilentBackgroundPrediction(profile);
        return profileRepository.save(profile);
    }

    /**
     * Real-time profile updates & AI recalculation trigger.
     */
    public StudentProfile updateProfileAndRecalculate(String userId, Map<String, Object> payload) {
        StudentProfile profile = findOrCreateProfile(userId);

        if (payload.containsKey("fullName")) profile.setFullName(parseString(payload.get("fullName")));
        if (payload.containsKey("institution")) profile.setInstitution(parseString(payload.get("institution")));
        if (payload.containsKey("degree")) profile.setDegree(parseString(payload.get("degree")));
        if (payload.containsKey("branch")) {
            String branchVal = parseString(payload.get("branch"));
            profile.setBranch(branchVal);
            profile.setCourse(branchVal);
        }
        if (payload.containsKey("course")) profile.setCourse(parseString(payload.get("course")));
        if (payload.containsKey("semester")) profile.setSemester(parseInt(payload.get("semester"), 1));
        if (payload.containsKey("currentCgpa")) profile.setCurrentCgpa(parseDouble(payload.get("currentCgpa"), 8.0));
        if (payload.containsKey("targetCgpa")) profile.setTargetCgpa(parseDouble(payload.get("targetCgpa"), 8.5));
        if (payload.containsKey("preferredStudyHoursPerDay")) profile.setPreferredStudyHoursPerDay(parseDouble(payload.get("preferredStudyHoursPerDay"), 4.0));
        if (payload.containsKey("learningStyle")) profile.setLearningStyle(parseString(payload.get("learningStyle")));
        if (payload.containsKey("subjects")) profile.setSubjects(parseList(payload.get("subjects")));
        if (payload.containsKey("careerGoals")) profile.setCareerGoals(parseList(payload.get("careerGoals")));

        // Sync with AcademicProfile collection
        Optional<AcademicProfile> academicOpt = academicRepository.findByUserId(userId);
        AcademicProfile academic = academicOpt.orElseGet(() -> {
            AcademicProfile a = new AcademicProfile();
            a.setUserId(userId);
            return a;
        });
        academic.setInstitution(profile.getInstitution());
        academic.setDegree(profile.getDegree());
        academic.setEngineeringBranch(profile.getBranch());
        academic.setSemester(profile.getSemester());
        academic.setCurrentCgpa(profile.getCurrentCgpa());
        academic.setTargetCgpa(profile.getTargetCgpa());
        if (profile.getSubjects() != null) academic.setCurrentSubjects(profile.getSubjects());
        academic.setUpdatedAt(LocalDateTime.now());
        academicRepository.save(academic);

        StudentProfile saved = profileRepository.save(profile);
        runSilentBackgroundPrediction(saved);
        return profileRepository.save(saved);
    }

    public void runSilentBackgroundPrediction(StudentProfile profile) {
        double avgSleep = 7.5;
        double avgStudyHoursWeekly = profile.getPreferredStudyHoursPerDay() * 7;
        double avgAttendance = 92.0;
        double avgExerciseHoursWeekly = 3.5;
        
        List<LifestyleData> history = lifestyleRepository.findByStudentProfileId(profile.getId());
        if (history != null && !history.isEmpty()) {
            double totalSleep = 0;
            double totalStudyMinutes = 0;
            double totalAttendance = 0;
            double totalExerciseMinutes = 0;
            for (LifestyleData d : history) {
                totalSleep += d.getSleepHours();
                totalStudyMinutes += d.getStudyMinutes();
                totalAttendance += d.getAttendanceRate();
                totalExerciseMinutes += d.getExerciseMinutes();
            }
            avgSleep = totalSleep / history.size();
            avgStudyHoursWeekly = (totalStudyMinutes / history.size()) * 7 / 60.0;
            avgAttendance = totalAttendance / history.size();
            avgExerciseHoursWeekly = (totalExerciseMinutes / history.size()) * 7 / 60.0;
        }

        double prevScores = 75.0;
        Map<String, Double> mastery = profile.getConceptMastery();
        if (mastery != null && !mastery.isEmpty()) {
            double sum = 0;
            for (double val : mastery.values()) {
                sum += val;
            }
            prevScores = sum / mastery.size();
        }

        LifestyleQuestionnaire questionnaire = LifestyleQuestionnaire.builder()
                .studentProfileId(profile.getId())
                .hoursStudied(avgStudyHoursWeekly)
                .attendance(avgAttendance)
                .parentalInvolvement(profile.getParentalInvolvement() != null ? profile.getParentalInvolvement() : "Medium")
                .accessToResources(profile.getAccessToResources() != null ? profile.getAccessToResources() : "High")
                .extracurricularActivities(profile.getExtracurricularActivities() != null ? profile.getExtracurricularActivities() : "Yes")
                .sleepHours(avgSleep)
                .previousScores(prevScores)
                .motivationLevel(profile.getMotivationLevel() != null ? profile.getMotivationLevel() : "High")
                .internetAccess(profile.getInternetAccess() != null ? profile.getInternetAccess() : "Yes")
                .tutoringSessions(profile.getTutoringSessions() > 0 ? profile.getTutoringSessions() : 1)
                .familyIncome(profile.getFamilyIncome() != null ? profile.getFamilyIncome() : "Medium")
                .teacherQuality(profile.getTeacherQuality() != null ? profile.getTeacherQuality() : "Medium")
                .schoolType(profile.getSchoolType() != null ? profile.getSchoolType() : "Public")
                .peerInfluence(profile.getPeerInfluence() != null ? profile.getPeerInfluence() : "Positive")
                .physicalActivity(avgExerciseHoursWeekly)
                .learningDisabilities(profile.getLearningDisabilities() != null ? profile.getLearningDisabilities() : "No")
                .parentalEducationLevel(profile.getParentalEducationLevel() != null ? profile.getParentalEducationLevel() : "College")
                .distanceFromHome(profile.getDistanceFromHome() != null ? profile.getDistanceFromHome() : "Near")
                .gender(profile.getGender() != null ? profile.getGender() : "Male")
                .build();

        Optional<LifestyleQuestionnaire> existingOpt = questionnaireRepository.findByStudentProfileId(profile.getId());
        if (existingOpt.isPresent()) {
            questionnaire.setId(existingOpt.get().getId());
        }
        questionnaireRepository.save(questionnaire);

        Map<String, Object> aiResult = aiServiceClient.predictStudentDevelopment(profile, questionnaire);
        if (aiResult != null) {
            if (aiResult.containsKey("student_growth_index")) {
                Number sgi = (Number) aiResult.get("student_growth_index");
                profile.setStudentGrowthIndex(Math.round(sgi.doubleValue() * 10.0) / 10.0);
            }
            if (aiResult.containsKey("predicted_cgpa")) {
                Number cgpa = (Number) aiResult.get("predicted_cgpa");
                profile.setPredictedCgpa(Math.round(cgpa.doubleValue() * 100.0) / 100.0);
            }
            if (aiResult.containsKey("academic_risk_level")) {
                profile.setAcademicRiskLevel((String) aiResult.get("academic_risk_level"));
            }
            if (aiResult.containsKey("consistency_score")) {
                Number consistency = (Number) aiResult.get("consistency_score");
                profile.setConsistencyScore(consistency.intValue());
            }
            if (aiResult.containsKey("productivity_score")) {
                Number prod = (Number) aiResult.get("productivity_score");
                profile.setProductivityScore(prod.intValue());
            }
            if (aiResult.containsKey("lifestyle_score")) {
                Number life = (Number) aiResult.get("lifestyle_score");
                profile.setLifestyleScore(life.intValue());
            }
        }
    }

    public Optional<LifestyleQuestionnaire> getQuestionnaireByProfileId(String idOrUserId) {
        Optional<LifestyleQuestionnaire> opt = questionnaireRepository.findByStudentProfileId(idOrUserId);
        if (opt.isPresent()) return opt;
        StudentProfile p = findOrCreateProfile(idOrUserId);
        return questionnaireRepository.findByStudentProfileId(p.getId());
    }

    public StudentProfile getProfileByUserId(String userId) {
        StudentProfile profile = findOrCreateProfile(userId);
        return populateLifestyleHistory(profile);
    }

    public Optional<StudentProfile> getProfileById(String id) {
        Optional<StudentProfile> opt = profileRepository.findById(id);
        return opt.map(this::populateLifestyleHistory);
    }

    private StudentProfile populateLifestyleHistory(StudentProfile profile) {
        List<LifestyleData> history = lifestyleRepository.findByStudentProfileId(profile.getId());
        if (history != null && !history.isEmpty()) {
            List<Map<String, Object>> formattedLogs = new ArrayList<>();
            for (LifestyleData d : history) {
                Map<String, Object> map = new HashMap<>();
                map.put("date", d.getDate() != null ? d.getDate().toString() : "Today");
                map.put("sleepHours", d.getSleepHours());
                map.put("screenTimeHours", d.getScreenTimeHours());
                map.put("stressLevel", d.getStressLevel());
                map.put("exerciseMinutes", d.getExerciseMinutes());
                map.put("studyMinutes", d.getStudyMinutes());
                map.put("productivityRating", d.getProductivityRating());
                map.put("attendanceRate", d.getAttendanceRate());
                formattedLogs.add(map);
            }
            profile.setLifestyleHistory(formattedLogs);
        } else {
            profile.setLifestyleHistory(Collections.emptyList());
        }
        return profile;
    }

    public Map<String, Object> getFullProfileByUserId(String userId) {
        Map<String, Object> fullProfile = new HashMap<>();
        StudentProfile profile = findOrCreateProfile(userId);
        Optional<PersonalInformation> personalOpt = personalRepository.findByUserId(userId);
        Optional<AcademicProfile> academicOpt = academicRepository.findByUserId(userId);
        Optional<LifestyleQuestionnaire> questionnaireOpt = getQuestionnaireByProfileId(userId);
        Optional<OnboardingStatus> statusOpt = statusRepository.findByUserId(userId);

        fullProfile.put("profile", profile);
        fullProfile.put("personalInfo", personalOpt.orElse(null));
        fullProfile.put("academicProfile", academicOpt.orElse(null));
        fullProfile.put("lifestyleQuestionnaire", questionnaireOpt.orElse(null));
        fullProfile.put("onboardingStatus", statusOpt.orElse(null));
        return fullProfile;
    }

    // Defensive parsing helper methods
    private double parseDouble(Object val, double defaultVal) {
        if (val instanceof Number) return ((Number) val).doubleValue();
        if (val instanceof String) {
            try { return Double.parseDouble((String) val); } catch (Exception ignored) {}
        }
        return defaultVal;
    }

    private int parseInt(Object val, int defaultVal) {
        if (val instanceof Number) return ((Number) val).intValue();
        if (val instanceof String) {
            try { return Integer.parseInt((String) val); } catch (Exception ignored) {}
        }
        return defaultVal;
    }

    private String parseString(Object val) {
        return val != null ? val.toString() : "";
    }

    private List<String> parseList(Object val) {
        if (val instanceof List) {
            List<?> rawList = (List<?>) val;
            List<String> result = new ArrayList<>();
            for (Object item : rawList) {
                if (item != null) result.add(item.toString());
            }
            return result;
        }
        if (val instanceof String) {
            String str = (String) val;
            if (!str.trim().isEmpty()) {
                return Arrays.asList(str.split(",\\s*"));
            }
        }
        return Collections.emptyList();
    }

    private StudentProfile ensureSubjectMastery(StudentProfile profile) {
        if (profile != null && profile.getSubjects() != null && !profile.getSubjects().isEmpty()) {
            Map<String, Double> masteryMap = profile.getConceptMastery() != null ? profile.getConceptMastery() : new HashMap<>();
            boolean updated = false;
            for (String subj : profile.getSubjects()) {
                if (subj != null && !subj.trim().isEmpty() && !masteryMap.containsKey(subj)) {
                    masteryMap.put(subj, 0.0);
                    updated = true;
                }
            }
            if (updated) {
                profile.setConceptMastery(masteryMap);
                return profileRepository.save(profile);
            }
        }
        return profile;
    }
}
