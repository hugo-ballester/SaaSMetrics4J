/*
 *    SaaSMetrics4J : https://github.com/hugozaragoza/SaaSMetrics4J
 *
 *    (c) 2014, Hugo Zaragoza, Websays.
 */
package websays.accounting.app;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import org.apache.log4j.Logger;

import websays.accounting.BillingReportPrinter;
import websays.accounting.Contracts;
import websays.accounting.Contracts.AccountFilter;
import websays.accounting.PrinterASCII;
import websays.accounting.Reporting;
import websays.core.utils.TimeWebsays;

public class MyMiniReports extends BasicCommandLineApp {
  
  private static final Logger logger = Logger.getLogger(MyMiniReports.class);
  
  // dont change timezone here, change default instead, since many use this.
  final static TimeWebsays cal = new TimeWebsays(Locale.getDefault(), TimeZone.getDefault());
  
  private static final int MONTHS = 8;
  Calendar c = cal.getCalendar(new Date());
  
  static BillingReportPrinter printer = new PrinterASCII();
  
  public MyMiniReports(String[] args) throws Exception {
    c.add(Calendar.MONTH, 1);
    int thisYear = c.get(Calendar.YEAR);
    int thisMonth = c.get(Calendar.MONTH) + 1;
    logger.info("SaaSMetrics4j - MiniReports v1");
    init(args);
    Contracts contracts = initContracts();
    
    Contracts contAll = contracts.getView(AccountFilter.CONTRACTED_OR_PROJECT);
    Contracts contCont = contracts.getView(AccountFilter.CONTRACT);
    boolean metricDate = false;
    
    SimpleDateFormat sdf1 = new SimpleDateFormat("yyyy-MM-dd ('W'w)");
    sdf1.setTimeZone(TimeZone.getTimeZone("Europe/Madrid"));
    String date = sdf1.format(new Date());
    
    for (String action : actions) {
      
      if (action.equals("mrr")) {
        String ret = line2 + "MRR REPORT " + date + line2;
        
        ret += line1 + "ALL (CONTRACTS + PROJECTS)\n\n";
        ret += Reporting.displayLastMRR(contAll, thisYear, thisMonth, MONTHS, metricDate) + "\n";
        
        ret += line1 + "CONTRACTS only (projects removed):\n\n";
        ret += Reporting.displayLastMRR(contCont, thisYear, thisMonth, MONTHS, metricDate) + "\n";
        
        ret += line2;
        
        if (email != null) {
          email("Mini MRR Report " + date, ret);
          logger.info("MRR Report e-mail sent");
        }
        System.out.println(ret);
        
      }
    }
  }
  
}