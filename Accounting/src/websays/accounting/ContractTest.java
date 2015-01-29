/*
 *    SaaSMetrics4J : https://github.com/hugozaragoza/SaaSMetrics4J
 *
 *    (c) 2014, Hugo Zaragoza, Websays.
 */
package websays.accounting;

import java.text.ParseException;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import org.junit.Assert;
import org.junit.Test;

import websays.accounting.Contract.BillingSchema;
import websays.accounting.Contract.Type;
import websays.core.utils.TimeWebsays;

/**
 * 
 * @author hugoz
 *
 */
public class ContractTest {
  
  private TimeWebsays calendar = new TimeWebsays(Locale.getDefault(), TimeZone.getDefault());
  
  @Test
  public void testMonthsRemaining() throws ParseException {
    
    // CONTRACT: [ 1 MAR - 31 MAR | APR | MAY | JUNE | 1 JULY - 31 JULY ]
    
    int months = 5;
    Date start = GlobalConstants.dateFormat2.parse("01/03/2010");
    Date end = calendar.addMonthsAndDays(start, months, -1);
    Contract c = new Contract(0, "test", Type.contract, BillingSchema.MONTHS_12, 1, start, end, 100., null, null);
    
    Date d = calendar.addMonthsAndDays(start, 0, -0);
    Assert.assertEquals(months - 1, c.getMonthsRemaining(d)); // actual duration in months, but number of months in between in months-1
    
    d = calendar.addMonthsAndDays(start, 3, -0); // 1 JUNE
    Assert.assertEquals(1, c.getMonthsRemaining(d));
    
    d = calendar.addMonthsAndDays(start, 4, -0); // 1 JULY, 0 months remainig (but 31 days left in contract ;)
    Assert.assertEquals(0, c.getMonthsRemaining(d));
    
  }
  
}
