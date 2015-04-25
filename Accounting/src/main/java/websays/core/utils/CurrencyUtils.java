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
  
  public static HashMap<Currency,Double> conversionRatesinEuros; // ammount(Currency) * conversionRate = ammount(EUR)
  
  static void init() {
    conversionRatesinEuros = new HashMap<Currency,Double>();
    conversionRatesinEuros.put(EUR, 1.0);
    conversionRatesinEuros.put(USD, 0.89); // EUR/USD
    conversionRatesinEuros.put(GBP, 1.37); // EUR/GBP
  }
  
  public synchronized static double convert(double x, Currency currencyFrom, Currency currencyTo) {
    if (!initted()) {
      init();
    }
    Double rate1 = getCononversionRateinEuros(currencyFrom);
    Double rate2 = getCononversionRateinEuros(currencyTo);
    
    double target = x * rate1 / rate2;
    
    return target;
    
  }
  
  private static Double getCononversionRateinEuros(Currency currencyFrom) {
    Double rate = conversionRatesinEuros.get(currencyFrom);
    if (rate == null) {
      logger.error("UNKNOWN CONVERSION RATE FOR CURRENCY: " + currencyFrom.getCurrencyCode());
      rate = null;
    }
    return rate;
  }
  
  private static boolean initted() {
    return conversionRatesinEuros != null;
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
  
  public static double toEuros(double x, Currency currency) {
    return convert(x, currency, EUR);
  }
}
