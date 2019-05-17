package sigea.simulation;

import sigea.main.DataPrimitiveUtil;
import sigea.entities.MsgConfig;
import sigea.entities.MsgReading;
import sigea.entities.Message;
import sigea.entities.MsgConnection;
import io.reactivex.functions.Consumer;
import io.vavr.Tuple;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.DatagramChannel;

import io.vavr.collection.Map;
import io.vavr.collection.Seq;
import io.vavr.collection.Stream;
import io.vavr.control.Option;
import io.vavr.control.Try;

public class MsgSimPanel extends SimulationPanel {

    private static class MsgSim implements Consumer<Stream<MsgReading>> {

        private final String hostIp;
        private final Map<String, Message> messageMap;
        private final Map<MsgConnection, Integer> bufSizeMap;
        private final DatagramChannel udpChan;
        private final MsgConfig msgCfg;

        public MsgSim(MsgConfig msgCfg, DatagramChannel udpChan, String hostIp) {
            this.msgCfg = msgCfg;
            this.hostIp = hostIp;
            this.udpChan = udpChan;
            messageMap = Stream.ofAll(msgCfg.getMsgs()).toMap(msg -> Tuple.of(msg.uniqueKeyName(), msg));
            bufSizeMap = messageMap.values()
                    .groupBy(msg -> getMsgConnection(msg.getMsgConn()))
                    .mapValues(this::bufferSize);
        }

        private MsgConnection getMsgConnection(String msgConnName) {
            return msgCfg.getMsgConnections().stream()
                    .filter(mc -> mc.getNameId().equalsIgnoreCase(msgConnName))
                    .findAny().get();
        }

        private void writeToBuffer(ByteBuffer buf, Message msg, double value) {
            String dataType = msg.getDataType();
            int byteOffset = msg.getByteOffset();
            int bitOffset = msg.getBitOffset();
            switch (dataType) {
                case "U1":
                    byte v = (byte) value;
                    int bitSize = 1;
                    // mask with bit size e.g. 1111 if len = 2
                    v &= (1 << bitSize) - 1; // mask lowest n bits
                    int shift = 8 - (bitSize - bitOffset); // lowest unused bits
                    v <<= shift; // shift down to match mask
                    buf.put(byteOffset, (byte) (v | buf.get(byteOffset)));
                    break;
                case "U8":
                    buf.put(byteOffset, (byte) value);
                    break;
                case "U16":
                case "S16":
                    buf.putShort(byteOffset, (short) value);
                    break;
                case "U32":
                case "S32":
                    buf.putInt(byteOffset, (int) value);
                    break;
                case "U64":
                case "S64":
                    buf.putLong(byteOffset, (long) value);
                    break;
                case "F32":
                    buf.putFloat(byteOffset, Float.floatToRawIntBits((float) value));
                    break;
                case "F64":
                    buf.putDouble(byteOffset, Double.doubleToRawLongBits((double) value));
                    break;
                default:
                    throw new RuntimeException("Invalid Data Type");
            }
        }

        private int bufferSize(Seq<Message> msgs) {
            int highestBit = msgs
                    .map(m -> m.getByteOffset() * 8 + m.getBitOffset() + DataPrimitiveUtil.dataTypeBitSize(m.getDataType()))
                    .max().getOrElse(0);
            //+2 for checksum 
            return (int) Math.ceil(highestBit / 8.0) + 2;
        }

        private Option<SimulationValue<Message>> mapMsgReadingToSimVal(MsgReading msgReading) {
            return messageMap.get(msgReading.getMsgKey())
                    .map(msg -> new SimulationValue<>(msg, msgReading.getValue()));
        }

        @Override
        public void accept(Stream<MsgReading> msgReadings) throws Exception {
            msgReadings.flatMap(this::mapMsgReadingToSimVal)
                    .groupBy(sv -> sv.msg.getMsgConn())
                    .forEach((msgConnName, simValSeq) -> {
                        MsgConnection msgConn = getMsgConnection(msgConnName);
                        int bufSizeInBytes = bufSizeMap.get(msgConn).get();
                        ByteBuffer buf = ByteBuffer.allocate(bufSizeInBytes)
                                .order(ByteOrder.LITTLE_ENDIAN);
                        simValSeq.forEach(sv -> writeToBuffer(buf, sv.getMsg(), sv.getVal()));
//                        System.out.printf("DOES THIS SHIT GET CALLED???").println();
                        checksum(buf, bufSizeInBytes - 2);
                        Try.run(() -> udpChan.send(buf, new InetSocketAddress(hostIp, msgConn.getPort())));
                    });
        }

        private void checksum(ByteBuffer buffer, int bufLengthWithoutChecksum) {
            short checksum = (short) UdpUtil.checksum(buffer, 0, bufLengthWithoutChecksum);
            buffer.putShort(bufLengthWithoutChecksum, checksum);
        }
    }

    @Override
    protected Consumer<Stream<MsgReading>> initMsgSimulationStream(MsgConfig msgCfg, DatagramChannel chan, String hostIp) {
        return new MsgSim(msgCfg, chan, hostIp);
    }
}
