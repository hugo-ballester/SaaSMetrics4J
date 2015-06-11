/*
 *    SAS4J
 *
 *    Hugo Zaragoza, Websays.
 */
package websays.accounting;

import static org.junit.Assert.assertEquals;

import java.text.ParseException;
import java.util.ArrayList;

import org.joda.time.LocalDate;
import org.junit.Test;

import websays.accounting.Commission;
import websays.accounting.Contract;
import websays.accounting.Contract.BillingSchema;
import websays.accounting.Contract.Type;
import websays.accounting.metrics.Metrics;

public class MetricsTest1 {
  
  final double eps = 0.00000001;
  
  @Test
  public void testCommissions() {
    int commission_months = 10000; // TODO: write unit tests for commission_months, set to infinity for now
    int cid = 10;
    int cnt = 0;
    
    LocalDate start = new LocalDate(2010, 10, 20);
    start.plusDays(200);
    
    // Simple MRR Contract with no Commission
    Type type = Type.subscription;
    BillingSchema bschema = BillingSchema.MONTHS_3;
    double mrr = 100.0;
    ArrayList<Commission> commission = new ArrayList<Commission>();
    
    Contract c = new Contract(++cnt, "" + cnt, type, bschema, cid, start, null, mrr, 0.0, commission);
    
    assertEquals(mrr, Metrics.computeMRR(c, start, true), eps);
    assertEquals(0.0, Metrics.computeCommission(c, start, true), eps);
    
    // Simple MRR Contract with one Commissions
    Commission c1 = new Commission(.10, null, commission_months, "T1");
    
    c.commission.add(c1);
    assertEquals(mrr, Metrics.computeMRR(c, start, true), eps);
    assertEquals(c1.pct * mrr, Metrics.computeCommission(c, start, true), eps);
    
    // Simple MRR Contract with two compounded Commissions
    Commission c2 = new Commission(.20, null, commission_months, "T2");
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
    int cid = 10;
    int cnt = 0;
    
    // Contract lasts exactly on month in same calendar month: charge all first month
    int duration = 1;
    LocalDate start = new LocalDate(2014, 03, 1);
    LocalDate cal = start.plusMonths(duration).plusDays(-1);
    LocalDate cal2 = cal.plusDays(+1);
    
    Type type = Type.subscription;
    BillingSchema bschema = BillingSchema.MONTHS_3;
    double fixed = 100.0;
    ArrayList<Commission> commission = new ArrayList<Commission>();
    
    Contract c = new Contract(++cnt, "" + cnt, type, bschema, cid, start, cal, null, fixed, commission);
    assertEquals(new LocalDate(2014, 03, 01), c.startRoundDate); // rounding
    assertEquals(new LocalDate(2014, 03, 31), c.endRoundDate);
    
    assertEquals(fixed, Metrics.computeMRR(c, start, true), eps);
    assertEquals(fixed, Metrics.computeMRR(c, cal, true), eps); // same month, same fee
    assertEquals(0.0, Metrics.computeMRR(c, cal2, true), eps); // next month, 0
    
    // Contract lasts two months
    duration = 2;
    start = new LocalDate(2014, 3, 10); // start rounded to 3/1
    cal = new LocalDate(2014, 4, 10); // end rounded to 4/30
    
    c = new Contract(++cnt, "" + cnt, type, bschema, cid, start, cal, null, fixed, commission);
    assertEquals(new LocalDate(2014, 03, 01), c.startRoundDate); // rounding
    assertEquals(new LocalDate(2014, 04, 30), c.endRoundDate);
    
    assertEquals(fixed / 2., Metrics.computeMRR(c, new LocalDate(2014, 03, 01), true), eps); // MRR is half, since in two months
    assertEquals(fixed / 2., Metrics.computeMRR(c, new LocalDate(2014, 03, 10), true), eps); // MRR is half, since in two months
    assertEquals(fixed / 2., Metrics.computeMRR(c, new LocalDate(2014, 04, 01), true), eps); // MRR is half, since in two months
    assertEquals(fixed / 2., Metrics.computeMRR(c, new LocalDate(2014, 04, 30), true), eps); // MRR is half, since in two months
    assertEquals(0.0, Metrics.computeMRR(c, new LocalDate(2014, 05, 01), true), eps); // completed
    
  }
}
