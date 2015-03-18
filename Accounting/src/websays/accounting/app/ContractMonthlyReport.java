/*
 *    SaaSMetrics4J : https://github.com/hugozaragoza/SaaSMetrics4J
 *
 *    (c) 2014, Hugo Zaragoza, Websays.
 */
package websays.accounting.app;

import java.sql.SQLException;
import java.text.ParseException;
import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;

import websays.accounting.BillingReportPrinter;
import websays.accounting.Contract;
import websays.accounting.Contracts;
import websays.accounting.PrinterASCII;
import websays.accounting.Reporting;
import websays.core.utils.TimeWebsays;

public class ContractMonthlyReport extends BasicCommandLineApp {
  
  final int M = Calendar.MONTH;
  final int Y = Calendar.YEAR;
  BillingReportPrinter printer = new PrinterASCII();
  private TimeWebsays calendar = new TimeWebsays(Locale.getDefault(), TimeZone.getDefault());
  
  public static void main(String[] args) throws Exception {
    System.out.println("<head><meta charset=\"UTF-8\"></head>");
    
    init(args);
    if (contractID == null) {
      System.err.println("ERROR You much define --contract contractID");
      return;
    }
    
    ContractMonthlyReport cmr = new ContractMonthlyReport();
    cmr.report(contractID);
  }
  
  void report(int contractID) throws Exception {
    
    Contracts contracts = initContracts();
    Contract c = contracts.getContract(contractID);
    if (c == null) {
      System.err.println("ERROR: NO CONTRACT FOUND WITH contractID=" + contractID);
      return;
    }
    
    report(c);
  }
  
  private void report(Contract c) throws ParseException, SQLException {
    Calendar cS = calendar.getCalendar(c.startContract);
    Calendar cE = (Calendar) cS.clone();
    if (c.endContract != null) {
      cE = calendar.getCalendar(c.endContract);
    }
    cS.add(Calendar.MONTH, -3);
    cE.add(Calendar.MONTH, +3);
    
    report(c, cS.get(Y), cS.get(M) + 1, cE.get(Y), cE.get(M) + 1);
    if (c.endContract == null) {
      System.out.println("... and continues forever ...");
    }
  }
  
  public void report(Contract c, int yearStart, int monthStart_srartAt1, int yearEnd, int monthEnd_startAt1) throws ParseException,
      SQLException {
    Calendar cal = calendar.getCalendar(yearStart, monthStart_srartAt1 - 1, 1);
    int m, y;
    Contracts contracts = new Contracts();
    contracts.add(c);
    Reporting r = new Reporting(printer);
    r.showInvoicesHeadlineWhenNone = false;
    
    do {
      m = cal.get(M) + 1;
      y = cal.get(Y);
      r.displayBilling(y, m, contracts);
      cal.add(M, 1);
    } while (!(y >= yearEnd && m == monthEnd_startAt1));
    
  }
  
}
