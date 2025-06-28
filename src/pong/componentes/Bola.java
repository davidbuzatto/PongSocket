package pong.componentes;

import br.com.davidbuzatto.jsge.core.engine.EngineFrame;
import br.com.davidbuzatto.jsge.math.Vector2;
import java.awt.Color;

/**
 * Bola.
 * 
 * @author Prof. Dr. David Buzatto
 */
public class Bola extends Componente {
    
    public int raio;

    public Bola( Vector2 pos, int raio, Vector2 vel , Color cor ) {
        this.pos = pos;
        this.raio = raio;
        this.vel = vel;
        this.cor = cor;
    }

    @Override
    public void atualizar( EngineFrame e, double delta ) {
        
        pos.x += vel.x * delta;
        pos.y += vel.y * delta;
        
        if ( pos.x - raio < 0 ) {
            pos.x = raio;
            vel.x = -vel.x;
        } else if ( pos.x + raio > e.getScreenWidth() ) {
            pos.x = e.getScreenWidth() - raio;
            vel.x = -vel.x;
        }
        
        if ( pos.y - raio < 0 ) {
            pos.y = raio;
            vel.y = -vel.y;
        } else if ( pos.y + raio > e.getScreenHeight() ) {
            pos.y = e.getScreenHeight() - raio;
            vel.y = -vel.y;
        }
        
    }

    @Override
    public void desenhar( EngineFrame e ) {
        e.fillCircle( pos, raio, cor );
    }
    
}
