/*
 * AssetState.java
 *
 * Created on September 8, 2003, 1:22 PM
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
import org.cougaar.core.persist.NotPersistable;
import java.io.Serializable;
/**
 * @author Paul Pazandak, Ph.D. OBJS, Inc.
 */
public class AssetState implements NotPersistable, Serializable {    
    
    private String name;
    private float mauCompleteness;
    private float mauSecurity;
    
    public final static AssetState ANY = new AssetState("*", 0, 0);
    
    /** Creates a new instance of AssetState */
    public AssetState(String name, float completeness, float security) {
        this.name = name;
        this.mauCompleteness = completeness;
        this.mauSecurity = security;
    }
    
    /** Return the string value of this state */
    public String getName() { return name; }
    
    /** 
     * @return the RelativeMauCompleteness value 
     */
    public float getRelativeMauCompleteness() { return mauCompleteness; }
    
    /** 
     * @return the RelativeMauSecurity value 
     */
    public float getRelativeMauSecurity() { return mauSecurity; }

    /** 
     * @return TRUE if the value returned by getName() of each obhect matches 
     */
    public boolean equals(Object o) {     
        return ( (o instanceof AssetState) && (o != null) && ( getName().equals(((AssetState)o).getName()) ) );
    }
    
    public String toString() { return "AssetState["+name+"] MauComplete="+this.mauCompleteness+", MauSecurity="+this.mauSecurity; }
}
