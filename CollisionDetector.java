package cylindricalarm;

import com.sun.j3d.utils.geometry.Box;
import java.util.Enumeration;
import javax.media.j3d.*;

public class CollisionDetector extends Behavior{
    public boolean collision = false;
    private WakeupOnCollisionEntry collisionEnter;
    private WakeupOnCollisionExit collisionExit;
    private Box box;
    
    public CollisionDetector(Box shape){
        box = shape;
    }
    
    @Override
    public void initialize() {
        collisionEnter = new WakeupOnCollisionEntry(box, WakeupOnCollisionEntry.USE_GEOMETRY );
        collisionExit = new WakeupOnCollisionExit(box, WakeupOnCollisionEntry.USE_GEOMETRY);
        wakeupOn(collisionEnter);
    }

    @Override
    public void processStimulus(Enumeration enmrtn) {          
        collision = !collision;
        if (collision){
            wakeupOn(collisionExit);
        }
        else{
            wakeupOn(collisionEnter);
        }
    } 
}
