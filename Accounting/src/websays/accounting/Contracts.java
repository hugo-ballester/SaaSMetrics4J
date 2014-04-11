/*
 *    SAS4J
 *
 *    Hugo Zaragoza, Websays.
 */
package websays.accounting;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;

import websays.accounting.Contract.Type;
import websays.core.utils.DateUtilsWebsays;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

public class Contracts extends ArrayList<Contract> {
  
  private static final long serialVersionUID = 1L;
  
  static Logger logger = Logger.getLogger(Contracts.class);
  
  public enum AccountFilter {
    contract, project, contractedORproject, starting, ending, changed;
    
    public String whereBoolean() {
      if (this == contract)
        return "type='contract'";
      else if (this == project)
        return "type='project'";
      else if (this == contractedORproject)
        return "type='project' OR type='contract'";
      else {
        logger.error("AccountFilter=" + name() + " DOES NOT HAVE A whereBoolean");
        return "";
      }
    }
    
  };
  
  SimpleDateFormat df = new SimpleDateFormat("dd/MM/yyyy");
  HashMap<String,Pricing> prizings = new HashMap<String,Pricing>();
  
  public Contracts() {}
  
  public String display() {
    StringBuilder sb = new StringBuilder();
    for (Contract a : this) {
      sb.append(a.toString());
      sb.append("\n");
    }
    return sb.toString();
  }
  
  public Contracts getActive(int year, int month, AccountFilter filter, boolean metricDate) throws ParseException {
    Date date = Metrics.df.parse("1/" + month + "/" + year);
    return getActive(date, filter, metricDate);
  }
  
  public Contracts getEndingThisMonth(Date date, boolean metricDate) {
    Contracts ret = new Contracts();
    for (Contract a : this) {
      Date d = a.endContract;
      if (metricDate) {
        d = a.endMetric;
      }
      if (d == null || !DateUtilsWebsays.isSameMonth(date, d)) {
        continue;
      }
      ret.add(a);
    }
    return ret;
  }
  
  public Contracts getStartingThisMonth(Date date, boolean metricDate) {
    Contracts ret = new Contracts();
    for (Contract a : this) {
      Date d = a.startContract;
      if (metricDate) {
        d = a.startMetric;
      }
      if (d == null || !DateUtilsWebsays.isSameMonth(date, d)) {
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
  public Contracts getActiveBill(Date date) {
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
  public Contracts getActive(Date date, AccountFilter filter, boolean metricDate) {
    if (filter == null)
      return (Contracts) clone();
    
    Contracts ret = new Contracts();
    
    if (filter.equals(AccountFilter.ending))
      return getEndingThisMonth(date, metricDate);
    else if (filter.equals(AccountFilter.starting))
      return getStartingThisMonth(date, metricDate);
    
    for (Contract a : this) {
      if (!a.isActive(date, metricDate)) {
        continue;
      }
      
      if (filter != null)
        
        if (filter == AccountFilter.contract) {
          if (a.type != Type.contract) {
            continue;
          }
        } else if (filter == AccountFilter.project) {
          if (a.type != Type.project) {
            continue;
          }
        } else if (filter == AccountFilter.contractedORproject) {
          if (a.type != Type.project && a.type != Type.contract) {
            continue;
          }
        } else if (filter == AccountFilter.changed) {
          if (a.expansion(date) == 0) {
            continue;
          }
        } else {
          System.err.println("UNKONWN FILTER: " + filter);
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
  
  public void linkPrizes() {
    
    for (Contract a : this) {
      a.linkPrize(prizings);
    }
  }
  
  public void loadPrizeNames(File prizeFile) {
    
    String[] p = null;
    try {
      p = file_read(prizeFile).split("\n");
    } catch (Exception e) {
      System.err.println("COULD NOT LOAD prizeNames from file: " + prizeFile == null ? "null" : prizeFile.getAbsoluteFile());
      return;
    }
    
    int n = 0;
    for (String line : p) {
      try {
        if (line.startsWith("#")) {
          continue;
        }
        String[] r = line.split("\t");
        Pricing pr = new Pricing(r[0]);
        for (int i = 1; i < r.length; i += 2) {
          pr.add(df.parse(r[i]), Double.parseDouble(r[i + 1]));
        }
        prizings.put(pr.name, pr);
        n++;
      } catch (Exception e) {
        System.err.println("PARSING ERROR line:" + n + "\n" + line);
      }
    }
    logger.info("Prizenames loaded: " + prizings.size());
    
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
    Kryo kryo = new Kryo();
    Output output = null;
    try {
      output = new Output(new FileOutputStream(file));
      kryo.writeObject(output, this);
    } catch (FileNotFoundException e) {
      e.printStackTrace();
    } finally {
      output.close();
    }
  }
  
  public static Contracts load(File file) {
    Kryo kryo = new Kryo();
    Input input = null;
    Contracts ret = null;
    try {
      input = new Input(new FileInputStream(file));
      ret = kryo.readObject(input, Contracts.class);
    } catch (Exception e) {
      e.printStackTrace();
    }
    if (input != null) {
      input.close();
    }
    return ret;
    
  }
  
  public enum SortType {
    client, contract
  };
  
  public void sort(SortType sort) {
    
    if (sort == SortType.client) {
      Collections.sort(this, new Comparator<Contract>() {
        
        @Override
        public int compare(Contract o1, Contract o2) {
          if (o1 == null || o2 == null || o1.client_name == null || o2.client_name == null)
            return 0;
          
          if (o1.client_name.equals(o2.client_name))
            return o1.name.compareTo(o2.name);
          else
            return o1.client_name.compareTo(o2.client_name);
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
  
  public Contracts getView(String string) {
    return getView(Pattern.compile(Pattern.quote(string)));
  }
  
  public Contracts getView(Pattern string) {
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
  
  static String file_read(File filename) throws IOException {
    Reader in = new InputStreamReader(new FileInputStream(filename), "UTF8");
    BufferedReader i = new BufferedReader(in);
    StringBuffer b = new StringBuffer();
    while (i.ready()) {
      b.append(i.readLine() + "\n");
    }
    i.close();
    return b.toString();
  }
  
}
