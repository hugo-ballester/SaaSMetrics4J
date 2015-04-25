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
import org.junit.Assert;
import org.junit.Test;

/**
 * @author hugoz
 *
 */
public class JodaUtilsTest {
  
  @Test
  public void testSeveralMonthOps() {
    
    LocalDate l1 = new LocalDate(2014, 10, 1);
    
    LocalDate l2 = l1;
    Assert.assertEquals(0, JodaUtils.monthsDifference(l1, l2));
    Assert.assertTrue(JodaUtils.isSameMonthAndYear(l1, l2));
    
    l2 = l1.plusDays(2);
    Assert.assertEquals(0, JodaUtils.monthsDifference(l1, l2));
    Assert.assertTrue(JodaUtils.isSameMonthAndYear(l1, l2));
    
    l2 = l1.withDayOfMonth(31);
    Assert.assertEquals(0, JodaUtils.monthsDifference(l1, l2));
    Assert.assertTrue(JodaUtils.isSameMonthAndYear(l1, l2));
    
    l2 = l1.withDayOfMonth(31).plusDays(1);
    Assert.assertEquals(1, JodaUtils.monthsDifference(l1, l2));
    Assert.assertFalse(JodaUtils.isSameMonthAndYear(l1, l2));
    
    l2 = new LocalDate(2014, 7, 17);
    l1 = new LocalDate(2014, 10, 17);
    Assert.assertEquals(-3, JodaUtils.monthsDifference(l1, l2));
    Assert.assertFalse(JodaUtils.isSameMonthAndYear(l1, l2));
    
    l2 = l2.plusMonths(24);
    Assert.assertEquals(21, JodaUtils.monthsDifference(l1, l2));
    Assert.assertFalse(JodaUtils.isSameMonthAndYear(l1, l2));
    
    // nearby dates of different month:
    l1 = new LocalDate(2014, 10, 31);
    l2 = l1.plusDays(2);
    Assert.assertEquals(1, JodaUtils.monthsDifference(l1, l2));
    Assert.assertFalse(JodaUtils.isSameMonthAndYear(l1, l2));
    
  }
  
}
