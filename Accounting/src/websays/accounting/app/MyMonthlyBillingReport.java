/*
 *    SAS4J
 *
 *    Hugo Zaragoza, Websays.
 */
package websays.accounting.app;

import java.io.File;
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
    
    int year = 2013;
    int month = 11;
    
    Contracts contracts = ContractDAO.loadAccounts(connectToDB, dumpDataFile != null ? new File(dumpDataFile) : null,
        pricingFile != null ? new File(pricingFile) : null);
    Reporting app = new Reporting(contracts);
    Date date = df.parse("1/" + month + "/" + year);
    
    // title("DEBUG Contract");
    // app.displayMetrics(2013, 1, 12, "Damm");
    // System.exit(-1);
    
    title("Total MRR per Client, then list of contracts with MRR");
    app.displayClientMRR(date, AccountFilter.contractedORproject, false);
    
    title("Staring, Ending & Changing contracts:");
    app.displayContracts(date, AccountFilter.starting, false);
    app.displayContracts(date, AccountFilter.ending, false);
    app.displayContracts(date, AccountFilter.changed, false);
    
    title("All active contracts:");
    app.displayContracts(date, AccountFilter.contract, false);
    app.displayContracts(date, AccountFilter.project, false);
    
  }
}