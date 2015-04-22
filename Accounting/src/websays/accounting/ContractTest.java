/*
 *    SaaSMetrics4J : https://github.com/hugozaragoza/SaaSMetrics4J
 *
 *    (c) 2014, Hugo Zaragoza, Websays.
 */
package websays.accounting;

import java.text.ParseException;

import org.joda.time.LocalDate;
import org.junit.Assert;
import org.junit.Test;

import websays.accounting.Contract.BillingSchema;
import websays.accounting.Contract.Type;

/**
 * 
 * @author hugoz
 *
 */
public class ContractTest {
  
  @Test
  public void testMonthsRemaining() throws ParseException {
    
    // CONTRACT: [ 1 MAR - 31 MAR | APR | MAY | JUNE | 1 JULY - 31 JULY ]
    
    int months = 5;
    LocalDate start = new LocalDate(2010, 3, 1);
    LocalDate end = start.plusMonths(months).plusDays(-1);
    Contract c = new Contract(0, "test", Type.contract, BillingSchema.MONTHS_12, 1, start, end, 100., null, null);
    
    LocalDate d = start;
    Assert.assertEquals(months - 1, c.getMonthsRemaining(d)); // actual duration in months, but number of months in between in months-1
    
    d = start.plusMonths(3);
    // 1 JUNE
    Assert.assertEquals(1, c.getMonthsRemaining(d));
    
    d = d.plusMonths(1);
    // 1 JULY, 0 months remainig (but 31 days left in contract ;)
    Assert.assertEquals(0, c.getMonthsRemaining(d));
    
  }
  
}
