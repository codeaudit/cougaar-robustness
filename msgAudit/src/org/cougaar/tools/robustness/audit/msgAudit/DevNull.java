/*
 * DevNull.java
 *
 * Created on December 31, 2002, 10:07 AM
 */

package LogPointAnalyzer;
import java.io.OutputStream;
/**
 *
 * @author  Administrator
 */
public class DevNull extends OutputStream {
    
    /** Creates a new instance of DevNull */
    public DevNull() {}

    public void close() {}
    public void flush() {}
    public void write(byte[] b) {}
    public void write(byte[] b, int off, int len) {}
    public void write(int b) throws java.io.IOException {}
    
}

