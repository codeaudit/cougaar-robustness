/*
 * <copyright>
 * 
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

import java.io.Serializable;

public class AssetChangeEvent implements Serializable, AssetChangeEventConstants {
    
    private int event;
    private AssetTechSpecInterface asset;
    
    public AssetChangeEvent(AssetTechSpecInterface asset, int event) {
        this.asset = asset;
        this.event = event;
    }
    
    public AssetTechSpecInterface getAsset() { return asset; }
    public boolean newAssetEvent() { return event == NEW_ASSET; }
    public boolean moveEvent() { return event == MOVED_ASSET; }
    public boolean assetRemovedEvent() { return event == REMOVED_ASSET; }
    
    public String toString() {
        String evt = UNKNOWN;
        if (event == NEW_ASSET) {evt = "NewAsset"; }
        else if (event == MOVED_ASSET) {evt = "MovedAsset"; }
        else if (event == REMOVED_ASSET) {evt = "RemovedAsset"; }
        return "AssetChangeEvent:: Asset="+asset.getName() + " Event = "+evt;
    }
}
