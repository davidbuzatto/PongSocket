package pong;

import br.com.davidbuzatto.jsge.core.engine.EngineFrame;
import static br.com.davidbuzatto.jsge.core.engine.EngineFrame.WHITE;
import br.com.davidbuzatto.jsge.math.Vector2;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import pong.componentes.Bola;
import pong.componentes.Jogador;
import pong.componentes.TipoJogador;

/**
 * Cliente.
 * 
 * @author Prof. Dr. David Buzatto
 */
public class Cliente extends EngineFrame {
    
    private static int idCounter;
    
    private int id;
    
    private int porta;
    private String host;
    private InetAddress iAddr;
    private DatagramSocket cliente;
    
    private boolean executando;
    private long tempoEspera;
    private int tamanhoBuffer;
    
    private Bola bola;
    private Jogador jogador;
    private TipoJogador tipo;
    private Jogador adversario;
    
    public Cliente( int porta, String host, long tempoEspera, int tamanhoBuffer, TipoJogador tipo ) {
        super( 800, 450, "Pong - Cliente", 60, true );
        this.id = idCounter++;
        this.porta = porta;
        this.host = host;
        this.tempoEspera = tempoEspera;
        this.tamanhoBuffer = tamanhoBuffer;
        this.tipo = tipo;
        iniciarCliente();
    }
    
    @Override
    public void create() {
        
        bola = new Bola( 
            new Vector2( getScreenWidth() / 2, getScreenHeight() / 2 ), 
            0, 
            new Vector2( 0, 0 ), 
            WHITE
        );
        
        adversario = new Jogador( 
            new Vector2(),
            new Vector2(),
            new Vector2(),
            WHITE,
            TipoJogador.SERVIDOR
        );
        
    }
    
    @Override
    public void update( double delta ) {
        
        criarJogador();
        
        jogador.atualizar( this, delta );
        enviarDados();
        
    }
    
    @Override
    public void draw() {
        clearBackground( BLACK );
        bola.desenhar( this );
        jogador.desenhar( this );
        adversario.desenhar( this );
    }
    
    private void criarJogador() {
        
        if ( jogador == null ) {
            
            int largJogador = 30;
            int altJogador = 150;
        
            if ( tipo == TipoJogador.UM ) {
                jogador = new Jogador(
                    new Vector2( largJogador, getScreenHeight() / 2 - altJogador / 2 ), 
                    new Vector2( largJogador, altJogador ), 
                    new Vector2( 0, 200 ), 
                    WHITE, 
                    tipo
                );
            } else if ( tipo == TipoJogador.DOIS ) {
                jogador = new Jogador(
                    new Vector2( getScreenWidth() - largJogador * 2, getScreenHeight() / 2 - altJogador / 2 ), 
                    new Vector2( largJogador, altJogador ), 
                    new Vector2( 0, 200 ), 
                    WHITE, 
                    tipo
                );
            }
        }
        
    }
    
    private void iniciarCliente() {
        if ( !executando ) {
            executando = true;
            try {
                iAddr = InetAddress.getByName( host );
                cliente = new DatagramSocket();
                iniciarThreadEnvio();
                iniciarThreadRecebimento();
            } catch ( SocketException | UnknownHostException exc ) {
                exc.printStackTrace();
            }
        }
    }
    
    private void enviarDados() {
        
        if ( cliente != null ) {
            
            try {
                DatagramUtils.enviarDados( 
                    cliente, 
                    iAddr, 
                    porta, 
                    String.format( 
                        "%d;%d;%d;%d;%d", 
                        jogador.tipo.valor,
                        (int) jogador.pos.x,
                        (int) jogador.pos.y,
                        (int) jogador.dim.x,
                        (int) jogador.dim.y
                    )
                );
            } catch ( IOException exc ) {
                System.out.println( "falha" );
            }
        
        }
        
    }
    
    private void iniciarThreadEnvio() {
        
        /*new Thread( () -> {
            while ( executando ) {
                try {
                    DatagramUtils.enviarDados( 
                        cliente, 
                        iAddr, 
                        porta, 
                        String.format( 
                            "%d;%d;%d;%d;%d", 
                            jogador.tipo.valor,
                            (int) jogador.pos.x,
                            (int) jogador.pos.y,
                            (int) jogador.dim.x,
                            (int) jogador.dim.y
                        )
                    );
                    Thread.sleep( tempoEspera );
                } catch ( InterruptedException | IOException exc ) {
                    System.out.println( "falha ao enviar (cliente)" );
                }
            }
        }).start();*/
        
    }
    
    private void iniciarThreadRecebimento() {
        
        new Thread( () -> {
            while ( executando ) {
                try {
                    
                    DatagramPacket pacoteRecebido = DatagramUtils.receberDados( cliente, tamanhoBuffer );
                    String dadosRececebidos = DatagramUtils.extrairDados( pacoteRecebido );
                    
                    String[] d = dadosRececebidos.split( ";" );
                    bola.pos.x = Integer.parseInt( d[0] );
                    bola.pos.y = Integer.parseInt( d[1] );
                    bola.raio = Integer.parseInt( d[2] );
                    
                    adversario.pos.x = Integer.parseInt( d[3] );
                    adversario.pos.y = Integer.parseInt( d[4] );
                    adversario.dim.x = Integer.parseInt( d[5] );
                    adversario.dim.y = Integer.parseInt( d[6] );
                    
                    Thread.sleep( tempoEspera );
                    
                } catch ( InterruptedException | IOException exc ) {
                    System.out.println( "falha ao receber (cliente)" );
                }
            }
        }).start();
        
    }
    
}
 