/*
 * ActionUtils.java
 *
/*
 * ActionUtils.java
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
import org.cougaar.coordinator.techspec.*;

/**
 * This class provides accessor methods to get at package private methods
 * of the Action class. The Action methods are package private to keep the 
 * Action api simple... for better or worse.
 */
public  class ActionUtils 
{

    /**
     * @return the asset type related to this defense condition
     */
    public static AssetType getAssetType(Action a) { 
        if (a != null) { return a.getAssetType();  }
        else { return null; }
        
    }
    
    
    /**
     * @return expanded name - "type:name"
     */
    public static AssetID getAssetID(Action a) { 
        if (a != null) { return a.getAssetID(); }
        else { return null; }
    }

    public static String getActuatorType(Action a) {
        if (a != null) { return a.getClass().getName(); }
        else { return null; }
    }

    
    /**
     * @return expanded name - "type:name"
     */
    public static String setPermittedValues(Action a, java.util.Set s) { 
        try {
            if (a != null) { a.setPermittedValues(s); }
            return null;
        } catch (Exception e) {
            return e.toString();
        }
    }
    
}
