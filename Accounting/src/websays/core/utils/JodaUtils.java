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

import org.joda.time.LocalDate;

/**
 * @author hugoz
 *
 */
public class JodaUtils {
  
  public static synchronized boolean isSameMonthAndYear(LocalDate c1, LocalDate c2) {
    return (c1.getYear() == c2.getYear()) && (c1.getMonthOfYear() == c2.getMonthOfYear());
  }
  
  public static synchronized int monthsDifference(LocalDate from, LocalDate to) {
    int months = (to.getYear() - from.getYear()) * 12;
    months += to.getMonthOfYear() - from.getMonthOfYear();
    return months;
  }
  
  public static LocalDate dateEndOfMonth(LocalDate d, int addMonths) {
    d = d.dayOfMonth().withMaximumValue();
    d = d.plusMonths(addMonths);
    return d;
  }
  
  public static LocalDate addMonthsAndDays(LocalDate startContract, Integer months, int days) {
    return startContract.plusMonths(months).plusDays(days);
  }
  
}
