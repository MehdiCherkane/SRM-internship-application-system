package com.onep.internship.model;

public enum ApplicationStatus {
    DRAFT("Brouillon", "draft"),
    SUBMITTED("Soumise", "submitted"),
    UNDER_REVIEW("En cours d'examen", "under-review"),
    ACCEPTED("Acceptée", "accepted"),
    REJECTED("Refusée", "rejected");

    private final String label;
    private final String cssClass;

    ApplicationStatus(String label, String cssClass) {
        this.label = label;
        this.cssClass = cssClass;
    }

    public String getLabel() {
        return label;
    }

    public String getCssClass() {
        return cssClass;
    }

    public boolean isApplicantEditable() {
        return this == DRAFT || this == SUBMITTED;
    }
}
