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

import org.apache.log4j.Logger;

import websays.accounting.Contracts.AccountFilter;
import websays.accounting.connectors.ContractDAO;
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
  public double mrr;
  SequenceStatistics profilesSt = new SequenceStatistics();
  SequenceStatistics mrrSt = new SequenceStatistics();
  
  int endAccs;
  int newAccs;
  double newmrr;
  public double churn, expansion;
  public double oldMrr, oldChurn;
  
  public static Metrics compute(int year, int month, AccountFilter filter, Contracts accounts, double oldMrr, double oldChurn)
      throws ParseException, SQLException {
    
    ContractDAO cdao = new ContractDAO();
    
    Date start = df.parse("1/" + month + "/" + year);
    Metrics m = new Metrics();
    m.oldMrr = oldMrr;
    m.oldChurn = oldChurn;
    
    Contracts accsFiltered = accounts.getActive(year, month, filter, true);
    accsFiltered.sort(websays.accounting.Contracts.SortType.contract);
    
    for (Contract a : accsFiltered) {
      double mrr = a.mrr(start);
      m.accounts++;
      m.mrrSt.add(mrr);
      m.profilesSt.add((1.0) * a.profiles);
      m.expansion += a.expansion(start);
      
      if (a.isLastMonth(start, true)) {
        m.endAccs++;
        m.churn += mrr;
      }
      if (a.newThisMonth(start)) {
        m.newAccs++;
        m.newmrr += mrr;
      }
      if (logger.isDebugEnabled()) {
        logger.debug(String.format("%d\t%40s\t%d\t%.0f\t%.0f\t%.0f", a.id, a.name, a.profiles, m.profilesSt.sum(), mrr, m.mrrSt.sum()));
      }
      
      // System.out.println(a.name + ": " + mrr);
    }
    m.mrr = m.mrrSt.sum();
    m.profiles = (int) m.profilesSt.sum();
    
    return m;
  }
  
  public static String headers() {
    
    return String.format("%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s", "#C", "new", "end", "OldMRR", "NewMRR", "Churn",
        "Exp", "MRR", "0", "avg", "min", "max", "#P", "avg", "min", "max");
  }
  
  public static String headersTop() {
    return String.format("%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s", "[", "CONT.", "]", "[", "MRR\t\t\t\t\t\t", "]", "[", "Prof.\t", "]");
  }
  
  static double checkmrr = 0;
  
  @Override
  public String toString() {
    double[] mrrA = mrrSt.getMinMaxSumAvg();
    double[] profA = profilesSt.getMinMaxSumAvg();
    String ret = String.format("%d\t%d\t%d\t%.0f\t%.0f\t%.0f\t%.0f\t%.0f\t%.0f\t%.0f\t%.0f\t%.0f\t%d\t%.1f\t%.0f\t%.0f", accounts, newAccs,
        endAccs, oldMrr, newmrr, oldChurn, expansion, mrr, mrr - (oldMrr + newmrr - oldChurn + expansion), mrrA[3], mrrA[0], mrrA[1],
        profiles, profA[3], profA[0], profA[1]);
    return ret;
  }
  
}
