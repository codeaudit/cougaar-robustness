/*   
 * CostBenefitKnob.java
 *
 * Created on July 8, 2003, 4:13 PM
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

package org.cougaar.coordinator.costBenefit;
import org.cougaar.core.persist.NotPersistable;

/**
 *  Used to control aspects of the cost benefit plugin
 * 
 */
public class CostBenefitKnob implements NotPersistable {
    
    long horizon = 10L;
    String calcMethod = "default";
    
    /** Creates a new instance of CostBenefitKnob */
    public CostBenefitKnob() { }
    
    public CostBenefitKnob(long horizon) { this.horizon = horizon; }
 
    
    public void setHorizon(long horizon) { this.horizon = horizon; }
    
    public long getHorizon() { return horizon; }
    

    public void setCalcMethod (String calcMethod) { this.calcMethod = calcMethod; }
    
    public String getCalcMethod() { return calcMethod; }
    
}
