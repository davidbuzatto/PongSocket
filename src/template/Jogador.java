package template;

import br.com.davidbuzatto.jsge.core.engine.EngineFrame;
import br.com.davidbuzatto.jsge.math.Vector2;
import java.awt.Color;

/**
 *
 * @author Prof. Dr. David Buzatto
 */
public class Jogador {
    
    public Vector2 pos;
    public Vector2 dim;
    public double vel;
    public Color cor;
    
    public TipoJogador tipo;

    public Jogador( Vector2 pos, Vector2 dim, double vel, Color cor, TipoJogador tipo ) {
        this.pos = pos;
        this.dim = dim;
        this.vel = vel;
        this.cor = cor;
        this.tipo = tipo;
    }
    
    public void atualizar( EngineFrame ef, double delta ) {
        
        if ( ef.isKeyDown( EngineFrame.KEY_UP ) ) {
            pos.y -= vel * delta;
        } else if ( ef.isKeyDown( EngineFrame.KEY_DOWN ) ) {
            pos.y += vel * delta;
        }
        
        if ( pos.y < 0 ) {
            pos.y = 0;
        } else if ( pos.y + dim.y > ef.getScreenHeight() ) {
            pos.y = ef.getScreenHeight() - dim.y;
        }
        
    }
    
    public void desenhar( EngineFrame ef ) {
        ef.fillRectangle( pos, dim, cor );
    }
    
}
