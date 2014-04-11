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
  
  static final SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
  
  static final String line = "---------------------------------------------\n";
  static final String line1 = "=========================================================\n";
  static final String line2 = "---------------------------------------------------------\n";
  
  public String printBill(Bill b, boolean sumary) {
    String s = String.format("INVOICE FOR CLIENT: %-30s\t%10s€\n", b.clientName, //
        NumberFormat.getIntegerInstance().format(b.sumFee));
    if (!sumary) {
      for (int i = 0; i < b.fees.size(); i++) {
        s += String.format("   %-20s\t(%s €)\n", b.contracts.get(i), NumberFormat.getIntegerInstance().format(b.fees.get(i)));
      }
    }
    return s;
  }
  
  public String printBills(ArrayList<Bill> bills, boolean summary) {
    StringBuilder sb = new StringBuilder();
    Date d = bills.get(0).date;
    for (Bill b : bills) {
      if (!b.date.equals(d)) {
        System.err.println("WARNING: not all bills have same date!");
      }
    }
    
    // sb.append(line);
    // sb.append(line);
    
    for (Bill b : bills) {
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
