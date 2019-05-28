package sigea.test;

import sigea.main.Io;
import sigea.main.UdpInetUtil;
import sigea.entities.BatchOfMsgReadings;
import sigea.entities.HealthReading;
import sigea.entities.HealthStatus;
import sigea.entities.MsgConfig;
import sigea.entities.MsgConnection;
import sigea.entities.MsgReading;
import sigea.entities.Message;
import sigea.entities.MsgQuality;
import org.junit.Test;
import static org.junit.Assert.*;
import io.reactivex.*;
import io.reactivex.schedulers.*;
import io.reactivex.subjects.ReplaySubject;
import io.reactivex.subscribers.TestSubscriber;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.concurrent.TimeUnit;
import javax.enterprise.event.Event;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.jglue.cdiunit.*;
import org.junit.*;
import org.junit.runner.RunWith;
import org.mockito.*;
import static org.mockito.Mockito.*;
import sigea.main.UdpMsgAcquisition;
//import org.slf4j.Logger;
/**
 * If tests for reading Udp packets fail randomly, increase wait time in
 * readFirst
 *
 * @author Pasquale Livecchi
 */
@RunWith(CdiRunner.class)
@AdditionalClasses(UdpMsgAcquisition.class)
//@Slf4j
public class UdpMsgAcquisitionTest {

    private final TestScheduler ts = new TestScheduler();
    private MsgConfig config;
    private int port;

    @Produces
    private final Scheduler scheduler = ts;
    @Produces
    @Io
    private final Scheduler io = Schedulers.io();
    @Mock
    private final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(UdpMsgAcquisitionTest.class);
    @Inject
    private HealthReadingObserver healthReadings;
    @Inject
    private MsgReadingObserver msgReadings;
    @Inject
    private Event<MsgConfig> loader;

    @Singleton
    public static class HealthReadingObserver {

        ReplaySubject<HealthReading> events = ReplaySubject.create();

        public void onHealth(@Observes HealthReading health) {
            events.onNext(health);
        }
    }

    @Singleton
    public static class MsgReadingObserver {

        ReplaySubject<BatchOfMsgReadings> events = ReplaySubject.create();

        public void onHealth(@Observes BatchOfMsgReadings readings) {
            events.onNext(readings);
        }
    }

    private Message create(String msgName, String fieldName, short byteOffset, byte bitOffset, String dataType) {
        Message msg = new Message();
        msg.setMsgConn("testConn");
        msg.setMsgName(msgName);
        msg.setFieldName(fieldName);
        msg.setByteOffset(byteOffset);
        msg.setBitOffset(bitOffset);
        msg.setDataType(dataType);
        return msg;
    }

    private Message create(String msgName, String fieldName, short byteOffset, String dataType) {
        return create(msgName, fieldName, byteOffset, (byte) 0, dataType);
    }

    @Before
    public void setup() throws Throwable {
        MockitoAnnotations.initMocks(this);
        port = UdpInetUtil.findRandomUnusedUdpPort();

        config = new MsgConfig();
        MsgConnection mc = new MsgConnection();
        config.getMsgConnections().add(mc);
        mc.setNameId("testConn");
        mc.setPort(port);
        config.getMsgs().add(create("test", "float", (short) 0, "F32"));
        config.getMsgs().add(create("test", "unsigned_short", (short) 4, "U16"));
        config.getMsgs().add(create("test", "unsigned_int", (short) 6, "U32"));
        config.getMsgs().add(create("test", "unsigned_long", (short) 10, "U64"));
        config.getMsgs().add(create("test", "signed_int", (short) 18, "S32"));
        config.getMsgs().add(create("test", "signed_long", (short) 22, "S64"));
        config.getMsgs().add(create("test", "signed_short", (short) 30, "S16"));
        config.getMsgs().add(create("test", "double", (short) 32, "F64"));
        config.getMsgs().add(create("test", "unsigned_byteorchar", (short) 40, "U8"));
        config.getMsgs().add(create("test", "bool_0", (short) 41, "U1"));
        config.getMsgs().add(create("test", "bool_1", (short) 41, (byte) 1, "U1"));
        config.getMsgs().add(create("test", "bool_2", (short) 41, (byte) 2, "U1"));
        config.getMsgs().add(create("test", "bool_3", (short) 41, (byte) 3, "U1"));
        config.getMsgs().add(create("test", "bool_4", (short) 41, (byte) 4, "U1"));
        config.getMsgs().add(create("test", "bool_5", (short) 41, (byte) 5, "U1"));
        config.getMsgs().add(create("test", "bool_6", (short) 41, (byte) 6, "U1"));
        config.getMsgs().add(create("test", "bool_7", (short) 41, (byte) 7, "U1"));
        
    }

    @After
    public void teardown() {
        loader.fire(new MsgConfig());
    }

    private ByteBuffer createPacket(float first, short second, int third, 
            long fourth, int s_fifth, long s_sixth, 
            short s_seventh, double eigth, byte ninth, byte tenth) {
        ByteBuffer packet = ByteBuffer.allocate(44)
                .order(ByteOrder.LITTLE_ENDIAN)
                .putFloat(first).putShort(second)
                .putInt(third).putLong(fourth)
                .putInt(s_fifth).putLong(s_sixth)
                .putShort(s_seventh).putDouble(eigth)
                .put((byte) ninth).put((byte) tenth);
        short checksum = 0;
        for (int i = 0; i < 42; i++) {
            checksum += Byte.toUnsignedLong(packet.get(i));
        }
        System.out.printf("checksum").println();
        packet.putShort(checksum);
        packet.flip();
        return packet;
    }
    
    private ByteBuffer createPacket(){
        return createPacket((float) 800.5, (short) 943, (int) 7023,
                (long) 50234230L, (int)-7023, (long) -50234230L, 
                (short) -943, (double) 50234230.4, (byte) 25, (byte) 25);
    }

    private void sendData(ByteBuffer packet) throws Exception {
        loader.fire(config);
//        verify(log).info("Starting Up {}", "Sigea Msg Acquisition");
        UdpInetUtil.createUdpInetChannel(new InetSocketAddress(0)).get()
                .send(packet, new InetSocketAddress("localhost", port));
        Thread.sleep(100); // give the packet a little time to arrive
        // jump ahead to read packet and send health
        ts.advanceTimeBy(30, TimeUnit.SECONDS);
        ts.triggerActions();
    }

    private BatchOfMsgReadings readFirst(ByteBuffer packet) throws Exception {
        sendData(packet);
        return msgReadings.events.blockingFirst();
    }

    private void assertHealthSent(HealthStatus... codes) {
        TestSubscriber code = TestSubscriber.create();
        healthReadings.events.toFlowable(BackpressureStrategy.BUFFER)
                .map(HealthReading::getCode).subscribe(code);
        code.assertValues((Object[]) codes);
//        TestSubscriber type = TestSubscriber.create();
//        health.events.toFlowable(BackpressureStrategy.BUFFER)
//                .map(HealthReading::getComponent).subscribe(type);
//        type.assertValues((Object[]) modules);
    }

    @Test
    public void shouldNotInitialize() {
        ts.advanceTimeBy(30, TimeUnit.SECONDS);
        ts.triggerActions();
        verifyZeroInteractions(log);
        TestSubscriber sub = TestSubscriber.create();
        healthReadings.events.toFlowable(BackpressureStrategy.BUFFER)
                .subscribe(sub);
        sub.assertNoValues();
        sub.dispose();
        msgReadings.events.toFlowable(BackpressureStrategy.BUFFER)
                .subscribe(sub);
        sub.assertNoValues();
    }

    @Test
    public void shouldLogStartupAndShutdown() {
        loader.fire(config);
        verify(log).info("Starting Up {}", "Sigea Msg Acquisition");
        loader.fire(new MsgConfig());
        verify(log).info("Shutting Down {}", "Sigea Msg Acquisition");
    }

    @Test(timeout = 1000)
    public void shouldSendBadDaqHealthOnNoData() throws Exception {
        loader.fire(config);
        verify(log).info("Starting Up {}", "Sigea Msg Acquisition");
        ts.advanceTimeBy(30, TimeUnit.SECONDS);
        ts.triggerActions();
        assertHealthSent(HealthStatus.NOT_OPERATIONAL);
    }

    @Test(timeout = 1000)
    public void shouldIgnoreBadId() throws Exception {
        ByteBuffer packet = createPacket((float) 800.5, (short) 943, 
                (int) 7023, (long) 50234230L, (int)-7023, (long) -50234230L, 
                (short) -943, (double) 50234230.4, (byte) 25, (byte)25);
        sendData(packet);
        TestSubscriber test = TestSubscriber.create();
        msgReadings.events.toFlowable(BackpressureStrategy.BUFFER)
                .subscribe(test);
        test.assertNoValues();
        assertHealthSent(HealthStatus.NOT_OPERATIONAL);
    }

    @Test(timeout = 1000)
    public void shouldReadEmptyIfBadChecksum() throws Exception {
        ByteBuffer packet = createPacket();
        int checkpos = packet.limit() - 2;
        packet.putShort(checkpos, (short) (~packet.getShort(checkpos))); // flip checksum bits

        BatchOfMsgReadings result = readFirst(packet);
        assertTrue(result.getMsgReadings().isEmpty());
        verify(log).warn("Bad CG checksum");
        assertHealthSent(HealthStatus.NOT_OPERATIONAL);
    }

    @Test(timeout = 1000)
    public void shouldReadBadIfFormatIsWrong() throws Exception {
        ByteBuffer packet = createPacket((float) 0, (short) 0, (int) 0, 
                (long) 50234230L, (int) -7023, (long) -50234230L, (short)-943, 
                (double)50234230.4, (byte) 25, (byte) 25);
        int checkpos = packet.limit() - 2;
        packet.putShort(4, packet.getShort(checkpos)); // move up the checksum
        packet.limit(6); // strip all but header and checksum

        BatchOfMsgReadings result = readFirst(packet);
//        assertEquals(DaqType.CG, result.getDaqType());
        assertEquals(9, result.getMsgReadings().size());
        result.getMsgReadings()
                .forEach(reading -> {
                    assertTrue(Double.isNaN(reading.getValue()));
                    assertEquals(MsgQuality.BAD.byteValue(), reading.getQuality());
                });
        assertHealthSent(HealthStatus.NOT_OPERATIONAL);
    }

    @Test(timeout = 1000)
    public void shouldParseFloat() throws Exception {
        BatchOfMsgReadings result = readFirst(createPacket());
//        assertEquals(DaqType.CG, result.getDaqType());
        MsgReading reading = result.getMsgReadings().get(0);
//        System.out.printf("reading %s", reading.getMsgKey()).println();
        assertEquals("test:float", reading.getMsgKey());
//        System.out.printf("fvalue %s", reading.getValue()).println();
        assertEquals(800.5, reading.getValue(), .1);
        assertHealthSent(HealthStatus.OPERATIONAL);
    }

    @Test(timeout = 1000)
    public void shouldParseUnsignedShort() throws Exception {
        BatchOfMsgReadings result = readFirst(createPacket());
//        assertEquals(DaqType.CG, result.getDaqType());
        MsgReading reading = result.getMsgReadings().get(1);
//        System.out.printf("reading %s", reading.getMsgKey()).println();
        assertEquals("test:unsigned_short", reading.getMsgKey());
//        System.out.printf("usvalue %s", reading.getValue()).println();
        assertEquals(943, reading.getValue(), .1);
        assertHealthSent(HealthStatus.OPERATIONAL);
    }
    
    @Test(timeout = 1000)
    public void shouldParseUnsignedInt() throws Exception {
        BatchOfMsgReadings result = readFirst(createPacket());
//        assertEquals(DaqType.CG, result.getDaqType());
        MsgReading reading = result.getMsgReadings().get(2);
//        System.out.printf("reading %s", reading.getMsgKey()).println();
        assertEquals("test:unsigned_int", reading.getMsgKey());
//        System.out.printf("uivalue %s", reading.getValue()).println();
        assertEquals(7023, reading.getValue(), .1);
        assertHealthSent(HealthStatus.OPERATIONAL);
    }
    
    @Test(timeout = 1000)
    public void shouldParseUnsignedLong() throws Exception {
        BatchOfMsgReadings result = readFirst(createPacket());
//        assertEquals(DaqType.CG, result.getDaqType());
        MsgReading reading = result.getMsgReadings().get(3);
//        System.out.printf("reading %s", reading.getMsgKey()).println();
        assertEquals("test:unsigned_long", reading.getMsgKey());
//        System.out.printf("ulvalue %s", reading.getValue()).println();
        assertEquals(50234230L, reading.getValue(), .1);
        assertHealthSent(HealthStatus.OPERATIONAL);
    }
    
        @Test(timeout = 1000)
    public void shouldParseSignedShort() throws Exception {
        BatchOfMsgReadings result = readFirst(createPacket());
//        assertEquals(DaqType.CG, result.getDaqType());
        MsgReading reading = result.getMsgReadings().get(6);
//        System.out.printf("reading %s", reading.getMsgKey()).println();
        assertEquals("test:signed_short", reading.getMsgKey());
//        System.out.printf("ssvalue %s", reading.getValue()).println();
        assertEquals(-943, reading.getValue(), .1);
        assertHealthSent(HealthStatus.OPERATIONAL);
    }
    
    @Test(timeout = 1000)
    public void shouldParseSignedInt() throws Exception {
        BatchOfMsgReadings result = readFirst(createPacket());
//        assertEquals(DaqType.CG, result.getDaqType());
        MsgReading reading = result.getMsgReadings().get(4);
//        System.out.printf("reading %s", reading.getMsgKey()).println();
        assertEquals("test:signed_int", reading.getMsgKey());
//        System.out.printf("sivalue %s", reading.getValue()).println();
        assertEquals(-7023, reading.getValue(), .1);
        assertHealthSent(HealthStatus.OPERATIONAL);
    }
    
    @Test(timeout = 1000)
    public void shouldParseSignedLong() throws Exception {
        BatchOfMsgReadings result = readFirst(createPacket());
//        assertEquals(DaqType.CG, result.getDaqType());
        MsgReading reading = result.getMsgReadings().get(5);
//        System.out.printf("reading %s", reading.getMsgKey()).println();
        assertEquals("test:signed_long", reading.getMsgKey());
//        System.out.printf("slvalue %s", reading.getValue()).println();
        assertEquals(-50234230L, reading.getValue(), .1);
        assertHealthSent(HealthStatus.OPERATIONAL);
    }
    
    @Test(timeout = 1000)
    public void shouldParseDouble() throws Exception {
        BatchOfMsgReadings result = readFirst(createPacket());
//        assertEquals(DaqType.CG, result.getDaqType());
        MsgReading reading = result.getMsgReadings().get(7);
//        System.out.printf("reading %s", reading.getMsgKey()).println();
        assertEquals("test:double", reading.getMsgKey());
//        System.out.printf("dvalue %s", reading.getValue()).println();
        assertEquals(50234230.4, reading.getValue(), .1);
        assertHealthSent(HealthStatus.OPERATIONAL);
    }

    @Test(timeout = 1000)
    public void shouldParseUnsignedByte() throws Exception {
        BatchOfMsgReadings result = readFirst(createPacket());
//        assertEquals(DaqType.CG, result.getDaqType());
        MsgReading reading = result.getMsgReadings().get(8);
//        System.out.printf("reading %s", reading.getMsgKey()).println();
        assertEquals("test:unsigned_byteorchar", reading.getMsgKey());
//        System.out.printf("ubcvalue %s", reading.getValue()).println();
        assertEquals(25, reading.getValue(), .1);
        assertHealthSent(HealthStatus.OPERATIONAL);
    }

    @Test(timeout = 1000)
    public void shouldParseBool() throws Exception {
        BatchOfMsgReadings result = readFirst(createPacket());
        MsgReading bool_0 = result.getMsgReadings().get(9);
        System.out.printf("reading %s", bool_0.getMsgKey()).println();
        MsgReading bool_1 = result.getMsgReadings().get(10);
        MsgReading bool_2 = result.getMsgReadings().get(11);
        MsgReading bool_3 = result.getMsgReadings().get(12);
        MsgReading bool_4 = result.getMsgReadings().get(13);
        MsgReading bool_5 = result.getMsgReadings().get(14);
        MsgReading bool_6 = result.getMsgReadings().get(15);
        MsgReading bool_7 = result.getMsgReadings().get(16);
        assertEquals("test:bool_0", bool_0.getMsgKey());
//        System.out.printf("b0value %s", bool_0.getValue()).println();
        assertEquals(0, bool_0.getValue(), .1);
        assertEquals("test:bool_1", bool_1.getMsgKey());
//        System.out.printf("b1value %s", bool_1.getValue()).println();
        assertEquals(0, bool_1.getValue(), .1);
        assertEquals("test:bool_2", bool_2.getMsgKey());
//        System.out.printf("b2value %s", bool_2.getValue()).println();
        assertEquals(0, bool_2.getValue(), .1);
        assertEquals("test:bool_3", bool_3.getMsgKey());
//        System.out.printf("b3value %s", bool_3.getValue()).println();
        assertEquals(1, bool_3.getValue(), .1);
        assertEquals("test:bool_4", bool_4.getMsgKey());
//        System.out.printf("b4value %s", bool_4.getValue()).println();
        assertEquals(1, bool_4.getValue(), .1);
        assertEquals("test:bool_5", bool_5.getMsgKey());
        assertEquals(0, bool_5.getValue(), .1);
        assertEquals("test:bool_6", bool_6.getMsgKey());
        assertEquals(0, bool_6.getValue(), .1);
        assertEquals("test:bool_7", bool_7.getMsgKey());
        assertEquals(1, bool_7.getValue(), .1);
        assertHealthSent(HealthStatus.OPERATIONAL);
    }

    @Test
    public void shouldAllowMultipleConnectionsOnSamePort() throws Exception {
        MsgConnection mc = new MsgConnection();
        config.getMsgConnections().add(mc);
        mc.setNameId("testConn2");
        mc.setPort(port);
        for (int i = 0; i < config.getMsgs().size(); i += 2) {
            config.getMsgs().get(i).setMsgConn(mc.getNameId());
        }
        loader.fire(config);
        // bind exception occurs when a port can't be reopened
        verify(log, times(0)).debug(any(), any(BindException.class));
    }
}
