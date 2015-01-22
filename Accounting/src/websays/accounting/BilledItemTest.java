/*
 *    SaaSMetrics4J : https://github.com/hugozaragoza/SaaSMetrics4J
 *
 *    (c) 2014, Hugo Zaragoza, Websays.
 */
package websays.accounting;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import org.apache.commons.lang3.time.DateUtils;
import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Test;

import websays.accounting.Contract.BillingSchema;
import websays.accounting.Contract.Currency;
import websays.accounting.Contract.Type;
import websays.core.utils.TimeWebsays;

public class BilledItemTest {
  
  public static final SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yy");
  
  private static final Logger logger = Logger.getLogger(BilledItemTest.class);
  
  private static TimeWebsays calendar = new TimeWebsays(Locale.getDefault(), TimeZone.getDefault());
  
  @Test
  public void testWarnings() throws ParseException {
    
    int year = 2010;
    int startMonth = 3;
    int months = 3;
    int id = 0;
    
    Date d;
    
    Date start1 = sdf.parse("1/" + startMonth + "/" + year); // will be rounded to 1/month/year in metric
    Date end2 = calendar.addDays(DateUtils.addMonths(start1, months), -1);
    
    Contract c = new Contract(0, "first", Type.contract, BillingSchema.MONTHS_1, 1, start1, end2, 100., null, null);
    
    BilledPeriod bp = new BilledPeriod(c.startContract, c.endContract, BillingSchema.MONTHS_1);
    
    // 1. Contract with defined end date
    BilledItem bi = new BilledItem(bp, 100., "name", 0, Currency.EUR);
    
    d = start1;
    bi.warningChecks(d, c);
    Assert.assertEquals(0, bi.notes.size());
    
    bi.notes.clear();
    bi.period.next();
    d = calendar.addMonths(d, 1);
    bi.warningChecks(d, c);
    System.out.println(bi.notes);
    Assert.assertEquals(1, bi.notes.size());
    Assert.assertTrue(bi.notes.get(0).indexOf("Contract ending soon as agreed") >= 0);
    
    bi.notes.clear();
    bi.period.next();
    d = calendar.addMonths(d, 1);
    bi.warningChecks(d, c);
    System.out.println(bi.notes);
    Assert.assertEquals(1, bi.notes.size());
    Assert.assertTrue(bi.notes.get(0), bi.notes.get(0).indexOf("Last bill as agreed") >= 0);
    
    bi.notes.clear();
    bi.period.next();
    d = calendar.addMonths(d, 1);
    bi.warningChecks(d, c);
    System.out.println(bi.notes);
    Assert.assertEquals(1, bi.notes.size());
    Assert.assertTrue(bi.notes.get(0), bi.notes.get(0).indexOf("contract has ended") >= 0);
    
    bi.notes.clear();
    bi.period.next();
    d = calendar.addMonths(d, 1);
    bi.warningChecks(d, c);
    System.out.println(bi.notes);
    Assert.assertEquals(1, bi.notes.size());
    Assert.assertTrue(bi.notes.get(0), bi.notes.get(0).indexOf("contract has ended") >= 0);
    
    // 2. Contract without defined end date but duration
    c = new Contract(0, "first", Type.contract, BillingSchema.MONTHS_1, 1, start1, null, 100., null, null);
    c.contractedMonths = months;
    
    bp = new BilledPeriod(c.startContract, c.endContract, BillingSchema.MONTHS_1);
    
    bi = new BilledItem(bp, 100., "name", 0, Currency.EUR);
    
    d = start1;
    bi.warningChecks(d, c);
    Assert.assertEquals(0, bi.notes.size()); // month 1
    
    bi.notes.clear();
    bi.period.next();
    d = calendar.addMonths(d, 1);
    bi.warningChecks(d, c);
    System.out.println(bi.notes);
    Assert.assertEquals(1, bi.notes.size()); // month 2
    Assert.assertTrue(bi.notes.get(0), bi.notes.get(0).indexOf(BilledItem.WILL_RENEW_2MONTHS) >= 0);
    
    bi.notes.clear();
    bi.period.next();
    d = calendar.addMonths(d, 1); // month 3: last
    bi.warningChecks(d, c);
    System.out.println(bi.notes);
    Assert.assertEquals(1, bi.notes.size());
    Assert.assertTrue(bi.notes.get(0), bi.notes.get(0).indexOf(BilledItem.WILL_RENEW_NEXT_MONTH) >= 0);
    
    bi.notes.clear();
    bi.period.next();
    d = calendar.addMonths(d, 1); // month 4: autorenewd
    bi.warningChecks(d, c);
    System.out.println(bi.notes);
    Assert.assertEquals(1, bi.notes.size());
    Assert.assertTrue(bi.notes.get(0), bi.notes.get(0).indexOf(BilledItem.AUTORENEWED) >= 0);
    
    bi.notes.clear();
    bi.period.next();
    d = calendar.addMonths(d, 1);
    bi.warningChecks(d, c);
    System.out.println(bi.notes);
    Assert.assertEquals(1, bi.notes.size());
    Assert.assertTrue(bi.notes.get(0), bi.notes.get(0).indexOf(BilledItem.AUTORENEWED) >= 0);
  }
}
