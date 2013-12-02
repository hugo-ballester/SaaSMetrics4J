/*
 *    SAS4J
 *
 *    Hugo Zaragoza, Websays.
 */
package websays.accounting.app;

import java.io.File;
import java.io.FileInputStream;
import java.util.Properties;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import websays.accounting.Contract;
import websays.accounting.Metrics;
import websays.accounting.connectors.DatabaseManager;

public class BasicCommandLineApp {
  
  private static final Logger logger = Logger.getLogger(BasicCommandLineApp.class);
  
  {
    Logger.getLogger(Contract.class).setLevel(Level.INFO);
    Logger.getLogger(Metrics.class).setLevel(Level.INFO);
    
  }
  public static boolean connectToDB;
  public static Properties props = new Properties();
  public static String pricingFile;
  public static String dumpDataFile;
  public static String dumpMetrics;
  
  public static void init(String[] args) {
    
    if (args.length < 1 || args.length > 2) {
      System.out.println("ARGUMENTS: file.properties [false (to run without DB connection)]");
      System.exit(0);
    }
    
    File propFile = new File(args[0]);
    if (!propFile.exists()) {
      System.out.println("Cannot find property file: " + args[0]);
      System.exit(0);
    }
    
    FileInputStream in;
    try {
      in = new FileInputStream(propFile);
      props.load(in);
      in.close();
    } catch (Exception e) {
      logger.error(e);
      System.exit(0);
    }
    
    connectToDB = !(args.length > 1 && args[1].equals("false"));
    
    pricingFile = props.getProperty("pricingFile", null);
    dumpDataFile = props.getProperty("dumpDBFile", null);
    dumpMetrics = props.getProperty("dumpMetricsFile", null);
    
    if (connectToDB) {
      DatabaseManager.initDatabaseManager(props.getProperty("host"), Integer.parseInt(props.getProperty("port")),
          props.getProperty("user"), props.getProperty("pass"), props.getProperty("db"), true);
    }
    
  }
}