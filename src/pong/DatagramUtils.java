package pong;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;

/**
 * Métodos utilitários para ligar com os sockets e pacotes de datagrama.
 * 
 * @author Prof. Dr. David Buzatto
 */
public class DatagramUtils {
    
    public static DatagramPacket receberDados( DatagramSocket socket, int tamanhoBuffer ) throws IOException {
        byte[] buffer = new byte[tamanhoBuffer];
        DatagramPacket packet = new DatagramPacket( buffer, buffer.length );
        socket.receive( packet ); // bloqueante
        return packet;
    }
    
    public static void enviarDados( DatagramSocket socket, DatagramPacket pacoteOrigem, String dados ) throws IOException {
        byte[] buffer = dados.getBytes( StandardCharsets.UTF_8 );
        DatagramPacket packet = new DatagramPacket( buffer, buffer.length, pacoteOrigem.getAddress(), pacoteOrigem.getPort() );
        socket.send( packet );
    }
    
    public static void enviarDados( DatagramSocket socket, InetAddress iAddr, int porta, String dados ) throws IOException {
        byte[] buffer = dados.getBytes( StandardCharsets.UTF_8 );
        DatagramPacket packet = new DatagramPacket( buffer, buffer.length, iAddr, porta );
        socket.send( packet );
    }
    
    public static String extrairDados( DatagramPacket packet ) {
        return new String( packet.getData(), 0, packet.getLength(), StandardCharsets.UTF_8 );
    }
    
}
