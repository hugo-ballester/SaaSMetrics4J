/*
 *    SAS4J
 *
 *    Hugo Zaragoza, Websays.
 */
package websays.accounting;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.TreeMap;

import org.apache.log4j.Logger;

import websays.accounting.Contract.BillingSchema;
import websays.core.utils.DateUtilsWebsays;

/**
 * 
 * @author hugoz
 * 
 */
public class Billing {
  
  private static final String error1 = "DONT KNOW HOW TO COMPUTE BILLING: ";
  private static final Logger logger = Logger.getLogger(Billing.class);
  
  static public void error(String s) {
    logger.error(error1 + s);
  }
  
  /**
   * WARNING: this ignores the date (replaced by billing date of the month), only cares about year and month
   * 
   * @param c
   * @param d
   * @return
   */
  public static Bill bill(Contract c, Date d) {
    Calendar cal = Calendar.getInstance();
    cal.setTime(d);
    return bill(c, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1);
  }
  
  public static Calendar getBillingDate(int year, int month) {
    Calendar billingDate = DateUtilsWebsays.getCalendar(year, month, BilledPeriod.billingDayOfMonth);
    return billingDate;
  }
  
  public static Bill bill(Contract c, int year, int month) {
    try {
      
      // integrity tests
      if (c == null) {
        error(" null contract.");
        return null;
      }
      
      Date billingDate = getBillingDate(year, month).getTime();
      
      if (!c.isActiveBill(billingDate)) {
        logger.trace("Contract not active: " + c.name);
        return null;
      }
      logger.trace("Contract ACTIVE: " + c.name);
      
      BilledPeriod bp = c.getFirstBilledPeriod();
      boolean ok = bp.moveForwardTo(billingDate);
      if (!ok) {
        return null;
      }
      if (!DateUtilsWebsays.isSameDay(billingDate, bp.billDate)) {
        return new Bill(bp.billDate, c.client_name, c.name, 0.0, bp);
      }
      
      BillingSchema bs = c.billingSchema;
      Double monthly = null;
      
      // boolean isFirstFullMonth = c.isFirstFullMonth(bd, false);
      // double firstMonth = 0;
      // // First month of contract
      // if (c.isFirstMonth(d, false)) {
      // // First Bill of Service: bill number of days
      // isFirstMonth = true;
      // Date endOfMonth = DateUtilsWebsays.dateBeginningOfDay(DateUtilsWebsays.dateEndOfMonth(d));
      // firstMonthDays = DateUtilsWebsays.getHowManyDays(c.startContract, endOfMonth) + 1;
      // firstMonth = c.getMonthlyPrize(d) / 30. * firstMonthDays;
      // if (c.fixedPrice != null) {
      // firstMonth += c.fixedPrice;
      // }
      // }
      //
      // // First month of successive same month
      // if (DateUtilsWebsays.getMonth(d) == DateUtilsWebsays.getMonth(c.startContract)) {
      // isSameMonth = true;
      // }
      //
      // // LastMonth of contract
      // if (c.isLastMonth(d, false)) {
      // isLastMonth = true;
      // }
      
      // Date firstBillingDate = c.startContract;
      // firstBillingDate = DateUtilsWebsays.getFirstDayOfNextMonth(firstBillingDate).getTime();
      
      double monthlyPrize = c.getMonthlyPrize(billingDate, true);
      double monthlyPrizeNoFixed = c.getMonthlyPrize(billingDate, false);
      double monthlyFixed = monthlyPrize - monthlyPrizeNoFixed;
      
      if (bs.isPeriodic()) {
        
        int n = c.billingSchema.getMonths();
        monthly = monthlyPrize * n;
        
        // // Add remaining of first incomplete month. E.g. a contract starting on the 30th of January, would be billed on the 1st of February for the
        // 2
        // // days of January plus February.
        // if (isFirstFullMonth && DateUtilsWebsays.getDayOfMonth(c.startContract) != 1) {
        // Date endOfLastMonth = DateUtilsWebsays.dateBeginningOfDay(DateUtilsWebsays.dateEndOfMonth(d, -1));
        // int firstMonthDays = DateUtilsWebsays.getHowManyDays(c.startContract, endOfLastMonth) + 1;
        // double firstMonth = monthlyFixed + monthlyPrizeNoFixed / 30. * firstMonthDays;
        // monthly += firstMonth;
        // }
        
      }
      
      else if (bs == BillingSchema.FULL_1) {
        if (c.monthlyPrice != null && c.monthlyPrice > 0.) {
          error(" monthly prized defined for FULL_1 billing schema");
        }
        
        if (bp.period == 1) {
          monthly = c.fixedPrice;
        } else {
          monthly = null;
        }
        
      } else {
        System.out.println("UNKNOWN BillingSchema '" + bs.name() + "'");
        return null;
      }
      
      return new Bill(bp.billDate, c.client_name, c.name, monthly, bp);
      
    } catch (Exception e) {
      e.printStackTrace();
      return null;
    }
    
  }
  
  public static ArrayList<Bill> bill(Contracts cs, int year, int month) {
    if (cs == null) {
      logger.error("ERROR: NULL contracts");
      return null;
    }
    TreeMap<String,Bill> ret = new TreeMap<String,Bill>();
    for (Contract c : cs) {
      Bill b = Billing.bill(c, year, month);
      if (b != null) {
        if (!ret.containsKey(c.client_name)) {
          ret.put(c.client_name, new Bill(b.date, c.client_name));
        }
        ret.get(c.client_name).mergeBill(b);
      }
    }
    
    return new ArrayList<Bill>(ret.values());
  }
  
}
