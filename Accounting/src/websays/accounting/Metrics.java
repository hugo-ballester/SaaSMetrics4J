/*
 *    SAS4J
 *
 *    Hugo Zaragoza, Websays.
 */
package websays.accounting;

import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import websays.accounting.Contracts.AccountFilter;
import websays.core.utils.maths.SequenceStatistics;

/**
 * Computes SAS Metrics for a set of Contracts
 * 
 * @author hugoz
 * 
 */
public class Metrics {
  
  private static final Logger logger = Logger.getLogger(Metrics.class);
  
  public static final SimpleDateFormat df = new SimpleDateFormat("dd/MM/yyyy");
  
  int accounts;
  int profiles;
  public double mrr, comm;
  SequenceStatistics profilesSt = new SequenceStatistics();
  SequenceStatistics mrrSt = new SequenceStatistics();
  
  int endAccs;
  int newAccs;
  double newmrr;
  public double churn, expansion;
  public double oldMrr, oldChurn;
  
  /**
   * should not be needed but for some reason can't force it from the outside
   */
  public static void setDebug() {
    Logger.getLogger(Metrics.class).setLevel(Level.DEBUG);
    
  }
  
  public static Metrics compute(int year, int month, AccountFilter filter, Contracts accounts, double oldMrr, double oldChurn)
      throws ParseException, SQLException {
    
    boolean metricDate = true;
    
    Date start = df.parse("1/" + month + "/" + year);
    Metrics m = new Metrics();
    m.oldMrr = oldMrr;
    m.oldChurn = oldChurn;
    
    Contracts accsFiltered = accounts.getActive(year, month, filter, true);
    accsFiltered.sort(websays.accounting.Contracts.SortType.contract);
    
    for (Contract a : accsFiltered) {
      double mrr = a.computeMRR(start, metricDate);
      m.accounts++;
      m.mrrSt.add(mrr);
      if (a.commission != null) {
        m.comm += a.commission * mrr;
      }
      m.profilesSt.add((1.0) * a.profiles);
      m.expansion += a.expansion(start);
      
      if (a.isLastMonth(start, metricDate)) {
        m.endAccs++;
        m.churn += mrr;
      }
      if (a.isFirstMonth(start, false)) {
        m.newAccs++;
        m.newmrr += mrr;
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
    
    return String.format("%s\t%3s\t%3s\t%6s\t%6s\t%6s\t%6s\t%6s\t%6s\t%6s\t%6s\t%6s\t%6s\t%3s\t%4s\t%3s\t%3s\t%6s", "#C", "new", "end",
        "OldMRR", "NewMRR", "Churn", "Exp", "MRR", "%inc", "0", "avg", "min", "max", "#P", "avg", "min", "max", "comm");
  }
  
  public static String headersTop() {
    return String.format("%s\t3%s\t%3s\t%6s\t%6s\t%3s\t%6s\t%6s\t%6s", "[", "CONT.", "]", "[", "MRR\t\t\t\t\t\t", "]", "[", "Prof.\t\t",
        "]\t[Comm.]");
  }
  
  static double checkmrr = 0;
  
  public Object[] toRow() {
    double[] mrrA = mrrSt.getMinMaxSumAvg();
    double[] profA = profilesSt.getMinMaxSumAvg();
    double mrrInc = oldMrr > 0 ? (mrr - oldMrr) / oldMrr * 100. : 0;
    double check = mrr - (oldMrr + newmrr - oldChurn + expansion);
    Object[] ret = new Object[] {accounts, newAccs, endAccs, oldMrr, newmrr, oldChurn, expansion, mrr, mrrInc, check, mrrA[3], mrrA[0],
        mrrA[1], profiles, profA[3], profA[0], profA[1], comm};
    return ret;
  }
  
  @Override
  public String toString() {
    String ret = String.format(
        "%d\t%d\t%d\t%6.0f\t%6.0f\t%6.0f\t%6.0f\t%6.0f\t%6.0f%%\t%6.0f\t%6.0f\t%6.0f\t%6.0f\t%3d\t%4.1f\t%3.0f\t%3.0f\t---%6.0f", toRow());
    return ret;
  }
  
}
