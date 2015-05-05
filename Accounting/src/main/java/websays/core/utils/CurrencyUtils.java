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
import java.util.HashMap;

import org.apache.log4j.Logger;

public class CurrencyUtils {
  
  private static final Logger logger = Logger.getLogger(CurrencyUtils.class);
  
  public static final Currency EUR = Currency.getInstance("EUR");
  public static final Currency USD = Currency.getInstance("USD");
  public static final Currency GBP = Currency.getInstance("GBP");
  
  public static HashMap<String,Double> conversionRatesinEuros; // ammount(Currency) * conversionRate = ammount(EUR)
  public static boolean initted = false;
  
  static void init() {
    if (!initted) {
      initted = true;
      
      conversionRatesinEuros = new HashMap<String,Double>();
      conversionRatesinEuros.put(EUR.getCurrencyCode(), 1.0);
      conversionRatesinEuros.put(USD.getCurrencyCode(), 0.89); // EUR/USD
      conversionRatesinEuros.put(GBP.getCurrencyCode(), 1.37); // EUR/GBP
    }
  }
  
  public synchronized static double convert(double x, Currency currencyFrom, Currency currencyTo) {
    init();
    
    Double rate1 = getCononversionRateinEuros(currencyFrom);
    Double rate2 = getCononversionRateinEuros(currencyTo);
    
    double target = x * rate1 / rate2;
    
    return target;
    
  }
  
  private static Double getCononversionRateinEuros(Currency currencyFrom) {
    init();
    
    Double rate = conversionRatesinEuros.get(currencyFrom.getCurrencyCode());
    if (rate == null) {
      logger.error("UNKNOWN CONVERSION RATE FOR CURRENCY: " + currencyFrom.getCurrencyCode());
      rate = null;
    }
    return rate;
  }
  
  /**
   * NOTE: had to use codes for comparison becayse defaultFractionDigits not always the same!
   * 
   * @param currency
   * @return
   */
  public static String getCurrencySymbol(Currency currency) {
    if (currency.getCurrencyCode().equals(EUR.getCurrencyCode())) {
      return "€";
    } else if (currency.getCurrencyCode().equals(GBP.getCurrencyCode())) {
      return "£";
    } else if (currency.getCurrencyCode().equals(USD.getCurrencyCode())) {
      return "$";
    } else {
      return "???";
    }
  }
  
  public static double toEuros(double x, Currency currency) {
    return convert(x, currency, EUR);
  }
}
