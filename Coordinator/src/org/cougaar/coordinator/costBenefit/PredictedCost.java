/*
 * PredictedCost.java
 *
 * Created on May 7, 2004, 11:48 AM
 *
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

package org.cougaar.coordinator.costBenefit;

/**
 *
 * @author  David Wells - OBJS
 * @version 
 */
public class PredictedCost {

    private double memoryCost = 0.0;
    private double cpuCost = 0.0;
    private double bandwidthCost = 0.0;
    private double timeCost = 0.0;

    /** Creates new PredictedCost */
    public PredictedCost() {
    }

    protected void incrementMemoryCost(double c) { memoryCost = memoryCost + c; }
    protected void incrementCPUCost(double c) { cpuCost = cpuCost + c; }
    protected void incrementBandwidthCost(double c) { bandwidthCost = bandwidthCost + c; }
    protected void incrementTimeCost(double c) { timeCost = timeCost + c; }

    public double getMemoryCost() { return memoryCost; }
    public double getCPUCost() { return cpuCost; }
    public double getBandwidthCost() { return bandwidthCost; }
    public long getTimeCost() { return Math.round(timeCost); }

    public String toString() {
        return "(CPU="+cpuCost+" Memory="+memoryCost+" Bandwidth="+bandwidthCost+" Time="+timeCost+")";
    }

}
