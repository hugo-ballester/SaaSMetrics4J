/*
 *    SaaSMetrics4J : https://github.com/hugozaragoza/SaaSMetrics4J
 *
 *    (c) 2014, Hugo Zaragoza, Websays.
 */
package websays.accounting.app;

import org.apache.log4j.Logger;
import org.joda.time.LocalDate;

import websays.accounting.BillingReportPrinter;
import websays.accounting.Contracts;
import websays.accounting.GlobalConstants;
import websays.accounting.PrinterHTML;
import websays.accounting.reporting.MiniReport;

public class MyMiniReports extends BasicCommandLineApp {
  
  private static final Logger logger = Logger.getLogger(MyMiniReports.class);
  
  static BillingReportPrinter printer = new PrinterHTML();
  
  public static void main(String[] args) throws Exception {
    new MyMiniReports(args);
  }
  
  public MyMiniReports(String[] args) throws Exception {
    
    LocalDate c = (new LocalDate()).plusMonths(-8);
    String nowStr = GlobalConstants.dtS.print(new LocalDate());
    
    logger.info("SaaSMetrics4j - MiniReports v1");
    init(args);
    if (actions == null || actions.length == 0) {
      System.err.println("YOU NEED A --do parameter with {mrr}");
      return;
    }
    Contracts contracts = initContracts();
    
    for (String action : actions) {
      
      if (action.equals("mrr")) {
        String ret = printer.line() + "MRR REPORT " + nowStr + printer.line();
        
        ret += MiniReport.miniReport(contracts, printer, c.getYear(), c.getMonthOfYear(), 12);
        
        if (email != null) {
          email("Mini MRR Report " + nowStr, ret);
          logger.info("MRR Report e-mail sent");
        } else {
          System.out.println(ret);
        }
        
      }
    }
  }
  
}