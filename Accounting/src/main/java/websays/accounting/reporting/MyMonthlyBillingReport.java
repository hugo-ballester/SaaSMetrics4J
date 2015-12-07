/*
 *    SaaSMetrics4J : https://github.com/hugozaragoza/SaaSMetrics4J
 *
 *    (c) 2014, Hugo Zaragoza, Websays.
 */
package websays.accounting.reporting;

import java.util.Collection;

import org.joda.time.LocalDate;

import websays.accounting.BillingReportPrinter;
import websays.accounting.Contracts;
import websays.accounting.Contracts.AccountFilter;
import websays.accounting.Reporting;
import websays.accounting.app.AgencyBilling;
import websays.accounting.app.BasicCommandLineApp;

public class MyMonthlyBillingReport extends BasicCommandLineApp {
  
  private BillingReportPrinter printer;
  
  public MyMonthlyBillingReport(BillingReportPrinter printer) {
    this.printer = printer;
  }
  
  public void report(Contracts contracts, int year, int month, Collection<AccountFilter> billingCenters) throws Exception {
    if (contracts == null) {
      System.err.println("ERROR: NO CONTRACTS WERE LOADED");
      return;
    }
    
    Reporting app = new Reporting(printer);
    
    LocalDate begOfMonth = new LocalDate(year, month, 1);
    LocalDate endOfM = begOfMonth.dayOfMonth().withMaximumValue();
    
    String billingSection = "";
    if (billingCenters == null) {
      billingSection = app.printer.box1(//
          "BILLING", //
          app.displayBilling(begOfMonth.getYear(), begOfMonth.getMonthOfYear(), contracts), connectToDB);
    } else {
      for (AccountFilter af : billingCenters) {
        Contracts conts = contracts.getView(af);
        billingSection += app.printer.box1("BILLING FOR CENTER: " + af.name() + "",
            app.displayBilling(begOfMonth.getYear(), begOfMonth.getMonthOfYear(), conts), connectToDB);
      }
    }
    System.out.println(//
        app.printer.box1("BILLING", billingSection, connectToDB, "bgcolor=\"grey\""));
    
    System.out.println(//
        app.printer.box1("Contracts ending soon:",
            printer.preserveString(app.displayEndingSoon(begOfMonth, AccountFilter.PAID_CONTRACT, contracts)), connectToDB));
    
    System.out.println(//
        app.printer.box1("Totals", printer.preserveString(app.displayTotals(begOfMonth, AccountFilter.PAID_CONTRACT, false, contracts)),
            connectToDB));
    
    System.out.println(//
        app.printer.box1(
            "Starting, Ending & Changing contracts:",
            // endOfM: day does not matter for contract filter, only month, but has to be end because otherwise mrr is 0 for non-started contracts
            printer.preserveString(app.displayContracts(endOfM, AccountFilter.STARTING, false, contracts)
                // beginningOfMonth: opposite reason as above, we need a day in which contracts are active so mrr!=0 end because otherwise mrr is 0
                // for non-started contracts
                + app.displayContracts(begOfMonth, AccountFilter.ENDING, false, contracts)
                + app.displayContracts(begOfMonth, AccountFilter.AUTORENEW, false, contracts)
                + app.displayContracts(begOfMonth, AccountFilter.CHANGED, false, contracts)//
            ), connectToDB));
    
    System.out.println(app.printer.box1("Special Agency Billing", //
        connectToDB ? AgencyBilling.agencyBillingReport(endOfM.toDate()) : "CANT DO IF NOT CONNECTED TO DB", connectToDB));
    
    System.out.println(//
        app.printer.box1("MRRs per Client",//
            printer.preserveString(app.displayAverages(begOfMonth, AccountFilter.PAID_CONTRACT, false, contracts)), connectToDB));
    
    System.out.println(//
        app.printer.box1(
            "All active contracts:",
            printer.preserveString(app.displayContracts(begOfMonth, AccountFilter.CONTRACT, false, contracts)
                + app.displayContracts(begOfMonth, AccountFilter.PROJECT, false, contracts)), //
            connectToDB));
    
  }
}