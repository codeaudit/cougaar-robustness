/*
 * TestHttpServer.java
 *
 * Created on September 16, 2003, 2:34 PM
 */

package org.cougaar.coordinator.test.defense;
/**  org\cougaar\tools\robustness\deconfliction\test\defense\Httpd
 * @(#)Httpd.java
 * @author Qusay H. Mahmoud
 */
import java.io.*;
import java.net.*;
import java.util.*;
import java.lang.*;

/**
 * This class implements a multithreaded simple minded HTTP server. This
 * server doesn't handle CGI. All it does it listens on a port  and
 * waits for connections and servers requested documents.
 * (I can, however, serve applets  -- dg)
 * To shut down, use ctrl-c.
 */
public class Httpd extends Thread {

    // there is no particular reason to use a separate thread here - dg.
    protected ServerSocket listen;
    
    public Httpd(int HTTP_PORT) {
     try {
        listen = new ServerSocket(HTTP_PORT);
        System.out.println("HTTP server running on port " + HTTP_PORT);
     } catch(IOException ex) {
        System.out.println("Exception..."+ex);
     }
     this.start();
    }

    // multi-threading -- create a new connection for each request
    public void run() {
    try {
       while(true) {
         Socket client = listen.accept();
         Connects cc = new Connects(client);
     }
     } catch(IOException e) {
       System.out.println("Exception..."+e);
     }
    }
    
    public static void main(String argv[]) throws IOException {

     // Otherwise the server would be a security hold. See the
     // code for OurHttpdSecurityManager below.

     //System.setSecurityManager(new OurHttpdSecurityManager());

     if(argv.length == 0 || argv.length > 1) {
         System.err.println("USAGE: start java http <port>");
         System.exit(0);
     }
     new Httpd(Integer.parseInt(argv[0]));
    }
}

class Connects extends Thread {
    Socket client;
    BufferedReader is;
    DataOutputStream os;

    public Connects(Socket s) { 
        client = s;
        try {
           is = new BufferedReader(new InputStreamReader(client.getInputStream()));
           os = new DataOutputStream(client.getOutputStream());
        } catch (IOException e) {
           try {
             client.close();
           } catch (IOException ex) {
             System.out.println("Error while getting socket streams.."+ex);
           }
           return;
        }
        this.start(); 
    }
    public void run() {
    try {
       // get a request and parse it.
       String request;

       // Analyse the http GET command. By default, StringTokenizer
       // uses whitespace as its delimiter.

       //StringTokenizer st = new StringTokenizer( request );
       
       for (int i=0;i<40;i++) {
           request = is.readLine();
           System.out.println( "Request: "+i+"-- "+request );
       }
       // Typical requests would be GET /doc.html HTTP/1.0
       // or GET /mydocs/ HTTP/1.0

        client.close();
        } catch ( IOException e ) {
            System.out.println( "I/O error " + e );
        } catch (Exception ex) {
            System.out.println("Exception: "+ex);
        }
     }
    /**
     * Read the requested file and ships it to the browser if found.
     */
     public static void shipDocument(DataOutputStream out, File f) throws Exception {
       try {
          DataInputStream in = new DataInputStream(new FileInputStream(f));
          int len = (int) f.length();
          byte buf[] = new byte[len];
          in.readFully(buf);

          // write the basic http server reply header

          out.writeBytes("HTTP/1.0 200 OK\r\n");
          out.writeBytes("Content-Length: " + buf.length +"\r\n");

          // Note the second \r\n here. The reply header is terminated
          // with a blank line

          out.writeBytes("Content-Type: text/html\r\n\r\n");

          // Now write the body of the reply (the actual document).
    
          out.write(buf);
          out.flush();
          in.close();
       } catch (FileNotFoundException e) {
          out.writeBytes("404 Not Found");
       } catch (SecurityException e1) {
          out.writeBytes("403 Forbidden...not enough access rights"+e1);
       }
     }
}
