package sigea.gui;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import javax.swing.JFrame;
import javax.swing.JTabbedPane;
import javax.swing.Timer;
import javax.swing.table.DefaultTableModel;
import lombok.extern.slf4j.Slf4j;

/**
 * * * @author Pasquale Livecchi
 */
@Slf4j
public class MsgGui extends javax.swing.JFrame {

    private static final DateFormat DATE_FMT = new SimpleDateFormat("HH:mm:ss:SSS");
    private Timer animationTimer;
    private Socket sock;
    private BufferedReader readBuffer;
    private Map<String, Integer> rowmap;
    private volatile boolean isLoading;
// private int nodata;

    void setupSocket() {
        if (sock == null) {
            try {
                sock = new Socket("localhost", 9069);
                readBuffer = new BufferedReader(new InputStreamReader(sock.getInputStream()));
                setTitle("Sigea Msg Gui -> Connected");
            } catch (IOException ex) {
                System.out.printf("Error During Socket Setup: %s", ex.getMessage());
                log.error("Error During Socket Setup: {}", ex.getMessage());
                setTitle("Sigea Msg Gui -> Disconnected");
                closeSocket();
            }
        }
    }

    void readMsgs() {
        try {
            while (readBuffer.ready()) {
                // nodata = -1; 
                String line = readBuffer.readLine();

                if (line != null) {
                    switch (line) {
                        case "Loading":
                            isLoading = true;
                            clear();
                            break;
                        case "DoneLoading":
                            isLoading = false;
                            break;
                        default:
                            String[] splitLine = line.split(",");
                            // System.out.printf("splitline.length %s", splitLine.length).println(); 
                            if (isLoading && splitLine.length == 1) {
//                                String msgKey = splitLine[0];
                            // System.out.printf("splitline isLoading %s", splitLine[0]).println(); 
                                addKeyToRowMap(splitLine[0]);
                            } else if (!isLoading && splitLine.length == 4) {
//                                System.out.printf("splitline !isLoading %s,%s,%s,%s", splitLine[0],splitLine[1],splitLine[2],splitLine[3]).println();
                                addMsgToGuiTable(splitLine);
                            }
                    }

                }
            }
        } catch (IOException | RuntimeException ex) {
            closeSocket();
        }
    }

    @Override
    protected void frameInit() {
        super.frameInit();
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setVisible(true);
        setTitle("Sigea Msg Gui");
        setSize(800, 600);
        rowmap = new HashMap<>();
        animationTimer = new Timer(100, evt -> {
            setupSocket();
            if (sock != null) {
                readMsgs();
            }
        });
        animationTimer.start();
    }

    public void addKeyToRowMap(String rowName) {
        rowmap.computeIfAbsent(rowName, key -> {
            DefaultTableModel.class.cast(jTablemap.getModel()).addRow(new String[]{key, "unk", "unk", "unk"});
            return jTablemap.getModel().getRowCount() - 1;
        });
    }

    public void addMsgToGuiTable(String[] stats) {
        Optional.ofNullable(rowmap.get(stats[0])).ifPresent(row -> {
            int rowviewId = jTablemap.convertRowIndexToView(row);
            jTablemap.setValueAt(stats[1], rowviewId, 1);
            jTablemap.setValueAt(stats[2], rowviewId, 2);
            jTablemap.setValueAt(stats[3], rowviewId, 3);
        });

    }

    void closeSocket() {
        if (sock != null) {
            try {
                sock.close();
            } catch (IOException ex) {
                log.error("Gui Socket Error On Close {}", ex);
            }
            sock = null;
            readBuffer = null;
        }
        clear();
        setTitle("Sigea Msg Gui -> Disconnected");

    }

    @Override
    public void dispose() {
        super.dispose();
        animationTimer.stop();
        if (sock != null) {
            try {
                sock.close();
            } catch (IOException ex) {
                log.error("Gui Error On Dispose {}", ex);
            }
        }
    }

    void clear() {
        // jTabs.removeAll(); 
        jTablemap.removeAll();
        repaint();
    }

    /**
     * Creates new form MsgGui
     */
    public MsgGui() {
        initComponents();
//        jTabs.add()
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jScrollPane1 = new javax.swing.JScrollPane();
        jTablemap = new javax.swing.JTable();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        jTablemap.setAutoCreateRowSorter(true);
        jTablemap.setFont(new java.awt.Font("Tahoma", 0, 14)); // NOI18N
        jTablemap.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "Message Key", "Quality", "Timestamp", "Value"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.String.class
            };
            boolean[] canEdit = new boolean [] {
                false, false, false, false
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        jScrollPane1.setViewportView(jTablemap);
        if (jTablemap.getColumnModel().getColumnCount() > 0) {
            jTablemap.getColumnModel().getColumn(0).setResizable(false);
            jTablemap.getColumnModel().getColumn(1).setResizable(false);
            jTablemap.getColumnModel().getColumn(2).setResizable(false);
            jTablemap.getColumnModel().getColumn(3).setResizable(false);
        }

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 776, Short.MAX_VALUE)
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 456, Short.MAX_VALUE)
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        /* Set the Nimbus look and feel */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html 
         */
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException ex) {
            java.util.logging.Logger.getLogger(MsgGui.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(MsgGui.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(MsgGui.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(MsgGui.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new MsgGui().setVisible(true);
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JTable jTablemap;
    // End of variables declaration//GEN-END:variables
}
