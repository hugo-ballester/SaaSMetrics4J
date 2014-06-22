/*
 *    SAS4J
 *
 *    Hugo Zaragoza, Websays.
 */
package websays.accounting;

import java.io.IOException;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

public class PrinterASCII {
  
  static final SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yy");
  
  static final String line = "---------------------------------------------\n";
  static final String line1 = "=========================================================\n";
  static final String line2 = "---------------------------------------------------------\n";
  
  public String printBill(Bill b, boolean sumary) {
    String s = String.format("INVOICE FOR CLIENT: %-30s\t%10s€\n", b.clientName, //
        NumberFormat.getIntegerInstance().format(b.sumFee));
    if (!sumary) {
      for (int i = 0; i < b.items.size(); i++) {
        BilledItem bi = b.items.get(i);
        s += String.format("   %-20s\t(%s-%s, %s€)\n", bi.contract_name, sdf.format(bi.period.periodStart),
            sdf.format(bi.period.periodEnd), NumberFormat.getIntegerInstance().format(bi.fee));
      }
    }
    return s;
  }
  
  public String printBills(ArrayList<Bill> bills, boolean summary) {
    if (bills.size() == 0) {
      return "\n";
    }
    StringBuilder sb = new StringBuilder();
    Date d = bills.get(0).date;
    for (Bill b : bills) {
      if (!b.date.equals(d)) {
        System.err.println("WARNING: not all bills have same date! " + sdf.format(b.date));
      }
    }
    
    // sb.append(line);
    // sb.append(line);
    ArrayList<Bill> noBills = new ArrayList<Bill>();
    
    for (Bill b : bills) {
      if (b.sumFee == 0) {
        noBills.add(b);
      } else {
        sb.append(printBill(b, summary));
        sb.append("\n");
      }
    }
    
    sb.append("\n" + line2 + "(Active contracts with no bills this month:)\n\n");
    
    for (Bill b : noBills) {
      sb.append(printBill(b, summary));
      sb.append("\n");
    }
    
    // sb.append(line);
    return sb.toString();
  }
  
  public static void title(String string, boolean connectToDB) throws IOException {
    String msg = "\n\n" + line1;
    if (!connectToDB) {
      msg += "WARNING! NOT CONNECTED TO DB!!!\n";
    }
    msg += string + "\n" + line1 + "\n";
    System.out.print(msg);
  }
  
  public static void subtitle(String string) throws IOException {
    String msg = "\n\n" + line2;
    msg += string + "\n" + line2 + "\n";
    System.out.print(msg);
  }
  
}
