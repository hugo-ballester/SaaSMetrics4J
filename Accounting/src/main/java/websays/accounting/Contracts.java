/*
 *    SaaSMetrics4J : https://github.com/hugozaragoza/SaaSMetrics4J
 *
 *    (c) 2014, Hugo Zaragoza, Websays.
 */
package websays.accounting;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.joda.time.LocalDate;

import websays.accounting.Contract.Type;
import websays.accounting.metrics.Metrics;
import websays.core.utils.JodaUtils;
import websays.core.utils.jodatime.LocalDateSerializer;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class Contracts extends ArrayList<Contract> {
  
  private static final long serialVersionUID = 1L;
  
  static Logger logger = Logger.getLogger(Contracts.class);
  
  public enum AccountFilter {
    CONTRACT, //
    PROJECT, //
    CONTRACTED_OR_PROJECT, //
    STARTING, // starting at a given supplied date
    ENDING, // ending at a given supplied date (because {@code endContract})
    AUTORENEW, // ending because {@code contractedMonths} reached, but no end-date, so auto-renew
    CHANGED, BILLCENTER_ES, BILLCENTER_UK; // changed at a given supplied date
    
    public String whereBoolean() {
      if (this == CONTRACT) {
        return "contract.type='contract'";
      } else if (this == PROJECT) {
        return "contract.type='project'";
      } else if (this == CONTRACTED_OR_PROJECT) {
        return "contract.type='project' OR contract.type='contract'";
      } else if (this == BILLCENTER_ES) {
        return "client.billingCenter='Websays_ES'";
      } else if (this == BILLCENTER_UK) {
        return "client.billingCenter='Websays_UK'";
      } else {
        logger.error("AccountFilter=" + name() + " DOES NOT HAVE A whereBoolean");
        return "";
      }
    }
    
    public Boolean accept(Contract c) {
      if (c == null) {
        return null;
      } else if (this == CONTRACT) {
        return c.type.equals(Type.subscription);
      } else if (this == CONTRACTED_OR_PROJECT) {
        return c.type.equals(Type.subscription) || c.type.equals(Type.project);
      } else if (this == PROJECT) {
        return c.type.equals(Type.project);
      } else if (this == BILLCENTER_ES) {
        return c.billingCenter.equals(GlobalConstants.WebsaysES);
      } else if (this == BILLCENTER_UK) {
        return c.billingCenter.equals(GlobalConstants.WebsaysUK);
      } else {
        logger.error("NOT IMPLEMENTED AccountFilter.accept(" + this + ")");
      }
      return null;
    }
    
  };
  
  static SimpleDateFormat df = new SimpleDateFormat("dd/MM/yyyy");
  
  public Contracts() {}
  
  public Contract getContract(int contractID) {
    for (Contract c : this) {
      if (c.getId() == contractID) {
        return c;
      }
    }
    return null;
    
  }
  
  public String display() {
    StringBuilder sb = new StringBuilder();
    for (Contract a : this) {
      sb.append(a.toString());
      sb.append("\n");
    }
    return sb.toString();
  }
  
  public Contracts getActive(int year, int month, AccountFilter filter, boolean metricDate) throws ParseException {
    LocalDate date = new LocalDate(year, month, 1);
    return getActive(date, filter, metricDate);
  }
  
  public Contracts getRenewThisMonth(LocalDate date, boolean metricDate) {
    Contracts ret = new Contracts();
    for (Contract a : this) {
      LocalDate d = a.endContract;
      if (metricDate) {
        d = a.endRoundDate;
      }
      if (d == null && a.contractedMonths != null) { // construct end date based on contractedMonths
        d = a.startContract;
        if (metricDate) {
          d = a.startRoundDate;
        }
        // renewing next month, so adding a full month:
        d = d.plusMonths(a.contractedMonths);
        if (d != null && JodaUtils.isSameMonthAndYear(date, d)) {
          ret.add(a);
        }
      }
      
    }
    return ret;
  }
  
  public Contracts getEndingThisMonth(LocalDate date, boolean metricDate) {
    Contracts ret = new Contracts();
    for (Contract a : this) {
      LocalDate d = a.endContract;
      if (metricDate) {
        d = a.endRoundDate;
      }
      
      if (d != null && JodaUtils.isSameMonthAndYear(date, d)) {
        ret.add(a);
      }
    }
    return ret;
  }
  
  public Contracts getStartingThisMonth(LocalDate date, boolean metricDate) {
    Contracts ret = new Contracts();
    for (Contract a : this) {
      LocalDate d = a.startContract;
      if (metricDate) {
        d = a.startRoundDate;
      }
      if (d == null || !JodaUtils.isSameMonthAndYear(date, d)) {
        continue;
      }
      ret.add(a);
    }
    return ret;
  }
  
  /**
   * @param date
   * @param filter
   * @return
   */
  public Contracts getActiveBill(LocalDate date) {
    Contracts ret = new Contracts();
    
    for (Contract a : this) {
      if (a.isActiveBill(date)) {
        ret.add(a);
      }
    }
    return ret;
  }
  
  /**
   * @param date
   * @param filter
   * @param metricDate
   *          : use approximated (rounded) date for metrics instead of real contract date.
   * @return
   */
  public Contracts getActive(LocalDate date, AccountFilter filter, boolean metricDate) {
    if (filter == null) {
      return (Contracts) clone();
    }
    
    Contracts ret = new Contracts();
    
    if (filter.equals(AccountFilter.ENDING)) {
      return getEndingThisMonth(date, metricDate);
    } else if (filter.equals(AccountFilter.STARTING)) {
      return getStartingThisMonth(date, metricDate);
    } else if (filter.equals(AccountFilter.AUTORENEW)) {
      return getRenewThisMonth(date, metricDate);
    }
    
    for (Contract a : this) {
      if (!a.isActive(date, metricDate)) {
        continue;
      }
      
      if (filter != null) {
        if (filter == AccountFilter.CONTRACT) {
          if (a.type != Type.subscription) {
            continue;
          }
        } else if (filter == AccountFilter.PROJECT) {
          if (a.type != Type.project) {
            continue;
          }
        } else if (filter == AccountFilter.CONTRACTED_OR_PROJECT) {
          if (a.type != Type.project && a.type != Type.subscription) {
            continue;
          }
        } else if (filter == AccountFilter.CHANGED) {
          if (Metrics.expansion(a, date) == 0) {
            continue;
          }
        } else {
          System.err.println("UNKONWN FILTER: " + filter);
        }
      }
      ret.add(a);
    }
    return ret;
    
  }
  
  public void remove(String clientName) {
    int r = 0;
    for (Iterator<Contract> iterator = iterator(); iterator.hasNext();) {
      Contract a = iterator.next();
      if (a.client_name != null && a.client_name.equals(clientName)) {
        iterator.remove();
        r++;
      }
    }
    System.out.println("REMOVED " + r);
  }
  
  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    for (Contract a : this) {
      sb.append(a.toString());
      sb.append("\n");
    }
    return sb.toString();
  }
  
  public void save(File file) {
    final GsonBuilder builder = new GsonBuilder().registerTypeAdapter(LocalDate.class, new LocalDateSerializer());
    final Gson gson = builder.create();
    
    ArrayList<String> data = new ArrayList<String>(1);
    data.add(gson.toJson(this));
    try {
      FileUtils.writeLines(file, "UTF8", data);
      logger.info("wrote dump file: " + file.getAbsolutePath());
    } catch (IOException e) {
      logger.error(e);
    }
    
    // Kryo kryo = new Kryo();
    // Output output = null;
    // try {
    // output = new Output(new FileOutputStream(file));
    // kryo.writeObject(output, this);
    // } catch (FileNotFoundException e) {
    // e.printStackTrace();
    // } finally {
    // output.close();
    // }
  }
  
  public static Contracts load(File file) {
    Contracts ret = null;
    
    final GsonBuilder builder = new GsonBuilder().registerTypeAdapter(LocalDate.class, new LocalDateSerializer());
    final Gson gson = builder.create();
    
    String json;
    try {
      json = FileUtils.readFileToString(file);
      ret = gson.fromJson(json, Contracts.class);
    } catch (IOException e) {
      logger.error(e);
    }
    
    // Kryo kryo = new Kryo();
    // Input input = null;
    // try {
    // input = new Input(new FileInputStream(file));
    // ret = kryo.readObject(input, Contracts.class);
    // } catch (Exception e) {
    // logger.error("Could not load " + (file == null ? "null" : file.getAbsolutePath()));
    // e.printStackTrace();
    // }
    // if (input != null) {
    // input.close();
    // }
    return ret;
    
  }
  
  public enum SortType {
    client, contract, date_ASC
  };
  
  /**
   * Sort by client anme or by contrct name
   * 
   * @param sort
   */
  public void sort(SortType sort) {
    
    if (sort == SortType.client) {
      Collections.sort(this, new Comparator<Contract>() {
        
        @Override
        public int compare(Contract o1, Contract o2) {
          if (o1 == null || o2 == null || o1.client_name == null || o2.client_name == null) {
            return 0;
          }
          
          if (o1.client_name.equals(o2.client_name)) {
            return o1.name.compareTo(o2.name);
          } else {
            return o1.client_name.compareTo(o2.client_name);
          }
        }
      });
    } else if (sort == SortType.date_ASC) {
      Collections.sort(this, new Comparator<Contract>() {
        
        @Override
        public int compare(Contract o1, Contract o2) {
          if (o1 == null || o2 == null || o1.startContract == null || o2.startContract == null) {
            return 0;
          }
          if (o1.startContract.isAfter(o2.startContract)) {
            return 1;
          } else if (o2.startContract.isAfter(o1.startContract)) {
            return -1;
          } else {
            return 0;
          }
        }
      });
      
    } else if (sort == SortType.contract) {
      Collections.sort(this, new Comparator<Contract>() {
        
        @Override
        public int compare(Contract o1, Contract o2) {
          return o1.name.compareTo(o2.name);
        }
      });
    }
    
  }
  
  public Contracts getView(AccountFilter filter) {
    Contracts ret = new Contracts();
    for (Contract c : this) {
      if (filter.accept(c)) {
        ret.add(c);
      }
    }
    return ret;
  }
  
  public Contracts getViewMatcingName(String string) {
    return getViewMatcingName(Pattern.compile(Pattern.quote(string)));
  }
  
  public Contracts getViewMatcingName(Pattern string) {
    Contracts ret = new Contracts();
    Matcher m = string.matcher("");
    for (Contract c : this) {
      m.reset(c.name);
      if (m.find()) {
        ret.add(c);
      }
    }
    return ret;
  }
  
  public String[] getCommissionnees() {
    HashSet<String> ret = new HashSet<String>();
    for (Contract c : this) {
      for (Commission com : c.commission) {
        if (com.commissionnee != null) {
          ret.add(com.commissionnee);
        }
      }
    }
    String[] rett = ret.toArray(new String[ret.size()]);
    Arrays.sort(rett);
    return rett;
  }
  
  /**
   * BY CONVENTION:
   * <ul>
   * <li>SET TO "project" all non-contracts
   * <li>SET TO "project" all contracts of less than minContractLength days
   * <li>REMOVE any projects with cost 0
   * </ul>
   * 
   * @param con
   * @param minContractLength
   */
  public void normalizeContracts(int minContractLength) {
    Iterator<Contract> cs = this.iterator();
    while (cs.hasNext()) {
      Contract c = cs.next();
      // if (c.isCostZero()) {
      // logger.warn("Ignoring contract " + c.name + " because is cost zero");
      // cs.remove();
      // } else {
      if (
      //
      (!c.type.equals(Type.subscription)) //
          && //
          (c.getDays() > 0 && c.getDays() < minContractLength) //
      ) {
        logger.debug("Setting contract " + c.name + " to type=project because duration is only " + c.getDays());
        c.type = Type.project;
      }
      // }
    }
    
  }
}
