/*
 *    SaaSMetrics4J : https://github.com/hugozaragoza/SaaSMetrics4J
 *
 *    (c) 2014, Hugo Zaragoza, Websays.
 */
package websays.accounting;

import org.apache.log4j.Logger;
import org.joda.time.LocalDate;
import org.joda.time.format.DateTimeFormatter;

import websays.accounting.Contract.BillingSchema;
import websays.core.utils.JodaUtils;

/**
 * 
 * Not multi-threaded!
 * 
 * @author hugoz
 *
 */
public class BilledPeriod {
  
  private static final Logger logger = Logger.getLogger(BilledPeriod.class);
  
  LocalDate periodStart, periodEnd; // both inclusive
  LocalDate contractStart, contractEnd; // both inclusive
      
  LocalDate billDate;
  BillingSchema billingSchema;
  int period; // 1,2,... one per billed service span
  
  /**
   * Creates first billed period from contract
   * 
   * @param contracStart
   * @param contractEnd
   * @param billingSchema
   * @throws Exception
   */
  public BilledPeriod(LocalDate contracStart, LocalDate contractEnd, BillingSchema billingSchema) throws Exception {
    super();
    period = 1;
    contractStart = contracStart;
    this.contractEnd = contractEnd;
    this.billingSchema = billingSchema;
    periodStart = new LocalDate(contracStart);
    
    setPeriodEnd();
    setBillingDate();
  }
  
  public BilledPeriod(BilledPeriod bp) throws Exception {
    this(bp.contractStart, bp.contractEnd, bp.billingSchema);
  }
  
  private void setPeriodEnd() throws Exception {
    
    periodEnd = new LocalDate(periodStart);
    
    if (billingSchema.isPeriodic()) {
      periodEnd = periodEnd.plusMonths(billingSchema.getMonths()).minusDays(1);
    } else if (billingSchema.equals(BillingSchema.FULL_1)) {
      if (contractEnd == null) {
        throw new Exception("BilledPeriod init issue: null date for non-periodic billing");
      }
    } else {
      throw new Exception("BilledPeriod init issue: null date for non-periodic billing");
    }
  }
  
  public void setBillingDate() {
    billDate = Billing.getBillingDate(periodStart.getYear(), periodStart.getMonthOfYear());
  }
  
  public boolean next() throws Exception {
    if (!billingSchema.isPeriodic()) {
      logger.warn("Calling next on a non-periodic BillingSchema : " + billingSchema);
      return false;
    }
    period++;
    int step = billingSchema.getMonths();
    periodStart = periodStart.plusMonths(step);
    setPeriodEnd();
    setBillingDate();
    if (contractEnd != null && periodStart.isAfter(contractEnd)) {
      return false;
    }
    return true;
  }
  
  @Override
  public String toString() {
    DateTimeFormatter df = GlobalConstants.dtS;
    String d = "#" + period + ": " + df.print(billDate) + " [" + df.print(periodStart) + ":" + df.print(periodEnd) + "]";
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
  
  public boolean inPeriod(LocalDate d) {
    return !d.isBefore(periodStart) && !d.isAfter(periodEnd);
  }
  
  public boolean isAfterPeriod(LocalDate d) {
    return d.isAfter(periodEnd);
  }
  
  public boolean moveForwardTo(LocalDate d) throws Exception {
    if (inPeriod(d)) {
      return true;
    }
    if (d.isBefore(periodStart)) {
      return false;
    }
    
    do {
      if (d.isBefore(periodStart)) {
        logger.error("moveForwardTo: SHOULD NEVER HAPPEN");
        return false;
      }
      boolean ok = false;
      if (billingSchema.isPeriodic()) {
        ok = next();
      }
      if (!ok) {
        return false; // cannot reach date
      }
    } while (!inPeriod(d));
    
    return true;
  }
  
  public int monthNumber(LocalDate d) {
    return JodaUtils.monthsDifference(contractStart, d) + 1;
  }
  
}
