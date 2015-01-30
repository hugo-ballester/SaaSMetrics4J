/*
 *    SAS4J
 *
 *    Hugo Zaragoza, Websays.
 */
package websays.accounting;

import java.util.Currency;

/**
 * A Billed Item may have a CommissionItem associated indicated commission on this item
 * 
 * @author hugoz
 * 
 */
public class CommissionItem {
  
  Currency currency = GlobalConstants.EUR; // all commissions computed in euros for now
  String commissionnee = null;
  double commission;
  
  // back references in case they are needed
  transient Commission com;
  transient BilledItem bi;
  
  public CommissionItem(String commissionnee, double commission, Commission com, BilledItem bi) {
    super();
    this.commissionnee = commissionnee;
    this.commission = commission;
    this.com = com;
    this.bi = bi;
  }
  
}
