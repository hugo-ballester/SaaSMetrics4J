/*
 *    SAS4J
 *
 *    Hugo Zaragoza, Websays.
 */
package websays.accounting;

import java.util.ArrayList;
import java.util.HashMap;

public class CommissionItemSet extends ArrayList<CommissionItem> {
  
  public double totalCommission() {
    double ret = 0.;
    for (CommissionItem ci : this) {
      ret += ci.commission;
    }
    return ret;
  }
  
  HashMap<String,Double> groupAndSum() {
    HashMap<String,Double> sum = new HashMap<String,Double>();
    
    for (CommissionItem ci : this) {
      String key = ci.commissionnee;
      double added = ci.commission;
      
      if (sum.containsKey(key)) {
        sum.put(key, sum.get(key) + added);
      } else {
        sum.put(key, added);
      }
    }
    return sum;
    
  }
  
}
