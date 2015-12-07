/*
 *    SaaSMetrics4J : https://github.com/hugozaragoza/SaaSMetrics4J
 *
 *    (c) 2014, Hugo Zaragoza, Websays.
 */
package websays.accounting;

import java.util.ArrayList;
import java.util.Currency;

import org.apache.log4j.Logger;
import org.joda.time.Days;
import org.joda.time.LocalDate;

import websays.core.utils.CurrencyUtils;
import websays.core.utils.JodaUtils;

/**
 * Represents a contract with a client (will genereate bills)
 * 
 * @author hugoz
 *
 */
public class Contract {
  
  private static final Logger logger = Logger.getLogger(Contract.class);
  
  public enum ContractDocument {
    none, missing, signed
  }
  
  public enum BillingSchema {
    MONTHS_1(1), //
    MONTHS_3(3), //
    MONTHS_6(6), //
    MONTHS_12(12), // the full year is payed on the first month of contract of every year (on the billing date)
    MONTHS_1000(1000); // for strange cases
    
    Integer months;
    
    BillingSchema(Integer months) {
      this.months = months;
    }
    
    Integer getMonths() {
      return months;
    }
    
  }
  
  public enum ClientType {
    agency, direct, project_only
  }
  
  public enum Type {
    subscription, project, pilot, internal, budget, test
  };
  
  // =============================================
  // DB view:
  public int id;
  // public Integer continuation_from_id; // when this contract is a continuation of a previous one, this indicated id of previous
  
  public String name;
  public Type type;
  public Integer client_id;
  public LocalDate startContract;
  // endContract when known (typical monthly renewable contracts will have endContract NULL until client cancels with a date).
  public LocalDate endContract;
  
  // agreed periods by contract (only used to warn when a contract is near the end). NOTE: this does not replace endContract, it is only an indication
  // since contracts may end early or auto-extend
  public Integer contractedMonths = null;
  
  // base recurring prize
  public Double monthlyPrice;
  
  // when monthlyPrice=0, set this to free so we don't bill and don't warn
  public Boolean free;
  
  // fixed total price (will be added to monthlyPrice (dividing by total contract lenght) if both not null)
  public Double fixedPrice;
  
  // Contracts have either fixed prizing or a pricingSchema
  public Pricing pricingSchema;
  
  public ContractDocument contractDocument = ContractDocument.none;
  
  public int profiles;
  
  public BillingSchema billingSchema = BillingSchema.MONTHS_1;
  
  public ClientType client_type = null;
  
  public Currency currency = CurrencyUtils.EUR;
  public String comments_billing;
  
  public Integer main_profile_id;
  
  // =============================================
  // DERIVED fields:
  
  public LocalDate startRoundDate;
  
  // for "metric" counts, we force contracts to start the first of the month and end the last of the month.
  public LocalDate endRoundDate;
  
  // from JOIN:
  public String client_name;
  public String billingCenter;
  
  // commission % of MRR (or of commission_base if defined) (not in data model, instead computed from commision schema at DAO time)
  public ArrayList<Commission> commission = new ArrayList<Commission>();
  
  /**
   * basic, gold, etc.
   */
  public String plan;
  
  Contract(int id, String name, Type type, BillingSchema bs, Integer client_id, LocalDate start, LocalDate end,
      ArrayList<Commission> commission, String comments_billing) {
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
    endRoundDate = roundEnd(endContract);
    startRoundDate = roundStart(startContract);
    this.comments_billing = comments_billing;
    initDerived();
  }
  
  public Contract() {};
  
  public void initDerived() {
    
    if (logger.isDebugEnabled()) {
      String m = "\n" + startContract.toString() + " --> " + startRoundDate.toString() + "\n";
      if (endContract != null) {
        m += endContract.toString() + " --> " + endRoundDate.toString();
      }
      logger.debug(m);
    }
    
  }
  
  private LocalDate roundStart(LocalDate date) {
    // OLD:
    // startRoundDate = (time.getDayOfMonth(startContract) <= MIDPOINT) ? //
    // time.dateBeginningOfMonth(startContract, 0)
    // : time.dateBeginningOfMonth(startContract, 1);
    // if (endContract != null) {
    // if (!endContract.after(startContract)) {
    // endRoundDate = time.dateEndOfMonth(startContract);
    // } else {
    // endRoundDate = (time.getDayOfMonth(endContract) <= MIDPOINT) ? //
    // time.dateEndOfMonth(endContract, -1)
    // : //
    // time.dateEndOfMonth(endContract);
    
    if (date == null) {
      return null;
    }
    LocalDate firstOfMonth = date.dayOfMonth().withMinimumValue();
    return firstOfMonth;
  }
  
  private LocalDate roundEnd(LocalDate date) {
    // OLD:
    // startRoundDate = (time.getDayOfMonth(startContract) <= MIDPOINT) ? //
    // time.dateBeginningOfMonth(startContract, 0)
    // : time.dateBeginningOfMonth(startContract, 1);
    // if (endContract != null) {
    // if (!endContract.after(startContract)) {
    // endRoundDate = time.dateEndOfMonth(startContract);
    // } else {
    // endRoundDate = (time.getDayOfMonth(endContract) <= MIDPOINT) ? //
    // time.dateEndOfMonth(endContract, -1)
    // : //
    // time.dateEndOfMonth(endContract);
    
    if (date == null) {
      return null;
    }
    LocalDate endOfMonth = date.dayOfMonth().withMaximumValue();
    return endOfMonth;
  }
  
  public Contract(int id, String name, Type type, BillingSchema bs, Integer client_id, LocalDate start, LocalDate end, Pricing pricing,
      ArrayList<Commission> commission) {
    this(id, name, type, bs, client_id, start, end, commission, null);
    pricingSchema = pricing;
    monthlyPrice = null;
    fixedPrice = null;
  }
  
  public Contract(int id, String name, Type type, BillingSchema bs, Integer client_id, LocalDate start, LocalDate end, Double monthlyPrize,
      Double fixPrize, ArrayList<Commission> commission) {
    this(id, name, type, bs, client_id, start, end, commission, null);
    monthlyPrice = monthlyPrize;
    fixedPrice = fixPrize != null ? fixPrize : 0.0;
    pricingSchema = null;
  }
  
  public int getId() {
    return id;
  }
  
  public String toStringShortName() {
    return String.format("[%d] %s", id, name);
  }
  
  @Override
  public String toString() {
    String startS = startContract == null ? "-" : startContract.toString();
    String endS = endContract == null ? "-" : endContract.toString();
    String startmS = startRoundDate == null ? "-" : startRoundDate.toString();
    String endmS = endRoundDate == null ? "-" : endRoundDate.toString();
    
    return String.format("%-20s %-12s %s %s (%s %s) %s ", name, client_id, startS, endS, startmS, endmS,
        (pricingSchema != null ? pricingSchema.name : "-") + "\t" + (monthlyPrice != null ? monthlyPrice : "-") + "\t"
            + (fixedPrice != null ? fixedPrice : "-"));
  }
  
  /**
   * Gets right monthly price, adding proportional fixed price
   * 
   * @param d
   *          only needd for pricing schemas that depend on date, otherwise can be null
   * @return
   */
  public double getMonthlyPrize(LocalDate d, boolean addFixedPrize, boolean roundDate) {
    Double p = 0.;
    if (monthlyPrice != null && pricingSchema != null) {
      logger.error("INCONSISTENT PRIZING FOR CONTRACT '" + name + "' : monthlyPrize AND prizing CANNOT BE BOTH DEFINED");
    }
    if (monthlyPrice == null && pricingSchema == null && fixedPrice == null) {
      logger.error("INCONSISTENT PRIZING FOR CONTRACT '" + name + "' : monthlyPrize=prizing=NULL and fixPrize=0");
    }
    
    // choose between monthlyPrize or pricingSchema
    if (monthlyPrice != null) {
      p = monthlyPrice;
    } else if (pricingSchema != null) {
      p = pricingSchema.getPrize(d);
      if (p == null || p == 0) {
        logger.error("Pricing schema returned prize 0 for on " + (d != null ? d.toString() : "null") + " for contract " + id
            + " and schema " + pricingSchema.name + ":\n" + pricingSchema.toString());
        System.exit(-1);
      }
    } else if (fixedPrice == null) {
      System.err.println("NEITHER monthlyPrice nor pricingSchema nor fixedPrice " + name);
    }
    
    // add fixedPrice fraction
    if (addFixedPrize && fixedPrice != null && fixedPrice > 0.) {
      if (endContract == null) {
        System.err.println("ERROR: fixedPrize but no endContract date !? " + name + ": " + fixedPrice);
        return 0;
      }
      int months = 0;
      if (roundDate) {
        months = JodaUtils.monthsDifference(startRoundDate, endRoundDate) + 1;
      } else {
        months = JodaUtils.monthsDifference(startContract, endContract) + 1;
        
      }
      p += fixedPrice / months;
    }
    
    return p;
  }
  
  public boolean isActive(LocalDate d, boolean roundDate) {
    LocalDate start = startContract;
    if (roundDate) {
      start = startRoundDate;
    }
    
    if (endContract == null) {
      return !(d.isBefore(start));
    }
    
    // endContract != null
    LocalDate end = endContract;
    if (roundDate) {
      end = endRoundDate;
    }
    
    return (!d.isBefore(start) && !d.isAfter(end));
    
  }
  
  /**
   * True if this contract is in billing period (from beginning of contract to last day of last month of contract)
   * 
   * @param d
   * @return
   */
  public boolean isActiveBill(LocalDate d) {
    if (d.isBefore(startContract)) {
      return false;
    }
    if (endContract != null && !JodaUtils.isSameMonthAndYear(endContract, d) && d.isAfter(endContract)) {
      return false;
    }
    return true;
  }
  
  public boolean isFirstMonth(LocalDate d, boolean roundDate) {
    
    if (roundDate) {
      if (startRoundDate != null && JodaUtils.isSameMonthAndYear(d, startRoundDate)) {
        return true;
      } else {
        return false;
      }
    } else {
      if (startContract != null && JodaUtils.isSameMonthAndYear(d, startContract)) {
        return true;
      } else {
        return false;
      }
    }
    
  }
  
  /**
   * true if d is in the last month of this contract
   * 
   */
  public boolean isLastMonth(LocalDate d, boolean roundDate) {
    if (roundDate) {
      if (endRoundDate != null && JodaUtils.isSameMonthAndYear(d, endRoundDate)) {
        return true;
      } else {
        return false;
      }
    } else {
      if (endContract != null && JodaUtils.isSameMonthAndYear(d, endContract)) {
        return true;
      } else {
        return false;
      }
    }
  }
  
  // public boolean isFirstFullMonth(Calendar d, boolean b) {
  //
  // if (time.getDayOfMonth(startContract) == 1) {
  // return isSameMonth(d, startContract);
  // } else {
  // return (time.getHowManyMonths(startContract, d) == 1);
  // }
  // }
  
  public BilledPeriod getFirstBilledPeriod() {
    try {
      return new BilledPeriod(startContract, endContract, billingSchema);
    } catch (Exception e) {
      logger.error("ERROR with contract " + toStringShortName() + ":\n  " + e.getMessage());
      return null;
    }
  }
  
  /**
   * @return -1 if end not defined
   */
  public int getDays() {
    if (endContract == null) {
      return -1;
    }
    return Days.daysBetween(startContract, endContract).getDays();
  }
  
  /**
   * Ignores DAY_OF_MONTH 0 if contracts ends same month as d, 1 if contract ends following month, etc.
   * 
   * @return
   */
  public int getMonthsRemaining(LocalDate d) {
    int months;
    if (endContract == null && contractedMonths == null) {
      logger.error("Neither endContract nor contractedMonths are defined! Setting contractedMonths=1");
      contractedMonths = 1;
    }
    if (endContract != null) {
      months = JodaUtils.monthsDifference(d, endContract);
      return months;
    } else if (contractedMonths != null) {
      LocalDate end = (new LocalDate(startContract)).plusMonths(contractedMonths).minusDays(1);
      months = JodaUtils.monthsDifference(d, end);
      return months;
    } else {
      logger.error("SHOULD NEVER ARRIVE HERE!!!");
      return 0;
    }
  }
  
  public boolean isCostZero() {
    return this.pricingSchema == null && (this.fixedPrice == null || this.fixedPrice == 0.0)
        && (this.monthlyPrice == null || this.monthlyPrice == 0.0);
  }
  
}
