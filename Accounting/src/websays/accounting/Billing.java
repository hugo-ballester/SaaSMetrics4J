/*
 *    SaaSMetrics4J : https://github.com/hugozaragoza/SaaSMetrics4J
 *
 *    (c) 2014, Hugo Zaragoza, Websays.
 */
package websays.accounting;

import java.util.ArrayList;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.joda.time.LocalDate;
import org.joda.time.Months;

import websays.accounting.Contract.BillingSchema;

/**
 * 
 * Computes bills.
 * <ul>
 * <li>Bill date is always the last day of the month
 * <li>Bills the active period touching the bill date
 * <li>One bill per client. A bill may contain many BilleedItem
 * </ul>
 * 
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
   * @param year
   * @param month
   *          1-12
   * @return
   */
  public static LocalDate getBillingDate(int year, int month) {
    LocalDate ret = new LocalDate(year, month, 1);
    ret = ret.dayOfMonth().withMaximumValue();
    return ret;
  }
  
  /**
   * The actual date depends on BilledPeriod.billingDayOfMonth
   * 
   * 
   * @param c
   * @param year
   * @param month
   *          1-12
   * 
   * @return
   */
  public static BilledItem bill(Contract c, int year, int month) {
    try {
      
      // integrity tests
      if (c == null) {
        error(" null contract.");
        return null;
      }
      
      LocalDate billingDate = getBillingDate(year, month);
      
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
      
      // If this month's billing date is not this period'd billing date it means that billing was in the past already, so nothing to charge
      
      if (!billingDate.isEqual(bp.billDate)) {
        // NO BILLING NEEDED, SET PRIZE TO 0.0 TO INDICATE THIS. TODO use boolean field
        bi.setFee(0.0, c.currency);
      } else {
        //
        // COMPUTE FEE
        BillingSchema bs = c.billingSchema;
        Double monthly = null;
        
        double monthlyPrize = c.getMonthlyPrize(billingDate, true, false);
        
        if (bs.isPeriodic()) {
          int n = c.billingSchema.getMonths();
          if (bp.contractEnd != null && n > 1) {
            LocalDate firstDate = bp.periodStart;
            LocalDate endDate = bp.contractEnd;
            if (firstDate.getDayOfMonth() != 1 || endDate.getDayOfMonth() != endDate.dayOfMonth().getMaximumValue()) {
              logger.error("FOR NOW CANNOT DEAL WITH PARTIAL MONTH CONTRACTS WITH BILLING FOR MORE THAN A MONTH!!! (Contract: " + c.name
                  + ")");
            }
            int m = Months.monthsBetween(firstDate, endDate).getMonths() + 1;
            if (m < n) {
              if (m < n) {
                n = m;
              }
            }
          }
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
   * @param month
   *          1-12
   * @return
   */
  public static ArrayList<Bill> bill(Contracts cs, int year, int month) {
    if (cs == null) {
      logger.error("ERROR: NULL contracts");
      return null;
    }
    
    TreeMap<String,Bill> ret = new TreeMap<String,Bill>();
    
    for (Contract c : cs) {
      BilledItem bi = Billing.bill(c, year, month);
      if (bi == null) {
        continue;
      }
      
      if (!ret.containsKey(c.client_name)) {
        Bill bill = new Bill(getBillingDate(year, month), c.client_name);
        ret.put(c.client_name, bill);
      }
      ret.get(c.client_name).addItem(bi);
      
    }
    
    return new ArrayList<Bill>(ret.values());
  }
}
