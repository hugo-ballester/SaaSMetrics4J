/*
 *    SaaSMetrics4J : https://github.com/hugozaragoza/SaaSMetrics4J
 *
 *    (c) 2014, Hugo Zaragoza, Websays.
 */
package websays.accounting;

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
public class BillingTest {
  
  @Test
  public void testPartialDaysContract() throws Exception {
    
    LocalDate dateStart = new LocalDate(2010, 3, 5);
    LocalDate dateEnd = new LocalDate(2010, 6, 22);
    double fee = 100.;
    
    Contract c = new Contract(0, "first", Type.contract, BillingSchema.MONTHS_1, 1,//
        dateStart, dateEnd, fee, null, null);
    
    BilledItem bi = Billing.bill(c, 2010, 2);
    Assert.assertNull(bi);
    
    bi = Billing.bill(c, 2010, 3);
    Assert.assertNotNull(bi);
    System.out.println(bi.getFee());
    
    bi = Billing.bill(c, 2010, 4);
    Assert.assertNotNull(bi);
    System.out.println(bi.getFee());
    
    bi = Billing.bill(c, 2010, 5);
    Assert.assertNotNull(bi);
    System.out.println(bi.getFee());
    
  }
  
}
