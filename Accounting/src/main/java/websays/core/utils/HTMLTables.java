package websays.core.utils;

import java.util.Collection;

public class HTMLTables {
  
  String patt_bot = "<table>\n";
  String patt_eot = "</table>\n";
  String patt_bohf = "<th>";
  String patt_eohf = "</th>";
  String patt_bof = "<td>";
  String patt_eof = "</td>";
  String patt_bol = "<tr>";
  String patt_eol = "</tr>\n";
  
  public void startTable(StringBuffer sb) {
    sb.append(patt_bot);
  }
  
  public void endTable(StringBuffer sb) {
    sb.append(patt_eot);
  }
  
  public void addRow(StringBuffer sb, Collection<String> value) {
    addRow(sb, value, false);
  }
  
  public void addRow(StringBuffer sb, Collection<String> value, boolean header) {
    sb.append(patt_bol);
    for (Object s : value) {
      sb.append(header ? patt_bohf : patt_bof);
      sb.append(s.toString());
      sb.append(header ? patt_eof : patt_eohf);
    }
    sb.append(patt_eol);
  }
}
