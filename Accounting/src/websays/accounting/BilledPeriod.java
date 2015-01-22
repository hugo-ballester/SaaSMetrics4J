/*
 *    SaaSMetrics4J : https://github.com/hugozaragoza/SaaSMetrics4J
 *
 *    (c) 2014, Hugo Zaragoza, Websays.
 */
package websays.accounting;

import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import org.apache.log4j.Logger;

import websays.accounting.Contract.BillingSchema;

/**
 * 
 * Not multi-threaded!
 * 
 * @author hugoz
 *
 */
public class BilledPeriod {
  
  private static final Logger logger = Logger.getLogger(BilledPeriod.class);
  private static CalendarWebsays calendar = new CalendarWebsays(Locale.getDefault(), TimeZone.getDefault());
  
  static int billingDayOfMonth = 28;
  
  Date periodStart, periodEnd;
  Date contractStart, contractEnd;
  
  Date billDate;
  BillingSchema billingSchema;
  int period; // 1,2,... one per billed service span
  
  /**
   * Creates first billed period from contract
   * 
   * @param contracStart
   * @param contractEnd
   * @param billingSchema
   */
  public BilledPeriod(Date contracStart, Date contractEnd, BillingSchema billingSchema) {
    super();
    period = 1;
    contractStart = contracStart;
    this.contractEnd = contractEnd;
    this.billingSchema = billingSchema;
    periodStart = (Date) contracStart.clone();
    
    setPeriodEnd();
    setBillingDate();
  }
  
  public BilledPeriod(BilledPeriod bp) {
    this(bp.contractStart, bp.contractEnd, bp.billingSchema);
  }
  
  private void setPeriodEnd() {
    
    if (billingSchema.isPeriodic()) {
      periodEnd = calendar.addMonthsAndDays(periodStart, billingSchema.getMonths(), -1);
    } else if (billingSchema.equals(BillingSchema.FULL_1)) {
      if (contractEnd == null) {
        logger.error("BilledPeriod init issue: null date for non-periodic billing");
      }
      periodEnd = (Date) contractEnd.clone();
    } else {
      logger.error("BilledPeriod init issue: unrecognized BillingSchema '" + billingSchema + "'");
    }
  }
  
  public void setBillingDate() {
    Calendar cal = calendar.getCalendar(periodStart);
    int day = calendar.getDayOfMonth(periodStart);
    if (day > billingDayOfMonth) {
      cal.add(Calendar.MONTH, 1);
    }
    cal.set(Calendar.DAY_OF_MONTH, billingDayOfMonth);
    billDate = cal.getTime();
  }
  
  public boolean next() {
    if (!billingSchema.isPeriodic()) {
      logger.warn("Calling next on a non-periodic BillingSchema?!");
      return false;
    }
    period++;
    int step = billingSchema.getMonths();
    periodStart = calendar.addMonths(periodStart, step);
    setPeriodEnd();
    setBillingDate();
    if (contractEnd != null && periodStart.after(contractEnd)) {
      return false;
    }
    return true;
  }
  
  @Override
  public String toString() {
    String d = "#" + period + ": " + calendar.dateFormat1.format(billDate) + " [" + calendar.dateFormat1.format(periodStart) + "-"
        + calendar.dateFormat1.format(periodEnd) + "]";
    return d;
  }
  
  // public static void main(String[] args) throws ParseException {
  //
  // Date start = calendar.dateFormat1.parse("21/01/2010");
  // Date end = calendar.dateFormat1.parse("31/01/2012");
  // Contract c = new Contract(1, "C1", Type.contract, BillingSchema.MONTHS_3, 0, start, end, null, 10.);
  // BilledPeriod bs = new BilledPeriod(c.startContract, c.endContract, c.billingSchema);
  // for (int i = 0; i < 10; i++) {
  // System.out.println(bs);
  // bs.next();
  // }
  // System.out.println("-----------------");
  //
  // bs = new BilledPeriod(c.startContract, c.endContract, c.billingSchema);
  // System.out.println(bs);
  // Date to = calendar.addMonths(c.startContract, 5);
  // bs.moveForwardTo(to);
  // System.out.println("-----------------");
  //
  // System.out.println(bs);
  //
  // }
  
  public boolean inPeriod(Date d) {
    Calendar dC = calendar.getCalendar(d);
    Calendar staC = calendar.getCalendar(periodStart);
    Calendar endC = calendar.getCalendar(periodEnd);
    return calendar.isInPeriod(dC, staC, endC);
  }
  
  public boolean isAfterPeriod(Date d) {
    return d.after(periodEnd);
  }
  
  public boolean moveForwardTo(Date d) {
    if (inPeriod(d)) {
      return true;
    }
    if (d.before(periodStart)) {
      return false;
    }
    
    do {
      if (d.before(periodStart)) {
        logger.error("moveForwardTo: SHOULD NEVER HAPPEN");
        return false;
      }
      next();
    } while (!inPeriod(d));
    
    return true;
  }
  
  public int monthNumber(Date d) {
    return calendar.getHowManyMonths(contractStart, d) + 1;
  }
  
}
