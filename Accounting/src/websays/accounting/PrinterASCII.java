/*
 *    SaaSMetrics4J : https://github.com/hugozaragoza/SaaSMetrics4J
 *
 *    (c) 2014, Hugo Zaragoza, Websays.
 */
package websays.accounting;

import java.io.IOException;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

public class PrinterASCII extends BillingReportPrinter {
  
  static String line1 = "=========================================================\n";
  static String line2 = "---------------------------------------------------------\n";
  static String line2S = "-------------------------------------------------------- :\n";
  static String line2E = "-------------------------------------------------------- .\n";
  String TAB = "\t";
  String RET = "\n";
  
  private static final Logger logger = Logger.getLogger(PrinterASCII.class);
  
  static final SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yy");
  
  @Override
  public String printBill(Bill b, boolean sumary) {
    
    StringBuilder s = new StringBuilder();
    
    s.append(String.format("INVOICE FOR CLIENT: %-30s" + TAB + "%10s" + b.items.get(0).getCurrencySymbol() + "\n", b.clientName, //
        NumberFormat.getIntegerInstance().format(b.getTotalFee())));
    
    if (!sumary) {
      for (int i = 0; i < b.items.size(); i++) {
        BilledItem bi = b.items.get(i);
        s.append(displayBilledItemAsLine(bi));
      }
    }
    return s.toString();
  }
  
  private String displayBilledItemAsLine(BilledItem bi) {
    StringBuilder s = new StringBuilder();
    int monthNumber = bi.period.monthNumber(bi.period.billDate);
    String comms = "";
    if (bi.commissions != null && bi.commissions.size() > 0) {
      comms = commissionsShortString(bi.commissions);
    }
    String per = String.format("B%2s-M%2s", bi.period.period, monthNumber);
    s.append(String.format("   %-20s" + TAB + "(%s %s-%s %s%s%s)", bi.contract_name, per, sdf.format(bi.period.periodStart),
        sdf.format(bi.period.periodEnd), NumberFormat.getIntegerInstance().format(bi.fee), bi.getCurrencySymbol(), comms));
    if (bi.notes != null && bi.notes.size() > 0) {
      s.append(TAB + StringUtils.join(bi.notes, " | "));
    }
    s.append("\n");
    return s.toString();
  }
  
  private String commissionsShortString(ArrayList<CommissionItem> commissions) {
    String s = "";
    if (commissions == null || commissions.size() == 0) {
      return s;
    }
    for (CommissionItem ci : commissions) {
      s += ci.commissionnee + ":" + ci.commission + ", ";
    }
    return "{" + s.substring(0, s.length() - 2) + "}";
  }
  
  /*
   * (non-Javadoc)
   * 
   * @see websays.accounting.BillingReportPrinter#printBills(java.util.ArrayList, boolean)
   */
  @Override
  public String printBills(ArrayList<Bill> bills, boolean summary) {
    if (bills.size() == 0) {
      return "\n";
    }
    
    // sb.append(line);
    // sb.append(line);
    ArrayList<Bill> noBills = new ArrayList<Bill>();
    CommissionItemSet comms = new CommissionItemSet();
    Date billDate = null;
    double tot = 0., totCom = 0.;
    
    // RENDER BILL REPORT:
    StringBuilder sb = new StringBuilder();
    for (Bill b : bills) {
      
      if (b.getTotalFee() == 0) {
        noBills.add(b);
      } else {
        
        tot += b.getTotalFee();
        for (BilledItem bi : b.items) {
          comms.addAll(bi.commissions);
          totCom += bi.commissions.totalCommission();
        }
        
        sb.append(printBill(b, summary));
        sb.append("\n");
        
        if (billDate == null) {
          billDate = b.date;
        } else {
          if (!billDate.equals(b.date)) {
            logger.warn("not all bills have same date! " + "\n" + TAB + "" + b.clientName + ": " + sdf.format(b.date) + " <> "
                + sdf.format(billDate));
          }
        }
      }
    }
    // RENDER TOTALS:
    sb.append("\n" + line2S + "TOTAL INVOICED:" + TAB + tot + "\n" + line2E);
    sb.append("\n" + line2S + "COMMISSIONS:" + TAB + "" + totCom + "\n");
    sb.append("\n" + TAB + comms.groupAndSum() + "\n" + line2E);
    
    // RENDER SERVICES NOT BILLED THIS MONTH:
    sb.append("\n" + line2S + "(Active contracts with no bills this month:)\n\n");
    for (Bill b : noBills) {
      sb.append(printBill(b, summary));
      sb.append("\n");
    }
    sb.append(line2E);
    
    // DONE.
    return sb.toString();
  }
  
  public static void printTitle(String string, boolean connectToDB) throws IOException {
    String msg = "\n\n" + line1;
    if (!connectToDB) {
      msg += "WARNING! NOT CONNECTED TO DB!!!\n";
    }
    msg += string + "\n" + line1 + "\n";
    System.out.print(msg);
  }
  
  public static void printSubtitle(String string) throws IOException {
    String msg = "\n\n" + line2;
    msg += string + "\n" + line2 + "\n";
    System.out.print(msg);
  }
  
}
