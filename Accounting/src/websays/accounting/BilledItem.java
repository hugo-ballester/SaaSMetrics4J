/*
 *    SAS4J
 *
 *    Hugo Zaragoza, Websays.
 */
package websays.accounting;

public class BilledItem {
  
  public BilledPeriod period;
  public Double fee;
  public String contract_name;
  public int contract_id;
  
  public BilledItem(BilledPeriod billedPeriod, Double fee, String contract_name, int contract_id) {
    super();
    this.period = billedPeriod;
    this.fee = fee;
    this.contract_name = contract_name;
    this.contract_id = contract_id;
  }
  
}
