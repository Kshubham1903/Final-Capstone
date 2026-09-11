package com.edupilot.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Document(collection = "subject_roadmaps")
public class SubjectRoadmap {

    @Id
    private String id;
    private String userId;
    private String subjectCode;
    private String subjectName;
    private int version = 1;
    private RoadmapStatus status = RoadmapStatus.ACTIVE;
    private LocalDateTime createdAt = LocalDateTime.now();
    private LocalDateTime updatedAt = LocalDateTime.now();
    private List<RoadmapTopicNode> topics = new ArrayList<>();

    public enum RoadmapStatus {
        ACTIVE,
        INACTIVE
    }

    public enum TopicStatus {
        LOCKED,
        UNLOCKED,
        COMPLETED
    }

    public static class RoadmapTopicNode {
        private String conceptId;
        private String conceptName;
        private int sequence;
        private String description;
        private List<String> prerequisiteConceptIds = new ArrayList<>();
        private TopicStatus status = TopicStatus.LOCKED;
        private double masteryRequiredAccuracy = 70.0;
        private double currentAccuracy = 0.0;
        private boolean isCompleted = false;
        private String aiRationale;

        public RoadmapTopicNode() {
        }

        public RoadmapTopicNode(String conceptId, String conceptName, int sequence, String description,
                                List<String> prerequisiteConceptIds, TopicStatus status,
                                double masteryRequiredAccuracy, double currentAccuracy,
                                boolean isCompleted, String aiRationale) {
            this.conceptId = conceptId;
            this.conceptName = conceptName;
            this.sequence = sequence;
            this.description = description;
            this.prerequisiteConceptIds = prerequisiteConceptIds != null ? prerequisiteConceptIds : new ArrayList<>();
            this.status = status != null ? status : TopicStatus.LOCKED;
            this.masteryRequiredAccuracy = masteryRequiredAccuracy;
            this.currentAccuracy = currentAccuracy;
            this.isCompleted = isCompleted;
            this.aiRationale = aiRationale;
        }

        public String getConceptId() {
            return conceptId;
        }

        public void setConceptId(String conceptId) {
            this.conceptId = conceptId;
        }

        public String getConceptName() {
            return conceptName;
        }

        public void setConceptName(String conceptName) {
            this.conceptName = conceptName;
        }

        public int getSequence() {
            return sequence;
        }

        public void setSequence(int sequence) {
            this.sequence = sequence;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public List<String> getPrerequisiteConceptIds() {
            return prerequisiteConceptIds;
        }

        public void setPrerequisiteConceptIds(List<String> prerequisiteConceptIds) {
            this.prerequisiteConceptIds = prerequisiteConceptIds;
        }

        public TopicStatus getStatus() {
            return status;
        }

        public void setStatus(TopicStatus status) {
            this.status = status;
        }

        public double getMasteryRequiredAccuracy() {
            return masteryRequiredAccuracy;
        }

        public void setMasteryRequiredAccuracy(double masteryRequiredAccuracy) {
            this.masteryRequiredAccuracy = masteryRequiredAccuracy;
        }

        public double getCurrentAccuracy() {
            return currentAccuracy;
        }

        public void setCurrentAccuracy(double currentAccuracy) {
            this.currentAccuracy = currentAccuracy;
        }

        public boolean isCompleted() {
            return isCompleted;
        }

        public void setCompleted(boolean completed) {
            isCompleted = completed;
        }

        public String getAiRationale() {
            return aiRationale;
        }

        public void setAiRationale(String aiRationale) {
            this.aiRationale = aiRationale;
        }
    }

    public SubjectRoadmap() {
    }

    public SubjectRoadmap(String id, String userId, String subjectCode, String subjectName, int version,
                          RoadmapStatus status, LocalDateTime createdAt, LocalDateTime updatedAt,
                          List<RoadmapTopicNode> topics) {
        this.id = id;
        this.userId = userId;
        this.subjectCode = subjectCode;
        this.subjectName = subjectName;
        this.version = version;
        this.status = status != null ? status : RoadmapStatus.ACTIVE;
        this.createdAt = createdAt != null ? createdAt : LocalDateTime.now();
        this.updatedAt = updatedAt != null ? updatedAt : LocalDateTime.now();
        this.topics = topics != null ? topics : new ArrayList<>();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getSubjectCode() {
        return subjectCode;
    }

    public void setSubjectCode(String subjectCode) {
        this.subjectCode = subjectCode;
    }

    public String getSubjectName() {
        return subjectName;
    }

    public void setSubjectName(String subjectName) {
        this.subjectName = subjectName;
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    public RoadmapStatus getStatus() {
        return status;
    }

    public void setStatus(RoadmapStatus status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public List<RoadmapTopicNode> getTopics() {
        return topics;
    }

    public void setTopics(List<RoadmapTopicNode> topics) {
        this.topics = topics;
    }
}
