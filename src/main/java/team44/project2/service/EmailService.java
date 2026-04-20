package team44.project2.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import io.quarkus.mailer.Mailer;
import io.quarkus.mailer.Mail;

@ApplicationScoped
public class EmailService {

    @Inject
    Mailer mailer;

    public void sendEmail() {

        Mail mail = Mail.withText(
                "lukeshull@tamu.edu",
                "Greetings from Boba Bob's",
                "This is a test of the poob warning system. No action is required."
        )
        .setFrom("Boba Bob's <bobabob@poob.store>");

        mailer.send(mail);
    }
}