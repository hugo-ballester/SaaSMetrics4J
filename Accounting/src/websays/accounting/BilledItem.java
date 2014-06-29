/*
 *    SaaSMetrics4J
 *
 *    (c) 2014, Hugo Zaragoza, Websays.
 */
package websays.accounting;

import java.util.ArrayList;
import java.util.Date;

import websays.core.utils.DateUtilsWebsays;

public class BilledItem {
  
  public BilledPeriod period;
  public Double fee;
  
  public String contract_name;
  public int contract_id;
  public ArrayList<String> notes = new ArrayList<String>(0);
  
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
  public BilledItem(BilledPeriod billedPeriod, Double fee, String contract_name, int contract_id) {
    super();
    period = billedPeriod;
    this.fee = fee;
    this.contract_name = contract_name;
    this.contract_id = contract_id;
  }
  
  public Date getDate() {
    return period.billDate;
  }
  
  public void warningChecks(Date billingDate, Contract c) {
    
    int monthNumber = DateUtilsWebsays.getHowManyMonths(c.startContract, billingDate);
    if (c.contractedMonths > 0) {
      if (monthNumber + 1 == c.contractedMonths) {
        notes.add("\t\t\t!!! ONE BEFORE LAST MONTH OF CONTRACT");
      } else if (monthNumber == c.contractedMonths) {
        notes.add("\t\t\t!!! LAST MONTH OF CONTRACT");
        
      } else if (monthNumber > c.contractedMonths) {
        // you should increment "contractedPeriods" field when this happens because auto-renewal
        notes.add("\t\t\t!!! PASSED LAST MONTH OF CONTRACT");
      }
    }
    
  }
  
}
