/*
 *    SaaSMetrics4J : https://github.com/hugozaragoza/SaaSMetrics4J
 *
 *    (c) 2014, Hugo Zaragoza, Websays.
 */
package websays.accounting;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

public class PrinterASCII extends BillingReportPrinter {
  
  static String line1 = "===========================================\n";
  static String line2 = "-------------------------------------------\n\n";
  static String line2S = "-------------------------------------------:\n\n";
  static String line2E = "-------------------------------------------.\n\n";
  String TAB = "\t";
  String RET = "\n";
  
  SimpleDateFormat sd = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
  
  private static final Logger logger = Logger.getLogger(PrinterASCII.class);
  
  public static final SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yy");
  
  @Override
  public String printBill(Bill b, boolean sumary) {
    
    StringBuilder s = new StringBuilder();
    
    s.append(String.format("%-30s" + TAB + "%10s" + "\n", //
        b.clientName, money(b.getTotalFee(), false, b.items.get(0).getCurrencySymbol())));
    
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
      s += String.format("%s:%.2f,", ci.commissionnee, ci.commission);
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
    sb.append("\n" + "TOTAL INVOICED:" + TAB + tot + "\n");
    sb.append("\n" + "COMMISSIONS:" + TAB + totCom + "\n");
    sb.append(TAB + joinMoney(comms.groupAndSum()) + "\n");
    
    // RENDER SERVICES NOT BILLED THIS MONTH:
    sb.append(subtitle("Active contracts with no bills this month:"));
    for (Bill b : noBills) {
      sb.append(printBill(b, summary));
      sb.append("\n");
    }
    // DONE.
    return sb.toString();
  }
  
  private String joinMoney(HashMap<String,Double> groupAndSum) {
    StringBuffer str = new StringBuffer();
    str.append("{");
    for (java.util.Map.Entry<String,Double> e : groupAndSum.entrySet()) {
      str.append(e.getKey() + ":" + money(e.getValue(), false, '€') + ", ");
    }
    if (str.length() > 0) {
      str.setLength(str.length() - 2);
    }
    str.append("}");
    return str.toString();
  }
  
  @Override
  public String title(String string, boolean connectToDB) {
    String db = connectToDB ? "" : "(!OFFLINE)";
    String msg = "<hl/><h2>" + string + db + "</h2>";
    return msg;
  }
  
  @Override
  public String subtitle(String string) {
    String msg = "<h4>" + string + "</h4>";
    return msg;
  }
  
  @Override
  public String header(String version) {
    String msg = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Strict//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd\">";
    msg += "\n<head></head>\n";
    msg += "<h4>SaaS4J Metrics Report v" + version + ". Generated on " + sd.format(new Date()) + "</h4><hr/>\n\n";
    return msg;
  }
  
}
