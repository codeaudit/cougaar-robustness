/*
 * ActionDescription.java
 *
 * Created on April 5, 2004, 3:31 PM
 * <copyright>  
 *  Copyright 2004 Object Services and Consulting, Inc.  
 *  under sponsorship of the Defense Advanced Research Projects  
 *  Agency (DARPA).  
 *   
 *  You can redistribute this software and/or modify it under the 
 *  terms of the Cougaar Open Source License as published on the 
 *  Cougaar Open Source Website (www.cougaar.org).   
 *   
 *  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS 
 *  "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT 
 *  LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR 
 *  A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT 
 *  OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, 
 *  SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT 
 *  LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, 
 *  DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY 
 *  THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT 
 *  (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE 
 *  OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE. 
 *   
 * </copyright>  
 */

package org.cougaar.coordinator.techspec;

import org.cougaar.core.plugin.ComponentPlugin;

import org.cougaar.core.service.LoggingService;

import org.cougaar.core.service.UIDService;

import org.cougaar.util.log.Logging;
import org.cougaar.util.log.Logger;
import org.cougaar.core.util.UID;

import org.cougaar.core.blackboard.IncrementalSubscription;
import org.cougaar.util.UnaryPredicate;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;
import java.util.ArrayList;

import org.w3c.dom.*;

/**
 *
 * @author  Administrator
 */
public class ActionDescription {
    
    String name;
    String desc;
    String whenStateIs, endStateWillBe;
    
    ActionCost oneTimeCost = null;
    ActionCost continuingCost = null;
    
    /** Creates a new instance of ActionDescription */
    public ActionDescription(String name) {
        this.name = name;
    }
        
    /** @return action name */
    public String name() { return name;}
    
    /** @return action description */
    public String description() { return desc;}
    
    /** @return whenStateIs */
    public String getWhenStateIs() { return whenStateIs;}
    
    /** @return whenStateIs */
    public void setWhenStateIs(String s) { whenStateIs = s;}
    
    /** @return endStateWillBe */
    public String getEndStateWillBe() { return endStateWillBe;}
    
    /** @return endStateWillBe */
    public void setEndStateWillBe(String s) { endStateWillBe = s;}

    
    /** Set the description for this action */
    public void setDescription(String d) {
        this.desc = d;
    }
    
    /** 
     *  Set the Action Cost. Set <b>isOneTimeCost</> to TRUE if
     *  this is the one time cost. Set to false if it is the 
     *  continuing cost.
     */
    public void setActionCost(ActionCost ac, boolean isOneTimeCost) {
     
        if (isOneTimeCost) { 
            oneTimeCost = ac; 
        } else {
            continuingCost = ac;
        }
    }        
    
    /** @return get one-time cost */
    public ActionCost getOneTimeCost() { return oneTimeCost; }

    /** @return get continuing cost */
    public ActionCost getContinuingCost() { return continuingCost; }
    
}
