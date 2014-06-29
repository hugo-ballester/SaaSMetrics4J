/*
 *    SaaSMetrics4J : https://github.com/hugozaragoza/SaaSMetrics4J
 *
 *    (c) 2014, Hugo Zaragoza, Websays.
 */
package websays.accounting;

import java.util.ArrayList;
import java.util.Date;

public class Pricing {
  
  public String name;
  ArrayList<Date> dates = new ArrayList<Date>();
  ArrayList<Double> prizes = new ArrayList<Double>();
  
  public Pricing() {
    super();
  }
  
  public Pricing(String name) {
    super();
    this.name = name;
  }
  
  public void add(Date date, Double prize) {
    dates.add(date);
    prizes.add(prize);
  }
  
  Double getPrize(Date d) {
    if (dates == null || dates.size() == 0) {
      return null;
    }
    int p = 0;
    for (int i = 0; i < dates.size(); i++) {
      Date start = dates.get(i);
      if (!start.after(d)) { // before or equal
        p = i;
      }
    }
    if (p < prizes.size()) {
      return prizes.get(p);
    } else {
      return null;
    }
  }
  
}
