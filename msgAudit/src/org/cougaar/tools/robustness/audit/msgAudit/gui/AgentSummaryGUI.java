/*
 * AgentSummaryGUI.java
 *
 * Created on February 16, 2003, 6:09 PM
 */

package LogPointAnalyzer.gui;

import LogPointAnalyzer.*;
import LogPointAnalyzer.event.*;

import javax.swing.JLabel;
import javax.swing.JSlider;
import javax.swing.table.*;
import javax.swing.DefaultListSelectionModel;

import java.util.Vector;
/**
 *
 * @author  Administrator
 */
public class AgentSummaryGUI extends javax.swing.JFrame implements EventListener {
    
    private TrafficAuditor auditor;
    int refreshTime = -1;
    
    
    /** Creates new form AgentSummaryGUI */
    public AgentSummaryGUI(TrafficAuditor _ta) {
        
        auditor = _ta;
        
        //Register for events
        EventHandler.handler().addListener(this);
        
        initComponents();
        setHeaders();
        initSelectionModel();
        this.setTitle("OBJS Traffic Analysis");
        
        //Start Auto Refresh thread
        new Thread(new Refresher(this)).start();
    }
    
    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    private void initComponents() {//GEN-BEGIN:initComponents
        lTitle = new javax.swing.JLabel();
        summaryScrollPane = new javax.swing.JScrollPane();
        summaryTable = new javax.swing.JTable();
        buttonPanel = new javax.swing.JPanel();
        bRefresh = new javax.swing.JButton();
        refreshSlider = new javax.swing.JSlider();
        bShowLogPointConsole = new javax.swing.JButton();
        bAutoRefresh = new javax.swing.JToggleButton();
        bQuit = new javax.swing.JButton();

        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent evt) {
                exitForm(evt);
            }
        });

        lTitle.setFont(new java.awt.Font("Dialog", 1, 18));
        lTitle.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        lTitle.setText("Agent Summary");
        getContentPane().add(lTitle, java.awt.BorderLayout.NORTH);

        summaryTable.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "Agent", "Sent", "Rec'd", "Outstanding"
            }
        ) {
            boolean[] canEdit = new boolean [] {
                false, false, false, false
            };

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        summaryTable.setAlignmentX(-1.0F);
        summaryTable.setAlignmentY(1.0F);
        summaryTable.setMinimumSize(new java.awt.Dimension(120, 300));
        summaryTable.setName("agentTable");
        summaryScrollPane.setViewportView(summaryTable);

        getContentPane().add(summaryScrollPane, java.awt.BorderLayout.CENTER);

        buttonPanel.setLayout(null);

        buttonPanel.setMinimumSize(new java.awt.Dimension(550, 80));
        buttonPanel.setPreferredSize(new java.awt.Dimension(550, 80));
        bRefresh.setText("Refresh Now");
        bRefresh.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                bRefreshActionPerformed(evt);
            }
        });

        buttonPanel.add(bRefresh);
        bRefresh.setBounds(20, 30, 110, 26);

        refreshSlider.setMajorTickSpacing(5);
        refreshSlider.setMaximum(30);
        refreshSlider.setMinimum(5);
        refreshSlider.setPaintLabels(true);
        refreshSlider.setPaintTicks(true);
        refreshSlider.setValue(5);
        refreshSlider.setName("Refresh Rate");
        refreshSlider.setEnabled(false);
        refreshSlider.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                refreshSliderStateChanged(evt);
            }
        });

        buttonPanel.add(refreshSlider);
        refreshSlider.setBounds(140, 30, 150, 40);

        bShowLogPointConsole.setText("Show LogPoint Console");
        bShowLogPointConsole.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                bShowLogPointConsoleActionPerformed(evt);
            }
        });

        buttonPanel.add(bShowLogPointConsole);
        bShowLogPointConsole.setBounds(300, 30, 168, 26);

        bAutoRefresh.setFont(new java.awt.Font("Dialog", 1, 10));
        bAutoRefresh.setText("AutoRefresh(off)");
        bAutoRefresh.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                bAutoRefreshActionPerformed(evt);
            }
        });

        buttonPanel.add(bAutoRefresh);
        bAutoRefresh.setBounds(160, 10, 120, 20);

        bQuit.setText("Quit");
        bQuit.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                bQuitActionPerformed(evt);
            }
        });

        buttonPanel.add(bQuit);
        bQuit.setBounds(480, 30, 60, 26);

        getContentPane().add(buttonPanel, java.awt.BorderLayout.SOUTH);

        pack();
    }//GEN-END:initComponents

    private void refreshSliderStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_refreshSliderStateChanged
        JSlider source = (JSlider)evt.getSource();
        if (!source.getValueIsAdjusting()) {
	    refreshTime = (int)source.getValue();
        }      
    }//GEN-LAST:event_refreshSliderStateChanged

    private void bAutoRefreshActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_bAutoRefreshActionPerformed
        if (bAutoRefresh.isSelected()) {
             bAutoRefresh.setText("AutoRefresh(on)");
             refreshSlider.setEnabled(true);
             //get time on slider & set refresh
             refreshTime = refreshSlider.getValue();
             
        } else { //auto refresh is now off
             bAutoRefresh.setText("AutoRefresh(off)");
             refreshSlider.setEnabled(false);
             
             //turn off auto refresh
             refreshTime = -1;
        }


    }//GEN-LAST:event_bAutoRefreshActionPerformed

    private void bQuitActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_bQuitActionPerformed
        exitForm(null);
    }//GEN-LAST:event_bQuitActionPerformed

    private void bShowLogPointConsoleActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_bShowLogPointConsoleActionPerformed
        showLogPointMgmtActionPerformed();
    }//GEN-LAST:event_bShowLogPointConsoleActionPerformed

    /*
     * Refresh all of the agent data
     */
    private void bRefreshActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_bRefreshActionPerformed

        //
        DefaultTableModel dtm = (DefaultTableModel)summaryTable.getModel();

        Vector data = dtm.getDataVector();
        
        //Update the values
        for (int i=0; i<data.size(); i++) {
         
            AgentData agent = (AgentData)((Vector)data.elementAt(i)).elementAt(0);
            ((Vector)data.elementAt(i)).setElementAt(agent.sentLabel(), 1);
            ((Vector)data.elementAt(i)).setElementAt(agent.recdLabel(), 2);
            ((Vector)data.elementAt(i)).setElementAt(agent.leftLabel(), 3);
        }
        
        dtm.fireTableDataChanged();
        //summaryTable.revalidate();
        //summaryTable.repaint();
    }//GEN-LAST:event_bRefreshActionPerformed
    
    /** Exit the Application */
    private void exitForm(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_exitForm
        System.exit(0);
    }//GEN-LAST:event_exitForm

    public int getRefreshTime() { return refreshTime; }

    void showLogPointMgmtActionPerformed() {
       LogPointVectorMgmt logPointMgmt = auditor.getLogPointMgmt();
       logPointMgmt.showConsole();
    }

    public void setRefreshButtonColor(java.awt.Color _c) {
        bAutoRefresh.setForeground(_c);
    }
    
    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        AgentSummaryGUI asg = new AgentSummaryGUI(null);
        asg.show();
//        asg.addAgent(new AgentData(mgmt, "me",1,2,3));
    }
    
    
    void setHeaders() {

        DefaultTableCellRenderer labelRenderer = new DefaultTableCellRenderer();
        labelRenderer.setHorizontalAlignment(JLabel.CENTER);
        DefaultTableModel dtm = (DefaultTableModel)summaryTable.getModel();
        
        TableColumnModel tcm = summaryTable.getColumnModel();
        TableColumn tc = tcm.getColumn(0);
        tc.setMinWidth(180);
        tc.setResizable(true);
        tc = tcm.getColumn(1);
        tc.setCellRenderer(labelRenderer);
        
        tc.setMinWidth(60);
        tc.setMaxWidth(60);
        tc.setResizable(false);
        tc = tcm.getColumn(2);
        tc.setCellRenderer(labelRenderer);
        tc.setMinWidth(60);
        tc.setMaxWidth(60);
        tc.setResizable(false);
        tc = tcm.getColumn(3);
        tc.setCellRenderer(labelRenderer);
        tc.setMinWidth(60);
        tc.setMaxWidth(60);
        tc.setResizable(false);
    }
    
    void initSelectionModel() {
        
        DefaultListSelectionModel lsm = (DefaultListSelectionModel)summaryTable.getSelectionModel();
        lsm.setSelectionMode(DefaultListSelectionModel.SINGLE_SELECTION);
        summaryTable.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2) {
                    java.awt.Point origin  = e.getPoint();
                    int row = summaryTable.rowAtPoint(origin);
                    if (row == -1) { // no cell clicked
                        return;
                    } else {
                        rowSelected(row);
                    }
                }
            }
        });
    }
    
    
    
    /* new final point event */
    public void newFinalPointEvent(LogPointLevel _lpl) {
        bRefreshActionPerformed(null);
    }
    
    
    /*
     * Display agent information when a user double-clicks on a row
     *
     */
    void rowSelected(int _row) {

        DefaultTableModel dtm = (DefaultTableModel)summaryTable.getModel();
        AgentData ad = (AgentData)dtm.getValueAt(_row, 0);
        if (ad != null) {
            ad.showMsgList();
        }
    }
    
    /*
     * Called to add a set of agents
     *
     */
    public void setData(AgentData[] _agents) {
     
        DefaultTableModel dtm = (DefaultTableModel)summaryTable.getModel();
        for (int i=0; i<_agents.length; i++) {
            Object[] row = {_agents[i], _agents[i].sentLabel(), _agents[i].recdLabel(), _agents[i].leftLabel()};             
            dtm.addRow(row); 
        }    
   }
    
    /*
     * Called to add one agent
     *
     */
    public void addAgent(AgentData _agent) {
     
        DefaultTableModel dtm = (DefaultTableModel)summaryTable.getModel();
        Object[] row = {_agent, _agent.sentLabel(), _agent.recdLabel(), _agent.leftLabel()};
        dtm.addRow(row);
   }

    
    
    
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton bShowLogPointConsole;
    private javax.swing.JTable summaryTable;
    private javax.swing.JButton bRefresh;
    private javax.swing.JButton bQuit;
    private javax.swing.JScrollPane summaryScrollPane;
    private javax.swing.JSlider refreshSlider;
    private javax.swing.JToggleButton bAutoRefresh;
    private javax.swing.JPanel buttonPanel;
    private javax.swing.JLabel lTitle;
    // End of variables declaration//GEN-END:variables
    

    class Refresher implements Runnable {
     
        AgentSummaryGUI parent;
        int sleepTime = 5000;
        
        Refresher(AgentSummaryGUI _parent) {
           parent = _parent;   
        }
        
        public void run() {
            int delay;
              try {
                while (true) {
                  delay = parent.getRefreshTime();
                  if (delay == -1) { // refresh is off
                      sleepTime = 5000;
                  } else {
                      //Refresh! Temporarily change button color to show auto refresh is on
                      parent.setRefreshButtonColor(java.awt.Color.red);
                      parent.bRefreshActionPerformed(null);
                      Thread.sleep(250);
                      parent.setRefreshButtonColor(java.awt.Color.black);
                      sleepTime = delay * 1000; //convert to milliseconds
                  }
                  //Now sleep!
                  Thread.sleep(sleepTime);
                }
             } catch (InterruptedException e) {}
         }
     }
    
}