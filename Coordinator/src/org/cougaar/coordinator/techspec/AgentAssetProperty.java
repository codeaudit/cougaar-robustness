/*
 * AgentAssetProperty.java
 *
 * Created on September 11, 2003, 9:32 AM
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
 * @author Paul Pazandak, Ph.D. OBJS, Inc.
 */
public class AgentAssetProperty extends AssetProperty {
  
    public static final AgentAssetPropertyName location = new AgentAssetPropertyName("LOCATION");
    public static final AssetPropertyName[] names = {location};    
    
    /** Creates a new instance of AgentAssetProperty */
    public AgentAssetProperty(AgentAssetPropertyName propName, Object value) {
        super(propName, value);
    }
    
    /**
     * @return the valid property names that are accepted
     */
    public static AssetPropertyName[] getValidPropertyNames() { return names; }

    /** This class is used to control the creation of property names */
    static class AgentAssetPropertyName implements AssetPropertyName {        
        private String name;
        private AgentAssetPropertyName(String name) { this.name = name; }        
        public String toString() { return name; }
    }
    
}
