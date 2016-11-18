/*
 *    SAS4J
 *
 *    Hugo Zaragoza, Websays.
 */
package websays.accounting.app;

import java.io.File;
import java.io.FileInputStream;
import java.sql.Connection;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Properties;

import org.apache.log4j.Logger;

import com.martiansoftware.jsap.FlaggedOption;
import com.martiansoftware.jsap.JSAP;
import com.martiansoftware.jsap.JSAPResult;
import com.martiansoftware.jsap.Switch;

import websays.accounting.GlobalConstants;
import websays.accounting.connectors.DatabaseManager;
import websays.core.utils.HTMLTables;

public class AgencyBilling {

  private static final Logger logger = Logger.getLogger(AgencyBilling.class);

  public static void main(String[] args) throws Exception {

    new AgencyBilling(args);
  }

  public AgencyBilling(String[] args) throws Exception {
    init(args);
    System.out.println(agencyBillingReport(new Date()));
  }

  public static String agencyBillingReport(Date reportDate) throws SQLException {
    Connection con = null;
    try {
      con = DatabaseManager.getConnection();
      SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
      String date = "DATE('" + sdf.format(reportDate) + "')";
      String cmdA = "" + //
          "CREATE TEMPORARY TABLE active_contracts (" + //
          "  SELECT * FROM contract c WHERE " + //
          "    (c.start <= DATE_FORMAT(" + date + ", '%Y-%m-%d'))" + //
          // OLD LINE (added 1 month not sure why: " AND " + "(end IS NULL OR c.end >= DATE_FORMAT(DATE_ADD(" + date +
          // ",INTERVAL 1 MONTH) ,'%Y-%m-01') ) " + //
          "    AND (c.end IS NULL OR (c.end >= DATE_FORMAT(" + date + ", '%Y-%m-%d')))" + //
          ")";

      String cmdAgencies = "CREATE TEMPORARY TABLE agencies1 (SELECT DISTINCT(cl.name) FROM contract c JOIN client cl ON c.client_id=cl.id WHERE plan='agencies_1')";

      String cmdB = "" + "CREATE TEMPORARY TABLE active_agency (" + //
          "SELECT cl.name AS Client,c.name AS Contract,COUNT(*) AS profiles, 200+50*(COUNT(*)-1) AS MRR " + //
          "FROM profiles p  " + //
          "LEFT JOIN active_contracts c ON p.contract_id=c.id " + //
          "LEFT JOIN client cl ON c.client_id=cl.id " + //
          "WHERE  " + //
          "  p.deleted=0 " + //
          "  AND (c.type='subscription' OR c.type='project') " + //
          "  AND ( c.confirmedClosed IS NULL ) " + // WHY???
          "  AND (cl.name  IN (SELECT * FROM agencies1 )) " + //
          "  AND c.name NOT LIKE '%Presale%'  " + //
          "GROUP BY c.name " + //
          "ORDER BY cl.name, c.name, profiles DESC " + //
          ")";

      String cmd1 = "SELECT * FROM active_agency";

      String cmd2 = "" + //
          "SELECT mrr.Client, SUM(mrr.profiles) AS profiles, SUM(mrr.MRR) AS MRR FROM " + //
          "  active_agency " + //
          " AS mrr GROUP BY mrr.Client";

      // System.out.println("\n" + cmdA + "\n" + cmdB + "\n" + cmd1 + "\n" + cmd2 + "\n");
      execute("DROP TABLE IF EXISTS active_contracts", con);
      execute("DROP TABLE IF EXISTS active_agency", con);
      execute("DROP TABLE IF EXISTS agencies1", con);

      logger.debug("agencyBillingReport cmdA\n" + cmdA);
      logger.debug("agencyBillingReport cmdB\n" + cmdB);
      execute(cmdA, con);
      execute(cmdAgencies, con);
      execute(cmdB, con);
      String ret = "<h4>Total per Agency:</h4>" + printResults(cmd2, con) + "<h4>Detail of each agency:</h4>" + printResults(cmd1, con);
      return ret;
    } catch (SQLException e) {
      logger.error("MYSQL QUERY ERROR");
      throw (e);
    } finally {
      if (con != null) {
        con.close();
      }
    }
  }

  private static void execute(String cmd0, Connection con) throws SQLException {
    Statement stmt = con.createStatement();
    stmt.executeUpdate(cmd0);
  }

  public static String printResults(String cmd, Connection con) throws SQLException {
    StringBuffer sb = new StringBuffer();
    HTMLTables table = new HTMLTables();
    Statement stmt = con.createStatement();
    java.sql.ResultSet rs = stmt.executeQuery(cmd);
    ResultSetMetaData md = rs.getMetaData();
    int colCount = md.getColumnCount();

    table.startTable(sb);
    ArrayList<String> values = new ArrayList<String>();
    for (int i = 1; i <= colCount; i++) {
      values.add(md.getColumnName(i));
    }
    table.addRow(sb, values, true);

    while (rs.next()) {
      values.clear();
      for (int i = 1; i <= colCount; i++) {
        values.add(rs.getObject(i).toString());
      }
      table.addRow(sb, values);
    }
    table.endTable(sb);
    return sb.toString();
  }

  public static void init(String[] args) throws Exception {
    // argument parser:
    JSAP jsap = new JSAP();

    jsap.registerParameter(new FlaggedOption("email").setStringParser(JSAP.STRING_PARSER).setRequired(false).setLongFlag("email"));

    jsap.registerParameter(new FlaggedOption("params").setStringParser(JSAP.STRING_PARSER).setRequired(true).setShortFlag('p'));

    jsap.registerParameter(new FlaggedOption("year").setStringParser(JSAP.INTEGER_PARSER).setRequired(false).setShortFlag('y'));

    jsap.registerParameter(new FlaggedOption("month").setStringParser(JSAP.INTEGER_PARSER).setRequired(false).setShortFlag('m'));

    jsap.registerParameter(
        new FlaggedOption("contract").setLongFlag("contract").setShortFlag('c').setStringParser(JSAP.INTEGER_PARSER).setRequired(false));

    jsap.registerParameter(new Switch("offline").setLongFlag("offline").setDefault("false"));

    jsap.registerParameter(new Switch("debug").setLongFlag("debug").setDefault("false"));

    jsap.registerParameter(new FlaggedOption("do").setStringParser(JSAP.STRING_PARSER).setRequired(false).setLongFlag("do"));

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

    boolean connectToDB = !config.getBoolean("offline");

    FileInputStream in;
    Properties props = new Properties();
    ;
    try {
      in = new FileInputStream(propFile);
      props.load(in);
      in.close();
    } catch (Exception e) {
      logger.error(e);
      System.exit(0);
    }

    GlobalConstants.load(props);

    if (connectToDB) {
      DatabaseManager.initDatabaseManager(props.getProperty("host"), Integer.parseInt(props.getProperty("port")), props.getProperty("user"),
          props.getProperty("pass"), props.getProperty("db"), true);
    } else {
      System.out.println("WARNING: not connecting to DB");
    }

  }
}
