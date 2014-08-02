/*
 *    SaaSMetrics4J : https://github.com/hugozaragoza/SaaSMetrics4J
 *
 *    (c) 2014, Hugo Zaragoza, Websays.
 */
package websays.accounting.app;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import websays.accounting.Billing;
import websays.accounting.Contracts.AccountFilter;
import websays.accounting.MonthlyMetrics;
import websays.accounting.PrinterASCII;
import websays.accounting.Reporting;
import websays.accounting.reporting.MyMonthlyBillingReport;
import websays.core.utils.DateUtilsWebsays;

public class MyHTMLReport extends BasicCommandLineApp {
  
  private static final Logger logger = Logger.getLogger(MyHTMLReport.class);
  
  int thisYear = DateUtilsWebsays.getYear(new Date());
  int thisMonth = DateUtilsWebsays.getMonth(new Date());
  
  public static void main(String[] args) throws Exception {
    PrintStream oldOut = System.out;
    init(args);
    
    // debug = true;
    if (debug) {
      Logger.getLogger(MyHTMLReport.class).setLevel(Level.DEBUG);
      Logger.getLogger(Reporting.class).setLevel(Level.DEBUG);
      Logger.getLogger(MonthlyMetrics.class).setLevel(Level.DEBUG);
      Logger.getLogger(Billing.class).setLevel(Level.TRACE);
      Logger.getLogger(PrinterASCII.class).setLevel(Level.TRACE);
    }
    
    System.out.println("Writing to " + reportingHTMLDir);
    if (reportingHTMLDir != null && reportingHTMLDir.length() > 0) {
      System.out.println("scp -r " + reportingHTMLDir + " deployer@stage:" + reportingHTMLDirRemote);
    }
    
    (new MyHTMLReport()).execute_HTML();
    System.setOut(oldOut);
    
    System.out.println("DONE.");
  }
  
  public void execute_HTML() throws Exception {
    
    if (reportingHTMLDir == null) {
      System.out.println("You need to define parameter reportingHtmlDir in properties file");
      return;
    }
    
    if (contracts == null) {
      initContracts();
    }
    Reporting app = new Reporting(contracts);
    
    File htmlDir = new File(reportingHTMLDir);
    
    // 1. Write "metrics.html"
    logger.trace("Metrics");
    setOutput(new File(htmlDir, "metrics.html"));
    System.out.println("<html><body><pre>\n");
    displayMetrics(app, 2013, 24);
    
    // 2. Write "index.html"
    String indexFile = "<html><body><table cellpadding=\"20\" border=\"1\"  >";
    
    // Billing
    logger.trace("Billing");
    indexFile += "\n<tr><th>Billing</th><th width=50%>Metrics</th></tr>\n";
    indexFile += "\n<tr><td>";
    indexFile += billing(htmlDir);
    indexFile += "\n</td>\n";
    indexFile += "\n<td>\n";
    indexFile += "<h4><a href=\"metrics.html\">Metrics</a><h4/>Changes:\n";
    
    // Metrics
    indexFile = metricChangesPerMonth(htmlDir, app, indexFile);
    indexFile += "\n</td></tr></table>\n";
    setOutput(new File(htmlDir, "index.html"));
    System.out.println(indexFile);
    
  }
  
  /**
   * METRICS and changes for each month:
   * 
   * @param htmlDir
   *          directory where to create file (creates metrics_*_*.html)
   * @param app
   *          rendered
   * @param indexFileContent
   *          adds link line to this string and returns it
   * @return returns new indexFileContent
   * @throws ParseException
   * @throws IOException
   * @throws SQLException
   * @throws FileNotFoundException
   */
  private String metricChangesPerMonth(File htmlDir, Reporting app, String indexFileContent) throws ParseException, IOException,
      SQLException, FileNotFoundException {
    
    Date date;
    
    String what = "(Metric)";
    for (int myear : new int[] {2013, 2014}) {
      for (int bmonth = 1; bmonth <= 12; bmonth++) {
        if (fixYear > 0 && (!(myear == fixYear && bmonth == fixMonth))) {
          continue;
        }
        
        String name1 = "metrics_" + myear + "_" + bmonth;
        String file = name1 + ".html";
        String line = "<li><a href=\"" + file + "\">" + bmonth + "/" + myear + "</a>\n";
        
        if (thisYear == myear && thisMonth == bmonth) {
          line = "<br/><bf>" + line + "<br/></bf>\n";
        }
        indexFileContent += line;
        
        setOutput(new File(htmlDir, file));
        
        System.out.println("<html><body><pre>\n");
        
        date = Reporting.sdf.parse("01/" + bmonth + "/" + myear);
        app.printTitle("MONTH: " + Reporting.sdf.format(date) + " " + what, connectToDB);
        
        app.printSubtitle("Changes");
        app.displayContracts(date, AccountFilter.starting, true, false);
        app.displayContracts(date, AccountFilter.ending, true, false);
        app.displayContracts(date, AccountFilter.changed, true, false);
        
        app.printSubtitle("All Active Contracts");
        app.displayContracts(date, AccountFilter.contract, true, true);
        app.displayContracts(date, AccountFilter.project, true, true);
        
      }
    }
    indexFileContent += "</li>\n";
    
    return indexFileContent;
  }
  
  /**
   * @param app
   *          print to here
   * @param yearMetricsStart
   *          start metrics on Jan of this year
   * @param monthsMetrics
   *          fo metrics for this many months
   * @throws IOException
   * @throws ParseException
   * @throws SQLException
   */
  private void displayMetrics(Reporting app, int yearMetricsStart, int monthsMetrics) throws IOException, ParseException, SQLException {
    app.printTitle("METRICS (contracted, then projects, then total)", connectToDB);
    app.displayMetrics(yearMetricsStart, 1, monthsMetrics, AccountFilter.contract);
    app.displayMetrics(yearMetricsStart, 1, monthsMetrics, AccountFilter.project);
    app.displayMetrics(yearMetricsStart, 1, monthsMetrics, AccountFilter.contractedORproject);
  }
  
  private String billing(File htmlDir) throws FileNotFoundException, Exception {
    if (fixYear > 0) {
      logger.warn("WARNING: Fixing year and month to: " + fixYear + " - " + fixMonth);
    }
    
    String indexFile = "\n\n<ul>\n";
    
    MyMonthlyBillingReport mbr = new MyMonthlyBillingReport();
    
    Calendar cal = Calendar.getInstance();
    
    for (int byear : new int[] {2013, 2014}) {
      for (int bmonth = 1; bmonth <= 12; bmonth++) {
        if (fixYear > 0 && (!(byear == fixYear && bmonth == fixMonth))) {
          continue;
        }
        boolean thisMonth = (byear == cal.get(Calendar.YEAR) && bmonth == cal.get(Calendar.MONTH) + 1);
        String name1 = "billing_" + byear + "_" + bmonth;
        String file = name1 + ".html";
        if (thisMonth) {
          indexFile += "<table border=1><tr><td>";
        }
        String line = "<li><a href=\"" + file + "\">" + bmonth + "/" + byear + "</a><br/>\n";
        indexFile += line;
        if (thisMonth) {
          indexFile += "</td></tr></table>";
        }
        setOutput(new File(htmlDir, file));
        System.out.println("<html><body><h1><a href=\"./\">BILLING:</a> " + file + "</h1><pre>\n");
        mbr.execute_String(contracts, byear, bmonth);
      }
    }
    indexFile += "</ul>";
    
    System.out.println("\n<hr/>\n");
    
    return indexFile;
    
  }
  
  String html_header() {
    
    SimpleDateFormat sd = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
    return "SaaS4J Metrics Report. Generated on " + sd.format(new Date()) + "<hr/>\n\n";
  }
  
  @Override
  public void setOutput(File file) throws FileNotFoundException {
    super.setOutput(file);
    System.out.println(html_header());
  }
}