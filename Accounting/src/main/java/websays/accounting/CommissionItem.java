/*
 *    SAS4J
 *
 *    Hugo Zaragoza, Websays.
 */
package websays.accounting;

/**
 * A Billed Item may have a CommissionItem associated indicated commission on this item
 * 
 * For now all commissions are stored in euros regardless of the currency of the bills
 * 
 * @author hugoz
 * 
 */
public class CommissionItem {
  
  String commissionnee = null;
  Double commission = null;
  
  // back references in case they are needed
  CommissionPlan com;
  BilledItem bi;
  
  public CommissionItem(String commissionnee, double commission, CommissionPlan com, BilledItem bi) {
    super();
    this.commissionnee = commissionnee;
    this.commission = commission;
    this.com = com;
    this.bi = bi;
  }
  
}
