 /* <copyright>
 *  Copyright 2002,2003 Object Services and Consulting, Inc. (OBJS),
 *  under sponsorship of the Defense Advanced Research Projects Agency (DARPA).
 * 
 *  The code in this file is free software; you can redistribute it and/or modify
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
 * Feb 2002: Created. (OBJS)
 */

package com.objs.surveyor.probemeister.probe;


  public class GenericArgumentArray {

    Argument[] ar;
    int i;
    int size;

    public GenericArgumentArray(int _size) {        
        ar = new Argument[_size];
        i=0;
        size = _size;
        for (int j=0; j<size; j++) ar[j]=new Argument();
    }
    
    public int length() {return size;}
    
    public Object getValue(int index) { 
        if (index > -1 && index < size) 
            return ar[index].value;
        else return null;
    }

    public String getName(int index) { 
        if (index > -1 && index < size) 
            return ar[index].name;
        else return null;
    }

    public String getType(int index) { 
        if (index > -1 && index < size) 
            return ar[index].type;
        else return null;
    }
    
    /* auto increment index ptr */
    void incIndex() {if (i< (size-1)) i++; else i = 0;}

    public void set(Object value, String name) { ar[i].value=value; ar[i].name=name; incIndex();}

    public void set(Object value, String type, String name) { ar[i].value=value; ar[i].type=type; ar[i].name=name; incIndex();}

    public void set(boolean z, String name) { ar[i].value=new Boolean(z); ar[i].name=name; incIndex();}

    public void set(byte b, String name) { ar[i].value=new Byte(b); ar[i].name=name; incIndex();}

    public void set(char c, String name) { ar[i].value=new Character(c); ar[i].name=name; incIndex();}

    public void set(double d, String name) { ar[i].value=new Double(d); ar[i].name=name; incIndex();}

    public void set(float f, String name) { ar[i].value=new Float(f); ar[i].name=name; incIndex();}

    public void set(int x, String name) { ar[i].value=new Integer(x); ar[i].name=name; incIndex();}

    public void set(long l, String name) { ar[i].value=new Long(l); ar[i].name=name; incIndex();}

    public void set(short s, String name) { ar[i].value=new Short(s); ar[i].name=name; incIndex();}


  
     class Argument {        
           String name=null;
           String type=null;
           Object value=null;
    }
          
  }