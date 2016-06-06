/*
 *    SAS4J
 *
 *    Hugo Zaragoza, Websays.
 */
package websays.accounting.metrics;

import org.joda.time.LocalDate;

import websays.accounting.Contract;
import websays.core.utils.CurrencyUtils;
import websays.core.utils.JodaUtils;

public class Metrics {
  
  // /**
  // *
  // * Metrics does not use BillItems nor CommissionItems, and therefore must compute commissions on the fly here.
  // *
  // * Commissions are compounded in order: first is MRR*x1, then MRR*(1-x1)*x2, etc.
  // *
  // *
  // * @param c
  // * @param d
  // * @param roundDate
  // * @return
  // */
  // public static double computeCommission(Contract c, LocalDate d, boolean roundDate) {
  // ArrayList<CommissionPlan> coms = c.commission;
  // if (coms == null || coms.size() == 0) {
  // return 0;
  // }
  //
  // double ret = 0.0;
  // CommissionPlan comInit = coms.get(0);
  // // double mrrLeft = comInit.commission_base != null ? comInit.commission_base : computeMRR(c, d, roundDate);
  // int monthsFromStartOfContract = JodaUtils.monthsDifference(c.startContract, d) + 1;
  // for (CommissionPlan com : coms) {
  // // double comm = com.computeCommission(mrrLeft, monthsFromStartOfContract);
  // double comm = com.computeCommission(monthsFromStartOfContract);
  // // mrrLeft -= comm;
  // ret += comm;
  // }
  //
  // return ret;
  // }
  
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
  public static double computeMRR(Contract c, LocalDate d, boolean roundDate) {
    
    if (roundDate) {
      if (d.isBefore(c.startRoundDate)) {
        return 0.;
      }
      if (c.endRoundDate != null && c.endRoundDate.isBefore(d)) {
        return 0;
      }
    } else {
      if (d.isBefore(c.startContract)) {
        return 0.;
      }
      if (c.endContract != null && c.endContract.isBefore(d)) {
        return 0;
      }
    }
    
    double p[] = c.getMonthlyPrize(d, true, true);
    return CurrencyUtils.toEuros(p[0], c.currency);
    
  }
  
  public static double mrrChange(Contract c, LocalDate d, boolean roundDate) {
    LocalDate newD = JodaUtils.dateEndOfMonth(d, 0);
    LocalDate oldD = JodaUtils.dateEndOfMonth(d, -1);
    double change = computeMRR(c, newD, roundDate) - computeMRR(c, oldD, roundDate);
    return change;
  }
  
  public static double expansion(Contract c, LocalDate d) {
    boolean roundDate = true;
    
    // if contract ended last month, return 0;
    LocalDate prevMonth = d.withDayOfMonth(1).minusDays(1);
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
