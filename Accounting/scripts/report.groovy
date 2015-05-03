// Generate simple email report about contracts ending soon
// Hugo Zaragoza, Websays, 2015


// HEADER
if (args.size()!=2) {
  println "ARGUMENTS:\n\t<WEBSAYS_HOME> <email>\n\t email format: 'first@mail.com;second@othermail.com'"
  return;
}
Globals g = new Globals(args[0]);
toAddress=args[1];

import groovy.sql.Sql
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

toAddress = "hugo.zaragoza@websays.com";
emailTitle = "ACCOUNTING: Contracts Ending or Renewing Soon";

commands << "Contracts ending in the next 30 days:";
commands << "SELECT c.id, c.name, cl.name AS client_name, start,end FROM contract c LEFT JOIN client cl ON c.client_id=cl.id WHERE c.end>=CURRENT_DATE() AND DATEDIFF(c.end, CURRENT_DATE())<=30;" 

commands << "Contracts auto-renewing in the next 30 days:";
commands << "SELECT c.id, c.name, cl.name AS client_name, start, DATE_ADD(c.start,INTERVAL c.contractedMonths MONTH)  AS renewing FROM contract c LEFT JOIN client cl ON c.client_id=cl.id WHERE DATEDIFF( DATE_ADD(c.start,INTERVAL c.contractedMonths MONTH) , CURRENT_DATE())<=30 AND end IS NULL;"

commands << "Contracts that Ended Recently"
commands << "SELECT c.id, c.name, cl.name AS client_name, start, end FROM contract c  LEFT JOIN client cl ON c.client_id=cl.id WHERE DATEDIFF(NOW(),c.end)<7 AND c.end<=CURRENT_DATE();"

commands << "---"

commands << "Active Contracts"
commands << "SELECT  c.id, c.name, cl.name AS client_name, start FROM contract c  LEFT JOIN client cl ON c.client_id=cl.id WHERE c.start < NOW() AND ( ( c.end IS NULL ) OR (c.end > NOW()) );"



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

simpleMail(toAddress,emailTitle,msg,p);



// FUNTCIONS:

String showCommand(title, command, Sql sql) {
     String ret = "\n\n${title}\n"
     sql.eachRow(command) {
       ret += "${it}\n"
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
