package com.edupilot.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "users")
public class User {
    @Id
    private String id;
    private String email;
    private String password;
    private String fullName;
    private Role role; // STUDENT, FACULTY, ADMIN
    private AccountType accountType; // GENUINE_STUDENT, TEST_AUTOMATION, SYNTHETIC_RESEARCH_SEED
    private LocalDateTime createdAt;
    
    public enum Role {
        STUDENT,
        FACULTY,
        ADMIN
    }

    public enum AccountType {
        GENUINE_STUDENT,
        TEST_AUTOMATION,
        SYNTHETIC_RESEARCH_SEED
    }

    public User() {
    }

    public User(String id, String email, String password, String fullName, Role role, LocalDateTime createdAt) {
        this(id, email, password, fullName, role, null, createdAt);
    }

    public User(String id, String email, String password, String fullName, Role role, AccountType accountType, LocalDateTime createdAt) {
        this.id = id;
        this.email = email;
        this.password = password;
        this.fullName = fullName;
        this.role = role;
        this.accountType = accountType;
        this.createdAt = createdAt;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public AccountType getAccountType() {
        return accountType;
    }

    public void setAccountType(AccountType accountType) {
        this.accountType = accountType;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public static UserBuilder builder() {
        return new UserBuilder();
    }

    public static class UserBuilder {
        private String id;
        private String email;
        private String password;
        private String fullName;
        private Role role;
        private AccountType accountType;
        private LocalDateTime createdAt;

        public UserBuilder id(String id) {
            this.id = id;
            return this;
        }

        public UserBuilder email(String email) {
            this.email = email;
            return this;
        }

        public UserBuilder password(String password) {
            this.password = password;
            return this;
        }

        public UserBuilder fullName(String fullName) {
            this.fullName = fullName;
            return this;
        }

        public UserBuilder role(Role role) {
            this.role = role;
            return this;
        }

        public UserBuilder accountType(AccountType accountType) {
            this.accountType = accountType;
            return this;
        }

        public UserBuilder createdAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public User build() {
            return new User(id, email, password, fullName, role, accountType, createdAt);
        }
    }
}
