package com.onep.internship.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "applications")
public class Application {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    private Long version;

    @Column
    private String firstName;

    @Column
    private String lastName;

    @Column
    private String email;

    @Column(length = 30)
    private String cni;

    @Column
    private String phone;

    @Column
    private String university;

    @Column
    private String major;

    @Column(columnDefinition = "TEXT")
    private String coverMessage;

    private String cvFilePath;

    @Column
    private LocalDateTime submittedDate;

    /** Populated when status transitions from DRAFT to SUBMITTED. NULL for drafts. Used for DB-level unique constraint. */
    @Column(name = "cycle_year")
    private Integer cycleYear;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ApplicationStatus status = ApplicationStatus.DRAFT;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "applicant_id")
    private User applicant;

    public Application() {}

    public Application(String firstName, String lastName, String email, String phone,
                       String university, String major, String coverMessage) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.phone = phone;
        this.university = university;
        this.major = major;
        this.coverMessage = coverMessage;
        this.status = ApplicationStatus.DRAFT;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getVersion() { return version; }

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getCni() { return cni; }
    public void setCni(String cni) { this.cni = cni; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getUniversity() { return university; }
    public void setUniversity(String university) { this.university = university; }

    public String getMajor() { return major; }
    public void setMajor(String major) { this.major = major; }

    public String getCoverMessage() { return coverMessage; }
    public void setCoverMessage(String coverMessage) { this.coverMessage = coverMessage; }

    public String getCvFilePath() { return cvFilePath; }
    public void setCvFilePath(String cvFilePath) { this.cvFilePath = cvFilePath; }

    public LocalDateTime getSubmittedDate() { return submittedDate; }
    public void setSubmittedDate(LocalDateTime submittedDate) { this.submittedDate = submittedDate; }

    public Integer getCycleYear() { return cycleYear; }
    public void setCycleYear(Integer cycleYear) { this.cycleYear = cycleYear; }

    public ApplicationStatus getStatus() { return status; }
    public void setStatus(ApplicationStatus status) { this.status = status; }

    public User getApplicant() { return applicant; }
    public void setApplicant(User applicant) { this.applicant = applicant; }
}
