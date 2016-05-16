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
  
  public synchronized static List<Commission> commissionFromSchema(double fix, String schema, Double commission_base, String commissionnee)
      throws Exception {
    if (schema == null || schema.equals("NONE")) {
      return null;
    }
    ArrayList<Commission> ret = new ArrayList<Commission>();
    Double pct = null;
    
    int COMMMISSION_MONTHS = 12;
    
    // Basic C_x% : a fixed x%
    if (schema.startsWith("C_")) {
      Integer i = Integer.parseInt(schema.substring(2));
      pct = 1.0 * i / 100.0;
      Commission com = new Commission(pct, pct * .25, COMMMISSION_MONTHS, commission_base, commissionnee);
      ret.add(com);
    }
    
    else if (schema.startsWith("Com2016_1")) {
      Commission com = new Commission(0.05, 0.01, COMMMISSION_MONTHS, commission_base, commissionnee);
      ret.add(com);
    }
    
    // UK 2015: Viqui .3, Oscar .2
    else if (schema.equals("UK1")) {
      if (commissionnee != null) {
        logger.warn("UK1 schema should not have commissionee! ignoring: " + commissionnee);
      }
      Commission com1 = new Commission(0.2, 0.2 * .25, COMMMISSION_MONTHS, commission_base, "OA");
      Commission com2 = new Commission(0.3, 0.3 * .25, COMMMISSION_MONTHS, commission_base, "VC");
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
