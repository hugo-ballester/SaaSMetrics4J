/*
 *    SAS4J
 *
 *    Hugo Zaragoza, Websays.
 */
package websays.accounting.app;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import websays.accounting.Contract;
import websays.accounting.Contracts;
import websays.accounting.Contracts.AccountFilter;
import websays.accounting.Metrics;
import websays.accounting.Reporting;
import websays.accounting.connectors.ContractDAO;

public class MyHTMLReport extends BasicCommandLineApp {
  
  {
    Logger.getLogger(Contract.class).setLevel(Level.INFO);
    Logger.getLogger(Metrics.class).setLevel(Level.INFO);
    
  }
  
  public static void main(String[] args) throws Exception {
    PrintStream oldOut = System.out;
    init(args);
    
    System.out.println("Writing to " + reportingHTMLDir);
    System.out.println("scp -r " + reportingHTMLDir + " deployer@stage:" + reportingHTMLDirRemote);
    MyHTMLReport r = new MyHTMLReport();
    
    (new MyHTMLReport()).execute_HTML();
    System.setOut(oldOut);
    
    System.out.println("DONE.");
  }
  
  public void execute_HTML() throws Exception {
    
    if (reportingHTMLDir == null) {
      System.out.println("You need to define parameter reportingHtmlDir in properties file");
      return;
    }
    
    File htmlDir = new File(reportingHTMLDir);
    
    Contracts contracts = ContractDAO.loadAccounts(connectToDB, dumpDataFile != null ? new File(dumpDataFile) : null,
        pricingFile != null ? new File(pricingFile) : null);
    Reporting app = new Reporting(contracts);
    
    String indexFile = "<html><body><table cellpadding=\"20\" border=\"1\"  >";
    indexFile += "\n<tr><th width=50%>Metrics</th><th>Billing</th></tr>\n";
    indexFile += "\n<tr><td><h4><a href=\"metrics.html\">Metrics</a><h4/>Changes:\n";
    
    // Build metrics.html file:
    setOutput(new File(htmlDir, "metrics.html"));
    System.out.println("<html><body><pre>\n");
    indexFile = metrics(htmlDir, app, indexFile);
    
    indexFile += "\n</td><td>\n";
    indexFile += billing(htmlDir);
    indexFile += "\n</td></tr></table>\n";
    
    // write Index File:
    setOutput(new File(htmlDir, "index.html"));
    System.out.println(indexFile);
    
  }
  
  private String metrics(File htmlDir, Reporting app, String indexFile) throws ParseException, IOException, SQLException,
      FileNotFoundException {
    // ==============================================================
    // METRICS and changes for each month:
    // ==============================================================
    
    int yearMetricsStart = 2013;
    int monthsMetrics = 24;
    
    int year = 2014;
    int month = 1;
    Date date = df.parse("1/" + month + "/" + year);
    
    app.title("METRICS (contracted, then projects, then total)", connectToDB);
    app.displayMetrics(yearMetricsStart, 1, monthsMetrics, AccountFilter.contract);
    app.displayMetrics(yearMetricsStart, 1, monthsMetrics, AccountFilter.project);
    app.displayMetrics(yearMetricsStart, 1, monthsMetrics, AccountFilter.contractedORproject);
    
    String what = "(Metric)";
    for (int myear : new int[] {2013, 2014}) {
      for (int bmonth = 1; bmonth <= 12; bmonth++) {
        String name1 = "metrics_" + myear + "_" + bmonth;
        String file = name1 + ".html";
        indexFile += "<li><a href=\"" + file + "\">" + bmonth + "/" + myear + "</a>\n";
        setOutput(new File(htmlDir, file));
        
        System.out.println("<html><body><pre>\n");
        
        date = Reporting.sdf.parse("01/" + bmonth + "/" + myear);
        app.title("MONTH: " + Reporting.sdf.format(date) + " " + what, connectToDB);
        app.displayContracts(date, AccountFilter.starting, true);
        app.displayContracts(date, AccountFilter.ending, true);
        app.displayContracts(date, AccountFilter.changed, true);
      }
    }
    indexFile += "</li>\n";
    
    return indexFile;
  }
  
  private String billing(File htmlDir) throws FileNotFoundException, Exception {
    // ==============================================================
    // BILLING for each month:
    // ==============================================================
    String indexFile = "\n\n<ul>\n";
    
    int day = 28;
    MyMonthlyBillingReport mbr = new MyMonthlyBillingReport();
    
    for (int byear : new int[] {2013, 2014}) {
      for (int bmonth = 1; bmonth <= 12; bmonth++) {
        String name1 = "billing_" + byear + "_" + bmonth;
        String file = name1 + ".html";
        indexFile += "<li><a href=\"" + file + "\">" + bmonth + "/" + byear + "</a>\n";
        setOutput(new File(htmlDir, file));
        System.out.println("<html><body><h1>" + file + "</h1><pre>\n");
        mbr.execute_String(byear, bmonth, day);
      }
    }
    indexFile += "</ul>";
    
    System.out.println("\n<hr/>\n");
    
    return indexFile;
    
  }
  
  String html_header() {
    Date d = new Date();
    SimpleDateFormat sd = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
    return "SaaS4J Metrics Report. Generated on " + sd.format(new Date()) + "<hr/>\n\n";
  }
  
  @Override
  public void setOutput(File file) throws FileNotFoundException {
    super.setOutput(file);
    System.out.println(html_header());
  }
}