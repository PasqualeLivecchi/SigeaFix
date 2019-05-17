package sigea.simulation;

import java.net.InetSocketAddress;
import java.net.StandardProtocolFamily;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;

import io.vavr.collection.Stream;
import io.vavr.control.Try;

public class UdpUtil {

    public static Try<DatagramChannel> open(int localAddress) {
        return Try.of(() -> {
            return DatagramChannel.open(StandardProtocolFamily.INET)
                    .bind(new InetSocketAddress(localAddress));
        });
    }

    public static Try<DatagramChannel> connect(String hostIp, int remoteAddress, int localAddress) {
        return open(localAddress).mapTry(udp -> udp.connect(new InetSocketAddress(hostIp, remoteAddress)));
    }

    public static int checksum(ByteBuffer buffer, int headerLength, int msgLength) {
        return Stream.ofAll(buffer.array())
                .drop(headerLength)
                .take(msgLength)
                .map(Byte::toUnsignedLong)
                .sum()
                .shortValue();
    }
}
