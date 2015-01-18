/*
 *    SAS4J
 *
 *    Hugo Zaragoza, Websays.
 */
package websays.accounting;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

import websays.core.utils.DateUtilsWebsays;

public class BillingReportPrinter {
  
  static final SimpleDateFormat dateFormat1 = new SimpleDateFormat("dd/MM/yyyy");
  
  private static final NumberFormat NF = NumberFormat.getNumberInstance(Locale.UK);
  
  {
    NF.setMinimumFractionDigits(2);
    NF.setMaximumFractionDigits(2);
  }
  
  public String printBill(Bill b, boolean sumary) {
    return "NOT IMPLEMENTED";
  };
  
  public String printBills(ArrayList<Bill> bills, boolean summary) {
    return "NOT IMPLEMENTED";
  };
  
  public synchronized static String euros(double x, boolean round) {
    return money(x, round, '€');
  }
  
  public synchronized static String euros(double x) {
    return money(x, false, '€');
  }
  
  public synchronized static String money(double x, boolean round, char currency) {
    if (round) {
      x = Math.round(x);
    }
    return NF.format(x) + currency;
  }
  
  public String title(String string, boolean connectToDB) {
    return "NOT IMPLEMENTED";
  }
  
  public String subtitle(String string) {
    return "NOT IMPLEMENTED";
  }
  
  public String header(String version) {
    return "NOT IMPLEMENTED";
  }
  
  public String stringPeriod(Contract c) {
    String cont = null;
    if (c.contractedMonths != null && c.contractedMonths > 0 && c.startContract != null) {
      Date end = DateUtilsWebsays.addMonthsAndDays(c.startContract, c.contractedMonths, -1);
      cont = "+" + c.contractedMonths + "M=" + dateFormat1.format(end);
    }
    
    String ret = //
    (c.startContract != null ? dateFormat1.format(c.startContract) : "?") + //
        (c.endContract != null ? "-" + dateFormat1.format(c.endContract) : "") + //
        (cont != null ? cont : "");
    return ret;
  }
  
}