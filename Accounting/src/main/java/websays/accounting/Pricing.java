/*
 *    SaaSMetrics4J : https://github.com/hugozaragoza/SaaSMetrics4J
 *
 *    (c) 2014, Hugo Zaragoza, Websays.
 */
package websays.accounting;

import java.util.ArrayList;

import org.joda.time.LocalDate;

public class Pricing {
  
  public String name;
  ArrayList<LocalDate> dates = new ArrayList<LocalDate>();
  ArrayList<Double> prizes = new ArrayList<Double>();
  
  public Pricing() {
    super();
  }
  
  public Pricing(String name) {
    super();
    this.name = name;
  }
  
  public void add(LocalDate date, Double prize) {
    dates.add(date);
    prizes.add(prize);
  }
  
  @Override
  public String toString() {
    StringBuffer sb = new StringBuffer();
    for (int i = 0; i < dates.size(); i++) {
      sb.append(dates.get(i).toString() + ":" + prizes.get(i).toString() + ", ");
    }
    if (sb.length() > 0) {
      sb.setLength(sb.length() - 2);
    }
    return sb.toString();
  }
  
  Double getPrize(LocalDate d) {
    if (dates == null || dates.size() == 0) {
      return null;
    }
    int p = 0;
    for (int i = 0; i < dates.size(); i++) {
      LocalDate start = dates.get(i);
      if (!start.isAfter(d)) { // before or equal
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
