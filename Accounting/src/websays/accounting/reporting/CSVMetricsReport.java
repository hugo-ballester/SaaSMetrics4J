/*
 *    SAS4J
 *
 *    Hugo Zaragoza, Websays.
 */
package websays.accounting.reporting;

import websays.accounting.MonthlyMetrics;

public class CSVMetricsReport {
  
  public String toStringLine1(MonthlyMetrics current, MonthlyMetrics average, boolean completeDetail) {
    String ret = String.format(
    //
        "%d\t%d\t%d" //
            + "\t%6.0f\t%6.0f\t%4.1f%%\t%6.0f\t%6.0f" //
            + "\t%6.0f\t%6.0f\t%4.1f%%" //
            + "\t%6.0f\t%6.0f\t%6.0f\t%6.0f\t%6.0f\t%6.0f" //
            + "\t%3d\t%3.0f\t%3.0f\t%3.0f\tC: %6.1f"//
        , toRow(current, average));
    // + //
    // "\t%6.0f\t%6.0f\t%6.0f%\t%6.0f\t%6.0f\t%6.0f\t%6.0f\t%3d\t%4.1f\t%3.0f\t%3.0f\t---%6.0f"
    //
    return ret;
  }
  
  public String headers(boolean completeDetail) {
    return String.format("%s\t%3s\t%3s\t" + //
        "%6s\t%6s\t%6s\t%6s\t%6s\t" + //
        "%6s\t%6s\t%6s\t" + //
        "%3s\t%4s\t%3s\t%3s\t%6s",//
        "#C", "new", "end" //
        , "MRR", "delta", "%delta" //
        , "New", "Churn", "Exp", "avg", "min", "max", "#P", "avg", "min", "max", "comm");
  }
  
  public String headersTop(boolean completeDetail) {
    String s1 = "[\tCONT.\t]";
    String s2 = "\t[\t~MRR\t\t\t\t]";
    String s3 = "  [\tMRR\t\t\t\t\t\t\t]";
    String s4 = "\t[\tProf.\t\t]";
    String s5 = "[\tComm.]";
    
    return String.format("%s%s%s%s%s", s1, s2, s3, s4, s5);
  }
  
  public Object[] toRow(MonthlyMetrics current, MonthlyMetrics average) {
    double[] mrrA = current.mrrSt.getMinMaxSumAvg();
    double[] profA = current.profilesSt.getMinMaxSumAvg();
    Object[] ret = new Object[] {//
    current.accounts, current.accsNew, current.accsEnd //
        , average.mrr, average.mrrDif, average.mrrRelInc, average.mrrNew, average.churn //
        , current.mrr, current.mrrDif, current.mrrRelInc //
        , current.mrrNew, current.churn, current.expansion, mrrA[3], mrrA[0], mrrA[1] //
        , current.profiles, profA[3], profA[0], profA[1], current.comm//
    };
    return ret;
  }
  
  public void addline(String string) {
    // TODO Auto-generated method stub
    
  }
  
}
