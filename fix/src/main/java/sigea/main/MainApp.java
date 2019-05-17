package sigea.main;

import sigea.entities.Message;
import sigea.entities.MsgConfig;
import sigea.entities.MsgConnection;
import com.opencsv.bean.CsvToBeanBuilder;
import io.reactivex.Scheduler;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import io.vavr.control.Try;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import javax.annotation.PreDestroy;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.Initialized;
import javax.enterprise.event.Event;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.Produces;
import javax.enterprise.inject.se.SeContainer;
import javax.enterprise.inject.se.SeContainerInitializer;

import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;

/**
 * * * * * @author Pasquale Livecchi
 */
@Slf4j
@ApplicationScoped
public class MainApp {

    private Disposable filewatcher;

    private static final Path DIRECTORY = Paths.get("");
    private static final Path MSG_CONFIG = Paths.get("sigea_msg_config.csv");
    private static final Path CONN_CONFIG = Paths.get("sigea_connection_config.csv");
    private static final Path SETTINGS = Paths.get("sigea_connection_settings.conf");

    @Inject
    private Event<MsgConfig> msgConfigEvent;
    private String sigeaHost;

    private static final Lock LOCK = new ReentrantLock();
    private static final Condition RUNCONDITION = LOCK.newCondition();
    private static volatile boolean isRunning = true;
    private static volatile SeContainer weldingOfCsvConfigToContainer;

    public static void main(String[] args) throws InterruptedException {
        startApp(args);
    }

    public static void startApp(String[] args) {
        LOCK.lock();
        try {
            weldingOfCsvConfigToContainer = SeContainerInitializer.newInstance().initialize();
        } finally {
            LOCK.unlock();
        }
        new Thread(() -> {
            while (isRunning) {
                LOCK.lock();
                try {
                    Try.run(RUNCONDITION::await);
                } finally {
                    LOCK.unlock();
                }
            }
        }).start();
    }

    public static void stop(String[] args) {
        isRunning = false;
        LOCK.lock();
        try {
            weldingOfCsvConfigToContainer.close();
            Try.run(RUNCONDITION::signal);
        } finally {
            LOCK.unlock();
        }
    }

    @Produces
    public InetSocketAddress sigea() {
        return new InetSocketAddress(sigeaHost, 8);
    }

    @Produces
    @ApplicationScoped
    public Scheduler computation() {
        return Schedulers.computation();
    }

    @Produces
    @Io
    @ApplicationScoped
    public Scheduler io() {
        return Schedulers.io();
    }

    private void createDefaultSettingsFile() {
        Properties settings = new Properties();
        settings.setProperty("sigea.host", "localhost");
        try (Writer w = Files.newBufferedWriter(SETTINGS)) {
            settings.store(w, "Master Msg Settings");
        } catch (IOException ex) {
            log.error(null, ex);
        }
    }

    private void lazyLoadSettings() {
        if (!Files.exists(SETTINGS)) {
            createDefaultSettingsFile();
        }
        Properties settings = new Properties();
        try (Reader r = Files.newBufferedReader(SETTINGS)) {
            settings.load(r);
        } catch (IOException ex) {
            settings.setProperty("sigea.host", "localhost");
        }
        sigeaHost = settings.getProperty("sigea.host");
    }

    public void initSigeaMsgModule(@Observes @Initialized(ApplicationScoped.class) Object init) {
        log.info("Starting Message Service V. {}", getClass().getPackage().getImplementationVersion());
        lazyLoadSettings();
        loadMsgConfiguration(MSG_CONFIG);
        filewatcher = new ConfigFileWatcher(DIRECTORY, computation())
                .createFileEventStream().subscribe(evt_path -> {
                    if (evt_path._2.equals(SETTINGS)) {
                        lazyLoadSettings();
                    } else if (evt_path._2.equals(MSG_CONFIG)) {
                        loadMsgConfiguration(MSG_CONFIG);
                    }
                });
    }

    @PreDestroy
    public void dispose() {
        log.info("Stopping Message Service");
        filewatcher.dispose();
    }

    private void unload() {
        msgConfigEvent.fire(new MsgConfig());
    }

    private MsgConfig loadConnectionConfiguration(Path file) {
        if (!Files.exists(file)) {
            log.warn("No connection config to load");
            return new MsgConfig();
        } else {
            log.info("Attempting to load new connection config");
            try {
                MsgConfig msgConfig = new MsgConfig();
                try (Reader r = Files.newBufferedReader(file)) {
                    List<MsgConnection> conns = new CsvToBeanBuilder(r).withType(MsgConnection.class).build().parse();
                    msgConfig.setMsgConnections(conns);
                } catch (IOException ex) {
                    throw new RuntimeException(ex);
                }
                log.info("New connection config loaded");
                log.info("Loaded connections from config: {}", msgConfig.getMsgConnections());
                return msgConfig;
            } catch (RuntimeException ex) {
                log.error(null, ex);
                return new MsgConfig();
            }
        }
    }

    private void loadMsgConfiguration(Path file) {
        MsgConfig msgConfig = loadConnectionConfiguration(CONN_CONFIG);
        if (!Files.exists(file)) {
            log.warn("No Message config to load");
            unload();
        } else {
            log.info("Attempting to load new message config");
            try {
                try (Reader r = Files.newBufferedReader(file)) {
                    List<Message> msgs = new CsvToBeanBuilder(r).withType(Message.class).build().parse();
                    msgConfig.setMsgs(msgs);
                } catch (IOException ex) {
                    throw new RuntimeException(ex);
                }
                msgConfigEvent.fire(msgConfig);
                log.info("New Message config loaded");
                log.info("Loaded {} messages from config", msgConfig.getMsgs().size());
            } catch (RuntimeException ex) {
                log.error(null, ex);
            }
        }
    }
}
