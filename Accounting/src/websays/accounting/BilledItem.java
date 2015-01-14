/*
 *    SaaSMetrics4J : https://github.com/hugozaragoza/SaaSMetrics4J
 *
 *    (c) 2014, Hugo Zaragoza, Websays.
 */
package websays.accounting;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import org.apache.log4j.Logger;

import websays.accounting.Contract.Currency;
import websays.core.utils.DateUtilsWebsays;

public class BilledItem {
  
  private static final Logger logger = Logger.getLogger(BilledItem.class);
  
  public BilledPeriod period;
  public Double fee;
  
  public String contract_name;
  public int contract_id;
  public ArrayList<String> notes = new ArrayList<String>(0);
  
  public CommissionItemSet commissions = new CommissionItemSet();
  private Contract.Currency currency = Currency.EUR;
  
  static final SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yy");
  
  /**
   * 
   * An item in a bill, a service billed (typically corresponds to a period of service billed)
   * 
   * @param billedPeriod
   * @param fee
   * @param contract_name
   * @param contract_id
   * @param contractedPeriods
   *          used to determine if a warning of last bill should be issued.
   */
  public BilledItem(BilledPeriod billedPeriod, Double fee, String contract_name, int contract_id, Currency currency) {
    super();
    period = billedPeriod;
    this.fee = fee;
    this.contract_name = contract_name;
    this.contract_id = contract_id;
    this.currency = currency;
  }
  
  public Date getDate() {
    return period.billDate;
  }
  
  public void warningChecks(Date billingDate, Contract c) {
    if (c == null) {
      logger.error("Null contract ???");
      return;
    }
    
    if (c.comments_billing != null && c.comments_billing.length() > 0) {
      notes.add("(BILLING_NOTE: " + c.comments_billing + ")");
    }
    int monthNumber = DateUtilsWebsays.getHowManyMonths(c.startContract, billingDate);
    
    if (c.endContract != null) {
      if (period.inPeriod(c.endContract)) {
        notes.add("\t\t\t! Last bill as agreed (end contract:" + sdf.format(c.endContract) + ")");
      } else {
        int months = DateUtilsWebsays.getHowManyMonths(billingDate, c.endContract);
        if (months >= 0 && months < 2) {
          notes.add("\t\t\t! Contract ending soon as agreed (end contract: " + sdf.format(c.endContract) + ")");
        }
      }
    } else {
      
      if (c.contractedMonths > 0) {
        if (monthNumber + 1 == c.contractedMonths) {
          notes.add("\t\t\t!!! ONE BEFORE LAST MONTH OF CONTRACT AGREED");
        } else if (monthNumber == c.contractedMonths) {
          notes.add("\t\t\t!!! LAST MONTH OF CONTRACT AGREED");
        } else if (monthNumber > c.contractedMonths) {
          // you should increment "contractedPeriods" field when this happens because auto-renewal
          notes.add("\t\t\t!!! PASSED LAST MONTH OF CONTRACT AGREED");
        }
      }
    }
    
  }
  
  public char getCurrencySymbol() {
    return currency.toChar();
  }
  
}
