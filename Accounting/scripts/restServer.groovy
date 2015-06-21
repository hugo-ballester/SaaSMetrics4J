// Groovy script to build a REST API wrapper around MyHTMLReport.main
// Used to ivoke remoetely the generation of billing reports
//
// depends on Accounting.jar, see https://dev1/dokuwiki/doku.php?id=accounting_procedures
//
// Author: hugoz 2015-6

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

@Path("/accountingReport")
class Main {
  static String paramFile=null;

   @PUT @Produces("text/plain")
   public Response writeReport() {
           def root = System.getenv("WEBSAYS_HOME");
           def args = ["-p",paramFile] as String[];
           MyHTMLReport.main(args);
           return Response.ok().entity("ok").build();
   }
   
   public static startServer(String paramFile, int myport) {
       Main.paramFile=paramFile;     
       ResourceConfig resources = new ClassNamesResourceConfig(Main)
       def uri = UriBuilder.fromUri("http://dev1/").port(myport).build();
       HttpServer httpServer = GrizzlyServerFactory.createHttpServer(uri, resources);
       println("Jersey app started with WADL available at ${uri}application.wadl")
       while (true) { sleep(1000); }; // stay alive until stopped
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