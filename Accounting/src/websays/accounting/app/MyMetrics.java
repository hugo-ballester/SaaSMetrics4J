/*
 *    SAS4J
 *
 *    Hugo Zaragoza, Websays.
 */
package websays.accounting.app;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.Date;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import websays.accounting.Contract;
import websays.accounting.Contracts;
import websays.accounting.Contracts.AccountFilter;
import websays.accounting.Metrics;
import websays.accounting.Reporting;
import websays.accounting.connectors.ContractDAO;

public class MyMetrics extends BasicCommandLineApp {
  
  {
    Logger.getLogger(Contract.class).setLevel(Level.INFO);
    Logger.getLogger(Metrics.class).setLevel(Level.INFO);
    
  }
  
  public static void main(String[] args) throws Exception {
    init(args);
    
    int year = 2013;
    int month = 11;
    
    BufferedWriter writer = null;
    
    Contracts contracts = ContractDAO.loadAccounts(connectToDB, dumpDataFile != null ? new File(dumpDataFile) : null,
        pricingFile != null ? new File(pricingFile) : null);
    Reporting app = new Reporting(contracts);
    Date date = df.parse("1/" + month + "/" + year);
    
    if (dumpMetrics != null) {
      writer = new BufferedWriter(new FileWriter(dumpMetrics));
      app.setMetricsOutput(writer);
    }
    title("METRICS (contracted, then projects, then total");
    app.displayMetrics(2013, 1, 11, AccountFilter.contract);
    app.displayMetrics(2013, 1, 11, AccountFilter.project);
    app.displayMetrics(2013, 1, 11, AccountFilter.contractedORproject);
    
    if (writer != null) {
      writer.close();
      System.out.println("WROTE: " + dumpMetrics);
    }
    //
    // title("");
    // title("Contract Changes Month By Month");
    // for (int i = 1; i <= 12; i++) {
    // date = Reporting.sdf.parse("01/" + i + "/2013");
    // title("MONTH: " + Reporting.sdf.format(date));
    // app.displayContracts(date, AccountFilter.starting, true);
    // app.displayContracts(date, AccountFilter.ending, true);
    // app.displayContracts(date, AccountFilter.changed, true);
    // }
    //
  }
}