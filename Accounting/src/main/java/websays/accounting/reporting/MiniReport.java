/*
 *    SAS4J
 *
 *    Hugo Zaragoza, Websays.
 */
package websays.accounting.reporting;

import org.joda.time.YearMonth;

import websays.accounting.BillingReportPrinter;
import websays.accounting.Contracts;
import websays.accounting.Contracts.AccountFilter;
import websays.accounting.Reporting;

public class MiniReport {
  
  public static String miniReport(Contracts contracts, BillingReportPrinter printer, int year, int monthStart, int months) throws Exception {
    StringBuffer sb = new StringBuffer();
    Contracts contAll = contracts.getView(AccountFilter.PAID_CONTRACT);
    Contracts contCont = contracts.getView(AccountFilter.CONTRACT);
    YearMonth ym = (new YearMonth()).minusMonths(1);
    
    sb.append(printer.line() + "ALL (CONTRACTS + PROJECTS)\n\n");
    sb.append(Reporting.displayLastMRR(contAll, year, monthStart, months, ym) + "\n");
    
    sb.append(printer.line() + "CONTRACTS only (projects removed):\n\n");
    sb.append(Reporting.displayLastMRR(contCont, year, monthStart, months, ym) + "\n");
    
    sb.append(printer.line());
    return sb.toString();
    
  }
}
