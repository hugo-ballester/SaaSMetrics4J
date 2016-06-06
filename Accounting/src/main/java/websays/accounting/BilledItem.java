/*
 *    SaaSMetrics4J : https://github.com/hugozaragoza/SaaSMetrics4J
 *
 *    (c) 2014, Hugo Zaragoza, Websays.
 */
package websays.accounting;

import java.util.ArrayList;
import java.util.Currency;

import org.apache.log4j.Logger;
import org.joda.time.LocalDate;

import websays.accounting.Contract.ContractDocument;
import websays.core.utils.JodaUtils;

public class BilledItem {
  
  static final String AUTORENEWED = "AUTORENEWED WITHOUT CLIENT AGREEMENT?";
  static final String WILL_RENEW_NEXT_MONTH = "WILL RENEW NEXT MONTH";
  static final String WILL_RENEW_2MONTHS = "TWO MONTHS TO AUTORENEW";
  
  private static final Logger logger = Logger.getLogger(BilledItem.class);
  
  public BilledPeriod period;
  private Double fee, commissionBaseFee;
  
  public String contract_name;
  public int contract_id;
  public ArrayList<String> notes = new ArrayList<String>(0);
  
  public CommissionItemSet commissions = new CommissionItemSet();
  private Currency currency;
  
  /**
   * 
   * An item in a bill, a service billed (typically corresponds to a period of service billed)
   * 
   * @param billedPeriod
   * @param fee
   * @param contract_name
   * @param contract_id
   * @param contractedPeriods
   *          used to determine if a warning of last bill should be issued.
   */
  public BilledItem(BilledPeriod billedPeriod, Double fee, Double commissionBaseFee, String contract_name, int contract_id,
      Currency currency) {
    super();
    period = billedPeriod;
    setFees(fee, commissionBaseFee, currency);
    this.contract_name = contract_name;
    this.contract_id = contract_id;
  }
  
  public BilledItem(BilledItem bi) throws Exception {
    this(new BilledPeriod(bi.period), new Double(bi.getFee()), new Double(bi.getCommissionBaseFee()), bi.contract_name, bi.contract_id,
        bi.currency);
  }
  
  public void warningChecks(LocalDate billingDate, Contract c) {
    if (c == null) {
      logger.error("Null contract ???");
      return;
    }
    
    if (c.comments_billing != null && c.comments_billing.length() > 0) {
      notes.add(c.comments_billing);
    }
    
    if (c.contractDocument.equals(ContractDocument.missing)) {
      notes.add("!!!MISSING CONTRACT");
    }
    
    // int monthNumber1 = JodaUtils.monthsDifference(c.startContract, billingDate);
    
    if (c.endContract != null) {
      if (period.inPeriod(c.endContract)) {
        notes.add("Last bill as agreed (end contract:" + c.endContract.toString() + ")");
      } else {
        int months = JodaUtils.monthsDifference(billingDate, c.endContract);
        if (months < 0) {
          notes.add("WARNING contract has ended! (end contract: " + c.endContract.toString() + ")");
        }
        if (months >= 0 && months < 2) {
          notes.add("Contract ending soon as agreed (end contract: " + c.endContract.toString() + ")");
        }
      }
    } else if (c.contractedMonths != null) {
      LocalDate end = c.startContract.plusMonths(c.contractedMonths).minusDays(1);
      int left = JodaUtils.monthsDifference(billingDate, end);
      if (left == 1) {
        notes.add(WILL_RENEW_2MONTHS);
      } else if (left == 0) {
        notes.add(WILL_RENEW_NEXT_MONTH);
      } else if (left < 0) {
        notes.add(AUTORENEWED);
      }
    }
    
  }
  
  public Currency getCurrency() {
    return currency;
  }
  
  public Boolean feeIsNull() {
    return this.fee == null;
  }
  
  public Double getFee() {
    return this.fee;
  }
  
  public Double getCommissionBaseFee() {
    return commissionBaseFee;
  }
  
  public void setFees(Double fee, Double commissionBaseFee, Currency currency) {
    this.fee = fee;
    this.commissionBaseFee = commissionBaseFee != null ? commissionBaseFee : fee;
    if (fee < 0.0) {
      logger.error("fee < 0.0");
    }
    this.currency = currency;
  }
  
  public String toShortString() {
    return contract_name + " (" + contract_id + ")" + period.toString() + " " + fee + currency;
  }
  
}
