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

public class ContractsTest {
  
  public static final SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yy");
  
  @Test
  public void AccountFiltersTest() throws ParseException {
    int year = 2010;
    int startMonth = 3;
    int months = 3;
    
    int id = 0;
    Contracts contracts = new Contracts();
    
    // ------------------------------
    // 1. Start and End
    
    // INIT:
    System.out.println("2.");
    Date start1 = sdf.parse("5/" + startMonth + "/" + year); // will be rounded to 1/month/year in metric
    Date end1 = DateUtils.addMonths(start1, months);
    contracts.add(new Contract(++id, "first", Type.contract, BillingSchema.MONTHS_1, 1, start1, end1, 100., null, null));
    
    _monthSequenceTest(year, startMonth, contracts);
    
    // ------------------------------
    // 2. Start and Duration (no end)
    System.out.println("2.");
    contracts = new Contracts();
    Contract c = new Contract(++id, "first", Type.contract,//
        BillingSchema.MONTHS_1, 1, start1, null, 100., null, null);
    contracts.add(c);
    c.contractedMonths = months;
    _monthSequenceTest(year, startMonth, contracts);
    
    _monthSequenceTest(year, startMonth, contracts);
    
  }
  
  private void _monthSequenceTest(int year, int startMonth, Contracts contracts) throws ParseException {
    Contracts cS;
    Contracts cE;
    int month = -1;
    
    // iterate over months and check:
    Date date = sdf.parse("1/" + startMonth + "/" + year);
    
    date = DateUtils.addMonths(date, -1); // nothing on previous month
    cS = contracts.getActive(date, AccountFilter.starting, true);
    cE = contracts.getActive(date, AccountFilter.ending, true);
    Assert.assertEquals("START month:" + month, 0, cS.size());
    Assert.assertEquals("END month:" + month, 0, cE.size());
    month++;
    
    date = DateUtils.addMonths(date, 1); // start (M1)
    cS = contracts.getActive(date, AccountFilter.starting, true);
    cE = contracts.getActive(date, AccountFilter.ending, true);
    Assert.assertEquals("START month:" + month, 1, cS.size());
    Assert.assertEquals("END month:" + month, 0, cE.size());
    month++;
    
    System.out.println("Month 1");
    date = DateUtils.addMonths(date, 1); // (M2)
    cS = contracts.getActive(date, AccountFilter.starting, true);
    cE = contracts.getActive(date, AccountFilter.ending, true);
    Assert.assertEquals("START month:" + month, 0, cS.size());
    Assert.assertEquals("END month:" + month, 0, cE.size());
    month++;
    
    System.out.println("Month 2");
    date = DateUtils.addMonths(date, 1); // end (M3)
    cS = contracts.getActive(date, AccountFilter.starting, true);
    cE = contracts.getActive(date, AccountFilter.ending, true);
    Assert.assertEquals("START month:" + month, 0, cS.size());
    Assert.assertEquals("END month:" + month, 1, cE.size());
    month++;
    
    System.out.println("Month 3");
    date = DateUtils.addMonths(date, 1); // nothing
    cS = contracts.getActive(date, AccountFilter.starting, true);
    cE = contracts.getActive(date, AccountFilter.ending, true);
    Assert.assertEquals("START month:" + month, 0, cS.size());
    Assert.assertEquals("END month:" + month, 0, cE.size());
    month++;
    
  }
}
