/*
 * DefenseOperatingMode.java
 *
 * @author David Wells - OBJS
 *
 * <copyright>
 * 
 *  Copyright 2003-2004 Object Services and Consulting, Inc.
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


package org.cougaar.coordinator;

import org.cougaar.core.adaptivity.SensorCondition;
import org.cougaar.core.adaptivity.OMCThruRange;
import org.cougaar.core.adaptivity.OMCRangeList;
import org.cougaar.core.adaptivity.OMCPoint;

import org.cougaar.core.service.BlackboardService;
import org.cougaar.util.UnaryPredicate;
import java.util.Collection;
import java.util.Iterator;

import org.cougaar.core.util.UID;


public class DefenseTimeCondition //extends DefenseCondition
{
    public static final Double MINTIME  = new Double(0.0);
    public static final Double MAXTIME = new Double(9223372036854775807.0);

    //protected static OMCRangeList allowedValues = new OMCRangeList(new OMCThruRange (MINTIME, MAXTIME));

     /* Do not use. Only use the constructors of the subclasses.
     */
    public DefenseTimeCondition(String assetType, String assetName, String managerID) {
        //super(assetType, assetName, managerID, allowedValues, new Double(0.0));
    }
    
    public DefenseTimeCondition(String assetType, String assetName, String managerID, long time) {
        //super(assetType, assetName, managerID, allowedValues, new Double(time));
    }
    
    protected void setValue(String newValue) {
        //super.setValue(newValue);
    }

    /** tiny helper class for VTH Operating Modes */
    //protected static class OMCStrPoint extends OMCPoint {
    //    public OMCStrPoint (String a) { super (a); }
    //}
    

}