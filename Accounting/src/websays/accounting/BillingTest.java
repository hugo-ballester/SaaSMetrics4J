/*
 *    SAS4J
 *
 *    Hugo Zaragoza, Websays.
 */
package websays.accounting;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;

import org.junit.Assert;
import org.junit.Test;

import websays.accounting.Contract.BillingSchema;
import websays.accounting.Contract.Type;

public class BillingTest {
  
  static SimpleDateFormat sfd = new SimpleDateFormat("yyyy/MM/dd");
  static final double delta = 0.000001;
  
  public static Contract c1() throws ParseException {
    Date d1 = sfd.parse("2013/02/25"); // 28 days, 4 days of service
    Date d2 = sfd.parse("2013/04/15");
    Contract c = new Contract(0, "C0", Type.contract, BillingSchema.fullMonth, 10, d1, d2, 0.);
    c.monthlyPrice = 1000.0;
    c.fixedPrice = 0.;
    return c;
  }
  
  // For example, is project starts on the 30/1 on a MRR of 200, on the 30th or 31st we will charge the first two days of Jan, then on any other date
  // of any successive month we would charge the full month
  @Test
  public void testBilling_onceAMonthFullMonth() throws Exception {
    System.out.println("testBilling_onceAMonthFullMonth");
    Contract c = c1();
    
    c.monthlyPrice = 100.0;
    c.fixedPrice = 11.;
    c.billingSchema = BillingSchema.fullMonth;
    
    Object[] obs = new Object[] {//
    sfd.parse("2013/02/14"), null // 1
        , sfd.parse("2013/05/14"), null // 2
        , sfd.parse("2013/02/25"), c.monthlyPrice / 30 * 4 + c.fixedPrice// 3
        , sfd.parse("2013/02/26"), c.monthlyPrice / 30 * 4 + c.fixedPrice // 4
        , sfd.parse("2013/02/28"), c.monthlyPrice / 30 * 4 + c.fixedPrice // 5
        , sfd.parse("2013/02/29"), c.monthlyPrice // 6 : same as 03/01 since only 28 days
        , sfd.parse("2013/03/01"), c.monthlyPrice // 7 : no such date, so null
        , sfd.parse("2013/04/18"), c.monthlyPrice //
        , sfd.parse("2013/04/1"), c.monthlyPrice //
        , sfd.parse("2013/04/15"), c.monthlyPrice //
        , sfd.parse("2013/04/16"), c.monthlyPrice //
        , sfd.parse("2013/04/30"), c.monthlyPrice //
        , sfd.parse("2013/05/01"), null //
    };
    
    for (int i = 0; i < obs.length; i += 2) {
      Date d = (Date) obs[i];
      Double p = (Double) obs[i + 1];
      System.out.println("#" + ((i / 2) + 1) + ": " + sfd.format(d));
      billTest(c, d, p);
    }
  }
  
  @Test
  public void testBilling_onceYear() throws Exception {
    System.out.println("testBilling_onceYear");
    Contract c = c1();
    c.startContract = sfd.parse("2012/02/10"); // 28 days, 4 days of service
    c.endContract = sfd.parse("2014/02/09"); // 28 days, 4 days of service
    
    c.monthlyPrice = 100.0;
    c.fixedPrice = 0.;
    c.billingSchema = BillingSchema.fullYear;
    
    Object[] obs = new Object[] {//
    sfd.parse("2012/02/1"), null // 1
        , sfd.parse("2012/02/2"), null // 2
        , sfd.parse("2012/02/10"), c.monthlyPrice * 12. // 3
        , sfd.parse("2012/02/11"), c.monthlyPrice * 12. // 4
        , sfd.parse("2012/02/28"), c.monthlyPrice * 12. // 5
        , sfd.parse("2012/03/01"), null // 6
        , sfd.parse("2012/05/01"), null // 7
        , sfd.parse("2013/02/10"), c.monthlyPrice * 12. // 8
        , sfd.parse("2012/02/28"), c.monthlyPrice * 12. // 9
        , sfd.parse("2013/03/01"), null // 10
        , sfd.parse("2013/04/15"), null // 11
        , sfd.parse("2014/02/02"), null // 12
        , sfd.parse("2014/02/09"), null // 13
        , sfd.parse("2014/02/10"), null // 14
        , sfd.parse("2014/02/11"), null //
        , sfd.parse("2014/03/11"), null //
    };
    
    for (int i = 0; i < obs.length; i += 2) {
      Date d = (Date) obs[i];
      Double p = (Double) obs[i + 1];
      int j = ((i / 2) + 1);
      String msg = "#" + j + ": " + sfd.format(d);
      billTest(c, d, p, msg);
    }
    System.out.println("---");
  }
  
  private void billTest(Contract c, Date date, Double prize) throws Exception {
    billTest(c, date, prize, null);
  }
  
  private void billTest(Contract c, Date date, Double prize, String msg) throws Exception {
    Date d = date;
    Double e = prize;
    if (msg != null) {
      System.out.println(msg);
    }
    Bill bill = Billing.bill(c, d);
    if (e == null) {
      Assert.assertNull(bill);
    } else {
      Assert.assertEquals(e, bill.sumFee, delta);
      Assert.assertEquals(c.client_name, bill.clientName);
      Assert.assertEquals(d, bill.date);
    }
  }
  
  // For example, is project starts on the 14/1 on a fixed prize of 1000 for 2 months, billing will be 1000 for any date 14-31 of month 1, 0
  // afterwards until last month of proj, null afterwards.
  @Test
  public void testBilling_fullFirstMonth() throws Exception {
    System.out.println("testBilling_fullFirstMonth");
    
    Contract c = c1();
    c.fixedPrice = 100.;
    c.monthlyPrice = null;
    c.billingSchema = BillingSchema.fullFirstMonth;
    
    Object[] obs = new Object[] {//
    sfd.parse("2013/02/14"), null // 1
        , sfd.parse("2013/05/14"), null // 2
        , sfd.parse("2013/02/25"), c.fixedPrice// 3
        , sfd.parse("2013/02/26"), c.fixedPrice // 4
        , sfd.parse("2013/02/28"), c.fixedPrice // 5
        , sfd.parse("2013/02/29"), null // 6 : same as 03/01 since only 28 days
        , sfd.parse("2013/03/01"), null // 7 : no such date, so null
        , sfd.parse("2013/04/18"), null //
        , sfd.parse("2013/04/1"), null //
        , sfd.parse("2013/04/15"), null //
        , sfd.parse("2013/04/16"), null //
        , sfd.parse("2013/04/30"), null //
        , sfd.parse("2013/05/01"), null //
    };
    
    for (int i = 0; i < obs.length; i += 2) {
      Date d = (Date) obs[i];
      Double p = (Double) obs[i + 1];
      System.out.println("#" + ((i / 2) + 1) + ": " + sfd.format(d));
      billTest(c, d, p);
    }
  }
  
  @Test
  public void testBillingContracts() throws ParseException {
    Contract c1 = c1();
    c1.client_name = "C1";
    Contract c2 = c1();
    c2.client_name = "C1";
    Contract c3 = c1();
    c3.client_name = "C2";
    Contracts cs = new Contracts();
    cs.addAll(Arrays.asList(new Contract[] {c1, c2, c3}));
    
    Date d = sfd.parse("2013/03/26");
    
    ArrayList<Bill> bills = Billing.bill(cs, d);
    
    Bill b1 = bills.get(0);
    Bill b2 = bills.get(1);
    Assert.assertEquals("C1", b1.clientName);
    Assert.assertEquals(2, b1.contracts.size());
    Assert.assertEquals(2, b1.fees.size());
    
    Assert.assertEquals(c1.monthlyPrice, b1.fees.get(0));
    Assert.assertEquals(c1.monthlyPrice, b1.fees.get(1));
    Assert.assertEquals("C2", b2.clientName);
    Assert.assertEquals(1, b2.contracts.size());
    Assert.assertEquals(1, b2.fees.size());
    
    // check quantities:
    d = sfd.parse("2013/02/26");
    bills = Billing.bill(cs, d);
    double des = (c1.monthlyPrice + c2.monthlyPrice) / 30. * 4 + c1.fixedPrice + c2.fixedPrice;
    Assert.assertEquals(des, bills.get(0).sumFee, delta);
    Assert.assertEquals(c3.monthlyPrice / 30. * 4 + c3.fixedPrice, bills.get(1).sumFee, delta);
    
  }
  
}