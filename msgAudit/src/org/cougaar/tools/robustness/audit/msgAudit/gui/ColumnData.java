/*
 * ColumnData.java
 *
 * Created on February 17, 2003, 12:41 PM
 */

package LogPointAnalyzer.gui;

/**
 *
 * @author  Administrator
 */
public class ColumnData {
    
    /** Creates a new instance of ColumnData */
    public String title;
    public int width;
    public int align;
    public ColumnData(String _t, int _w, int _a) {
        title = _t;
        width = _w;
        align = _a;
    }
    
}
