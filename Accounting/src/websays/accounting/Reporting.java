/*
 *    SAS4J
 *
 *    Hugo Zaragoza, Websays.
 */
package websays.accounting;

import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.regex.Pattern;

import websays.accounting.Contracts.AccountFilter;
import websays.accounting.Contracts.SortType;

public class Reporting {
  
  static Boolean connectToDB = true;
  Contracts contracts;
  
  static final SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yy");
  
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
  
  public void displayContracts(Date d, AccountFilter filter) {
    System.out.println("CONTRACTS  (" + filter.toString() + ") at " + Metrics.df.format(d) + "\n");
    Contracts cs = contracts.getActive(d, filter);
    cs.sort(SortType.client); // cs.sort(SortType.contract);
    double totM = 0;
    int totC = 0;
    for (Contract c : cs) {
      String endS = "";
      if (c.endBill != null) {
        endS = sdf.format(c.endBill);
      }
      System.out.println(String.format("%4d %-20s %-20s %.0f\t%s\t%s", c.getId(), c.name, c.client_name, c.mrr(d), c.type, endS));
      totM += c.mrr(d);
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
    
    Contracts lis = contracts.getActive(date, filter);
    lis.sort(SortType.client);
    
    String lastN = null;
    double sum = 0., summ = 0.;
    int count = 0, countt = 0, clientN = 1;
    System.out.println(String.format("%3s %-20s\t%s\t%s", "#", "CLIENT", "Conts.", "MRR"));
    
    for (int i = 0; i < lis.size(); i++) {
      Contract c = lis.get(i);
      double mrr = c.mrr(date);
      
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
  
  private void displayMetrics(int year, int monthStart, int months, String contractName) throws ParseException, SQLException {
    System.out.println("displayAll   : " + contractName + "\n");
    System.out.println("     \t" + Metrics.headersTop());
    System.out.println("month\t" + Metrics.headers());
    double oldmrr = 0, oldchurn = 0;
    Contracts cs = contracts.getView(Pattern.compile(Pattern.quote(contractName)));
    
    for (int i = monthStart; i <= monthStart + months - 1; i++) {
      Metrics m = Metrics.compute(year, i, null, cs, oldmrr, oldchurn);
      System.out.println("" + year + "/" + i + "\t" + m.toString());
      oldmrr = m.mrr;
      oldchurn = m.churn;
      
    }
    
  }
  
  public void displayMetrics(int year, int monthStart, int months, AccountFilter filter) throws ParseException, SQLException {
    System.out.println("displayAll   : " + filter.toString() + "\n");
    System.out.println("     \t" + Metrics.headersTop());
    System.out.println("month\t" + Metrics.headers());
    double oldmrr = 0, oldchurn = 0;
    for (int i = monthStart; i <= (monthStart + months - 1); i++) {
      Metrics m = Metrics.compute(year, i, filter, contracts, oldmrr, oldchurn);
      System.out.println("" + year + "/" + i + "\t" + m);
      oldmrr = m.mrr;
      oldchurn = m.churn;
    }
    
  }
}
