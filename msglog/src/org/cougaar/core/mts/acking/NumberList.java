/*
 * <copyright>
 *  Copyright 2001-2003 Object Services and Consulting, Inc. (OBJS),
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
 *
 * CHANGE RECORD
 * 14 Mar 2003: Comment out tests - style police object to System.out 
 * 23 Apr 2002: Split out from MessageAckingAspect. (OBJS)
 */

package org.cougaar.core.mts.acking;

import java.util.Arrays;

public class NumberList implements java.io.Serializable
{
  private int list[];
  private int n;

  public NumberList ()
  {
    this (2);
  }

  public NumberList (int initialSize)
  {
    initialize (initialSize);
  }

  public NumberList (int list[])
  {
    if (list != null) initialize (list);
    else initialize (2);
  }

  public NumberList (NumberList list)
  {
    if (list != null) initialize (list.list, list.n);
    else initialize (2);
  }

  private void initialize (int initialSize)
  {
    if (initialSize < 2) initialSize = 2;
    list = new int[initialSize];
    n = 0;
  }

  public synchronized void initialize (int list[])
  {
    int len = (list != null ? list.length : 0);
    initialize (list, len);
  }

  public synchronized void initialize (int list[], int n)
  {
    checkListValidity (list, n);

    //  Validity cannot be maintained if we do not copy 
    //  as opposed to reference.

    int copy[] = new int[n];
    System.arraycopy (list, 0, copy, 0, n);

    this.list = copy;
    this.n = n;
  }

  public static void checkListValidity (NumberList list)
  {
    if (list == null) return;
    checkListValidity (list.list, list.n);
  }

  public static void checkListValidity (int list[])
  {
    int len = (list != null ? list.length : 0);
    checkListValidity (list, len);
  }

  public static void checkListValidity (int list[], int n)
  {
    if (list == null)
    {
      throw new IllegalArgumentException ("Illegal null list!");
    }

    if (n < 0 || n > list.length || n%2 != 0)
    {
      throw new IllegalArgumentException ("Invalid list length!");
    }

    int last1 = Integer.MIN_VALUE;

    for (int i=0; i<n; i+=2)
    {
      if (last1+1 >= list[i] || list[i] > list[i+1])
      {
        throw new IllegalArgumentException ("Invalid list!");
      }

      last1 = list[i+1];
    }
  }

  public int size ()  // synchronization not needed
  {
    return n;  
  }

  public boolean isEmpty ()  // synchronization not needed
  {
    return n == 0;  
  }

  public synchronized boolean find (int number)
  {
    for (int i=0; i<n; i+=2)
    {
      if (list[i] <= number && number <= list[i+1])
      {
        return true;
      }
    }

    return false;
  }

  public synchronized boolean add (int number)
  {
    int i;

    //  Adding a number to the list can increase its size by 2.
    //  It can also shrink it by 2, or not change the size.
    //  For simplicity, we'll always expand the list up front
    //  if it cannot fit 2 more numbers.

    if (list.length - n < 2)
    {
      int newlist[] = new int[list.length*2];
      System.arraycopy (list, 0, newlist, 0, n);
      list = newlist;
    }

    //  Go through the list and try to add the number

    for (i=0; i<n; i+=2)
    {
      if (number > list[i+1]+1) continue;  // go further down the list

      //  Return if we already cover the number

      if (list[i] <= number && number <= list[i+1])
      {
        return false;  // didn't need to add it
      }

      //  Try different ways the number could fit in

      if (list[i]-1 > number)  // insert here
      {
        int next0 = list[i];
        int next1 = list[i+1];

        list[i] = number;
        list[i+1] = number;
        n += 2;

        for (i=i+2; i<n; i+=2)
        {
          int sav0 = list[i];
          int sav1 = list[i+1];
          list[i] =   next0;
          list[i+1] = next1;
          next0 = sav0;
          next1 = sav1;          
        }

        return true;
      }      
      else if (list[i]-1 == number)  // extend lower bound
      {
        //  Simple extension

        list[i] = number;
        return true;
      }
      else if (list[i+1]+1 == number)  // extend upper bound
      {
        if (i+2 < n && list[i+2]-1 == number)
        {
          //  Merger

          list[i+1] = list[i+3];
          n -= 2;
          for (i=i+2; i<n; i++) list[i] = list[i+2];
          return true;
        }
        else
        {
          //  Simple extension

          list[i+1] = number;
          return true;
        }
      }
    }

    //  If we get to this point, that means the number needs to be
    //  appended to the end of the list.

    list[i] = number;
    list[i+1] = number;
    n += 2;
    return true;
  }

  public synchronized boolean remove (int number)
  {
    int i;

    //  Removing a number from the list can increase its size by 2.
    //  It can also shrink it by 2, or not change the size.
    //  For simplicity, we'll always expand the list up front
    //  if it cannot fit 2 more numbers.

    if (list.length - n < 2)
    {
      int newlist[] = new int[list.length*2];
      System.arraycopy (list, 0, newlist, 0, n);
      list = newlist;
    }
    
    //  Go through the list and try to remove the number

    for (i=0; i<n; i+=2)
    {
      if (list[i] > number) break;  // number not here

      if (list[i] == number)
      {
        if (list[i+1] == number)
        {
          //  Remove instance

          n -= 2;
          for ( ; i<n; i++) list[i] = list[i+2];
          return true;
        }
        else
        {
          //  Reduce endpoint

          list[i] += 1;
          return true; 
        }
      }
      else if (list[i+1] == number)
      {
        //  Reduce endpoint

        list[i+1] -= 1;
        return true;
      }
      else if (list[i] < number && number < list[i+1])
      {
        //  Split entry

        n += 2;

        int tmp = list[i+1];
        list[i+1] = number - 1;
        int next0 = list[i+2];
        int next1 = list[i+3];
        list[i+2] = number + 1;
        list[i+3] = tmp;

        for (i=i+4; i<n; i+=2)
        {
          int sav0 = list[i];
          int sav1 = list[i+1];
          list[i] =   next0;
          list[i+1] = next1;
          next0 = sav0;
          next1 = sav1;
        }

        return true;
      }
    }

    //  If we get to this point, it means the number was not
    //  in the list.

    return false;
  }

  public synchronized void remove (NumberList numbers)
  {
    if (numbers == null) return;

    //  Special case: We are deleting ourselves from ourselves.
    //  This is just not for efficiency, but remove methods like
    //  the one here can mess up operating on itself.

    if (this == numbers)
    {
      Arrays.fill (list, 0);
      n = 0;
      return;
    }

    //  This is a HACK - need to write a more efficient version

    for (int i=0; i<numbers.n; i+=2)
    {
      if (numbers.list[i] != numbers.list[i+1])
      {
        for (int j=numbers.list[i]; j<=numbers.list[i+1]; j++) remove (j);
      }
      else remove (numbers.list[i]);
    }
  }

  public void dump (String tag)
  {
    System.out.println (tag+ " NumberList: n=" +n+ " length=" +list.length);

    for (int i=0; i<list.length; i++) 
    {
      System.out.println (tag+ " NumberList: list[" +i+ "] =" +list[i]);
    }
  }

/* //102B Comment out tests - style police object to System.out
    
  public boolean debugAdd (int number)
  {
    System.out.println ("\nBfore add: " +this+ " n=" +n);
    System.out.println ("Adding " + number);
    boolean b = add (number);
    System.out.println ("After add: " +this+ " n=" +n+ " returns:" + b);
    return b;
  }

  public boolean debugRemove (int number)
  {
    System.out.println ("\nBfore remove: " +this+ " n=" +n);
    System.out.println ("Removing " + number);
    boolean b = remove (number);
    System.out.println ("After remove: " +this+ " n=" +n+ " returns:" + b);
    return b;
  }

  public void debugRemove (NumberList numbers)
  {
    System.out.println ("\nBfore remove: " +this+ " n=" +n);
    System.out.println ("Removing " + numbers);
    remove (numbers);
    System.out.println ("After remove: " +this+ " n=" +n);
  }
*/ //102B

  public synchronized int[] toArray ()
  {
    int copy[] = new int[n];
    System.arraycopy (list, 0, copy, 0, n);
    return copy;
  }

  public synchronized String toString ()
  {
    if (n == 0) return "<empty>";

    StringBuffer buf = new StringBuffer();
    
    for (int i=0; i<n; i+=2)
    {
      buf.append ("" + list[i]);
      if (list[i+1] != list[i]) buf.append ("-" + list[i+1]);
      buf.append (" ");
    }

    return buf.toString();
  }

/* //102B Comment out tests - style police object to System.out
  public static void main (String args[])
  {
    //  Test cases

    NumberList nl = new NumberList();

    nl.debugAdd (0);
    nl.debugAdd (1);
    nl.debugAdd (3);
    nl.debugAdd (4);
    nl.debugAdd (6);
    nl.debugAdd (9);
    nl.debugAdd (5);
    nl.debugAdd (10);
    nl.debugAdd (3);

    nl.initialize (new int[] { 0,4, 8,12 });
    nl.debugAdd (7);

    nl.initialize (new int[] { 0,4, 8,12 });
    nl.debugAdd (20);
    nl.debugAdd (22);

    nl.initialize (new int[] { 0,11, 13,13, 15,20, 25,25 });
    nl.debugAdd (14);
    nl.debugAdd (-1);
    nl.debugAdd (-3);
    nl.debugAdd (-2);
    nl.debugAdd (12);
    System.out.println ("nl.find (-2)=" +nl.find(-2));

    nl.initialize (new int[] { 5,10, 12,13, 15,15 });
    nl.debugRemove (7);
    nl.debugRemove (13);
    nl.debugRemove (5);
    nl.debugRemove (12);
    nl.debugRemove (9);

    nl.initialize (new int[] { 2,6 });
    nl.debugRemove (7);

    int x[] = new int[] { 1,1, 3,5 };
    nl.initialize (x);
    nl.debugRemove (nl);

    int a[] = new int[] { 2,5, 8,9 };
    int b[] = new int[] { 1,1, 3,5, 9,11 };
    nl.initialize (a);
    NumberList nl2 = new NumberList (b);
    nl.debugRemove (nl2);

    // Bad lists
    // nl.initialize (new int[] { 1 });
    // nl.initialize (new int[] { 2,1 });
    // nl.initialize (new int[] { 1,3, 2,4 });
    // nl.initialize (new int[] { 1,3, 4,5 });
  }
*/ //102B

}
