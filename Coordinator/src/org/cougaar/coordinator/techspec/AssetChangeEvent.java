/*
 * AssetChangeListener.java
 *
 * Created on September 15, 2003, 2:32 PM
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

package org.cougaar.coordinator.techspec;

import org.cougaar.core.persist.NotPersistable;


/**
 * AssetChangeListener interface
 *
 * @author Paul Pazandak, OBJS
 */
public class AssetChangeEvent implements NotPersistable {
    
    public static final Event NEW_ASSET = new Event();
    public static final Event MOVED_ASSET = new Event();    
    public static final Event REMOVED_ASSET = new Event();
    public static class Event {}
    
    private Event event;
    private AssetTechSpecInterface asset;
    
    public AssetChangeEvent(AssetTechSpecInterface asset, Event event) {
    
        this.asset = asset;
        this.event = event;
        
    }
    
    public AssetTechSpecInterface getAsset() { return asset; }
    public boolean newAssetEvent() { return event == NEW_ASSET; }
    public boolean moveEvent() { return event == MOVED_ASSET; }
    public boolean assetRemovedEvent() { return event == REMOVED_ASSET; }
}
