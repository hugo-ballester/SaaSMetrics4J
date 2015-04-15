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
import websays.accounting.Contracts;
import websays.accounting.Contracts.AccountFilter;
import websays.accounting.GlobalConstants;
import websays.accounting.PrinterASCII;
import websays.accounting.Reporting;
import websays.accounting.reporting.MyMonthlyBillingReport;
import websays.core.utils.TimeWebsays;

public class MyHTMLReport extends BasicCommandLineApp {
  
  private static boolean roundDate = false;
  
  private static final Logger logger = Logger.getLogger(MyHTMLReport.class);
  
  final String HTML_PREFIX = "<html><head><meta charset=\"UTF-8\"></head>\n<body>\n";
  
  // dont change timezone here, change default instead, since many use this.
  final static TimeWebsays cal = new TimeWebsays(Locale.getDefault(), TimeZone.getDefault());
  
  int thisYear = cal.getYear(new Date());
  int thisMonth = cal.getMonth(new Date());
  
  static BillingReportPrinter printer = new PrinterASCII();
  
  public static void main(String[] args) throws Exception {
    logger.info("SaaSMetrics4j - MyHTMLReport " + GlobalConstants.VERSION + " : START");
    
    PrintStream oldOut = System.out;
    init(args);
    
    int[] years = new int[] {2015, 2014, 2013, 2012};
    int metricStarYear = 2013;
    int metricMonths = 12 * 3;
    (new MyHTMLReport()).execute_HTML(years, metricStarYear, metricMonths);
    
    System.setOut(oldOut);
    
    logger.info("SaaSMetrics4j - MyHTMLReport " + GlobalConstants.VERSION + " : END");
  }
  
  public void execute_HTML(int[] billingYears, int yearStart, int months) throws Exception {
    
    if (reportingHTMLDir == null) {
      System.out.println("You need to define parameter reportingHtmlDir in properties file");
      return;
    }
    
    Contracts contracts = initContracts();
    
    Reporting app = new Reporting(new PrinterASCII());
    
    File htmlDir = new File(reportingHTMLDir);
    
    // 1. Write "metrics.html"
    setOutput(new File(htmlDir, "metrics.html"));
    System.out.println("<html><body><pre>\n");
    File metricsFile = new File(htmlDir, "metrics.tsv");
    displayMetrics(app, yearStart, months, metricsFile, contracts);
    String metricChanges = metricChangesPerMonth(yearStart, months, htmlDir, app, contracts);
    
    // 2. Write monthly billing files and get index
    String billing = monthlyBillingReport(htmlDir, billingYears, contracts);
    
    // 3. Build "index.html"
    StringBuffer indexFile = new StringBuffer();
    indexFile.append(HTML_PREFIX + "<table cellpadding=\"20\" border=\"1\"  >");
    indexFile.append("\n<tr><th>Billing</th><th>Metrics</th><th>Other</th></tr>\n");
    
    indexFile.append("\n<tr><td valign=\"top\">");
    indexFile.append(billing);
    indexFile.append("\n</td>\n");
    indexFile.append("\n<td valign=\"top\">\n");
    
    indexFile.append("<h4><a href=\"metrics.html\">Metrics</a><h4/>Changes:\n");
    indexFile.append(metricChanges);
    indexFile.append("\n</td>\n");
    
    // 2. Build "Last" files
    String content;
    indexFile.append("\n<td valign=\"top\">\n");
    
    String lastTitle = "Last Contracts";
    indexFile.append("<a href=\"last_1.html\">" + lastTitle + "</a><br/>");
    content = HTML_PREFIX + "<h2>" + lastTitle + "</h2><pre>\n\n" + Reporting.report_last(app.printer, false, contracts);
    FileUtils.writeStringToFile(new File(htmlDir, "last_1.html"), content);
    
    lastTitle += " (new clients only)";
    indexFile.append("<a href=\"last_2.html\">" + lastTitle + "</a><br/>");
    content = HTML_PREFIX + "<h2>" + lastTitle + "</h2><pre>\n\n" + Reporting.report_last(app.printer, true, contracts);
    FileUtils.writeStringToFile(new File(htmlDir, "last_2.html"), content);
    
    lastTitle = "Commissions";
    indexFile.append("<a href=\"commissions.html\">" + lastTitle + "</a><br/>");
    content = HTML_PREFIX + "<h2>" + lastTitle + "</h2><pre>\n\n";
    String[] commssionnees = contracts.getCommissionnees();
    for (int year : billingYears) {
      content += "\n<h4>" + lastTitle + " " + year + "</h4><pre>\n";
      content += Reporting.report_comm(year, commssionnees, contracts);
    }
    FileUtils.writeStringToFile(new File(htmlDir, "commissions.html"), content);
    
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
  private String metricChangesPerMonth(int yearStart, int months, File htmlDir, Reporting app, Contracts contracts) throws ParseException,
      IOException, SQLException, FileNotFoundException {
    
    Date date;
    String index = "";
    String what = "(Metric)";
    int bmonth = 1;
    int myear = yearStart;
    for (int i = 0; i < months; i++) {
      bmonth++;
      if (bmonth > 12) {
        bmonth = 1;
        myear++;
      }
      
      String name1 = "metrics_" + myear + "_" + bmonth;
      String file = name1 + ".html";
      String line = "<li><a href=\"" + file + "\">" + bmonth + "/" + myear + "</a>\n";
      
      if (thisYear == myear && thisMonth == bmonth) {
        line = "<br/><bf>" + line + "<br/></bf>\n";
      }
      index += line;
      
      setOutput(new File(htmlDir, file));
      
      date = Reporting.sdf.parse("01/" + bmonth + "/" + myear);
      String content = "<html><body><pre>\n"
      //
          + printer.title("MONTH: " + Reporting.sdf.format(date) + " " + what, connectToDB)
          //
          + printer.subtitle("Changes") //
          + app.displayContracts_header() //
          + app.displayContracts(date, AccountFilter.STARTING, roundDate, contracts) //
          + app.displayContracts(date, AccountFilter.ENDING, roundDate, contracts) //
          + app.displayContracts(date, AccountFilter.CHANGED, roundDate, contracts) //
          //
          + printer.subtitle("All Active Contracts") //
          + app.displayContracts_header() //
          + app.displayContracts(date, AccountFilter.CONTRACT, roundDate, contracts) //
          + app.displayContracts(date, AccountFilter.PROJECT, roundDate, contracts) //
          + "\n</pre>";
      System.out.println(content);
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
   * @param dump
   *          results to tsv file as well as stdout
   * @throws IOException
   * @throws ParseException
   * @throws SQLException
   */
  private void displayMetrics(Reporting app, int yearMetricsStart, int monthsMetrics, File tsvOut, Contracts contracts) throws IOException,
      ParseException, SQLException {
    System.out.println(printer.title("METRICS (contracted, then projects, then total)", connectToDB));
    
    System.out.println(app.displayMetrics(yearMetricsStart, 1, monthsMetrics, AccountFilter.CONTRACT, true, contracts));
    System.out.println(app.displayMetrics(yearMetricsStart, 1, monthsMetrics, AccountFilter.PROJECT, true, contracts));
    System.out.println(app.displayMetrics(yearMetricsStart, 1, monthsMetrics, AccountFilter.CONTRACTED_OR_PROJECT, true, contracts));
    
    String tsv = app.displayMetrics(yearMetricsStart, 1, monthsMetrics, AccountFilter.CONTRACTED_OR_PROJECT, false, contracts);
    FileUtils.writeStringToFile(tsvOut, tsv);
    
  }
  
  private String monthlyBillingReport(File htmlDir, int[] years, Contracts contracts) throws FileNotFoundException, Exception {
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
        System.out.println("<html><body>");
        
        // GENERATE BILLING REPORT FOR THE MONTH
        System.out.println("<html><body><h1><a href=\"./\">BILLING:</a> " + file + "</h1><pre>\n");
        mbr.report(contracts, byear, bmonth, GlobalConstants.billingCenters);
      }
      
      indexFile.append("\n</ul>\n");
      
    }
    
    System.out.println("\n<hr/>\n");
    
    return indexFile.toString();
  }
  
  @Override
  public void setOutput(File file) throws FileNotFoundException {
    super.setOutput(file);
    System.out.println(printer.header(GlobalConstants.VERSION));
  }
}