/*
 *    SAS4J
 *
 *    Hugo Zaragoza, Websays.
 */
package websays.accounting;

import static org.junit.Assert.assertEquals;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.commons.lang3.time.DateUtils;
import org.junit.Test;

import websays.accounting.Contract.BillingSchema;
import websays.accounting.Contract.Type;

public class MonthlyMetricsTest {
  
  public static final SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yy");
  
  @Test
  public void computeTest() throws ParseException {
    Contracts contracts = new Contracts();
    int year = 2010;
    
    Date start1 = sdf.parse("25/02/" + year); // will be rounded to 1/3/year
    Date end1 = DateUtils.addDays(start1, 31 + 30 + 31);
    
    Date start2 = DateUtils.addDays(start1, 31);
    Date end2 = DateUtils.addDays(start2, 30 + 31 + 30 + 31);
    
    // MM: 2 3 4 5 6 7
    // C1: _ [ * ] _ _
    // C2: _ [ * * ] _
    
    int id = 0;
    contracts.add(new Contract(++id, "first", Type.contract, BillingSchema.MONTHS_1, 1, start1, end1, 100., null, null));
    contracts.add(new Contract(++id, "second", Type.contract, BillingSchema.MONTHS_1, 1, start2, end2, 1000., null, null));
    
    MonthlyMetrics m = null;
    double d = 0.0001;
    
    int month = 2;
    m = MonthlyMetrics.compute(year, month, null, contracts, 0, 0, 0);
    assertEquals(0, m.accounts);
    assertEquals(0, m.accsNew);
    assertEquals(0, m.accsEnd);
    assertEquals(0.0, m.churn, d);
    
    month++; // 3
    m = MonthlyMetrics.compute(year, month, null, contracts, 0, 0, 0);
    assertEquals(1, m.accounts);
    assertEquals(1, m.accsNew);
    assertEquals(0, m.accsEnd);
    assertEquals(0.0, m.churn, d);
    
    month++; // 4
    m = MonthlyMetrics.compute(year, month, null, contracts, 0, 0, 0);
    assertEquals(2, m.accounts);
    assertEquals(1, m.accsNew);
    assertEquals(0, m.accsEnd);
    assertEquals(0.0, m.churn, d);
    
    month++; // 5
    m = MonthlyMetrics.compute(year, month, null, contracts, 0, 0, 0);
    assertEquals(2, m.accounts);
    assertEquals(0, m.accsNew);
    assertEquals(0, m.accsEnd);
    assertEquals(0, m.churn, d);
    
    month++; // 6
    m = MonthlyMetrics.compute(year, month, null, contracts, 0, 0, 0);
    assertEquals(1, m.accounts);
    assertEquals(0, m.accsNew);
    assertEquals(1, m.accsEnd);
    assertEquals(100.0, m.churn, d);
    
    month++; // 7
    m = MonthlyMetrics.compute(year, month, null, contracts, 0, 0, 0);
    assertEquals(1, m.accounts);
    assertEquals(0, m.accsNew);
    assertEquals(0, m.accsEnd);
    assertEquals(0.0, m.churn, d);
    
    month++; // 8
    m = MonthlyMetrics.compute(year, month, null, contracts, 0, 0, 0);
    assertEquals(0, m.accounts);
    assertEquals(0, m.accsNew);
    assertEquals(1, m.accsEnd);
    assertEquals(1000.0, m.churn, d);
    
    month++; // 9
    m = MonthlyMetrics.compute(year, month, null, contracts, 0, 0, 0);
    assertEquals(0, m.accounts);
    assertEquals(0, m.accsNew);
    assertEquals(0, m.accsEnd);
    assertEquals(0, m.churn, d);
    
  }
}
