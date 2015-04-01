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

import javax.mail.PasswordAuthentication;



/**
* SimpleAuthenticator is used to do simple authentication
* when the SMTP server requires it.
*/
public class SMTPAuthenticator extends javax.mail.Authenticator {
	
	private String smtpUser;
	
	private String smtpPassword;
	
	public SMTPAuthenticator(String user,String password) {
		this.smtpUser = user;
		this.smtpPassword = password;
	}

    public PasswordAuthentication getPasswordAuthentication()   {
        String username = this.smtpUser;
        String password = this.smtpPassword;
        return new PasswordAuthentication(username, password);
    }
}

