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
import java.util.TreeMap;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

public class PrinterASCII {
  
  private static final Logger logger = Logger.getLogger(PrinterASCII.class);
  
  static final SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yy");
  
  static final String line = "---------------------------------------------\n";
  static final String line1 = "=========================================================\n";
  static final String line2 = "---------------------------------------------------------\n";
  
  public String printBill(Bill b, boolean sumary) {
    String s = String.format("INVOICE FOR CLIENT: %-30s\t%10s" + b.items.get(0).getCurrencySymbol() + "\n", b.clientName, //
        NumberFormat.getIntegerInstance().format(b.getTotalFee()));
    
    if (!sumary) {
      for (int i = 0; i < b.items.size(); i++) {
        BilledItem bi = b.items.get(i);
        int monthNumber = bi.period.monthNumber(bi.period.billDate);
        String comms = "";
        if (bi.comissionees != null && bi.comissionees.size() > 0) {
          comms = bi.comissionees.toString();
          comms = " C:" + comms.substring(1, comms.length() - 1);
        }
        String per = String.format("B%2s-M%2s", bi.period.period, monthNumber);
        s += String.format("   %-20s\t(%s %s-%s %s%s%s)", bi.contract_name, per, sdf.format(bi.period.periodStart),
            sdf.format(bi.period.periodEnd), NumberFormat.getIntegerInstance().format(bi.fee), bi.getCurrencySymbol(), comms);
        if (bi.notes != null && bi.notes.size() > 0) {
          s += "\t" + StringUtils.join(bi.notes, " | ");
        }
        s += "\n";
      }
    }
    return s;
  }
  
  public String printBills(ArrayList<Bill> bills, boolean summary) {
    if (bills.size() == 0) {
      return "\n";
    }
    StringBuilder sb = new StringBuilder();
    
    // sb.append(line);
    // sb.append(line);
    ArrayList<Bill> noBills = new ArrayList<Bill>();
    Date billDate = null;
    double tot = 0., totCom = 0.;
    
    TreeMap<String,Double> comms = new TreeMap<String,Double>();
    
    for (Bill b : bills) {
      
      if (b.getTotalFee() == 0) {
        noBills.add(b);
      } else {
        
        tot += b.getTotalFee();
        totCom += b.getTotalCommission();
        logger.trace("TOTOCOM: " + totCom + "\t" + b.clientName + ": " + b.getTotalCommission());
        
        for (BilledItem bi : b.items) { // collect all commissionnees
          Billing.addValues(comms, bi.comissionees);
        }
        
        sb.append(printBill(b, summary));
        sb.append("\n");
        
        if (billDate == null) {
          billDate = b.date;
        } else {
          if (!billDate.equals(b.date)) {
            logger.warn("not all bills have same date! " + "\n\t" + b.clientName + ": " + sdf.format(b.date) + " <> "
                + sdf.format(billDate));
          }
        }
      }
    }
    sb.append("\n" + line2 + "TOTAL INVOICED:\t" + tot + "\n");
    
    sb.append("\n" + line2 + "COMMISSIONS:\t" + totCom + "\n");
    sb.append("\n\t" + comms.toString() + "\n");
    
    sb.append("\n" + line2 + "(Active contracts with no bills this month:)\n\n");
    
    for (Bill b : noBills) {
      sb.append(printBill(b, summary));
      sb.append("\n");
    }
    
    // sb.append(line);
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
