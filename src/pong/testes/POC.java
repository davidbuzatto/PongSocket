package pong.testes;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;

/**
 * Para o jogo de ping-pong, terei que ter:
 * 
 * Um servidor que deve gerenciar a bolinha, executando a simulação.
 * O servidor é responsável em calcular a posição da bolinha e suas colisões
 * com os jogadores e com os limites da quadra/mesa do jogo.
 * 
 * Os clientes enviam periodicamente suas posições ao servidor, que os responde
 * com a posição atual da bolinha e o estado do jogo, ou seja, se ainda 
 * está executando ou se algum jogador fez ponto naquele momento.
 * 
 * Caso algum jogador tenha feito ponto, o servidor para o jogo e aguarda o
 * jogador que sofreu o ponto recomeçar. Novamente, outro comando enviado
 * para o servidor, a partir do cliente, para iniciar outro ciclo de jogo/pontuação.
 * 
 * O servidor que deve gerenciar todo o jogo, inclusive a pontuação entre os jogadores. 
 * 
 * @author Prof. Dr. David Buzatto
 */
public class POC {
    
    private static int idsClientes;
    private static int idsServidores;
    private static final int BUFF_SIZE = 1024 * 2;
    private int id;
    private int contador;
    
    private int porta;
    private String host;
    private InetAddress iAddr;
    
    private DatagramSocket servidor;
    private DatagramSocket cliente;
    
    public POC( boolean executarComoServidor ) {
        
        this.porta = 8888;
        this.host = "localhost";
        
        try {
            
            this.iAddr = InetAddress.getByName( this.host );
            
            if ( executarComoServidor ) {
                this.id = idsServidores++;
                this.servidor = new DatagramSocket( this.porta, this.iAddr );
                iniciarThreadServidor();
            } else {
                this.id = idsClientes++;
                this.cliente = new DatagramSocket();
                iniciarThreadEnvioCliente();
                iniciarThreadRecebimentoCliente();
            }
            
        } catch ( IOException exc ) {
            exc.printStackTrace();
        }
        
        
        
    }
    
    private void iniciarThreadEnvioCliente() {
        
        new Thread( () -> {
            while ( true ) {
                if ( cliente != null ) {
                    try {
                        String dados = String.format( "teste %d do cliente %d", contador++, id );
                        byte[] buff = dados.getBytes( StandardCharsets.UTF_8 );
                        DatagramPacket p = new DatagramPacket( buff, buff.length, iAddr, porta );
                        cliente.send( p );
                        System.out.printf( "cliente %d - enviado: %s\n", id, dados );
                    } catch ( IOException exc ) {
                        exc.printStackTrace();
                    }
                }
                try {
                    Thread.sleep( 500 );
                } catch ( InterruptedException exc ) {
                    exc.printStackTrace();
                }
            }
        }).start();
        
    }
    
    private void iniciarThreadRecebimentoCliente() {
        
        new Thread( () -> {
            while ( true ) {
                if ( cliente != null ) {
                    try {
                        byte[] buff = new byte[BUFF_SIZE];
                        DatagramPacket p = new DatagramPacket( buff, buff.length );
                        cliente.receive( p );
                        String dados = new String( p.getData(), 0, p.getLength(), StandardCharsets.UTF_8 );
                        System.out.printf( "cliente %d - recebido: %s\n", id, dados );
                    } catch ( IOException exc ) {
                        exc.printStackTrace();
                    }
                }
                try {
                    Thread.sleep( 500 );
                } catch ( InterruptedException exc ) {
                    exc.printStackTrace();
                }
            }
        }).start();
        
    }
    
    private void iniciarThreadServidor() {
        
        new Thread( () -> {
            while ( true ) {
                if ( servidor != null ) {
                    try {
                        
                        byte[] buff = new byte[BUFF_SIZE];
                        DatagramPacket p = new DatagramPacket( buff, buff.length );
                        servidor.receive( p );
                        String dados = new String( p.getData(), 0, p.getLength(), StandardCharsets.UTF_8 );
                        System.out.printf( "servidor %d - recebido: %s\n", id, dados );
                        
                        String dadosCliente = dados.substring( dados.indexOf( "cliente" ) );
                        String resposta = String.format( "%s, recebi seus dados: %s", dadosCliente, dados );
                        byte[] respBuff = resposta.getBytes( StandardCharsets.UTF_8 );
                        DatagramPacket respP = new DatagramPacket( respBuff, respBuff.length, p.getAddress(), p.getPort() );
                        servidor.send( respP );
                        System.out.printf( "servidor %d - enviado: %s\n", id, resposta );
                        
                    } catch ( IOException exc ) {
                        exc.printStackTrace();
                    }
                }
                try {
                    Thread.sleep( 500 );
                } catch ( InterruptedException exc ) {
                    exc.printStackTrace();
                }
            }
        }).start();
        
    }
    
    public static void main( String[] args ) {
        
        new POC( true );
        new POC( false );
        new POC( false );
        
    }
    
}
