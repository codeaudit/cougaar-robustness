/*
 * TransitiveEffectDescription.java
 *
 * Created on May 6, 2004, 9:51 AM
 */

package org.cougaar.coordinator.techspec;

import org.cougaar.core.util.UID;


/**
 * This class describes the TransitiveEffects that might be caused by 
 * the occurrence of some event. The transitive effect is that it causes
 * another event to occur.
 *
 * @author  Administrator
 */
public class TransitiveEffectDescription {

    
    private String transitiveEventName = null; 
    private EventDescription transitiveEvent = null; 
    private AssetType transitiveAssetType = null; 
    private TransitiveEffectVulnerabilityFilter transitiveVulnerabilityFilter = null;     

    
    /** Creates a new instance of TransitiveEffect */
    public TransitiveEffectDescription(String eventName, AssetType assetType) {
        
        this.transitiveEventName = eventName;
        this.transitiveAssetType = assetType;
        
    }

    
    public void setTransitiveEvent(EventDescription event) { this.transitiveEvent = event; }
    public void setTransitiveEventName(String name) { this.transitiveEventName = name; }
    public void settransitiveAssetType(AssetType transType) { this.transitiveAssetType = transType; }
    public void setTransitiveVulnerabilityFilter(TransitiveEffectVulnerabilityFilter vf) { this.transitiveVulnerabilityFilter = vf; }

    /** Supports only ONE transitive effect in 2004 
     * @return the name of the event read in from XML that is transitively caused by this event
     */
    public String getTransitiveEventName() { return transitiveEventName; }

    /** Supports only ONE transitive effect in 2004 
     * @return the event that is transitively caused by this event
     */
    public EventDescription getTransitiveEvent() { return transitiveEvent; }
    
    /** Supports only ONE transitive effect in 2004 
     * @return the assetType affected by the event that is transitively caused by this event
     */
    public AssetType getTransitiveAssetType() { return transitiveAssetType; }

    /** Supports only ONE transitive effect in 2004 
     * @return the vulnerability filter that identifies the assets that are transitively affected by this event
     */
    public TransitiveEffectVulnerabilityFilter getTransitiveVulnerabilityFilter() { return transitiveVulnerabilityFilter; }
    
        
    
    /**
     * @return true if an asset qualifies -- if the threat's filter doesn't exclude the asset.
     * Will return <b>false</b> if the asset type of the asset and the getTransitiveAssetType() aren't equal.
     * If this is a TransitiveEffectDescription without a filter, it will return true if the asset type of the
     * asset equals the getTransitiveAssetType() of the threat.
     */
    public boolean qualifies(TransitiveEffectModelManagerPlugin mgr, AssetTechSpecInterface asset) {

        //If the asset type & affectAssetType don't equal, then this threat doesn't apply.
        if (! (asset.getAssetType().equals(this.getTransitiveAssetType())) ) {
            return false;
        }
        
        if (this.getTransitiveVulnerabilityFilter() == null) {
            return true;
        } else { // check filter
            return this.getTransitiveVulnerabilityFilter().qualifies(mgr, asset);
        }        
    }
    
    public String toString() {
     
        String s = "TransitiveEffect causes event=["+this.getTransitiveEventName()+"], affects asset type="+this.getTransitiveAssetType()+"\n";
        if (this.getTransitiveVulnerabilityFilter() != null) {
            s += "[VulnerabilityFilter = "+this.getTransitiveVulnerabilityFilter().toString()+"]\n";
        }        
        return s;
    }
    

    /** A ptr to an instantiation of this meta model */
    private TransitiveEffectModel instantiation = null;
    
    //EventModelManagerPlugin uses the following methods
    public TransitiveEffectModel getInstantiation() { return instantiation; }
    /** Create a new DefaultEventModel */
    public TransitiveEffectModel instantiate(UID uid) { 
        instantiation = new TransitiveEffectModel(this, uid);
        return instantiation; 
    }
    
}
