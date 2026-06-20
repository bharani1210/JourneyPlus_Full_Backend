package com.journeyplus.analytics.entity;
 
import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;
 
@Entity
@Table(name = "travel_reports")
@Getter
@Setter
public class TravelReport {
 
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
 
    @Column(nullable = false, length = 150)
    private String title;
 
    @Column(name = "report_type", nullable = false, length = 50)
    private String reportType; // SPENDING_BY_DEPT, POLICY_VIOLATIONS, ADVANCE_AGING
 
    @Column(columnDefinition = "TEXT")
    private String parameters;
 
    @Column(name = "generated_by", nullable = false, length = 100)
    private String generatedBy;
 
    @Column(name = "generated_at")
    private LocalDateTime generatedAt = LocalDateTime.now();
 
    @Column(name = "file_path", nullable = false)
    private String filePath;
 
    public TravelReport() {}
 
    public TravelReport(String title, String reportType, String parameters, String generatedBy, String filePath) {
        this.title = title;
        this.reportType = reportType;
        this.parameters = parameters;
        this.generatedBy = generatedBy;
        this.filePath = filePath;
        this.generatedAt = LocalDateTime.now();
    }
}
