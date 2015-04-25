/*
 *    SAS4J
 *
 *    Hugo Zaragoza, Websays.
 */
package websays.accounting;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Currency;
import java.util.Locale;
import java.util.TimeZone;

import org.joda.time.LocalDate;
import org.joda.time.format.DateTimeFormatter;

import websays.core.utils.CurrencyUtils;
import websays.core.utils.JodaUtils;

public class BillingReportPrinter {
  
  TimeZone tz = TimeZone.getDefault();
  DateTimeFormatter dateFormat1;
  
  private static final NumberFormat NF = NumberFormat.getNumberInstance(Locale.UK);
  private static final Currency EUROS = Currency.getInstance("EUR");
  
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
    return money(x, round, EUROS);
  }
  
  public synchronized static String euros(double x) {
    return money(x, false, EUROS);
  }
  
  public synchronized static String money(double x, boolean round, Currency currency) {
    if (round) {
      x = Math.round(x);
    }
    return NF.format(x) + CurrencyUtils.getCurrencySymbol(currency);
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
      LocalDate end = JodaUtils.addMonthsAndDays(c.startContract, c.contractedMonths, -1);
      cont = "+" + c.contractedMonths + "M=" + dateFormat1.print(end);
    }
    
    String ret = //
    (c.startContract != null ? dateFormat1.print(c.startContract) : "?") + //
        (c.endContract != null ? "-" + dateFormat1.print(c.endContract) : "") + //
        (cont != null ? cont : "");
    return ret;
  }
  
  public String stringMonths(Contract c) {
    if (c.contractedMonths != null && c.contractedMonths > 0 && c.startContract != null) {
      return "" + c.contractedMonths + "M";
    } else if (c.startContract != null && c.endContract != null) {
      return "" + JodaUtils.monthsDifference(c.startContract, c.endContract) + "M";
    }
    return "?";
  }
  
  public String line() {
    // TODO Auto-generated method stub
    return null;
  }
  
  public String box1(String title, String content, boolean connectToDB, String extraStyle) {
    return null;
  }
  
  public String box1(String title, String content, boolean connectToDB) {
    return null;
  }
  
}