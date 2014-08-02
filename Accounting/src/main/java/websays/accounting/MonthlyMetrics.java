/*
 *    SaaSMetrics4J : https://github.com/hugozaragoza/SaaSMetrics4J
 *
 *    (c) 2014, Hugo Zaragoza, Websays.
 */
package websays.accounting;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import websays.accounting.Contracts.AccountFilter;
import websays.accounting.metrics.Metrics;
import websays.core.utils.maths.SequenceStatistics;

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
  
  public static final SimpleDateFormat df = new SimpleDateFormat("dd/MM/yyyy");
  
  public double oldMrr, oldChurn;
  public double mrr, comm;
  int accounts, profiles;
  SequenceStatistics profilesSt = new SequenceStatistics();
  SequenceStatistics mrrSt = new SequenceStatistics();
  
  /**
   * MRR from new accounts (accounts starting this month)
   */
  double mrrNew;
  
  /**
   * number of accounts ending this month
   */
  int accsEnd;
  
  /**
   * number of accounts starting this month
   */
  int accsNew;
  
  /**
   * Ammount of money that we will "loose" next month due to loosing accounts (due to accounts for which this is the current last month)
   */
  public double churn;
  
  /**
   * Ammount of money that we will "change" next month from existing accounts (due to accounts for which this month will pay a different ammount)
   */
  public double expansion;
  
  /**
   * Number of accounts in previous month
   */
  public int oldAccs;
  
  /**
   * should not be needed but for some reason can't force it from the outside
   */
  public static void setDebug() {
    Logger.getLogger(MonthlyMetrics.class).setLevel(Level.DEBUG);
    
  }
  
  public static MonthlyMetrics compute(int year, int month, AccountFilter filter, Contracts accounts, double oldMrr, double oldChurn,
      int oldAccs) throws ParseException {
    
    boolean metricDate = true;
    
    Date start = df.parse("1/" + month + "/" + year);
    MonthlyMetrics m = new MonthlyMetrics();
    m.oldMrr = oldMrr;
    m.oldChurn = oldChurn;
    
    Contracts accsFiltered = accounts.getActive(year, month, filter, metricDate);
    accsFiltered.sort(websays.accounting.Contracts.SortType.contract);
    
    for (Contract a : accsFiltered) {
      if (!a.isActive(start, true)) {
        continue;
      }
      double mrr = Metrics.getMRR(a, start, metricDate);
      m.accounts++;
      m.mrrSt.add(mrr);
      m.comm += Metrics.getCommission(a, start, metricDate);
      m.profilesSt.add((1.0) * a.profiles);
      m.expansion += Metrics.expansion(a, start);
      
      if (a.isLastMonth(start, metricDate)) {
        m.accsEnd++;
        m.churn += mrr;
      }
      if (a.isFirstMonth(start, metricDate)) {
        m.accsNew++;
        m.mrrNew += mrr;
      }
      if (logger.isDebugEnabled()) {
        logger.debug(String.format("%d\t%40s\t%d\t%6.0f\t%6.0f\t%6.0f", a.id, a.name, a.profiles, m.profilesSt.sum(), mrr, m.mrrSt.sum()));
      }
      
      // System.out.println(a.name + ": " + mrr);
    }
    m.mrr = m.mrrSt.sum();
    m.profiles = (int) m.profilesSt.sum();
    
    return m;
  }
  
  public static String headers() {
    
    return String.format("%s\t%3s\t%3s\t%6s\t%6s\t%6s\t%6s\t%6s\t%6s\t%6s\t%6s\t%3s\t%4s\t%3s\t%3s\t%6s",//
        "#C", "new", "end"//
        , "MRR", "delta", "%delta"//
        , "New", "Churn", "Exp", "avg", "min", "max", "#P", "avg", "min", "max", "comm");
  }
  
  public static String headersTop() {
    String s1 = "[\tCONT.\t\t]";
    String s2 = "\t[\tMRR\t\t\t\t\t\t]";
    String s3 = "\t[\tProf.\t\t]";
    String s4 = "[\tComm.]";
    
    return String.format("%s%s%s%s", s1, s2, s3, s4);
  }
  
  static double checkmrr = 0;
  
  public Object[] toRow() {
    double[] mrrA = mrrSt.getMinMaxSumAvg();
    double[] profA = profilesSt.getMinMaxSumAvg();
    double mrrDif = (mrr - oldMrr);
    double mrrRelInc = oldMrr > 0 ? mrrDif / oldMrr * 100. : 0;
    Object[] ret = new Object[] {//
    accounts, accsNew, accsEnd //
        , mrr, mrrDif, mrrRelInc //
        , mrrNew, oldChurn, expansion, mrrA[3], mrrA[0], mrrA[1], //
        profiles, profA[3], profA[0], profA[1], comm//
    };
    return ret;
  }
  
  @Override
  public String toString() {
    String ret = String.format(
    //
        "%d\t%d\t%d" //
            + "\t%6.0f\t%6.0f\t%4.1f%%" //
            + "\t%6.0f\t%6.0f\t%6.0f\t%6.0f\t%6.0f\t%6.0f" //
            + "\t%3d\t%3.0f\t%3.0f\t%3.0f\tC: %6.1f"//
        , toRow());
    // + //
    // "\t%6.0f\t%6.0f\t%6.0f%\t%6.0f\t%6.0f\t%6.0f\t%6.0f\t%3d\t%4.1f\t%3.0f\t%3.0f\t---%6.0f"
    //
    return ret;
  }
}
