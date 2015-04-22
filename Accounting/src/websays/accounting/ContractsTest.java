/*
 *    SAS4J
 *
 *    Hugo Zaragoza, Websays.
 */
package websays.accounting;

import java.text.ParseException;
import java.text.SimpleDateFormat;

import org.joda.time.LocalDate;
import org.junit.Assert;
import org.junit.Test;

import websays.accounting.Contract.BillingSchema;
import websays.accounting.Contract.Type;
import websays.accounting.Contracts.AccountFilter;

public class ContractsTest {
  
  public static final SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yy");
  
  @Test
  public void AccountFiltersTest() throws ParseException {
    int year = 2010;
    int startMonth = 3;
    int months = 3;
    
    int id = 0;
    
    // ------------------------------
    // 1. Start and End
    
    // INIT:
    System.out.println("1.");
    Contracts contracts = new Contracts();
    LocalDate start1 = new LocalDate(year, startMonth, 1);
    LocalDate end1 = start1.plusMonths(months).plusDays(-1);
    
    contracts.add(new Contract(++id, "first", Type.contract, BillingSchema.MONTHS_1, 1, start1, end1, 100., null, null));
    _monthSequenceTest(year, startMonth - 1, contracts, new int[] {0, 1, 0, 0, 0}, new int[] {0, 0, 0, 1, 0}, new int[] {0, 0, 0, 0, 0});
    
    // ------------------------------
    // 2. Start and Duration (no end)
    System.out.println("2.");
    contracts = new Contracts();
    Contract c = new Contract(++id, "first", Type.contract,//
        BillingSchema.MONTHS_1, 1, start1, null, 100., null, null);
    contracts.add(c);
    c.contractedMonths = months;
    _monthSequenceTest(year, startMonth - 1, contracts, new int[] {0, 1, 0, 0, 0}, new int[] {0, 0, 0, 0, 0}, new int[] {0, 0, 0, 0, 1});
    
  }
  
  private void _monthSequenceTest(int year, int startMonth, Contracts contracts, //
      int[] starts, int[] ends, int[] renews //
  ) throws ParseException {
    Contracts cS, cE, cR;
    
    // iterate over months and check:
    LocalDate date = new LocalDate(year, startMonth, 1);
    for (int i = 0; i < starts.length; i++) {
      System.out.println("Month " + i);
      
      cS = contracts.getActive(date, AccountFilter.STARTING, true);
      cE = contracts.getActive(date, AccountFilter.ENDING, true);
      cR = contracts.getActive(date, AccountFilter.AUTORENEW, true);
      
      Assert.assertEquals("STARTING month: " + i, starts[i], cS.size());
      Assert.assertEquals("ENDING month: " + i, ends[i], cE.size());
      Assert.assertEquals("RENEWING month: " + i, renews[i], cR.size());
      date = date.plusMonths(1); // nothing on previous month
    }
    
  }
}
