/*
 *    SaaSMetrics4J : https://github.com/hugozaragoza/SaaSMetrics4J
 *
 *    (c) 2014, Hugo Zaragoza, Websays.
 */
package websays.accounting.connectors;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Currency;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.joda.time.LocalDate;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import websays.accounting.Commission;
import websays.accounting.Contract;
import websays.accounting.Contract.BillingSchema;
import websays.accounting.Contract.ContractDocument;
import websays.accounting.ContractFactory;
import websays.accounting.Contracts;
import websays.accounting.Contracts.AccountFilter;
import websays.accounting.Pricing;

public class ContractDAO extends MySQLDAO {
  
  private static final String COLUMNS_READ = "contract.id, contract.name, contract.start, contract.end, contract.contractedMonths, contract.type, contract.contract, contract.billingSchema, contract.currency_id, mrr, free, fixed, pricing, client_id, client.name, client.billingCenter, client.type, commissionMonthlyBase,commissionnee,commission_type,commissionnee2,commission_type2,comments_billing";
  private static final String tableName = "(contract LEFT JOIN client ON contract.client_id=client.id)";
  
  private HashMap<String,Pricing> pricingSchemaNames = new HashMap<String,Pricing>(0);
  
  public ContractDAO(String pricingFile) {
    super();
    HashMap<String,Pricing> p = loadPriceNames(pricingFile);
    if (p != null) {
      setPricing(p);
    }
  }
  
  // public static void updateProfilesPerContract(Contracts cs) {
  // String cmd = "SELECT c.id, COUNT(c.name) FROM profiles p JOIN contract c ON p.contract_id=c.id GROUP BY c.id";
  // PreparedStatement p = null;
  // Connection connection = null;
  // ResultSet r = null;
  // try {
  // connection = DatabaseManager.getConnection();
  // p = connection.prepareStatement(cmd);
  // r = p.executeQuery();
  // while (r.next()) {
  // int id = r.getInt(0);
  // int profiles = r.getInt(1);
  // Contract c = cs.get(id);
  // if (c == null) {
  // logger.error("COULD NOT FIND contract.id []" + id);
  // continue;
  // }
  // c.profiles = profiles;
  // }
  // } catch (Exception e) {
  // logger.error(e);
  // } finally {
  // close(r);
  // close(p);
  // close(connection);
  // }
  // }
  
  public static String getPriceFileFromDB() {
    String table = "frontend_property";
    String key = "key";
    String value = "value";
    String orderField = "updated";
    
    String st = "SELECT `" + value + "` FROM `" + table + "` WHERE `" + key + "`='accounting_pricing' ORDER BY `" + orderField + "` DESC";
    
    PreparedStatement p = null;
    Connection connection = null;
    ResultSet r = null;
    try {
      connection = DatabaseManager.getConnection();
      p = connection.prepareStatement(st);
      r = p.executeQuery();
      r.next();
      String pricingFile = r.getString(1);
      return pricingFile;
    } catch (Exception e) {
      logger.error(e);
      return null;
    } finally {
      close(r);
      close(p);
      close(connection);
    }
  }
  
  public static String getPricingFileFromFile(File priceFile) {
    String ret;
    try {
      ret = file_read(priceFile);
      return ret;
    } catch (Exception e) {
      System.err.print("\nCOULD NOT LOAD priceNames from file: ");
      System.err.println((priceFile == null ? "null" : priceFile.getAbsolutePath()) + "\n");
      return null;
    }
  }
  
  public static HashMap<String,Pricing> loadPriceNames(String priceFile) {
    HashMap<String,Pricing> pricings = new HashMap<String,Pricing>();
    DateTimeFormatter df = DateTimeFormat.forPattern("yyyy/MM/dd");
    
    int n = 0;
    String[] lines = priceFile.split("\n");
    for (String line : lines) {
      try {
        if (line.startsWith("#") || line.length() == 0) {
          continue;
        }
        String[] r = line.split("[\t ,;]+");
        Pricing pr = new Pricing(r[0]);
        for (int i = 1; i < r.length; i += 2) {
          LocalDate d = new LocalDate(df.parseLocalDate(r[i])); // TODO: do not parse to Date pr.add(d, Double.parseDouble(r[i + 1]));
          if (d.getDayOfMonth() != 1) {
            logger.error("PRICING STARTS ON A DATE DIFFERENT FORM THE 1st OF THE MONTH: " + d.toString());
          }
          double mrr = Double.parseDouble(r[i + 1]);
          pr.add(d, mrr);
        }
        pricings.put(pr.name, pr);
        n++;
      } catch (Exception e) {
        String[] r = line.split("[\t ,;]+");
        System.err.println("PARSING ERROR line:" + n + "\n" + line + "\n" + StringUtils.join(r, "|"));
        System.err.println(e.getMessage());
      }
    }
    logger.info("Pricing names loaded: " + pricings.size());
    return pricings;
  }
  
  public int getNumberOfProfiles(int contractId) throws SQLException {
    
    String st = "SELECT COUNT(profile_id) FROM profiles WHERE contract_id=" + contractId + " GROUP BY profile_id";
    
    PreparedStatement p = null;
    Connection connection = null;
    ResultSet r = null;
    try {
      connection = DatabaseManager.getConnection();
      p = connection.prepareStatement(st);
      r = p.executeQuery();
      int ret = 0;
      if (r.next()) {
        ret = r.getInt(1);
      }
      return ret;
    } catch (Exception e) {
      logger.error(e);
      return -1;
    } finally {
      super.close(r);
      super.close(p);
      super.close(connection);
    }
  }
  
  public Contracts getAccounts(AccountFilter filter, boolean getNumberOfProfiles) throws SQLException {
    Contracts accs = new Contracts();
    String st = "SELECT " + COLUMNS_READ + " FROM " + tableName;
    if (filter != null) {
      st += " WHERE " + filter.whereBoolean();
    }
    st += " ORDER BY client.name, contract.name";
    PreparedStatement p = null;
    Connection connection = null;
    ResultSet r = null;
    try {
      connection = DatabaseManager.getConnection();
      p = connection.prepareStatement(st);
      r = p.executeQuery();
      while (r.next()) {
        Contract c = readFromResulset(r);
        if (c != null) {
          accs.add(c);
          if (getNumberOfProfiles) {
            c.profiles = getNumberOfProfiles(c.id);
          }
        }
      }
    } catch (SQLException e) {
      logger.error("MYSQL QUERY ERROR ON: " + p.toString());
      throw (e);
      
    } finally {
      super.close(r);
      super.close(p);
      super.close(connection);
    }
    logger.info("Accounts loaded: " + accs.size());
    return accs;
  }
  
  private Contract readFromResulset(ResultSet rs) {
    Integer id = null;
    try {
      
      // READ ROW:
      int column = 1;
      id = rs.getInt(column++);
      String name = rs.getString(column++);
      Date start = rs.getDate(column++);
      LocalDate startJ = new LocalDate(start); // WARNING this uses local timezone!
      Date end = null;
      LocalDate endJ = null;
      if (rs.getInt(column++) != 0) {
        end = rs.getDate(column - 1);
        endJ = new LocalDate(end);
      }
      Integer contracteMonths = rs.getInt(column++);
      Contract.Type type = Contract.Type.valueOf(rs.getString(column++));
      Contract.ContractDocument contractDocument = ContractDocument.valueOf(rs.getString(column++));
      
      BillingSchema bs = BillingSchema.valueOf(rs.getString(column++));
      String currency = rs.getString(column++);
      
      BigDecimal mrrBD = rs.getBigDecimal(column++);
      Double mrr = rs.wasNull() ? null : mrrBD.doubleValue();
      
      Boolean free = rs.getBoolean(column++);
      
      Double fix = rs.getDouble(column++); // casting to get the null
      String pricing = rs.getString(column++);
      Integer client_id = rs.getInt(column++);
      String client_name = rs.getString(column++);
      String billingCenter = rs.getString(column++);
      String client_type = rs.getString(column++);
      
      BigDecimal bd = (BigDecimal) rs.getObject(column++);
      Double cmb = bd == null ? null : bd.doubleValue(); // casting to get the null
      String commissionee = rs.getString(column++);
      String commisionLabel = rs.getString(column++);
      String commissionee2 = rs.getString(column++);
      String commisionLabel2 = rs.getString(column++);
      
      String comments_billing = rs.getString(column++);
      
      ArrayList<Commission> comms = new ArrayList<Commission>();
      if (commisionLabel != null) {
        List<Commission> comm = ContractFactory.commissionFromSchema(commisionLabel, cmb, commissionee);
        comms.addAll(comm);
      }
      if (commisionLabel2 != null) {
        List<Commission> comm = ContractFactory.commissionFromSchema(commisionLabel2, null, commissionee2);
        comms.addAll(comm);
      }
      
      // ----
      
      // Build object
      Contract a = null;
      if (pricing == null) {
        a = new Contract(id, name, type, bs, client_id, startJ, endJ, mrr, fix, comms);
      } else {
        if (pricingSchemaNames == null) {
          logger.error("PRCING SCHEMAS NOT LOADED!? Skipping.");
          return null;
        }
        Pricing p = pricingSchemaNames.get(pricing);
        if (p == null) {
          logger.error("UNKOWN PRICING SCHEMA NAME: '" + pricing + "'");
          System.exit(1);
        }
        a = new Contract(id, name, type, bs, client_id, startJ, endJ, p, comms);
      }
      a.free = free;
      a.contractDocument = contractDocument;
      a.client_name = client_name;
      a.contractedMonths = contracteMonths;
      a.currency = Currency.getInstance(currency);
      a.comments_billing = comments_billing;
      a.billingCenter = billingCenter;
      if (client_type != null) {
        a.client_type = Contract.ClientType.valueOf(client_type);
      }
      
      return a;
    } catch (Exception e) {
      String msg = null;
      if (id != null) {
        msg = "ERROR reading contract id=" + id + " (skipping)";
        logger.error(msg, e);
      } else {
        logger.error(msg, e);
      }
      return null;
    }
    
  }
  
  // TODO: replace Queries.initContext by a stand-alone option
  public static Contracts loadAccounts(boolean connectToDB, File dumpDataFile, String pricingFile) throws Exception {
    Contracts contracts;
    if (connectToDB) {
      ContractDAO adao = new ContractDAO(pricingFile);
      contracts = adao.getAccounts(null, true);
      
      if (dumpDataFile != null) { // save for future use without Internet connection.
        contracts.save(dumpDataFile);
      }
    } else {
      // load last saved
      contracts = Contracts.load(dumpDataFile);
      
    }
    return contracts;
  }
  
  private void setPricing(HashMap<String,Pricing> loadPriceNames) {
    pricingSchemaNames = loadPriceNames;
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
