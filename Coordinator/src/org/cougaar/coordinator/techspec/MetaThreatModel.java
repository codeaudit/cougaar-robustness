/*
 * MetaThreatModel.java
 *
 * Created on September 17, 2003, 12:24 PM
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
import org.cougaar.core.util.UID;

/**
 *
 * @author  Paul Pazandak, Ph.D, OBJS
 */
public class MetaThreatModel implements NotPersistable {
    

        /** the name of this treat */
        private String name = "NoNameMetaThreatModel";

        /** The ThreatType of this threat */
        private ThreatType threatType = null;

        /** All of the DamageDistributions */
        private DamageDistribution distribution[] = new DamageDistribution[0];

        /** ThreatMembershipFilter filters used to determine membership */
        private ThreatMembershipFilter filters[] = new ThreatMembershipFilter[0];

        /** Static properties used to filter membership of asset list */
        private AssetType assetType = null;
        
        /** The likelihood of the threat occurring */
        private ThreatLikelihoodInterval threatLikelihoods[] = new ThreatLikelihoodInterval[0];
        
        /** A ptr to an instantiation of this meta model */
        private DefaultThreatModel instantiation = null;
        
        public MetaThreatModel() { }
        public MetaThreatModel(String name, ThreatType threatType, DamageDistribution[] distribution, ThreatMembershipFilter[] filters, AssetType assetType, ThreatLikelihoodInterval[] threatLikelihoods) {
            
            this.name = name;
            this.threatType = threatType;        
            this.assetType = assetType;
            this.distribution = distribution;
            this.filters = filters;
            this.threatLikelihoods = threatLikelihoods;
        }
        
        public void setThreatName(String name) { this.name = name; }
        public void setThreatType(String threat) { this.threatType = new ThreatType(threat); }
        public void setAssetType(AssetType type) { this.assetType = type; }
        public String getThreatName( ) { return this.name; }
        public ThreatType getThreatType( ) { return this.threatType; }
        public AssetType getAssetType( ) { return this.assetType; }
        
        public void addDistribution(DamageDistribution dist) { 
            DamageDistribution[] t = new DamageDistribution[distribution.length+1];
            for (int i=0; i<distribution.length; i++) {
                t[i] = distribution[i];
            }
            t[t.length-1] = dist;
            distribution = t;
        }
        public DamageDistribution[] getDistributions() { return distribution; }
        
        public void addFilter(ThreatMembershipFilter filter) { 
            ThreatMembershipFilter[] t = new ThreatMembershipFilter[filters.length+1];
            for (int i=0; i<filters.length; i++) {
                t[i] = filters[i];
            }
            t[t.length-1] = filter;  
            filters = t;
        }
        public ThreatMembershipFilter[] getFilters() { return filters; }

        public void addLikelihood(ThreatLikelihoodInterval threatLikelihood) { 
            ThreatLikelihoodInterval[] t = new ThreatLikelihoodInterval[threatLikelihoods.length+1];
            for (int i=0; i<threatLikelihoods.length; i++) {
                t[i] = threatLikelihoods[i];
            }
            t[t.length-1] = threatLikelihood;  
            threatLikelihoods = t;
        }
        public ThreatLikelihoodInterval[] getLikelihood() { return threatLikelihoods; }
        
        public String toString() {
         
            try {
                String s = "ThreatName="+name+"  --- threatType="+threatType.getName()+"   assetType="+assetType.getName();
                for (int i=0; i<distribution.length; i++) { s = s+"\nDistribution: "+distribution[i].toString(); }
                for (int i=0; i<filters.length; i++) { s = s+"\nFilter: "+filters[i].toString(); }
                for (int j=0; j<threatLikelihoods.length; j++) { s = s+"\nLikelihood: "+threatLikelihoods[j].toString(); }
            
            return s;
            } catch (Exception e) {
                e.printStackTrace();
                return e.toString();
            }
        }        
        
        public DefaultThreatModel getInstantiation() { return instantiation; }


        
        public DefaultThreatModel instantiate(UID uid) { 
            
            instantiation = new DefaultThreatModel(this.name, this.threatType, this.assetType, uid, distribution, filters, threatLikelihoods);
            return instantiation; 
        
        }
        
}
