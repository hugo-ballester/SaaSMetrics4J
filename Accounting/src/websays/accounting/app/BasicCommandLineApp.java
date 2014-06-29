/*
 *    SaaSMetrics4J : https://github.com/hugozaragoza/SaaSMetrics4J
 *
 *    (c) 2014, Hugo Zaragoza, Websays.
 */
package websays.accounting.app;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.Properties;

import org.apache.log4j.Appender;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.SimpleLayout;

import websays.accounting.Contract;
import websays.accounting.Contracts;
import websays.accounting.Metrics;
import websays.accounting.connectors.ContractDAO;
import websays.accounting.connectors.DatabaseManager;

import com.martiansoftware.jsap.FlaggedOption;
import com.martiansoftware.jsap.JSAP;
import com.martiansoftware.jsap.JSAPException;
import com.martiansoftware.jsap.JSAPResult;
import com.martiansoftware.jsap.Switch;

public class BasicCommandLineApp {
  
  public static boolean connectToDB = true;
  public static boolean debug = false;
  
  private static final Logger logger = Logger.getLogger(BasicCommandLineApp.class);
  public static BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
  public static final SimpleDateFormat df = new SimpleDateFormat("dd/MM/yy");
  protected Contracts contracts;
  
  {
    Logger.getLogger(Contract.class).setLevel(Level.INFO);
    Logger.getLogger(Metrics.class).setLevel(Level.INFO);
    
  }
  
  public static Properties props = new Properties();
  public static String pricingFile;
  public static String reportingHTMLDir;
  public static String reportingHTMLDirRemote;
  public static String dumpDataFile;
  public static String reportingTxtFile;
  
  public static int fixYear = 0, fixMonth = 0;
  protected static Integer contractID = null;
  
  public static void init(String[] args) throws JSAPException {
    // init log4j
    Logger r = Logger.getRootLogger();
    Appender myAppender;
    r.setLevel(Level.INFO);
    myAppender = new ConsoleAppender(new SimpleLayout());
    r.addAppender(myAppender);
    
    // argument parser:
    JSAP jsap = new JSAP();
    
    jsap.registerParameter(new FlaggedOption("params").setStringParser(JSAP.STRING_PARSER).setRequired(true).setShortFlag('p'));
    
    jsap.registerParameter(new FlaggedOption("year").setStringParser(JSAP.INTEGER_PARSER).setRequired(false).setShortFlag('y'));
    
    jsap.registerParameter(new FlaggedOption("month").setStringParser(JSAP.INTEGER_PARSER).setRequired(false).setShortFlag('m'));
    
    jsap.registerParameter(new FlaggedOption("contract").setLongFlag("contract").setShortFlag('c').setStringParser(JSAP.INTEGER_PARSER)
        .setRequired(false));
    
    jsap.registerParameter(new Switch("offline").setLongFlag("offline").setDefault("false"));
    
    jsap.registerParameter(new Switch("debug").setLongFlag("debug").setDefault("false"));
    
    JSAPResult config = jsap.parse(args);
    if (!config.success()) {
      for (@SuppressWarnings("rawtypes")
      java.util.Iterator errs = config.getErrorMessageIterator(); errs.hasNext();) {
        System.err.println("Error: " + errs.next());
      }
      System.out.println("Usage:\n\n\t" + jsap.getUsage() + "\n\n");
      
      System.exit(1);
    }
    
    File propFile = new File(config.getString("params"));
    if (!propFile.exists()) {
      System.out.println("Cannot find property file: " + args[0]);
      System.exit(0);
    }
    
    if (config.contains("year")) {
      fixYear = config.getInt("year");
      fixMonth = config.getInt("month");
    }
    
    if (config.contains("contract")) {
      contractID = config.getInt("contract");
    }
    connectToDB = !config.getBoolean("offline");
    debug = config.getBoolean("offline");
    
    FileInputStream in;
    try {
      in = new FileInputStream(propFile);
      props.load(in);
      in.close();
    } catch (Exception e) {
      logger.error(e);
      System.exit(0);
    }
    
    pricingFile = props.getProperty("pricingFile", null);
    dumpDataFile = props.getProperty("dumpDBFile", null);
    reportingHTMLDir = props.getProperty("reportingHTMLDir", null);
    reportingHTMLDirRemote = props.getProperty("reportingHTMLDirRemote", null);
    reportingTxtFile = props.getProperty("reportingTxtFile", null);
    
    if (connectToDB) {
      DatabaseManager.initDatabaseManager(props.getProperty("host"), Integer.parseInt(props.getProperty("port")),
          props.getProperty("user"), props.getProperty("pass"), props.getProperty("db"), true);
    } else {
      System.out.println("WARNING: not connecting to DB");
    }
    
    if (reportingHTMLDir != null && !(new File(reportingHTMLDir).exists())) {
      (new File(reportingHTMLDir)).mkdir();
    }
    
  }
  
  public static String ask(String string) throws IOException {
    System.out.print(string);
    return br.readLine();
  }
  
  public void setOutput(File newFile) throws FileNotFoundException {
    PrintStream out = new PrintStream(new FileOutputStream(newFile));
    System.setOut(out);
  }
  
  public void initContracts() throws Exception {
    
    contracts = ContractDAO.loadAccounts(connectToDB, dumpDataFile != null ? new File(dumpDataFile) : null, pricingFile != null ? new File(
        pricingFile) : null);
  }
  
}