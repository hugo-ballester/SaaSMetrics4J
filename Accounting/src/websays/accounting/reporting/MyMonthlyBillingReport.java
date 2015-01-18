/*
 *    SaaSMetrics4J : https://github.com/hugozaragoza/SaaSMetrics4J
 *
 *    (c) 2014, Hugo Zaragoza, Websays.
 */
package websays.accounting.reporting;

import java.util.Calendar;
import java.util.Date;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import websays.accounting.BillingReportPrinter;
import websays.accounting.Contract;
import websays.accounting.Contracts;
import websays.accounting.Contracts.AccountFilter;
import websays.accounting.MonthlyMetrics;
import websays.accounting.Reporting;
import websays.accounting.app.BasicCommandLineApp;
import websays.core.utils.DateUtilsWebsays;

public class MyMonthlyBillingReport extends BasicCommandLineApp {
  
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
    
    Calendar cal = DateUtilsWebsays.getCalendar(year, month, 1);
    Date date = cal.getTime();
    
    // title("DEBUG Contract");
    // app.displayMetrics(2013, 1, 12, "Damm");
    // System.exit(-1);
    
    title("BILLING", connectToDB);
    app.displayBilling(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1);
    
    Date endOfM = DateUtilsWebsays.dateEndOfMonth(date);
    Date begNextM = DateUtilsWebsays.addDays(DateUtilsWebsays.dateEndOfMonth(date), 1);
    
    title("Contracts ending soon:", connectToDB);
    app.displayEndingSoon(date, AccountFilter.contractedORproject);
    
    title("Total, Starting, Ending & Changing contracts:", connectToDB);
    app.displayTotals(date, AccountFilter.contractedORproject, false);
    app.displayContracts(date, AccountFilter.starting, false, false);
    app.displayContracts(endOfM, AccountFilter.ending, false, false);
    app.displayContracts(date, AccountFilter.renewing, false, false);
    app.displayContracts(date, AccountFilter.changed, false, false);
    
    title("Total MRR per Client", connectToDB);
    app.displayClientMRR(date, AccountFilter.contractedORproject, false);
    
    title("All active contracts:", connectToDB);
    app.displayContracts(date, AccountFilter.contract, false, false);
    app.displayContracts(date, AccountFilter.project, false, false);
    
  }
  
  private void title(String string, boolean connectToDB) {
    System.out.println(printer.title(string, connectToDB));
    
  }
  
}