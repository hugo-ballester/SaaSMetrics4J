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
import org.joda.time.Months;

import websays.accounting.BillingReportPrinter;
import websays.accounting.Contract;
import websays.accounting.Contracts;
import websays.accounting.PrinterHTML;
import websays.accounting.Reporting;

public class ContractMonthlyReport extends BasicCommandLineApp {
  
  final int M = Calendar.MONTH;
  final int Y = Calendar.YEAR;
  BillingReportPrinter printer = new PrinterHTML();
  
  public static void main(String[] args) throws Exception {
    StringBuffer sb = new StringBuffer();
    sb.append("<head><meta charset=\"UTF-8\"></head>");
    
    init(args);
    if (contractID == null) {
      System.err.println("ERROR You much define --contract contractID");
      return;
    }
    
    ContractMonthlyReport cmr = new ContractMonthlyReport();
    sb.append(cmr.report(contractID));
    System.out.println(sb.toString());
  }
  
  String report(int contractID) throws Exception {
    
    Contracts contracts = initContracts();
    Contract c = contracts.getContract(contractID);
    if (c == null) {
      return "ERROR: NO CONTRACT FOUND WITH contractID=" + contractID;
    }
    
    return report(c);
  }
  
  String report(Contract c) throws ParseException, SQLException {
    LocalDate cS = c.startContract;
    int months = 12;
    if (c.endContract != null) {
      months = Months.monthsBetween(c.startContract, c.endContract).getMonths() + 1;
    } else if (c.contractedMonths != null) {
      months = c.contractedMonths;
    }
    
    String ret = report(c, cS, months);
    if (c.endContract == null) {
      ret += "... and continues forever ...\n";
    }
    return ret;
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
  public String report(Contract c, LocalDate cS, int months) throws ParseException, SQLException {
    StringBuffer sb = new StringBuffer();
    LocalDate cal = new LocalDate(cS);
    
    Contracts contracts = new Contracts();
    contracts.add(c);
    Reporting r = new Reporting(printer);
    r.showInvoicesHeadlineWhenNone = false;
    
    for (int m = 0; m < months; m++) {
      sb.append(r.displayBilling(cal.getYear(), cal.getMonthOfYear(), contracts) + "\n");
      cal = cal.plusMonths(1);
    }
    return sb.toString();
  }
}
