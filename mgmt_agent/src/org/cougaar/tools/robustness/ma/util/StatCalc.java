/*
 * <copyright>
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
package org.cougaar.tools.robustness.ma.util;
/*
   An object of class StatCalc can be used to compute several simple statistics
   for a set of numbers.  Numbers are entered into the dataset using
   the enter(double) method.  Methods are provided to return the following
   statistics for the set of numbers that have been entered: The number
   of items, the sum of the items, the average, and the standard deviation.
*/

public class StatCalc implements Cloneable {

   private String community;
   private String node;
   private int samples;   // Number of numbers that have been entered.
   private double sum;  // The sum of all the items that have been entered.
   private double sumSquares;  // The sum of the squares of all the items.
   private double high = Double.NaN;
   private double low = Double.NaN;

   public StatCalc() {}

   public StatCalc(String community, String node) {
     this.community = community;
     this.node = node;
   }

   public StatCalc(String community,
                   String node,
                   int count,
                   double sum,
                   double sumSquares,
                   double high,
                   double low) {
     this.community = community;
     this.node = node;
     this.samples = samples;
     this.sum = sum;
     this.sumSquares = sumSquares;
     this.high = high;
     this.low = low;
   }

   public void enter(double num) {
         // Add the number to the dataset.
      if (samples == 0 || num > high) high = num;
      if (samples == 0 || num < low) low = num;
      samples++;
      sum += num;
      sumSquares += num*num;
   }

   public String getCommunityName() {
      return community;
   }

   public String getNodeName() {
      return node;
   }

   public int getCount() {
         // Return number of items that have been entered.
      return samples;
   }

   public double getSum() {
         // Return the sum of all the items that have been entered.
      return sum;
   }

   public double getHigh() {
     return high;
   }

   public double getLow() {
     return low;
   }

   public double getMean() {
         // Return average of all the items that have been entered.
         // Value is Double.NaN if count == 0.
      return sum / samples;
   }

   public double getStandardDeviation() {
        // Return standard deviation of all the items that have been entered.
        // Value will be Double.NaN if count == 0.
      double mean = getMean();
      return Math.sqrt( sumSquares/samples - mean*mean );
   }

   public Object clone() {
     try {
       return super.clone();
     } catch (CloneNotSupportedException e) {
       e.printStackTrace();
       return null;
     }
   }

   public String toString() {
     return "samples=" + samples + " sum=" + sum + " low=" + low + " high=" + high +
         " mean=" + getMean() + " stdDev=" + getStandardDeviation();
   }

   public String toXML() {
     return "<Item community=\"" + community + "\" " +
                  "node=\"" + node + "\" " +
                  "samples=\"" + samples + "\" " +
                  "sum=\"" + sum + "\" " +
                  "sumSquares=\"" + sumSquares + " \" " +
                  "low=\"" + low + " \" " +
                  "high=\"" + high + "\" " +
                  "mean=\"" + getMean() + "\" " +
                  "stdev=\"" + getStandardDeviation() + "\" " +
                  "/>";
   }

}  // end of class StatCalc
