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
msg="";


// -----------------------
// DEFINE COMMANDS TO RUN:
// -----------------------

emailTitle = "ACCOUNTING: Contracts Ending or Renewing Soon";

//commands << "Active Pilots:";
//commands << '''SELECT  sales_person, c.id, c.name, c.pilot_length - DATEDIFF(CURRENT_DATE(), c.start) AS daysReamining, COUNT(c.id) AS '#profiles'
//  FROM profiles p LEFT JOIN contract c ON p.contract_id=c.id
//  WHERE (c.type='internal' OR c.type='pilot')
//    AND ( (c.end IS  NULL) OR (c.end>CURRENT_DATE()) )
//  GROUP BY c.id ORDER BY sales_person, daysReamining, c.name;'''

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

commands << "---"

//commands << "PROBLEMS: Ended but not confirmed"
//commands << "SELECT  c.id,c.name FROM contract c WHERE DATEDIFF(CURRENT_DATE(),c.end)>=0 AND c.end IS NOT NULL AND confirmEnd=1"

commands << "PROBLEMS: Missing main_profile_id"
commands << "SELECT  c.id,c.name FROM contract c WHERE main_profile_id IS NULL"

commands << "PROBLEMS: Missing contract_id"
commands << "SELECT  p.profile_id, p.name FROM profiles p WHERE contract_id IS NULL AND deleted <> 0"


commands << "---"


commands << "Active Contracts"
commands << '''SELECT  c.id, c.name, cl.name AS client_name, start, c.type FROM contract c  LEFT JOIN client cl\
 ON c.client_id=cl.id WHERE c.start < NOW() AND ( ( c.end IS NULL ) OR (c.end > NOW()) )
ORDER BY c.type DESC, client_name;'''



// -----------------------
// CODE
// -----------------------
Properties p = g.properties;

uri = "jdbc:mysql://${p.host}:${p.port}/${p.db}"
def sql = Sql.newInstance(uri, p.user, g.properties.pass, "com.mysql.jdbc.Driver")

def i =0;
while (i<commands.size()) {
      if (commands[i]=="---") {
      msg += "\n-----------------------------\n";
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
     String ret = "\n\n\n${title}\n\n"
     sql.eachRow(command) {
       ret += "${it}\n";
     }
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
    
    for(email in to.split(";")) {
       InternetAddress toAddress = new InternetAddress(email); 
       message.addRecipient(Message.RecipientType.TO, toAddress);
    }

    message.setSubject(subject);
    message.setText(body);
 
    Transport transport = session.getTransport("smtp");
    transport.connect(p.SMTP_HOST, p.SMTP_USER, p.SMTP_PASSWORD);
    transport.sendMessage(message, message.getAllRecipients());
    transport.close();
}
