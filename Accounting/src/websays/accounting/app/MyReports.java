/*
 *    SAS4J
 *
 *    Hugo Zaragoza, Websays.
 */
package websays.accounting.app;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.util.Date;
import java.util.Properties;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import utils.DateUtilsWebsays;
import websays.accounting.Contract;
import websays.accounting.Contracts;
import websays.accounting.Contracts.AccountFilter;
import websays.accounting.Metrics;
import websays.accounting.Reporting;
import websays.accounting.connectors.ContractDAO;
import websays.accounting.connectors.DatabaseManager;

public class MyReports {
  
  {
    Logger.getLogger(Contract.class).setLevel(Level.INFO);
    Logger.getLogger(Metrics.class).setLevel(Level.INFO);
    
  }
  
  public static void main(String[] args) throws Exception {
    String dumpDataFile, dumpMetrics, pricingFile;
    BufferedWriter writer = null;
    
    if (args.length < 1 || args.length > 2) {
      System.out.println("ARGUMENTS: file.properties [false (to run without DB connection)]");
      System.exit(0);
    }
    
    File propFile = new File(args[0]);
    if (!propFile.exists()) {
      System.out.println("Cannot find property file: " + args[0]);
      System.exit(0);
    }
    
    boolean connectToDB = !(args.length > 1 && args[1].equals("false"));
    
    Properties p = new Properties();
    FileInputStream in = new FileInputStream(propFile);
    p.load(in);
    in.close();
    
    pricingFile = p.getProperty("pricingFile", null);
    dumpDataFile = p.getProperty("dumpDBFile", null);
    dumpMetrics = p.getProperty("dumpMetricsFile", null);
    
    if (connectToDB) {
      DatabaseManager.initDatabaseManager(p.getProperty("host"), Integer.parseInt(p.getProperty("port")), p.getProperty("user"),
          p.getProperty("pass"), p.getProperty("db"), true);
    }
    
    Contracts contracts = ContractDAO.loadAccounts(connectToDB, dumpDataFile != null ? new File(dumpDataFile) : null,
        pricingFile != null ? new File(pricingFile) : null);
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
      System.out.println("WROTE: " + dumpMetrics);
    }
    
    Reporting.title("");
    Reporting.title("Contract Changes Month By Month");
    for (int i = 1; i <= 12; i++) {
      date = Reporting.sdf.parse("01/" + i + "/2013");
      Reporting.title("MONTH: " + Reporting.sdf.format(date));
      app.displayContracts(date, AccountFilter.starting);
      app.displayContracts(date, AccountFilter.ending);
      app.displayContracts(date, AccountFilter.changed);
    }
    
  }
}