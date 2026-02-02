package org.dce.ed.notifications;

import java.util.List;
import java.util.Properties;

import jakarta.mail.Authenticator;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;

public class TextNotificationSender {

    private TextNotificationSender() {
        // util
    }

    /**
     * Sends a text via an email-to-SMS gateway address (or any email address).
     *
     * SMTP config is read from system properties:
     *   - edo.smtp.host
     *   - edo.smtp.port
     *   - edo.smtp.username
     *   - edo.smtp.password
     *   - edo.smtp.from
     *   - edo.smtp.starttls (true/false, default true)
     *   - edo.smtp.ssl (true/false, default false)
     */
    public static void sendText(List<String> toAddress, String subject, String body) throws MessagingException {
    	for (String name : toAddress) {
    		System.out.println("Texting " + name + ": (" + subject + ") " +body);
    		sendText(name.trim(), subject,body);
    		
            try {
                Thread.sleep(1500);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
            }
    	}
    }
    private static void sendText(String toAddress, String subject, String body) throws MessagingException {

        if (toAddress == null || toAddress.isBlank()) {
            return;
        }
        
        String host = require("edo.smtp.host");
        int port = Integer.parseInt(require("edo.smtp.port"));
        String username = require("edo.smtp.username");
        String password = require("edo.smtp.password");
        String from = require("edo.smtp.from");
        boolean startTls = Boolean.parseBoolean(System.getProperty("edo.smtp.starttls", "true"));
        boolean ssl = Boolean.parseBoolean(System.getProperty("edo.smtp.ssl", "false"));

        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.host", host);
        props.put("mail.smtp.port", String.valueOf(port));

        if (startTls) {
            props.put("mail.smtp.starttls.enable", "true");
        }
        if (ssl) {
            props.put("mail.smtp.ssl.enable", "true");
        }

        Session session = Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(username, password);
            }
        });

        MimeMessage msg = new MimeMessage(session);
        msg.setFrom(new InternetAddress(from));
        msg.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toAddress.trim(), false));
        msg.setSubject(subject, "UTF-8");
        msg.setText(body, "UTF-8");

        Transport.send(msg);
    }

    private static String require(String key) {
        String v = System.getProperty(key);
        if (v == null || v.trim().isEmpty()) {
            throw new IllegalStateException("Missing required system property: " + key);
        }
        return v.trim();
    }
}
