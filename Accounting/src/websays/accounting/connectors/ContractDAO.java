/*
 *    SAS4J
 *
 *    Hugo Zaragoza, Websays.
 */
package websays.accounting.connectors;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Date;

import websays.accounting.Contract;
import websays.accounting.Contracts;
import websays.accounting.Contracts.AccountFilter;

public class ContractDAO extends MySQLDAO {
  
  private static final String COLUMNS_READ = "contract.id, contract.name, start, end, type, mrr, fixed, prizing, client_id, client.name";
  private static final String tableName = "(contract LEFT JOIN client ON contract.client_id=client.id)";
  
  protected Connection getConnection() throws SQLException {
    return DatabaseManager.getConnection();
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
        Contract a = readFromResulset(r);
        accs.add(a);
        if (getNumberOfProfiles) {
          a.profiles = getNumberOfProfiles(a.id);
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
    int column = 1;
    int id = rs.getInt(column++);
    String name = rs.getString(column++);
    Date start = rs.getDate(column++);
    Date end = null;
    if (rs.getInt(column++) != 0) {
      end = rs.getDate(column - 1);
    }
    Contract.Type type = Contract.Type.valueOf(rs.getString(column++));
    Double mrr = rs.getDouble(column++);
    if (rs.wasNull()) {
      mrr = null;
    }
    Double fix = rs.getDouble(column++);
    if (rs.wasNull()) {
      fix = null;
    }
    String prizing = rs.getString(column++);
    Integer client_id = rs.getInt(column++);
    String cname = rs.getString(column++);
    
    Contract a = null;
    if (prizing == null) {
      a = new Contract(id, name, type, client_id, start, end, mrr, fix);
    } else {
      a = new Contract(id, name, type, client_id, start, end, prizing);
    }
    a.client_name = cname;
    return a;
  }
  
  /**
   * @param c
   * @return updates c.id and returns it
   * @throws SQLException
   */
  public int create(Contract c) throws SQLException {
    
    String query = "INSERT INTO contract (" //
        + "name,start,end,type, mrr, fixed , prizing, client_id, main_profile_id" + ")" + " values(?,?,?,?,?,?,?,?,?)";
    
    PreparedStatement p = null;
    Connection con = null;
    Integer key = null;
    try {
      
      con = getConnection();
      p = con.prepareStatement(query, Statement.RETURN_GENERATED_KEYS);
      
      int i = 1;
      p.setString(i++, c.name);
      p.setDate(i++, new java.sql.Date(c.startContract.getTime()));
      p.setDate(i++, new java.sql.Date(c.endContract.getTime()));
      p.setString(i++, c.type.toString());
      p.setDouble(i++, c.monthlyPrize);
      p.setDouble(i++, c.fixPrize);
      p.setString(i++, c.prizingName);
      p.setInt(i++, c.client_id);
      p.setInt(i++, c.main_profile_id);
      p.executeUpdate();
      ResultSet rs = p.getGeneratedKeys();
      rs.next();
      key = rs.getInt(1);
      c.id = key;
      
    } finally {
      super.close(p);
      super.close(con);
    }
    return key;
  }
  
  // TODO: replace Queries.initContext by a stand-alone option
  public static Contracts loadAccounts(boolean connectToDB, File dumpDataFile, File pricingFile) throws Exception {
    Contracts contracts;
    if (connectToDB) {
      ContractDAO adao = new ContractDAO();
      contracts = adao.getAccounts(AccountFilter.contractedORproject, true);
      contracts.loadPrizeNames(pricingFile);
      contracts.linkPrizes();
      if (dumpDataFile != null) { // save for future use without Internet connection.
        contracts.save(dumpDataFile);
      }
    } else {
      // load last saved
      contracts = Contracts.load(dumpDataFile);
    }
    return contracts;
  }
  
}
