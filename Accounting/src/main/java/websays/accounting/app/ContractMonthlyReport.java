/*
 *    SaaSMetrics4J : https://github.com/hugozaragoza/SaaSMetrics4J
 *
 *    (c) 2014, Hugo Zaragoza, Websays.
 */
package websays.accounting.app;

import java.sql.SQLException;
import java.text.ParseException;
import java.util.Calendar;

import org.joda.time.LocalDate;

import websays.accounting.BillingReportPrinter;
import websays.accounting.Contract;
import websays.accounting.Contracts;
import websays.accounting.PrinterASCII;
import websays.accounting.Reporting;

public class ContractMonthlyReport extends BasicCommandLineApp {
  
  final int M = Calendar.MONTH;
  final int Y = Calendar.YEAR;
  BillingReportPrinter printer = new PrinterASCII();
  
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
    LocalDate cS = c.startContract;
    LocalDate cE = cS;
    if (c.endContract != null) {
      cE = c.endContract;
    }
    cS.plusMonths(-3);
    cS.plusMonths(+3);
    
    report(c, cS.getYear(), cS.getMonthOfYear(), cE.getYear(), cE.getMonthOfYear());
    if (c.endContract == null) {
      System.out.println("... and continues forever ...");
    }
  }
  
  /**
   * @param c
   * @param yearStart
   * @param month
   *          1-12
   * @param yearEnd
   * @param monthEnd_startAt1
   *          1-12
   * @throws ParseException
   * @throws SQLException
   */
  public void report(Contract c, int yearStart, int month, int yearEnd, int monthEnd_startAt1) throws ParseException, SQLException {
    LocalDate cal = new LocalDate(yearStart, month, 1);
    int m, y;
    Contracts contracts = new Contracts();
    contracts.add(c);
    Reporting r = new Reporting(printer);
    r.showInvoicesHeadlineWhenNone = false;
    
    do {
      m = cal.getMonthOfYear();
      y = cal.getYear();
      r.displayBilling(y, m, contracts);
      cal.plusMonths(1);
    } while (!(y >= yearEnd && m == monthEnd_startAt1));
    
  }
  
}
