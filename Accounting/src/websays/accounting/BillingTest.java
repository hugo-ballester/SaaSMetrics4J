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
  public void testMonthSchedules_1() throws Exception {
    
    // :1 - 2 - 3 - 4 - 5 - 6 - 7 - 8 - 9
    // [3 - 4 - 5 - 6 - 7]
    LocalDate dateStart = new LocalDate(2010, 3, 1);
    LocalDate dateEnd = dateStart.plusMonths(5).minusDays(1);
    double fee = 100.;
    BilledItem bi;
    
    // ------------------------------------------------------------------------------
    // SIMPLE TEST WITH MONTHS_1
    // ------------------------------------------------------------------------------
    Contract c = new Contract(0, "ContractName", Type.contract, BillingSchema.MONTHS_1, 1,//
        dateStart, dateEnd, fee, null, null);
    
    for (int month = 1; month < 12; month++) {
      bi = Billing.bill(c, 2010, month);
      if (bi == null) {
        System.out.println("MONTH " + month + ": BI is NULL");
      } else {
        System.out.println("MONTH " + month + ": BI :" + bi.toShortString());
      }
      if (month < 3) {
        Assert.assertNull("M" + month, bi);
      } else if (month <= 7) {
        Assert.assertNotNull("M" + month, bi);
        Assert.assertEquals("M" + month, fee, bi.getFee(), 0.001);
        
      } else {
        Assert.assertNull("M" + month, bi);
      }
    }
    
    // ------------------------------------------------------------------------------
    // TEST WITH MONTHS_3
    // ------------------------------------------------------------------------------
    // : 1 - 2 - 3 - 4 - 5 - 6 - 7 - 8 - 9
    // [F3 - 4 - 5 - F6 - 7]
    
    c = new Contract(0, "ContractName", Type.contract, BillingSchema.MONTHS_3, 1,//
        dateStart, dateEnd, fee, null, null);
    
    for (int month = 1; month < 12; month++) {
      bi = Billing.bill(c, 2010, month);
      if (bi == null) {
        System.out.println("MONTH " + month + ": BI is NULL");
      } else {
        System.out.println("MONTH " + month + ": BI :" + bi.toShortString());
      }
      if (month < 3) {
        Assert.assertNull("M" + month, bi);
      } else if (month <= 7) {
        Assert.assertNotNull("M" + month, bi);
        if (month == 3) {
          Assert.assertEquals("M" + month, 3 * fee, bi.getFee(), 0.001);
        } else if (month == 6) {
          Assert.assertEquals("M" + month, 2 * fee, bi.getFee(), 0.001);
        } else {
          Assert.assertEquals("M" + month, 0.0, bi.getFee(), 0.001);
        }
      } else {
        Assert.assertNull("M" + month, bi);
      }
    }
    
  }
  
  @Test
  public void testPartialDaysContract() throws Exception {
    
    LocalDate dateStart = new LocalDate(2010, 3, 15);
    LocalDate dateEnd = new LocalDate(2010, 6, 15); // ends one day after period ends, so we will charge a full extra month
    double fee = 100.;
    
    // M3: 15/3 - 14/4
    // M4: 15/4 - 14/5
    // M5: 15/5 - 14/6
    // M6: 15/6 - 15/6 : Note that even though it ends the second day of the period, we bill at the end of month because one days left
    
    Contract c = new Contract(0, "first", Type.contract, BillingSchema.MONTHS_1, 1,//
        dateStart, dateEnd, fee, null, null);
    
    BilledItem bi = Billing.bill(c, 2010, 2);
    Assert.assertNull(bi);
    
    for (int month = 1; month < 10; month++) {
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
