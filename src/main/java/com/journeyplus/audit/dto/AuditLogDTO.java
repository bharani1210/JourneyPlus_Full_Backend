

package com.journeyplus.audit.dto;
 
import java.time.LocalDateTime;
 
public class AuditLogDTO {
 
    private String username;
    private String action;
    private String module;
    private LocalDateTime timestamp;
    private String details;
 
    public AuditLogDTO(String username, String action, String module,
                       LocalDateTime timestamp, String details) {
        this.username = username;
        this.action = action;
        this.module = module;
        this.timestamp = timestamp;
        this.details = details;
    }
 
    // Getters
 
    public String getUsername() { return username; }
    public String getAction() { return action; }
    public String getModule() { return module; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public String getDetails() { return details; }
}