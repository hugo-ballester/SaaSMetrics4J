/**
 * Websays Opinion Analytics Engine
 *
 * (Websays Copyright © 2010-2014. All rights reserved. http://websays.com )
 *
 * Primary Author: Marco Martinez/Hugo Zaragoza
 * Contributors:
 * Date: Jul 7, 2014
 */
package websays.core.utils;

import java.util.Currency;

import org.apache.log4j.Logger;

public class CurrencyUtils {
  
  private static final Logger logger = Logger.getLogger(CurrencyUtils.class);
  
  public static final Currency EUR = Currency.getInstance("EUR");
  public static final Currency USD = Currency.getInstance("USD");
  public static final Currency GBP = Currency.getInstance("GBP");
  
  public synchronized static double toEuros(double x, Currency currency) {
    double rate = 1.;
    if (currency.equals(EUR)) {
      rate = 1.0;
    } else if (currency.equals(USD)) {
      rate = 0.89;
    } else if (currency.equals(GBP)) {
      rate = 1.37;
    } else {
      logger.error("UNKNOWN CONVERSION RATE FOR CURRENCY: " + currency.getCurrencyCode());
    }
    return x * rate;
    
  }
  
  public static String getCurrencySymbol(Currency currency) {
    if (currency.equals(EUR)) {
      return "€";
    } else if (currency.equals(GBP)) {
      return "£";
    } else if (currency.equals(USD)) {
      return "$";
    } else {
      return "???";
    }
  }
}
