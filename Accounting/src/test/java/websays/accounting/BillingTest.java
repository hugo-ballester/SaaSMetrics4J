/*
 *    SaaSMetrics4J : https://github.com/hugozaragoza/SaaSMetrics4J
 *
 *    (c) 2014, Hugo Zaragoza, Websays.
 */
package websays.accounting;

import java.util.ArrayList;

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
  
  private static final double delta = 0.0001;
  
  @Test
  public void full() {
    LocalDate dateStart = new LocalDate(2010, 3, 29);
    LocalDate dateEnd = dateStart.plusMonths(4).minusDays(1);
    ArrayList<Commission> lis = new ArrayList<Commission>();
    lis.add(new Commission(.1, null, 3, "MrX"));
    lis.add(new Commission(.2, null, 3, "MrY"));
    BillingSchema billing = BillingSchema.MONTHS_12;
    
    // MONTH 1 - 2 - [3 - 4 - 5 - 6 - 7] - 8 - 9
    // BILLS . - . - [1 - . - . - . - .] - . - .
    // COMMS . - . - [C - . - . - . - .] - . - .
    
    double fee = 100.;
    BilledItem bi;
    Contract c = new Contract(0, "ContractName", Type.subscription, billing, 1,//
        dateStart, dateEnd, fee, null, lis);
    
    for (int month = 1; month <= 12; month++) {
      bi = Billing.bill(c, 2010, month);
      if (month < 3) {
        Assert.assertNull("M" + month, bi);
      } else if (month == 3) {
        Assert.assertNotNull("M" + month, bi);
        Assert.assertEquals("M" + month, 4 * fee, bi.getFee(), 0.001);
        double com1 = bi.getFee() * .1, com2 = bi.getFee() * .2;
        Assert.assertEquals("M" + month, com1, bi.commissions.get(0).commission, delta);
        Assert.assertEquals("M" + month, "MrX", bi.commissions.get(0).commissionnee);
        Assert.assertEquals("M" + month, com2, bi.commissions.get(1).commission, delta);
        Assert.assertEquals("M" + month, "MrY", bi.commissions.get(1).commissionnee);
        Assert.assertEquals("M" + month, 2, bi.commissions.size());
      } else if (month < 8) {
        Assert.assertNotNull("M" + month, bi);
        Assert.assertEquals("M" + month, 0. * fee, bi.getFee(), 0.001);
        
      } else {
        Assert.assertNull("M" + month, bi);
      }
    }
    
  }
  
  @Test
  public void PartilaMonthWithMultiMmonthBill() {
    LocalDate dateStart = new LocalDate(2010, 3, 29);
    LocalDate dateEnd = dateStart.plusMonths(5).minusDays(1);
    ArrayList<Commission> lis = new ArrayList<Commission>();
    lis.add(new Commission(.1, null, 3, "MrX"));
    lis.add(new Commission(.2, null, 3, "MrY"));
    BillingSchema billing = BillingSchema.MONTHS_3;
    
    // MONTH 1 - 2 - [3 - 4 - 5 - 6 - 7] - 8 - 9
    // BILLS . - . - [1 - . - . - 2 - .] - . - .
    // COMMS . - . - [C - C - C - c - c] - . - .
    
    double fee = 100.;
    BilledItem bi;
    
    // ------------------------------------------------------------------------------
    // SIMPLE TEST WITH MONTHS_1
    // ------------------------------------------------------------------------------
    Contract c = new Contract(0, "ContractName", Type.subscription, billing, 1,//
        dateStart, dateEnd, fee, null, lis);
    
    int month = 6;
    bi = Billing.bill(c, 2010, month);
    Assert.assertNotNull("M" + month, bi);
    Assert.assertEquals("M" + month, 2 * fee, bi.getFee(), 0.001);
    double com1 = bi.getFee() * .1, com2 = bi.getFee() * .2;
    if (month >= 6) {
      com1 *= GlobalConstants.COMMMISSION_REMAINING;
      com2 *= GlobalConstants.COMMMISSION_REMAINING;
    }
    Assert.assertEquals("M" + month, com1, bi.commissions.get(0).commission, delta);
    Assert.assertEquals("M" + month, "MrX", bi.commissions.get(0).commissionnee);
    Assert.assertEquals("M" + month, com2, bi.commissions.get(1).commission, delta);
    Assert.assertEquals("M" + month, "MrY", bi.commissions.get(1).commissionnee);
    Assert.assertEquals("M" + month, 2, bi.commissions.size());
    
  }
  
  @Test
  public void testMonthSchedules() throws Exception {
    ArrayList<Commission> lis = new ArrayList<Commission>();
    lis.add(new Commission(.1, null, 3, "MrX"));
    lis.add(new Commission(.2, null, 3, "MrY"));
    
    // MONTH 1 - 2 - [3 - 4 - 5 - 6 - 7] - 8 - 9
    // BILLS . - . - [1 - 2 - 3 - 4 - 5] - . - .
    // COMMS . - . - [C - C - C - c - c] - . - .
    
    LocalDate dateStart = new LocalDate(2010, 3, 1);
    LocalDate dateEnd = dateStart.plusMonths(5).minusDays(1);
    double fee = 100.;
    BilledItem bi;
    
    // ------------------------------------------------------------------------------
    // SIMPLE TEST WITH MONTHS_1
    // ------------------------------------------------------------------------------
    Contract c = new Contract(0, "ContractName", Type.subscription, BillingSchema.MONTHS_1, 1,//
        dateStart, dateEnd, fee, null, lis);
    
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
        double com1 = bi.getFee() * .1, com2 = bi.getFee() * .2;
        if (month >= 6) {
          com1 *= GlobalConstants.COMMMISSION_REMAINING;
          com2 *= GlobalConstants.COMMMISSION_REMAINING;
        }
        Assert.assertEquals("M" + month, com1, bi.commissions.get(0).commission, delta);
        Assert.assertEquals("M" + month, "MrX", bi.commissions.get(0).commissionnee);
        Assert.assertEquals("M" + month, com2, bi.commissions.get(1).commission, delta);
        Assert.assertEquals("M" + month, "MrY", bi.commissions.get(1).commissionnee);
        Assert.assertEquals("M" + month, 2, bi.commissions.size());
        
      } else {
        Assert.assertNull("M" + month, bi);
      }
    }
    
    // ------------------------------------------------------------------------------
    // TEST WITH MONTHS_3
    // ------------------------------------------------------------------------------
    // : 1 - 2 - 3 - 4 - 5 - 6 - 7 - 8 - 9
    // [F3 - 4 - 5 - F6 - 7]
    
    c = new Contract(0, "ContractName", Type.subscription, BillingSchema.MONTHS_3, 1,//
        dateStart, dateEnd, fee, null, lis);
    
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
    
    // ------------------------------------------------------------------------------
    // TEST WITH MONTHS_3 ending few days after bill
    // ------------------------------------------------------------------------------
    dateStart = new LocalDate(2010, 3, 13);
    dateEnd = new LocalDate(2010, 6, 15);
    
    // MONTH 1 - 2 - [3 - 4 - 5 - 6] - 7 - 8 - 9
    // BILLS . - . - [1 - . - . - 2] - . - . - . // Second bill for one month
    // COMMS . - . - [C - C - C - c] - . - .
    
    c = new Contract(0, "ContractName", Type.subscription, BillingSchema.MONTHS_3, 1,//
        dateStart, dateEnd, fee, null, lis);
    
    for (int month = 1; month < 12; month++) {
      bi = Billing.bill(c, 2010, month);
      if (bi == null) {
        System.out.println("MONTH " + month + ": BI is NULL");
      } else {
        System.out.println("MONTH " + month + ": BI :" + bi.toShortString());
      }
      if (month < 3) {
        Assert.assertNull("M" + month, bi);
      } else if (month <= 6) {
        Assert.assertNotNull("M" + month, bi);
        if (month == 3) {
          Assert.assertEquals("M" + month, 3 * fee, bi.getFee(), 0.001);
        } else if (month == 6) {
          Assert.assertEquals("M" + month, 1 * fee, bi.getFee(), 0.001);
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
    
    Contract c = new Contract(0, "first", Type.subscription, BillingSchema.MONTHS_1, 1,//
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