/*
 *    SAS4J
 *
 *    Hugo Zaragoza, Websays.
 */
package websays.accounting;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;

import org.apache.log4j.Logger;

import utils.DateUtilsWebsays;

public class Contract {
  
  static Logger logger = Logger.getLogger(Contract.class);
  
  static final SimpleDateFormat df = new SimpleDateFormat("dd/MM/yyyy");
  
  public enum Type {
    contract, project, pilot, internal, budget, test
  };
  
  // DB view:
  public int id;
  public String name;
  public Type type;
  public Integer client_id;
  public Date startContract, endContract;
  
  public Double monthlyPrize, fixPrize;
  public String prizingName;
  
  public int profiles;
  
  // Optional:
  public Integer main_profile_id;
  Pricing prizing;
  
  // DERIVED:
  
  // to simplify we force contracts to start the first of the month and end the last of the month. This is arbitrarely done by rounding down up to the
  // 15th.
  public Date startBill, endBill;
  Integer billedMonths = null;
  
  // from JOIN:
  public String client_name;
  
  public Contract() {}
  
  Contract(int id, String name, Type type, Integer client_id, Date start, Date end) {
    super();
    this.id = id;
    this.name = name;
    this.type = type;
    this.client_id = client_id;
    startContract = start;
    endContract = end;
    profiles = 0;
    
    startBill = (DateUtilsWebsays.getDayOfMonth(start) <= 15) ? //
    DateUtilsWebsays.dateBeginningOfMonth(start)
        : DateUtilsWebsays.dateBeginningOfMonth(start, 1);
    
    endBill = end;
    if (endBill != null) {
      if (!end.after(start)) {
        endBill = DateUtilsWebsays.dateEndOfMonth(start);
      } else {
        endBill = (DateUtilsWebsays.getDayOfMonth(end) <= 15) ? //
        DateUtilsWebsays.dateEndOfMonth(end, -1)
            : //
            DateUtilsWebsays.dateEndOfMonth(end);
      }
      billedMonths = DateUtilsWebsays.getHowManyMonths(startBill, endBill) + 1;
      if (logger.isDebugEnabled()) {
        String m = "\n" + df.format(start) + " --> " + df.format(startBill) + "\n" + df.format(end) + " --> " + df.format(endBill) + "   "
            + billedMonths + "\n\n";
        logger.debug(m);
      }
    }
    
  }
  
  public Contract(int id, String name, Type type, Integer client_id, Date start, Date end, String prize) {
    this(id, name, type, client_id, start, end);
    prizingName = prize;
    monthlyPrize = null;
    fixPrize = null;
  }
  
  public Contract(int id, String name, Type type, Integer client_id, Date start, Date end, Double monthlyPrize, Double fixPrize) {
    this(id, name, type, client_id, start, end);
    this.monthlyPrize = monthlyPrize;
    this.fixPrize = fixPrize != null ? fixPrize : 0.0;
    prizing = null;
  }
  
  public int getId() {
    return id;
  }
  
  public void linkPrize(HashMap<String,Pricing> map) {
    if (prizingName != null)
      if (!map.containsKey(prizingName)) {
        logger.error("Could not find prizing name '" + prizingName + "'");
      } else {
        prizing = map.get(prizingName);
      }
  }
  
  @Override
  public String toString() {
    String startS = startContract == null ? "-" : df.format(startContract);
    String endS = endContract == null ? "-" : df.format(endContract);
    
    return String.format("%-20s %-12s %s", name, client_id, startS + "\t" + endS + "\t" + (prizing != null ? prizing.name : "-") + "\t"
        + (monthlyPrize != null ? monthlyPrize : "-") + "\t" + (fixPrize != null ? fixPrize : "-"));
  }
  
  public double mrrChange(Date d) {
    Date newD = DateUtilsWebsays.dateEndOfMonth(d, 0);
    Date oldD = DateUtilsWebsays.dateEndOfMonth(d, -1);
    double change = mrr(newD) - mrr(oldD);
    return change;
  }
  
  public double expansion(Date d) {
    if (newThisMonth(d))
      return 0;
    else
      return mrrChange(d);
  }
  
  public double mrr(Date d) {
    if (d.before(startBill))
      return 0.;
    if (endBill != null && endBill.before(d))
      return 0;
    
    double p = 0.;
    
    if (monthlyPrize != null && prizing != null) {
      logger.error("INCONSISTENT PRIZING FOR CONTRACT '" + name + "' : monthlyPrize AND prizing CANNOT BE BOTH DEFINED");
    }
    if (monthlyPrize == null && prizing == null && fixPrize == 0) {
      logger.error("INCONSISTENT PRIZING FOR CONTRACT '" + name + "' : monthlyPrize=prizing=NULL and fixPrize=0");
    }
    
    if (monthlyPrize != null) {
      p = monthlyPrize;
    } else if (prizing != null) {
      p = prizing.getPrize(d);
    }
    
    if (fixPrize != null && fixPrize > 0) {
      // Split fix prize evenly through all months, unless no end date is know (in that case put it al in the beginning)
      if (billedMonths != null) {
        p += fixPrize / billedMonths;
      } else {
        // add fixed prize at the first month of the project
        if (fixPrize != null && isFirstMonth(d)) {
          p += fixPrize;
        }
      }
    }
    
    return p;
  }
  
  public boolean isActive(Date d) {
    if (d.before(startBill) || (endBill != null && (endBill.before(d) || endBill.equals(d))))
      return false;
    else
      return true;
    
  }
  
  public boolean isFirstMonth(Date d) {
    if (startBill != null && isSameMonth(d, startBill))
      return true;
    else
      return false;
    
  }
  
  public boolean isLastMonth(Date d) {
    if (endBill != null && isSameMonth(d, endBill))
      return true;
    else
      return false;
  }
  
  private boolean isSameMonth(Date d, Date d2) {
    if (d.getMonth() == d2.getMonth() && d.getYear() == d2.getYear())
      return true;
    else
      return false;
  }
  
  public boolean newThisMonth(Date d) {
    return (isSameMonth(startBill, d));
  }
}