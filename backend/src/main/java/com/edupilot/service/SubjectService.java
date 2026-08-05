package com.edupilot.service;

import com.edupilot.dto.SubjectRequest;
import com.edupilot.dto.SubjectResponse;
import com.edupilot.model.Subject;
import com.edupilot.repository.SubjectRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class SubjectService {

    @Autowired
    private SubjectRepository subjectRepository;

    @PostConstruct
    public void initDefaultCatalog() {
        if (subjectRepository.count() == 0) {
            List<Subject> seeds = new ArrayList<>();

            // Branch: Computer Science & Engineering
            seeds.add(new Subject(null, "EduPilot Academy", "B.Tech", "Computer Science & Engineering", 1, "CS101", "Programming in C", 4, true));
            seeds.add(new Subject(null, "EduPilot Academy", "B.Tech", "Computer Science & Engineering", 1, "CS102", "Engineering Mathematics I", 4, true));
            seeds.add(new Subject(null, "EduPilot Academy", "B.Tech", "Computer Science & Engineering", 1, "CS103", "Engineering Physics", 3, true));

            seeds.add(new Subject(null, "EduPilot Academy", "B.Tech", "Computer Science & Engineering", 2, "CS201", "Object Oriented Programming in Java", 4, true));
            seeds.add(new Subject(null, "EduPilot Academy", "B.Tech", "Computer Science & Engineering", 2, "CS202", "Digital Logic & Computer Organization", 3, true));
            seeds.add(new Subject(null, "EduPilot Academy", "B.Tech", "Computer Science & Engineering", 2, "CS203", "Linear Algebra & Probability", 3, true));

            seeds.add(new Subject(null, "EduPilot Academy", "B.Tech", "Computer Science & Engineering", 3, "CS301", "Data Structures & Algorithms", 4, true));
            seeds.add(new Subject(null, "EduPilot Academy", "B.Tech", "Computer Science & Engineering", 3, "CS302", "Database Management Systems", 4, true));
            seeds.add(new Subject(null, "EduPilot Academy", "B.Tech", "Computer Science & Engineering", 3, "CS303", "Discrete Mathematical Structures", 3, true));

            seeds.add(new Subject(null, "EduPilot Academy", "B.Tech", "Computer Science & Engineering", 4, "CS401", "Operating Systems", 4, true));
            seeds.add(new Subject(null, "EduPilot Academy", "B.Tech", "Computer Science & Engineering", 4, "CS402", "Computer Networks", 4, true));
            seeds.add(new Subject(null, "EduPilot Academy", "B.Tech", "Computer Science & Engineering", 4, "CS403", "Theory of Computation", 3, true));

            seeds.add(new Subject(null, "EduPilot Academy", "B.Tech", "Computer Science & Engineering", 5, "CS501", "Design & Analysis of Algorithms", 4, true));
            seeds.add(new Subject(null, "EduPilot Academy", "B.Tech", "Computer Science & Engineering", 5, "CS502", "Software Engineering", 3, true));
            seeds.add(new Subject(null, "EduPilot Academy", "B.Tech", "Computer Science & Engineering", 5, "CS503", "Web Technologies & Systems", 3, true));

            seeds.add(new Subject(null, "EduPilot Academy", "B.Tech", "Computer Science & Engineering", 6, "CS601", "Artificial Intelligence & Machine Learning", 4, true));
            seeds.add(new Subject(null, "EduPilot Academy", "B.Tech", "Computer Science & Engineering", 6, "CS602", "Compiler Design", 4, true));
            seeds.add(new Subject(null, "EduPilot Academy", "B.Tech", "Computer Science & Engineering", 6, "CS603", "Cloud Computing & Architecture", 3, true));

            seeds.add(new Subject(null, "EduPilot Academy", "B.Tech", "Computer Science & Engineering", 7, "CS701", "Deep Learning & Neural Networks", 4, true));
            seeds.add(new Subject(null, "EduPilot Academy", "B.Tech", "Computer Science & Engineering", 7, "CS702", "Cyber Security & Cryptography", 3, true));

            seeds.add(new Subject(null, "EduPilot Academy", "B.Tech", "Computer Science & Engineering", 8, "CS801", "Capstone Senior Thesis & Project", 8, true));

            // Branch: Artificial Intelligence & Data Science
            seeds.add(new Subject(null, "EduPilot Academy", "B.Tech", "Artificial Intelligence & Data Science", 1, "AI101", "Introduction to Data Science", 4, true));
            seeds.add(new Subject(null, "EduPilot Academy", "B.Tech", "Artificial Intelligence & Data Science", 1, "AI102", "Python for Scientific Computing", 4, true));
            seeds.add(new Subject(null, "EduPilot Academy", "B.Tech", "Artificial Intelligence & Data Science", 2, "AI201", "Applied Probability & Statistics", 4, true));
            seeds.add(new Subject(null, "EduPilot Academy", "B.Tech", "Artificial Intelligence & Data Science", 3, "AI301", "Data Mining & Predictive Modeling", 4, true));
            seeds.add(new Subject(null, "EduPilot Academy", "B.Tech", "Artificial Intelligence & Data Science", 4, "AI401", "Deep Learning Architectures", 4, true));

            // Branch: Information Technology
            seeds.add(new Subject(null, "EduPilot Academy", "B.Tech", "Information Technology", 1, "IT101", "Fundamentals of Information Technology", 4, true));
            seeds.add(new Subject(null, "EduPilot Academy", "B.Tech", "Information Technology", 2, "IT201", "Full Stack Web Development", 4, true));
            seeds.add(new Subject(null, "EduPilot Academy", "B.Tech", "Information Technology", 3, "IT301", "Database Systems & SQL", 4, true));

            subjectRepository.saveAll(seeds);
        }
    }

    public List<SubjectResponse> getAllActiveSubjects() {
        return subjectRepository.findByIsActiveTrue()
                .stream()
                .map(SubjectResponse::new)
                .collect(Collectors.toList());
    }

    public List<String> getDistinctBranches() {
        List<Subject> active = subjectRepository.findByIsActiveTrue();
        Set<String> branches = new LinkedHashSet<>();
        for (Subject s : active) {
            if (s.getBranch() != null && !s.getBranch().trim().isEmpty()) {
                branches.add(s.getBranch());
            }
        }
        return new ArrayList<>(branches);
    }

    public List<SubjectResponse> getSubjectsByBranch(String branch) {
        if (branch == null || branch.trim().isEmpty()) {
            return Collections.emptyList();
        }
        return subjectRepository.findByBranchAndIsActiveTrue(branch)
                .stream()
                .map(SubjectResponse::new)
                .collect(Collectors.toList());
    }

    public List<SubjectResponse> getSubjectsByBranchAndSemester(String branch, int semester) {
        if (branch == null || branch.trim().isEmpty() || semester < 1) {
            return Collections.emptyList();
        }
        return subjectRepository.findByBranchAndSemesterAndIsActiveTrue(branch, semester)
                .stream()
                .map(SubjectResponse::new)
                .collect(Collectors.toList());
    }

    public Optional<SubjectResponse> getSubjectByCode(String subjectCode) {
        if (subjectCode == null || subjectCode.trim().isEmpty()) {
            return Optional.empty();
        }
        return subjectRepository.findBySubjectCode(subjectCode.trim().toUpperCase())
                .map(SubjectResponse::new);
    }

    public SubjectResponse createOrUpdateSubject(SubjectRequest req) {
        if (req.getSubjectCode() == null || req.getSubjectCode().trim().isEmpty()) {
            throw new IllegalArgumentException("Subject code is required.");
        }
        if (req.getSubjectName() == null || req.getSubjectName().trim().isEmpty()) {
            throw new IllegalArgumentException("Subject name is required.");
        }

        String code = req.getSubjectCode().trim().toUpperCase();
        Optional<Subject> existingOpt = subjectRepository.findBySubjectCode(code);
        
        Subject subject = existingOpt.orElseGet(Subject::new);
        subject.setInstitution(req.getInstitution() != null ? req.getInstitution() : "EduPilot Academy");
        subject.setDegree(req.getDegree() != null ? req.getDegree() : "B.Tech");
        subject.setBranch(req.getBranch() != null ? req.getBranch() : "Computer Science & Engineering");
        subject.setSemester(req.getSemester() > 0 ? req.getSemester() : 1);
        subject.setSubjectCode(code);
        subject.setSubjectName(req.getSubjectName().trim());
        subject.setCredits(req.getCredits() > 0 ? req.getCredits() : 4);
        subject.setActive(req.isActive());

        Subject saved = subjectRepository.save(subject);
        return new SubjectResponse(saved);
    }
}
