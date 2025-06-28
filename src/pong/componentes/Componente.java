package pong.componentes;

import br.com.davidbuzatto.jsge.core.engine.EngineFrame;
import br.com.davidbuzatto.jsge.math.Vector2;
import java.awt.Color;

/**
 * Componentes do jogo.
 * 
 * @author Prof. Dr. David Buzatto
 */
public abstract class Componente {
    
    public Vector2 pos;
    public Vector2 vel;
    public Vector2 dim;
    public Color cor;
    
    public abstract void atualizar( EngineFrame e, double delta );
    public abstract void desenhar( EngineFrame e );
    
}
