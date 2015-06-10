/*
 *    SaaSMetrics4J : https://github.com/hugozaragoza/SaaSMetrics4J
 *
 *    (c) 2014, Hugo Zaragoza, Websays.
 */
package websays.accounting.connectors;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;

import org.apache.log4j.Logger;

public class MySQLDAO {
  
  static Logger logger = Logger.getLogger(MySQLDAO.class);
  
  /**
   * Delete all records from tables
   * 
   * @param tableName
   *          list of tables
   * @throws SQLException
   */
  public void deleteAll(Connection con, String... tablesName) {
    try {
      for (String tableName : tablesName) {
        try {
          if (logger.isTraceEnabled()) {
            logger.trace("Deleting all rows from table " + tableName);
          }
          doDeleteAll(con, tableName, false);
          if (logger.isTraceEnabled()) {
            logger.trace("Records deleted");
          }
        } catch (SQLException e) {
          logger.error("Unable to delete rows from table " + tableName, e);
        }
      }
    } finally {
      close(con);
    }
  }
  
  /**
   * Delete all records from table.
   * 
   * @param tableName
   * @throws SQLException
   */
  public void deleteAll(Connection con, String tableName) throws SQLException {
    doDeleteAll(con, tableName, true);
  }
  
  /**
   * Delete all records from table.
   * 
   * @param tableName
   * @throws SQLException
   */
  private void doDeleteAll(Connection con, String tableName, boolean closeConnection) throws SQLException {
    String query = "DELETE FROM " + tableName;
    PreparedStatement p = null;
    try {
      p = con.prepareStatement(query);
      p.executeUpdate();
    } finally {
      close(p);
      if (closeConnection) {
        close(con);
      }
    }
  }
  
  public void deleteById(Connection con, String tableName, String nameFieldId, long id) throws SQLException {
    String query = "DELETE FROM " + tableName + " WHERE " + nameFieldId + " = ?";
    PreparedStatement p = null;
    try {
      p = con.prepareStatement(query);
      p.setLong(1, id);
      p.executeUpdate();
      
    } finally {
      close(p);
      close(con);
    }
  }
  
  public void deleteById(Connection con, String tableName, String nameFieldId, int id) throws SQLException {
    String query = "DELETE FROM " + tableName + " WHERE " + nameFieldId + " = ?";
    PreparedStatement p = null;
    try {
      p = con.prepareStatement(query);
      p.setInt(1, id);
      p.executeUpdate();
    } finally {
      close(p);
      close(con);
    }
  }
  
  public static void close(ResultSet r, PreparedStatement p, Connection c) {
    if (r != null) {
      close(r);
    }
    if (p != null) {
      close(p);
    }
    if (c != null) {
      close(c);
    }
  }
  
  public static void close(ResultSet r) {
    if (r != null) {
      try {
        r.close();
      } catch (Exception e) {
        logger.error("Unable to close ResultSet", e);
      }
    }
  }
  
  public static void close(PreparedStatement p) {
    if (p != null) {
      try {
        p.close();
      } catch (Exception e) {
        logger.error("Unable to close sql PreparedStatement", e);
      }
    }
  }
  
  public static void close(Statement stmt) {
    if (stmt != null) {
      try {
        stmt.close();
      } catch (Exception e) {
        logger.error("Unable to close sql Statement", e);
      }
    }
  }
  
  public static void close(Connection con) {
    if (con != null) {
      try {
        con.close();
      } catch (Exception e) {
        logger.error("Unable to close connection to database", e);
      }
    }
  }
  
  public void executeUpdate(Connection con, String cmd) throws SQLException {
    Statement stm = null;
    try {
      stm = con.createStatement();
      stm.executeUpdate(cmd);
    } finally {
      close(stm);
      close(con);
    }
  }
  
  /**
   * Execute all SQL extracted from buffer. Buffer can has more than one SQL string separated by semicolon.
   * 
   * NOTE: every SQL must be ended by character semicolon (;)
   * 
   * @param con
   *          conection to database; connection is closed before return
   * @param buffer
   *          with zero, one or more sql separated by semicolon.
   * @throws SQLException
   */
  public void executeSQLsSeparatedBySemiColon(Connection con, String buffer) throws SQLException {
    String[] listSQL = buffer.split(";");
    
    Statement stm = null;
    try {
      for (String sql : listSQL) {
        sql = sql.trim();
        if (sql.length() == 0) {
          continue;
        }
        
        try {
          stm = con.createStatement();
          stm.executeUpdate(sql);
        } finally {
          close(stm);
        }
      }
    } finally {
      close(con);
    }
    
  }
  
  public boolean tableExists(Connection con, String dbName, String tableName) throws SQLException {
    if (dbName == null || dbName.length() == 0) {
      throw new IllegalArgumentException("dbName can not be null");
    }
    if (tableName == null || tableName.length() == 0) {
      throw new IllegalArgumentException("tableName can not be null");
    }
    
    String cmd = "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = '" + dbName + "' AND table_name = '" + tableName
        + "';";
    
    Statement stmt = null;
    ResultSet rs = null;
    
    try {
      stmt = con.createStatement();
      rs = stmt.executeQuery(cmd);
      Long numRows = null;
      if (rs.next()) {
        numRows = rs.getLong(1);
      }
      if (numRows != null && numRows.longValue() > 0) {
        return true;
      }
      return false;
    } finally {
      close(rs);
      close(stmt);
      close(con);
    }
  }
  
  /**
   * Construct a condition in with the next format [field] in (?,?,?)
   * 
   * @param numParameters
   *          number of question mark to append
   */
  public String makeConditionIn(String fieldName, int numParameters) {
    StringBuffer strRes = new StringBuffer(" ").append(fieldName).append(" in (");
    for (int i = 0; i < numParameters; i++) {
      if (i != 0) {
        strRes.append(",");
      }
      strRes.append("?");
    }
    strRes.append(")");
    return strRes.toString();
  }
  
  /**
   * Set a parameter of type timestamp. If value to pass sql is null, calls function
   * {@link PreparedStatement#setNull(position, java.sql.Types.TIMESTAMP)}
   * 
   * @param position
   *          of argument or question mark
   * @param preparedStatement
   * @param value
   *          to pass to prepared statement
   * @throws SQLException
   */
  protected void setParameterTimestamp(int position, PreparedStatement preparedStatement, java.util.Date value) throws SQLException {
    if (value != null) {
      java.sql.Timestamp timeStamp = new java.sql.Timestamp(value.getTime());
      preparedStatement.setTimestamp(position++, timeStamp);
    } else {
      preparedStatement.setNull(position, java.sql.Types.TIMESTAMP);
    }
  }
  
  /**
   * Set a parameter of type string. If value to pass sql is null or empty, calls function
   * {@link PreparedStatement#setNull(position, java.sql.Types.VARCHAR)}
   * 
   * @param position
   *          of argument or question mark
   * @param preparedStatement
   * @param value
   *          to pass to prepared statement
   * @throws SQLException
   */
  protected void setParameterString(int position, PreparedStatement preparedStatement, String value) throws SQLException {
    if (value != null && value.length() > 0) {
      preparedStatement.setString(position++, value);
    } else {
      preparedStatement.setNull(position, java.sql.Types.VARCHAR);
    }
  }
  
  protected long getNumRows(ResultSet rs) throws SQLException {
    Long numRows = null;
    if (rs.next()) {
      numRows = rs.getLong(1);
    }
    if (numRows != null && numRows.longValue() > 0) {
      return numRows.longValue();
    }
    return 0;
  }
  
  /**
   * Reads strings in first column returned by the query and returns them
   * 
   * @param con
   * @param query
   * @return
   * @throws SQLException
   */
  public ArrayList<String> getStringArray(Connection con, String query) throws SQLException {
    ArrayList<String> ret = new ArrayList<String>();
    Statement stmt = null;
    ResultSet rs = null;
    try {
      stmt = con.createStatement();
      rs = stmt.executeQuery(query);
      while (rs.next()) {
        ret.add(rs.getString(1));
      }
    } finally {
      close(rs);
      close(stmt);
      close(con);
    }
    return ret;
  }
  
  protected long getNumRows(Connection con, String tableName) throws SQLException {
    String cmd = "SELECT COUNT(*) FROM " + tableName;
    
    Connection connection = null;
    ResultSet rs = null;
    Statement stmt = null;
    
    try {
      stmt = con.createStatement();
      rs = stmt.executeQuery(cmd);
      return this.getNumRows(rs);
    } finally {
      close(rs);
      close(stmt);
      close(connection);
    }
    
  }
  
  private long doGetNumRowsById(Connection con, String tableName, String fieldName, Object id) throws SQLException {
    String cmd = "SELECT COUNT(*) FROM " + tableName + " where " + fieldName + " =? ";
    
    Connection connection = null;
    ResultSet rs = null;
    PreparedStatement p = null;
    
    try {
      p = con.prepareStatement(cmd);
      if (id instanceof Long) {
        p.setLong(1, (Long) id);
      } else if (id instanceof String) {
        p.setString(1, (String) id);
      } else {
        throw new IllegalArgumentException("Id can be from class java.lang.Long or java.lang.String.");
      }
      
      rs = p.executeQuery();
      return this.getNumRows(rs);
    } finally {
      close(rs);
      close(p);
      close(connection);
    }
  }
  
  protected long getNumRowsById(Connection con, String tableName, String fieldName, long id) throws SQLException {
    return doGetNumRowsById(con, tableName, fieldName, id);
  }
  
  protected long getNumRowsById(Connection con, String tableName, String fieldName, String id) throws SQLException {
    return doGetNumRowsById(con, tableName, fieldName, id);
  }
  
  protected Long getSelectMax(Connection con, String tableName, String fieldName) throws SQLException {
    String cmd = "SELECT MAX(" + fieldName + ") FROM " + tableName;
    Statement stmt = null;
    ResultSet rs = null;
    
    try {
      stmt = con.createStatement();
      rs = stmt.executeQuery(cmd);
      Long maxValue = null;
      if (rs.next()) {
        maxValue = rs.getLong(1);
      }
      
      return maxValue;
    } finally {
      close(rs);
      close(stmt);
      close(con);
    }
  }
  
  protected String addAndCondition(String currentWhere, String newCondition) {
    if (newCondition == null || newCondition.length() == 0) {
      return currentWhere;
    }
    
    String result = currentWhere == null ? "" : currentWhere;
    if (result.length() > 0) {
      result += " and ";
    }
    result += newCondition;
    return result;
  }
  
}
