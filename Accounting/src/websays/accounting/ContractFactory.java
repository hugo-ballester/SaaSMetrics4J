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
  
  public synchronized static List<Commission> commissionFromSchema(String schema, Double commission_base, String commissionnee) {
    if (schema == null) {
      return null;
    }
    ArrayList<Commission> ret = new ArrayList<Commission>();
    Double pct = null;
    if (schema.startsWith("C_")) {
      Integer i = Integer.parseInt(schema.substring(2));
      pct = 1.0 * i / 100.0;
      Commission com = new Commission(pct, commission_base, GlobalConstants.COMMMISSION_MONTHS, commissionnee);
      ret.add(com);
    } else if (schema.equals("UK1")) {
      if (commissionnee != null) {
        logger.warn("UK1 schema should not have commissionee! ignoring: " + commissionnee);
      }
      Commission com1 = new Commission(.2, commission_base, GlobalConstants.COMMMISSION_MONTHS, "OA");
      Commission com2 = new Commission(.3, commission_base, GlobalConstants.COMMMISSION_MONTHS, "VS");
      ret.add(com1);
      ret.add(com2);
    } else {
      logger.error("ERROR: unknown commission type");
      return null;
    }
    return ret;
  }
}
