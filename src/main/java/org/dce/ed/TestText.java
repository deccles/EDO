package org.dce.ed;

import org.dce.ed.notifications.TextNotificationSender;

import jakarta.mail.MessagingException;

public class TestText {

	public static void main(String[] args) {
		try {
			System.out.println(System.getProperty("edo.smtp.host"));
			
			TextNotificationSender.sendText(
			        OverlayPreferences.getTextNotificationAddress(),
			        "EDO",
			        "Hello World text test"
			);
		} catch (MessagingException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
	}
}
