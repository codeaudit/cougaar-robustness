/*
 * ActionCost.java
 *
 * Created on April 6, 2004, 10:20 AM
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

/**
 * This class holds the transition costs for a given action.
 * @author  Administrator
 */
public class ActionCost {
    
    Cost bandwidth=null, cpu=null, memory=null;
    int time=-1;
    
    
    /** Creates a new instance of ActionCost */
    public ActionCost() {
    }
    
    
    /** Set the cost of a given dimension - bandwidth, cpu or memory */
    public void setCost(String costName, float fIntensity, boolean asf, boolean msf) {
        
        if (costName.equalsIgnoreCase("Bandwidth")) { bandwidth = new Cost( fIntensity, asf, msf); }
        else if (costName.equalsIgnoreCase("CPU")) { cpu = new Cost( fIntensity, asf, msf); }
        else if (costName.equalsIgnoreCase("Memory")) { memory = new Cost( fIntensity, asf, msf); }
    }

    /** Set the time duration the action will take to execute */
    public void setTimeCost(int duration) { time = duration; }
    
    public Cost getBandwidthCost() { return bandwidth; }
    public Cost getCPUCost() { return cpu; }
    public Cost getMemoryCost() { return memory; }
    public int getTimeCost() { return time; }
    
    public String toString() {
     
        String s = "      Bandwidth: "+ ((bandwidth != null) ? bandwidth.toString() : "N/A") + "\n";
              s += "      CPU:       "+ ((cpu != null) ? cpu.toString() : "N/A") + "\n";
              s += "      Memory:    "+ ((memory != null) ? memory.toString() : "N/A") + "\n";
              s += "      Time:      "+ ((time >=0) ? "duration = " + time : "N/A") + "\n";
        return s;
    }
    
        
    /** Holds the cost info for a given dimension */
    public class Cost {
        
        float fIntensity;
        boolean asf = false; //asf = agentSizeFactor, msf = messageSizeFactor
        boolean msf = false; //asf = agentSizeFactor, msf = messageSizeFactor
        
        Cost(float fIntensity, boolean asf, boolean msf) {
            this.fIntensity = fIntensity;
            this.asf = asf;
            this.msf = msf;
        }
        
        public float getIntensity() { return fIntensity; }
        public boolean isAgentSizeAFactor() { return asf; } 
        public boolean isMessageSizeAFactor() { return msf; }         
        
        public String toString() {
            return "Intensity="+fIntensity+ "  " + (asf ? "Agent Size Factor; " : "") + 
                                                   (msf ? "Message Size Factor " : "") ;
        }
    }
}
