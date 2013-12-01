/**
 * WebSays Core
 * Hugo Zaragoza 2011. 
 */
package websays.accounting.connectors;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import javax.sql.DataSource;

import org.apache.commons.dbcp.BasicDataSource;
import org.apache.log4j.Logger;

public class DatabaseManager {
  
  private static final int CONNECTION_RETRIES = 3;
  private static Logger logger = Logger.getLogger(DatabaseManager.class);
  
  static {
    try {
      Class.forName("com.mysql.jdbc.Driver").newInstance();
      
    } catch (Exception ex) {
      logger.error("Unable to load MySQL Driver.  Not found library in classpath.");
    }
  }
  
  public static String DEFAULT_DRIVER = "com.mysql.jdbc.Driver";
  public static int DEFAULT_INIT_SIZE_POOL = 0;
  
  protected static final String KEY = "dbBDGlobal";
  
  /**
   * map of datasources
   */
  private Map<String,DataSource> dataSourcesMap = new HashMap<String,DataSource>();
  
  /**
   * instance private
   */
  private static DatabaseManager databaseManagerInstance;
  
  private DatabaseManager() {
    // Contructor private..
  }
  
  public static boolean isStarted() {
    return DatabaseManager.databaseManagerInstance != null;
  }
  
  protected static DatabaseManager getInstance() {
    // nothing to do..
    if (DatabaseManager.databaseManagerInstance == null)
      throw new IllegalStateException("you can not access connections before call initDatabaseManager().");
    return DatabaseManager.databaseManagerInstance;
  }
  
  public enum DB {
    // generic names for backwards compatibility, should use more specific names instead:
    accounting
  };
  
  public static Connection getConnection() {
    return DatabaseManager.getInstance().getConnection(KEY);
  }
  
  public static void initDatabaseManager(String host, int port, String user, String pswd, String db, boolean forceRestart) {
    if (DatabaseManager.databaseManagerInstance != null && !forceRestart)
      return;
    
    DatabaseManager databaseManager = new DatabaseManager();
    
    databaseManager.setupDataSource(KEY, host, port, db, user, pswd);
    
    // Assign variable..
    DatabaseManager.databaseManagerInstance = databaseManager;
  }
  
  public static void shutdownDatabaseManager() {
    if (DatabaseManager.databaseManagerInstance == null) {
      DatabaseManager.logger.warn("calling to shutdownDatabaseManager() that is not initiated.");
      return;
    }
    
    // Destroy datasource and clear instance
    try {
      DatabaseManager.databaseManagerInstance.shutdown();
      DatabaseManager.databaseManagerInstance = null;
    } catch (SQLException e) {
      logger.error("Error shutdown dataSources ", e);
    }
  }
  
  private void shutdown() throws SQLException {
    for (DataSource dataSource : dataSourcesMap.values()) {
      shutdownDataSource(dataSource);
    }
    dataSourcesMap.clear();
  }
  
  private Connection getConnection(String key) {
    DataSource dataSource = dataSourcesMap.get(key);
    if (dataSource == null) {
      logger.error("No valid datasource for key " + key);
      return null;
    }
    // retry to connect with database 3 times
    int connectionRetries = 0;
    Connection con = null;
    while (connectionRetries <= CONNECTION_RETRIES) {
      try {
        // System.out.println("NUM ACTIVE:" + ((BasicDataSource) dataSource).getNumActive());
        con = dataSource.getConnection();
        break;
      } catch (SQLException e) {
        connectionRetries++;
        logger.warn("Retrying connection for dataSource " + key + " ExceptionCause: " + e.getCause(), e);
        try {
          Thread.sleep(1000 * connectionRetries);
        } catch (InterruptedException e1) {
          logger.error("Error while sleeping ", e);
        }
        
      }
    }
    // if con==null, there is a connection error
    if (con == null) {
      logger.error("Could not getConnection for dataSource '" + key + "'");
    }
    
    return con;
  }
  
  /**
   * only for test purpose
   * 
   * @param key
   * @return
   * @throws SQLException
   */
  protected DataSource getDataSource(String key) throws SQLException {
    return dataSourcesMap.get(key);
  }
  
  private void setupDataSource(String keyDataSource, String host, int port, String database, String user, String password) {
    String urlConnection = createConnectionUrl(host, port, database);
    DataSource dataSource = createDataSource(urlConnection, user, password);
    dataSourcesMap.put(keyDataSource, dataSource);
  }
  
  private String createConnectionUrl(String host, int port, String database) {
    String url = "jdbc:mysql://" + host + ":" + port + "/" + database;
    return url;
  }
  
  private DataSource createDataSource(String url, String user, String password) {
    BasicDataSource ds = new BasicDataSource();
    ds.setDriverClassName(DEFAULT_DRIVER);
    ds.setUsername(user);
    ds.setPassword(password);
    ds.setUrl(url);
    ds.setTestOnBorrow(true);
    ds.setInitialSize(DEFAULT_INIT_SIZE_POOL);
    ds.setValidationQuery("SELECT 1");
    // 30 second wait a connection
    ds.setMaxWait(30 * 1000);
    return ds;
  }
  
  private void shutdownDataSource(DataSource ds) throws SQLException {
    BasicDataSource bds = (BasicDataSource) ds;
    bds.close();
  }
  
}
