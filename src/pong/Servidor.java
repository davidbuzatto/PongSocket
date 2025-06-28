package pong;

import br.com.davidbuzatto.jsge.core.engine.EngineFrame;
import br.com.davidbuzatto.jsge.math.Vector2;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import pong.componentes.Bola;
import pong.componentes.Jogador;
import pong.componentes.TipoJogador;

/**
 * Servidor.
 * 
 * @author Prof. Dr. David Buzatto
 */
public class Servidor extends EngineFrame {
    
    private int porta;
    private DatagramSocket servidor;
    
    private boolean executando;
    private long tempoEspera;
    private int tamanhoBuffer;
    
    private Bola bola;
    private Jogador jogador1;
    private Jogador jogador2;
    
    public Servidor( int porta, long tempoEspera, int tamanhoBuffer ) {
        super( 800, 450, "Pong - Servidor", 60, true );
        this.porta = porta;
        this.tempoEspera = tempoEspera;
        this.tamanhoBuffer = tamanhoBuffer;
        iniciarServidor();
    }
    
    @Override
    public void create() {
        
        bola = new Bola( 
            new Vector2( getScreenWidth() / 2, getScreenHeight() / 2 ), 
            15, 
            new Vector2( 200, 200 ), 
            WHITE
        );
        
        jogador1 = new Jogador( 
            new Vector2(),
            new Vector2(),
            new Vector2(),
            WHITE,
            TipoJogador.SERVIDOR
        );
        
        jogador2 = new Jogador( 
            new Vector2(),
            new Vector2(),
            new Vector2(),
            WHITE,
            TipoJogador.SERVIDOR
        );
        
    }
    
    @Override
    public void update( double delta ) {
        bola.atualizar( this, delta );
    }
    
    @Override
    public void draw() {
        clearBackground( BLACK );
        bola.desenhar( this );
        jogador1.desenhar( this );
        jogador2.desenhar( this );
    }
    
    private void iniciarServidor() {
        if ( !executando ) {
            executando = true;
            try {
                servidor = new DatagramSocket( porta );
                iniciarThreadDados();
            } catch ( SocketException exc ) {
                exc.printStackTrace();
            }
        }
    }
    
    private void iniciarThreadDados() {
        
        new Thread( () -> {
            while ( executando ) {
                try {
                    
                    DatagramPacket pacoteRecebido = DatagramUtils.receberDados( servidor, tamanhoBuffer );
                    String dadosRececebidos = DatagramUtils.extrairDados( pacoteRecebido );
                    
                    String[] d = dadosRececebidos.split( ";" );
                    int tipoJogador = Integer.parseInt( d[0] );
                    int x = Integer.parseInt( d[1] );
                    int y = Integer.parseInt( d[2] );
                    int larg = Integer.parseInt( d[3] );
                    int alt = Integer.parseInt( d[4] );
                    
                    String dadosAdversario = "";
                    
                    if ( tipoJogador == 1 ) {
                        jogador1.pos.x = x;
                        jogador1.pos.y = y;
                        jogador1.dim.x = larg;
                        jogador1.dim.y = alt;
                        dadosAdversario = String.format( 
                            "%d;%d;%d;%d",
                            (int) jogador2.pos.x,
                            (int) jogador2.pos.y,
                            (int) jogador2.dim.x,
                            (int) jogador2.dim.y
                        );
                    } else if ( tipoJogador == 2 ) {
                        jogador2.pos.x = x;
                        jogador2.pos.y = y;
                        jogador2.dim.x = larg;
                        jogador2.dim.y = alt;
                        dadosAdversario = String.format( 
                            "%d;%d;%d;%d",
                            (int) jogador1.pos.x,
                            (int) jogador1.pos.y,
                            (int) jogador1.dim.x,
                            (int) jogador1.dim.y
                        );
                    }
                    
                    DatagramUtils.enviarDados( 
                        servidor, 
                        pacoteRecebido, 
                        String.format( 
                            "%d;%d;%d;%s", 
                            (int) bola.pos.x, 
                            (int) bola.pos.y,
                            bola.raio,
                            dadosAdversario
                        )
                    );
                    
                    Thread.sleep( tempoEspera );
                    
                } catch ( InterruptedException | IOException exc ) {
                    System.out.println( "falha ao receber/enviar (servidor)" );
                }
            }
        }).start();
        
    }
    
}
 