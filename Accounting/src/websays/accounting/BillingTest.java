/*
 *    SAS4J
 *
 *    Hugo Zaragoza, Websays.
 */
package websays.accounting;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.junit.Assert;
import org.junit.Test;

import websays.accounting.Contract.BillingSchema;
import websays.accounting.Contract.Type;
import websays.core.utils.DateUtilsWebsays;

public class BillingTest {
  
  static SimpleDateFormat sfd = new SimpleDateFormat("yyyy/MM/dd");
  static final double delta = 0.000001;
  
  int daysOfFirstMonthContracted = 4;
  
  public static Contract exampleContract() throws ParseException {
    Date d1 = sfd.parse("2013/02/25"); // 28 days, 4 days of service
    Date d2 = sfd.parse("2014/02/28");
    Contract c = new Contract(0, "C0", Type.contract, BillingSchema.MONTHS_1, 10, d1, d2, 0.);
    c.monthlyPrice = 300.0;
    c.fixedPrice = 130.;
    c.billingSchema = BillingSchema.MONTHS_1;
    
    return c;
  }
  
  // For example, is project starts on the 30/1 on a MRR of 200, on the 30th or 31st we will charge the first two days of Jan, then on any other date
  // of any successive month we would charge the full month
  @Test
  public void testBilling__MONTHS_X() throws Exception {
    Contract c = exampleContract();
    
    double months = DateUtilsWebsays.getHowManyMonths(c.startContract, c.endContract) + 1;
    double fixedMonthly = c.fixedPrice / months;
    
    double rr = c.getMonthlyPrize(null, true);
    double r1 = c.getMonthlyPrize(null, false) / 30. * 4. + fixedMonthly;
    
    // CONTRACT: 2013/02/25 - 2014/02/28
    // MONTHS: 4 days + 12 months
    // MONTHLY: 300 + 10 = 310;
    // MONTH1: 300/30*4 + 130/13 = 40 + 10 = 50
    
    Assert.assertEquals(13, (int) months);
    Assert.assertEquals(10, fixedMonthly, 0.001);
    Assert.assertEquals(310, rr, 0.001);
    Assert.assertEquals(50., r1, 0.001);
    
    System.out.println("testBilling: " + c.billingSchema);
    
    // note that day is ignored, only month an year counts
    Object[] obs = new Object[] {//
    sfd.parse("2013/01/1"), null // 1
        , sfd.parse("2013/02/1"), null // 2
        , sfd.parse("2013/03/1"), rr + r1 // 3
        , sfd.parse("2013/04/1"), rr // 4
        , sfd.parse("2014/05/1"), null // 5
        , sfd.parse("2015/04/1"), null // 5
        , sfd.parse("2011/04/1"), null // 5
    
    };
    
    for (int i = 0; i < obs.length; i += 2) {
      Date d = (Date) obs[i];
      Double p = (Double) obs[i + 1];
      System.out.println("#" + ((i / 2) + 1) + ": " + sfd.format(d));
      billTest(c, d, p);
    }
    
    // 3 MONTH BILLING
    c.billingSchema = BillingSchema.MONTHS_3;
    System.out.println("testBilling: " + c.billingSchema);
    
    obs = new Object[] {//
    sfd.parse("2013/01/1"), null // 1
        , sfd.parse("2013/02/1"), null // 2
        , sfd.parse("2013/03/1"), 3 * rr + r1 // 3
        , sfd.parse("2013/04/1"), null // 4
        , sfd.parse("2013/05/1"), null // 5
        , sfd.parse("2013/06/1"), 3 * rr // 6
        , sfd.parse("2013/07/1"), null // 7
        , sfd.parse("2014/05/1"), null // 8
        , sfd.parse("2015/04/1"), null // 8
    
    };
    
    for (int i = 0; i < obs.length; i += 2) {
      Date d = (Date) obs[i];
      Double p = (Double) obs[i + 1];
      System.out.println("#" + ((i / 2) + 1) + ": " + sfd.format(d));
      billTest(c, d, p);
    }
    
    // 12 MONTH BILLING
    c.billingSchema = BillingSchema.MONTHS_12;
    System.out.println("testBilling: " + c.billingSchema);
    
    obs = new Object[] {//
    sfd.parse("2013/01/1"), null // 1
        , sfd.parse("2013/02/1"), null // 2
        , sfd.parse("2013/03/1"), 12 * rr + r1 // 3
        , sfd.parse("2013/04/1"), null // 4
        , sfd.parse("2013/05/1"), null // 5
        , sfd.parse("2013/06/1"), null // 6
        , sfd.parse("2013/07/1"), null // 7
        , sfd.parse("2014/05/1"), null // 8
        , sfd.parse("2015/04/1"), null // 8
    
    };
    
    for (int i = 0; i < obs.length; i += 2) {
      Date d = (Date) obs[i];
      Double p = (Double) obs[i + 1];
      System.out.println("#" + ((i / 2) + 1) + ": " + sfd.format(d));
      billTest(c, d, p);
    }
    
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
  
  @Test
  public void testBilling_FULL_1() throws Exception {
    System.out.println("FULL_1");
    
    Contract c = exampleContract();
    c.billingSchema = BillingSchema.FULL_1;
    c.monthlyPrice = null;
    c.fixedPrice = 300.;
    
    Object[] obs = new Object[] {//
    sfd.parse("2013/01/1"), null // 1
        , sfd.parse("2013/02/1"), null // 2
        , sfd.parse("2013/03/1"), c.fixedPrice // 3
        , sfd.parse("2013/04/1"), null // 4 active but not bill
        , sfd.parse("2014/05/1"), null // 5
        , sfd.parse("2015/04/1"), null // 5
        , sfd.parse("2011/04/1"), null // 5
    
    };
    for (int i = 0; i < obs.length; i += 2) {
      Date d = (Date) obs[i];
      Double p = (Double) obs[i + 1];
      System.out.println("#" + ((i / 2) + 1) + ": " + sfd.format(d));
      billTest(c, d, p);
    }
    
  }
  
}