/*
 *<SOURCE_HEADER>
 *
 *<NAME>
 * $RCSfile: BeliefMonitorServlet.java,v $
 *</NAME>
 *
 * <copyright>
 *  Copyright 2004 Telcordia Technologies, Inc.
 *  under sponsorship of the Defense Advanced Research Projects Agency (DARPA)
 *  and the Defense Logistics Agency (DLA).
 * 
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the Cougaar Open Source License as published by
 *  DARPA on the Cougaar Open Source Website (www.cougaar.org).
 * 
 *  THE COUGAAR SOFTWARE AND ANY DERIVATIVE SUPPLIED BY LICENSOR IS
 *  PROVIDED 'AS IS' WITHOUT WARRANTIES OF ANY KIND, WHETHER EXPRESS OR
 *  IMPLIED, INCLUDING (BUT NOT LIMITED TO) ALL IMPLIED WARRANTIES OF
 *  MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE, AND WITHOUT
 *  ANY WARRANTIES AS TO NON-INFRINGEMENT.  IN NO EVENT SHALL COPYRIGHT
 *  HOLDER BE LIABLE FOR ANY DIRECT, SPECIAL, INDIRECT OR CONSEQUENTIAL
 *  DAMAGES WHATSOEVER RESULTING FROM LOSS OF USE OF DATA OR PROFITS,
 *  TORTIOUS CONDUCT, ARISING OUT OF OR IN CONNECTION WITH THE USE OR
 *  PERFORMANCE OF THE COUGAAR SOFTWARE.
 * </copyright>
 */

package org.cougaar.coordinator.believability;

import java.io.IOException;
import java.io.PrintWriter;

import java.text.DecimalFormat;

import java.util.Enumeration;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Vector;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.cougaar.coordinator.*;
import org.cougaar.coordinator.techspec.AssetID;
import org.cougaar.coordinator.techspec.AssetState;
import org.cougaar.coordinator.techspec.AssetStateDimension;
import org.cougaar.coordinator.techspec.AssetType;

import org.cougaar.core.blackboard.IncrementalSubscription;
import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.plugin.ComponentPlugin;
import org.cougaar.core.service.LoggingService;
import org.cougaar.core.service.ServletService;

import org.cougaar.util.UnaryPredicate;

/**
 * Servlet for Sample Defense
 */
public class BeliefMonitorServlet extends ComponentPlugin
{
    private ServiceBroker sb;
    private LoggingService log;
    private ServletService servletSvc;

    private int _state_estimation_count = 0;

    // This is hashed on Asset ID name , which will return a String,
    // which is an HTML table representation of a StateEstimation
    // object.
    //
    private Hashtable _last_estimate = new Hashtable();

    // An ordered list of asset IDs to ensure that we always show the
    // assets in the same order (arrival order).  A Hastable may
    // shuffle things around as new things get added. 
    //
    private Vector _asset_id_list = new Vector();

    private DecimalFormat _double_format = new DecimalFormat("0.0000");

    // Base RGB values that map to probability 1.0. All other
    // probabilities will scale these from 0 to this value based on
    // the probability.
    //
    private int _base_color[] = { 255, 255, 0 };

    MyServlet myServlet;

    private IncrementalSubscription stateEstimationSub;

    private UnaryPredicate stateEstimationPred = new UnaryPredicate() {
         public boolean execute(Object o) {
          return (o instanceof StateEstimation);}};
      
    public void load() {
     super.load();
     sb = getServiceBroker();
     log = (LoggingService) 
         sb.getService(this, LoggingService.class, null);
     if (log == null) {
         log = LoggingService.NULL;
     }
     servletSvc = (ServletService)
         sb.getService(this, ServletService.class, null);
    }

    public void unload() {
     if ((log != null) &&
         (log != LoggingService.NULL)) {
         sb.releaseService(this, LoggingService.class, log);
         log = LoggingService.NULL;
     }
     if (servletSvc != null) {
         sb.releaseService(this, ServletService.class, servletSvc);
     }
     super.unload();
    }
    
    public void setupSubscriptions() 
    {
     try {
         servletSvc.register("/BeliefMonitor", new MyServlet());
     } catch (Exception e) {
         log.error("Unable to register belief monitor servlet",e);
     }
        stateEstimationSub = 
         (IncrementalSubscription)blackboard.subscribe(stateEstimationPred);
        myServlet = new MyServlet();
    }

    public void execute() {   
     Iterator iter = stateEstimationSub.getAddedCollection().iterator();
     while (iter.hasNext()) {
         StateEstimation data = (StateEstimation)iter.next();
         if (data != null) {
             handleStateEstimation( data );
         }
     }
    }

    /**
     * Call this upon the receipt of  anew StateEstimation object and
     * it will update the local state variables accordinly.
     *
     * @param se The new StateEstimation to be added
     */
    protected void handleStateEstimation( StateEstimation se )
    {
        _state_estimation_count++;

        AssetID asset_id = se.getAssetID();

        String old_se 
                = (String) _last_estimate.get( asset_id );

        if (old_se == null )
        {
            _asset_id_list.addElement( asset_id );
        }

        _last_estimate.put( asset_id, 
                            stateEstimationToHTMLTableString(se) );

    } // method handleStateEstimation

    /**
     * Convert the state estimation to a string representation as
     * an HTML table.
     */
    protected String stateEstimationToHTMLTableString( StateEstimation se )
    {
        StringBuffer buff = new StringBuffer();

        AssetType asset_type = se.getAssetType();

        Vector state_dim_list = asset_type.getCompositeState();

        if ( state_dim_list == null )
            return "State Dimension Error";  // don't deal with errors for now
              
        if ( state_dim_list.size() == 0 )
            return "State Dimension Error";  // don't deal with errors for now
              
        buff.append( "<p><table border=\"2\">\n" );
              
        buff.append( "  <tr><th colspan=\""
                     + state_dim_list.size()
                     + "\" bgcolor=\"#c0c0c0\">Asset ID: " 
                     + se.getAssetID().getName()
                     + ", Asset Type: "
                     + asset_type.getName()
                     + "</th></tr>\n" );
              
        buff.append( "  <tr valign=\"top\">\n" );
              
        Enumeration se_dim_enum = se.getStateDimensionEstimations();
        while ( se_dim_enum.hasMoreElements() )
        {
            StateDimensionEstimation se_dim
                    = (StateDimensionEstimation) se_dim_enum.nextElement();
            
            AssetStateDimension state_dim 
                    = se_dim.getBeliefStateDimension().getAssetStateDimension();

            // This will allow us to enumerate all the
            // possible states this dimension can be in.
            //
            Vector asset_state_list = state_dim.getPossibleStates();
                  
            if ( asset_state_list == null )
                continue;  // don't deal with errors for now
                  
            if ( asset_state_list.size() == 0 )
                continue;  // don't deal with errors for now
                  
                  
            buff.append( "    <td>\n" );
                  
            buff.append( "      <table border=\"1\""
                         + " cellpadding=\"2\">\n" );
                  
            buff.append( "        <tr><th colspan=\"2\"" 
                         + " bgcolor=\"#dadada\"\">" 
                         + state_dim.getStateName() + "</th></tr>\n" );
                  
            Iterator asset_state_iter = asset_state_list.iterator();
                  
            for ( int val_idx = 0; asset_state_iter.hasNext(); val_idx++ )
            {
                AssetState state = (AssetState) asset_state_iter.next();                          
                String val_name = state.getName();
                      
                double prob;   
                try {
                    prob = se_dim.getProbability( val_name );
                } catch ( BelievabilityException be)
                {
                    buff.append( "        <tr><td colspan=\"2\">" 
                                 + "No Data (" + be.getMessage()
                                 + ")</td></tr>\n" );
                    continue;
                }
                      
                int bg_color_value
                        = 256 * 256 * ((int) (_base_color[0] * prob))
                        + 256 * ((int) (_base_color[1] * prob))
                        + ((int) (_base_color[2] * prob));

                String bg_color = "000000" 
                        + Integer.toHexString( bg_color_value );

                String fg_color = "black";

                if ( prob < 0.5 )
                    fg_color = "white";

                buff.append( "        <tr><td>" 
                             + val_name + "</td>\n" );
                      
                buff.append( "<td bgcolor=\"#" 
                             + bg_color.substring( bg_color.length()-6,
                                                   bg_color.length() )
                             + "\">"
                             + "<font color=\"" 
                             + fg_color + "\">"
                             + _double_format.format(prob) 
                             + "</font>"
                             + "</td></tr>\n" );

            } // while asset_state_iter
                  
            buff.append( "      </table>\n" );
            buff.append( "    </td>\n" );
                  
        } // while state_dim_iter
              
        buff.append( "  </tr>\n</table></p>\n" );

        return buff.toString();
    }

    private class MyServlet extends HttpServlet {
     
     public void doGet(HttpServletRequest request, HttpServletResponse response) {
            
         int refreshRate = 60000;
         String refreshError = null;

         response.setContentType("text/html");
         
         try {
          PrintWriter out = response.getWriter();
          out.println("<html>");
          out.println("<META HTTP-EQUIV=\"PRAGMA\" CONTENT=\"NO-CACHE\">");
          out.println("<script type=\"text/javascript\">");
          out.println("function refreshPage(){");   
          out.println("     var szURL = \"?REFRESH="+refreshRate+" \";");
          out.println("     self.location.replace(szURL);");
          out.println("}</script>");
          out.println("<head></head><body onload=\"setTimeout('refreshPage()', " +refreshRate+");\">");
          out.println("<h2>Belief State Monitoring Servlet</h2>");
          out.println("<p><em>(Will refresh every " + (refreshRate/1000) + " seconds.)</em></p>");



          out.println( "<p>Total state estimates seen: " 
                       + _state_estimation_count + "</p>" );

          out.println( "<center>" );

          Enumeration id_enum = _asset_id_list.elements();
          while( id_enum.hasMoreElements() )
          {
              AssetID id = (AssetID) id_enum.nextElement();

              String se_str 
                      = (String) _last_estimate.get( id );

              if ( se_str == null )
                  continue;

              out.println( se_str );

          } // while id_enum
          
          out.println( "</center>" );
          
          out.println( "</body></html>" );
          
          out.close();
         } catch (IOException e) { 
             log.error("IOException writing servlet response",e);
         }
     }
    }
}
