/*
 *    SaaSMetrics4J : https://github.com/hugozaragoza/SaaSMetrics4J
 *
 *    (c) 2014, Hugo Zaragoza, Websays.
 */
package websays.accounting;

import org.apache.log4j.Logger;
import org.joda.time.LocalDate;
import org.junit.Assert;
import org.junit.Test;

import websays.accounting.Contract.BillingSchema;
import websays.accounting.Contract.Type;
import websays.core.utils.CurrencyUtils;

public class BilledItemTest {
  
  public static final String SDF = "dd/MM/yy";
  
  private static final Logger logger = Logger.getLogger(BilledItemTest.class);
  
  @Test
  public void testWarnings() throws Exception {
    
    int year = 2010;
    int startMonth = 3;
    int months = 3;
    int id = 0;
    
    LocalDate dateStart = new LocalDate(year, startMonth, 1);
    LocalDate dateEnd = dateStart.plusMonths(months).minusDays(1);
    
    Contract c = new Contract(0, "first", Type.subscription, BillingSchema.MONTHS_1, 1, dateStart, dateEnd, 100., null, null);
    
    BilledPeriod bp = new BilledPeriod(c.startContract, c.endContract, BillingSchema.MONTHS_1);
    
    // 1. Contract with defined end date
    BilledItem bi = new BilledItem(bp, 100., "name", 0, CurrencyUtils.EUR);
    
    LocalDate d = dateStart;
    bi.warningChecks(d, c);
    Assert.assertEquals(0, bi.notes.size());
    
    bi.notes.clear();
    bi.period.next();
    d = d.plusMonths(1);
    bi.warningChecks(d, c);
    System.out.println(bi.notes);
    Assert.assertEquals(1, bi.notes.size());
    Assert.assertTrue(bi.notes.get(0).indexOf("Contract ending soon as agreed") >= 0);
    
    bi.notes.clear();
    bi.period.next();
    d = d.plusMonths(1); // last
    bi.warningChecks(d, c);
    System.out.println(bi.notes);
    Assert.assertEquals(1, bi.notes.size());
    Assert.assertTrue(bi.notes.get(0), bi.notes.get(0).indexOf("Last bill as agreed") >= 0);
    
    bi.notes.clear();
    bi.period.next();
    d = d.plusMonths(1); // ended
    bi.warningChecks(d, c);
    System.out.println(bi.notes);
    Assert.assertEquals(1, bi.notes.size());
    Assert.assertTrue(bi.notes.get(0), bi.notes.get(0).indexOf("contract has ended") >= 0);
    
    bi.notes.clear();
    bi.period.next();
    d = d.plusMonths(1);
    bi.warningChecks(d, c);
    System.out.println(bi.notes);
    Assert.assertEquals(1, bi.notes.size());
    Assert.assertTrue(bi.notes.get(0), bi.notes.get(0).indexOf("contract has ended") >= 0);
    
    // 2. Contract without defined end date but duration
    c = new Contract(0, "first", Type.subscription, BillingSchema.MONTHS_1, 1, dateStart, null, 100., null, null);
    c.contractedMonths = months;
    
    bp = new BilledPeriod(c.startContract, c.endContract, BillingSchema.MONTHS_1);
    
    bi = new BilledItem(bp, 100., "name", 0, CurrencyUtils.EUR);
    
    d = dateStart;
    bi.warningChecks(d, c);
    Assert.assertEquals(0, bi.notes.size()); // month 1
    
    bi.notes.clear();
    bi.period.next();
    d = d.plusMonths(1);
    bi.warningChecks(d, c);
    System.out.println(bi.notes);
    Assert.assertEquals(1, bi.notes.size()); // month 2
    Assert.assertTrue(bi.notes.get(0), bi.notes.get(0).indexOf(BilledItem.WILL_RENEW_2MONTHS) >= 0);
    
    bi.notes.clear();
    bi.period.next();
    d = d.plusMonths(1); // month 3 last
    bi.warningChecks(d, c);
    System.out.println(bi.notes);
    Assert.assertEquals(1, bi.notes.size());
    Assert.assertTrue(bi.notes.get(0), bi.notes.get(0).indexOf(BilledItem.WILL_RENEW_NEXT_MONTH) >= 0);
    
    bi.notes.clear();
    bi.period.next();
    d = d.plusMonths(1);
    // month 4 autorenew
    bi.warningChecks(d, c);
    System.out.println(bi.notes);
    Assert.assertEquals(1, bi.notes.size());
    Assert.assertTrue(bi.notes.get(0), bi.notes.get(0).indexOf(BilledItem.AUTORENEWED) >= 0);
    
    bi.notes.clear();
    bi.period.next();
    d = d.plusMonths(1);
    bi.warningChecks(d, c);
    System.out.println(bi.notes);
    Assert.assertEquals(1, bi.notes.size());
    Assert.assertTrue(bi.notes.get(0), bi.notes.get(0).indexOf(BilledItem.AUTORENEWED) >= 0);
  }
  
}
