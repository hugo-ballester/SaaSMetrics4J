/*
 *    SAS4J
 *
 *    Hugo Zaragoza, Websays.
 */
package websays.core.utils;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class TimeWebsays extends DateUtilsWebsays {
  
  private TimeZone timezone;
  private Locale locale;
  public SimpleDateFormat dateFormat1;
  
  public TimeWebsays(Locale locale, TimeZone timezone) {
    if (locale == null) {
      this.locale = Locale.getDefault();
    } else {
      this.locale = locale;
    }
    if (timezone == null) {
      this.timezone = DateUtilsWebsays.DEFAULT_TIMEZONE;
    } else {
      this.timezone = timezone;
    }
    dateFormat1 = new SimpleDateFormat("dd/MM/yyyy", locale);
    dateFormat1.setTimeZone(timezone);
    
  }
  
  public int getDayOfMonth(Date date) {
    return super.getDayOfMonth(date, timezone);
  }
  
  public Date addDays(Date date, int days) {
    return super.addDays(date, days, timezone);
  }
  
  public Date addMonths(Date date, int months) {
    return super.addMonthsAndDays(date, months, 0, timezone);
  }
  
  public Date addMonthsAndDays(Date date, int months, int days) {
    return super.addMonthsAndDays(date, months, days, timezone);
  }
  
  public int getYear(Date date) {
    return super.getYear(date, timezone);
  }
  
  /**
   * @param date
   * @return 0-11
   */
  public int getMonth(Date date) {
    return super.getMonth(date, timezone);
  }
  
  public Date dateEndOfMonth(Date date) {
    return super.dateEndOfMonth(date, timezone);
  }
  
  public int getHowManyMonths(Date start, Date end) {
    return super.getHowManyMonths(start, end, timezone);
  }
  
  /**
   * @param year
   * @param month
   *          1-12
   * @param day
   * @return
   */
  public Calendar getCalendar(int year, int month, int day) {
    return super.getCalendar(year, month, day, timezone);
  }
  
  public Calendar getCalendar(Date periodStart) {
    return getCalendarInstance(periodStart, timezone);
  }
  
  public Date dateBeginningOfMonth(Date date, int addMonths) {
    return super.dateBeginningOfMonth(date, addMonths, timezone);
  }
  
  public Date dateEndOfMonth(Date date, int addMonths) {
    return super.dateEndOfMonth(date, addMonths, timezone);
  }
  
  public boolean isSameMonth(Date date1, Date date2) {
    return super.isSameMonth(date1, date2, timezone);
  }
  
  public boolean isSameDay(Date date1, Date date2) {
    return super.isSameDay(date1, date2, timezone);
  }
  
  public SimpleDateFormat getSimpleDateFormat(String dateFormatString) {
    return super.getSimpleDateFormat(dateFormatString, timezone);
  }
}
