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

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import org.apache.commons.lang3.time.DateUtils;
import org.apache.log4j.Logger;

public class DateUtilsWebsays {
  
  private static final Logger logger = Logger.getLogger(DateUtilsWebsays.class);
  
  // TODO: phase this out, force code to specify:
  public static final String DEFAULT_TIMEZONE_NAME = "Europe/Madrid"; // used whenever unspecified or null
  public static final TimeZone DEFAULT_TIMEZONE = TimeZone.getTimeZone(DEFAULT_TIMEZONE_NAME);
  
  // ============================
  // TIME AND DATES:
  // ============================
  
  public static synchronized void calToBeginningOfHour(Calendar c) {
    c.set(Calendar.MINUTE, 0);
    c.set(Calendar.SECOND, 0);
    c.set(Calendar.MILLISECOND, 000);
  }
  
  public static synchronized void calToBeginningOfDay(Calendar c) {
    c.set(Calendar.HOUR_OF_DAY, 0);
    c.set(Calendar.MINUTE, 0);
    c.set(Calendar.SECOND, 0);
    c.set(Calendar.MILLISECOND, 000);
  }
  
  public static synchronized void calToEndOfDay(Calendar c) {
    // System.out.println(c.getTimeZone());
    // SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss:SSS");
    // sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
    // System.out.println(sdf.format(c.getTime()));
    c.set(Calendar.HOUR, 23);
    c.set(Calendar.MINUTE, 59);
    c.set(Calendar.SECOND, 59);
    c.set(Calendar.MILLISECOND, 999);
  }
  
  public static synchronized void calToEndOfYear(Calendar c) {
    c.set(Calendar.HOUR_OF_DAY, 23);
    c.set(Calendar.MINUTE, 59);
    c.set(Calendar.SECOND, 59);
    c.set(Calendar.MILLISECOND, 999);
    c.set(Calendar.MONTH, 11);
    c.set(Calendar.DAY_OF_MONTH, 31);
  }
  
  public static synchronized void calToEndOfMonth(Calendar c) {
    int lastDate = c.getActualMaximum(Calendar.DATE);
    c.set(Calendar.DATE, lastDate);
    c.set(Calendar.HOUR_OF_DAY, 23);
    c.set(Calendar.MINUTE, 59);
    c.set(Calendar.SECOND, 59);
    c.set(Calendar.MILLISECOND, 999);
  }
  
  public static synchronized int lastDayOfMonth(Calendar c) {
    // final int[] daysPerMonth = {31, 28, 31, 30, 31, 30, 31, 31, 30, 31,
    // 30, 31};
    // int month = c.get(Calendar.MONTH);
    // return daysPerMonth[month];
    return c.getActualMaximum(Calendar.DATE);
  }
  
  public static synchronized void calToLastDayOfMonth(Calendar c) {
    int day = lastDayOfMonth(c);
    c.set(Calendar.DATE, day);
  }
  
  public static synchronized boolean isLastDayOfMonth(Date d, TimeZone timezone) {
    Calendar c = getCalendarInstance(timezone);
    c.setTime(d);
    int ld = lastDayOfMonth(c);
    return ld == c.get(Calendar.DAY_OF_MONTH);
  }
  
  public static synchronized Date addMonthsAndDays(Date date, int months, int days, TimeZone timezone) {
    Calendar c = getCalendarInstance(date, timezone);
    c.add(Calendar.MONTH, months);
    c.add(Calendar.DAY_OF_YEAR, days);
    return c.getTime();
  }
  
  /**
   * @param now
   * @return
   */
  public static synchronized Date dateOneWeekAgo(Date now, TimeZone timezone) {
    Calendar c = getCalendarInstance(timezone);
    c.setTime(now);
    c.add(Calendar.DAY_OF_YEAR, -7);
    calToBeginningOfDay(c);
    return c.getTime();
  }
  
  public static synchronized Date dateEndOfPreviousDay(Date cal, TimeZone timezone) {
    cal = dateBeginningOfDay(cal, timezone);
    cal = DateUtils.addMilliseconds(cal, -1); // end of yesterday
    return cal;
  }
  
  public static synchronized Date dateEndOfDay(Date cal, TimeZone timezone) {
    Calendar c = Calendar.getInstance(guessNullTimeZone(timezone));
    c.setTime(cal);
    calToEndOfDay(c);
    return c.getTime();
  }
  
  public static synchronized Date dateEndOfYear(Date cal, TimeZone timezone) {
    // THIS BLOWS UP IN alpha: cal = DateUtils.ceiling(cal,
    // Calendar.DAY_OF_MONTH); // end of today
    Calendar c = Calendar.getInstance(guessNullTimeZone(timezone));
    c.setTime(cal);
    calToEndOfYear(c);
    return c.getTime();
  }
  
  public static synchronized Date dateBeginningOfDay(Date date, TimeZone timezone) {
    Calendar calendar = Calendar.getInstance(guessNullTimeZone(timezone));
    calendar.setTime(date);
    calToBeginningOfDay(calendar);
    
    return calendar.getTime();
  }
  
  public static synchronized Date dateBeginningOfHour(Date date, TimeZone timezone) {
    Calendar calendar = Calendar.getInstance(guessNullTimeZone(timezone));
    calendar.setTime(date);
    calToBeginningOfHour(calendar);
    return calendar.getTime();
  }
  
  public static synchronized Date dateBeginningOfYear(Date date, TimeZone timezone) {
    Calendar calendar = Calendar.getInstance(guessNullTimeZone(timezone));
    calendar.setTime(date);
    calendar.set(Calendar.MONTH, 0);
    calendar.set(Calendar.DATE, 1);
    calendar.set(Calendar.HOUR_OF_DAY, 0);
    calendar.set(Calendar.MINUTE, 0);
    calendar.set(Calendar.SECOND, 0);
    calendar.set(Calendar.MILLISECOND, 0);
    
    return calendar.getTime();
  }
  
  public static synchronized Date dateEndOfMonth(Date date, TimeZone timezone) {
    return dateEndOfMonth(date, 0, timezone);
  }
  
  public static synchronized Date dateEndOfMonth(Date date, int addMonths, TimeZone timezone) {
    Calendar cal = Calendar.getInstance(guessNullTimeZone(timezone));
    cal.setTime(date);
    cal.add(Calendar.MONTH, addMonths);
    calToEndOfMonth(cal);
    return cal.getTime();
  }
  
  public static synchronized Date dateBeginningOfMonth(Date cal, TimeZone timezone) {
    return dateBeginningOfMonth(cal, 0, timezone);
  }
  
  /**
   * @param cal
   * @param addMonths
   *          0 for beginning of this month, -1 for beginning last month, etc.
   * @return
   */
  public static synchronized Date dateBeginningOfMonth(Date cal, int addMonths, TimeZone timezone) {
    Calendar c = Calendar.getInstance(guessNullTimeZone(timezone));
    c.setTime(cal);
    c.add(Calendar.MONTH, addMonths);
    c.set(Calendar.DAY_OF_MONTH, 1);
    c.set(Calendar.HOUR_OF_DAY, 0);
    c.set(Calendar.MINUTE, 0);
    c.set(Calendar.SECOND, 0);
    c.set(Calendar.MILLISECOND, 0);
    return c.getTime();
  }
  
  public static synchronized Date dateBeginningOfWeek(Date cal, TimeZone timezone) {
    Calendar c = Calendar.getInstance(guessNullTimeZone(timezone));
    c.setTime(cal);
    c.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
    c.set(Calendar.HOUR_OF_DAY, 0);
    c.set(Calendar.MINUTE, 0);
    c.set(Calendar.SECOND, 0);
    c.set(Calendar.MILLISECOND, 0);
    return c.getTime();
  }
  
  public static synchronized boolean isSameDay(Calendar c1, Calendar c2) {
    int[] f = {Calendar.YEAR, Calendar.MONTH, Calendar.DAY_OF_MONTH};
    for (int i : f) {
      if (c1.get(i) != c2.get(i)) {
        return false;
      }
    }
    return true;
  }
  
  public static synchronized boolean isSameDay(Date c1, Date c2, TimeZone tz) {
    Calendar cal = getCalendarInstance(tz);
    cal.setTime(c1);
    Calendar cal2 = getCalendarInstance(tz);
    cal2.setTime(c2);
    return isSameDay(cal, cal2);
  }
  
  public static synchronized boolean isSameMonth(Calendar c1, Calendar c2) {
    int[] f = {Calendar.YEAR, Calendar.MONTH};
    for (int i : f) {
      if (c1.get(i) != c2.get(i)) {
        return false;
      }
    }
    return true;
  }
  
  public static synchronized boolean isSameMonth(Date d1, Date d2, TimeZone timezone) {
    Calendar c1 = getCalendarInstance(timezone);
    c1.setTime(d1);
    Calendar c2 = getCalendarInstance(timezone);
    c2.setTime(d2);
    return isSameMonth(c1, c2);
  }
  
  /**
   * //TODO this is not correct because time savings etc. Perhaps use JodaTime
   * 
   * @param gap
   * @return
   * @throws ParseException
   */
  public static synchronized int getHowManyDays(Date start, Date end) throws ParseException {
    // the +1 makes a whole day when starting form 0:0 to 24:59
    int howManyDays = (int) ((end.getTime() - start.getTime() + 1) / (1000 * 60 * 60 * 24));
    return howManyDays;
  }
  
  /**
   * 
   * @param gap
   * @return
   * @throws ParseException
   */
  public static synchronized int getHowManyMonths(Date start, Date end, TimeZone timezone) {
    Calendar s = getCalendarInstance(timezone);
    Calendar e = getCalendarInstance(timezone);
    s.setTime(start);
    e.setTime(end);
    
    int months = 12 * (e.get(Calendar.YEAR) - s.get(Calendar.YEAR));
    months += (e.get(Calendar.MONTH) - s.get(Calendar.MONTH));
    return months;
  }
  
  public static synchronized Date addDays(Date start, int days, TimeZone timezone) {
    Calendar cal = getCalendarInstance(timezone);
    cal.setTime(start);
    cal.add(Calendar.DATE, days); // minus number would decrement the days
    return cal.getTime();
  }
  
  public static synchronized Date addHours(Date start, int hours, TimeZone timezone) {
    Calendar cal = getCalendarInstance(timezone);
    cal.setTime(start);
    cal.add(Calendar.HOUR, hours); // minus number would decrement the hours
    return cal.getTime();
  }
  
  public static synchronized int getDayOfMonth(Date start, TimeZone timezone) {
    Calendar cal = getCalendarInstance(timezone);
    cal.setTime(start);
    return cal.get(Calendar.DAY_OF_MONTH);
  }
  
  /**
   * THIS SHOULD GO AWAY, force people to decide timezone or use default by not sending one
   * 
   * @param timezone
   * @return
   */
  
  private static synchronized TimeZone guessNullTimeZone(TimeZone timezone) {
    if (timezone == null) {
      return DEFAULT_TIMEZONE;
    } else {
      return timezone;
    }
  }
  
  public static synchronized Calendar getCalendarInstance(TimeZone timezone) {
    return Calendar.getInstance(guessNullTimeZone(timezone));
  }
  
  public static synchronized Calendar getCalendarInstance(Date date, TimeZone timezone) {
    Calendar cal = getCalendarInstance(timezone);
    cal.setTime(date);
    return cal;
  }
  
  public static synchronized SimpleDateFormat getSimpleDateFormat(String sampleDateFormatString, TimeZone timezone) {
    SimpleDateFormat sdf = new SimpleDateFormat(sampleDateFormatString);
    sdf.setTimeZone(guessNullTimeZone(timezone));
    return sdf;
  }
  
  public static synchronized int getYear(Date date, TimeZone timezone) {
    Calendar cal = getCalendarInstance(timezone);
    return cal.get(Calendar.YEAR);
  }
  
  public static synchronized int getMonth(Date date, TimeZone timezone) {
    Calendar cal = getCalendarInstance(date, timezone);
    return cal.get(Calendar.MONTH);
  }
  
  public static synchronized boolean isInPeriod(Calendar dC, Calendar staC, Calendar endC) {
    return (dC.compareTo(staC) >= 0 && dC.compareTo(endC) <= 0);
  }
  
  /**
   * @param year
   * @param month
   *          1-12
   * @param day
   * @param tz
   * @return
   */
  public static synchronized Calendar getCalendar(int year, int month, int day, TimeZone tz) {
    Calendar cal = getCalendarInstance(tz);
    cal.set(year, month - 1, day);
    calToBeginningOfDay(cal);
    return cal;
  }
  
}
