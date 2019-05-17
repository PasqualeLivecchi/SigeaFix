package sigea.main;

import java.net.*;
import java.nio.channels.DatagramChannel;
import java.util.Enumeration;
import io.vavr.control.Try;

/**
 * Utility class for UDP operations
 * @author Pasquale Livecchi
 */
public class UdpInetUtil {
    
    /**
     * Creates a DatagramChannel and binds to the provided address
     * @param address
     * @return 
     */
    public static Try<DatagramChannel> createUdpInetChannel(SocketAddress address) {
        return Try.of(() -> DatagramChannel.open(StandardProtocolFamily.INET).bind(address));
    }
    
    /**
     * Gets the network interface for an address
     * @param addr
     * @return 
     */
    public static Try<NetworkInterface> obtainNicForAddress(InetSocketAddress addr) {
        return Try.of(() -> {
            Enumeration<NetworkInterface> nics = NetworkInterface.getNetworkInterfaces();
            while (nics.hasMoreElements()) {
                NetworkInterface nic = nics.nextElement();
                boolean matches = nic.getInterfaceAddresses().stream()
                        .map(InterfaceAddress::getAddress)
                        .anyMatch(ip -> ip.equals(addr.getAddress()));
                if (matches) {
                    return nic;
                }
            }
            throw new RuntimeException("Unable to find NIC for " + addr);
        });
    }
    
    /**
     * Utility method to return an unused random UDP port by creating a
     * channel then closing it and returning the assigned address
     * @return 
     */
    public static int findRandomUnusedUdpPort() {
        return createUdpInetChannel(new InetSocketAddress(0))
                .mapTry(chan -> {
                    int res = ((InetSocketAddress) chan.getLocalAddress()).getPort();
                    chan.close();
                    return res;
                })
                .get();
    }
}
