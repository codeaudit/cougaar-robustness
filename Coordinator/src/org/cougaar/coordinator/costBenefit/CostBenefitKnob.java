/*   
 * CostBenefitKnob.java
 *
 * Created on July 8, 2003, 4:13 PM
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


package org.cougaar.coordinator.costBenefit;

import org.cougaar.util.UnaryPredicate;
import org.cougaar.core.persist.NotPersistable;
import org.cougaar.util.log.Logger;
import org.cougaar.util.log.Logging;

/**
 *  Used to control aspects of the cost benefit plugin
 * 
 */
public class CostBenefitKnob implements NotPersistable {
    
    long horizon = 1000L * 60L * 15L; // 15 minutes
    double completenessWeight = 0.6;
    double securityWeight = 0.4;
    double timelinessWeight = 0.0;
    
    /** Creates a new instance of CostBenefitKnob */
    public CostBenefitKnob() { }
        
    public void setHorizon(long horizon) { this.horizon = horizon; }   
    public long getHorizon() { return horizon; }

    public void setWeights(double securityWeight, double completenessWeight, double timelinessWeight) throws BadWeightsException {
        double totalWeight = completenessWeight + securityWeight + timelinessWeight;
        if ((totalWeight > 1.000000001) || (totalWeight < 0.999999999))
            throw new BadWeightsException(securityWeight, completenessWeight, timelinessWeight);
        else {
            this.completenessWeight = completenessWeight;
            this.securityWeight = securityWeight;
            this.timelinessWeight = timelinessWeight;
	}
	Logger logger = Logging.getLogger(getClass());
        if (logger.isDebugEnabled()) logger.debug("MAU weights changed to: " 
						  + "Security=" + this.securityWeight 
						  + ", Completeness=" + this.completenessWeight
						  + ", Timeliness=" + this.timelinessWeight);
    }

    protected double getCompletenessWeight() { return completenessWeight; }
    protected double getSecurityWeight() { return securityWeight; }
    protected double getTimelinessWeight() { return timelinessWeight; }

    public final static UnaryPredicate pred = new UnaryPredicate() {
        public boolean execute(Object o) {  
            return 
                (o instanceof CostBenefitKnob);
        }
    };

}
