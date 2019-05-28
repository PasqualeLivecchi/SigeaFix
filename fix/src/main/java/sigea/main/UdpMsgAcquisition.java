package sigea.main;

import static io.vavr.API.$;
import static io.vavr.API.Case;
import static io.vavr.API.Match;
import static io.vavr.Predicates.isIn;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.DatagramChannel;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import sigea.entities.BatchOfMsgReadings;
import sigea.entities.Message;
import sigea.entities.MsgConfig;
import sigea.entities.MsgQuality;
import sigea.entities.MsgReading;
import io.reactivex.Observable;
import io.reactivex.ObservableTransformer;
import io.vavr.collection.Seq;
import io.vavr.collection.Stream;
import io.vavr.control.Try;
import java.util.function.Function;
import lombok.Synchronized;
import lombok.extern.slf4j.Slf4j;

/**
 * * * @author Pasquale Livecchi
 */
@Slf4j
@ApplicationScoped
public class UdpMsgAcquisition extends MsgAcquisition {

    private final MsgReadingSubscription msgSub = new MsgReadingSubscription();

    private class UdpMsgInterface implements ObservableMsgInterface {

        private final Seq<Message> msgItems;
        private final int udpPort;
        private Observable<BatchOfMsgReadings> cachedMsgReadings;

        public UdpMsgInterface(int udpPort, Seq<Message> msgs) {
            this.udpPort = udpPort;//UdpInetUtil.findRandomUnusedUdpPort();
            this.msgItems = msgs;
        }

        @Override
        @Synchronized
        public Observable<BatchOfMsgReadings> asObservable() {
            if (cachedMsgReadings == null) {
                cachedMsgReadings = Observable.merge(composeMsgStream());
            }
            return cachedMsgReadings;
        }

        private Seq<Observable<BatchOfMsgReadings>> composeMsgStream() {
            Observable<ByteBuffer> udpPackets = setupUdpPacketStream();
            return Stream.of(msgItems)
                    .map(mi -> linkMsgStreamToUdpPackets(mi))
                    .map(udpPackets::compose);
        }

        private Observable<ByteBuffer> setupUdpPacketStream() {
            return UdpResource.openStream(this::openUdpChannel)
                    .subscribeOn(io)
                    .doOnSubscribe(sub -> log.info("Opening Udp socket connection on port {}", udpPort))
                    .doOnDispose(() -> log.info("Closing Udp socket connection on port {}", udpPort))
                    .map(udpDgram -> copyAsLittleEndianToPreventByteOrderMishaps(udpDgram.getDatagram()))
                    .doOnError(ex -> log.error("Error reading Udp data: {}", ex.getMessage()))
                    .retryWhen(throwable -> throwable.delay(5, TimeUnit.SECONDS, computation))//retry after 5 seconds when there is an error
                    .share(); // share makes sure we don't have multiple resources open and allows async multicast of observable stream
        }

        private DatagramChannel openUdpChannel() throws IOException {
            return UdpInetUtil.createUdpInetChannel(new InetSocketAddress("0.0.0.0", udpPort)).get();
        }

        private ByteBuffer copyAsLittleEndianToPreventByteOrderMishaps(ByteBuffer buf) {
            return ByteBuffer.wrap(buf.array(), 0, buf.limit())
                    .order(ByteOrder.LITTLE_ENDIAN);
        }

        private ObservableTransformer<ByteBuffer, BatchOfMsgReadings> linkMsgStreamToUdpPackets(Seq<Message> msgs) {
            return incomingBatchOfMsgs -> incomingBatchOfMsgs
                    .sample(1, TimeUnit.SECONDS, computation)//one data packet per second (most recent value within 1 second)
                    .map(buf -> parseFromBufferIntoListOfMsgReadings(msgs, buf))
                    .doOnError(ex -> log.error("Error parsing data: {}", ex.getMessage()))
                    .compose(onErrorInsert(Collections.EMPTY_LIST))
                    .retry()
                    .map(msgReadingList -> new BatchOfMsgReadings(msgReadingList));

        }

        private List<MsgReading> parseFromBufferIntoListOfMsgReadings(Seq<Message> msgs, ByteBuffer buf) {
            if (!checksum(buf)) {
                log.warn("Bad msg checksum");
                return Collections.EMPTY_LIST;
            }
            return msgs.map(msg -> readBufferValue(buf, msg)).toJavaList();
        }

        // attempt to read data value and return it
        // returns a NaN with bad quality if an exception occurs
        private MsgReading readBufferValue(ByteBuffer buf, Message msg) {
            return Try.of(() -> value(msg, buf))
                    .map(value -> new MsgReading(msg, value.doubleValue(), MsgQuality.GOOD.byteValue()))
                    .getOrElse(() -> new MsgReading(msg, Double.NaN, MsgQuality.BAD.byteValue()));
        }

        // returns whether or not the checksum matches
        private boolean checksum(ByteBuffer buf) {
            int len = buf.limit() - 2; // checksum everything but the last two bytes
            short sum = 0;
            for (int i = 0; i < len; i++) {
                sum += Byte.toUnsignedLong(buf.get(i));
            }
            short expected = buf.getShort(len);
            // System.out.printf("sum %s, expected %s", sum, expected).println();
            return sum == expected; // compare sum to last two bytes 
        }

    }

    public static double value(Message msg, ByteBuffer buf) {
        String dataType = msg.getDataType();
        int byteOffset = msg.getByteOffset(),
                bitOffset = msg.getBitOffset(),
                bitSize = DataPrimitiveUtil.dataTypeBitSize(dataType);

        // since our biggest value is 32 bits long can fit with an extra byte
        switch (dataType) {
            case "U1":
                int value = Byte.toUnsignedInt(buf.get(byteOffset));
                if (bitSize < 8) {
                    // bit 0 is the highest value bit
                    int shift = 8 - (bitSize + bitOffset); // lowest unused bits
                    value >>>= shift; // shift down to match mask
                }
                value &= (1 << bitSize) - 1; // mask lowest n bits
                return value;
            case "U8":
                return buf.get(byteOffset);
            case "U16":
            case "S16":
                return buf.getShort(byteOffset);
            case "U32":
            case "S32":
                return buf.getInt(byteOffset);
            case "U64":
            case "S64":
                return buf.getLong(byteOffset);
            case "F32":
                return buf.getFloat(byteOffset);
            case "F64":
                return buf.getDouble(byteOffset);
            default:
                throw new RuntimeException("Invalid Data Type");
        }

    }

    @Synchronized
    public void loadMsgs(@Observes MsgConfig msgConfig) {
        loadBatchOfMsgReadings(msgConfig.getMsgs(), msgSub, ms -> msgConfig.getMsgConnections().stream()
                .filter(mc -> mc.getNameId().equalsIgnoreCase(ms.getMsgConn())).findAny().get().getPort(), UdpMsgInterface::new);
    }

}
