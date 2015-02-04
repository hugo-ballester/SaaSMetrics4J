/*
 *    SaaSMetrics4J : https://github.com/hugozaragoza/SaaSMetrics4J
 *
 *    (c) 2014, Hugo Zaragoza, Websays.
 */
package websays.accounting.reporting;

import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import websays.accounting.BillingReportPrinter;
import websays.accounting.Contract;
import websays.accounting.Contracts;
import websays.accounting.Contracts.AccountFilter;
import websays.accounting.MonthlyMetrics;
import websays.accounting.Reporting;
import websays.accounting.app.BasicCommandLineApp;
import websays.core.utils.TimeWebsays;

public class MyMonthlyBillingReport extends BasicCommandLineApp {
  
  private static final TimeWebsays calendar = new TimeWebsays(Locale.getDefault(), TimeZone.getDefault());
  private BillingReportPrinter printer;
  
  {
    Logger.getLogger(Contract.class).setLevel(Level.INFO);
    Logger.getLogger(MonthlyMetrics.class).setLevel(Level.INFO);
    
  }
  
  public MyMonthlyBillingReport(BillingReportPrinter printer) {
    this.printer = printer;
  }
  
  Contracts contracts = null;
  
  public void report(Contracts contracts, int year, int month) throws Exception {
    if (contracts == null) {
      System.err.println("ERROR: NO CONTRACTS WERE LOADED");
      return;
    }
    
    Reporting app = new Reporting(contracts, printer);
    
    Calendar cal = calendar.getCalendar(year, month, 1);
    Date begOfMonth = cal.getTime();
    Date endOfM = calendar.dateEndOfMonth(begOfMonth);
    
    title("BILLING", connectToDB);
    app.displayBilling(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1);
    
    title("Contracts ending soon:", connectToDB);
    app.displayEndingSoon(begOfMonth, AccountFilter.CONTRACTED_OR_PROJECT);
    
    title("Totals", connectToDB);
    app.displayTotals(begOfMonth, AccountFilter.CONTRACTED_OR_PROJECT, false);
    
    title("Starting, Ending & Changing contracts:", connectToDB);
    app.displayContracts(endOfM, AccountFilter.STARTING, false, false); // endOfM: day does not matter for contract filter, only month, but has to be
                                                                        // end because otherwise mrr is 0 for non-started contracts
    app.displayContracts(begOfMonth, AccountFilter.ENDING, false, false); // beginningOfMonth: opposite reason as above, we need a day in which
                                                                                // contracts are active so mrr!=0
    // end because otherwise mrr is 0 for non-started contracts
    app.displayContracts(begOfMonth, AccountFilter.AUTORENEW, false, false);
    app.displayContracts(begOfMonth, AccountFilter.CHANGED, false, false);
    
    title("Total MRR per Client", connectToDB);
    app.displayClientMRR(begOfMonth, AccountFilter.CONTRACTED_OR_PROJECT, false);
    
    title("All active contracts:", connectToDB);
    app.displayContracts(begOfMonth, AccountFilter.CONTRACT, false, false);
    app.displayContracts(begOfMonth, AccountFilter.PROJECT, false, false);
    
  }
  
  private void title(String string, boolean connectToDB) {
    System.out.println(printer.title(string, connectToDB));
    
  }
  
}