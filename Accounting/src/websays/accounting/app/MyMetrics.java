///*
// *    SAS4J
// *
// *    Hugo Zaragoza, Websays.
// */
//package websays.accounting.app;
//
//import java.io.File;
//import java.util.Date;
//
//import org.apache.log4j.Level;
//import org.apache.log4j.Logger;
//
//import websays.accounting.Contract;
//import websays.accounting.Contracts;
//import websays.accounting.Contracts.AccountFilter;
//import websays.accounting.Metrics;
//import websays.accounting.Reporting;
//import websays.accounting.connectors.ContractDAO;
//
//public class MyMetrics extends BasicCommandLineApp {
//  
//  {
//    Logger.getLogger(Contract.class).setLevel(Level.INFO);
//    Logger.getLogger(Metrics.class).setLevel(Level.INFO);
//    
//  }
//  
//  public static void main(String[] args) throws Exception {
//    init(args);
//    (new MyMetrics()).execute_String();
//  }
//  
//  public void execute_String() throws Exception {
//    
//    int year = 2013;
//    int month = 11;
//    
//    Contracts contracts = ContractDAO.loadAccounts(connectToDB, dumpDataFile != null ? new File(dumpDataFile) : null,
//        pricingFile != null ? new File(pricingFile) : null);
//    Reporting app = new Reporting(contracts);
//    Date date = df.parse("1/" + month + "/" + year);
//    
//    if (reportingTxtFile != null) {
//      setOutput(new File(reportingTxtFile));
//    }
//    app.title("METRICS (contracted, then projects, then total", connectToDB);
//    app.displayMetrics(2013, 1, 11, AccountFilter.contract);
//    app.displayMetrics(2013, 1, 11, AccountFilter.project);
//    app.displayMetrics(2013, 1, 11, AccountFilter.contractedORproject);
//    
//    app.title("", connectToDB);
//    app.title("Contract Changes Month By Month", connectToDB);
//    for (int i = 1; i <= 12; i++) {
//      date = Reporting.sdf.parse("01/" + i + "/2013");
//      app.title("MONTH: " + Reporting.sdf.format(date), connectToDB);
//      app.displayContracts(date, AccountFilter.starting, true);
//      app.displayContracts(date, AccountFilter.ending, true);
//      app.displayContracts(date, AccountFilter.changed, true);
//    }
//    
//    System.out.close(); // may be a file
//  }
//  
// }