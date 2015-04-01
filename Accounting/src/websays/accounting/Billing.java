/*
 *    SaaSMetrics4J : https://github.com/hugozaragoza/SaaSMetrics4J
 *
 *    (c) 2014, Hugo Zaragoza, Websays.
 */
package websays.accounting;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.TreeMap;

import org.apache.log4j.Logger;

import websays.accounting.Contract.BillingSchema;
import websays.core.utils.TimeWebsays;

/**
 * 
 * @author hugoz
 * 
 */
public class Billing {
  
  private static final String error1 = "DONT KNOW HOW TO COMPUTE BILLING: ";
  private static final Logger logger = Logger.getLogger(Billing.class);
  private static final TimeWebsays calendar = new TimeWebsays(Locale.getDefault(), TimeZone.getDefault());
  
  static public void error(String s) {
    logger.error(error1 + s);
  }
  
  /**
   * @param year
   * @param month
   *          1-12
   * @return
   */
  public static Calendar getBillingDate(int year, int month) {
    Calendar billingDate = calendar.getCalendar(year, month, BilledPeriod.billingDayOfMonth);
    return billingDate;
  }
  
  /**
   * The actual date depends on BilledPeriod.billingDayOfMonth
   * 
   * 
   * @param c
   * @param year
   * @param month_firstIs1
   *          1-12
   * @return
   */
  public static BilledItem bill(Contract c, int year, int month_firstIs1) {
    try {
      
      // integrity tests
      if (c == null) {
        error(" null contract.");
        return null;
      }
      
      Date billingDate = getBillingDate(year, month_firstIs1).getTime();
      
      if (!c.isActiveBill(billingDate)) {
        logger.trace("Contract not active: " + c.name);
        return null;
      }
      logger.debug("Contract ACTIVE: " + c.name);
      
      BilledPeriod bp = c.getFirstBilledPeriod();
      boolean ok = bp.moveForwardTo(billingDate);
      if (!ok) {
        return null;
      }
      
      BilledItem bi = new BilledItem(bp, 0.0, c.name, c.id, c.currency);
      
      if (!calendar.isSameDay(billingDate, bp.billDate)) {
        // NO BILLING NEEDED, SET PRIZE TO 0.0 TO INDICATE THIS. TODO use boolean field
        bi.setFee(0.0, c.currency);
      } else {
        
        // COMPUTE FEE
        BillingSchema bs = c.billingSchema;
        Double monthly = null;
        
        double monthlyPrize = c.getMonthlyPrize(billingDate, true, false);
        
        if (bs.isPeriodic()) {
          int n = c.billingSchema.getMonths();
          monthly = monthlyPrize * n;
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
        
        bi.setFee(monthly, c.currency);
      }
      if (bi != null) {
        bi.warningChecks(billingDate, c);
      }
      
      if (c.commission != null) {
        for (Commission com : c.commission) {
          if (com == null) {
            logger.error("ERROR null Commission for contract " + c.name + " (#" + c.id + ")");
            continue;
          }
          
          CommissionItem ci = com.createCommissionItem(bi);
          bi.commissions.add(ci);
        }
      }
      
      return bi;
      
    } catch (Exception e) {
      e.printStackTrace();
      return null;
    }
    
  }
  
  /**
   * @param cs
   * @param year
   * @param month_firstIs1
   *          1-12
   * @return
   */
  public static ArrayList<Bill> bill(Contracts cs, int year, int month_firstIs1) {
    if (cs == null) {
      logger.error("ERROR: NULL contracts");
      return null;
    }
    
    TreeMap<String,Bill> ret = new TreeMap<String,Bill>();
    
    for (Contract c : cs) {
      BilledItem bi = Billing.bill(c, year, month_firstIs1);
      if (bi == null) {
        continue;
      }
      
      if (!ret.containsKey(c.client_name)) {
        Bill bill = new Bill(bi.getDate(), c.client_name);
        ret.put(c.client_name, bill);
      }
      ret.get(c.client_name).addItem(bi);
      
    }
    
    return new ArrayList<Bill>(ret.values());
  }
  
}
