/*
 *    SAS4J
 *
 *    Hugo Zaragoza, Websays.
 */
package websays.accounting;

import static org.junit.Assert.assertEquals;

import java.text.ParseException;
import java.text.SimpleDateFormat;

import org.joda.time.LocalDate;
import org.junit.Assert;
import org.junit.Test;

import websays.accounting.Contract.BillingSchema;
import websays.accounting.Contract.Type;

public class MonthlyMetricsTest {
  
  public static final SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yy");
  
  public Contracts init1(int year) throws ParseException {
    Contracts contracts = new Contracts();
    
    LocalDate start1 = new LocalDate(year, 3, 25); // will be rounded to year/3/1
    LocalDate end1 = new LocalDate(year, 5, 25); // will be rounded to year/5/31
    
    LocalDate start2 = start1.plusDays(1); // will be rounded to year/3/1
    LocalDate end2 = new LocalDate(year, 6, 25);
    
    // MM: 2 3 4 5 6 7
    // C1: _ [ * ] _ _
    // C2: _ [ * * ] _
    
    int id = 0;
    Contract c1 = new Contract(++id, "first", Type.subscription, BillingSchema.MONTHS_1, 1, start1, end1, 100., null, null, null);
    Assert.assertTrue(c1.startRoundDate.equals((new LocalDate(year, 3, 1))));
    Assert.assertEquals(((new LocalDate(year, 5, 31))), c1.endRoundDate);
    
    Contract c2 = new Contract(++id, "second", Type.subscription, BillingSchema.MONTHS_1, 1, start2, end2, 1000., null, null, null);
    Assert.assertTrue(c2.startRoundDate.equals((new LocalDate(year, 3, 1))));
    Assert.assertEquals(((new LocalDate(year, 6, 30))), c2.endRoundDate);
    
    contracts.add(c1);
    contracts.add(c2);
    
    return contracts;
  }
  
  @Test
  public void computeTest() throws ParseException {
    int year = 2010;
    
    Contracts contracts = init1(year);
    
    MonthlyMetrics m = null;
    double d = 0.0001;
    
    int month = 2;
    
    m = MonthlyMetrics.compute(year, month, contracts);
    assertEquals(0, m.accounts);
    assertEquals(0, m.accsNew);
    assertEquals(0, m.accsEnd);
    assertEquals(0.0, m.churn, d);
    
    month++; // 3
    m = MonthlyMetrics.compute(year, month, contracts);
    assertEquals(2, m.accounts);
    assertEquals(2, m.accsNew);
    assertEquals(0, m.accsEnd);
    assertEquals(0.0, m.churn, d);
    
    month++; // 4
    m = MonthlyMetrics.compute(year, month, contracts);
    assertEquals(2, m.accounts);
    assertEquals(0, m.accsNew);
    assertEquals(0, m.accsEnd);
    assertEquals(0.0, m.churn, d);
    
    month++; // 5
    m = MonthlyMetrics.compute(year, month, contracts);
    assertEquals("Month " + month, 1, m.accounts); // ending account not counted
    assertEquals("Month " + month, 0, m.accsNew);
    assertEquals(1, m.accsEnd);
    assertEquals(100.0, m.churn, d);
    
    month++; // 6
    m = MonthlyMetrics.compute(year, month, contracts);
    assertEquals(0, m.accounts); // ending account not counted
    assertEquals(0, m.accsNew);
    assertEquals(1, m.accsEnd);
    assertEquals(1000.0, m.churn, d);
    
    month++; // 7
    m = MonthlyMetrics.compute(year, month, contracts);
    assertEquals(0, m.accounts);
    assertEquals(0, m.accsNew);
    assertEquals(0, m.accsEnd);
    assertEquals(0.0, m.churn, d);
    
    month++; // 8
    m = MonthlyMetrics.compute(year, month, contracts);
    assertEquals(0, m.accounts);
    assertEquals(0, m.accsNew);
    assertEquals(0, m.accsEnd);
    
    month++; // 9
    m = MonthlyMetrics.compute(year, month, contracts);
    assertEquals(0, m.accounts);
    assertEquals(0, m.accsNew);
    assertEquals(0, m.accsEnd);
    assertEquals(0, m.churn, d);
    
  }
}
