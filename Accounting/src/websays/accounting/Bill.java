/*
 *    SAS4J
 *
 *    Hugo Zaragoza, Websays.
 */
package websays.accounting;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

public class Bill {
  
  final static SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd");
  
  public Date date;
  public String clientName;
  public Double sumFee = 0.0;
  public ArrayList<String> contracts;
  public ArrayList<Double> fees;
  
  public Bill(Date d, String clientName, String contractName, Double fee) {
    this(d, clientName);
    addFee(contractName, fee);
  }
  
  public Bill(Date d, String clientName) {
    super();
    date = d;
    this.clientName = clientName;
  }
  
  public void addFee(String contract, Double fee) {
    if (fee == null) {
      System.err.println("ERROR: null fee cannot be added to bill!");
    }
    if (contracts == null) {
      contracts = new ArrayList<String>();
    }
    if (fees == null) {
      fees = new ArrayList<Double>();
    }
    contracts.add(contract);
    fees.add(fee);
    sumFee += fee;
  }
  
  @Override
  public String toString() {
    return "BILL FOR '" + clientName + "', ON " + sdf.format(date) + ", FOR: " + sumFee;
  }
  
  public void addBill(Bill b) {
    if (b.clientName != null && clientName != null && !b.clientName.equals(clientName)) {
      System.err.println("MERGING TWO BILLS OF DIFFERENT CLIENTS!?");
    }
    for (int i = 0; i < b.contracts.size(); i++) {
      addFee(b.contracts.get(i), b.fees.get(i));
    }
    
  }
  
}
