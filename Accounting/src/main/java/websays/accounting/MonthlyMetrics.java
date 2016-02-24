/*
 *    SaaSMetrics4J : https://github.com/hugozaragoza/SaaSMetrics4J
 *
 *    (c) 2014, Hugo Zaragoza, Websays.
 */
package websays.accounting;

import java.text.ParseException;

import org.apache.log4j.Logger;
import org.joda.time.LocalDate;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import websays.accounting.metrics.Metrics;
import websays.core.utils.maths.DescriptiveStats;

/**
 * 
 * Computes SAS Metrics for a set of Contracts
 * 
 * 
 * @author hugoz
 * 
 */
public class MonthlyMetrics {
  
  private static final Logger logger = Logger.getLogger(MonthlyMetrics.class);
  
  public static final DateTimeFormatter df = DateTimeFormat.forStyle("S-");
  boolean metricDate = GlobalConstants.roundDatesForMetrics;
  
  public double mrr, comm;
  public int accounts;
  
  public int profiles;
  
  /**
   * MRR from new accounts (accounts starting this month)
   */
  public double mrrNew;
  
  /**
   * number of accounts ending this month
   */
  public int accsEnd;
  
  /**
   * number of accounts starting this month
   */
  public int accsNew;
  
  /**
   * Amount of money that we will "loose" next month due to loosing accounts (due to accounts for which this is the current last month)
   */
  public double churn;
  
  /**
   * Amount of money that we will "change" next month from existing accounts (due to accounts for which this month will pay a different ammount)
   */
  public double expansion;
  
  /**
   * Number of accounts in previous month
   */
  public int oldAccs;
  
  /**
   * Previous MRR
   */
  private double oldMrr;
  
  /**
   * difference with previous MRR
   */
  public double mrrDif;
  
  /**
   * relative difference with previous MRR
   */
  public double mrrRelInc;
  
  // Other temporary: -----------------------------------------------
  
  public DescriptiveStats profilesSt = new DescriptiveStats();
  public DescriptiveStats mrrSt = new DescriptiveStats();
  
  /**
   * Exponential Smoothing Constant
   */
  private static final double alpha = 0.3333333;
  
  /**
   * Computes MRR and other metrics
   * <ul>
   * <li>Dates are rounded (see contract rounding)
   * <li>Metrics are computed at the last day of the month
   * <li>Churn is any contracts ending in the month (including the last day of current month)
   * <li>New business is any contracts starting in the month
   * </ul>
   * 
   * 
   * @param year
   * @param month
   * @param accounts
   * @param metricDate
   *          if true rounds all dates
   * @return
   * @throws ParseException
   */
  public static MonthlyMetrics compute(int year, int month, Contracts accounts) throws ParseException {
    boolean metricDate = true;
    LocalDate lastDayOfMonth = (new LocalDate(year, month, 1)).dayOfMonth().withMaximumValue();
    
    MonthlyMetrics m = new MonthlyMetrics();
    
    accounts.sort(websays.accounting.Contracts.SortType.contract);
    
    String debug = String.format("%5s%20s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\n", //
        "id", "name", "profs", "Sprofs", "mrr", "Smrr", "new", "churn", "expansion");
    
    for (Contract a : accounts) {
      
      if (!a.isActiveBill(lastDayOfMonth)) {
        continue;
      }
      
      boolean isFirstMonth = a.isFirstMonth(lastDayOfMonth, metricDate);
      boolean isLastMonth = a.isLastMonth(lastDayOfMonth, metricDate);
      
      // ENDING
      if (isLastMonth) {
        // if (!a.isActive(lastDayOfMonth, metricDate)) {
        
        // Date prevMonth = DateUtils.addMonths(lastDayOfMonth, -1);
        // if (a.isLastMonth(prevMonth, metricDate)) {
        double mrr = Metrics.computeMRR(a, a.endContract, metricDate);
        m.accsEnd++;
        m.churn += mrr;
        if (logger.isDebugEnabled()) {
          debug += String.format("%5d%20s\t%d\t%6.0f\t%6.0f\t%6.0f\t%6.0f\t%6.0f\t%6.0f\n", //
              a.id, a.name, a.profiles, 0., -mrr, 0., 0., m.churn, m.expansion);
        }
      }
      
      double mrr = Metrics.computeMRR(a, lastDayOfMonth, metricDate);
      if (!isLastMonth) { // churn not included in month
        m.mrrSt.add(mrr);
        m.accounts++;
      }
      if (isFirstMonth) {
        m.accsNew++;
        m.mrrNew += mrr;
      }
      m.comm += Metrics.computeCommission(a, lastDayOfMonth, metricDate);
      m.profilesSt.add((1.0) * a.profiles);
      m.expansion += Metrics.expansion(a, lastDayOfMonth);
      
      if (logger.isDebugEnabled()) {
        debug += (String.format("%5d%20s\t%d\t%6.0f\t%6.0f\t%6.0f\t%6.0f\t%6.0f\t%6.0f\n", a.id, a.name, a.profiles, m.profilesSt.sum(),
            mrr, m.mrrSt.sum(), m.mrrNew, m.churn, m.expansion));
      }
    }
    m.mrr = m.mrrSt.sum();
    m.profiles = (int) m.profilesSt.sum();
    logger.debug("DEBUGGING MonthlyMetrics COMPUTATION FOR " + year + "-" + month + ":\n" + debug + "\n");
    return m;
  }
  
  static double checkmrr = 0;
  
  private void setOldMRR(double oldMrr) {
    this.oldMrr = oldMrr;
    mrrDif = mrr - oldMrr;
    mrrRelInc = oldMrr > 0 ? mrrDif / oldMrr * 100. : 0;
  }
  
  public double exponentialSmooth(double value, double oldAvg, double a) {
    return a * value + (1. - a) * oldAvg;
  }
  
  public void addAsNewValueInAverage(MonthlyMetrics m) {
    mrr = exponentialSmooth(m.mrr, mrr, alpha);
    mrrDif = exponentialSmooth(m.mrrDif, mrrDif, alpha);
    mrrRelInc = exponentialSmooth(m.mrrRelInc, mrrRelInc, alpha);
    mrrNew = exponentialSmooth(m.mrrNew, mrrNew, alpha);
    
    churn = exponentialSmooth(m.churn, churn, alpha);
    comm = exponentialSmooth(m.comm, comm, alpha);
    profiles = 0;
    accounts = 0;
    accsNew = 0;
    accsEnd = 0;
  }
  
  public void setOldValues(MonthlyMetrics old) {
    setOldMRR(old.mrr);
    
  }
  
}
