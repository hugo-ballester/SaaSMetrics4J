/* *    SaaSMetrics4J : https://github.com/hugozaragoza/SaaSMetrics4J
 *
 *    (c) 2014, Hugo Zaragoza, Websays.
 */
package websays.accounting.app;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.lang.reflect.Field;
import java.text.SimpleDateFormat;
import java.util.Properties;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

import websays.accounting.Contracts;
import websays.accounting.GlobalConstants;
import websays.accounting.connectors.ContractDAO;
import websays.accounting.connectors.DatabaseManager;
import websays.utils.mail.MailConfiguration;
import websays.utils.mail.SendMailUsingAuthentication;

import com.martiansoftware.jsap.FlaggedOption;
import com.martiansoftware.jsap.JSAP;
import com.martiansoftware.jsap.JSAPResult;
import com.martiansoftware.jsap.Switch;

public class BasicCommandLineApp {
  
  public static boolean connectToDB = true;
  
  private static final Logger logger = Logger.getLogger(BasicCommandLineApp.class);
  public static BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
  public static final SimpleDateFormat df = new SimpleDateFormat("dd/MM/yy");
  
  private static final int MIN_CONTRACT_LENGTH = 120;
  
  public static Properties props = new Properties();
  public static String pricingFile;
  public static String reportingHTMLDir;
  public static String reportingHTMLDirRemote;
  public static String dumpDataFile;
  public static String reportingTxtFile;
  public static int commission_months;
  public static MailConfiguration mc;
  
  public static int fixYear = 0, fixMonth = 0;
  protected static Integer contractID = null;
  public static String[] email = null;
  public static String[] actions = null;
  
  String line2 = "\n=================================================\n";
  String line1 = "\n-------------------------------------------------\n";
  
  public static void init(String[] args) throws Exception {
    // argument parser:
    JSAP jsap = new JSAP();
    
    jsap.registerParameter(new FlaggedOption("email").setStringParser(JSAP.STRING_PARSER).setRequired(false).setLongFlag("email"));
    
    jsap.registerParameter(new FlaggedOption("params").setStringParser(JSAP.STRING_PARSER).setRequired(true).setShortFlag('p'));
    
    jsap.registerParameter(new FlaggedOption("year").setStringParser(JSAP.INTEGER_PARSER).setRequired(false).setShortFlag('y'));
    
    jsap.registerParameter(new FlaggedOption("month").setStringParser(JSAP.INTEGER_PARSER).setRequired(false).setShortFlag('m'));
    
    jsap.registerParameter(new FlaggedOption("contract").setLongFlag("contract").setShortFlag('c').setStringParser(JSAP.INTEGER_PARSER)
        .setRequired(false));
    
    jsap.registerParameter(new FlaggedOption("do").setStringParser(JSAP.STRING_PARSER).setRequired(false).setLongFlag("do"));
    
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
      String msg = "Cannot find property file: " + config.getString("params");
      System.out.println(msg);
      throw new Exception(msg);
    } else {}
    
    if (config.contains("year")) {
      fixYear = config.getInt("year");
      fixMonth = config.getInt("month");
    }
    
    if (config.contains("email")) {
      email = config.getString("email").split("[, ;]");
    }
    
    if (config.contains("do")) {
      actions = StringUtils.split(config.getString("do"));
    }
    
    if (config.contains("contract")) {
      contractID = config.getInt("contract");
    }
    connectToDB = !config.getBoolean("offline");
    
    FileInputStream in;
    try {
      in = new FileInputStream(propFile);
      props.load(in);
      in.close();
    } catch (Exception e) {
      logger.error(e);
      System.exit(0);
    }
    
    GlobalConstants.load(props);
    
    pricingFile = props.getProperty("pricingFile", null);
    dumpDataFile = props.getProperty("dumpDBFile", null);
    reportingHTMLDir = props.getProperty("reportingHTMLDir", null);
    reportingHTMLDirRemote = props.getProperty("reportingHTMLDirRemote", null);
    reportingTxtFile = props.getProperty("reportingTxtFile", null);
    
    mc = null;
    if (props.containsKey("SMTP_HOST")) {
      mc = new MailConfiguration();
      mc.setSmtpHostName(props.getProperty("SMTP_HOST", null));
      mc.setSmtpUser(props.getProperty("SMTP_USER", null));
      mc.setSmtpPassword(props.getProperty("SMTP_PASSWORD", null));
      mc.setSmtpPort(Integer.parseInt(props.getProperty("SMTP_PORT", null)));
      mc.setUseSSL(Boolean.parseBoolean(props.getProperty("SMTP_SSL", null)));
      mc.setUseTSL(Boolean.parseBoolean(props.getProperty("SMTP_TSL", null)));
    }
    
    if (connectToDB) {
      DatabaseManager.initDatabaseManager(props.getProperty("host"), Integer.parseInt(props.getProperty("port")),
          props.getProperty("user"), props.getProperty("pass"), props.getProperty("db"), true);
    } else {
      System.out.println("WARNING: not connecting to DB");
    }
    
    if (reportingHTMLDir != null && !(new File(reportingHTMLDir).exists())) {
      (new File(reportingHTMLDir)).mkdir();
    }
    
    String[] showParams = new String[] {"pricingFile", "dumpDataFile", "reportingHTMLDir", "reportingHTMLDirRemote", "reportingTxtFile"};
    logger.info("Loaded Parameters from: " + propFile.getAbsolutePath());
    logger.info("  Started with the following params:\n" + showFields(showParams));
    
  }
  
  public static String ask(String string) throws IOException {
    System.out.print(string);
    return br.readLine();
  }
  
  public void setOutput(File newFile) throws FileNotFoundException {
    if (newFile == null) {
      System.setOut(new PrintStream(new FileOutputStream(FileDescriptor.out)));
    } else {
      PrintStream out = new PrintStream(new FileOutputStream(newFile));
      System.setOut(out);
    }
  }
  
  public Contracts initContracts() throws Exception {
    
    Contracts con = ContractDAO.loadAccounts(connectToDB, dumpDataFile != null ? new File(dumpDataFile) : null,
        pricingFile != null ? new File(pricingFile) : null);
    con.normalizeContracts(MIN_CONTRACT_LENGTH);
    return con;
    
  }
  
  public static String showFields(String[] names) throws Exception {
    String ret = "";
    for (String s : names) {
      ret += s + ":\t" + getField(s) + "\n";
    }
    return ret;
  }
  
  public static String getField(String name) throws Exception {
    Field f = BasicCommandLineApp.class.getField(name);
    return f.get(BasicCommandLineApp.class).toString();
  }
  
  public static boolean email(String title, String content) {
    if (email == null) {
      logger.error("Cannot email without email addresses");
      return false;
    }
    if (mc == null) {
      logger
          .error("You need to specify email server parameters in config file: SMTP_HOST, SMTP_USER, SMTP_PASSWORD, SMTP_PORT, SMTP_SSL, SMTP_TSL");
      return false;
    }
    
    SendMailUsingAuthentication sender = new SendMailUsingAuthentication(mc);
    try {
      logger.debug(mc.toString());
      sender.postMail(email, title, "<html><body><pre>" + content + "</pre></body></html>", mc.getSmtpUser(), new File[] {},
          "text/html; charset=utf-8", "no-reply@websays.info");
    } catch (Exception e) {
      logger.error("Could not send email:", e);
      return false;
    }
    return true;
  }
  
  public static void main(String[] args) throws Exception {
    new MyMiniReports(args);
  }
  
}