/*
 * AgentMessageList.java
 *
 * Created on February 17, 2003, 10:32 AM
 */

package org.cougaar.tools.robustness.audit.msgAudit.gui;

import org.cougaar.tools.robustness.audit.msgAudit.*;
import org.cougaar.tools.robustness.audit.msgAudit.event.*;

import javax.swing.UIManager;
import java.awt.Component;
import javax.swing.JCheckBox;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.event.TableModelEvent;
import javax.swing.table.*;
import javax.swing.JLabel;

import java.util.Vector;

/**
 *
 * @author  Administrator
 */
public class AgentMessageList extends javax.swing.JFrame implements EventListener {
    
    AgentData agent = null;
    MessageTableModel tableModel = null;
    
    /** Creates new form AgentMessageList */
    public AgentMessageList() {
        initComponents();
        setSize(600,480);      
    }

    /** Creates new form AgentMessageList to display all of the agent's messages */
    public AgentMessageList(AgentData _agent) {
        initComponents();
        
        messageTable.setAutoCreateColumnsFromModel(false);
        messageTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        tableModel = new MessageTableModel(this);
        messageTable.setModel(tableModel);
        
        setSize(780,480);      
        agentName.setText(_agent.toString());

        setHeaders();
        initSelectionModel();
        agent = _agent;
        
        //Register for events
        EventHandler.handler().addListener(this);
        
    }
    
    AgentData getAgent() { return agent; }
    
    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    private void initComponents() {//GEN-BEGIN:initComponents
        buttonGroup1 = new javax.swing.ButtonGroup();
        lTitle = new javax.swing.JLabel();
        messageScrollPane = new javax.swing.JScrollPane();
        messageTable = new javax.swing.JTable();
        rbShowAll = new javax.swing.JRadioButton();
        rbShowIncomplete = new javax.swing.JRadioButton();
        bOK = new javax.swing.JButton();
        agentName = new javax.swing.JLabel();
        bRefresh = new javax.swing.JButton();

        getContentPane().setLayout(null);

        setName("frame");
        setResizable(false);
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent evt) {
                exitForm(evt);
            }
        });

        lTitle.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        lTitle.setText("Agent Message List");
        getContentPane().add(lTitle);
        lTitle.setBounds(330, 10, 112, 16);

        messageTable.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {

            }
        ));
        messageScrollPane.setViewportView(messageTable);

        getContentPane().add(messageScrollPane);
        messageScrollPane.setBounds(30, 80, 710, 330);

        rbShowAll.setText("Show All");
        buttonGroup1.add(rbShowAll);
        getContentPane().add(rbShowAll);
        rbShowAll.setBounds(520, 50, 74, 24);

        rbShowIncomplete.setText("Show Incomplete");
        buttonGroup1.add(rbShowIncomplete);
        getContentPane().add(rbShowIncomplete);
        rbShowIncomplete.setBounds(610, 50, 123, 24);

        bOK.setText("OK");
        bOK.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                bOKActionPerformed(evt);
            }
        });

        getContentPane().add(bOK);
        bOK.setBounds(320, 420, 51, 26);

        agentName.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        agentName.setText("jLabel1");
        getContentPane().add(agentName);
        agentName.setBounds(220, 30, 330, 16);

        bRefresh.setText("Refresh");
        bRefresh.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                bRefreshActionPerformed(evt);
            }
        });

        getContentPane().add(bRefresh);
        bRefresh.setBounds(410, 420, 79, 26);

        pack();
    }//GEN-END:initComponents

    private void bOKActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_bOKActionPerformed
        this.hide();
    }//GEN-LAST:event_bOKActionPerformed

    private void bRefreshActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_bRefreshActionPerformed
        updateData();
    }//GEN-LAST:event_bRefreshActionPerformed

    
    
    /** Exit the Application */
    private void exitForm(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_exitForm
    //    System.exit(0);
    }//GEN-LAST:event_exitForm
    
    
    void setHeaders() {

        DefaultTableCellRenderer labelRenderer = new DefaultTableCellRenderer();
        labelRenderer.setHorizontalAlignment(JLabel.CENTER);
        //DefaultTableModel dtm = (DefaultTableModel)messageTable.getModel();

        for (int k = 0; k < tableModel.getColumnCount(); k++) {
            TableCellRenderer renderer = null;
            TableCellEditor editor = null;
            switch (k) {
            case MessageTableModel.COL_DEST:
                    renderer = labelRenderer;
                    break;

            case MessageTableModel.COL_SEND_VEC:
                    renderer = labelRenderer;
                    break;

            case MessageTableModel.COL_RECV_VEC:
                    renderer = labelRenderer;
                    break;

            case MessageTableModel.COL_ARRIVED:
                    renderer = new CheckCellRenderer();
                    JCheckBox chBox = new JCheckBox();
                    chBox.setHorizontalAlignment(JCheckBox.CENTER);
                    chBox.setBackground(messageTable.getBackground());
                    chBox.setEnabled(false);
                    break;

            }
            if (renderer instanceof JLabel)
                    ((JLabel)renderer).setHorizontalAlignment(
                            MessageTableModel.m_columns[k].align);
            //if (editor instanceof DefaultCellEditor)
            //        ((DefaultCellEditor)editor).setClickCountToStart(2);

            TableColumn column = new TableColumn(k,
                    MessageTableModel.m_columns[k].width,
                    renderer, editor);
            messageTable.addColumn(column);
        }

        //JTableHeader header = messageTable.getTableHeader();
        //header.setUpdateTableInRealTime(false);
        
        
    }
    
    void initSelectionModel() {
        
        messageTable.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2) {
                    java.awt.Point origin  = e.getPoint();
                    int row = messageTable.rowAtPoint(origin);
                    int col = messageTable.columnAtPoint(origin);
                    if (row == -1 && col != -1) { // no cell clicked
                        return;
                    } else {
                        rowSelected(row, col);
                    }
                }
            }
        });
    }
    
    /*
     * Display agent information when a user double-clicks on a row
     *
     */
    void rowSelected(int _row, int _col) {

        Message md = (Message)tableModel.getValueAt(_row);
        if (md == null) return;
        //If dest selected, show dest agent message list
        if (_col == 0) {
            AgentData dest = md.destAgent();
            if (dest != null) {
                dest.showMsgList();
            } else {
                java.awt.Toolkit.getDefaultToolkit().beep();
            }
        } else { // show message detail
            md.showDetails();
        }
    }
    

    /*
     * Called to add one agent
     *
     */
    public void addMessage(Message _msg) {
     
        int row = tableModel.addRow(_msg);
        messageTable.tableChanged(new TableModelEvent(
            tableModel, row, row, TableModelEvent.ALL_COLUMNS,
            TableModelEvent.INSERT));
        //messageTable.setRowSelectionInterval(row,row);
        messageTable.repaint();
   }


    public void updateData() {

        tableModel.fireTableDataChanged();

/*        Vector data = tableModel.getDataVector();
        Vector msgs = agent.getMsgs();
        
        //if (data.size() < msgs.size()) {
        //    data.setSize( msgs.size() );
        //}
        
        Message msg = null;
        
        //Update the values
        for (int i=0; i<data.size(); i++) {
         
            msg = (Message)msgs.elementAt(i);
            Vector row = (Vector)data.elementAt(i);
            if (row == null && msg != null) {
                row = new Vector();
                row.addElement(msg.dest());
                row.addElement(msg.sendVector());
                row.addElement(msg.recvVector());
                row.addElement(new Boolean(msg.arrived()));
                System.out.println("2 row Vector Size was: " + row.size());
                data.setElementAt(row, i);
            } else {
                if (msg != null) {
 
            row.setElementAt(msg.dest(), 0);
            row.setElementAt(msg.sendVector(), 1);
            row.setElementAt(msg.recvVector(), 2);
            row.setElementAt(new Boolean(msg.arrived()), 3);
//                }
//            }
        }
*/        
    }
    
    
    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        new AgentMessageList().show();
    }
    
    /* Handle new Final Point event */
    public void newFinalPointEvent(LogPointLevel _lpl) {
        updateData();
        messageTable.repaint();
    }    
    
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JTable messageTable;
    private javax.swing.JScrollPane messageScrollPane;
    private javax.swing.JLabel agentName;
    private javax.swing.JButton bRefresh;
    private javax.swing.JRadioButton rbShowIncomplete;
    private javax.swing.JRadioButton rbShowAll;
    private javax.swing.ButtonGroup buttonGroup1;
    private javax.swing.JLabel lTitle;
    private javax.swing.JButton bOK;
    // End of variables declaration//GEN-END:variables
    

}


class CheckCellRenderer extends JCheckBox implements TableCellRenderer
{
	protected static Border m_noFocusBorder = new EmptyBorder(1, 1, 1, 1);
	protected static Border m_focusBorder = UIManager.getBorder(
			"Table.focusCellHighlightBorder");

	public CheckCellRenderer() {
		super();
		setOpaque(true);
		setBorderPainted(true);	// Important
		setBorder(m_noFocusBorder);
		setHorizontalAlignment(JCheckBox.CENTER);
	}

	public Component getTableCellRendererComponent(JTable table,
		Object value, boolean isSelected, boolean hasFocus,
		int nRow, int nCol)
	{
		if (value instanceof Boolean) {
			Boolean b = (Boolean)value;
			setSelected(b.booleanValue());
		}

		setBackground(isSelected && !hasFocus ?
			table.getSelectionBackground() : table.getBackground());
		setForeground(isSelected && !hasFocus ?
			table.getSelectionForeground() : table.getForeground());
		setFont(table.getFont());
		setBorder(hasFocus ? m_focusBorder : m_noFocusBorder);

		return this;
	}
        
      
}


class MessageTableModel extends AbstractTableModel
{
    
        static final String NOT_IN_USE = "[-]";
        static final String MISSING    = "[?]";
        static final String GOT_IT     = "[X]";
    
	public static final ColumnData m_columns[] = {
		new ColumnData( "Destination Agent", 340, JLabel.LEFT ),
		new ColumnData( "Send Vector", 170, JLabel.CENTER ),
		new ColumnData( "Recv Vector", 170, JLabel.CENTER ),
		new ColumnData( "OK", 30, JLabel.CENTER ),
	};

	public static final int COL_DEST = 0;
	public static final int COL_SEND_VEC = 1;
	public static final int COL_RECV_VEC = 2;
	public static final int COL_ARRIVED = 3;

	public static final String[] CATEGORIES = {
            "Destination Agent", "Send Vector", "Recv Vector", "OK"
        };

	protected AgentMessageList m_parent;
	protected Vector m_vector;

	public MessageTableModel(AgentMessageList parent) {
		m_parent = parent;
		m_vector = new Vector();
	}

	public int getRowCount() {
		return m_vector==null ? 0 : m_vector.size();
	}

	public int getColumnCount() {
		return m_columns.length;
	}

	public String getColumnName(int nCol) {
		return m_columns[nCol].title;
	}

	public boolean isCellEditable(int nRow, int nCol) {
		return false;
	}

	public Object getValueAt(int nRow, int nCol) {
		if (nRow < 0 || nRow>=getRowCount())
			return "";
		Message row = (Message)m_vector.elementAt(nRow);
		switch (nCol) {
			case COL_DEST:
				return row.dest();
			case COL_SEND_VEC:
				return genVectorDisplay(row.sendVector(), true);
			case COL_RECV_VEC:
				return genVectorDisplay(row.recvVector(), false);
			case COL_ARRIVED:
				return new Boolean(row.arrived());
		}
		return "";
	}

	public Object getValueAt(int nRow) {
		if (nRow < 0 || nRow>=getRowCount())
			return null;
		return (Message)m_vector.elementAt(nRow);
	}
         
        
        private String genVectorDisplay(Vector _v, boolean _isSend) {
            
            LogPointVectorMgmt mgmt = m_parent.getAgent().getMgmt();
            Vector display = mgmt.convertToLogicalOrder( _v , _isSend);
            
            //Possible Values - a LogPointEntry, null, or LogPointVectorMgmt.NOT_IN_USE 
            int sz = display.size();
            String result = "";
            for (int i=0; i<sz; i++) {                
                Object item = display.elementAt(i);
                if (item == null) {
                    result = result + MISSING;
                } else if (item instanceof Integer) {
                    result = result + NOT_IN_USE;                    
                } else if (item instanceof LogPointEntry) {
                    result = result + GOT_IT;                    
                } else {
                    result = result + "???";                    
                }
            }
            if (result.length() == 0) {
                result = "ERROR";
            }
             
            return result;
        }
            
	public void setValueAt(Object value, int nRow, int nCol) {
		if (nRow < 0 || nRow>=getRowCount() || value == null)
			return;
		Message row = (Message)m_vector.elementAt(nRow);
		String svalue = value.toString();

System.out.println("**AgentMessageList table model -- setValue() called!!");                
/*
		switch (nCol) {
			case COL_DATE:
				row.m_date = (Date)value;
				break;
			case COL_AMOUNT:
				if (value instanceof Double)
					row.m_amount = (Double)value;
				else
					row.m_amount = new Double(((Number)value).doubleValue());
				m_parent.calcTotal();
				break;
			case COL_CATEGORY:
				for (int k=0; k<CATEGORIES.length; k++)
					if (svalue.equals(CATEGORIES[k])) {
						row.m_category = new Integer(k);
						break;
					}
				break;
			case COL_APPROVED:
				row.m_approved = (Boolean)value;
				m_parent.calcTotal();
				break;
			case COL_DESCRIPTION:
				row.m_description = svalue;
				break;
		}
 */
	}

	public int addRow(Message _msg) {
            synchronized(m_vector) {
                m_vector.addElement(_msg);
                return m_vector.size();
            }
	}
/*
	public boolean delete(int nRow) {
		if (nRow < 0 || nRow >= m_vector.size())
			return false;
		m_vector.remove(nRow);
		return true;
	}
 */
}
