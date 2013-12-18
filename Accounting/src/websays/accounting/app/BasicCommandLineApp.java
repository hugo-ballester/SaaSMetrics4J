/*
 *    SAS4J
 *
 *    Hugo Zaragoza, Websays.
 */
package websays.accounting.app;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.Properties;

import org.apache.log4j.Appender;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.SimpleLayout;

import websays.accounting.Contract;
import websays.accounting.Metrics;
import websays.accounting.Reporting;
import websays.accounting.connectors.DatabaseManager;

public class BasicCommandLineApp {
  
  public static boolean connectToDB = true;
  
  private static final Logger logger = Logger.getLogger(BasicCommandLineApp.class);
  public static BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
  public static final SimpleDateFormat df = new SimpleDateFormat("dd/MM/yy");
  
  {
    Logger.getLogger(Contract.class).setLevel(Level.INFO);
    Logger.getLogger(Metrics.class).setLevel(Level.INFO);
    
  }
  
  public static Properties props = new Properties();
  public static String pricingFile;
  public static String dumpDataFile;
  public static String dumpMetrics;
  
  public static void init(String[] args) {
    // init log4j
    Logger r = Logger.getRootLogger();
    Appender myAppender;
    r.setLevel(Level.INFO);
    myAppender = new ConsoleAppender(new SimpleLayout());
    r.addAppender(myAppender);
    
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
  
  public static String ask(String string) throws IOException {
    System.out.print(string);
    return br.readLine();
  }
  
  public static void title(String s) {
    Reporting.title(s, connectToDB);
  }
  
}