package com.edupilot.service;

import com.edupilot.dto.StudyResourceDTO;
import com.edupilot.model.StudentProfile;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class RoadmapResourceRankingService {

    @Autowired
    private StudyResourceService studyResourceService;

    @Autowired
    private StudentStateService studentStateService;

    @Autowired
    private StudentService studentService;

    /**
     * Retrieves study resources from StudyResourceService and ranks them based on the student's VARK preferences.
     */
    public StudyResourceDTO getRankedResourcesForTopic(String userId, String subjectName, String conceptName) {
        if (conceptName == null || conceptName.isBlank()) {
            return new StudyResourceDTO(subjectName, conceptName, Collections.emptyList());
        }

        // 1. Fetch raw resources from existing StudyResourceService
        StudyResourceDTO dto = studyResourceService.getStudyResources(subjectName, conceptName);
        if (dto == null || dto.getResources() == null || dto.getResources().isEmpty()) {
            return dto != null ? dto : new StudyResourceDTO(subjectName, conceptName, Collections.emptyList());
        }

        // 2. Retrieve student VARK preferences from existing StudentStateService with safe fallback
        Map<String, Double> varkPreferences = getSafeVarkPreferences(userId);

        // 3. Rank resources using VARK compatibility scoring
        List<StudyResourceDTO.ResourceItem> rankedItems = new ArrayList<>(dto.getResources());
        rankedItems.sort((r1, r2) -> Double.compare(
                calculateVarkScore(r2, varkPreferences),
                calculateVarkScore(r1, varkPreferences)
        ));

        // 4. Return updated DTO with ranked items
        StudyResourceDTO rankedDto = new StudyResourceDTO(dto.getSubject(), dto.getConcept(), rankedItems);
        return rankedDto;
    }

    /**
     * Safely fetches VARK preferences from StudentStateService, falling back gracefully if uninitialized.
     */
    private Map<String, Double> getSafeVarkPreferences(String userId) {
        Map<String, Double> prefs = null;
        try {
            if (userId != null && !userId.isBlank() && !"anonymous_student".equals(userId)) {
                String canonicalUserId = studentService.resolveUserId(userId);
                StudentProfile profile = studentService.findOrCreateProfile(canonicalUserId);
                prefs = studentStateService.computePreferenceScores(canonicalUserId, profile);
            }
        } catch (Exception ex) {
            System.err.println("[RoadmapResourceRankingService] VARK fetch exception: " + ex.getMessage() + ". Using fallback preferences.");
        }

        if (prefs == null || prefs.isEmpty()) {
            prefs = new LinkedHashMap<>();
            prefs.put("visual", 0.75);
            prefs.put("readingVerbal", 0.60);
            prefs.put("practicalKinesthetic", 0.75);
            prefs.put("sequentialGlobal", 0.70);
            prefs.put("feedbackPractice", 0.70);
        }

        return prefs;
    }

    /**
     * Computes deterministic VARK suitability score for a given resource item.
     */
    public double calculateVarkScore(StudyResourceDTO.ResourceItem item, Map<String, Double> vark) {
        if (item == null) return 0.0;

        double pVis = vark.getOrDefault("visual", 0.60);
        double pPract = vark.getOrDefault("practicalKinesthetic", 0.60);
        double pRead = vark.getOrDefault("readingVerbal", 0.60);
        double pSeq = vark.getOrDefault("sequentialGlobal", 0.60);

        String domain = item.getDomain() != null ? item.getDomain().toLowerCase() : "";
        String title = item.getTitle() != null ? item.getTitle().toLowerCase() : "";
        String desc = item.getDescription() != null ? item.getDescription().toLowerCase() : "";
        String combined = domain + " " + title + " " + desc;

        // Visual compatibility
        double cVis = (domain.contains("youtube.com") || combined.contains("video") || combined.contains("tutorial") 
                || combined.contains("lecture") || combined.contains("diagram") || combined.contains("visual")) ? 1.0 : 0.3;

        // Practical / Kinesthetic compatibility
        double cPract = (domain.contains("w3schools.com") || domain.contains("stackoverflow.com") || domain.contains("geeksforgeeks.org")
                || combined.contains("practice") || combined.contains("code") || combined.contains("coding") 
                || combined.contains("exercise") || combined.contains("interactive") || combined.contains("hands-on")) ? 1.0 : 0.3;

        // Reading / Verbal compatibility
        double cRead = (domain.contains("wikipedia.org") || domain.contains("arxiv.org") || domain.contains("openalex")
                || combined.contains("paper") || combined.contains("article") || combined.contains("documentation") 
                || combined.contains("reference") || combined.contains("text")) ? 1.0 : 0.3;

        // Sequential vs. Global compatibility
        double cSeq = (pSeq >= 0.65)
                ? ((domain.contains("w3schools.com") || domain.contains("geeksforgeeks.org") || domain.contains("stackoverflow.com")) ? 1.0 : 0.4)
                : ((domain.contains("wikipedia.org") || domain.contains("arxiv.org") || domain.contains("youtube.com")) ? 1.0 : 0.4);

        return (pVis * cVis) + (pPract * cPract) + (pRead * cRead) + (pSeq * cSeq);
    }
}
