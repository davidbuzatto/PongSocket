package template;

import br.com.davidbuzatto.jsge.core.engine.EngineFrame;
import br.com.davidbuzatto.jsge.math.Vector2;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;

/**
 * Pong.
 * 
 * @author Prof. Dr. David Buzatto
 */
public class Jogo extends EngineFrame {
    
    private EstadoJogo estado;
    
    private TipoJogador tipo;
    private Jogador jogador;
    private Bola bola;
    
    private boolean jogoIniciado;
    
    // pode virar um só, vai depender se é cliente ou servidor
    // usar o mesmo socket para enviar e receber
    private DatagramSocket servidor;
    private DatagramSocket cliente;
    
    public Jogo( TipoJogador tipo ) {
        
        super( 800, 450, "Pong", 60, true );
        this.tipo = tipo;
        
        iniciarJogo();
        this.jogoIniciado = true;
        
        this.estado = EstadoJogo.PARADO;
        
    }
    
    @Override
    public void create() {
    }
    
    @Override
    public void update( double delta ) {
        
        if ( estado == EstadoJogo.PARADO ) {
            
            if ( tipo == TipoJogador.DIREITA ) {
                if ( isKeyPressed( KEY_ENTER ) ) {
                    estado = EstadoJogo.EXECUTANDO;
                }
            }
            
        } else if ( estado == EstadoJogo.EXECUTANDO ) {
        
            if ( jogoIniciado ) {
                jogador.atualizar( this, delta );
                if ( cliente != null ) {
                    bola.atualizar( this, delta );
                }
            }
        
        }
        
    }
    
    @Override
    public void draw() {
        
        clearBackground( BLACK );
        
        if ( jogoIniciado ) {
            jogador.desenhar( this );
            bola.desenhar( this );
        }
        
    }
    
    private void iniciarJogo() {
        
        Vector2 dim = new Vector2( 30, 150 );
        Vector2 pos = new Vector2( 0, getScreenHeight() / 2 - dim.y / 2 );
        
        if ( tipo == TipoJogador.ESQUERDA ) {
            pos.x = dim.x;
        } else {
            pos.x = getScreenWidth()- dim.x * 2;
        }
        
        jogador = new Jogador( pos, dim, 200, WHITE, tipo );
        
        bola = new Bola( 
            new Vector2( getScreenWidth() / 2, getScreenHeight() / 2 ), 
            new Vector2( 200, 200 ),
            15, WHITE
        );
        
        if ( tipo == TipoJogador.ESQUERDA ) {
            try {
                servidor = new DatagramSocket( 8888 );
                iniciarThreadRecebimentoDadosBolinha();
            } catch ( SocketException exc ) {
                exc.printStackTrace();
            }
        } else {
            iniciarThreadConexaoCliente();
        }
        
    }
    
    private void iniciarThreadConexaoCliente() {
        
        new Thread( () -> {
            while ( true ) {
                try {
                    cliente = new DatagramSocket();
                    iniciarThreadEnvioDadosBolinha();
                    break;
                } catch ( IOException exc ) {
                    exc.printStackTrace();
                }
                try {
                    Thread.sleep( 100 );
                } catch ( InterruptedException exc ) {
                    exc.printStackTrace();
                }
            }
        }).start();
        
    }
    
    private void iniciarThreadEnvioDadosBolinha() {
        
        new Thread( () -> {
            while ( true ) {
                if ( cliente != null ) {
                    try {
                        byte[] buff = ( bola.pos.x + " " + bola.pos.y ).getBytes( StandardCharsets.UTF_8 );
                        DatagramPacket p = new DatagramPacket( buff, buff.length, InetAddress.getLocalHost(), 8888 );
                        //System.out.println( "enviando..." );
                        cliente.send( p );
                        //System.out.println( "enviado!" );
                    } catch ( IOException exc ) {
                        exc.printStackTrace();
                    }
                }
                try {
                    Thread.sleep( 10 );
                } catch ( InterruptedException exc ) {
                    exc.printStackTrace();
                }
            }
        }).start();
        
    }
    
    private void iniciarThreadRecebimentoDadosBolinha() {
        
        new Thread( () -> {
            while ( true ) {
                if ( servidor != null ) {
                    try {
                        byte[] buff = new byte[1024];
                        DatagramPacket p = new DatagramPacket( buff, buff.length );
                        //System.out.println( "recebendo..." );
                        servidor.receive( p );
                        String dados = new String( p.getData(), StandardCharsets.UTF_8 );
                        String[] d = dados.split( " " );
                        //System.out.println( "recebido: " + dados );
                        bola.pos.x = Double.parseDouble( d[0] );
                        bola.pos.y = Double.parseDouble( d[1] );
                        estado = EstadoJogo.EXECUTANDO;
                    } catch ( IOException exc ) {
                        exc.printStackTrace();
                    }
                }
                try {
                    Thread.sleep( 10 );
                } catch ( InterruptedException exc ) {
                    exc.printStackTrace();
                }
            }
        }).start();
        
    }
    
    public static void main( String[] args ) {
        
        Jogo jDireita = new Jogo( TipoJogador.DIREITA );
        Jogo jEsquerda = new Jogo( TipoJogador.ESQUERDA );
        
        jDireita.setLocation( jDireita.getLocation().x + jDireita.getScreenWidth() / 2 + 10, jDireita.getLocation().y );
        jEsquerda.setLocation( jEsquerda.getLocation().x - jEsquerda.getScreenWidth() / 2 - 10, jEsquerda.getLocation().y );
        
    }
    
}
