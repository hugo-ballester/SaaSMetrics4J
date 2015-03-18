/*
 *    SaaSMetrics4J : https://github.com/hugozaragoza/SaaSMetrics4J
 *
 *    (c) 2014, Hugo Zaragoza, Websays.
 */
package websays.accounting.app;

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
  
  private static final int MONTHS = 6;
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
    
    for (String action : actions) {
      
      if (action.equals("mrr")) {
        String ret = line2 + "MRR REPORT" + line2;
        ret += "ALL (CONTRACTS + PROJECTS)\n\n";
        
        ret += Reporting.displayLastMRR(contAll, thisYear, thisMonth, MONTHS) + "\n";
        ret += line1 + "CONTRACTS only (projects removed):\n\n";
        ret += Reporting.displayLastMRR(contCont, thisYear, thisMonth, MONTHS) + "\n";
        ret += line2;
        
        System.out.println(ret);
        
        if (super.email != null) {
          email("MRR Report", ret);
        }
      }
    }
  }
  
  public static void main(String[] args) throws Exception {
    new MyMiniReports(args);
  }
}