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
  
  public static final String VERSION = "2.9";
  public static boolean roundDatesForMetrics = false;
  
  public static final DateTimeFormatter dtS = DateTimeFormat.forPattern("dd/MM/YY");
  public static final DateTimeFormatter dtLL = DateTimeFormat.forPattern("YYYY/MM/dd HH:mm:ss");
  
  public static final String WebsaysES = "Websays_ES", WebsaysUK = "Websays_UK";
  
  public static final List<AccountFilter> billingCenters = Arrays.asList(new AccountFilter[] {AccountFilter.BILLCENTER_ES,
      AccountFilter.BILLCENTER_UK});
  
  public static TimeZone timezone = TimeZone.getTimeZone("Europe/Madrid");
  
  public static void load(Properties props) {
    // e.f. param = Integer.parseInt(props.getProperty("param"));
    
  }
  
}
