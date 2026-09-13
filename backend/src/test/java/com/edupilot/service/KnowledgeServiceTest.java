package com.edupilot.service;

import com.edupilot.dto.ConceptMasteryResponse;
import com.edupilot.dto.KnowledgeProfileResponse;
import com.edupilot.model.ConceptMastery;
import com.edupilot.model.KnowledgeProfile;
import com.edupilot.model.StudentProfile;
import com.edupilot.repository.ConceptMasteryRepository;
import com.edupilot.repository.KnowledgeProfileRepository;
import com.edupilot.repository.StudentProfileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

public class KnowledgeServiceTest {

    private ConceptMasteryRepository conceptRepository;
    private KnowledgeProfileRepository profileRepository;
    private StudentProfileRepository studentProfileRepository;
    private StudentService studentService;
    private RecommendationService recommendationService;
    private StudentStateSnapshotService studentStateSnapshotService;
    private KnowledgeService knowledgeService;

    @BeforeEach
    public void setUp() throws Exception {
        conceptRepository = mock(ConceptMasteryRepository.class);
        profileRepository = mock(KnowledgeProfileRepository.class);
        studentProfileRepository = mock(StudentProfileRepository.class);
        studentService = mock(StudentService.class);
        recommendationService = mock(RecommendationService.class);
        studentStateSnapshotService = mock(StudentStateSnapshotService.class);

        knowledgeService = new KnowledgeService();

        setField(knowledgeService, "conceptRepository", conceptRepository);
        setField(knowledgeService, "profileRepository", profileRepository);
        setField(knowledgeService, "studentProfileRepository", studentProfileRepository);
        setField(knowledgeService, "studentService", studentService);
        setField(knowledgeService, "recommendationService", recommendationService);
        setField(knowledgeService, "studentStateSnapshotService", studentStateSnapshotService);
    }

    private void setField(Object target, String fieldName, Object value) throws Exception {
        var field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    @Test
    public void testCanonicalUserIdResolution() {
        when(studentService.resolveUserId("user@example.com")).thenReturn("canonical_user_123");
        when(profileRepository.findByUserId("canonical_user_123")).thenReturn(Optional.empty());
        when(conceptRepository.findByUserId("canonical_user_123")).thenReturn(Collections.emptyList());

        KnowledgeProfileResponse response = knowledgeService.getKnowledgeProfile("user@example.com");

        assertNotNull(response);
        assertEquals("canonical_user_123", response.getUserId());
        verify(studentService).resolveUserId("user@example.com");
        verify(profileRepository).findByUserId("canonical_user_123");
        verify(conceptRepository).findByUserId("canonical_user_123");
    }

    @Test
    public void testFirstAttemptStrongVisibilityInKnowledgeProfile() {
        String userId = "canonical_user_123";
        when(studentService.resolveUserId(userId)).thenReturn(userId);

        ConceptMastery cm = new ConceptMastery();
        cm.setUserId(userId);
        cm.setSubjectName("Data Structures & Algorithms");
        cm.setConceptName("Arrays & Linked Lists");
        cm.setAttemptCount(1);
        cm.setCorrectCount(1);
        cm.setAccuracy(100.0);
        cm.setMasteryLevel(ConceptMastery.MasteryLevel.MASTER);
        cm.setStatus(ConceptMastery.ConceptStatus.UNCERTAIN); // Status remains UNCERTAIN for 1 attempt

        when(conceptRepository.findByUserId(userId)).thenReturn(List.of(cm));
        when(profileRepository.findByUserId(userId)).thenReturn(Optional.empty());

        StudentProfile mockProfile = new StudentProfile();
        mockProfile.setUserId(userId);
        when(studentService.findOrCreateProfile(userId)).thenReturn(mockProfile);

        knowledgeService.syncKnowledgeProfileSummary(userId, "Data Structures & Algorithms");

        ArgumentCaptor<KnowledgeProfile> kpCaptor = ArgumentCaptor.forClass(KnowledgeProfile.class);
        verify(profileRepository).save(kpCaptor.capture());

        KnowledgeProfile savedKp = kpCaptor.getValue();
        assertNotNull(savedKp);
        assertEquals(1, savedKp.getMasteredCount());
        assertTrue(savedKp.getStrongConcepts().contains("Arrays & Linked Lists"), "1st attempt concept with 100% accuracy must appear in strongConcepts list");
        assertFalse(savedKp.getWeakConcepts().contains("Arrays & Linked Lists"));
    }

    @Test
    public void testFirstAttemptWeakVisibilityInKnowledgeProfile() {
        String userId = "canonical_user_123";
        when(studentService.resolveUserId(userId)).thenReturn(userId);

        ConceptMastery cm = new ConceptMastery();
        cm.setUserId(userId);
        cm.setSubjectName("Data Structures & Algorithms");
        cm.setConceptName("Trees & Graphs");
        cm.setAttemptCount(1);
        cm.setCorrectCount(0);
        cm.setAccuracy(40.0);
        cm.setMasteryLevel(ConceptMastery.MasteryLevel.BEGINNER);
        cm.setStatus(ConceptMastery.ConceptStatus.UNCERTAIN); // Status remains UNCERTAIN for 1 attempt

        when(conceptRepository.findByUserId(userId)).thenReturn(List.of(cm));
        when(profileRepository.findByUserId(userId)).thenReturn(Optional.empty());

        StudentProfile mockProfile = new StudentProfile();
        mockProfile.setUserId(userId);
        when(studentService.findOrCreateProfile(userId)).thenReturn(mockProfile);

        knowledgeService.syncKnowledgeProfileSummary(userId, "Data Structures & Algorithms");

        ArgumentCaptor<KnowledgeProfile> kpCaptor = ArgumentCaptor.forClass(KnowledgeProfile.class);
        verify(profileRepository).save(kpCaptor.capture());

        KnowledgeProfile savedKp = kpCaptor.getValue();
        assertNotNull(savedKp);
        assertEquals(1, savedKp.getBeginnerCount());
        assertTrue(savedKp.getWeakConcepts().contains("Trees & Graphs"), "1st attempt concept with <70% accuracy must appear in weakConcepts list");
        assertFalse(savedKp.getStrongConcepts().contains("Trees & Graphs"));
    }

    @Test
    public void testExistingMultiAttemptBehaviorPreserved() {
        String userId = "canonical_user_123";
        when(studentService.resolveUserId(userId)).thenReturn(userId);

        ConceptMastery cmStrong = new ConceptMastery();
        cmStrong.setUserId(userId);
        cmStrong.setSubjectName("Data Structures & Algorithms");
        cmStrong.setConceptName("Stacks & Queues");
        cmStrong.setAttemptCount(4);
        cmStrong.setCorrectCount(4);
        cmStrong.setAccuracy(100.0);
        cmStrong.setMasteryLevel(ConceptMastery.MasteryLevel.MASTER);
        cmStrong.setStatus(ConceptMastery.ConceptStatus.STRONG);

        ConceptMastery cmWeak = new ConceptMastery();
        cmWeak.setUserId(userId);
        cmWeak.setSubjectName("Data Structures & Algorithms");
        cmWeak.setConceptName("Sorting Algorithms");
        cmWeak.setAttemptCount(4);
        cmWeak.setCorrectCount(1);
        cmWeak.setAccuracy(25.0);
        cmWeak.setMasteryLevel(ConceptMastery.MasteryLevel.BEGINNER);
        cmWeak.setStatus(ConceptMastery.ConceptStatus.WEAK);

        when(conceptRepository.findByUserId(userId)).thenReturn(List.of(cmStrong, cmWeak));
        when(profileRepository.findByUserId(userId)).thenReturn(Optional.empty());

        StudentProfile mockProfile = new StudentProfile();
        mockProfile.setUserId(userId);
        when(studentService.findOrCreateProfile(userId)).thenReturn(mockProfile);

        knowledgeService.syncKnowledgeProfileSummary(userId, "Data Structures & Algorithms");

        ArgumentCaptor<KnowledgeProfile> kpCaptor = ArgumentCaptor.forClass(KnowledgeProfile.class);
        verify(profileRepository).save(kpCaptor.capture());

        KnowledgeProfile savedKp = kpCaptor.getValue();
        assertNotNull(savedKp);
        assertEquals(1, savedKp.getMasteredCount());
        assertEquals(1, savedKp.getBeginnerCount());
        assertTrue(savedKp.getStrongConcepts().contains("Stacks & Queues"));
        assertTrue(savedKp.getWeakConcepts().contains("Sorting Algorithms"));
    }
}
