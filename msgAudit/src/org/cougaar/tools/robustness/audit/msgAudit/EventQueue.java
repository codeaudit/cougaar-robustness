/*
 * EventQueue.java
 *
 * Code borrowed from Java Thread Programming book by Paul Hyde
 * Created on February 14, 2003, 4:32 PM
 */

package LogPointAnalyzer;

import org.cougaar.core.mts.logging.LogEvent;

/**
 *
 */
public class EventQueue {
    
    /** Creates a new instance of EventQueue */
    public EventQueue() {
    }

    private LogEvent[] queue;
    private int capacity;
    private int size;
    private int head;
    private int tail;

    public EventQueue(int cap) {
            capacity = ( cap > 0 ) ? cap : 1; // at least 1
            queue = new LogEvent[capacity];
            head = 0;
            tail = 0;
            size = 0;
    }

    public int getCapacity() {
            return capacity;
    }

    public synchronized int getSize() {
            return size;
    }

    public synchronized boolean isEmpty() {
            return ( size == 0 );
    }

    public synchronized boolean isFull() {
            return ( size == capacity );
    }

    public synchronized void add(LogEvent obj) 
                    throws InterruptedException {

            waitWhileFull();

            queue[head] = obj;
            head = ( head + 1 ) % capacity;
            size++;

            notifyAll(); // let any waiting threads know about change
    }

    public synchronized void addEach(LogEvent[] list) 
                    throws InterruptedException {

            //
            // You might want to code a more efficient 
            // implementation here ... (see ByteFIFO.java)
            // (e.g. add waits in between calls to add())
           
            for ( int i = 0; i < list.length; i++ ) {
                    add(list[i]);
            }
    }

    public synchronized LogEvent remove() 
                    throws InterruptedException {

            waitWhileEmpty();

            LogEvent obj = queue[tail];

            // don't block GC by keeping unnecessary reference
            queue[tail] = null; 

            tail = ( tail + 1 ) % capacity;
            size--;

            notifyAll(); // let any waiting threads know about change

            return obj;
    }

    public synchronized LogEvent[] removeAll() 
                    throws InterruptedException {

            //
            // You might want to code a more efficient 
            // implementation here ... (see ByteFIFO.java)
            //

            LogEvent[] list = new LogEvent[size]; // use the current size

            for ( int i = 0; i < list.length; i++ ) {
                    list[i] = remove();
            }

            // if FIFO was empty, a zero-length array is returned
            return list; 
    }

    public synchronized LogEvent[] removeAtLeastOne() 
                    throws InterruptedException {

            waitWhileEmpty(); // wait for a least one to be in FIFO
            return removeAll();
    }

    public synchronized boolean waitUntilEmpty(long msTimeout) 
                    throws InterruptedException {

            if ( msTimeout == 0L ) {
                    waitUntilEmpty();  // use other method
                    return true;
            }

            // wait only for the specified amount of time
            long endTime = System.currentTimeMillis() + msTimeout;
            long msRemaining = msTimeout;

            while ( !isEmpty() && ( msRemaining > 0L ) ) {
                    wait(msRemaining);
                    msRemaining = endTime - System.currentTimeMillis();
            }

            // May have timed out, or may have met condition, 
            // calc return value.
            return isEmpty();
    }

    public synchronized void waitUntilEmpty() 
                    throws InterruptedException {

            while ( !isEmpty() ) {
                    wait();
            }
    }

    public synchronized void waitWhileEmpty() 
                    throws InterruptedException {

            while ( isEmpty() ) {
                    wait();
            }
    }

    public synchronized void waitUntilFull() 
                    throws InterruptedException {

            while ( !isFull() ) {
                    wait();
            }
    }

    public synchronized void waitWhileFull() 
                    throws InterruptedException {

            while ( isFull() ) {
                    wait();
            }
    }
    
}
