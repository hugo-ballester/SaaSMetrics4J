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
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;

import websays.accounting.BillingReportPrinter;
import websays.accounting.CalendarWebsays;
import websays.accounting.Contracts.AccountFilter;
import websays.accounting.PrinterASCII;
import websays.accounting.Reporting;
import websays.accounting.reporting.MyMonthlyBillingReport;

public class MyHTMLReport extends BasicCommandLineApp {
  
  String VERSION = "v7.7";
  
  private static final Logger logger = Logger.getLogger(MyHTMLReport.class);
  
  // dont change timezone here, change default instead, since many use this.
  final static CalendarWebsays cal = new CalendarWebsays(Locale.getDefault(), TimeZone.getDefault());
  
  int thisYear = cal.getYear(new Date());
  int thisMonth = cal.getMonth(new Date());
  
  static BillingReportPrinter printer = new PrinterASCII();
  
  public static void main(String[] args) throws Exception {
    PrintStream oldOut = System.out;
    init(args);
    
    String msg = "Writing to " + reportingHTMLDir;
    System.out.println(msg);
    
    int[] years = new int[] {2013, 2014, 2015};
    
    (new MyHTMLReport()).execute_HTML(years);
    
    System.setOut(oldOut);
    
    System.out.println("DONE.");
  }
  
  public void execute_HTML(int[] years) throws Exception {
    
    if (reportingHTMLDir == null) {
      System.out.println("You need to define parameter reportingHtmlDir in properties file");
      return;
    }
    
    if (contracts == null) {
      initContracts();
    }
    
    Reporting app = new Reporting(contracts, new PrinterASCII());
    
    File htmlDir = new File(reportingHTMLDir);
    
    // 1. Write "metrics.html"
    setOutput(new File(htmlDir, "metrics.html"));
    System.out.println("<html><body><pre>\n");
    displayMetrics(app, 2013, 24, new File(htmlDir, "metrics.tsv"));
    
    String metricChanges = metricChangesPerMonth(htmlDir, app);
    
    // 2. Write monthly billing files and get index
    String billing = monthlyBillingReport(htmlDir, years);
    
    // 3. Build "index.html"
    StringBuffer indexFile = new StringBuffer();
    indexFile.append("<html><body><table cellpadding=\"20\" border=\"1\"  >");
    indexFile.append("\n<tr><th>Billing</th><th>Metrics</th><th>Other</th></tr>\n");
    
    indexFile.append("\n<tr><td valign=\"top\">");
    indexFile.append(billing);
    indexFile.append("\n</td>\n");
    indexFile.append("\n<td valign=\"top\">\n");
    
    indexFile.append("<h4><a href=\"metrics.html\">Metrics</a><h4/>Changes:\n");
    indexFile.append(metricChanges);
    indexFile.append("\n</td>\n");
    
    // 2. Build "Last" files
    indexFile.append("\n<td valign=\"top\">\n");
    
    String lastTitle = "Last Contracts";
    indexFile.append("<a href=\"last_1.html\">" + lastTitle + "</a><br/>");
    setOutput(new File(htmlDir, "last_1.html"));
    System.out.println("<h2>" + lastTitle + "</h2><pre>");
    System.out.println(app.report_last(false));
    
    lastTitle += " (new clients only)";
    indexFile.append("<a href=\"last_2.html\">" + lastTitle + "</a><br/>");
    setOutput(new File(htmlDir, "last_2.html"));
    System.out.println("<h2>" + lastTitle + "</h2><pre>");
    System.out.println(app.report_last(true));
    
    lastTitle = "Commissions";
    indexFile.append("<a href=\"commissions.html\">" + lastTitle + "</a><br/>");
    setOutput(new File(htmlDir, "commissions.html"));
    System.out.println("<h2>" + lastTitle + "</h2><pre>");
    String[] commssionnees = contracts.getCommissionnees();
    for (int year : new int[] {2013, 2014, 2015}) {
      System.out.println("<h4>" + lastTitle + " " + year + "</h2><pre>");
      System.out.println(app.report_comm(year, commssionnees));
    }
    indexFile.append("\n</td></tr></table>\n");
    
    FileUtils.writeStringToFile(new File(htmlDir, "index.html"), indexFile.toString());
    
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
  private String metricChangesPerMonth(File htmlDir, Reporting app) throws ParseException, IOException, SQLException, FileNotFoundException {
    
    Date date;
    String index = "";
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
        index += line;
        
        setOutput(new File(htmlDir, file));
        
        System.out.println("<html><body><pre>\n");
        
        date = Reporting.sdf.parse("01/" + bmonth + "/" + myear);
        System.out.println(printer.title("MONTH: " + Reporting.sdf.format(date) + " " + what, connectToDB));
        
        System.out.println(printer.subtitle("Changes"));
        app.displayContracts(date, AccountFilter.starting, true, false);
        app.displayContracts(date, AccountFilter.ending, true, false);
        app.displayContracts(date, AccountFilter.changed, true, false);
        
        System.out.println(printer.subtitle("All Active Contracts"));
        app.displayContracts(date, AccountFilter.contract, true, true);
        app.displayContracts(date, AccountFilter.project, true, true);
        
      }
    }
    index += "</li>\n";
    
    return index;
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
  private void displayMetrics(Reporting app, int yearMetricsStart, int monthsMetrics, File tsvOut) throws IOException, ParseException,
      SQLException {
    System.out.println(printer.title("METRICS (contracted, then projects, then total)", connectToDB));
    
    System.out.println(app.displayMetrics(yearMetricsStart, 1, monthsMetrics, AccountFilter.contract, true));
    System.out.println(app.displayMetrics(yearMetricsStart, 1, monthsMetrics, AccountFilter.project, true));
    System.out.println(app.displayMetrics(yearMetricsStart, 1, monthsMetrics, AccountFilter.contractedORproject, true));
    
    String tsv = app.displayMetrics(yearMetricsStart, 1, monthsMetrics, AccountFilter.contractedORproject, false);
    FileUtils.writeStringToFile(tsvOut, tsv);
    
  }
  
  private String monthlyBillingReport(File htmlDir, int[] years) throws FileNotFoundException, Exception {
    if (fixYear > 0) {
      logger.warn("WARNING: Fixing year and month to: " + fixYear + " - " + fixMonth);
    }
    
    StringBuffer indexFile = new StringBuffer();
    
    MyMonthlyBillingReport mbr = new MyMonthlyBillingReport(printer);
    
    Calendar cal = Calendar.getInstance();
    
    for (int byear : years) {
      indexFile.append("\n\n<bf>" + byear + "</bf><ul>\n");
      
      for (int bmonth = 1; bmonth <= 12; bmonth++) {
        logger.debug("YEAR: " + byear + ", MONTH: " + bmonth);
        if (fixYear > 0 && (!(byear == fixYear && bmonth == fixMonth))) {
          continue;
        }
        boolean thisMonth = (byear == cal.get(Calendar.YEAR) && bmonth == cal.get(Calendar.MONTH) + 1);
        String name1 = "billing_" + byear + "_" + bmonth;
        String file = name1 + ".html";
        if (thisMonth) {
          indexFile.append("<table border=1><tr><td>");
        }
        String line = "<li><a href=\"" + file + "\">" + bmonth + "/" + byear + "</a><br/>\n";
        indexFile.append(line);
        if (thisMonth) {
          indexFile.append("</td></tr></table>");
        }
        setOutput(new File(htmlDir, file));
        System.out.println("<html><body><h1><a href=\"./\">BILLING:</a> " + file + "</h1><pre>\n");
        mbr.report(contracts, byear, bmonth);
      }
      
      indexFile.append("\n</ul>\n");
      
    }
    
    System.out.println("\n<hr/>\n");
    
    return indexFile.toString();
  }
  
  @Override
  public void setOutput(File file) throws FileNotFoundException {
    super.setOutput(file);
    System.out.println(printer.header(VERSION));
  }
}