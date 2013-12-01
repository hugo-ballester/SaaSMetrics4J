/*
 *    SAS4J
 *
 *    Hugo Zaragoza, Websays.
 */
package websays.accounting.app;

import static websays.accounting.app.BasicCommandLineApp.dumpDataFile;
import static websays.accounting.app.BasicCommandLineApp.dumpMetrics;
import static websays.accounting.app.BasicCommandLineApp.pricingFile;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.Date;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import utils.DateUtilsWebsays;
import websays.accounting.Contract;
import websays.accounting.Contracts;
import websays.accounting.Contracts.AccountFilter;
import websays.accounting.Metrics;
import websays.accounting.Reporting;
import websays.accounting.connectors.ContractDAO;

public class MyReports extends BasicCommandLineApp {
  
  {
    Logger.getLogger(Contract.class).setLevel(Level.INFO);
    Logger.getLogger(Metrics.class).setLevel(Level.INFO);
    
  }
  
  public static void main(String[] args) throws Exception {
    init(args);
    
    Date date = DateUtilsWebsays.dateEndOfMonth(new Date(), -1);
    // date = sdf.parse("1/4/2013");
    
    BufferedWriter writer = null;
    
    Contracts contracts = ContractDAO.loadAccounts(connectToDB, dumpDataFile != null ? new File(dumpDataFile) : null,
        pricingFile != null ? new File(pricingFile) : null);
    Reporting app = new Reporting(contracts);
    
    // title("DEBUG Contract");
    // app.displayMetrics(2013, 1, 12, "Damm");
    // System.exit(-1);
    
    Reporting.title("Total MRR per Client, then list of contracts with MRR");
    app.displayClientMRR(date, AccountFilter.contractedORproject);
    
    Reporting.title("Staring, Ending & Changing contracts:");
    app.displayContracts(date, AccountFilter.starting, false);
    app.displayContracts(date, AccountFilter.ending, false);
    app.displayContracts(date, AccountFilter.changed, false);
    
    Reporting.title("All active contracts:");
    app.displayContracts(date, AccountFilter.contract, false);
    app.displayContracts(date, AccountFilter.project, false);
    
    if (dumpMetrics != null) {
      writer = new BufferedWriter(new FileWriter(dumpMetrics));
      app.setMetricsOutput(writer);
    }
    Reporting.title("METRICS (contracted, then projects, then total");
    app.displayMetrics(2013, 1, 11, AccountFilter.contract);
    app.displayMetrics(2013, 1, 11, AccountFilter.project);
    app.displayMetrics(2013, 1, 11, AccountFilter.contractedORproject);
    
    if (writer != null) {
      writer.close();
      System.out.println("WROTE: " + dumpMetrics);
    }
    
    Reporting.title("");
    Reporting.title("Contract Changes Month By Month");
    for (int i = 1; i <= 12; i++) {
      date = Reporting.sdf.parse("01/" + i + "/2013");
      Reporting.title("MONTH: " + Reporting.sdf.format(date));
      app.displayContracts(date, AccountFilter.starting, true);
      app.displayContracts(date, AccountFilter.ending, true);
      app.displayContracts(date, AccountFilter.changed, true);
    }
    
  }
}