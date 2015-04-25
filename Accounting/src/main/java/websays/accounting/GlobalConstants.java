/*
 *    SAS4J
 *
 *    Hugo Zaragoza, Websays.
 */
package websays.accounting;

import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.TimeZone;

import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import websays.accounting.Contracts.AccountFilter;

public class GlobalConstants {
  
  public static final String VERSION = "7.15";
  public static boolean roundDatesForMetrics = false;
  
  public static final DateTimeFormatter dtS = DateTimeFormat.forPattern("dd/MM/YY");
  public static final DateTimeFormatter dtLL = DateTimeFormat.forPattern("dd/MM/YYYY HH:mm:ss");
  
  public static final String WebsaysES = "Websays_ES", WebsaysUK = "Websays_UK";
  
  public static final List<AccountFilter> billingCenters = Arrays.asList(new AccountFilter[] {AccountFilter.BILLCENTER_ES,
      AccountFilter.BILLCENTER_UK});
  
  /**
   * number of months of normal commission before it is discounted
   */
  public static int COMMMISSION_MONTHS = 1;
  
  /**
   * % of remaining commission after COMMISSION_MONTHS elapsed (e.g. 0.25 would mean commission goes down to 1/4th; 0.0 would mean no commission)
   */
  public static double COMMMISSION_REMAINING = 0.0;
  
  public static TimeZone timezone = TimeZone.getTimeZone("Europe/Madrid");
  
  public static void load(Properties props) {
    COMMMISSION_MONTHS = Integer.parseInt(props.getProperty("commission_months"));
    COMMMISSION_REMAINING = Double.parseDouble(props.getProperty("commission_remaining"));
  }
  
}
