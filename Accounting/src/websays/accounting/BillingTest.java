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
    
    LocalDate dateStart = new LocalDate(2010, 3, 15);
    LocalDate dateEnd = new LocalDate(2010, 6, 16);
    double fee = 100.;
    
    // M3: 15/3 - 14/4
    // M4: 15/4 - 14/5
    // M5: 15/5 - 14/6
    // M6: 15/6 - 14/7 : Note that even though it ends the second day of the period, we bill at the end of month because some days left
    
    Contract c = new Contract(0, "first", Type.contract, BillingSchema.MONTHS_1, 1,//
        dateStart, dateEnd, fee, null, null);
    
    BilledItem bi = Billing.bill(c, 2010, 2);
    Assert.assertNull(bi);
    
    for (int i = 0; i < 10; i++) {
      int month = 1 + i;
      bi = Billing.bill(c, 2010, month);
      if (month < 3) {
        Assert.assertNull("M" + month, bi);
      } else if (month < 7) {
        Assert.assertNotNull("M" + month, bi);
        Assert.assertEquals("M" + month, fee, bi.getFee(), 0.001);
        System.out.println("M" + month + ": " + bi.period + " " + bi.getFee());
      } else {
        Assert.assertNull("M" + month, bi);
      }
    }
    
  }
  
}
