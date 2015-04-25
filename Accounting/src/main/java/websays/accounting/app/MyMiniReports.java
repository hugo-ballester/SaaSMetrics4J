/*
 *    SaaSMetrics4J : https://github.com/hugozaragoza/SaaSMetrics4J
 *
 *    (c) 2014, Hugo Zaragoza, Websays.
 */
package websays.accounting.app;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import org.apache.log4j.Logger;
import org.joda.time.LocalDate;

import websays.accounting.BillingReportPrinter;
import websays.accounting.Contracts;
import websays.accounting.Contracts.AccountFilter;
import websays.accounting.PrinterASCII;
import websays.accounting.Reporting;

public class MyMiniReports extends BasicCommandLineApp {
  
  private static final Logger logger = Logger.getLogger(MyMiniReports.class);
  
  private static final int MONTHS = 8;
  LocalDate c;
  
  static BillingReportPrinter printer = new PrinterASCII();
  
  public MyMiniReports(String[] args) throws Exception {
    c = (new LocalDate()).plusMonths(1);
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
        ret += Reporting.displayLastMRR(contAll, c.getYear(), c.getMonthOfYear(), MONTHS, metricDate) + "\n";
        
        ret += line1 + "CONTRACTS only (projects removed):\n\n";
        ret += Reporting.displayLastMRR(contCont, c.getYear(), c.getMonthOfYear(), MONTHS, metricDate) + "\n";
        
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