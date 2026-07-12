package com.onep.internship.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.web.multipart.MultipartFile;

public class ApplicationForm {

    @NotBlank(message = "Le prénom est requis.")
    @Size(max = 100, message = "Le prénom ne peut pas dépasser 100 caractères.")
    private String firstName;

    @NotBlank(message = "Le nom est requis.")
    @Size(max = 100, message = "Le nom ne peut pas dépasser 100 caractères.")
    private String lastName;

    @NotBlank(message = "L'email est requis.")
    @Email(message = "Veuillez entrer une adresse email valide.")
    @Size(max = 150, message = "L'email ne peut pas dépasser 150 caractères.")
    private String email;

    @NotBlank(message = "Le téléphone est requis.")
    @Size(max = 20, message = "Le téléphone ne peut pas dépasser 20 caractères.")
    private String phone;

    @NotBlank(message = "Veuillez sélectionner votre université.")
    private String university;

    private String universityOther;

    @NotBlank(message = "La filière est requise.")
    @Size(max = 200, message = "La filière ne peut pas dépasser 200 caractères.")
    private String major;

    @Size(max = 5000, message = "La lettre de motivation ne peut pas dépasser 5000 caractères.")
    private String coverMessage;

    private MultipartFile cvFile;

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

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
        return "__other__".equals(university) ? universityOther : university;
    }

    @AssertTrue(message = "Veuillez préciser votre établissement.")
    public boolean isUniversityValid() {
        return !"__other__".equals(university) || (universityOther != null && !universityOther.isBlank());
    }
}
