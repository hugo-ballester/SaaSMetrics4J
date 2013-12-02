/*
 *    SAS4J
 *
 *    Hugo Zaragoza, Websays.
 */
package websays.accounting;

import java.io.IOException;
import java.io.Writer;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import websays.accounting.Contracts.AccountFilter;
import websays.accounting.Contracts.SortType;

public class Reporting {
  
  static Boolean connectToDB = true;
  Contracts contracts;
  
  public Writer metricsWSB;
  
  public static final SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yy");
  
  public Reporting(Contracts contracts) {
    this.contracts = contracts;
  }
  
  public static void title(String string) {
    final String line = "=========================================================\n";
    String msg = "\n\n" + line;
    if (!connectToDB) {
      msg += "WARNING! NOT CONNECTED TO DB!!!\n";
    }
    msg += string + "\n" + line + "\n";
    System.out.print(msg);
  }
  
  public void displayContracts(Date d, AccountFilter filter, boolean metricDate) {
    System.out.println("CONTRACTS  (" + filter.toString() + ") at " + Metrics.df.format(d) + "\n");
    Contracts cs = contracts.getActive(d, filter, metricDate);
    cs.sort(SortType.client); // cs.sort(SortType.contract);
    double totM = 0;
    int totC = 0;
    for (Contract c : cs) {
      String endS = "";
      if (c.endContract != null)
        if (metricDate) {
          endS = sdf.format(c.endBill);
        } else {
          endS = sdf.format(c.endContract);
        }
      double mrr = c.mrr(d, metricDate);
      System.out.println(String.format("%4d %-20s %-20s %.0f\t%s\t%s", c.getId(), c.name, c.client_name, mrr, c.type, endS));
      totM += mrr;
      totC++;
    }
    System.out.println(String.format("%4d %-20s %-20s %.0f", totC, "TOTAL", "", totM));
    System.out.println();
    
  }
  
  private void displayClientMRR(String clientName, Date start, Date end) throws ParseException, SQLException {
    
    System.out.println("displayClientMRR   : " + (clientName));
    ArrayList<Contract> lis = new ArrayList<Contract>();
    for (Contract c : contracts) {
      if (c.client_name.equals(clientName)) {
        lis.add(c);
      }
    }
    
  }
  
  public void displayClientMRR(Date date, AccountFilter filter) throws ParseException, SQLException {
    
    System.out.println("displayClientMRR   : " + (filter == null ? "ALL" : filter.toString()) + " " + Metrics.df.format(date) + "\n");
    
    Contracts lis = contracts.getActive(date, filter, true);
    lis.sort(SortType.client);
    
    String lastN = null;
    double sum = 0., summ = 0.;
    int count = 0, countt = 0, clientN = 1;
    System.out.println(String.format("%3s %-20s\t%s\t%s", "#", "CLIENT", "Conts.", "MRR"));
    
    for (int i = 0; i < lis.size(); i++) {
      Contract c = lis.get(i);
      double mrr = c.mrr(date, true);
      
      if (lastN == null) {
        lastN = c.client_name;
      } else if ((i == lis.size() - 1) || (!lastN.equals(c.client_name))) {
        System.out.println(String.format("%3d %-20s\t(%d)\t%.2f", clientN, lastN, count, sum));
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
    System.out.println(String.format("%3s %-20s\t(%d)\t%.2f", "", "TOTAL", countt, summ));
    
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
  
  public void displayMetrics(int year, int monthStart, int months, AccountFilter filter) throws ParseException, SQLException {
    StringBuffer sb = new StringBuffer();
    sb.append("displayAll   : " + filter.toString() + "\n");
    sb.append("     \t" + Metrics.headersTop() + "\n");
    sb.append("month\t" + Metrics.headers() + "\n");
    double oldmrr = 0, oldchurn = 0;
    for (int i = monthStart; i <= (monthStart + months - 1); i++) {
      Metrics m = Metrics.compute(year, i, filter, contracts, oldmrr, oldchurn);
      sb.append("" + year + "/" + i + "\t" + m + "\n");
      oldmrr = m.mrr;
      oldchurn = m.churn;
    }
    sb.append("\n");
    try {
      if (metricsWSB != null) {
        metricsWSB.write(sb.toString());
      }
    } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    System.out.print(sb.toString());
    
  }
  
  public void setMetricsOutput(Writer writer) {
    metricsWSB = writer;
  }
}
