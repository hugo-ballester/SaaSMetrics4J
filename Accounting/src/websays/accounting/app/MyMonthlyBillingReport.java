/*
 *    SAS4J
 *
 *    Hugo Zaragoza, Websays.
 */
package websays.accounting.app;

import java.io.File;
import java.util.Calendar;
import java.util.Date;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import websays.accounting.Contract;
import websays.accounting.Contracts;
import websays.accounting.Contracts.AccountFilter;
import websays.accounting.Metrics;
import websays.accounting.Reporting;
import websays.accounting.connectors.ContractDAO;

public class MyMonthlyBillingReport extends BasicCommandLineApp {
  
  {
    Logger.getLogger(Contract.class).setLevel(Level.INFO);
    Logger.getLogger(Metrics.class).setLevel(Level.INFO);
    
  }
  
  public static void main(String[] args) throws Exception {
    init(args);
    
    MyMonthlyBillingReport m = new MyMonthlyBillingReport();
    
    Calendar cal = Calendar.getInstance();
    int year = cal.get(Calendar.YEAR);
    int month = cal.get(Calendar.MONTH) + 1;
    int day = 25;
    
    m.execute_String(year, month, day);
    
  }
  
  Contracts contracts = null;
  
  public void initContracts() throws Exception {
    contracts = ContractDAO.loadAccounts(connectToDB, dumpDataFile != null ? new File(dumpDataFile) : null, pricingFile != null ? new File(
        pricingFile) : null);
  }
  
  public void execute_String(int year, int month, int day) throws Exception {
    if (contracts == null) {
      initContracts();
    }
    
    Reporting app = new Reporting(contracts);
    Date date = df.parse(day + "/" + month + "/" + year);
    
    // title("DEBUG Contract");
    // app.displayMetrics(2013, 1, 12, "Damm");
    // System.exit(-1);
    
    app.title("Total MRR per Client, then list of contracts with MRR", connectToDB);
    app.displayClientMRR(date, AccountFilter.contractedORproject, false);
    
    app.title("Starting, Ending & Changing contracts:", connectToDB);
    app.displayContracts(date, AccountFilter.starting, false);
    app.displayContracts(date, AccountFilter.ending, false);
    app.displayContracts(date, AccountFilter.changed, false);
    
    app.title("All active contracts:", connectToDB);
    app.displayContracts(date, AccountFilter.contract, false);
    app.displayContracts(date, AccountFilter.project, false);
    
  }
  
}