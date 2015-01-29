/*
 *    SAS4J
 *
 *    Hugo Zaragoza, Websays.
 */
package websays.accounting;

import java.util.Date;

import org.apache.log4j.Logger;

import websays.accounting.connectors.ContractDAO;
import websays.core.utils.DateUtilsWebsays;

public class Commission {
  
  static Logger logger = Logger.getLogger(ContractDAO.class);
  
  int commission_months;
  public String commissionnee;
  public double pct;
  public Double commission_base;
  
  /**
   * @param pct
   *          [0-1] commission
   * @param commission_base
   * @param commissionnee
   */
  public Commission(Double pct, Double commission_base, int commission_months, String commissionnee) {
    super();
    this.pct = pct;
    this.commissionnee = commissionnee;
    this.commission_base = commission_base;
    this.commission_months = commission_months;
  }
  
  public Commission() {};
  
  public double computeCommission(double fee, int month) {
    double commBase = fee * pct;
    if (month <= commission_months) {
      commBase *= GlobalConstants.COMMMISSION_REMAINING;
    }
    return commBase;
    
  }
  
  public CommissionItem createCommissionItem(BilledItem bi) {
    Date contractStart = bi.period.contractStart;
    Date billDate = bi.getDate();
    int months = DateUtilsWebsays.getHowManyMonths(contractStart, billDate, null);
    double fee = 0;
    fee = computeCommission(bi.fee, months);
    
    return new CommissionItem(commissionnee, fee, this, bi);
  }
  
  public static Commission commissionFromSchema(String schema, Double commission_base, String commissionnee) {
    Double pct = null;
    int commission_months = 0;
    if (schema == null) {
      return null;
    } else if (schema.startsWith("C_")) {
      Integer i = Integer.parseInt(schema.substring(2));
      pct = 1.0 * i / 100.0;
      commission_months = GlobalConstants.COMMMISSION_MONTHS;
    } else {
      logger.error("ERROR: unknown commission type");
      return null;
    }
    return new Commission(pct, commission_base, commission_months, commissionnee);
  }
  
}
