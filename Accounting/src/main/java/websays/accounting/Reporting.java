/*
 *    SaaSMetrics4J : https://github.com/hugozaragoza/SaaSMetrics4J
 *
 *    (c) 2014, Hugo Zaragoza, Websays.
 */
package websays.accounting;

import java.sql.SQLException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.TreeMap;

import org.apache.commons.collections4.map.DefaultedMap;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import org.apache.log4j.Logger;
import org.joda.time.LocalDate;
import org.joda.time.YearMonth;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import websays.accounting.Contracts.AccountFilter;
import websays.accounting.Contracts.SortType;
import websays.accounting.metrics.Metrics;
import websays.accounting.reporting.CSVMetricsReport;
import websays.core.utils.CurrencyUtils;

/**
 * Functions to display contracts and bills in different ways.
 * 
 * @author hugoz
 * 
 */
public class Reporting {
  
  @SuppressWarnings("unused")
  private static final Logger logger = Logger.getLogger(Reporting.class);
  
  public static final DateTimeFormatter sdf = DateTimeFormat.forStyle("S-");
  
  public BillingReportPrinter printer;
  CSVMetricsReport reporter = new CSVMetricsReport(); // for now hardcoded here
  
  public boolean showInvoicesHeadlineWhenNone = true;
  
  public Reporting(BillingReportPrinter printer) {
    this.printer = printer;
  }
  
  public String displayContracts_header() {
    return String.format("%4s %-20s %-20s %-12s\t%-11s\t%s\t%s    \t%s-%s\n", //
        "ID", "Contract Name", "Client Name", "MRR", "Commission", "Type     ", "Billing", "start", "end");
  }
  
  public String displayContracts(LocalDate d, AccountFilter filter, boolean metricDate, Contracts contracts) {
    StringBuffer sb = new StringBuffer();
    
    if (contracts == null) {
      String err = "ERROR: NULL contracts?";
      System.err.println(err);
      return err;
    }
    sb.append("CONTRACTS  (" + filter.toString() + ") at " + MonthlyMetrics.df.print(d) + "\n");
    sb.append(String.format("%4s\t%-20s\t%-20s"//
        + "\t%-5s\t%4s\t%-5s\t%3s\t"//
        + "%4s\t%8s\t%8s\t%4s\t%8s\n", //
        "id", "contract", "client" //
        , "mrr", "#P", "MRR/p", "comm." //
        , "type", "billing", "start", "months", "end"));
    
    Contracts cs = contracts.getActive(d, filter, metricDate);
    cs.sort(SortType.client); // cs.sort(SortType.contract);
    double totM = 0, totCom = 0;
    int totC = 0;
    
    for (Contract c : cs) {
      String endS = "", startS = "";
      if (metricDate) {
        startS = sdf.print(c.startRoundDate);
      } else {
        startS = sdf.print(c.startContract);
      }
      
      if (c.endContract != null) {
        if (metricDate) {
          endS = sdf.print(c.endRoundDate);
        } else {
          endS = sdf.print(c.endContract);
        }
      }
      
      double mrr = Metrics.computeMRR(c, d, metricDate);
      double commission = Metrics.computeCommission(c, d, metricDate);
      sb.append(String.format("%4d\t%-20s\t%-20s" //
          + "\t%5.0f\t%4d\t%5.0f\t%4.0f" //
          + "\t%4s\t%8s\t%8s\t%4s\t%8s\n", //
          c.getId(), c.name, c.client_name//
          , mrr, c.profiles, (mrr / c.profiles), commission//
          , c.type.name().substring(0, 3), c.billingSchema, startS, c.contractedMonths, endS));
      totM += mrr;
      totCom += commission;
      totC++;
    }
    sb.append(String.format("%4d %-20s %-20s %8.2f\t%8.2f\n", totC, "TOTAL", "", totM, totCom));
    return sb.toString();
    
  }
  
  public String displayAverages(LocalDate d, AccountFilter filter, boolean metricDate, Contracts contracts) {
    StringBuffer sb = new StringBuffer();
    
    if (contracts == null) {
      String err = "ERROR: NULL contracts?";
      System.err.println(err);
      return err;
    }
    sb.append("ACCOUNTS  (" + filter.toString() + ") at " + MonthlyMetrics.df.print(d) + "\n");
    
    Contracts cs = contracts.getActive(d, filter, metricDate);
    double totM = 0, totCom = 0;
    int totC = 0;
    TreeMap<String,Vector3D> map = new TreeMap<String,Vector3D>();
    Vector3D tot = new Vector3D(0, 0, 0);
    for (Contract c : cs) {
      String endS = "", startS = "";
      double mrr = Metrics.computeMRR(c, d, metricDate);
      double profs = c.profiles;
      
      Vector3D v = map.get(c.client_name);
      if (v == null) {
        v = new Vector3D(0, 0, 0);
      }
      Vector3D n = new Vector3D(new double[] {1, c.profiles, mrr});
      v = v.add(n);
      tot = tot.add(n);
      map.put(c.client_name, v);
    }
    sb.append(String.format("%4s\t%-20s\t%4s\t%s\t%12s\t%12s\n", "cnt", "client", "conts", "profs", "MRR", "MRR/Prof"));
    
    int cnt = 1;
    for (String n : map.keySet()) {
      Vector3D v = map.get(n);
      sb.append(String.format("%4d\t%-20s\t%4.0f\t%4.0f\t%10.1f\t%10.1f\n", cnt++, n, v.getX(), v.getY(), v.getZ(), v.getZ() / v.getY()));
    }
    sb.append(String.format("\n%4s\t%-20s\t%4.0f\t%4.0f\t%10.1f\t%10.1f\n", "-", "TOTAL", tot.getX(), tot.getY(), tot.getZ(), tot.getZ()
        / tot.getY()));
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
   * @throws ParseException
   * @throws SQLException
   * 
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
      LocalDate billingDate = bs.get(0).date;
      sb.append(printer.subtitle("INVOICES. " + sdf.print(billingDate)));
      String invoices = p.printBills(bs, false);
      sb.append(printer.preserveString(invoices));
    } else if (showInvoicesHeadlineWhenNone) {
      String invoices = p.printBills(bs, false);
      sb.append(printer.preserveString(invoices));
    }
    return sb.toString();
  }
  
  public static String displayLastMRR(Contracts contracts, int yearStart, int monthStart, int months, YearMonth highlightMonth)
      throws ParseException {
    
    String headerLine = "MONTH:\t\t";
    
    ArrayList<StringBuffer> lines = new ArrayList<StringBuffer>();
    for (int l = 0; l < 7; l++) {
      lines.add(new StringBuffer());
    }
    lines.get(0).append("DEPRECAETED POSITION");
    String avgLine = "Avg. Delta MRR (k€):";
    lines.get(1).append("     Delta MRR (k€):");
    lines.get(2).append("           MRR (k€):");
    lines.get(3).append("       MRR New (k€):");
    lines.get(4).append("      MRR Lost (k€):");
    lines.get(5).append("       MRR Exp (k€):");
    
    YearMonth end = new YearMonth(yearStart, monthStart).plusMonths(months - 1);
    int WINDOW_FOR_AVERAGE = 6;
    ArrayList<Double> average = new ArrayList<Double>();
    
    for (int i = 0; i < (months + WINDOW_FOR_AVERAGE - 1); i++) {
      
      YearMonth ym = end.minusMonths(i);
      YearMonth ymPrev = ym.minusMonths(1);
      int month = ym.getMonthOfYear();
      int year = ym.getYear();
      
      String label = year + "-" + month;
      
      MonthlyMetrics met1 = MonthlyMetrics.compute(ym.getYear(), ym.getMonthOfYear(), contracts);
      MonthlyMetrics met2 = MonthlyMetrics.compute(ymPrev.getYear(), ymPrev.getMonthOfYear(), contracts);
      
      double mrrDiff = (met1.mrr - met2.mrr) / 1000;
      double mrr = met1.mrr / 1000;
      average.add(mrrDiff);
      
      // highlight this month:
      if (highlightMonth != null && month == highlightMonth.getMonthOfYear() && year == highlightMonth.getYear()) {
        for (StringBuffer l : lines) {
          l.append("<strong>");
        }
      }
      
      if (i < months) { // the rest of months are only to compute delta average.
        lines.get(1).append(String.format("\t%6.1f", mrrDiff));
        lines.get(2).append(String.format("\t%6.1f", mrr));
        lines.get(3).append(String.format("\t%6.1f", met1.mrrNew / 1000));
        lines.get(4).append(String.format("\t%6.1f", -met1.churn / 1000));
        lines.get(5).append(String.format("\t%6.1f", met1.expansion / 1000));
        
        // highlight this month:
        if (highlightMonth != null && month == highlightMonth.getMonthOfYear() && year == highlightMonth.getYear()) {
          for (StringBuffer l : lines) {
            l.append("</strong>");
          }
        }
        headerLine += String.format("\t%6s", label);
      }
      
    }
    
    // COMPUTE AVERAGES
    double[] avgA = new double[average.size() - WINDOW_FOR_AVERAGE + 1];
    for (int i = 0; i < average.size() - WINDOW_FOR_AVERAGE + 1; i++) {
      for (int j = 0; j < WINDOW_FOR_AVERAGE; j++) {
        avgA[i] += average.get(i + j);
      }
      avgLine += String.format("\t%6.1f", avgA[i] / WINDOW_FOR_AVERAGE);
    }
    
    return headerLine + "\n" + lines.get(3) + "\n" + lines.get(4) + "\n" + lines.get(5) + "\n" + lines.get(1) + "\n" + lines.get(2) + "\n"
        + "\n\n<strong>" + avgLine + "</strong>\n";
  }
  
  /**
   * Notes:
   * <ul>
   * <li>Metrics are computed based on rounded dates (see Contract)
   * <li>See MonthlyMetrics doc for explanation of metrics
   * 
   * @param yearStart
   * @param monthStart
   * @param months
   * @param filter
   * @param completeDetail
   * @param allContracts
   * @return
   * @throws ParseException
   * @throws SQLException
   */
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
      } else {
        double dif = (m.expansion + m.mrrNew - m.churn);
        String debug = "\n==============================\n"//
            + "YEAR-MONTH: " + year + "-" + month + "\n" //
            + "OLD MRR: " + old.mrr + "\n" //
            + "NEW MRR Exp Churn New Tot= " + m.expansion + " - " + m.churn + " + " + m.mrrNew + " = " + dif + "\n"//
            + "FINAL MRR: " + m.mrr + "  -  " + old.mrr + "=" + (m.mrr - old.mrr)//
            + "\n==============================";
        logger.debug(debug);
        if (Math.abs(old.mrr + m.expansion + m.mrrNew - m.churn - m.mrr) > 0.001) {
          logger.error("INCONSISTENT MRR!!! " + debug);
        }
        
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
    String format = "%4s\t%30s\t%20s\t%10s\t%4s\t%-25s";
    String line;
    int lastmonth = -1, lastyear = -1;
    double total = 0;
    
    for (Contract c : contracts) {
      if (onlyFirstOfEachClient && clients.contains(c.client_name)) { // skip
        continue;
      }
      
      int month = c.startContract.getMonthOfYear();
      if (lastmonth < 0) { // init
        lastmonth = month;
      } else if (month != lastmonth) { // compute sum of previous month before starting
        String monthStr = "MONTH " + lastyear + "-" + lastmonth;
        line = String.format("\n" + format, "", "", "TOTAL:", BillingReportPrinter.money(total, true, CurrencyUtils.EUR), "", monthStr);
        lis.push(line);
        total = 0.;
        lastmonth = month;
        lastyear = c.startContract.getYear();
      }
      
      clients.add(c.client_name);
      double mrr = c.getMonthlyPrize(c.startContract, true, false);
      
      line = String.format(format, //
          toStringShort_commissionees(c), //
          (c.name == null ? "" : c.name), //
          ("(" + (c.client_name == null ? "" : c.client_name) + ")"), //
          BillingReportPrinter.money(mrr, true, c.currency), //
          printer.stringMonths(c), //
          printer.stringPeriod(c) //
          );
      // c.endContract != null ? sdf.print(c.endContract) :
      
      lis.push(line); // oldest to the bottom
      total += CurrencyUtils.toEuros(mrr, c.currency);
    }
    String monthStr = "MONTH " + lastyear + "-" + lastmonth;
    line = String.format("\n" + format, "", "", "TOTAL:", BillingReportPrinter.money(total, true, CurrencyUtils.EUR), "", monthStr);
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
  
  private static String toStringShort_commissionees(Contract c) {
    String ret = "";
    for (Commission com : c.commission) {
      ret += (com == null ? "NULL" : com.commissionnee) + ", ";
    }
    if (ret.length() == 0) {
      ret = "???";
    } else {
      ret = ret.substring(0, ret.length() - 2);
    }
    return ret;
  }
  
  public String displayTotals(LocalDate date, AccountFilter filter, boolean metricDate, Contracts contracts) throws ParseException,
      SQLException {
    StringBuffer sb = new StringBuffer();
    sb.append("TOTALS  (" + filter.toString() + ") at " + MonthlyMetrics.df.print(date) + "\n");
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
  
  public String displayEndingSoon(LocalDate date, AccountFilter filter, Contracts contracts) {
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
