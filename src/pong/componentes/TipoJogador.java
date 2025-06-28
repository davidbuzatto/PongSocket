package pong.componentes;

/**
 * Tipo do jogador.
 * 
 * @author Prof. Dr. David Buzatto
 */
public enum TipoJogador {
    
    UM( 1 ),
    DOIS( 2 ),
    SERVIDOR( 3 );
    
    public int valor;
    
    TipoJogador( int valor ) {
        this.valor = valor;
    }

    public int getValor() {
        return valor;
    }
    
}
