/*
 * PredictedCost.java
 *
 * Created on May 7, 2004, 11:48 AM
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
