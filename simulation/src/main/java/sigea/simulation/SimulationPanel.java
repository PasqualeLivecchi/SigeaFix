package sigea.simulation;

import java.awt.EventQueue;
import java.nio.channels.DatagramChannel;
import java.nio.file.Path;
import java.nio.file.Paths;
import javax.swing.JOptionPane;
import sigea.entities.Message;
import sigea.entities.MsgConfig;
import sigea.entities.MsgConnection;
import sigea.entities.MsgReading;
import com.opencsv.bean.CsvToBeanBuilder;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import io.vavr.collection.Stream;
import io.vavr.control.Try;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.util.List;

public abstract class SimulationPanel extends javax.swing.JPanel {

    private static final long serialVersionUID = 1L;
    private Disposable simulationTask;
    private boolean simCfgLoaded = false;
    private static MsgConfig msgConfig = null;
    private static final Path CONN_CONFIG = Paths.get("sigea_connection_config.csv");
    private static final Path SIM_DATA = Paths.get("sigea_simulation_data.csv");
    private static final Path CONFIG = Paths.get("sigea_msg_config.csv");

    protected abstract Consumer<Stream<MsgReading>> initMsgSimulationStream(MsgConfig msgCfg, DatagramChannel chan, String hostIp);

    public static MsgConfig getMsgConfig() {
        if (msgConfig == null) {
            loadMsgConfig();
        }
        return msgConfig;
    }

    public static void loadMsgConfig() {
        msgConfig = new MsgConfig();
        msgConfig.setId("simCfg");
        try (Reader r = Files.newBufferedReader(CONN_CONFIG)) {
            List<MsgConnection> conns = new CsvToBeanBuilder(r).withType(MsgConnection.class).build().parse();
            msgConfig.setMsgConnections(conns);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
        try (Reader r = Files.newBufferedReader(CONFIG)) {
            List<Message> msgs = new CsvToBeanBuilder(r).withType(Message.class).build().parse();
            msgConfig.setMsgs(msgs);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    public SimulationPanel() {
        initComponents();
    }

    protected Iterable<Message> messages(MsgConfig msgConfig) {
        return msgConfig.getMsgs();
    }

    @SuppressWarnings("unchecked")
    private void initComponents() {
// <editor-fold defaultstate="collapsed" desc="Generated Code">                          
        javax.swing.JButton start = new javax.swing.JButton();
        javax.swing.JButton stop = new javax.swing.JButton();
        status = new javax.swing.JLabel();

        start.setText("Start Msg Sim");
        start.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                start(evt);
            }
        });

        stop.setText("Stop Simulator");
        stop.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                stop(evt);
            }
        });

        status.setFont(new java.awt.Font("Tahoma", 0, 14)); // NOI18N
        status.setText("Status: Stopped");

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
                layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(layout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                        .addGroup(layout.createSequentialGroup()
                                                .addComponent(start)
                                                .addGap(18, 18, 18)
                                                .addComponent(stop)
                                                .addGap(0, 9, Short.MAX_VALUE))
                                        .addComponent(status, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                                .addContainerGap())
        );
        layout.setVerticalGroup(
                layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(layout.createSequentialGroup()
                                .addContainerGap()
                                .addComponent(status)
                                .addGap(18, 18, 18)
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                        .addComponent(start)
                                        .addComponent(stop))
                                .addContainerGap(231, Short.MAX_VALUE))
        );
    }// </editor-fold>                        

    private void start(java.awt.event.ActionEvent evt) {
        stop(null);
        loadMsgConfig();
        loadSimCfg();
        SimStreamUtil.chooseFile("sigea_simulation_data", ".csv").toTry()
                .andThenTry(this::streamData)
                .onFailure(this::error)
                .onSuccess(csv -> status.setText("Status: Running"));
    }

    private void stop(java.awt.event.ActionEvent evt) {
        if (simulationTask != null) {
            status.setText("Status: Stopped");
            simCfgLoaded = false;
            simulationTask.dispose();
        }
    }

    private void loadSimCfg() {
        if (!simCfgLoaded) {
            msgConfig = getMsgConfig();
            if (msgConfig == null) {
                JOptionPane.showMessageDialog(null, "No Msg Config Loaded");
            } else {
                // SimStreamUtil.generateBlank(msgConfig.getMsgs());
                simCfgLoaded = true;
            }
        }
    }

    private void error(Throwable err) {
        
        String message = String.format("(%s) %s", err.getClass().getSimpleName(), err.getMessage());
        System.out.printf("error %s", message).println();
        status.setText("Status: Stopped | Error - " + message);
    }

    private Try<?> streamData(Path dataCsv) {
        msgConfig = getMsgConfig();
        if (msgConfig == null) {
            throw new RuntimeException("No Msg Config Loaded");
        }
        return UdpUtil.open(1)
                .onFailure(ex -> JOptionPane.showMessageDialog(null, Stream.of("Error Opening Port: ", ex.getMessage()).mkString(" ")))
                .andThenTry(chan -> setupSubscription(dataCsv, msgConfig, chan));
    }

    private void setupSubscription(Path csv, MsgConfig msgConfig, DatagramChannel channel) {
        Consumer<Throwable> onError = ex -> EventQueue.invokeLater(() -> error(ex));
        simulationTask = SimStreamUtil.simulateStream(csv, 1).get()
                .subscribe(initMsgSimulationStream(msgConfig, channel, SimulationFrame.getSigeaHost()), onError);
    }
// Variables declaration - do not modify                     
    private javax.swing.JLabel status;
// End of variables declaration                   
}
