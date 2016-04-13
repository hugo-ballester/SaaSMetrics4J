/*
 *    SaaSMetrics4J : https://github.com/hugozaragoza/SaaSMetrics4J
 *
 *    (c) 2014, Hugo Zaragoza, Websays.
 */
package websays.accounting.app;

import java.sql.SQLException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map.Entry;
import java.util.TreeSet;

import org.joda.time.LocalDate;
import org.joda.time.Months;

import websays.accounting.Bill;
import websays.accounting.Billing;
import websays.accounting.BillingReportPrinter;
import websays.accounting.Contract;
import websays.accounting.Contracts;
import websays.accounting.PrinterHTML;
import websays.accounting.Reporting;
import websays.core.utils.CurrencyUtils;

public class ContractMonthlyReport extends BasicCommandLineApp {
  
  final int M = Calendar.MONTH;
  final int Y = Calendar.YEAR;
  BillingReportPrinter printer = new PrinterHTML();
  
  public static void main(String[] args) throws Exception {
    StringBuffer sb = new StringBuffer();
    sb.append("<head><meta charset=\"UTF-8\"></head>");
    
    init(args);
    ContractMonthlyReport cmr = new ContractMonthlyReport();
    sb.append(cmr.report(contractID));
    System.out.println(sb.toString());
    
  }
  
  public String report(Integer contractID) throws Exception {
    Contracts contracts = initContracts();
    if (contractID != null) {
      Contract c = contracts.getContract(contractID);
      if (c == null) {
        return "ERROR: NO CONTRACT FOUND WITH contractID=" + contractID;
      }
      return oneContractReport(c);
    } else {
      return allContractReport(contracts, 2016);
    }
    
  }
  
  String oneContractReport(Contract c) throws ParseException, SQLException {
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
  
  public String allContractReport(Contracts contracts, int year) throws ParseException, SQLException {
    TreeSet<String> clients = new TreeSet<String>();
    ArrayList<HashMap<String,Double>> billed = new ArrayList<HashMap<String,Double>>();
    StringBuffer sb = new StringBuffer();
    
    for (int i = 1; i <= 12; i++) {
      HashMap<String,Double> map = allContractReport(contracts, year, i);
      billed.add(map);
      clients.addAll(map.keySet());
    }
    
    sb.append(String.format("\n\n%20s\t", "CLIENT"));
    ArrayList<Double> totCols = new ArrayList<Double>();
    for (int i = 0; i <= 11; i++) {
      sb.append(String.format("%9d\t", i + 1));
      totCols.add(0.0);
    }
    sb.append(String.format("%9s\n", "Tot"));
    
    for (String s : clients) {
      sb.append(String.format("%20s\t", s));
      Double totRow = 0.0;
      for (int i = 0; i <= 11; i++) {
        Double d = billed.get(i).get(s);
        d = d == null ? 0.0 : d;
        totRow += d;
        totCols.set(i, totCols.get(i) + d);
        sb.append(String.format("%9.2f\t", d));
      }
      sb.append(String.format("%9.2f\n", totRow != null ? totRow : 0.0));
    }
    
    sb.append(String.format("\n\n%20s\t", "Total"));
    double tott = 0.0;
    for (int i = 0; i <= 11; i++) {
      tott += totCols.get(i);
      sb.append(String.format("%9.2f\t", totCols.get(i)));
    }
    sb.append(String.format("%9.2f\n", tott));
    
    return sb.toString();
  }
  
  HashMap<String,Double> allContractReport(Contracts contracts, int year, int month) throws ParseException, SQLException {
    HashMap<String,Double> map = new LinkedHashMap<String,Double>();
    
    ArrayList<Bill> bs = Billing.bill(contracts, year, month);
    for (Bill b : bs) {
      String key = b.clientName;
      Double fee = b.getTotalFee();
      if (!b.currency.equals(CurrencyUtils.EUR)) {
        fee = CurrencyUtils.convert(fee, b.currency, CurrencyUtils.EUR);
      }
      Double tot = map.get(key);
      tot = (tot == null ? 0 : tot) + fee;
      map.put(key, tot);
    }
    
    // remove all at 0
    for (Iterator<Entry<String,Double>> it = map.entrySet().iterator(); it.hasNext();) {
      Entry<String,Double> entry = it.next();
      if (entry.getValue() == 0.0) {
        it.remove();
      }
    }
    
    return map;
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
