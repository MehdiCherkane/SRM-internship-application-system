package com.onep.internship.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.web.multipart.MultipartFile;

public class ApplicationForm {

    @NotBlank(message = "Le prénom est requis.", groups = SubmissionValidation.class)
    @Size(max = 100, message = "Le prénom ne peut pas dépasser 100 caractères.", groups = SubmissionValidation.class)
    private String firstName;

    @NotBlank(message = "Le nom est requis.", groups = SubmissionValidation.class)
    @Size(max = 100, message = "Le nom ne peut pas dépasser 100 caractères.", groups = SubmissionValidation.class)
    private String lastName;

    @NotBlank(message = "L'email est requis.", groups = SubmissionValidation.class)
    @Email(message = "Veuillez entrer une adresse email valide.", groups = SubmissionValidation.class)
    @Size(max = 150, message = "L'email ne peut pas dépasser 150 caractères.", groups = SubmissionValidation.class)
    private String email;

    @NotBlank(message = "Le CIN est requis.", groups = SubmissionValidation.class)
    @Size(max = 30, message = "Le CIN ne peut pas dépasser 30 caractères.", groups = SubmissionValidation.class)
    @Pattern(regexp = "^[A-Za-z0-9-]+$", message = "Le CIN contient des caractères non valides.", groups = SubmissionValidation.class)
    private String cni;

    @NotBlank(message = "Le téléphone est requis.", groups = SubmissionValidation.class)
    @Size(max = 20, message = "Le téléphone ne peut pas dépasser 20 caractères.", groups = SubmissionValidation.class)
    @Pattern(regexp = "^(?:0[5-7]\\d{8}|(?:\\+212|00212)[5-7]\\d{8})$", message = "Veuillez entrer un numéro de téléphone marocain valide (05/06/07 ou +212).", groups = SubmissionValidation.class)
    private String phone;

    @NotBlank(message = "Veuillez sélectionner votre université.", groups = SubmissionValidation.class)
    @Size(max = 200, message = "L'université ne peut pas dépasser 200 caractères.", groups = SubmissionValidation.class)
    private String university;

    @Size(max = 200, message = "L'établissement ne peut pas dépasser 200 caractères.", groups = SubmissionValidation.class)
    private String universityOther;

    @NotBlank(message = "La filière est requise.", groups = SubmissionValidation.class)
    @Size(max = 200, message = "La filière ne peut pas dépasser 200 caractères.", groups = SubmissionValidation.class)
    private String major;

    @Size(max = 5000, message = "La lettre de motivation ne peut pas dépasser 5000 caractères.", groups = SubmissionValidation.class)
    private String coverMessage;

    private MultipartFile cvFile;

    private Long id;

    private boolean existingCv;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public boolean isExistingCv() { return existingCv; }
    public void setExistingCv(boolean existingCv) { this.existingCv = existingCv; }

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

    public String getUniversityOther() { return universityOther; }
    public void setUniversityOther(String universityOther) { this.universityOther = universityOther; }

    public String getMajor() { return major; }
    public void setMajor(String major) { this.major = major; }

    public String getCoverMessage() { return coverMessage; }
    public void setCoverMessage(String coverMessage) { this.coverMessage = coverMessage; }

    public MultipartFile getCvFile() { return cvFile; }
    public void setCvFile(MultipartFile cvFile) { this.cvFile = cvFile; }

    public String getResolvedUniversity() {
        if ("__other__".equals(university)) {
            return universityOther != null ? universityOther.trim() : "";
        }
        return university != null ? university.trim() : "";
    }

    @AssertTrue(message = "Veuillez préciser votre établissement.", groups = SubmissionValidation.class)
    public boolean isUniversityValid() {
        return !"__other__".equals(university) || (universityOther != null && !universityOther.isBlank());
    }
}
