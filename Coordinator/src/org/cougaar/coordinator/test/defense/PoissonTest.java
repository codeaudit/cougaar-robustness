/*
 * PoissonTest.java
 *
 * Created on September 24, 2003, 9:21 AM
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
