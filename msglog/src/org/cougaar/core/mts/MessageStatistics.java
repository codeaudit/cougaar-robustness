/*
 * <copyright>
 *  Copyright 1997-2001 BBNT Solutions, LLC
 *  under sponsorship of the Defense Advanced Research Projects Agency (DARPA).
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

package org.cougaar.core.mts;

/**
 * Abstract MessageStatistics layer for Society interaction.
 * Used for Scalability testing.
 **/

public interface MessageStatistics {
  int[] BIN_SIZES = {
    100,
    200,
    500,
    1000,
    2000,
    5000,
    10000,
    20000,
    50000,
    100000,
    1000000,
    10000000,
    100000000
  };

  int NBINS = BIN_SIZES.length;

  class Statistics {
    public double averageMessageQueueLength;
    public long totalMessageBytes;
    public long totalMessageCount;
    public long[] histogram = new long[NBINS];
    public Statistics(double amql, long tmb, long tmc, long[] h) {
      averageMessageQueueLength = amql;
      totalMessageBytes = tmb;
      totalMessageCount = tmc;
      if (h != null) {
        System.arraycopy(h, 0, histogram, 0, NBINS);
      }
    }
  }
      
  Statistics getMessageStatistics(boolean reset);
}
