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

import websays.accounting.Contract.BillingSchema;
import websays.core.utils.DateUtilsWebsays;

/**
 * 
 * MONTH_X Billing:
 * <ul>
 * <li>Contracts can start on any date, must end the last day of a month.
 * <li>Billing is in advanced on 1st of the month
 * <li>On the very first bill, the days of the previous month since contract_start are added at a prop. prize
 * <li>Consequents bills are at months previousBillMonth+X
 * </ul>
 * 
 * @author hugoz
 * 
 */
public class Billing {
  
  private static final String error1 = "DONT KNOW HOW TO COMPUTE BILLING: ";
  
  static public void error(String s) {
    System.err.println(error1 + s);
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
    return bill(c, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH));
  }
  
  public static Bill bill(Contract c, int year, int month) {
    
    // Set billing date:
    Calendar cal = Calendar.getInstance();
    cal.set(Calendar.YEAR, year);
    cal.set(Calendar.MONTH, month - 1);
    cal.set(Calendar.DAY_OF_MONTH, 1);
    DateUtilsWebsays.calToStartOfDay(cal);
    Date d = cal.getTime();
    
    // integrity tests
    if (c == null) {
      error(" null contract.");
      return null;
    }
    if (c.startContract == null) {
      error(" null starting date for contract.");
      return null;
    }
    
    // if not active, return
    if (!c.isActiveBill(d)) {
      return null;
    }
    
    BillingSchema bs = c.billingSchema;
    Double monthly = null;
    // boolean isFirstMonth = false, isSameMonth = false, isLastMonth = false;
    
    boolean isFirstFullMonth = c.isFirstFullMonth(d, false);
    
    try {
      
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
      
      Date firstBillingDate = c.startContract;
      firstBillingDate = DateUtilsWebsays.getFirstDayOfNextMonth(firstBillingDate).getTime();
      
      double monthlyPrize = c.getMonthlyPrize(d, true);
      double monthlyPrizeNoFixed = c.getMonthlyPrize(d, false);
      double monthlyFixed = monthlyPrize - monthlyPrizeNoFixed;
      
      if (bs.isPeriodic()) {
        
        int n = c.billingSchema.getMonths();
        
        int months = DateUtilsWebsays.getHowManyMonths(firstBillingDate, d);
        if (months % n != 0) {
          return null;
        }
        
        // Standard Period Price:
        
        monthly = monthlyPrize * n;
        
        // Add remaining of first incomplete month. E.g. a contract starting on the 30th of January, would be billed on the 1st of February for the 2
        // days of January plus February.
        if (isFirstFullMonth && DateUtilsWebsays.getDayOfMonth(c.startContract) != 1) {
          Date endOfLastMonth = DateUtilsWebsays.dateBeginningOfDay(DateUtilsWebsays.dateEndOfMonth(d, -1));
          int firstMonthDays = DateUtilsWebsays.getHowManyDays(c.startContract, endOfLastMonth) + 1;
          double firstMonth = monthlyFixed + monthlyPrizeNoFixed / 30. * firstMonthDays;
          monthly += firstMonth;
        }
        
      }
      
      else if (bs == BillingSchema.FULL_1) {
        if (c.monthlyPrice != null && c.monthlyPrice > 0.) {
          error(" monthly prized defined for FULL_1 billing schema");
        }
        
        if (isFirstFullMonth) {
          monthly = c.fixedPrice;
        } else {
          monthly = null;
        }
        
      } else {
        System.out.println("UNKNOWN BillingSchema '" + bs.name() + "'");
        return null;
      }
      
    } catch (Exception e) {
      e.printStackTrace();
      return null;
    }
    
    if (monthly == null) {
      return null;
    } else {
      return new Bill(d, c.client_name, c.name, monthly);
    }
    
  }
  
  public static ArrayList<Bill> bill(Contracts cs, int year, int month) {
    if (cs == null) {
      System.err.println("ERROR: NULL contracts");
      return null;
    }
    TreeMap<String,Bill> ret = new TreeMap<String,Bill>();
    for (Contract c : cs) {
      Bill b = Billing.bill(c, year, month);
      if (b != null) {
        if (!ret.containsKey(c.client_name)) {
          ret.put(c.client_name, new Bill(b.date, c.client_name));
        }
        ret.get(c.client_name).addBill(b);
      }
    }
    
    return new ArrayList<Bill>(ret.values());
  }
  
}
