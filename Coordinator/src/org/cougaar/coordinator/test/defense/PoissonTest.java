/*
 * PoissonTest.java
 *
 * Created on September 24, 2003, 9:21 AM
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

//org/cougaar/tools/robustness/deconfliction/test/defense/PoissonTest
package org.cougaar.coordinator.test.defense;

import JSci.maths.statistics.PoissonDistribution;
/**
 *
 * @author  Administrator
 */
public class PoissonTest {
    
    /** Creates a new instance of PoissonTest */
    public PoissonTest() {
    }
    
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        
        try { 
        Double ii = Double.valueOf(args[0]);
//        System.out.println(" lamda = "+ii);
        //System.out.println(" i = "+i);
        double dd = ii.doubleValue();
        System.out.println(" lambda = "+dd);
        PoissonDistribution pd = new PoissonDistribution(dd);
        
        System.out.println(" P("+args[1]+") = "+pd.probability(Integer.valueOf(args[1]).intValue()));
        } catch (Exception e) { System.out.println(" Exception was : "+e); e.printStackTrace();}
    }
    
}
