/*
 *    SAS4J
 *
 *    Hugo Zaragoza, Websays.
 */
package websays.accounting;

import java.util.ArrayList;

public abstract class BillingReportPrinter {
  
  public abstract String printBill(Bill b, boolean sumary);
  
  public abstract String printBills(ArrayList<Bill> bills, boolean summary);
  
}