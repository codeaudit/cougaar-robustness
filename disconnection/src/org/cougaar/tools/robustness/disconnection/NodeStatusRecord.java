/*
 * ActiveDisconnect.java
 *
 * Created on July 27, 2004, 1:08 PM
 * * 
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

package org.cougaar.tools.robustness.disconnection;

/**
 *
 * @author  David Wells
 * @version 
 */

import org.cougaar.coordinator.techspec.AssetID;
import java.io.Serializable;

public class NodeStatusRecord implements Serializable {

    AssetID nodeID;
    AgentVector agents;
    double reconnectTime;
    String diagnosis;

    /** Creates new ActiveDisconnect */
    public NodeStatusRecord(AssetID nodeID, AgentVector agents, double reconnectTime, String diagnosis) {
        this.nodeID = nodeID;
        this.agents = agents;
        this.reconnectTime = reconnectTime;
        this.diagnosis = diagnosis;
    }

    public AssetID getNodeID() { return nodeID; }
    public AgentVector getAgents() { return agents; }
    public double getReconnectTime() { return reconnectTime; }
    public void setReconnectTime(double reconnectTime) { this.reconnectTime = reconnectTime; }
    public String getDiagnosis() { return diagnosis; }
    public void setDiagnosis(String diagnosis) { this.diagnosis = diagnosis; }

    public String toString() {
        String buff = "NodeID: " + getNodeID() + "\n";
        buff = buff + "Agents: " + getAgents().toString() + "\n";
        buff = buff + "Diagnosis: " + getDiagnosis() + "\n";
        buff = buff + "ReconnectTime: " + getReconnectTime();
        return buff;
    }

}
