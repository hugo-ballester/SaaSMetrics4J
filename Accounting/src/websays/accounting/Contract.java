/*
 *    SaaS4J
 *
 *    Hugo Zaragoza, Websays.
 */
package websays.accounting;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;

import org.apache.log4j.Logger;

import websays.core.utils.DateUtilsWebsays;

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
  
  public BillingSchema billingSchema = BillingSchema.fullMonth;
  
  public enum BillingSchema {
    fullYear, // the full year is payed on the first month of contract of every year (on the billing date)
    fullMonth, // the full current month is payed on the billing date of the month
    fullFirstMonth // the full amount due is payed at the billing date of the first month
  }
  
  // Optional:
  public Integer main_profile_id;
  Pricing prizing;
  
  // DERIVED:
  
  // to simplify we force contracts to start the first of the month and end the last of the month. This is arbitrarely done by rounding down up to the
  // 15th.
  public Date startMetric, endMetric;
  Integer billedMonths = null;
  
  // from JOIN:
  public String client_name;
  
  // from COMMISSION TYPES:
  Double commission;
  
  public Contract() {}
  
  Contract(int id, String name, Type type, BillingSchema bs, Integer client_id, Date start, Date end, Double commission) {
    super();
    this.id = id;
    this.name = name;
    this.type = type;
    this.client_id = client_id;
    this.commission = commission;
    billingSchema = bs;
    startContract = start;
    endContract = end;
    profiles = 0;
    
    startMetric = (DateUtilsWebsays.getDayOfMonth(start) <= 15) ? //
    DateUtilsWebsays.dateBeginningOfMonth(start)
        : DateUtilsWebsays.dateBeginningOfMonth(start, 1);
    
    endMetric = end;
    if (endMetric != null) {
      if (!end.after(start)) {
        endMetric = DateUtilsWebsays.dateEndOfMonth(start);
      } else {
        endMetric = (DateUtilsWebsays.getDayOfMonth(end) <= 15) ? //
        DateUtilsWebsays.dateEndOfMonth(end, -1)
            : //
            DateUtilsWebsays.dateEndOfMonth(end);
      }
      billedMonths = DateUtilsWebsays.getHowManyMonths(startMetric, endMetric) + 1;
      if (logger.isDebugEnabled()) {
        String m = "\n" + df.format(start) + " --> " + df.format(startMetric) + "\n" + df.format(end) + " --> " + df.format(endMetric)
            + "   " + billedMonths + "\n\n";
        logger.debug(m);
      }
    }
    
  }
  
  public Contract(int id, String name, Type type, BillingSchema bs, Integer client_id, Date start, Date end, String prize, Double commission) {
    this(id, name, type, bs, client_id, start, end, commission);
    prizingName = prize;
    monthlyPrize = null;
    fixPrize = null;
  }
  
  public Contract(int id, String name, Type type, BillingSchema bs, Integer client_id, Date start, Date end, Double monthlyPrize,
      Double fixPrize, Double commission) {
    this(id, name, type, bs, client_id, start, end, commission);
    this.monthlyPrize = monthlyPrize;
    this.fixPrize = fixPrize != null ? fixPrize : 0.0;
    prizing = null;
  }
  
  public int getId() {
    return id;
  }
  
  public void linkPrize(HashMap<String,Pricing> map) {
    if (prizingName != null) {
      if (!map.containsKey(prizingName)) {
        logger.error("Could not find prizing name '" + prizingName + "'");
      } else {
        prizing = map.get(prizingName);
      }
    }
  }
  
  @Override
  public String toString() {
    String startS = startContract == null ? "-" : df.format(startContract);
    String endS = endContract == null ? "-" : df.format(endContract);
    String startmS = startMetric == null ? "-" : df.format(startMetric);
    String endmS = endMetric == null ? "-" : df.format(endMetric);
    
    return String.format("%-20s %-12s %s %s (%s %s) %s", name, client_id, startS, endS, startmS, endmS, (prizing != null ? prizing.name
        : "-") + "\t" + (monthlyPrize != null ? monthlyPrize : "-") + "\t" + (fixPrize != null ? fixPrize : "-"));
  }
  
  public double mrrChange(Date d, boolean metricDate) {
    Date newD = DateUtilsWebsays.dateEndOfMonth(d, 0);
    Date oldD = DateUtilsWebsays.dateEndOfMonth(d, -1);
    double change = computeMRR(newD, metricDate) - computeMRR(oldD, metricDate);
    return change;
  }
  
  public double expansion(Date d) {
    boolean metricDate = true;
    
    // if contract ended last month, return 0;
    Date prevMonth = DateUtilsWebsays.dateBeginningOfMonth(d, -1);
    if (isLastMonth(prevMonth, metricDate)) {
      return 0;
    }
    // if contract started this month, return 0;
    if (isFirstMonth(d, metricDate)) {
      return 0;
    } else {
      return mrrChange(d, metricDate);
    }
  }
  
  public double computeCommission(Date d, boolean metricDate) {
    if (commission == null || commission == 0) {
      return 0.;
    }
    return commission * computeMRR(d, metricDate);
  }
  
  public double computeMRR(Date d, boolean metricDate) {
    if (d.before(startMetric)) {
      return 0.;
    }
    if (metricDate) {
      if (endMetric != null && endMetric.before(d)) {
        return 0;
      }
    } else {
      if (endContract != null && endContract.before(d)) {
        return 0;
      }
    }
    
    double p = 0.;
    
    p = getMonthlyPrize(d);
    
    if (fixPrize != null && fixPrize > 0) {
      // Split fix prize evenly through all months, unless no end date is know (in that case put it al in the beginning)
      if (billedMonths != null) {
        p += fixPrize / billedMonths;
      } else {
        // add fixed prize at the first month of the project
        if (fixPrize != null && isFirstMonth(d, true)) {
          p += fixPrize;
        }
      }
    }
    
    return p;
  }
  
  public double getMonthlyPrize(Date d) {
    double p = 0;
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
    return p;
  }
  
  public boolean isActive(Date d, boolean metricDate) {
    if (metricDate) {
      if (d.before(startMetric) || (endMetric != null && (endMetric.before(d) || endMetric.equals(d)))) {
        return false;
      } else {
        return true;
      }
    } else {
      if (d.before(startContract) || (endContract != null && (endContract.before(d) || endContract.equals(d)))) {
        return false;
      } else {
        return true;
      }
      
    }
    
  }
  
  /**
   * True if this contract is in billing perior (from beginning of contract to last day of last month of contract)
   * 
   * @param d
   * @return
   */
  public boolean isActiveBill(Date d) {
    if (d.before(startContract)) {
      return false;
    }
    if (endContract != null && !DateUtilsWebsays.isSameMonth(endContract, d) && d.after(endContract)) {
      return false;
    }
    return true;
  }
  
  public boolean isFirstMonth(Date d, boolean metricDate) {
    if (metricDate) {
      if (startMetric != null && isSameMonth(d, startMetric)) {
        return true;
      } else {
        return false;
      }
    } else {
      if (startContract != null && isSameMonth(d, startContract)) {
        return true;
      } else {
        return false;
      }
    }
    
  }
  
  public boolean isLastMonth(Date d, boolean metricDate) {
    if (metricDate) {
      if (endMetric != null && isSameMonth(d, endMetric)) {
        return true;
      } else {
        return false;
      }
    } else {
      if (endContract != null && isSameMonth(d, endContract)) {
        return true;
      } else {
        return false;
      }
    }
  }
  
  private boolean isSameMonth(Date d, Date d2) {
    if (d.getMonth() == d2.getMonth() && d.getYear() == d2.getYear()) {
      return true;
    } else {
      return false;
    }
  }
  
}
