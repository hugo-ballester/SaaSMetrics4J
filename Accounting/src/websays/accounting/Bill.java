/*
 *    SaaSMetrics4J : https://github.com/hugozaragoza/SaaSMetrics4J
 *
 *    (c) 2014, Hugo Zaragoza, Websays.
 */

package websays.accounting;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

public class Bill {
  
  private static final Logger logger = Logger.getLogger(Bill.class);
  {
    logger.setLevel(Level.TRACE);
  }
  final static SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd");
  
  public Date date;
  public String clientName;
  
  public ArrayList<BilledItem> items;
  
  public Bill(Date d, String clientName) {
    super();
    date = d;
    this.clientName = clientName;
  }
  
  public void addItem(BilledItem bi) {
    
    if (bi.fee == null) {
      System.err.println("ERROR: null fee cannot be added to bill!");
    }
    if (items == null) {
      items = new ArrayList<BilledItem>();
    }
    items.add(bi);
  }
  
  @Override
  public String toString() {
    return "BILL FOR '" + clientName + "', ON " + sdf.format(date) + ", FOR: " + getTotalFee();
  }
  
  public double getTotalFee() {
    if (items == null) {
      return 0.0;
    }
    double sum = 0.0;
    for (BilledItem bi : items) {
      sum += bi.fee;
    }
    return sum;
  }
  
  public void mergeBill(Bill b) {
    if (b.clientName != null && clientName != null && !b.clientName.equals(clientName)) {
      System.err.println("MERGING TWO BILLS OF DIFFERENT CLIENTS!?");
    }
    for (int i = 0; i < b.items.size(); i++) {
      addItem(b.items.get(i));
    }
  }
  
}
