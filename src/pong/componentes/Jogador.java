package pong.componentes;

import br.com.davidbuzatto.jsge.core.engine.EngineFrame;
import br.com.davidbuzatto.jsge.math.Vector2;
import java.awt.Color;

/**
 * Jogador.
 * 
 * @author Prof. Dr. David Buzatto
 */
public class Jogador extends Componente {

    public TipoJogador tipo;
    public double velY;
    
    public Jogador( Vector2 pos, Vector2 dim, Vector2 vel, Color cor, TipoJogador tipo ) {
        this.pos = pos;
        this.dim = dim;
        this.vel = vel;
        this.cor = cor;
        this.tipo = tipo;
    }
    
    @Override
    public void atualizar( EngineFrame e, double delta ) {
        
        if ( e.isKeyDown( e.KEY_UP ) ) {
            pos.y -= vel.y * delta;
        }
        
        if ( e.isKeyDown( e.KEY_DOWN ) ) {
            pos.y += vel.y * delta;
        }
        
        if ( pos.y < 0 ) {
            pos.y = 0;
        } else if ( pos.y + dim.y > e.getScreenHeight() ) {
            pos.y = e.getScreenHeight() - dim.y;
        }
        
    }

    @Override
    public void desenhar( EngineFrame e ) {
        e.fillRectangle( pos, dim, cor );
    }
    
}
