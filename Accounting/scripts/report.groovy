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
msg="<html><body>";


// -----------------------
// DEFINE COMMANDS TO RUN:
// -----------------------

emailTitle = "ACCOUNTING: Contracts Ending or Renewing Soon";

commands << "--- ENDING, AUTORENEWING, ETC."

commands << "Contracts ending in the next 30 days:";
commands << '''SELECT c.id,c.name, cl.name AS client_name, start, end, c.type FROM contract c LEFT JOIN client \
cl ON c.client_id=cl.id WHERE c.end>=CURRENT_DATE() AND DATEDIFF(c.end, CURRENT_DATE())<=30
ORDER BY c.type DESC, client_name;'''


commands << "Contracts auto-renewing in the next 30 days:";
commands << '''
SELECT c.id,c.name, cl.name AS client_name, start, DATE_ADD(c.start,INTERVAL c.contractedMonths MONTH) AS renewing , c.type 
FROM contract c LEFT JOIN client cl ON c.client_id=cl.id 
WHERE 
 DATEDIFF( DATE_ADD(c.start,INTERVAL c.contractedMonths MONTH) , CURRENT_DATE())<=30 
 AND end IS NULL
 AND (c.type != 'internal' AND c.type != 'pilot') 
ORDER BY c.type DESC, client_name;'''


commands << "Contracts that Ended Recently"
commands << '''SELECT c.id, c.name, cl.name AS client_name, start, end, c.type FROM contract c  LEFT JOIN clien\
t cl ON c.client_id=cl.id WHERE DATEDIFF(NOW(),c.end)<7 AND c.end<=CURRENT_DATE()
ORDER BY c.type DESC, client_name;'''

commands << "--- PILOTS"

commands << "Active Pilots:";
commands << '''
SELECT  sales_person AS SP, c.id, c.name, c.pilot_length - DATEDIFF(CURRENT_DATE(), c.start) AS daysReamining, COUNT(c.id) AS '#profiles'
  FROM profiles p LEFT JOIN contract c ON p.contract_id=c.id
  WHERE (c.type='internal' OR c.type='pilot')
    AND ( c.confirmedClosed IS NULL )
  GROUP BY c.id ORDER BY sales_person, daysReamining, c.name;
'''

commands << "--- CONTRACTS IN GENERAL"


commands << "Active Contracts"
commands << '''
SELECT  c.id, c.name, cl.name AS client_name, start, c.type FROM contract c  LEFT JOIN client cl\
 ON c.client_id=cl.id WHERE c.start < NOW() AND ( ( c.end IS NULL ) OR (c.end > NOW()) )
 ORDER BY c.type DESC, client_name;
'''

commands << "---- DB ADMIN WARNINGS:"

commands << "PROBLEMS: Conatracts ended but not confirmed"
commands << "SELECT  c.id,c.name FROM contract c WHERE DATEDIFF(CURRENT_DATE(),c.end)>=0 AND c.end IS NOT NULL AND c.confirmedClosed IS NULL"

commands << "PROBLEMS: Contracts missing main_profile_id"
commands << "SELECT  c.id,c.name FROM contract c WHERE main_profile_id IS NULL"

commands << "PROBLEMS: Profiles missing contract_id"
commands << "SELECT  p.profile_id, p.name, deleted FROM profiles p WHERE contract_id IS NULL AND deleted != 1"

commands << "Profiles that are ACTIVE but DO NOT HAVE a contract:"
commands << '''
SELECT p.name AS "Profile_Name", c.name AS "Client_Name", DATE(p.created) AS created, deleted, schedule FROM profiles p LEFT JOIN contract c ON p.contract_id=c.id
WHERE
  (deleted =0) AND (`schedule` != 'Frozen') 
  AND ( NOT EXISTS( SELECT * FROM contract c WHERE c.confirmedClosed IS NULL AND p.contract_id=c.id) )
  order by c.name, p.created DESC
'''


// -----------------------
// CODE
// -----------------------
Properties p = g.properties;

uri = "jdbc:mysql://${p.host}:${p.port}/${p.db}"
def sql = Sql.newInstance(uri, p.user, g.properties.pass, "com.mysql.jdbc.Driver")

def i =0;
while (i<commands.size()) {
      if (commands[i].startsWith("---")) {
      msg += "\n\n\n<h2>"+commands[i].substring(3)+"</h2>\n\n";
      i++;
} else {
      msg += showCommand(commands[i],commands[i+1], sql);
      i+=2
};
}

if (toAddress!="") {
 simpleMail(toAddress,emailTitle,msg,p);
} else {
 println msg;
}


// FUNTCIONS:

String showCommand(title, command, Sql sql) {
     String ret = "\n\n\n<h4>${title}</h4>\n\n<table>\n"
     first = true;  
     sql.rows(command).each {  Map row ->
       ret += "<tr><td>";
       if (first) {
         first=false;
         ret += "<u>"+(row.keySet().join("</u></td><td><u>"))+"\n</u></tr><tr><td>\n";         
       }
       ret += (row.values().join("</td><td>"))+"\n";
       ret += "</td></tr>\n";
     }
     ret +="</table>\n\n";
     return ret;
     
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
