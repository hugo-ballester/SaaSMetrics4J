/*
 *    SaaSMetrics4J : https://github.com/hugozaragoza/SaaSMetrics4J
 *
 *    (c) 2014, Hugo Zaragoza, Websays.
 */
package websays.accounting;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Currency;
import java.util.Date;
import java.util.TimeZone;

import org.apache.log4j.Logger;

import websays.accounting.Contract.ContractDocument;
import websays.core.utils.DateUtilsWebsays;

public class BilledItem {
  
  static final String AUTORENEWED = "AUTORENEWED WITHOUT CLIENT AGREEMENT?";
  static final String WILL_RENEW_NEXT_MONTH = "WILL RENEW NEXT MONTH";
  static final String WILL_RENEW_2MONTHS = "TWO MONTHS TO AUTORENEW";
  
  private static final Logger logger = Logger.getLogger(BilledItem.class);
  
  public BilledPeriod period;
  public Double fee;
  
  public String contract_name;
  public int contract_id;
  public ArrayList<String> notes = new ArrayList<String>(0);
  
  public CommissionItemSet commissions = new CommissionItemSet();
  private Currency currency;
  
  static final SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yy");
  private static final TimeZone tz = TimeZone.getDefault();
  
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
  
  public BilledItem(BilledItem bi) throws Exception {
    this(new BilledPeriod(bi.period), new Double(bi.fee), bi.contract_name, bi.contract_id, bi.currency);
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
      notes.add(c.comments_billing);
    }
    
    if (c.contractDocument.equals(ContractDocument.missing)) {
      notes.add("!!!MISSING CONTRACT");
    }
    
    int monthNumber1 = DateUtilsWebsays.getHowManyMonths(c.startContract, billingDate, tz);
    
    if (c.endContract != null) {
      if (period.inPeriod(c.endContract)) {
        notes.add("Last bill as agreed (end contract:" + sdf.format(c.endContract) + ")");
      } else {
        int months = DateUtilsWebsays.getHowManyMonths(billingDate, c.endContract, tz);
        if (months < 0) {
          notes.add("WARNING contract has ended! (end contract: " + sdf.format(c.endContract) + ")");
        }
        if (months >= 0 && months < 2) {
          notes.add("Contract ending soon as agreed (end contract: " + sdf.format(c.endContract) + ")");
        }
      }
    } else if (c.contractedMonths != null) {
      Date end = DateUtilsWebsays.addMonthsAndDays(c.startContract, c.contractedMonths, -1, tz);
      int left = DateUtilsWebsays.getHowManyMonths(billingDate, end, tz);
      if (left == 1) {
        notes.add(WILL_RENEW_2MONTHS);
      } else if (left == 0) {
        notes.add(WILL_RENEW_NEXT_MONTH);
      } else if (left < 0) {
        notes.add(AUTORENEWED);
      }
    }
    
  }
  
  public Currency getCurrency() {
    return currency;
  }
  
}
