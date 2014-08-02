/*
 *    SaaSMetrics4J : https://github.com/hugozaragoza/SaaSMetrics4J
 *
 *    (c) 2014, Hugo Zaragoza, Websays.
 */
package websays.core.utils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import org.apache.commons.lang3.time.DateUtils;
import org.apache.log4j.Logger;

public class DateUtilsWebsays {
  
  @SuppressWarnings("unused")
  private static final Logger logger = Logger.getLogger(DateUtilsWebsays.class);
  
  /**
   * Helper class for "thread-safe" SimpleDateFormat'ers, holding them in a ThreadLocal ensures they are not called from different threads at the same
   * time, without resorting to synchronization
   */
  static class FormattersThreadCache extends ThreadLocal<Map<String,SimpleDateFormat>> {
    
    @Override
    protected Map<String,SimpleDateFormat> initialValue() {
      return new HashMap<String,SimpleDateFormat>();
    }
    
    public SimpleDateFormat get(String formatString) {
      Map<String,SimpleDateFormat> map = get();
      SimpleDateFormat sdf = map.get(formatString);
      if (sdf == null) {
        sdf = new SimpleDateFormat(formatString, Locale.US);
        sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
        map.put(formatString, sdf);
      }
      return sdf;
    }
  };
  
  public static final String SOLR_DATE_FORMAT1 = "yyyy-MM-dd";
  public static final String SOLR_DATE_FORMAT2 = "HH:mm:ss";
  // public static final SimpleDateFormat SOLR_sdf1 = new
  // SimpleDateFormat(SOLR_DATE_FORMAT1);
  // public static final SimpleDateFormat SOLR_sdf2 = new
  // SimpleDateFormat(SOLR_DATE_FORMAT2);
  // static final SimpleDateFormat SOLR_sdf3 = new
  // SimpleDateFormat(SOLR_DATE_FORMAT1 + "-" + SOLR_DATE_FORMAT2);
  // static {
  // SOLR_sdf1.setTimeZone(TimeZone.getTimeZone("GMT"));
  // SOLR_sdf2.setTimeZone(TimeZone.getTimeZone("GMT"));
  // SOLR_sdf3.setTimeZone(TimeZone.getTimeZone("GMT"));
  // }
  
  /** ThreadLocal with SimpleDateFormat'ers */
  private static final FormattersThreadCache fmtCache = new FormattersThreadCache();
  public static final SimpleDateFormat dateFormat1 = new SimpleDateFormat("dd/MM/yyyy");
  
  // public static Date parse(String date) throws ParseException {
  // return parse(ISO_DATE_FORMAT, date);
  // }
  //
  // public static String format(Date date) {
  // return format(ISO_DATE_FORMAT, date);
  // }
  
  private static Date parse(String format, String date) throws ParseException {
    return fmtCache.get(format).parse(date);
  }
  
  private static String format(String format, Date date) {
    
    // System.out.println("Date Format: " + date);
    SimpleDateFormat simpleDateFormat = fmtCache.get(format);
    // System.out.println("TimeZone Format: " +
    // simpleDateFormat.getTimeZone());
    return simpleDateFormat.format(date);
  }
  
  // ============================
  // TIME AND DATES:
  // ============================
  
  public static String toSOLRDate(Date date) {
    if (date == null) {
      return null;
    }
    return "" + format(SOLR_DATE_FORMAT1, date) + "T" + format(SOLR_DATE_FORMAT2, date) + "Z";
  }
  
  public static Date fromSOLRDate(String solrDate) {
    // TODO: do this properly
    int i = solrDate.indexOf("T");
    String s1 = solrDate.substring(0, i);
    String s2 = solrDate.substring(i + 1, solrDate.length() - 1);
    String s3 = s1 + "-" + s2;
    try {
      return parse(SOLR_DATE_FORMAT1 + "-" + SOLR_DATE_FORMAT2, s3);
    } catch (ParseException e) {
      return null;
    }
  }
  
  public static void calToStartOfDay(Calendar c) {
    c.set(Calendar.HOUR_OF_DAY, 0);
    c.set(Calendar.MINUTE, 0);
    c.set(Calendar.SECOND, 0);
    c.set(Calendar.MILLISECOND, 000);
  }
  
  public static void calToEndOfDay(Calendar c) {
    c.set(Calendar.HOUR_OF_DAY, 23);
    c.set(Calendar.MINUTE, 59);
    c.set(Calendar.SECOND, 59);
    c.set(Calendar.MILLISECOND, 000);
  }
  
  public static void calToEndOfYear(Calendar c) {
    c.set(Calendar.HOUR_OF_DAY, 23);
    c.set(Calendar.MINUTE, 59);
    c.set(Calendar.SECOND, 59);
    c.set(Calendar.MILLISECOND, 000);
    c.set(Calendar.MONTH, 11);
    c.set(Calendar.DAY_OF_MONTH, 31);
  }
  
  public static void calToEndOfMonth(Calendar c) {
    int lastDate = c.getActualMaximum(Calendar.DATE);
    c.set(Calendar.DATE, lastDate);
    c.set(Calendar.HOUR_OF_DAY, 23);
    c.set(Calendar.MINUTE, 59);
    c.set(Calendar.SECOND, 59);
    c.set(Calendar.MILLISECOND, 000);
  }
  
  public static boolean isLastDayOfMonth(Date d) {
    Calendar c = Calendar.getInstance();
    c.setTime(d);
    int ld = lastDayOfMonth(c);
    return ld == c.get(Calendar.DAY_OF_MONTH);
  }
  
  public static int lastDayOfMonth(Calendar c) {
    // final int[] daysPerMonth = {31, 28, 31, 30, 31, 30, 31, 31, 30, 31,
    // 30, 31};
    // int month = c.get(Calendar.MONTH);
    // return daysPerMonth[month];
    return c.getActualMaximum(Calendar.DATE);
  }
  
  public static void calToLastDayOfMonth(Calendar c) {
    int day = lastDayOfMonth(c);
    c.set(Calendar.DATE, day);
  }
  
  /**
   * @param now
   * @return
   */
  public static Date dateOneWeekAgo(Date now) {
    Calendar c = Calendar.getInstance();
    c.setTime(now);
    c.add(Calendar.DAY_OF_YEAR, -7);
    calToStartOfDay(c);
    return c.getTime();
  }
  
  public static Date dateEndOfPreviousDay(Date cal) {
    cal = dateBeginningOfDay(cal);
    cal = DateUtils.addMilliseconds(cal, -1); // end of yesterday
    return cal;
  }
  
  public static Date dateEndOfDay(Date cal) {
    // THIS BLOWS UP IN alpha: cal = DateUtils.ceiling(cal,
    // Calendar.DAY_OF_MONTH); // end of today
    Calendar c = Calendar.getInstance();
    c.setTime(cal);
    calToEndOfDay(c);
    return c.getTime();
  }
  
  public static Date dateEndOfYear(Date cal) {
    // THIS BLOWS UP IN alpha: cal = DateUtils.ceiling(cal,
    // Calendar.DAY_OF_MONTH); // end of today
    Calendar c = Calendar.getInstance();
    c.setTime(cal);
    calToEndOfYear(c);
    return c.getTime();
  }
  
  public static Date dateBeginningOfDay(Date cal) {
    // TODO: use calStartOfDay ?
    cal = DateUtils.truncate(cal, Calendar.DAY_OF_MONTH); // beginning of
    // today
    cal = DateUtils.setSeconds(cal, 0);
    cal = DateUtils.setMilliseconds(cal, 0);
    return cal;
  }
  
  public static Date dateBeginningOfYear(Date cal) {
    cal = DateUtils.truncate(cal, Calendar.YEAR); // beginning of today
    cal = DateUtils.setSeconds(cal, 0);
    cal = DateUtils.setMilliseconds(cal, 0);
    return cal;
  }
  
  public static Date dateEndOfMonth(Date date) {
    return dateEndOfMonth(date, 0);
  }
  
  public static Date dateEndOfMonth(Date date, int addMonths) {
    Calendar cal = Calendar.getInstance();
    cal.setTime(date);
    cal.add(Calendar.MONTH, addMonths);
    calToEndOfMonth(cal);
    return cal.getTime();
  }
  
  public static Date dateBeginningOfMonth(Date cal) {
    return dateBeginningOfMonth(cal, 0);
  }
  
  /**
   * @param cal
   * @param addMonths
   *          0 for beginning of this month, -1 for beginning last month, etc.
   * @return
   */
  public static Date dateBeginningOfMonth(Date cal, int addMonths) {
    Calendar c = Calendar.getInstance();
    c.setTime(cal);
    c.add(Calendar.MONTH, addMonths);
    c.set(Calendar.DAY_OF_MONTH, 1);
    c.set(Calendar.HOUR, 0);
    c.set(Calendar.MINUTE, 0);
    c.set(Calendar.SECOND, 0);
    c.set(Calendar.MILLISECOND, 0);
    return c.getTime();
  }
  
  public static Date dateBeginningOfWeek(Date cal) {
    // TODO can we do without a Calendar?
    Calendar c = Calendar.getInstance();
    c.setTime(cal);
    c.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
    Date d = dateBeginningOfDay(c.getTime());
    return d;
  }
  
  /**
   * Same year, month and day
   * 
   * @param c1
   * @param c2
   * @return
   */
  public static boolean isSameDay(Calendar c1, Calendar c2) {
    int[] f = {Calendar.YEAR, Calendar.MONTH, Calendar.DAY_OF_MONTH};
    for (int i : f) {
      if (c1.get(i) != c2.get(i)) {
        return false;
      }
    }
    return true;
  }
  
  public static boolean isSameDay(Date d1, Date d2) {
    Calendar c1 = getCalendar(d1);
    Calendar c2 = getCalendar(d2);
    return isSameDay(c1, c2);
  }
  
  public static boolean isSameMonth(Calendar c1, Calendar c2) {
    int[] f = {Calendar.YEAR, Calendar.MONTH};
    for (int i : f) {
      if (c1.get(i) != c2.get(i)) {
        return false;
      }
    }
    return true;
  }
  
  public static boolean isSameMonth(Date d1, Date d2) {
    Calendar c1 = Calendar.getInstance();
    c1.setTime(d1);
    Calendar c2 = Calendar.getInstance();
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
  public static int getHowManyDays(Date start, Date end) throws ParseException {
    // the +1 makes a whole day when starting form 0:0 to 24:59
    int howManyDays = (int) ((end.getTime() - start.getTime() + 1) / (1000 * 60 * 60 * 24));
    return howManyDays;
  }
  
  /**
   * Return the number of months in between. If start and end are of same months, returns 0
   * 
   * @param gap
   * @return
   * @throws ParseException
   */
  public static int getHowManyMonths(Date start, Date end) {
    Calendar s = Calendar.getInstance();
    Calendar e = Calendar.getInstance();
    s.setTime(start);
    e.setTime(end);
    
    int months = 12 * (e.get(Calendar.YEAR) - s.get(Calendar.YEAR));
    months += (e.get(Calendar.MONTH) - s.get(Calendar.MONTH));
    return months;
  }
  
  public static Date addDays(Date start, int days) {
    Calendar cal = Calendar.getInstance();
    cal.setTime(start);
    cal.add(Calendar.DATE, days); // minus number would decrement the days
    return cal.getTime();
  }
  
  public static Date addMonths(Date start, int months) {
    Calendar cal = Calendar.getInstance();
    cal.setTime(start);
    cal.add(Calendar.MONTH, months);
    return cal.getTime();
  }
  
  public static int getDayOfMonth(Date start) {
    Calendar cal = Calendar.getInstance();
    cal.setTime(start);
    return cal.get(Calendar.DAY_OF_MONTH);
  }
  
  public static int getMonth(Date start) {
    Calendar cal = Calendar.getInstance();
    cal.setTime(start);
    return cal.get(Calendar.MONTH);
  }
  
  public static int getYear(Date start) {
    Calendar cal = Calendar.getInstance();
    cal.setTime(start);
    return cal.get(Calendar.YEAR);
  }
  
  public static Calendar getFirstDayOfNextMonth(Date firstBillingDate) {
    Calendar c = Calendar.getInstance();
    c.setTime(firstBillingDate);
    if (c.get(Calendar.DAY_OF_MONTH) != 1) {
      c.set(Calendar.DAY_OF_MONTH, 1);
      c.add(Calendar.MONTH, 1);
    }
    return c;
  }
  
  /**
   * @param year
   * @param month
   *          {1..12}
   * @param i
   */
  public static Calendar getCalendar(int year, int month, int day) {
    Calendar c = Calendar.getInstance();
    c.set(Calendar.YEAR, year);
    c.set(Calendar.MONTH, month - 1);
    c.set(Calendar.DAY_OF_MONTH, day);
    c.set(Calendar.HOUR_OF_DAY, 0);
    c.set(Calendar.MINUTE, 0);
    c.set(Calendar.SECOND, 0);
    c.set(Calendar.MILLISECOND, 0);
    return c;
  }
  
  public static Calendar getCalendar(Date d) {
    Calendar c = Calendar.getInstance();
    c.setTime(d);
    return c;
  }
  
  /**
   * Only care about year, month, date, it ignores hours, minutes, etc.
   * 
   * Includes beginning and end of period in period.
   * 
   * @param dC
   * @param staC
   * @param endC
   */
  public static boolean isInPeriod_Day(Calendar dC, Calendar staC, Calendar endC) {
    
    if (DateUtilsWebsays.isSameDay(dC, staC) || DateUtilsWebsays.isSameDay(dC, endC)) {
      return true;
    }
    
    if (dC.after(staC) && dC.before(endC)) {
      return true;
    }
    return false;
    
  }
  
}
