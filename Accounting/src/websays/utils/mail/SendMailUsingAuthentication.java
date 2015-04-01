/**
 * Websays Opinion Analytics Engine
 *
 * (Websays Copyright © 2010-2014. All rights reserved. http://websays.com )
 *
 * Primary Author: Marco Martinez/Hugo Zaragoza
 * Contributors:
 * Date: Jul 7, 2014
 */
package websays.utils.mail;

/*
 Some SMTP servers require a username and password authentication before you
 can use their Server for Sending mail. This is most common with couple
 of ISP's who provide SMTP Address to Send Mail.

 This Program gives any example on how to do SMTP Authentication
 (User and Password verification)

 */

import java.io.File;
import java.io.IOException;
import java.util.Properties;

import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

public class SendMailUsingAuthentication {
  
  private static final String DEFAULT_MIME_TYPE = "text/html; charset=utf-8";
  private MailConfiguration configuration;
  
  /**
   * 
   * WARNING: DO NOT ADD javax DIRECTLY TO POM, INSTEAD ADD om.sun.mail mailapi and smtp
   * 
   * 
   * @param configuration
   */
  public SendMailUsingAuthentication(MailConfiguration configuration) {
    this.configuration = configuration;
  }
  
  public void postSimpleMail(String recipients[], String subject, String message, String from) throws MessagingException {
    postSimpleMail(recipients, subject, message, from, null);
  }
  
  public void postSimpleMail(String recipients[], String subject, String message, String from, String replyTo) throws MessagingException {
    InternetAddress addressFrom = new InternetAddress(from);
    postSimpleMail(recipients, subject, message, addressFrom, replyTo);
  }
  
  public void postSimpleMail(String recipients[], String subject, String message, InternetAddress addressFrom, String replyTo)
      throws MessagingException {
    boolean debug = false;
    
    // create a message
    Message msg = createMessage(debug);
    // set the from and to address
    // InternetAddress addressFrom = new InternetAddress(from);
    msg.setFrom(addressFrom);
    
    InternetAddress[] addressTo = new InternetAddress[recipients.length];
    for (int i = 0; i < recipients.length; i++) {
      addressTo[i] = new InternetAddress(recipients[i]);
    }
    msg.setRecipients(Message.RecipientType.TO, addressTo);
    
    // reply to
    if (replyTo != null) {
      InternetAddress[] addressReplyTo = new InternetAddress[1];
      addressReplyTo[0] = new InternetAddress(replyTo);
      msg.setReplyTo(addressReplyTo);
    }
    
    // Setting the Subject and Content Type
    msg.setSubject(subject);
    msg.setContent(message, "text/plain; charset=\"UTF-8\"");
    Transport.send(msg);
  }
  
  private Message createMessage(boolean debug) {
    // Set the host smtp address
    Properties props = createMailProps();
    Authenticator auth = new SMTPAuthenticator(configuration.getSmtpUser(), configuration.getSmtpPassword());
    Session session = Session.getDefaultInstance(props, auth);
    session.setDebug(debug);
    Message msg = new MimeMessage(session);
    return msg;
  }
  
  public void postMail(String recipients[], String subject, String message, String from, File file) throws MessagingException, IOException {
    File[] files = {file};
    postMail(recipients, subject, message, from, files, DEFAULT_MIME_TYPE, null);
  }
  
  public void postMail(String recipients[], String subject, String message, String from, File file, String replyTo)
      throws MessagingException, IOException {
    File[] files = {file};
    postMail(recipients, subject, message, from, files, DEFAULT_MIME_TYPE, replyTo);
  }
  
  /**
   * @param recipients
   * @param subject
   * @param message
   * @param from
   * @param file
   * @param mimeType
   *          (example for utf-8 html: "text/html; charset=utf-8")
   * @param replyTo
   * @throws MessagingException
   * @throws IOException
   */
  public void postMail(String recipients[], String subject, String message, String from, File file, String mimeType, String replyTo)
      throws MessagingException, IOException {
    File[] files = {file};
    postMail(recipients, subject, message, from, files, mimeType, replyTo);
  }
  
  public void postMail(String recipients[], String subject, String message, String from, File[] files, String mimeType, String replyTo)
      throws MessagingException, IOException {
    InternetAddress addressFrom = new InternetAddress(from);
    postMail(recipients, subject, message, addressFrom, files, mimeType, replyTo);
  }
  
  public void postMail(String recipients[], String subject, String message, InternetAddress addressFrom, File[] files, String mimeType,
      String replyTo) throws MessagingException, IOException {
    boolean debug = false;
    
    // create a message
    Message msg = createMessage(debug);
    
    // set the from and to address
    msg.setFrom(addressFrom);
    
    InternetAddress[] addressTo = new InternetAddress[recipients.length];
    for (int i = 0; i < recipients.length; i++) {
      addressTo[i] = new InternetAddress(recipients[i]);
    }
    msg.setRecipients(Message.RecipientType.TO, addressTo);
    
    // reply to
    if (replyTo != null) {
      InternetAddress[] addressReplyTo = new InternetAddress[1];
      addressReplyTo[0] = new InternetAddress(replyTo);
      msg.setReplyTo(addressReplyTo);
    }
    
    // subject
    msg.setSubject(subject);
    
    // create and fill the first message part
    MimeBodyPart mbp1 = new MimeBodyPart();
    mbp1.setContent(message, mimeType);
    Multipart mp = new MimeMultipart();
    mp.addBodyPart(mbp1);
    
    // attached files
    for (File file : files) {
      // create the second message part
      // create the Multipart and add its parts to it
      MimeBodyPart mbp2 = new MimeBodyPart();
      // attach the file to the message
      mbp2.attachFile(file);
      mp.addBodyPart(mbp2);
    }
    
    // add the Multipart to the message
    msg.setContent(mp);
    
    Transport.send(msg);
    
  }
  
  private Properties createMailProps() {
    // Set the host smtp address
    Properties props = new Properties();
    props.put("mail.smtp.host", configuration.getSmtpHostName());
    props.put("mail.smtp.auth", "true");
    
    if (configuration.getUseTSL()) {
      props.put("mail.smtp.starttls.required", "true");
    }
    if (configuration.getUseSSL()) {
      props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
    }
    if (configuration.getSmtpPort() != null) {
      props.put("mail.smtp.socketFactory.port", configuration.getSmtpPort() + "");
    }
    return props;
  }
  
}
