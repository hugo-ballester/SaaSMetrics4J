/*
 *    SaaSMetrics4J : https://github.com/hugozaragoza/SaaSMetrics4J
 *
 *    (c) 2014, Hugo Zaragoza, Websays.
 */
package websays.accounting;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import org.apache.log4j.Logger;

import websays.core.utils.DateUtilsWebsays;

public class Contract {
  
  static final SimpleDateFormat df = new SimpleDateFormat("dd/MM/yyyy");
  private static final Logger logger = Logger.getLogger(Contract.class);
  
  public enum BillingSchema {
    MONTHS_1(true, 1), //
    MONTHS_3(true, 3), //
    MONTHS_6(true, 6), //
    MONTHS_12(true, 12), // the full year is payed on the first month of contract of every year (on the billing date)
    FULL_1(false, null);
    
    Integer months;
    boolean periodic;
    
    BillingSchema(boolean periodic, Integer months) {
      this.periodic = periodic;
      this.months = months;
    }
    
    boolean isPeriodic() {
      return periodic;
    }
    
    Integer getMonths() {
      return months;
    }
    
  }
  
  public enum Type {
    contract, project, pilot, internal, budget, test
  };
  
  // DB view:
  public int id;
  public String name;
  public Type type;
  public Integer client_id;
  public Date startContract;
  // endContract when known (typical monthly renewable contracts will have endContract NULL until client cancels with a date).
  public Date endContract;
  
  // agreed periods by contract (only used to warn when a contract is near the end). NOTE: this does not replace endContract, it is only an indication
  // since contracts may end early or auto-extend
  public int contractedMonths = 0;
  
  // base recurring prize
  public Double monthlyPrice;
  
  // fixed total price (will be added to monthlyPrice (dividing by total contract lenght) if both not null)
  public Double fixedPrice;
  
  // if not null, this is used instead of cost base to compute comision. This is useful when comision is on a different
  // quantity from cost.
  public Double commissionMonthlyBase;
  
  // Contracts have either fixed prizing or a pricingSchema
  public Pricing pricingSchema;
  
  public int profiles;
  
  public BillingSchema billingSchema = BillingSchema.MONTHS_1;
  
  // Optional:
  public Integer main_profile_id;
  
  // DERIVED:
  
  // for "metric" counts, we force contracts to start the first of the month and end the last of the month. This is arbitrarely done by rounding down
  // up to the
  // 15th.
  public Date startRoundDate, endRoundDate;
  Integer billedMonths = null;
  
  // from JOIN:
  public String client_name;
  
  // from COMMISSION %:
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
    
    startRoundDate = (DateUtilsWebsays.getDayOfMonth(start) <= 15) ? //
    DateUtilsWebsays.dateBeginningOfMonth(start)
        : DateUtilsWebsays.dateBeginningOfMonth(start, 1);
    
    // if (endContract != null && !DateUtilsWebsays.isLastDayOfMonth(endContract)) {
    // logger.warn("CONTRACT ENDING ON A DATE WHICH IS NOT END OF MONTH: " + name + " " + df.format(endContract));
    // }
    
    endRoundDate = end;
    if (endRoundDate != null) {
      if (!end.after(start)) {
        endRoundDate = DateUtilsWebsays.dateEndOfMonth(start);
      } else {
        endRoundDate = (DateUtilsWebsays.getDayOfMonth(end) <= 15) ? //
        DateUtilsWebsays.dateEndOfMonth(end, -1)
            : //
            DateUtilsWebsays.dateEndOfMonth(end);
      }
      billedMonths = DateUtilsWebsays.getHowManyMonths(startRoundDate, endRoundDate) + 1;
      if (logger.isDebugEnabled()) {
        String m = "\n" + df.format(start) + " --> " + df.format(startRoundDate) + "\n" + df.format(end) + " --> "
            + df.format(endRoundDate) + "   " + billedMonths + "\n\n";
        logger.debug(m);
      }
    }
    
  }
  
  public Contract(int id, String name, Type type, BillingSchema bs, Integer client_id, Date start, Date end, Pricing pricing,
      Double commission) {
    this(id, name, type, bs, client_id, start, end, commission);
    pricingSchema = pricing;
    monthlyPrice = null;
    fixedPrice = null;
  }
  
  public Contract(int id, String name, Type type, BillingSchema bs, Integer client_id, Date start, Date end, Double monthlyPrize,
      Double fixPrize, Double commission) {
    this(id, name, type, bs, client_id, start, end, commission);
    monthlyPrice = monthlyPrize;
    fixedPrice = fixPrize != null ? fixPrize : 0.0;
    pricingSchema = null;
  }
  
  public int getId() {
    return id;
  }
  
  @Override
  public String toString() {
    String startS = startContract == null ? "-" : df.format(startContract);
    String endS = endContract == null ? "-" : df.format(endContract);
    String startmS = startRoundDate == null ? "-" : df.format(startRoundDate);
    String endmS = endRoundDate == null ? "-" : df.format(endRoundDate);
    
    return String.format("%-20s %-12s %s %s (%s %s) %s ", name, client_id, startS, endS, startmS, endmS,
        (pricingSchema != null ? pricingSchema.name : "-") + "\t" + (monthlyPrice != null ? monthlyPrice : "-") + "\t"
            + (fixedPrice != null ? fixedPrice : "-"));
  }
  
  public double mrrChange(Date d, boolean roundDate) {
    Date newD = DateUtilsWebsays.dateEndOfMonth(d, 0);
    Date oldD = DateUtilsWebsays.dateEndOfMonth(d, -1);
    double change = computeMRR(newD, roundDate) - computeMRR(oldD, roundDate);
    return change;
  }
  
  public double expansion(Date d) {
    boolean roundDate = true;
    
    // if contract ended last month, return 0;
    Date prevMonth = DateUtilsWebsays.dateBeginningOfMonth(d, -1);
    if (isLastMonth(prevMonth, roundDate)) {
      return 0;
    }
    // if contract started this month, return 0;
    if (isFirstMonth(d, roundDate)) {
      return 0;
    } else {
      return mrrChange(d, roundDate);
    }
  }
  
  public double computeCommission(Date d, boolean roundDate) {
    if (commission == null || commission == 0) {
      return 0.;
    } else if (commissionMonthlyBase != null) {
      return commission * commissionMonthlyBase;
    } else {
      return commission * computeMRR(d, roundDate);
    }
  }
  
  public double computeMRR(Date d, boolean roundDate) {
    if (d.before(startRoundDate)) {
      return 0.;
    }
    if (roundDate) {
      if (endRoundDate != null && endRoundDate.before(d)) {
        return 0;
      }
    } else {
      if (endContract != null && endContract.before(d)) {
        return 0;
      }
    }
    
    double p = 0.;
    
    p = getMonthlyPrize(d, true);
    
    if (fixedPrice != null && fixedPrice > 0) {
      // Split fix prize evenly through all months, unless no end date is know (in that case put it al in the beginning)
      if (billedMonths != null) {
        p += fixedPrice / billedMonths;
      } else {
        // add fixed prize at the first month of the project
        if (fixedPrice != null && isFirstMonth(d, true)) {
          p += fixedPrice;
        }
      }
    }
    
    return p;
  }
  
  /**
   * Gets right monthly price, adding proportional fixed price
   * 
   * @param d
   *          only needd for pricing schemas that depend on date, otherwise can be null
   * @return
   */
  public double getMonthlyPrize(Date d, boolean addFixedPrize) {
    double p = 0;
    if (monthlyPrice != null && pricingSchema != null) {
      logger.error("INCONSISTENT PRIZING FOR CONTRACT '" + name + "' : monthlyPrize AND prizing CANNOT BE BOTH DEFINED");
    }
    if (monthlyPrice == null && pricingSchema == null && fixedPrice == 0) {
      logger.error("INCONSISTENT PRIZING FOR CONTRACT '" + name + "' : monthlyPrize=prizing=NULL and fixPrize=0");
    }
    
    // choose between monthlyPrize or pricingSchema
    if (monthlyPrice != null) {
      p = monthlyPrice;
    } else if (pricingSchema != null) {
      p = pricingSchema.getPrize(d);
    } else if (fixedPrice == null) {
      System.err.println("NEITHER monthlyPrice nor pricingSchema nor fixedPrice " + name);
    }
    
    // add fixedPrice fraction
    if (addFixedPrize && fixedPrice != null && fixedPrice > 0.) {
      if (endContract == null) {
        System.err.println("ERROR: fixedPrize but no endContract date !? " + name + ": " + fixedPrice);
        return 0;
      }
      int months = DateUtilsWebsays.getHowManyMonths(startContract, endContract) + 1;
      p += fixedPrice / months;
    }
    
    return p;
  }
  
  public boolean isActive(Date d, boolean roundDate) {
    if (roundDate) {
      if (d.before(startRoundDate) || (endRoundDate != null && (endRoundDate.before(d) || endRoundDate.equals(d)))) {
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
   * True if this contract is in billing period (from beginning of contract to last day of last month of contract)
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
  
  public boolean isFirstMonth(Date d, boolean roundDate) {
    if (roundDate) {
      if (startRoundDate != null && isSameMonth(d, startRoundDate)) {
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
  
  public boolean isLastMonth(Date d, boolean roundDate) {
    if (roundDate) {
      if (endRoundDate != null && isSameMonth(d, endRoundDate)) {
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
    Calendar c1 = Calendar.getInstance(), c2 = Calendar.getInstance();
    c1.setTime(d);
    c2.setTime(d2);
    
    if (c1.get(Calendar.MONTH) == c2.get(Calendar.MONTH) && c1.get(Calendar.YEAR) == c2.get(Calendar.YEAR)) {
      return true;
    } else {
      return false;
    }
  }
  
  public boolean isFirstFullMonth(Date d, boolean b) {
    if (DateUtilsWebsays.getDayOfMonth(startContract) == 1) {
      return isSameMonth(d, startContract);
    } else {
      return (DateUtilsWebsays.getHowManyMonths(startContract, d) == 1);
    }
  }
  
  public BilledPeriod getFirstBilledPeriod() {
    return new BilledPeriod(startContract, endContract, billingSchema);
  }
  
}
