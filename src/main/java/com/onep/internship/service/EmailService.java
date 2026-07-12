package com.onep.internship.service;

import com.onep.internship.model.Application;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;
    private final String from;

    public EmailService(JavaMailSender mailSender,
                        @Value("${spring.mail.username}") String from) {
        this.mailSender = mailSender;
        this.from = from;
    }

    @Async
    public void sendReceived(Application app) {
        try {
            SimpleMailMessage msg = new SimpleMailMessage();
            msg.setFrom(from);
            msg.setTo(app.getEmail());
            msg.setSubject("Confirmation de réception de votre candidature — ONEP");

            msg.setText("Bonjour " + app.getFirstName() + " " + app.getLastName() + ",\n\n"
                    + "Nous accusons réception de votre candidature pour un stage au sein de l'Office National de l'Électricité et de l'Eau Potable (ONEP).\n\n"
                    + "Votre dossier est actuellement en cours d'examen par notre équipe des ressources humaines. Nous vous recontacterons dans les plus brefs délais pour vous informer de la suite réservée à votre candidature.\n\n"
                    + "Nous vous remercions de l'intérêt que vous portez à notre établissement et vous souhaitons bonne chance dans votre parcours.\n\n"
                    + "Cordialement,\n"
                    + "Service des Ressources Humaines\n"
                    + "ONEP — Office National de l'Électricité et de l'Eau Potable");

            mailSender.send(msg);
            log.info("Received email sent to {}", app.getEmail());
        } catch (Exception e) {
            log.warn("Failed to send received email to {}: {}", app.getEmail(), e.getMessage());
        }
    }

    @Async
    public void sendAccepted(Application app) {
        try {
            SimpleMailMessage msg = new SimpleMailMessage();
            msg.setFrom(from);
            msg.setTo(app.getEmail());
            msg.setSubject("Candidature acceptée — Félicitations — ONEP");

            msg.setText("Bonjour " + app.getFirstName() + " " + app.getLastName() + ",\n\n"
                    + "Nous avons le plaisir de vous informer que votre candidature pour un stage au sein de l'ONEP a été retenue.\n\n"
                    + "Félicitations ! Votre profil a été sélectionné parmi de nombreux candidats. Un membre de notre équipe vous contactera prochainement pour vous communiquer les détails relatifs à votre intégration et les prochaines étapes à suivre.\n\n"
                    + "Nous sommes ravis de vous accueillir et nous nous réjouissons de collaborer avec vous.\n\n"
                    + "Dans l'attente de vous rencontrer, veuillez agréer l'expression de nos salutations distinguées.\n\n"
                    + "Cordialement,\n"
                    + "Service des Ressources Humaines\n"
                    + "ONEP — Office National de l'Électricité et de l'Eau Potable");

            mailSender.send(msg);
            log.info("Acceptance email sent to {}", app.getEmail());
        } catch (Exception e) {
            log.warn("Failed to send acceptance email to {}: {}", app.getEmail(), e.getMessage());
        }
    }

    @Async
    public void sendRejected(Application app) {
        try {
            SimpleMailMessage msg = new SimpleMailMessage();
            msg.setFrom(from);
            msg.setTo(app.getEmail());
            msg.setSubject("Statut de votre candidature — ONEP");

            msg.setText("Bonjour " + app.getFirstName() + " " + app.getLastName() + ",\n\n"
                    + "Nous vous remercions d'avoir postulé pour un stage au sein de l'Office National de l'Électricité et de l'Eau Potable (ONEP).\n\n"
                    + "Après un examen attentif de votre dossier, nous regrettons de vous informer que votre candidature n'a pas été retenue pour cette session.\n\n"
                    + "Cette décision n'est en aucun cas un jugement de vos compétences. Le nombre important de candidatures reçues nous a contraints à faire des choix difficiles.\n\n"
                    + "Nous vous encourageons vivement à postuler à nouveau lors de nos prochaines campagnes de recrutement.\n\n"
                    + "Nous vous remercions de votre compréhension et vous souhaitons plein de succès dans vos projets futurs.\n\n"
                    + "Cordialement,\n"
                    + "Service des Ressources Humaines\n"
                    + "ONEP — Office National de l'Électricité et de l'Eau Potable");

            mailSender.send(msg);
            log.info("Rejection email sent to {}", app.getEmail());
        } catch (Exception e) {
            log.warn("Failed to send rejection email to {}: {}", app.getEmail(), e.getMessage());
        }
    }
}
