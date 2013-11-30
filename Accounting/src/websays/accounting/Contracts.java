/*
 *    SAS4J
 *
 *    Hugo Zaragoza, Websays.
 */
package websays.accounting;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
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

import utils.Utils;
import websays.accounting.Contract.Type;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

public class Contracts extends ArrayList<Contract> {
  
  public enum AccountFilter {
    contracted, project, contractedORproject, starting, ending, changed
  };
  
  static Logger logger = Logger.getLogger(Contracts.class);
  
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
  
  public Contracts getActive(int year, int month, AccountFilter filter) throws ParseException {
    Date date = Metrics.df.parse("1/" + month + "/" + year);
    return getActive(date, filter);
  }
  
  public Contracts getActive(Date date, AccountFilter filter) {
    Contracts ret = new Contracts();
    
    for (Contract a : this) {
      if (!a.isActive(date)) {
        continue;
      }
      
      if (filter != null)
        
        if (filter == AccountFilter.contracted) {
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
        } else if (filter == AccountFilter.starting) {
          if (!a.isFirstMonth(date)) {
            continue;
          }
        } else if (filter == AccountFilter.ending) {
          if (!a.isLastMonth(date)) {
            continue;
          }
        } else if (filter == AccountFilter.changed) {
          if (a.isFirstMonth(date) || a.mrrChange(date) == 0.) {
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
      p = Utils.file_read(prizeFile).split("\n");
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
  
  public Contracts getView(Pattern match) {
    Contracts ret = new Contracts();
    Matcher m = match.matcher("");
    for (Contract c : this) {
      m.reset(c.name);
      if (m.find()) {
        ret.add(c);
      }
    }
    return ret;
  }
}
