/*
 *    SAS4J
 *
 *    Hugo Zaragoza, Websays.
 */
package websays.accounting.metrics;

import static org.junit.Assert.assertEquals;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

import org.junit.Test;

import websays.accounting.Commission;
import websays.accounting.Contract;
import websays.accounting.Contract.BillingSchema;
import websays.accounting.Contract.Type;

public class MetricsTest1 {
  
  final double eps = 0.00000001;
  
  @Test
  public void testCommissions() {
    int cid = 10;
    int cnt = 0;
    Date start = new Date();
    
    // Simple MRR Contract with no Commission
    Type type = Type.contract;
    BillingSchema bschema = BillingSchema.MONTHS_3;
    double mrr = 100.0;
    ArrayList<Commission> commission = new ArrayList<Commission>();
    
    Contract c = new Contract(++cnt, "" + cnt, type, bschema, cid, start, null, mrr, 0.0, commission);
    
    assertEquals(mrr, Metrics.computeMRR(c, start, true), eps);
    assertEquals(0.0, Metrics.computeCommission(c, start, true), eps);
    
    // Simple MRR Contract with one Commissions
    Commission c1 = new Commission(.10, null, "T1");
    
    c.commission.add(c1);
    assertEquals(mrr, Metrics.computeMRR(c, start, true), eps);
    assertEquals(c1.pct * mrr, Metrics.computeCommission(c, start, true), eps);
    
    // Simple MRR Contract with two compounded Commissions
    Commission c2 = new Commission(.20, null, "T2");
    c.commission.add(c2);
    assertEquals(mrr, Metrics.computeMRR(c, start, true), eps);
    assertEquals(c1.pct * mrr + (mrr - (c1.pct * mrr)) * c2.pct, Metrics.computeCommission(c, start, true), eps);
    
    // Change commission base:
    c1.commission_base = mrr / 2.0;
    assertEquals(mrr, Metrics.computeMRR(c, start, true), eps);
    assertEquals(c1.pct * mrr / 2 + (mrr / 2 - (c1.pct * mrr / 2)) * c2.pct, Metrics.computeCommission(c, start, true), eps);
    
  }
  
  /**
   * Test Fixed Prize Contracts
   * 
   * @throws ParseException
   */
  @Test
  public void testFixedPrizes() throws ParseException {
    SimpleDateFormat sdf = new SimpleDateFormat("yyyy MM dd");
    int cid = 10;
    int cnt = 0;
    
    // Contract lasts exactly on month in same calendar month: charge all first month
    int duration = 1;
    Date start = sdf.parse("2014 03 01");
    Calendar cal = Calendar.getInstance();
    cal.setTime(start);
    cal.add(Calendar.MONTH, duration);
    cal.add(Calendar.DAY_OF_YEAR, -1);
    Calendar cal2 = Calendar.getInstance();
    cal2.setTime(cal.getTime());
    cal2.add(Calendar.DAY_OF_YEAR, +1);
    
    Type type = Type.contract;
    BillingSchema bschema = BillingSchema.MONTHS_3;
    double fixed = 100.0;
    ArrayList<Commission> commission = new ArrayList<Commission>();
    
    Contract c = new Contract(++cnt, "" + cnt, type, bschema, cid, start, cal.getTime(), null, fixed, commission);
    assertEquals("2014 03 01", sdf.format(c.startRoundDate)); // rounding
    assertEquals("2014 03 31", sdf.format(c.endRoundDate));
    
    assertEquals(fixed, Metrics.computeMRR(c, start, true), eps);
    assertEquals(fixed, Metrics.computeMRR(c, cal.getTime(), true), eps); // same month, same fee
    assertEquals(0.0, Metrics.computeMRR(c, cal2.getTime(), true), eps); // next month, 0
    
    // Contract lasts two months
    duration = 2;
    start = sdf.parse("2014 3 10");
    cal.setTime(start);
    cal.add(Calendar.MONTH, duration);
    cal.add(Calendar.DAY_OF_YEAR, -1);
    cal2.setTime(cal.getTime());
    cal2.add(Calendar.DAY_OF_YEAR, -2);
    
    c = new Contract(++cnt, "" + cnt, type, bschema, cid, start, cal.getTime(), null, fixed, commission);
    assertEquals("2014 03 01", sdf.format(c.startRoundDate)); // rounding
    assertEquals("2014 04 30", sdf.format(c.endRoundDate));
    
    assertEquals(fixed / 2., Metrics.computeMRR(c, sdf.parse("2014 03 01"), true), eps); // MRR is half, since in two months
    assertEquals(fixed / 2., Metrics.computeMRR(c, sdf.parse("2014 03 10"), true), eps); // MRR is half, since in two months
    assertEquals(fixed / 2., Metrics.computeMRR(c, sdf.parse("2014 04 01"), true), eps); // MRR is half, since in two months
    assertEquals(fixed / 2., Metrics.computeMRR(c, sdf.parse("2014 04 30"), true), eps); // MRR is half, since in two months
    assertEquals(0.0, Metrics.computeMRR(c, sdf.parse("2014 05 01"), true), eps); // completed
    
  }
  
}
