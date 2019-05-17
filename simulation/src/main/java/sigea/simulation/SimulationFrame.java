package sigea.simulation;

import java.awt.EventQueue;

/**
 * * * @author Pasquale Livecchi
 */
public class SimulationFrame extends javax.swing.JFrame {

    private static SimulationFrame instance;

    public static String getSigeaHost() {
        return instance.sigeaHost.getText();
    }

    /**
     * * Creates new form SimFrame
     */
    public SimulationFrame() {
        initComponents();
    }

    @SuppressWarnings("unchecked")
    private void initComponents() { // <editor-fold defaultstate="collapsed" desc="Generated Code">
        javax.swing.JTabbedPane content = new javax.swing.JTabbedPane();
        sigea.simulation.MsgSimPanel msgsim = new sigea.simulation.MsgSimPanel();
        javax.swing.JLabel lblHost = new javax.swing.JLabel();
        sigeaHost = new javax.swing.JTextField();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setTitle("Sigea Simulator");

        content.addTab("Msg Simulator", msgsim);

        lblHost.setText("Destination");

        sigeaHost.setText("localhost");

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
                layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addComponent(content)
                        .addGroup(layout.createSequentialGroup()
                                .addContainerGap()
                                .addComponent(lblHost)
                                .addGap(18, 18, 18)
                                .addComponent(sigeaHost, javax.swing.GroupLayout.PREFERRED_SIZE, 113, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(18, 18, 18)
                                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(
                layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                        .addComponent(lblHost)
                                        .addComponent(sigeaHost, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addComponent(content, javax.swing.GroupLayout.PREFERRED_SIZE, 282, javax.swing.GroupLayout.PREFERRED_SIZE))
        );

        pack();
    }// </editor-fold>

    /**
     * * @param args the command line arguments
     */
    public static void main(String args[]) {
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException ex) {
            java.util.logging.Logger.getLogger(SimulationFrame.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(SimulationFrame.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(SimulationFrame.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(SimulationFrame.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }

        /* Create and display the form */
        EventQueue.invokeLater(() -> {
            instance = new SimulationFrame();
            instance.setVisible(true);
        });
    } // Variables declaration - do not modify
    private javax.swing.JTextField sigeaHost; // End of variables declaration
}
