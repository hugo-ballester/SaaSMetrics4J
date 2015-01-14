/*
 *    SAS4J
 *
 *    Hugo Zaragoza, Websays.
 */
package websays.accounting;

public class Commission {
  
  public String commissionnee;
  public double pct;
  public Double commission_base;
  
  /**
   * @param pct
   *          [0-1] commission
   * @param commission_base
   * @param commissionnee
   */
  public Commission(Double pct, Double commission_base, String commissionnee) {
    super();
    this.pct = pct;
    this.commissionnee = commissionnee;
    this.commission_base = commission_base;
  }
  
  public Commission() {};
  
  public double computeCommission(double fee) {
    return fee * pct;
  }
  
  public CommissionItem createCommissionItem(BilledItem bi) {
    
    return new CommissionItem(commissionnee, computeCommission(bi.fee), this, bi);
  }
  
}
