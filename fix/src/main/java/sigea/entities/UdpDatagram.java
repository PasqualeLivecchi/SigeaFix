/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package sigea.entities;

import java.net.SocketAddress;
import java.nio.ByteBuffer;
import lombok.Getter;

/**
 * 
 * Container class for udp byte datagrams.
 * @author Pasquale Livecchi
 */
@Getter
public class UdpDatagram {

    private final SocketAddress sourceAddress;
    private final ByteBuffer datagram;

    public UdpDatagram(SocketAddress sourceAddress, ByteBuffer datagram) {
        this.sourceAddress = sourceAddress;
        this.datagram = datagram;
    }
}
