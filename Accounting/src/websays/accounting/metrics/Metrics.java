/*
 *    SAS4J
 *
 *    Hugo Zaragoza, Websays.
 */
package websays.accounting.metrics;

import java.util.Date;

import websays.accounting.Contract;
import websays.accounting.Contract.Currency;
import websays.core.utils.DateUtilsWebsays;

public class Metrics {
  
  public static double getCommission(Contract c, Date d, boolean roundDate) {
    if (c.commission == null || c.commission == 0) {
      return 0.;
    } else if (c.commissionMonthlyBase != null) {
      return c.commission * c.commissionMonthlyBase;
    } else {
      return c.commission * getMRR(c, d, roundDate);
    }
  }
  
  public static double getMRR(Contract c, Date d, boolean roundDate) {
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
    
    p = c.getMonthlyPrize(d, true);
    
    if (c.currency != null && !c.currency.equals(Currency.EUR)) {
      p *= c.currency.exhangeRateToEU();
    }
    
    return p;
  }
  
  public static double mrrChange(Contract c, Date d, boolean roundDate) {
    Date newD = DateUtilsWebsays.dateEndOfMonth(d, 0);
    Date oldD = DateUtilsWebsays.dateEndOfMonth(d, -1);
    double change = getMRR(c, newD, roundDate) - getMRR(c, oldD, roundDate);
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
