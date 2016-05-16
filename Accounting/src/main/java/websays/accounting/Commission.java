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

public class Commission {
  
  static Logger logger = Logger.getLogger(ContractDAO.class);
  
  int commission_months;
  public String commissionnee;
  public double pct1, pct2;
  public Double commission_base;
  
  /**
   * @param pct1
   *          : % commission during the first commission_months
   * @param pct2
   *          : % commission after the first commission_months
   * @param commission_months
   * @param commission_base
   * @param commissionnee
   */
  public Commission(Double pct1, Double pct2, int commission_months, Double commission_base, String commissionnee) {
    super();
    this.pct1 = pct1;
    this.pct2 = pct2;
    this.commissionnee = commissionnee;
    this.commission_base = commission_base;
    this.commission_months = commission_months;
  }
  
  public Commission() {};
  
  public double computeCommission(double fee, int month) {
    double pct = (month <= commission_months ? pct1 : pct2);
    double commBase = 0;
    if (commission_base != null) {
      commBase = commission_base * pct;
    } else {
      commBase = fee * pct;
    }
    return commBase;
    
  }
  
  public CommissionItem createCommissionItem(BilledItem bi) {
    double fee = CurrencyUtils.toEuros(bi.getFee(), bi.getCurrency());
    int monthsFromStartOfContract = JodaUtils.monthsDifference(bi.period.contractStart, bi.period.billDate) + 1;
    fee = computeCommission(fee, monthsFromStartOfContract);
    return new CommissionItem(commissionnee, fee, this, bi);
  }
  
}
