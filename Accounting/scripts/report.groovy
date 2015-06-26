// Generate simple email report about contracts ending soon
// Hugo Zaragoza, Websays, 2015
//
//

// ARGUMENTS
if ( args.size() <1 ||  args.size() >2 ) {
  errorArgs();
  return;
}

home = System.getenv("WEBSAYS_HOME")
if (home==null) {
  println "YOU NEED TO DEFINE ENVIRONMENT VARIABLE WEBSAYS_HOME"
  return;
} else {
  homeF = new File( home )
  if (!homeF.exists()) {
    println "COULD NOT FIND WEBSAYS_HOME = "+home
    return;  
  }
}

toAddress="";
reportType=args[0];
if (args.size()>1) {
  toAddress=args[1];
}


// IMPORTS
import groovy.sql.*
import javax.mail.*
import javax.mail.internet.*
@GrabConfig( systemClassLoader=true )
@Grab( 'mysql:mysql-connector-java:5.1.27' )
@Grab( group = 'javax.mail', module = 'mail', version = '1.4.7' )
@Grab( group = 'javax.activation', module = 'activation', version = '1.1.1' )

// -----------------------
// DEFINE COMMANDS TO RUN:
// -----------------------
Globals g = new Globals(home);
commands = [];


def cols1 = """
c.type, c.sales_person AS SP, c.id, c.name as contract_name, cl.name AS client_name,start,
end AS end_C,
DATEDIFF( dataAccessEnd, end)  AS extra_days,
dataAccessEnd AS end_A
""";
 
def FROM1 = """
FROM contract c LEFT JOIN client cl ON c.client_id=cl.id 
"""

if (reportType=="client_notifications") {
  
  emailTitle = "ACCOUNTING REPORT: NOTIFICATIONS TO CLIENTS";
  
  commands << "Pilots ending in less than 5 days"
  commands << """
SELECT $cols1, DATEDIFF( DATE_ADD(c.pilot_start,INTERVAL c.pilot_length DAY) ,CURRENT_DATE()) as days_remaining
    FROM profiles p
    LEFT JOIN contract c ON p.contract_id=c.id
    LEFT JOIN client cl ON c.client_id=cl.id
  WHERE
    p.deleted=0
    AND ( c.type='pilot' )
    AND ( c.confirmedClosed IS NULL )
  GROUP BY c.id
  HAVING days_remaining >=0 AND days_remaining < 5
  ORDER BY days_remaining, c.sales_person, c.name;
"""

commands << "Pilots in the middle"
commands << """
SELECT $cols1
 ,c.pilot_length as days_total
 ,ROUND(c.pilot_length/2) as days_mid
 ,DATEDIFF( CURRENT_DATE(), c.pilot_start) as days_sofar
 ,DATEDIFF( DATE_ADD(c.pilot_start,INTERVAL c.pilot_length DAY) ,CURRENT_DATE()) as days_remaining
 ,  100.0 * DATEDIFF( CURRENT_DATE(), c.pilot_start) / c.pilot_length as frac
    FROM profiles p
    LEFT JOIN contract c ON p.contract_id=c.id
    LEFT JOIN client cl ON c.client_id=cl.id
  WHERE
    p.deleted=0
    AND ( c.type='pilot' )
    AND ( c.confirmedClosed IS NULL )
  GROUP BY c.id
  HAVING ABS(days_mid-days_sofar)<2
  ORDER BY days_remaining, c.sales_person, c.name;
"""

commands << "Pilots ended already (but not confirmed)"
commands << """
SELECT $cols1, DATEDIFF( DATE_ADD(c.pilot_start,INTERVAL c.pilot_length DAY) ,CURRENT_DATE()) as days_remaining
    FROM profiles p
    LEFT JOIN contract c ON p.contract_id=c.id
    LEFT JOIN client cl ON c.client_id=cl.id
  WHERE
    p.deleted=0
    AND ( c.type='pilot' )
    AND ( c.confirmedClosed IS NULL )
  GROUP BY c.id
  HAVING days_remaining <0
  ORDER BY days_remaining, c.sales_person, c.name;
"""

commands << "Profiles that are ACTIVE but DO NOT HAVE a contract:"
commands << """
SELECT u.name AS created_by,  p.profile_id, p.name AS "Profile_Name", DATE(p.created) AS created, p.status, p.schedule
   FROM profiles p
   LEFT JOIN contract c ON p.contract_id=c.id
   LEFT JOIN users u ON p.owner_id=u.id
 WHERE
  (deleted =0) AND p.`contract_id` IS NULL
ORDER BY  created_by, p.schedule, p.created DESC
"""


} else if (reportType=="urgent") {
  emailTitle = "URGENT ACCOUNTING REPORT: Actions needed";
  commands << "Contracts that ended in the last 30 days BUT NOT CONFIRMED!"
  commands << """
SELECT $cols1, DATEDIFF(CURRENT_DATE(),c.end) as days_since_end
$FROM1
WHERE DATEDIFF(NOW(),c.end)<30 AND c.end<=CURRENT_DATE() AND c.confirmedClosed IS NULL
ORDER BY c.end, client_name;
"""

commands << "Contracts with 0 MRR!"
commands << """
SELECT $cols1 
$FROM1
WHERE free=0 AND pricing IS NULL AND ( (mrr+IFNULL(fixed,0))=0 ) AND (c.type='subscription' OR c.type='project')
"""


} else if (reportType=="periodic") {
  emailTitle = "PERIODIC ACCOUNTING REPORT: Contracts Ending or Renewing Soon";

commands << "--- ENDING SOON"


commands << "Contracts ending in the next 30 days:";
commands << """SELECT $cols1, DATEDIFF(c.end,CURRENT_DATE()) as remaining_days
$FROM1
WHERE c.end>=CURRENT_DATE() AND DATEDIFF(c.end, CURRENT_DATE())<=30
ORDER BY remaining_days ASC;
"""

commands << "Contracts auto-renewing in the next 30 days:";
commands << """
SELECT $cols1, contractedMonths, DATEDIFF( DATE_ADD(c.start,INTERVAL c.contractedMonths MONTH) , CURRENT_DATE()) as days_remaining
$FROM1
WHERE 
 DATEDIFF( DATE_ADD(c.start,INTERVAL c.contractedMonths MONTH) , CURRENT_DATE())<=30 
 AND end IS NULL
 AND (c.type != 'internal' AND c.type != 'pilot') 
ORDER BY c.type DESC, client_name;"""


commands << "Active Pilots:";
commands << """
SELECT  $cols1, pilot_length AS pilot_length, DATEDIFF( DATE_ADD(c.pilot_start,INTERVAL c.pilot_length DAY) , CURRENT_DATE()) days_remaining, COUNT(c.id) AS '#profiles', GROUP_CONCAT(profile_id, ':', p.name) AS profiles 
  FROM profiles p 
    LEFT JOIN contract c ON p.contract_id=c.id 
    LEFT JOIN client cl ON c.client_id=cl.id
  WHERE 
    p.deleted=0
    AND ( c.type='internal' OR c.type='pilot' )
    AND ( c.confirmedClosed IS NULL )
  GROUP BY c.id
  ORDER BY days_remaining, c.sales_person, c.name;
"""

commands << "---- DB ADMIN WARNINGS:"

commands << "PROBLEMS: Contract main_profile lists a different contract_id:"
commands <<"""
SELECT c.name AS Contract, c.id, c.main_profile_id AS MainProfile, p.contract_id AS ContractIDofProfile 
  FROM contract c LEFT JOIN  profiles p ON c.main_profile_id=profile_id
  WHERE  c.id != p.contract_id
"""

commands << "Profiles that are ACTIVE but DO NOT HAVE a contract:"
commands << """
SELECT u.name AS created_by,  p.profile_id, p.name AS "Profile_Name", DATE(p.created) AS created, p.status, p.schedule
   FROM profiles p
   LEFT JOIN contract c ON p.contract_id=c.id
   LEFT JOIN users u ON p.owner_id=u.id
 WHERE
  (deleted =0) AND p.`contract_id` IS NULL
ORDER BY  created_by, p.schedule, p.created DESC
"""

commands << "--- ALL ACTIVE CONTRACTS"

commands << "Active Contracts"
commands << """
SELECT  $cols1 
$FROM1 
WHERE c.start < NOW() AND ( ( c.end IS NULL ) OR (c.end > NOW()) )
 ORDER BY c.type DESC, client_name;
"""

} else {

  println "ERROR: unknown report type '$reportType'"
  errorArgs();
  return;
}


// -----------------------
// CODE
// -----------------------
something=false;
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
      msg += "\n<h4>${commands[i]}</h4>\n";
      table = showCommand(commands[i],commands[i+1], sql);
      if (table!=null) {
        msg += table;
        something=true;
      } else {
        none +="<li>${commands[i]}</li>\n";
        msg += "<p>none</p>\n";
      }
      i+=2      
};
}

def today = new Date()

msg = "<html><body>\n<p>Report generated on: $today.</p>\n${msg}\n"

if (none!="") {
  msg += "<hr/><h3>Queries with no results:</h3>\n<ul>\n${none}\n</ul>";
}

if (something && toAddress!="") {
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
     rows=0;
//     println "COMMAND: ${command}\n----\n";
     
     sql.rows(command.toString()).each {  Map row ->
       if (rows==0) { // build header
         header = trStart+thStart+(row.keySet().join("</th>"+thStart))+"</th>\n</tr>\n"
       }
       if (rows%20==0) { // show header at intervals
         table += header
       }
       first=false;
       line = trStart+tdStart+(row.values().join("</td>"+tdStart))+"</td></tr>\n"
       line = line.replaceAll(">null<",">-<")
       table += line
       rows++;
       
     }   
     if (first) {
       table=null;
     } else {
       table = "$tableStart\n" +  table + "\n</table>\n\n";
     }
     return table;
}

class Globals {

  Properties properties = new Properties()

  public Globals(String websaysHome) {
    String path = websaysHome+"/conf/accounting_dev1.properties";
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
    for(email in to.split("[;,]")) {
       InternetAddress toAddress = new InternetAddress(email); 
       message.addRecipient(Message.RecipientType.TO, toAddress);
    }
 
    Transport transport = session.getTransport("smtp");
    transport.connect(p.SMTP_HOST, p.SMTP_USER, p.SMTP_PASSWORD);
    transport.sendMessage(message, message.getAllRecipients());
    transport.close();
}

    
public static void errorArgs(){
  println "ARGUMENTS:\n\treportType{client_notifications,urgent,periodic} [<email>]\n\t email format: 'first@mail.com,second@othermail.com'"  
}
    
    
