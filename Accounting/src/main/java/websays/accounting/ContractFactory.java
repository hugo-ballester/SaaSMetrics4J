/*
 *    SAS4J
 *
 *    Hugo Zaragoza, Websays.
 */
package websays.accounting;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;

public class ContractFactory {
  
  private static final Logger logger = Logger.getLogger(ContractFactory.class);
  
  public synchronized static List<CommissionPlan> commissionFromSchema(Double fix, String schema, Double billTotal, Double commission_base,
      String commissionnee) throws Exception {
    
    ArrayList<CommissionPlan> ret = new ArrayList<CommissionPlan>();
    Double pct = null;
    
    if (commission_base == null) {
      commission_base = billTotal;
    }
    
    int COMMMISSION_MONTHS = 12;
    
    // Basic C_x% : a fixed x%
    if (schema == null || schema.startsWith("NONE")) {}
    
    // ----------------------------------------------------------
    // Planes 2015
    // ----------------------------------------------------------
    
    Pattern p1year = Pattern.compile("C_(\\d+)_1Y");
    
    if (schema.startsWith("NONE")) {
      // no commission
    }
    
    // Basic C_x% : a fixed x%
    else if (schema.startsWith("C_")) {
      
      Matcher m = p1year.matcher(schema);
      
      // C_XX_1Y : XX% of commissionBase on the first year, 0 afterwards
      if (m.find()) {
        Integer i = Integer.parseInt(m.group(1));
        pct = 1.0 * i / 100.0;
        CommissionPlan com = new CommissionPlan(pct, 0.0, COMMMISSION_MONTHS, commissionnee);
        ret.add(com);
      }
      
      // C_XX : XX% of commissionBase on the first year, 1/4 on subsequent years
      else {
        Integer i = Integer.parseInt(schema.substring(2));
        pct = 1.0 * i / 100.0;
        CommissionPlan com = new CommissionPlan(pct, pct * .25, COMMMISSION_MONTHS, commissionnee);
        ret.add(com);
      }
    }
    
    // RES_2016_0 are previous to 2016-05 change
    else if (schema.startsWith("RES_2016_0")) {
      CommissionPlan com1 = new CommissionPlan(0.30, 0.0, COMMMISSION_MONTHS, commissionnee);
      com1.commissionAppliesToFullFee = true;
      CommissionPlan com2 = new CommissionPlan(0.70 * 0.10, 0.025, COMMMISSION_MONTHS, "OA");
      com1.commissionAppliesToFullFee = true;
      ret.add(com1);
      ret.add(com2);
      checkCOM1(billTotal, commission_base, .7, schema);
    }
    
    // UK 2015: Viqui .3, Oscar .2
    else if (schema.equals("UK1")) {
      if (commissionnee != null) {
        logger.warn("UK1 schema should not have commissionee! ignoring: " + commissionnee);
      }
      CommissionPlan com1 = new CommissionPlan(0.2, 0.2 * .25, COMMMISSION_MONTHS, "OA");
      CommissionPlan com2 = new CommissionPlan(0.3, 0.0, COMMMISSION_MONTHS, "VC");
      ret.add(com1);
      ret.add(com2);
    }
    
    // ----------------------------------------------------------
    // Planes 2016 Q2-4
    // ----------------------------------------------------------
    
    // RES_2016_1 are post to 2016-05 change
    else if (schema.startsWith("RES_2016_1")) {
      CommissionPlan com1 = new CommissionPlan(0.30, 0.0, COMMMISSION_MONTHS, commissionnee);
      com1.commissionAppliesToFullFee = true;
      CommissionPlan com2 = new CommissionPlan(0.70 * 0.05, 0.01, COMMMISSION_MONTHS, "OA");
      com1.commissionAppliesToFullFee = true;
      ret.add(com1);
      ret.add(com2);
      checkCOM1(billTotal, commission_base, .7, schema);
    }
    
    // RES_2016_1 are post to 2016-05 change
    else if (schema.startsWith("COM_2016_1")) {
      CommissionPlan com = new CommissionPlan(0.05, 0.01, COMMMISSION_MONTHS, commissionnee);
      ret.add(com);
    }
    
    else {
      logger.error("ERROR: unknown commission type [" + schema + "]");
      throw new Exception("ERROR: unknown commission type [" + schema + "]");
    }
    return ret;
  }
  
  private static void checkCOM1(Double billTotal, Double commission_base, double pct, String schema) {
    if (diff(commission_base, billTotal)) { // different commission base specified.
      if (diff(commission_base, (billTotal * .7))) {
        logger.error(schema + ": commission base should be empty or [" + (pct * billTotal) + "] but is [" + commission_base + "].");
      } else {
        logger.warn(schema + ": don't specify the commission base for RES_2016_0 contracts");
      }
    }
  }
  
  private static boolean diff(double d1, double d2) {
    return (Math.abs(d1 - d2) > 0.0001);
  }
}
