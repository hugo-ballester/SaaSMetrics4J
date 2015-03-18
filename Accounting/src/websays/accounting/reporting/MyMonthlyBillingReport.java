/*
 *    SaaSMetrics4J : https://github.com/hugozaragoza/SaaSMetrics4J
 *
 *    (c) 2014, Hugo Zaragoza, Websays.
 */
package websays.accounting.reporting;

import java.util.Calendar;
import java.util.Collection;
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
  
  public void report(Contracts contracts, int year, int month, Collection<AccountFilter> billingCenters) throws Exception {
    if (contracts == null) {
      System.err.println("ERROR: NO CONTRACTS WERE LOADED");
      return;
    }
    
    Reporting app = new Reporting(printer);
    
    Calendar cal = calendar.getCalendar(year, month, 1);
    Date begOfMonth = cal.getTime();
    Date endOfM = calendar.dateEndOfMonth(begOfMonth);
    
    String billingSection = "";
    if (billingCenters == null) {
      billingSection = app.printer.box1("BILLING", app.displayBilling(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1, contracts),
          connectToDB);
    } else {
      for (AccountFilter af : billingCenters) {
        Contracts conts = contracts.getView(af);
        billingSection += app.printer.box1("BILLING FOR CENTER " + af.name(),
            app.displayBilling(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1, conts), connectToDB);
        
      }
    }
    System.out.println(//
        app.printer.box1("BILLING", billingSection, connectToDB, "bgcolor=\"pink\""));
    
    System.out.println(//
        app.printer.box1("Contracts ending soon:", app.displayEndingSoon(begOfMonth, AccountFilter.CONTRACTED_OR_PROJECT, contracts),
            connectToDB));
    
    System.out.println(//
        app.printer.box1("Totals", app.displayTotals(begOfMonth, AccountFilter.CONTRACTED_OR_PROJECT, false, contracts), connectToDB));
    
    System.out.println(//
        app.printer.box1(
            "Starting, Ending & Changing contracts:",
            // endOfM: day does not matter for contract filter, only month, but has to be end because otherwise mrr is 0 for non-started contracts
            app.displayContracts(endOfM, AccountFilter.STARTING, false, false, contracts)
                // beginningOfMonth: opposite reason as above, we need a day in which contracts are active so mrr!=0 end because otherwise mrr is 0
                // for non-started contracts
                + app.displayContracts(begOfMonth, AccountFilter.ENDING, false, false, contracts)
                + app.displayContracts(begOfMonth, AccountFilter.AUTORENEW, false, false, contracts)
                + app.displayContracts(begOfMonth, AccountFilter.CHANGED, false, false, contracts)//
            , connectToDB));
    
    System.out.println(//
        app.printer.box1("Total MRR per Client",//
            Reporting.displayClientMRR(begOfMonth, AccountFilter.CONTRACTED_OR_PROJECT, false, contracts), //
            connectToDB));
    
    System.out.println(//
        app.printer.box1(
            "All active contracts:",
            app.displayContracts(begOfMonth, AccountFilter.CONTRACT, false, false, contracts)
                + app.displayContracts(begOfMonth, AccountFilter.PROJECT, false, false, contracts), //
            connectToDB));
    
  }
  
  private void title(String string, boolean connectToDB) {
    System.out.println(printer.title(string, connectToDB));
  }
  
  private void line() {
    System.out.println(printer.line());
  }
  
}