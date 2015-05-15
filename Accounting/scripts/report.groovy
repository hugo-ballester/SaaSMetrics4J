// Generate simple email report about contracts ending soon
// Hugo Zaragoza, Websays, 2015


// HEADER
if (args.size()>2 || args.size()<1) {
  println "ARGUMENTS:\n\t<WEBSAYS_HOME> [<email>]\n\t email format: 'first@mail.com;second@othermail.com'"
  return;
}
Globals g = new Globals(args[0]);

toAddress="";
if (args.size()>1) {
  toAddress=args[1];
}
import groovy.sql.*
import javax.mail.*
import javax.mail.internet.*
@GrabConfig( systemClassLoader=true )
@Grab( 'mysql:mysql-connector-java:5.1.27' )
@Grab( group = 'javax.mail', module = 'mail', version = '1.4.7' )
@Grab( group = 'javax.activation', module = 'activation', version = '1.1.1' )

commands = [];


// -----------------------
// DEFINE COMMANDS TO RUN:
// -----------------------

def cols1 = """
c.sales_person AS SP, c.id, c.name as contract_name, cl.name AS client_name,start,
end AS end_C,
DATEDIFF( dataAccessEnd, end)  AS end_A,
 c.type""";

emailTitle = "ACCOUNTING: Contracts Ending or Renewing Soon";

commands << "--- ENDING SOON"

commands << "Contracts ending in the next 30 days:";
commands << """SELECT $cols1, DATEDIFF(c.end,CURRENT_DATE()) as days_remaining
FROM contract c LEFT JOIN client cl ON c.client_id=cl.id 
WHERE c.end>=CURRENT_DATE() AND DATEDIFF(c.end, CURRENT_DATE())<=30
ORDER BY days_remaining ASC;
"""

commands << "Contracts auto-renewing in the next 30 days:";
commands << """
SELECT $cols1, contractedMonths, DATEDIFF( DATE_ADD(c.start,INTERVAL c.contractedMonths MONTH) , CURRENT_DATE()) as days_remaining
FROM contract c LEFT JOIN client cl ON c.client_id=cl.id 
WHERE 
 DATEDIFF( DATE_ADD(c.start,INTERVAL c.contractedMonths MONTH) , CURRENT_DATE())<=30 
 AND end IS NULL
 AND (c.type != 'internal' AND c.type != 'pilot') 
ORDER BY c.type DESC, client_name;"""

commands << "--- ENDED"

commands << "Contracts that ended in the last 30 days (BUT ARE NOT CONFIRMED!?):"
commands << """SELECT $cols1, c.confirmedClosed, DATEDIFF(CURRENT_DATE(),c.end) as days_since_end
FROM contract c  LEFT JOIN client cl ON c.client_id=cl.id 
WHERE DATEDIFF(NOW(),c.end)<30 AND c.end<=CURRENT_DATE() AND c.confirmedClosed IS NULL
ORDER BY c.end, client_name;"""

commands << "Contracts that ended in the last 30 days (confirmed):"
commands << """SELECT $cols1, c.confirmedClosed, DATEDIFF(CURRENT_DATE(),c.end) as days_since_end
FROM contract c  LEFT JOIN client cl ON c.client_id=cl.id 
WHERE DATEDIFF(NOW(),c.end)<30 AND c.end<=CURRENT_DATE() AND c.confirmedClosed IS NOT NULL
ORDER BY c.end, client_name;"""

commands << "--- PILOTS"

commands << "Active Pilots:";
commands << """
SELECT  $cols1, pilot_length AS pilot_length, DATEDIFF( DATE_ADD(c.start,INTERVAL c.pilot_length DAY) , CURRENT_DATE()) days_remaining, COUNT(c.id) AS '#profiles', GROUP_CONCAT(profile_id, ':', p.name) AS profiles 
  FROM profiles p LEFT JOIN contract c ON p.contract_id=c.id LEFT JOIN client cl ON c.client_id=cl.id
  WHERE (c.type='internal' OR c.type='pilot')
    AND ( c.confirmedClosed IS NULL )
  GROUP BY c.id
  ORDER BY days_remaining, c.sales_person, c.name;
"""


commands << "---- DB ADMIN WARNINGS:"

commands << "PROBLEMS: Contract main_profile lists a different contract_id:"
commands <<"""
SELECT c.name AS Contract, c.id, c.main_profile_id AS MainProfile, p.contract_id AS ContractIDofProfile FROM contract c LEFT JOIN  profiles p ON c.main_profile_id=profile_id
WHERE  c.id != p.contract_id
"""

commands << "PROBLEMS: Contracts missing end confirmation:"
commands << "SELECT  c.id,c.name, c.start, c.end, c.dataAccessEnd FROM contract c WHERE  c.end<CURRENT_DATE() AND c.confirmedClosed IS NULL AND (c.dataAccessEnd IS NULL OR c.dataAccessEnd<CURRENT_DATE() )"

commands << "Profiles that are ACTIVE but DO NOT HAVE a contract:"
commands << """
SELECT p.name AS "Profile_Name", c.name AS "Client_Name", DATE(p.created) AS created, deleted, schedule FROM profiles p LEFT JOIN contract c ON p.contract_id=c.id
WHERE
  (deleted =0) AND (`schedule` != 'Frozen') 
  AND ( NOT EXISTS( SELECT * FROM contract c WHERE c.confirmedClosed IS NULL AND p.contract_id=c.id) )
  order by c.name, p.created DESC
"""

commands << "--- ALL ACTIVE CONTRACTS"

commands << "Active Contracts"
commands << """
SELECT  c.id, c.name, cl.name AS client_name, start, c.type FROM contract c  LEFT JOIN client cl\
 ON c.client_id=cl.id WHERE c.start < NOW() AND ( ( c.end IS NULL ) OR (c.end > NOW()) )
 ORDER BY c.type DESC, client_name;
"""



// -----------------------
// CODE
// -----------------------
Properties p = g.properties;

uri = "jdbc:mysql://${p.host}:${p.port}/${p.db}"
def sql = Sql.newInstance(uri, p.user, g.properties.pass, "com.mysql.jdbc.Driver")
msg="";
none = "";
def i =0;
while (i<commands.size()) {
      if (commands[i].startsWith("---")) {
      msg += "\n\n\n<h2>"+commands[i].substring(3)+"</h2>\n\n";
      i++;
} else {
      table = showCommand(commands[i],commands[i+1], sql);
      if (table!=null) {
      msg += showCommand(commands[i],commands[i+1], sql);
      } else {
      none +="<li>${commands[i]}</li>\n";
      }
      i+=2      
};
}

msg = "<html><body>\n${msg}\n<hr/><h3>Queries with no results:</h3>\n<ul>\n${none}\n</ul>";

if (toAddress!="") {
 simpleMail(toAddress,emailTitle,msg,p);
} else {
 println msg;
}


// FUNTCIONS:

String showCommand(title, command, Sql sql) {
     style1 = "style=\"padding: .5rem; border: 1px solid black; \"";
     style2 = "style=\"padding: .5rem; border: 1px solid black; font-size: 85%; background-color: lightgray; \"";
     tableStart = "<table style=\"border-collapse:collapse;\">";
     trStart = "\n\n<tr $style1>";
     tdStart = "<td $style1>";
     thStart = "<th $style2>";
     // td = "<td style=\"\">";
     first = true;  
     String header = "";
     String table = "";
//     println "COMMAND: ${command}\n----\n";
     
     sql.rows(command.toString()).each {  Map row ->

       if (first) {
         first=false;
         header = trStart+thStart+(row.keySet().join("</th>"+thStart))+"</th>\n</tr>\n";         
       }
       table += trStart+tdStart+(row.values().join("</td>"+tdStart))+"</td></tr>\n";
     }   
     if (first) {
       return null;
     } else {
       table = "\n<h4>${title}</h4>\n$tableStart\n" + header  + table+"\n</table>\n\n";
     }
     return table;
}

class Globals {

  Properties properties = new Properties()

  public Globals(String websaysHome) {
    String path = websaysHome+"/conf/accounting_stage.properties";
    File propertiesFile = new File(path)
    propertiesFile.withInputStream {
      properties.load(it)
    }

  }
}


public static void simpleMail2(String to,
    String subject, String body, Properties p) throws Exception {
    println to;
    System.exit(1);
}


public static void simpleMail(String to,
    String subject, String body, Properties p) throws Exception {
 
    Properties props = System.getProperties();
    props.put("mail.smtp.starttls.enable",true);
    props.setProperty("mail.smtp.ssl.trust", p.SMTP_HOST);
    props.put("mail.smtp.auth", true);      
    props.put("mail.host",p.SMTP_HOST);
    props.put("mail.smtp.port",p.SMTP_PORT);

    props.put("mail.smtp.user", p.SMTP_USER);
    props.put("mail.smtp.password", p.SMTP_PASSWORD);


    Session session = Session.getDefaultInstance(props, null);
    MimeMessage message = new MimeMessage(session);
    message.setFrom(new InternetAddress(p.SMTP_USER));
    message.setContent(body, 'text/html');
    message.setSubject(subject);    
    for(email in to.split(";")) {
       InternetAddress toAddress = new InternetAddress(email); 
       message.addRecipient(Message.RecipientType.TO, toAddress);
    }
 
    Transport transport = session.getTransport("smtp");
    transport.connect(p.SMTP_HOST, p.SMTP_USER, p.SMTP_PASSWORD);
    transport.sendMessage(message, message.getAllRecipients());
    transport.close();
}
