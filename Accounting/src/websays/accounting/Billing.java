/*
 *    SAS4J
 *
 *    Hugo Zaragoza, Websays.
 */
package websays.accounting;

import java.util.ArrayList;
import java.util.Date;
import java.util.TreeMap;

import websays.accounting.Contract.BillingSchema;
import websays.core.utils.DateUtilsWebsays;

/**
 * 
 * Assumes one bill per month on the date d.
 * 
 * Typically bill includes services on that date.
 * 
 * For example, is project starts on the 14/1 on a fixed prize of 1000 for 2 months, billing will be 1000 for any date 14-31 of month 1, 0 afterwards
 * until last month of proj, null afterwards.
 * 
 * First month is billed only for days used.
 * 
 * @author hugoz
 * 
 */
public class Billing {
  
  private static final String error1 = "DONT KNOW HOW TO COMPUTE BILLING: ";
  
  public static Bill bill(Contract c, Date d) {
    if (c == null) {
      System.err.println(error1 + " null contract.");
      return null;
    }
    if (c.startContract == null) {
      System.err.println(error1 + " null starting date for contract.");
      return null;
    }
    if (d == null) {
      System.err.println(error1 + " null date.");
      return null;
    }
    
    if (!c.isActiveBill(d))
      return null;
    
    BillingSchema bs = c.billingSchema;
    Double monthly = null;
    boolean isFirstMonth = false, isSameMonth = false, isLastMonth = false;
    int firstMonthDays = 0;
    
    try {
      
      double firstMonth = 0;
      
      // First month of contract
      if (c.isFirstMonth(d, false)) {
        // First Bill of Service: bill number of days
        isFirstMonth = true;
        Date endOfMonth = DateUtilsWebsays.dateBeginningOfDay(DateUtilsWebsays.dateEndOfMonth(d));
        firstMonthDays = DateUtilsWebsays.getHowManyDays(c.startContract, endOfMonth) + 1;
        firstMonth = c.getMonthlyPrize(d) / 30. * firstMonthDays;
        if (c.fixPrize != null) {
          firstMonth += c.fixPrize;
        }
      }
      
      // First month of successive same month
      if (DateUtilsWebsays.getMonth(d) == DateUtilsWebsays.getMonth(c.startContract)) {
        isSameMonth = true;
      }
      
      // LastMonth of contract
      if (c.isLastMonth(d, false)) {
        isLastMonth = true;
      }
      
      if (bs == BillingSchema.fullYear) {
        if (isSameMonth && !isLastMonth) {
          if (c.fixPrize != null && c.fixPrize > 0) {
            System.err.println(error1 + " Cant deal with fullYear and FixPrize");
          }
          monthly = 12. * c.getMonthlyPrize(d);
        } else
          return null;
      }
      
      else if (bs == BillingSchema.fullMonth) {
        if (isFirstMonth) {
          monthly = firstMonth;
        } else {
          monthly = c.getMonthlyPrize(d);
        }
        
      } else if (bs == BillingSchema.fullFirstMonth) {
        if (c.getMonthlyPrize(d) > 0.) {
          System.err.println(error1 + "has MRR but is " + bs.name());
        }
        if (isFirstMonth) {
          monthly = c.fixPrize;
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
    if (monthly == null)
      return null;
    else
      return new Bill(d, c.client_name, c.name, monthly);
  }
  
  public static ArrayList<Bill> bill(Contracts cs, Date d) {
    TreeMap<String,Bill> ret = new TreeMap<String,Bill>();
    for (Contract c : cs) {
      Bill b = Billing.bill(c, d);
      if (b != null) {
        if (!ret.containsKey(c.client_name)) {
          ret.put(c.client_name, new Bill(d, c.client_name));
        }
        ret.get(c.client_name).addBill(b);
      }
    }
    
    return new ArrayList<Bill>(ret.values());
  }
  
}
