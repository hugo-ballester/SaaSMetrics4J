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
import java.util.HashMap;
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
import websays.core.utils.CurrencyUtils;
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
  private static TimeWebsays calendar = new TimeWebsays(Locale.getDefault(), TimeZone.getDefault());
  
  public static final SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yy");
  
  public BillingReportPrinter printer;
  CSVMetricsReport reporter = new CSVMetricsReport(); // for now hardcoded here
  
  public boolean showInvoicesHeadlineWhenNone = true;
  
  public Reporting(BillingReportPrinter printer) {
    this.printer = printer;
  }
  
  public String displayContracts(Date d, AccountFilter filter, boolean metricDate, boolean colheader, Contracts contracts) {
    StringBuffer sb = new StringBuffer();
    
    if (contracts == null) {
      String err = "ERROR: NULL contracts?";
      System.err.println(err);
      return err;
    }
    sb.append("CONTRACTS  (" + filter.toString() + ") at " + MonthlyMetrics.df.format(d) + "\n");
    Contracts cs = contracts.getActive(d, filter, metricDate);
    cs.sort(SortType.client); // cs.sort(SortType.contract);
    double totM = 0, totCom = 0;
    int totC = 0;
    if (colheader) {
      sb.append(String.format("%4s %-20s %-20s %-12s\t%-11s\t%s\t%s    \t%s-%s\n", //
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
      sb.append(String.format("%4d %-20s %-20s %10.2f\t%9.2f\t%s\t%s\t%s-%s\n", //
          c.getId(), c.name, c.client_name, mrr, commission, c.type, c.billingSchema, startS, endS));
      totM += mrr;
      totCom += commission;
      totC++;
    }
    sb.append(String.format("%4d %-20s %-20s %10.2f\t%9.2f\n", totC, "TOTAL", "", totM, totCom));
    return sb.toString();
    
  }
  
  @SuppressWarnings("unused")
  private void displayClientMRR(String clientName, Date start, Date end, Contracts contracts) throws ParseException, SQLException {
    
    System.out.println("displayClientMRR   : " + (clientName));
    ArrayList<Contract> lis = new ArrayList<Contract>();
    for (Contract c : contracts) {
      if (c.client_name.equals(clientName)) {
        lis.add(c);
      }
    }
    
  }
  
  /**
   * @param year
   * @param month
   *          1-12
   * @throws ParseException
   * @throws SQLException
   */
  public String displayBilling(int year, int month, Contracts contracts) throws ParseException, SQLException {
    
    if (contracts == null) {
      String err = "ERROR: displayBilling: NULL contracts?";
      System.err.println("ERROR: displayBilling: NULL contracts?");
      return err;
    }
    
    StringBuffer sb = new StringBuffer();
    BillingReportPrinter p = new PrinterASCII();
    ArrayList<Bill> bs = Billing.bill(contracts, year, month);
    
    if (bs.size() > 0) {
      Date billingDate = bs.get(0).date;
      sb.append(printer.subtitle("INVOICES. " + sdf.format(billingDate)));
      String invoices = p.printBills(bs, false);
      sb.append(invoices);
    } else if (showInvoicesHeadlineWhenNone) {
      String invoices = p.printBills(bs, false);
      sb.append(invoices);
    }
    return sb.toString();
  }
  
  public static String displayClientMRR(Date date, AccountFilter filter, boolean metricDate, Contracts contracts) throws ParseException,
      SQLException {
    StringBuffer sb = new StringBuffer();
    
    sb.append("displayClientMRR   : " + (filter == null ? "ALL" : filter.toString()) + " " + MonthlyMetrics.df.format(date) + "\n");
    if (contracts == null) {
      String err = "ERROR: NULL contracts?";
      System.err.println(err);
      return err;
    }
    
    Contracts lis = contracts.getActive(date, filter, metricDate);
    lis.sort(SortType.client);
    
    String lastN = null;
    double sum = 0., summ = 0.;
    int count = 0, countt = 0, clientN = 1;
    sb.append(String.format("%3s %-20s\t%s\t%s\n", "#", "CLIENT", "Conts.", "MRR"));
    
    for (int i = 0; i < lis.size(); i++) {
      Contract c = lis.get(i);
      double mrr = Metrics.computeMRR(c, date, metricDate);
      
      if (lastN == null) {
        lastN = c.client_name;
      } else if ((i == lis.size() - 1) || (!lastN.equals(c.client_name))) {
        sb.append(String.format("%3d %-20s\t(%d)\t%10.2f\n", clientN, lastN, count, sum));
        if (sum == 0) {
          sb.append("\n!!! ERROR: 0 MRR for Client: " + lastN + "\n");
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
    
    sb.append(String.format("%3d %-20s\t(%d)\t%10.2f", clientN, lastN, count, sum));
    
    sb.append(String.format("%3s %-20s\t(%d)\t%10.2f", "", "TOTAL", countt, summ));
    
    return sb.toString();
  }
  
  public static String displayLastMRR(Contracts contracts, int yearStart, int monthEnd, int months) throws ParseException {
    HashMap<String,MonthlyMetrics> cache = new HashMap<String,MonthlyMetrics>();
    String header = "MONTH:\t\t";
    String line1 = "\nAvg. Delta MRR (k€):";
    String line2 = "\n     Delta MRR (k€):";
    String line3 = "\n           MRR (k€):";
    ;
    int month = monthEnd;
    int year = yearStart;
    int WINDOW = 6;
    for (int i = 0; i < months; i++) {
      
      double avg = 0;
      for (int j = WINDOW; j > 0; j--) {
        
        int m = month - j + 1;
        int y = year;
        if (m < 1) {
          m += 12;
          y--;
        }
        
        int m2 = m - 1;
        int y2 = y;
        if (m2 == 0) {
          m2 = 12;
          y2--;
        }
        
        String label = y + "-" + m;
        String label2 = y2 + "-" + m2;
        
        MonthlyMetrics met1 = cache.get(label);
        if (cache.get(label) == null) {
          met1 = MonthlyMetrics.compute(y, m, contracts);
          cache.put(label, met1);
        }
        
        MonthlyMetrics met2 = cache.get(label2);
        if (cache.get(label2) == null) {
          met2 = MonthlyMetrics.compute(y2, m2, contracts);
          cache.put(label2, met2);
        }
        
        double value1 = (met1.mrr - met2.mrr);
        avg += value1;
        // System.out.println(label + "  -  " + label2 + "\t" + value1 + "\t" + avg);
        
      }
      avg /= WINDOW;
      // System.out.println(avg);
      
      String label = year + "-" + month;
      
      int m2 = month - 1;
      int y2 = year;
      if (m2 == 0) {
        m2 = 12;
        y2--;
      }
      String label2 = y2 + "-" + m2;
      
      MonthlyMetrics met2 = cache.get(label2);
      MonthlyMetrics met1 = cache.get(label);
      double v1 = avg / 1000;
      double v2 = (met1.mrr - met2.mrr) / 1000;
      double v3 = met1.mrr / 1000;
      line1 += (String.format("\t%6.1f", v1));
      line2 += (String.format("\t%6.1f", v2));
      line3 += (String.format("\t%6.1f", v3));
      header += String.format("\t%6s", label);
      
      if (--month == 0) {
        month = 12;
        year--;
      }
    }
    
    return header + "\n" + line1 + "\n" + line2 + "\n" + line3;
  }
  
  public String displayMetrics(int yearStart, int monthStart, int months, AccountFilter filter, boolean completeDetail,
      Contracts allContracts) throws ParseException, SQLException {
    
    Contracts contracts = allContracts;
    if (filter != null) {
      contracts = allContracts.getView(filter);
    }
    
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
      
      MonthlyMetrics m = MonthlyMetrics.compute(year, month, contracts);
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
   * Print new contracts, in reverse chronological order, for all history
   * 
   * @param onlyFirstOfEachClient
   *          show only contracts for new clients
   * @return
   */
  public static String report_last(BillingReportPrinter printer, boolean onlyFirstOfEachClient, Contracts contracts) {
    // sort by reverse date
    contracts.sort(SortType.date_ASC);
    
    LinkedList<String> lis = new LinkedList<String>(); // used to revert order of print (newest at the top)
    HashSet<String> clients = new HashSet<String>(); // used to remove non-first when onlyFirstOfEachClient
    String format = "%4s\t%30s\t%20s\t%10s\t%-25s";
    String line;
    int lastmonth = -1, lastyear = -1;
    double total = 0;
    
    for (Contract c : contracts) {
      if (onlyFirstOfEachClient && clients.contains(c.client_name)) { // skip
        continue;
      }
      int month = calendar.getMonth(c.startContract) + 1;
      if (lastmonth < 0) { // init
        lastmonth = month;
      } else if (month != lastmonth) { // compute sum of previous month before starting
        String monthStr = "MONTH " + lastyear + "-" + lastmonth;
        line = String.format("\n" + format, "", "", "TOTAL:", BillingReportPrinter.money(total, true, CurrencyUtils.EUR), monthStr);
        lis.push(line);
        total = 0.;
        lastmonth = month;
        lastyear = calendar.getYear(c.startContract);
      }
      
      clients.add(c.client_name);
      double mrr = c.getMonthlyPrize(c.startContract, true, false, false);
      
      line = String.format(format, //
          toStringShort_commissionees(c), //
          (c.name == null ? "" : c.name), //
          ("(" + (c.client_name == null ? "" : c.client_name) + ")"), //
          BillingReportPrinter.money(mrr, true, c.currency), printer.stringPeriod(c) //
          );
      // c.endContract != null ? sdf.format(c.endContract) :
      
      lis.push(line); // oldest to the bottom
      total += CurrencyUtils.toEuros(mrr, c.currency);
    }
    String monthStr = "MONTH " + lastyear + "-" + lastmonth;
    line = String.format("\n" + format, "", "", "TOTAL:", BillingReportPrinter.money(total, true, CurrencyUtils.EUR), monthStr);
    lis.push(line);
    
    return StringUtils.join(lis, "\n");
    
  }
  
  /**
   * Print commissions
   * 
   * @return
   */
  public static String report_comm(int year, String[] names, Contracts contracts) {
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
      double tot = 0;
      for (String s : names) {
        Double c = mapMonth.get(s);
        tot += c;
        row.add(PrinterASCII.euros(c));
      }
      row.add(PrinterASCII.euros(tot));
      rows.add(join(row));
      
    }
    
    ret.append(String.format("%10s", "month") + "\t" + join(Arrays.asList(names)) + String.format("%10s", "TOTAL") + "\n");
    
    ret.append(StringUtils.join(rows, "\n"));
    
    ret.append("\n");
    ret.append(String.format("\n%10s", "TOTAL"));
    double total = 0;
    for (String s : names) {
      Double c = mapYear.get(s);
      total += c;
      ret.append(String.format("\t%10s", PrinterASCII.euros(c)));
    }
    ret.append(String.format("\t%10s", PrinterASCII.euros(total)));
    
    return ret.toString();
  }
  
  private static String join(Collection<String> names) {
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
  
  private static String toStringShort_commissionees(Contract c) {
    String ret = "";
    for (Commission com : c.commission) {
      ret += (com == null ? "" : com.commissionnee) + ", ";
    }
    return printAndStripLastComma(ret);
  }
  
  public String displayTotals(Date date, AccountFilter filter, boolean metricDate, Contracts contracts) throws ParseException, SQLException {
    StringBuffer sb = new StringBuffer();
    sb.append("TOTALS  (" + filter.toString() + ") at " + MonthlyMetrics.df.format(date) + "\n");
    if (contracts == null) {
      System.err.println("ERROR: NULL contracts?");
      return null;
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
    
    sb.append("  Active Contracts:\t" + lis.size() + "\n");
    sb.append("  Active Clients:\t" + clients.size() + "\n");
    sb.append("  Total MRR:     \t" + PrinterASCII.euros(totMRR, true) + "\n");
    
    return sb.toString();
  }
  
  public String displayEndingSoon(Date date, AccountFilter filter, Contracts contracts) {
    StringBuffer sb = new StringBuffer();
    
    Contracts lis = contracts.getActive(date, filter, false);
    for (int i = 0; i < lis.size(); i++) {
      Contract c = lis.get(i);
      int left = c.getMonthsRemaining(date);
      if (left == 0) {
        sb.append("CONTRACT ENDING NOW   : " + c.name + "\t" + printer.stringPeriod(c) + "\n");
      } else if (left < 0) {
        sb.append("CONTRACT ENDED ALREADY: " + c.name + "\t" + printer.stringPeriod(c) + "\n");
      } else if (left < 3) {
        sb.append("CONTRACT ENDING SOON  : " + c.name + "\t" + printer.stringPeriod(c) + "\n");
      }
      
    }
    return sb.toString();
  }
}
