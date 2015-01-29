/*
 *    SAS4J
 *
 *    Hugo Zaragoza, Websays.
 */
package websays.accounting;

import java.text.SimpleDateFormat;
import java.util.Properties;

public class GlobalConstants {
  
  public static final String VERSION = "v7.9";
  
  public static final SimpleDateFormat dateFormat1 = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
  public static final SimpleDateFormat dateFormat2 = new SimpleDateFormat("dd/MM/yyyy");
  
  /**
   * number of months of normal commission before it is discounted
   */
  public static int COMMMISSION_MONTHS = 12;
  
  /**
   * % of remaining commission after COMMISSION_MONTHS elapsed (e.g. 0.25 would mean commission goes down to 1/4th; 0.0 would mean no commission)
   */
  public static double COMMMISSION_REMAINING = 0.0;
  
  public static void load(Properties props) {
    COMMMISSION_MONTHS = Integer.parseInt(props.getProperty("commission_months"));
    COMMMISSION_REMAINING = Double.parseDouble(props.getProperty("commission_remaining"));
  }
}
