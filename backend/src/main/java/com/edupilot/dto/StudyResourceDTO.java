package com.edupilot.dto;

import java.util.List;

public class StudyResourceDTO {

    public static class ResourceItem {
        private String title;
        private String url;
        private String domain;
        private String description;

        public ResourceItem() {}

        public ResourceItem(String title, String url, String domain, String description) {
            this.title = title;
            this.url = url;
            this.domain = domain;
            this.description = description;
        }

        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public String getUrl() { return url; }
        public void setUrl(String url) { this.url = url; }
        public String getDomain() { return domain; }
        public void setDomain(String domain) { this.domain = domain; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
    }

    private String subject;
    private String concept;
    private int totalResources;
    private List<ResourceItem> resources;

    public StudyResourceDTO() {}

    public StudyResourceDTO(String subject, String concept, List<ResourceItem> resources) {
        this.subject = subject;
        this.concept = concept;
        this.resources = resources;
        this.totalResources = resources != null ? resources.size() : 0;
    }

    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }
    public String getConcept() { return concept; }
    public void setConcept(String concept) { this.concept = concept; }
    public int getTotalResources() { return totalResources; }
    public void setTotalResources(int totalResources) { this.totalResources = totalResources; }
    public List<ResourceItem> getResources() { return resources; }
    public void setResources(List<ResourceItem> resources) {
        this.resources = resources;
        this.totalResources = resources != null ? resources.size() : 0;
    }
}
