/*
 *    SAS4J
 *
 *    Hugo Zaragoza, Websays.
 */
package websays.accounting.reporting;

import java.util.Calendar;
import java.util.Date;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import websays.accounting.Contract;
import websays.accounting.Contracts;
import websays.accounting.Contracts.AccountFilter;
import websays.accounting.Metrics;
import websays.accounting.Reporting;
import websays.accounting.app.BasicCommandLineApp;
import websays.core.utils.DateUtilsWebsays;

public class MyMonthlyBillingReport extends BasicCommandLineApp {
  
  {
    Logger.getLogger(Contract.class).setLevel(Level.INFO);
    Logger.getLogger(Metrics.class).setLevel(Level.INFO);
    
  }
  
  Contracts contracts = null;
  
  public void execute_String(Contracts contracts, int year, int month) throws Exception {
    if (contracts == null) {
      System.err.println("ERROR: NO CONTRACTS WERE LOADED");
      return;
    }
    
    Reporting app = new Reporting(contracts);
    
    Calendar cal = DateUtilsWebsays.getCalendar(year, month, 1);
    Date date = cal.getTime();
    
    // title("DEBUG Contract");
    // app.displayMetrics(2013, 1, 12, "Damm");
    // System.exit(-1);
    
    app.printTitle("BILLING", connectToDB);
    app.displayBilling(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1);
    
    app.printTitle("Starting, Ending & Changing contracts:", connectToDB);
    app.displayContracts(date, AccountFilter.starting, false, false);
    app.displayContracts(date, AccountFilter.ending, false, false);
    app.displayContracts(date, AccountFilter.changed, false, false);
    
    app.printTitle("Total MRR per Client, then list of contracts with MRR", connectToDB);
    app.displayClientMRR(date, AccountFilter.contractedORproject, false);
    
    app.printTitle("All active contracts:", connectToDB);
    app.displayContracts(date, AccountFilter.contract, false, false);
    app.displayContracts(date, AccountFilter.project, false, false);
    
  }
  
}