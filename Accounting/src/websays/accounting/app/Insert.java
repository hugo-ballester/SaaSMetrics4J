/*
 *    SAS4J
 *
 *    Hugo Zaragoza, Websays.
 */
package websays.accounting.app;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Date;
import java.util.List;

import profile.Profile;
import profile.ProfileDAO;
import websays.accounting.Contract;
import websays.accounting.Contract.Type;
import websays.accounting.Contracts;
import websays.accounting.Metrics;
import websays.accounting.connectors.ContractDAO;

public class Insert extends BasicCommandLineApp {
  
  public static void main(String[] args) throws Exception {
    init(args);
    
    ProfileDAO pdao = new ProfileDAO();
    ContractDAO cdao = new ContractDAO();
    List<Profile> lisP = pdao.getAllProfiles(true, null);
    Contracts lisC = cdao.getAccounts(null, false);
    BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
    Integer lastCID = null;
    boolean skip = false;
    while (true) {
      String str = ask(br, "\nSEARCH PROFILE: ");
      if (str.length() == 0) {
        continue;
      }
      skip = false;
      for (Profile p : lisP) {
        if (skip) {
          break;
        }
        if (p.getName() == null || !p.getName().toLowerCase().contains(str.toLowerCase())) {
          continue;
        }
        
        if (p.getContractId() != null) {
          System.out.println("  PROFFILE: " + p.getName() + " (contract: " + p.getContractId() + ")");
          continue;
        }
        
        while (true) {
          
          System.out.println("\n  PROFILE " + p.getAccountID() + ": " + p.getName() + "  (no contract assigned)");
          System.out
              .print("  Command (create contract (c), assign to last contract (l), assign (a contract_id)), serch contract (s text)?");
          String ans = br.readLine();
          if (ans.startsWith("s")) {
            ans = ans.substring(2);
            System.out.println("  | searching '" + ans + "'");
            for (Contract c : lisC) {
              if (c.name.toLowerCase().contains(ans.toLowerCase())) {
                System.out.println("  | " + c.getId() + " " + c.name);
              }
            }
            System.out.println("  | --");
          } else if (ans.equals("c")) {
            
            String name = ask(br, "Name");
            Integer client_id = Integer.parseInt(ask(br, "Client Id"));
            String type = "pilot";
            Date start = Metrics.df.parse("1/1/2010");
            Date end = Metrics.df.parse("1/1/2010");
            Contract c = new Contract(0, name, Type.valueOf(type), client_id, start, end, 0., 0.);
            c.main_profile_id = p.getAccountID();
            cdao.create(c);
            p.setContractId(c.getId());
            pdao.update(p);
            lastCID = c.getId();
            System.out.println("contract_id: " + lastCID);
            break;
          } else if (ans.equals("l")) {
            p.setContractId(lastCID);
            pdao.update(p);
            break;
          } else if (ans.startsWith("a")) {
            ans = ans.substring(2);
            Integer cid = Integer.parseInt(ans);
            lastCID = cid;
            p.setContractId(cid);
            pdao.update(p);
            break;
          } else if (ans.length() == 0) {
            break;
          } else if (ans.equals("skip")) {
            skip = true;
            break;
          } else {
            System.out.println("UNKOWN COMMAND '" + ans + "'");
          }
        }
      }
      
    }
    
  }
  
  private static String ask(BufferedReader br, String string) throws IOException {
    System.out.print(string);
    return br.readLine();
  }
}
