/*
 * ContainmentFilter.java
 *
 * Created on March 26, 2004, 4:24 PM
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

package org.cougaar.coordinator.techspec;

/**
 *
 * @author  Administrator
 */
public class ContainmentFilter {
    
    private String eventAssetContainerName;
    
    /** Creates a new instance of ContainmentFilter */
    public ContainmentFilter(String eventAssetContainerName) {
        
        this.eventAssetContainerName = eventAssetContainerName;
    }
    
    /**
     * @return the container name for this filter
     */
    public String getContainerName() { return eventAssetContainerName; }
    
    /**
     * @return true if an asset qualifies -- if the threat's filters don't exclude the asset
     */
    protected boolean qualifies(ThreatModelManagerPlugin mgr, AssetTechSpecInterface asset) {

        return true;
//FIX        
    }

    
    /**
     * @return true if an asset qualifies -- if the threat's filters don't exclude the asset
     */
    protected boolean qualifies(TransitiveEffectModelManagerPlugin mgr, AssetTechSpecInterface asset) {

        return true;
//FIX        
    }
    
    
    public String toString() {
        return "CONTAINMENT FILTER. Filter on container = "+this.getContainerName();
    }
}
