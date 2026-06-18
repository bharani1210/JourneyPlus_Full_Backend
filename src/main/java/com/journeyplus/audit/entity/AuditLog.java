package com.journeyplus.audit.entity;
 
import com.fasterxml.jackson.annotation.JsonIgnore;   // ✅ IMPORT ADDED
import com.journeyplus.iam.entity.User;
import jakarta.persistence.*;
import java.time.LocalDateTime;
 
@Entity
@Table(name = "audit_logs")
public class AuditLog {
 
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
 
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    @JsonIgnore   // ✅ FIX: Prevents infinite recursion / 500 error
    private User user;
 
    @Column(nullable = false, length = 100)
    private String username;
 
    @Column(nullable = false, length = 255)
    private String action;
 
    @Column(nullable = false, length = 100)
    private String module;
 
    @Column(nullable = false)
    private LocalDateTime timestamp = LocalDateTime.now();
 
    @Column(columnDefinition = "TEXT")
    private String details;
 
    public AuditLog() {}
 
    public AuditLog(User user, String username, String action, String module, String details) {
        this.user = user;
        this.username = username;
        this.action = action;
        this.module = module;
        this.details = details;
        this.timestamp = LocalDateTime.now();
    }
 
    // ✅ Getters and Setters
 
    public Long getId() {
        return id;
    }
 
    public void setId(Long id) {
        this.id = id;
    }
 
    public User getUser() {
        return user;
    }
 
    public void setUser(User user) {
        this.user = user;
    }
 
    public String getUsername() {
        return username;
    }
 
    public void setUsername(String username) {
        this.username = username;
    }
 
    public String getAction() {
        return action;
    }
 
    public void setAction(String action) {
        this.action = action;
    }
 
    public String getModule() {
        return module;
    }
 
    public void setModule(String module) {
        this.module = module;
    }
 
    public LocalDateTime getTimestamp() {
        return timestamp;
    }
 
    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
 
    public String getDetails() {
        return details;
    }
 
    public void setDetails(String details) {
        this.details = details;
    }
}