/*
 *    SaaSMetrics4J : https://github.com/hugozaragoza/SaaSMetrics4J
 *
 *    (c) 2014, Hugo Zaragoza, Websays.
 */
package websays.accounting;

import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Locale;
import java.util.TimeZone;

import org.apache.commons.collections4.map.DefaultedMap;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

import websays.accounting.Contracts.AccountFilter;
import websays.accounting.Contracts.SortType;
import websays.accounting.metrics.Metrics;
import websays.accounting.reporting.CSVMetricsReport;
import websays.core.utils.TimeWebsays;

/**
 * Functions to display contracts and bills in different ways...
 * 
 * @author hugoz
 * 
 */
public class Reporting {
  
  @SuppressWarnings("unused")
  private static final Logger logger = Logger.getLogger(Reporting.class);
  private TimeWebsays calendar = new TimeWebsays(Locale.getDefault(), TimeZone.getDefault());
  
  Contracts contracts;
  
  public static final SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yy");
  
  private BillingReportPrinter printer;
  CSVMetricsReport reporter = new CSVMetricsReport(); // for now hardcoded here
  
  public boolean showInvoicesHeadlineWhenNone = true;
  
  public Reporting(Contracts contracts, BillingReportPrinter printer) {
    this.contracts = contracts;
    this.printer = printer;
  }
  
  public void displayContracts(Date d, AccountFilter filter, boolean metricDate, boolean colheader) {
    if (contracts == null) {
      System.err.println("ERROR: NULL contracts?");
      return;
    }
    System.out.println("CONTRACTS  (" + filter.toString() + ") at " + MonthlyMetrics.df.format(d) + "\n");
    Contracts cs = contracts.getActive(d, filter, metricDate);
    cs.sort(SortType.client); // cs.sort(SortType.contract);
    double totM = 0, totCom = 0;
    int totC = 0;
    if (colheader) {
      System.out.println(String.format("%4s %-20s %-20s %-12s\t%-11s\t%s\t%s    \t%s-%s", //
          "ID", "Contract Name", "Client Name", "MRR", "Commission", "Type     ", "Billing", "start", "end"));
    }
    
    for (Contract c : cs) {
      String endS = "", startS = "";
      if (metricDate) {
        startS = sdf.format(c.startRoundDate);
      } else {
        startS = sdf.format(c.startContract);
      }
      
      if (c.endContract != null) {
        if (metricDate) {
          endS = sdf.format(c.endRoundDate);
        } else {
          endS = sdf.format(c.endContract);
        }
      }
      
      double mrr = Metrics.computeMRR(c, d, metricDate);
      double commission = Metrics.computeCommission(c, d, metricDate);
      System.out.println(String.format("%4d %-20s %-20s %10.2f\t%9.2f\t%s\t%s\t%s-%s", c.getId(), c.name, c.client_name, mrr, commission,
          c.type, c.billingSchema, startS, endS));
      totM += mrr;
      totCom += commission;
      totC++;
    }
    System.out.println(String.format("%4d %-20s %-20s %10.2f\t%9.2f", totC, "TOTAL", "", totM, totCom));
    System.out.println();
    
  }
  
  @SuppressWarnings("unused")
  private void displayClientMRR(String clientName, Date start, Date end) throws ParseException, SQLException {
    
    System.out.println("displayClientMRR   : " + (clientName));
    ArrayList<Contract> lis = new ArrayList<Contract>();
    for (Contract c : contracts) {
      if (c.client_name.equals(clientName)) {
        lis.add(c);
      }
    }
    
  }
  
  public void displayBilling(int year, int month) throws ParseException, SQLException {
    if (contracts == null) {
      System.err.println("ERROR: displayBilling: NULL contracts?");
      return;
    }
    
    BillingReportPrinter p = new PrinterASCII();
    
    ArrayList<Bill> bs = Billing.bill(contracts, year, month);
    
    // System.out.println(p.line + "SUMMARY\n" + p.line);
    // System.out.println(p.printBills(bs, true));
    
    if (bs.size() > 0 || showInvoicesHeadlineWhenNone) {
      Date billingDate = bs.get(0).date;
      System.out.println(printer.subtitle("INVOICES. " + sdf.format(billingDate)));
      String invoices = p.printBills(bs, false);
      System.out.println(invoices);
    }
  }
  
  public void displayClientMRR(Date date, AccountFilter filter, boolean metricDate) throws ParseException, SQLException {
    System.out
        .println("displayClientMRR   : " + (filter == null ? "ALL" : filter.toString()) + " " + MonthlyMetrics.df.format(date) + "\n");
    if (contracts == null) {
      System.err.println("ERROR: NULL contracts?");
      return;
    }
    
    Contracts lis = contracts.getActive(date, filter, metricDate);
    lis.sort(SortType.client);
    
    String lastN = null;
    double sum = 0., summ = 0.;
    int count = 0, countt = 0, clientN = 1;
    System.out.println(String.format("%3s %-20s\t%s\t%s", "#", "CLIENT", "Conts.", "MRR"));
    
    for (int i = 0; i < lis.size(); i++) {
      Contract c = lis.get(i);
      double mrr = Metrics.computeMRR(c, date, metricDate);
      
      if (lastN == null) {
        lastN = c.client_name;
      } else if ((i == lis.size() - 1) || (!lastN.equals(c.client_name))) {
        System.out.println(String.format("%3d %-20s\t(%d)\t%10.2f", clientN, lastN, count, sum));
        if (sum == 0) {
          System.out.println("\n!!! ERROR: 0 MRR for Client: " + lastN + "\n");
        }
        sum = 0;
        count = 0;
        lastN = c.client_name;
        clientN++;
      }
      sum += mrr;
      summ += mrr;
      count++;
      countt++;
      // System.out.println(String.format("%20s\t%s\t%.2f", c.name,
      // c.client, mrr));
    }
    
    System.out.println(String.format("%3d %-20s\t(%d)\t%10.2f", clientN, lastN, count, sum));
    
    System.out.println(String.format("%3s %-20s\t(%d)\t%10.2f", "", "TOTAL", countt, summ));
    
  }
  
  public String displayMetrics(int yearStart, int monthStart, int months, AccountFilter filter, boolean completeDetail)
      throws ParseException, SQLException {
    
    StringBuffer sb = new StringBuffer();
    sb.append("displayAll   : " + filter.toString() + "\n");
    sb.append("     \t" + reporter.headersTop(completeDetail) + "\n");
    sb.append("month\t" + reporter.headers(completeDetail) + "\n");
    int year = yearStart;
    int month = monthStart - 1;
    
    MonthlyMetrics old = new MonthlyMetrics();
    MonthlyMetrics average = new MonthlyMetrics();
    
    for (int i = 0; i < months; i++) {
      month++;
      if (month == 13) {
        month = 1;
        year++;
      }
      
      MonthlyMetrics m = MonthlyMetrics.compute(year, month, filter, contracts);
      if (i == 0) {
        old = m;
      }
      m.setOldValues(old);
      
      if (i > 0) {
        average.addAsNewValueInAverage(m);
      } else {
        average = m;
      }
      
      sb.append("" + year + "/" + month + "\t" + reporter.toStringLine1(m, average, completeDetail) + "\n");
      old = m;
    }
    sb.append("\n");
    return sb.toString();
    
  }
  
  /**
   * Print new contracts, in reverse chronological order
   * 
   * @param onlyFirstOfEachClient
   *          show only contracts for new clients
   * @return
   */
  public String report_last(boolean onlyFirstOfEachClient) {
    // sort by reverse date
    contracts.sort(SortType.date_ASC);
    
    LinkedList<String> lis = new LinkedList<String>(); // used to revert order of print (newest at the top)
    HashSet<String> clients = new HashSet<String>();
    int lastmonth = -1;
    for (Contract c : contracts) {
      if (onlyFirstOfEachClient && clients.contains(c.client_name)) {
        continue;
      }
      double mrr = c.getMonthlyPrize(c.startContract, true, false);
      clients.add(c.client_name);
      if (calendar.getMonth(c.startContract) != lastmonth) {
        lis.push("\n");
        lastmonth = calendar.getMonth(c.startContract);
      }
      String line = String.format("%4s\t%30s\t%20s\t%10s\t%-25s", //
          toStringShort_commissionees(c), //
          (c.name == null ? "" : c.name), //
          ("(" + (c.client_name == null ? "" : c.client_name) + ")"), //
          BillingReportPrinter.money(mrr, true, c.currency), printer.stringPeriod(c) //
          );
      // c.endContract != null ? sdf.format(c.endContract) :
      lis.push(line);
    }
    
    return StringUtils.join(lis, "\n");
    
  }
  
  /**
   * Print commissions
   * 
   * @param onlyFirstOfEachClient
   *          show only contracts for new clients
   * @return
   */
  public String report_comm(int year, String[] names) {
    StringBuffer ret = new StringBuffer();
    DefaultedMap<String,Double> mapMonth = new DefaultedMap<String,Double>(0.0);
    DefaultedMap<String,Double> mapYear = new DefaultedMap<String,Double>(0.0);
    ArrayList<String> rows = new ArrayList<String>(); // to keep them in order for the rows
    
    for (int month = 1; month <= 12; month++) {
      mapMonth.clear();
      
      ArrayList<Bill> bs = Billing.bill(contracts, year, month);
      
      for (Bill b : bs) {
        for (BilledItem bi : b.items) {
          for (CommissionItem c : bi.commissions) {
            if (c.commissionnee != null) {
              mapMonth.put(c.commissionnee, mapMonth.get(c.commissionnee) + c.commission);
              mapYear.put(c.commissionnee, mapYear.get(c.commissionnee) + c.commission);
              
            }
          }
        }
      }
      
      ArrayList<String> row = new ArrayList<String>(names.length + 5);
      row.add(month + "/" + year);
      for (String s : names) {
        row.add(PrinterASCII.euros(mapMonth.get(s)));
      }
      rows.add(join(row));
      
    }
    
    ret.append(String.format("%10s", "month") + "\t" + join(Arrays.asList(names)) + "\n");
    
    ret.append(StringUtils.join(rows, "\n"));
    
    ret.append("\n");
    ret.append(String.format("\n%10s", "Total"));
    for (String s : names) {
      ret.append(String.format("\t%10s", PrinterASCII.euros(mapYear.get(s))));
    }
    
    return ret.toString();
  }
  
  private String join(Collection<String> names) {
    final String format = "%10s";
    StringBuffer ret = new StringBuffer();
    for (String s : names) {
      ret.append(String.format(format + "\t", s));
    }
    return ret.toString();
  }
  
  private static String printAndStripLastComma(String s) {
    if (s == null) {
      return "";
    } else if (s.endsWith(", ")) {
      return s.substring(0, s.length() - 2);
    } else {
      return s;
    }
  }
  
  private String toStringShort_commissionees(Contract c) {
    String ret = "";
    for (Commission com : c.commission) {
      ret += (com == null ? "" : com.commissionnee) + ", ";
    }
    return printAndStripLastComma(ret);
  }
  
  public void displayTotals(Date date, AccountFilter filter, boolean metricDate) throws ParseException, SQLException {
    System.out.println("TOTALS  (" + filter.toString() + ") at " + MonthlyMetrics.df.format(date) + "\n");
    if (contracts == null) {
      System.err.println("ERROR: NULL contracts?");
      return;
    }
    
    Contracts lis = contracts.getActive(date, filter, metricDate);
    HashSet<Integer> clients = new HashSet<Integer>();
    double totMRR = 0;
    
    for (int i = 0; i < lis.size(); i++) {
      Contract c = lis.get(i);
      double mrr = Metrics.computeMRR(c, date, metricDate);
      totMRR += mrr;
      clients.add(c.client_id);
    }
    
    System.out.println("  Active Contracts:\t" + lis.size());
    System.out.println("  Active Clients:\t" + clients.size());
    System.out.println("  Total MRR:     \t" + PrinterASCII.euros(totMRR, true));
    System.out.println();
  }
  
  public void displayEndingSoon(Date date, AccountFilter filter) {
    
    Contracts lis = contracts.getActive(date, filter, false);
    for (int i = 0; i < lis.size(); i++) {
      Contract c = lis.get(i);
      int left = c.getMonthsRemaining(date);
      if (left == 0) {
        System.out.println("CONTRACT ENDING NOW   : " + c.name + "\t" + printer.stringPeriod(c));
      } else if (left < 0) {
        System.out.println("CONTRACT ENDED ALREADY: " + c.name + "\t" + printer.stringPeriod(c));
      } else if (left < 3) {
        System.out.println("CONTRACT ENDING SOON  : " + c.name + "\t" + printer.stringPeriod(c));
      }
      
    }
  }
}
