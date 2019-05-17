package sigea.main;

import sigea.entities.BatchOfMsgReadings;
import sigea.entities.MsgConfig;
import sigea.entities.MsgReading;
import io.reactivex.Observable;
import io.reactivex.Scheduler;
import io.reactivex.disposables.Disposable;
import io.vavr.collection.Stream;
import io.vavr.control.Try;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import lombok.NonNull;
import lombok.Synchronized;
import lombok.extern.slf4j.Slf4j;

/**
 * * * * * @author Pasquale Livecchi
 */
@Slf4j
@ApplicationScoped
public class MsgResourceBroadcast {

    @Inject
    private Scheduler guiScheduler;
    private ServerSocketChannel serverSocketChan;
    private final Set<SocketChannel> socketChans = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private final BlockingQueue<MsgReading> msgReadingQue = new LinkedBlockingQueue<>();
    private final Set<String> msgKeys = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private Disposable broadcastTask;
    private volatile boolean isLoaded;

    @PostConstruct
    public void initServerSocket() {
        try {
            serverSocketChan = ServerSocketChannel.open();
            serverSocketChan.configureBlocking(false);
            serverSocketChan.bind(new InetSocketAddress(InetAddress.getLoopbackAddress(), 9069));
            log.info("Opening Udp Gui socket connection on port 9069");
        } catch (IOException ex) {
            log.error("Error setting up server socket for GUI {}", ex.getMessage());
        }
        broadcastTask = Observable.interval(100, TimeUnit.MILLISECONDS, guiScheduler)
                .subscribe(i -> broadcastMsgs());
    }

    @Synchronized
    public void loadConfig(@Observes @NonNull MsgConfig msgCfg) {
        msgCfg.getMsgs().forEach(msg -> {
            msgKeys.add(msg.uniqueKeyName());
        });
        msgReadingQue.clear();
        isLoaded = true;
    }

    public void loadMsgReadings(@Observes @NonNull BatchOfMsgReadings bomr) {
        msgReadingQue.addAll(bomr.getMsgReadings());
    }

    @Synchronized
    private void broadcastMsgs() {
        try {
            Set<SocketChannel> newChans = new HashSet<>();
            SocketChannel newConn;
            while ((newConn = serverSocketChan.accept()) != null) {
                newConn.configureBlocking(false);
                newChans.add(newConn);
            }
            socketChans.addAll(newChans);
            if (isLoaded) {
                broadcast(socketChans, bufferForMsgKeys());
                isLoaded = false;
            } else if (!newChans.isEmpty()) {
                broadcast(newChans, bufferForMsgKeys());
            }
            List<MsgReading> currentMsgReadings = new LinkedList<>();
            msgReadingQue.drainTo(currentMsgReadings);
            broadcast(socketChans, bufferForBatchOfMsgReadings(currentMsgReadings));
            socketChans.removeIf(sc -> !sc.isOpen());
        } catch (IOException | RuntimeException ex) {
            log.error("Error processing socket information", ex);
            closeChans();
        }

    }

    private void broadcast(Iterable<SocketChannel> chans, ByteBuffer byteMsg) {
        chans.forEach(chan -> {
            if (chan.isOpen()) {
                try {
                    ByteBuffer buf = ByteBuffer.allocate(1024);
                    boolean responsePing = false;
                    while (chan.read(buf) > 0) {
                        responsePing = true;
                        buf.clear();
                    }
                    if (responsePing) {
                        buf.clear();
                        buf.put("Pong\n".getBytes(StandardCharsets.UTF_8));
                        buf.flip();
                        while (buf.hasRemaining()) {
                            chan.write(buf);
                        }
                    }
                    while (byteMsg.hasRemaining()) {
                        chan.write(byteMsg);
                    }
                    byteMsg.rewind();
                } catch (IOException ex) {
                    try {
                        chan.close();
                    } catch (IOException ex1) {
                        // pass
                    }
                }
            }
        });
    }

    @Synchronized
    protected ByteBuffer bufferForMsgKeys() {
        String start = "Loading\n";
        String byteString = Stream.ofAll(msgKeys).mkString("\n") + "\n";
        String end = "DoneLoading\n";
        return ByteBuffer.wrap((start + byteString + end).getBytes(StandardCharsets.UTF_8));
    }

    private ByteBuffer bufferForBatchOfMsgReadings(List<MsgReading> msgs) {
        String byteString = Stream.ofAll(msgs)
                .map(m -> m.toString())
                .mkString("\n") + "\n";
        return ByteBuffer.wrap(byteString.getBytes(StandardCharsets.UTF_8));
    }

    private void closeChans() {
        msgReadingQue.clear();
        socketChans.forEach(chan -> Try.run(chan::close));
        socketChans.clear();
    }

    @PreDestroy
    public void dispose() {
        try {
            serverSocketChan.close();
        } catch (IOException ex) {
            log.error("Error closing socket");
        }
        broadcastTask.dispose();
    }
}
