/*
 *    SAS4J
 *
 *    Hugo Zaragoza, Websays.
 */
package websays.accounting;

import org.apache.log4j.Logger;

import websays.accounting.connectors.ContractDAO;
import websays.core.utils.CurrencyUtils;
import websays.core.utils.JodaUtils;

/**
 * Defines a commission plan
 * 
 * @author hugoz
 *
 */
public class CommissionPlan {
  
  static Logger logger = Logger.getLogger(ContractDAO.class);
  
  /**
   * pct1 is used for months 1...commission_months, then pct2 is used forever
   */
  public double pct1, pct2;
  int commission_months;
  public String commissionnee;
  
  /**
   * if true, this commission applies to the full bill fee, ignoring the commission base fee
   * 
   * we need this to have plans where one salesperson gets the fee on the total and the second on the total-firstCommission
   */
  boolean commissionAppliesToFullFee = false;
  
  /**
   * @param pct1
   *          : % commission during the first commission_months
   * @param pct2
   *          : % commission after the first commission_months
   * @param commission_months
   * @param commission_base
   * @param commissionnee
   */
  public CommissionPlan(Double pct1, Double pct2, int commission_months, String commissionnee) {
    super();
    this.pct1 = pct1;
    this.pct2 = pct2;
    this.commissionnee = commissionnee;
    this.commission_months = commission_months;
  }
  
  public CommissionPlan() {};
  
  public double computeCommission(int month, double commissionBase) {
    double pct = (month <= commission_months ? pct1 : pct2);
    return commissionBase * pct;
  }
  
  public CommissionItem createCommissionItem(BilledItem bi) {
    int monthsFromStartOfContract = JodaUtils.monthsDifference(bi.period.contractStart, bi.period.billDate) + 1;
    double base = 0.0;
    if (commissionAppliesToFullFee) {
      base = bi.getFee();
    } else {
      base = bi.getCommissionBaseFee();
    }
    base = CurrencyUtils.toEuros(base, bi.getCurrency());
    
    double comm = computeCommission(monthsFromStartOfContract, base);
    return new CommissionItem(commissionnee, comm, this, bi);
  }
  
}
