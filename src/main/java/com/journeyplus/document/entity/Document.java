package com.journeyplus.document.entity;
 
import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.Getter;
 
@Entity
@Table(name = "documents")
@Getter
public class Document {
 
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
 
    @Column(nullable = false)
    private String filename;
 
    @Column(nullable = false)
    private String contentType;
 
    @Column(nullable = false)
    private String path;
 
    @Column(name = "owner_id", nullable = false)
    private Long ownerId;
 
    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();
 
    public Document() {}
 
    public Document(String filename, String contentType, String path, Long ownerId) {
        this.filename = filename;
        this.contentType = contentType;
        this.path = path;
        this.ownerId = ownerId;
    }
}
