package pong;

import pong.componentes.TipoJogador;

/**
 *
 * @author Prof. Dr. David Buzatto
 */
public class Pong {
    
    public static void main( String[] args ) {
        
        int porta = 80;
        int tamanhoBuffer = 1024;
        //long tempoEspera = (int) ( 1000.0 / 60 ); // 60 fps
        long tempoEspera = 15;
        String host = "localhost";
        
        Servidor s = new Servidor( porta, tempoEspera, tamanhoBuffer );
        Cliente c1 = new Cliente( porta, host, tempoEspera, tamanhoBuffer, TipoJogador.UM );
        Cliente c2 = new Cliente( porta, host, tempoEspera, tamanhoBuffer, TipoJogador.DOIS );
        
        s.setLocation( s.getLocation().x, s.getLocation().y - s.getSize().height / 2 );
        c1.setLocation( c1.getLocation().x - c1.getSize().width / 2, c1.getLocation().y + c1.getSize().height / 2 );
        c2.setLocation( c2.getLocation().x + c2.getSize().width / 2, c2.getLocation().y + c2.getSize().height / 2 );
    
    }
    
}
