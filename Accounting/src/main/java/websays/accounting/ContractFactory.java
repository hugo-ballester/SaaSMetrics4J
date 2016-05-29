/*
 *    SAS4J
 *
 *    Hugo Zaragoza, Websays.
 */
package websays.accounting;

import java.util.ArrayList;
import java.util.List;

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
    
    // Basic C_x% : a fixed x%
    else if (schema.startsWith("C_")) {
      Integer i = Integer.parseInt(schema.substring(2));
      pct = 1.0 * i / 100.0;
      CommissionPlan com = new CommissionPlan(pct, pct * .25, COMMMISSION_MONTHS, commissionnee);
      ret.add(com);
    }
    
    // Basic C_x% : a fixed x%
    else if (schema.startsWith("RES_2016_1")) {
      CommissionPlan com1 = new CommissionPlan(0.30, 0.0, COMMMISSION_MONTHS, commissionnee);
      com1.commissionAppliesToFullFee = true;
      CommissionPlan com2 = new CommissionPlan(0.7 * 0.05, 0.01, COMMMISSION_MONTHS, "OA");
      com1.commissionAppliesToFullFee = true;
      ret.add(com1);
      ret.add(com2);
      if (!commission_base.equals(billTotal)) {
        if (commission_base.equals(billTotal * .7)) {
          logger.warn("RES_2016_1 commision base is correct but should be left empty!");
        }
        logger.error("RES_2016_1 commission base should be empty or [" + (.7 * billTotal) + "] but is [" + commission_base + "].");
      }
    }
    
    else if (schema.startsWith("COM_2016_1")) {
      CommissionPlan com = new CommissionPlan(0.05, 0.01, COMMMISSION_MONTHS, commissionnee);
      ret.add(com);
    }
    
    // UK 2015: Viqui .3, Oscar .2
    else if (schema.equals("UK1")) {
      if (commissionnee != null) {
        logger.warn("UK1 schema should not have commissionee! ignoring: " + commissionnee);
      }
      CommissionPlan com1 = new CommissionPlan(0.2, 0.2 * .25, COMMMISSION_MONTHS, "OA");
      CommissionPlan com2 = new CommissionPlan(0.3, 0.3 * .25, COMMMISSION_MONTHS, "VC");
      ret.add(com1);
      ret.add(com2);
    }
    
    else {
      logger.error("ERROR: unknown commission type [" + schema + "]");
      throw new Exception("ERROR: unknown commission type [" + schema + "]");
    }
    return ret;
  }
}
