/*
 *    SAS4J
 *
 *    Hugo Zaragoza, Websays.
 */
package websays.accounting;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.commons.lang3.time.DateUtils;
import org.junit.Assert;
import org.junit.Test;

import websays.accounting.Contract.BillingSchema;
import websays.accounting.Contract.Type;
import websays.accounting.Contracts.AccountFilter;
import websays.core.utils.DateUtilsWebsays;

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
    Date start1 = sdf.parse("1/" + startMonth + "/" + year); // will be rounded to 1/month/year in metric
    Date end1 = DateUtilsWebsays.addDays(DateUtils.addMonths(start1, months), -1);
    
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
    
    int month = 0;
    
    // iterate over months and check:
    Date date = sdf.parse("1/" + startMonth + "/" + year);
    for (int i = 0; i < starts.length; i++) {
      System.out.println("Month " + i);
      
      cS = contracts.getActive(date, AccountFilter.starting, true);
      cE = contracts.getActive(date, AccountFilter.ending, true);
      cR = contracts.getActive(date, AccountFilter.renewing, true);
      
      Assert.assertEquals("START month:" + month, starts[month], cS.size());
      Assert.assertEquals("END month  :" + month, ends[month], cE.size());
      Assert.assertEquals("RENEW month:" + month, renews[month], cR.size());
      month++;
      date = DateUtils.addMonths(date, 1); // nothing on previous month
    }
    
  }
}
