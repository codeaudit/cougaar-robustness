/*
 * <copyright>
 *  Copyright 2004 Object Services and Consulting, Inc.
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

/*
 * MediatedTechSpecMap.java
 *
 * Created on October 8, 2004, 12:47 PM
 */

package org.cougaar.coordinator.costBenefit;

/**
 *
 * @author  David Wells - OBJS
 * @version 
 */

import java.util.Vector;
import org.cougaar.coordinator.techspec.AssetStateDimension;
import org.cougaar.coordinator.techspec.DiagnosisTechSpecService;

public class MediatedTechSpecMap {

    /** Creates new MediatedTechSpecMap */
    public MediatedTechSpecMap() {
    }

    public Vector getEmptyCompensationVector(AssetStateDimension compensatedStateDimension) {
        Vector initialProbVector = null;
        if (compensatedStateDimension.getStateName().equals("MediatedConnectionStatus")) {
            initialProbVector = new Vector(5);
            initialProbVector.add(new StateProb("Excellent", 0.0));
            initialProbVector.add(new StateProb("Good", 0.0));
            initialProbVector.add(new StateProb("Fair", 0.0));
            initialProbVector.add(new StateProb("Poor", 0.0));
            initialProbVector.add(new StateProb("None", 0.0));
        }
        return initialProbVector;
    }

    public AssetStateDimension getBaseDimension(String actionName, DiagnosisTechSpecService diagnosisTechSpecService) {
        if (actionName.equals("org.cougaar.mts.std.LinksEnablingAction")) 
            return diagnosisTechSpecService.getDiagnosisTechSpec("org.cougaar.mts.std.RMILinksStatusDiagnosis").getStateDimension();
        return null;
    }

    public AssetStateDimension getCompensatedDimension(String actionName, DiagnosisTechSpecService diagnosisTechSpecService) {
        if (actionName.equals("org.cougaar.mts.std.LinksEnablingAction")) 
            return diagnosisTechSpecService.getDiagnosisTechSpec("org.cougaar.mts.std.AllLinksStatusDiagnosis").getStateDimension();
        return null;
    }

    public Vector mapCompensation(String actionName, String baseStateName, String actionSettingName) {
        if (actionName.equals("org.cougaar.mts.std.LinksEnablingAction")) {
            Vector v = new Vector(5);
            if (baseStateName.equals("DirectPathExists") && actionSettingName.equals("Normal")) {
                v.add(new StateProb("Excellent", 0.8));
                v.add(new StateProb("Good", 0.1));
                v.add(new StateProb("Fair", 0.1));
                v.add(new StateProb("Poor", 0.0));
                v.add(new StateProb("None", 0.0));
                return v;
            }
            else if (baseStateName.equals("DirectPathExists") && actionSettingName.equals("AlternateDirect")) {
                v.add(new StateProb("Excellent", 0.7));
                v.add(new StateProb("Good", 0.1));
                v.add(new StateProb("Fair", 0.1));
                v.add(new StateProb("Poor", 0.1));
                v.add(new StateProb("None", 0.0));
                return v;
            }
            else if (baseStateName.equals("DirectPathExists") && actionSettingName.equals("StoreAndForward")) {
                v.add(new StateProb("Excellent", 0.1));
                v.add(new StateProb("Good", 0.6));
                v.add(new StateProb("Fair", 0.2));
                v.add(new StateProb("Poor", 0.1));
                v.add(new StateProb("None", 0.0));
                return v;
            }
            else if (baseStateName.equals("DirectPathExists") && actionSettingName.equals("Disable")) {
                v.add(new StateProb("Excellent", 0.0));
                v.add(new StateProb("Good", 0.0));
                v.add(new StateProb("Fair", 0.0));
                v.add(new StateProb("Poor", 0.0));
                v.add(new StateProb("None", 1.0));
                return v;
            }
            else if (baseStateName.equals("OnlyIndirectPathExists") && actionSettingName.equals("Normal")) {
                v.add(new StateProb("Excellent", 0.0));
                v.add(new StateProb("Good", 0.0));
                v.add(new StateProb("Fair", 0.0));
                v.add(new StateProb("Poor", 0.0));
                v.add(new StateProb("None", 1.0));
                return v;
            }
            else if (baseStateName.equals("OnlyIndirectPathExists") && actionSettingName.equals("AlternateDirect")) {
                v.add(new StateProb("Excellent", 0.0));
                v.add(new StateProb("Good", 0.0));
                v.add(new StateProb("Fair", 0.2));
                v.add(new StateProb("Poor", 0.3));
                v.add(new StateProb("None", 0.5));
                return v;
            }
            else if (baseStateName.equals("OnlyIndirectPathExists") && actionSettingName.equals("StoreAndForward")) {
                v.add(new StateProb("Excellent", 0.1));
                v.add(new StateProb("Good", 0.7));
                v.add(new StateProb("Fair", 0.1));
                v.add(new StateProb("Poor", 0.1));
                v.add(new StateProb("None", 0.0));
                return v;
            }
            else if (baseStateName.equals("OnlyIndirectPathExists") && actionSettingName.equals("Disable")) {
                v.add(new StateProb("Excellent", 0.0));
                v.add(new StateProb("Good", 0.0));
                v.add(new StateProb("Fair", 0.0));
                v.add(new StateProb("Poor", 0.0));
                v.add(new StateProb("None", 1.0));
                return v;
            }
            else if (baseStateName.equals("NoPathExists") && actionSettingName.equals("Normal")) {
                v.add(new StateProb("Excellent", 0.0));
                v.add(new StateProb("Good", 0.0));
                v.add(new StateProb("Fair", 0.0));
                v.add(new StateProb("Poor", 0.0));
                v.add(new StateProb("None", 1.0));
                return v;
            }
            else if (baseStateName.equals("NoPathExists") && actionSettingName.equals("AlternateDirect")) {
                v.add(new StateProb("Excellent", 0.0));
                v.add(new StateProb("Good", 0.0));
                v.add(new StateProb("Fair", 0.0));
                v.add(new StateProb("Poor", 0.0));
                v.add(new StateProb("None", 1.0));
                return v;
            }
            else if (baseStateName.equals("NoPathExists") && actionSettingName.equals("StoreAndForward")) {
                v.add(new StateProb("Excellent", 0.0));
                v.add(new StateProb("Good", 0.0));
                v.add(new StateProb("Fair", 0.0));
                v.add(new StateProb("Poor", 0.0));
                v.add(new StateProb("None", 1.0));
                return v;
            }
            else if (baseStateName.equals("NoPathExists") && actionSettingName.equals("Disable")) {
                v.add(new StateProb("Excellent", 0.0));
                v.add(new StateProb("Good", 0.0));
                v.add(new StateProb("Fair", 0.0));
                v.add(new StateProb("Poor", 0.0));
                v.add(new StateProb("None", 1.0));
                return v;
            }
            else return null;
        }
        if (actionName.equals("org.cougaar.core.security.coordinator.SecurityLevelAction")) {
        }
        return null;
    }

}
