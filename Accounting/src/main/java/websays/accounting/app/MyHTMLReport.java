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

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.joda.time.LocalDate;

import websays.accounting.BillingReportPrinter;
import websays.accounting.Contracts;
import websays.accounting.Contracts.AccountFilter;
import websays.accounting.GlobalConstants;
import websays.accounting.PrinterHTML;
import websays.accounting.Reporting;
import websays.accounting.reporting.MiniReport;
import websays.accounting.reporting.MyMonthlyBillingReport;

public class MyHTMLReport extends BasicCommandLineApp {
  
  private static boolean roundDate = false;
  
  private static final Logger logger = Logger.getLogger(MyHTMLReport.class);
  
  final static BillingReportPrinter printer = new PrinterHTML();
  
  int thisYear = (new LocalDate()).getYear();
  int thisMonth = (new LocalDate()).getMonthOfYear();
  
  public static void main(String[] args) throws Exception {
    logger.info("SaaSMetrics4j - MyHTMLReport " + GlobalConstants.VERSION + " : START");
    int[] billingYears = new int[] {2016, 2015, 2014, 2013};
    
    PrintStream oldOut = System.out;
    init(args);
    
    if (fixYear != null) {
      billingYears = new int[] {fixYear};
    }
    
    (new MyHTMLReport()).execute_HTML(billingYears, billingYears);
    
    System.setOut(oldOut);
    
    logger.info("SaaSMetrics4j - MyHTMLReport " + GlobalConstants.VERSION + " : END");
  }
  
  /**
   * @param billingYears
   *          : should be a sequence of decreasing year numbers: { 2016, 2015, etc.}
   * @param metricsyears
   *          : should be a sequence of decreasing year numbers: { 2016, 2015, etc.}
   * @throws Exception
   */
  public void execute_HTML(int[] billingYears, int[] metricsyears) throws Exception {
    
    if (reportingHTMLDir == null) {
      System.out.println("You need to define parameter reportingHtmlDir in properties file");
      return;
    }
    
    Contracts contracts = initContracts();
    
    Reporting app = new Reporting(new PrinterHTML());
    
    File htmlDir = new File(reportingHTMLDir);
    
    // 1. Write "metrics.html"
    setOutput(new File(htmlDir, "metrics.html"));
    System.out.println("<html><body><pre>\n");
    File metricsFile = new File(htmlDir, "metrics.tsv");
    int yearStart = metricsyears[metricsyears.length - 1];
    int months = Math.abs((metricsyears[0] - yearStart + 1) * 12);
    displayMetrics(app, yearStart, months, metricsFile, contracts);
    String metricChanges = metricChangesPerMonth(yearStart, months, htmlDir, app, contracts);
    
    // 2. Write monthly billing files and get index
    String billing = monthlyBillingReport(htmlDir, billingYears, contracts);
    
    // 3. Build "index.html"
    StringBuffer indexFile = new StringBuffer();
    indexFile.append(printer.header() + "<table cellpadding=\"20\" border=\"1\"  >");
    indexFile.append("\n<tr><th>Billing</th><th>Metrics</th><th>Other</th></tr>\n");
    
    indexFile.append("\n<tr><td valign=\"top\">");
    indexFile.append(billing);
    indexFile.append("\n</td>\n");
    indexFile.append("\n<td valign=\"top\">\n");
    
    indexFile.append("<h4><a href=\"metrics.html\">Metrics</a><h4/>Changes:\n");
    indexFile.append(metricChanges);
    indexFile.append("\n</td>\n");
    
    // == Build "Last" files
    String content;
    indexFile.append("\n<td valign=\"top\">\n");
    
    String lastTitle = "Last Contracts";
    indexFile.append("<a href=\"last_1.html\">" + lastTitle + "</a><br/>");
    content = printer.header() + "<h2>" + lastTitle + "</h2><pre>\n\n" + //
        Reporting.report_last(app.printer, false, contracts, Contracts.AccountFilter.PAID_CONTRACT);
    FileUtils.writeStringToFile(new File(htmlDir, "last_1.html"), content);
    
    lastTitle += " (new clients only)";
    indexFile.append("<a href=\"last_2.html\">" + lastTitle + "</a><br/>");
    content = printer.header() + "<h2>" + lastTitle + "</h2><pre>\n\n"
        + Reporting.report_last(app.printer, true, contracts, Contracts.AccountFilter.PAID_CONTRACT);
    FileUtils.writeStringToFile(new File(htmlDir, "last_2.html"), content);
    
    // == Commissions
    lastTitle = "Commissions";
    indexFile.append("<br/><a href=\"" + lastTitle + ".html\">" + lastTitle + "</a><br/>");
    content = printer.header() + "<h2>" + lastTitle + "</h2><pre>\n\n";
    String[] commssionees = contracts.getCommissionnees();
    for (int year : billingYears) {
      content += "\n<h4>" + lastTitle + " " + year + "</h4><pre>\n";
      content += Reporting.report_comm(year, commssionees, contracts);
    }
    FileUtils.writeStringToFile(new File(htmlDir, lastTitle + ".html"), content);
    
    // == Client Monthly Report
    lastTitle = "Client Monthly";
    indexFile.append("<br/><a href=\"" + lastTitle + ".html\">" + lastTitle + "</a><br/>");
    content = printer.header() + "<h2>" + lastTitle + "</h2><pre>\n\n";
    for (int year : billingYears) {
      content += "\n<h4>" + lastTitle + " " + year + "</h4><pre>\n";
      ContractMonthlyReport cmr = new ContractMonthlyReport();
      content += cmr.allContractReport(contracts, year);
    }
    FileUtils.writeStringToFile(new File(htmlDir, lastTitle + ".html"), content);
    
    // == MRR Report
    indexFile.append("<br/>");
    lastTitle = "MRR_MiniReport";
    indexFile.append("<a href=\"" + lastTitle + ".html\">" + lastTitle + "</a><br/>");
    content = printer.header() + "<h2>" + lastTitle + "</h2><pre>\n\n";
    LocalDate miniStart = new LocalDate().minusMonths(6);
    content += MiniReport.miniReport(contracts, printer, miniStart.getYear(), miniStart.getMonthOfYear(), 9);
    FileUtils.writeStringToFile(new File(htmlDir, lastTitle + ".html"), content);
    
    indexFile.append("<br/>");
    indexFile.append("<a href=\"./BACKUP\">BACKUPs</a><br/>");
    
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
    
    LocalDate date;
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
      date = new LocalDate(myear, bmonth, 1);
      String content = "<html><body><pre>\n"
      //
          + printer.title("MONTH: " + Reporting.sdf.print(date) + " " + what, connectToDB)
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
    System.out.println(app.displayMetrics(yearMetricsStart, 1, monthsMetrics, AccountFilter.PAID_CONTRACT, true, contracts));
    
    String tsv = app.displayMetrics(yearMetricsStart, 1, monthsMetrics, AccountFilter.PAID_CONTRACT, false, contracts);
    FileUtils.writeStringToFile(tsvOut, tsv);
    
  }
  
  private String monthlyBillingReport(File htmlDir, int[] years, Contracts contracts) throws FileNotFoundException, Exception {
    if (fixYear != null) {
      logger.warn("WARNING: Fixing year and month to: " + fixYear + " - " + fixMonth);
    }
    
    StringBuffer indexFile = new StringBuffer();
    
    MyMonthlyBillingReport mbr = new MyMonthlyBillingReport(printer);
    
    LocalDate cal = new LocalDate();
    
    for (int byear : years) {
      indexFile.append("\n\n<bf>" + byear + "</bf><ul>\n");
      
      for (int bmonth = 1; bmonth <= 12; bmonth++) {
        logger.debug("YEAR: " + byear + ", MONTH: " + bmonth);
        if (fixYear != null && (!(byear == fixYear && bmonth == fixMonth))) {
          continue;
        }
        boolean thisMonth = (byear == cal.getYear()) && bmonth == cal.getMonthOfYear();
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
        
        // GENERATE BILLING REPORT FOR THE MONTH
        // System.out.println("<h3><a href=\"./\">[home]</a></h3>\n");
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
    System.out.println(printer.header());
  }
}