/*
 * DisconnectAgentPlugin.java
 *
 * @author David Wells - OBJS
 *
 * <copyright>
 *  Copyright 2003 Object Services and Consulting, Inc.
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

package org.cougaar.tools.robustness.disconnection;

import org.cougaar.tools.robustness.disconnection.InternalConditionsAndOpModes.*;

import java.util.Iterator;
import java.util.Date;
import java.util.Collection;

import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.blackboard.IncrementalSubscription;
import org.cougaar.util.UnaryPredicate;
import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.service.community.Community;
import org.cougaar.core.service.community.CommunityChangeEvent;
import org.cougaar.core.service.community.CommunityChangeListener;
import org.cougaar.core.service.community.CommunityResponse;
import org.cougaar.core.service.community.CommunityResponseListener;
import org.cougaar.core.service.community.CommunityService;
import org.cougaar.core.service.community.Entity;

public class DisconnectAgentPlugin extends DisconnectPluginBase {
 
  private CommunityService commSvc;
  private String community = null;
  private AgentExistsCondition aec = null;

  public DisconnectAgentPlugin() {
    super();
  }

  
  public void load() {
      super.load();
      ServiceBroker sb = getServiceBroker();
      commSvc = (CommunityService)
	  sb.getService(this, CommunityService.class, null);
//      initObjects(); 
  }
  
  
    public void suspend() {
	if (aec != null) {
	    // Remove the AgentExistsCondition so that the DisconnectNodePlugin will know the Agent has left the Node
	    UnaryPredicate pred = new UnaryPredicate() {
		    public boolean execute(Object o) {
			return 
			    (o instanceof AgentExistsCondition);
		    }
		};
	    
	    AgentExistsCondition cond = null;
	    
	    getBlackboardService().openTransaction();
	    Collection c = getBlackboardService().query(pred);
	    if (c.iterator().hasNext()) {
		cond = (AgentExistsCondition)c.iterator().next();
		if (logger.isDebugEnabled()) logger.debug("UNLOADING "+cond.getAsset());
		getBlackboardService().publishRemove(cond); //lets the NodeAgent learn that the Agent has unloaded
	    }    
	    getBlackboardService().closeTransaction();
	    
	    super.suspend();
	}
    }
    
    public void setupSubscriptions() {
	
	// Add a listener for changes in robustness community
	commSvc.addListener(new CommunityChangeListener() {
		public void communityChanged(CommunityChangeEvent cce) {
		    if (logger.isDebugEnabled()) 
			logger.debug("CommunityChangeListener.communityChanged("+cce+")");
		    communitySearch(); }
		public String getCommunityName() { 
		    return community; }} );
    }
    
    private void initObjects() {
	// create an AgentExistsCondition object to inform the NodeAgent that the agent is here
	aec = new AgentExistsCondition("Agent", getAgentID());
	aec.setUID(getUIDService().nextUID());
	aec.setSourceAndTarget(getAgentAddress(), getNodeAddress());
	if (logger.isDebugEnabled()) logger.debug("Source: "+getAgentAddress()+", Target: "+getNodeAddress());
	
	//getBlackboardService().openTransaction();
	getBlackboardService().publishAdd(aec);
	//getBlackboardService().closeTransaction();
	
	if (logger.isDebugEnabled()) logger.debug("Announced existence of "+getAgentID());   
    }      
    
    
    public void execute() {
	if (community != null && aec == null)
	    initObjects();
    }
    
    private void communitySearch() {
	Collection communities = 
	    commSvc.listParentCommunities(getAgentID(),
					  "(CommunityType=Robustness)",
					  new CommunityResponseListener() {
					      public void getResponse(CommunityResponse response) {
						  if (response.getStatus()==CommunityResponse.SUCCESS) {
						      Collection communities = 
							  (Collection)response.getContent();
						      if (communities != null) 
							  gotCommunity(communities);}}});
	if (communities != null) gotCommunity(communities);
    }
    
    private void gotCommunity(Collection comms) {
	if (comms == null) {
	    return;
	} else if (comms.size() == 0) {
	    return;
	} 
        Iterator it = comms.iterator();
	while (it.hasNext()) {
	    String commName = (String)it.next();
	    Community comm = commSvc.getCommunity(commName, null);
	    if (comm != null) {
		javax.naming.directory.Attributes attrs = comm.getAttributes();
		if (attrs != null) {
		    javax.naming.directory.Attribute attr = attrs.get("CommunityType");
		    if (attr != null && attr.contains("Robustness") && community == null) {
			community = commName;
			if (logger.isInfoEnabled()) 
			    logger.info("Found Robustness Community "+comm);
			getBlackboardService().signalClientActivity();
		    }
		}
	    }
	}
    }
}

