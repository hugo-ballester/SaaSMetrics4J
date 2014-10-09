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
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;

import websays.accounting.Contract;
import websays.accounting.Contract.BillingSchema;
import websays.accounting.Contracts;
import websays.accounting.Contracts.AccountFilter;
import websays.accounting.Pricing;

public class ContractDAO extends MySQLDAO {
  
  private static final SimpleDateFormat df = new SimpleDateFormat("dd/MM/yyyy");
  private static final String COLUMNS_READ = "contract.id, contract.name, contract.start, contract.end, contract.contractedMonths, contract.type, contract.billingSchema, contract.currency_id, mrr, fixed, pricing, client_id, client.name, commission_type,commissionnee,commissionMonthlyBase,comments_billing";
  private static final String tableName = "(contract LEFT JOIN client ON contract.client_id=client.id)";
  private HashMap<String,Pricing> pricingSchemaNames = new HashMap<String,Pricing>(0);
  
  public ContractDAO(File pricingFile) {
    super();
    HashMap<String,Pricing> p = loadPriceNames(pricingFile);
    if (p != null) {
      setPricing(p);
    }
  }
  
  protected Connection getConnection() throws SQLException {
    return DatabaseManager.getConnection();
  }
  
  public static HashMap<String,Pricing> loadPriceNames(File priceFile) {
    HashMap<String,Pricing> pricings = new HashMap<String,Pricing>();
    String[] p = null;
    try {
      p = file_read(priceFile).split("\n");
    } catch (Exception e) {
      System.err.print("\nCOULD NOT LOAD priceNames from file: ");
      System.err.println((priceFile == null ? "null" : priceFile.getAbsolutePath()) + "\n");
      return null;
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
        pricings.put(pr.name, pr);
        n++;
      } catch (Exception e) {
        System.err.println("PARSING ERROR line:" + n + "\n" + line);
      }
    }
    logger.info("Pricing names loaded: " + pricings.size());
    return pricings;
  }
  
  public int getNumberOfProfiles(int contractId) throws SQLException {
    
    String st = "SELECT COUNT(p.profile_id) FROM profiles p , " + tableName + " WHERE contract.id=" + contractId
        + " AND p.contract_id=contract.id";
    
    PreparedStatement p = null;
    Connection connection = null;
    ResultSet r = null;
    try {
      connection = getConnection();
      p = connection.prepareStatement(st);
      r = p.executeQuery();
      r.next();
      int ret = r.getInt(1);
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
      connection = getConnection();
      p = connection.prepareStatement(st);
      r = p.executeQuery();
      while (r.next()) {
        Contract c = readFromResulset(r);
        accs.add(c);
        if (getNumberOfProfiles) {
          c.profiles = getNumberOfProfiles(c.id);
        }
      }
    } finally {
      super.close(r);
      super.close(p);
      super.close(connection);
    }
    logger.info("Accounts loaded: " + accs.size());
    return accs;
  }
  
  private Contract readFromResulset(ResultSet rs) throws SQLException {
    
    // READ ROW:
    int column = 1;
    int id = rs.getInt(column++);
    String name = rs.getString(column++);
    Date start = rs.getDate(column++);
    Date end = null;
    if (rs.getInt(column++) != 0) {
      end = rs.getDate(column - 1);
    }
    Integer contracteMonths = rs.getInt(column++);
    Contract.Type type = Contract.Type.valueOf(rs.getString(column++));
    BillingSchema bs = BillingSchema.valueOf(rs.getString(column++));
    
    String currency = rs.getString(column++);
    
    Double mrr = rs.getDouble(column++);
    if (rs.wasNull()) {
      mrr = null;
    }
    Double fix = rs.getDouble(column++);
    if (rs.wasNull()) {
      fix = null;
    }
    String pricing = rs.getString(column++);
    Integer client_id = rs.getInt(column++);
    String cname = rs.getString(column++);
    String commisionLabel = rs.getString(column++);
    Double comm = commission(commisionLabel);
    String commissionee = rs.getString(column++);
    Double cmb = rs.getDouble(column++);
    String comments_billing = rs.getString(column++);
    // ----
    
    // Build object
    Contract a = null;
    if (pricing == null) {
      a = new Contract(id, name, type, bs, client_id, start, end, mrr, fix, comm);
    } else {
      if (pricingSchemaNames == null) {
        logger.error("PRCING SCHEMAS NOT LOADED!? Skipping.");
        return null;
      }
      Pricing p = pricingSchemaNames.get(pricing);
      if (p == null) {
        logger.error("UNKOWN PRICING SCHEMA NAME: '" + pricing + "'");
      }
      a = new Contract(id, name, type, bs, client_id, start, end, p, comm);
    }
    a.client_name = cname;
    a.contractedMonths = contracteMonths;
    a.commissionnee = commissionee;
    a.commissionMonthlyBase = cmb;
    a.currency = Contract.Currency.valueOf(currency);
    a.comments_billing = comments_billing;
    
    return a;
  }
  
  // TODO: replace Queries.initContext by a stand-alone option
  public static Contracts loadAccounts(boolean connectToDB, File dumpDataFile, File pricingFile) throws Exception {
    Contracts contracts;
    if (connectToDB) {
      ContractDAO adao = new ContractDAO(pricingFile);
      contracts = adao.getAccounts(AccountFilter.contractedORproject, true);
      
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
  
  private Double commission(String c) {
    if (c == null) {
      return null;
    } else if (c.equals("C_10")) {
      return 0.1;
    } else if (c.equals("C_30")) {
      return 0.3;
    } else if (c.equals("C_60")) {
      return 0.6;
    } else {
      System.out.println("ERROR: unknown commission type: '" + c + "'");
      return 0.;
    }
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
