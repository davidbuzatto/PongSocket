package template;

import br.com.davidbuzatto.jsge.core.engine.EngineFrame;
import br.com.davidbuzatto.jsge.math.Vector2;
import java.awt.Color;

/**
 *
 * @author Prof. Dr. David Buzatto
 */
public class Bola {
    
    public Vector2 pos;
    public Vector2 vel;
    public double raio;
    public Color cor;

    public Bola( Vector2 pos, Vector2 vel, double raio, Color cor ) {
        this.pos = pos;
        this.vel = vel;
        this.raio = raio;
        this.cor = cor;
    }
    
    public void atualizar( EngineFrame ef, double delta ) {
        
        pos.x += vel.x * delta;
        pos.y += vel.y * delta;
        
        if ( pos.x - raio < 0 ) {
            pos.x = raio;
            vel.x = -vel.x;
        } else if ( pos.x + raio > ef.getScreenWidth() ) {
            pos.x = ef.getScreenWidth() - raio;
            vel.x = -vel.x;
        }
        
        if ( pos.y - raio < 0 ) {
            pos.y = raio;
            vel.y = -vel.y;
        } else if ( pos.y + raio > ef.getScreenHeight() ) {
            pos.y = ef.getScreenHeight() - raio;
            vel.y = -vel.y;
        }
        
    }
    
    public void desenhar( EngineFrame ef ) {
        ef.fillCircle( pos, raio, cor );
    }
    
}
