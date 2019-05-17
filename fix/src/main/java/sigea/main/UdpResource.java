package sigea.main;

import sigea.entities.UdpDatagram;
import io.reactivex.Observable;
import java.io.IOException;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.util.Arrays;
import io.vavr.control.Try;

/**
 * * This class creates an Observable[UdpDatagram] from a Udp DatagramChannel
 * connection * @author Pasquale Livecchi
 */
public class UdpResource implements Resource<UdpDatagram> {

    private static final int BUFFER_SIZE = 64 * 1024;

    @FunctionalInterface
    public static interface IoSupplier<T> {

        T create() throws IOException;
    }

    protected final DatagramChannel datagramChan;
    protected final ByteBuffer buffer;

    private UdpResource(IoSupplier<DatagramChannel> ioChan) throws IOException {
        this.datagramChan = ioChan.create();
        this.buffer = ByteBuffer.allocate(BUFFER_SIZE);
    }

    @Override
    public UdpDatagram next() {
        buffer.clear();
        SocketAddress sockAddress = Try.of(() -> datagramChan.receive(buffer)).get();
        // copy from index 0 to the current position
        byte[] byteData = Arrays.copyOfRange(buffer.array(), 0, buffer.position());
        return new UdpDatagram(sockAddress, ByteBuffer.wrap(byteData));
    }

    @Override
    public void dispose() {
        try {
            datagramChan.close();
        } catch (IOException ex) {
            // close silently
        }
    }

    /**
     * Continuously attempts to read values from the udp io channel
     *
     * @param func
     * @return
     */
    public static Observable<UdpDatagram> openStream(IoSupplier<DatagramChannel> ioChan) {
        return Resource.continuousResourceStream(() -> new UdpResource(ioChan));
    }
}
