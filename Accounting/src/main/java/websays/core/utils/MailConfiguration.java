/**
 * Websays Opinion Analytics Engine
 *
 * (Websays Copyright © 2010-2014. All rights reserved. http://websays.com )
 *
 * Primary Author: Marco Martinez/Hugo Zaragoza
 * Contributors:
 * Date: Jul 7, 2014
 */
package websays.core.utils;

public class MailConfiguration {
  
  private String smtpHostName;
  
  private String smtpUser;
  
  private String smtpPassword;
  
  private Integer smtpPort;
  
  private Boolean useSSL = Boolean.FALSE;
  
  private Boolean useTSL = Boolean.FALSE;
  
  public String getSmtpHostName() {
    return smtpHostName;
  }
  
  public void setSmtpHostName(String smtpHostName) {
    this.smtpHostName = smtpHostName;
  }
  
  public String getSmtpUser() {
    return smtpUser;
  }
  
  public void setSmtpUser(String smtpUser) {
    this.smtpUser = smtpUser;
  }
  
  public String getSmtpPassword() {
    return smtpPassword;
  }
  
  public void setSmtpPassword(String smtpPassword) {
    this.smtpPassword = smtpPassword;
  }
  
  public Integer getSmtpPort() {
    return smtpPort;
  }
  
  public void setSmtpPort(Integer smtpPort) {
    this.smtpPort = smtpPort;
  }
  
  public Boolean getUseSSL() {
    return useSSL;
  }
  
  public void setUseSSL(Boolean useSSL) {
    this.useSSL = useSSL;
  }
  
  /**
   * @return the useTSL
   */
  public Boolean getUseTSL() {
    return useTSL;
  }
  
  /**
   * @param useTSL
   *          the useTSL to set
   */
  public void setUseTSL(Boolean useTSL) {
    this.useTSL = useTSL;
  }
  
}
