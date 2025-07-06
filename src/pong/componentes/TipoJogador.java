package pong.componentes;

/**
 * Tipo do jogador.
 * 
 * @author Prof. Dr. David Buzatto
 */
public enum TipoJogador {
    
    UM( 1 ),
    DOIS( 2 ),
    SERVIDOR( 0 );
    
    public int valor;
    
    TipoJogador( int valor ) {
        this.valor = valor;
    }

    public int getValor() {
        return valor;
    }
    
    public static TipoJogador fromValor(int valor) {
        for (TipoJogador tipo : TipoJogador.values()) {
            if (tipo.valor == valor) {
                return tipo;
            }
        }
        throw new IllegalArgumentException("Valor de TipoJogador inválido: " + valor);
    }
    
}
