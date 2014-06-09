/*
 *    SAS4J
 *
 *    Hugo Zaragoza, Websays.
 */
package websays.accounting;

import java.io.IOException;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import org.apache.log4j.Logger;

import websays.accounting.Contracts.AccountFilter;
import websays.accounting.Contracts.SortType;

public class Reporting {
  
  @SuppressWarnings("unused")
  private static final Logger logger = Logger.getLogger(Reporting.class);
  
  Contracts contracts;
  
  public static final SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yy");
  
  public boolean showInvoicesHeadlineWhenNone = true;
  
  public Reporting(Contracts contracts) {
    this.contracts = contracts;
  }
  
  public void displayContracts(Date d, AccountFilter filter, boolean metricDate, boolean colheader) {
    System.out.println("CONTRACTS  (" + filter.toString() + ") at " + Metrics.df.format(d) + "\n");
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
      if (c.endContract != null) {
        if (metricDate) {
          endS = sdf.format(c.endRoundDate);
          startS = sdf.format(c.startRoundDate);
        } else {
          endS = sdf.format(c.endContract);
          startS = sdf.format(c.startContract);
        }
      }
      double mrr = c.computeMRR(d, metricDate);
      double commission = c.computeCommission(d, metricDate);
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
    
    PrinterASCII p = new PrinterASCII();
    
    ArrayList<Bill> bs = Billing.bill(contracts, year, month);
    
    // System.out.println(p.line + "SUMMARY\n" + p.line);
    // System.out.println(p.printBills(bs, true));
    
    if (bs.size() > 0 || showInvoicesHeadlineWhenNone) {
      String invoices = p.printBills(bs, false);
      System.out.println(PrinterASCII.line + "INVOICES at " + year + "/" + month + ":\n" + PrinterASCII.line);
      System.out.println(invoices);
    }
  }
  
  public void displayClientMRR(Date date, AccountFilter filter, boolean metricDate) throws ParseException, SQLException {
    
    System.out.println("displayClientMRR   : " + (filter == null ? "ALL" : filter.toString()) + " " + Metrics.df.format(date) + "\n");
    
    Contracts lis = contracts.getActive(date, filter, metricDate);
    lis.sort(SortType.client);
    
    String lastN = null;
    double sum = 0., summ = 0.;
    int count = 0, countt = 0, clientN = 1;
    System.out.println(String.format("%3s %-20s\t%s\t%s", "#", "CLIENT", "Conts.", "MRR"));
    
    for (int i = 0; i < lis.size(); i++) {
      Contract c = lis.get(i);
      double mrr = c.computeMRR(date, metricDate);
      
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
  
  public void displayMetrics(int year, int monthStart, int months) throws ParseException, SQLException {
    System.out.println("displayMetrics");
    System.out.println("     \t" + Metrics.headersTop());
    System.out.println("month\t" + Metrics.headers());
    double oldmrr = 0, oldchurn = 0;
    
    for (int i = monthStart; i <= monthStart + months - 1; i++) {
      Metrics m = Metrics.compute(year, i, null, contracts, oldmrr, oldchurn);
      System.out.println("" + year + "/" + i + "\t" + m.toString());
      oldmrr = m.mrr;
      oldchurn = m.churn;
      
    }
    
  }
  
  public void displayMetrics(int yearStart, int monthStart, int months, AccountFilter filter) throws ParseException, SQLException {
    
    StringBuffer sb = new StringBuffer();
    sb.append("displayAll   : " + filter.toString() + "\n");
    sb.append("     \t" + Metrics.headersTop() + "\n");
    sb.append("month\t" + Metrics.headers() + "\n");
    double oldmrr = 0, oldchurn = 0;
    int year = yearStart;
    int month = monthStart - 1;
    for (int i = 0; i < months; i++) {
      month++;
      if (month == 13) {
        month = 1;
        year++;
      }
      
      Metrics m = Metrics.compute(year, month, filter, contracts, oldmrr, oldchurn);
      sb.append("" + year + "/" + month + "\t" + m.toString() + "\n");
      
      oldmrr = m.mrr;
      oldchurn = m.churn;
    }
    sb.append("\n");
    System.out.print(sb.toString());
    
  }
  
  public void title(String string, boolean connectToDB) throws IOException {
    PrinterASCII.title(string, connectToDB);
  }
  
  public void subtitle(String string) throws IOException {
    PrinterASCII.subtitle(string);
  }
}
