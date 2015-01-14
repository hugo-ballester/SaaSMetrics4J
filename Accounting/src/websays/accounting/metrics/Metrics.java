/*
 *    SAS4J
 *
 *    Hugo Zaragoza, Websays.
 */
package websays.accounting.metrics;

import java.util.ArrayList;
import java.util.Date;

import websays.accounting.Commission;
import websays.accounting.Contract;
import websays.accounting.Contract.Currency;
import websays.core.utils.DateUtilsWebsays;

public class Metrics {
  
  /**
   * 
   * Metrics does not use BillItems nor CommissionItems, and therefore must compute commissions on the fly here.
   * 
   * Commissions are compounded in order: firs is MRR*x1, then MRR*(1-x1)*x2, etc.
   * 
   * 
   * @param c
   * @param d
   * @param roundDate
   * @return
   */
  public static double computeCommission(Contract c, Date d, boolean roundDate) {
    ArrayList<Commission> coms = c.commission;
    if (coms == null || coms.size() == 0) {
      return 0;
    }
    
    double ret = 0.0;
    Commission comInit = coms.get(0);
    double mrrLeft = comInit.commission_base != null ? comInit.commission_base : computeMRR(c, d, roundDate);
    for (Commission com : coms) {
      double x = mrrLeft * com.pct;
      mrrLeft -= x;
      ret += x;
    }
    
    return ret;
  }
  
  /**
   * Returns monthly revenue from contract (at given date)
   * 
   * Converts all currencies to euros.
   * 
   * @param c
   * @param d
   * @param roundDate
   * @return
   */
  public static double computeMRR(Contract c, Date d, boolean roundDate) {
    if (d.before(c.startRoundDate)) {
      return 0.;
    }
    if (roundDate) {
      if (c.endRoundDate != null && c.endRoundDate.before(d)) {
        return 0;
      }
    } else {
      if (c.endContract != null && c.endContract.before(d)) {
        return 0;
      }
    }
    
    double p = 0.;
    
    p = c.getMonthlyPrize(d, true, true);
    
    if (c.currency != null && !c.currency.equals(Currency.EUR)) {
      p *= c.currency.exhangeRateToEU();
    }
    
    return p;
  }
  
  public static double mrrChange(Contract c, Date d, boolean roundDate) {
    Date newD = DateUtilsWebsays.dateEndOfMonth(d, 0);
    Date oldD = DateUtilsWebsays.dateEndOfMonth(d, -1);
    double change = computeMRR(c, newD, roundDate) - computeMRR(c, oldD, roundDate);
    return change;
  }
  
  public static double expansion(Contract c, Date d) {
    boolean roundDate = true;
    
    // if contract ended last month, return 0;
    Date prevMonth = DateUtilsWebsays.dateBeginningOfMonth(d, -1);
    if (c.isLastMonth(prevMonth, roundDate)) {
      return 0;
    }
    // if contract started this month, return 0;
    if (c.isFirstMonth(d, roundDate)) {
      return 0;
    } else {
      return mrrChange(c, d, roundDate);
    }
  }
}
