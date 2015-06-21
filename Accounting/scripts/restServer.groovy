
import javax.ws.rs.*
import javax.ws.rs.core.*
import com.sun.jersey.api.core.*
import com.sun.jersey.api.container.grizzly2.GrizzlyServerFactory
import org.glassfish.grizzly.http.server.HttpServer

import websays.accounting.app.MyHTMLReport

@GrabConfig(systemClassLoader = true)
@GrabResolver(name = 'gretty', root = 'http://groovypp.artifactoryonline.com/groovypp/libs-releases-local')
@Grapes([
   @Grab('com.sun.jersey:jersey-server:1.12'),
   @Grab('com.sun.jersey:jersey-core:1.12'),
   @Grab(group='com.sun.jersey', module='jersey-grizzly2', version='1.12'),
   @Grab(group='javax.ws.rs', module='jsr311-api', version='1.1.1')
   ])

//@Path("/{code}")
//class Main {
//
//   @GET @Produces("text/plain")
//   public Response getUserByCode(@PathParam('code') String code) {
//           def user = "hugo"
//           return Response.ok().entity("Usage of the code '${code}': $user\n".toString()).build();
//   }

class Globals {
  static int port = 0;
  static String paramFile = "";
}

@Path("/report")
class Main {
  static String paramFile=null;

   @GET @Produces("text/plain")
   public Response getReport() {
           println "GET /report";
           def root = System.getenv("WEBSAYS_HOME");
           def args = ["-p",paramFile,"-y","2015", "-m","1"] as String[];
           MyHTMLReport.main(args);
           return;
           
           return Response.ok().entity("ok").build();
   }
   
   public static startServer(String paramFile, int myport) {
       Main.paramFile=paramFile;     
       ResourceConfig resources = new ClassNamesResourceConfig(Main)
       def uri = UriBuilder.fromUri("http://localhost/").port(myport).build();
       HttpServer httpServer = GrizzlyServerFactory.createHttpServer(uri, resources);
       println("Jersey app started with WADL available at ${uri}application.wadl")
       while (true) { sleep(1000); };
       
//       System.in.read();
//       httpServer.stop();
   }
}

if (args.size()<2) {
  println "USAGE: <parameter_file> <port>"
}

def paramFile = args[0];
def params = new File(paramFile);
if (!params.exists()) {
  println "Could not find parameter file: "+paramFile;
  return;
}

myport=args[1] as Integer;
Main.startServer(paramFile,myport)