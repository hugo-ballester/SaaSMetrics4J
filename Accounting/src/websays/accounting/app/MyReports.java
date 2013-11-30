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

import scripts.hugo.Queries;
import utils.DateUtilsWebsays;
import websays.accounting.Contract;
import websays.accounting.Contracts;
import websays.accounting.Contracts.AccountFilter;
import websays.accounting.Metrics;
import websays.accounting.Reporting;
import websays.accounting.connectors.ContractDAO;

public class MyReports {
  
  {
    Logger.getLogger(Contract.class).setLevel(Level.INFO);
    Logger.getLogger(Metrics.class).setLevel(Level.INFO);
    
  }
  
  public static void main(String[] args) throws Exception {
    boolean connectToDB = false;
    File dumpDataFile = null, dumpMetrics = null;
    BufferedWriter writer = null;
    
    File pricingFile = new File(args[0]);
    
    if (args.length > 1) {
      dumpDataFile = new File(args[1]);
    }
    if (args.length > 2) {
      dumpMetrics = new File(args[2]);
    }
    
    Contracts contracts = loadAccounts(connectToDB, dumpDataFile, pricingFile);
    Reporting app = new Reporting(contracts);
    
    Date date = DateUtilsWebsays.dateEndOfMonth(new Date());
    // date = sdf.parse("1/4/2013");
    
    // title("DEBUG Contract");
    // app.displayMetrics(2013, 1, 12, "Damm");
    // System.exit(-1);
    
    Reporting.title("Total MRR per Client, then list of contracts with MRR");
    app.displayClientMRR(date, AccountFilter.contractedORproject);
    
    Reporting.title("Staring, Ending & Changing contracts:");
    app.displayContracts(date, AccountFilter.starting);
    app.displayContracts(date, AccountFilter.ending);
    app.displayContracts(date, AccountFilter.changed);
    
    Reporting.title("All active contracts:");
    app.displayContracts(date, AccountFilter.contracted);
    app.displayContracts(date, AccountFilter.project);
    
    if (dumpMetrics != null) {
      writer = new BufferedWriter(new FileWriter(dumpMetrics));
      app.setMetricsOutput(writer);
    }
    Reporting.title("METRICS (contracted, then projects, then total");
    app.displayMetrics(2013, 1, 11, AccountFilter.contracted);
    app.displayMetrics(2013, 1, 11, AccountFilter.project);
    app.displayMetrics(2013, 1, 11, AccountFilter.contractedORproject);
    
    if (writer != null) {
      writer.close();
    }
  }
  
  public static Contracts loadAccounts(boolean connectToDB, File dumpDataFile, File pricingFile) throws Exception {
    Contracts contracts;
    if (connectToDB) {
      Queries.initContext("stage");
      ContractDAO adao = new ContractDAO();
      contracts = adao.getAccounts(null, true);
      contracts.loadPrizeNames(pricingFile);
      contracts.linkPrizes();
      if (dumpDataFile != null) { // save for future use without Internet connection.
        contracts.save(dumpDataFile);
      }
    } else {
      // load last saved
      contracts = Contracts.load(dumpDataFile);
    }
    return contracts;
  }
  
}