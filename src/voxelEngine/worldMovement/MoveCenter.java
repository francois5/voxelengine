package voxelEngine.worldMovement;

import com.jme3.math.Vector3f;

/**
 *
 * @author francois
 */
public class MoveCenter implements WorldMovement {
    public final Vector3f translation;
    public int iColumn = 0;
    public int iy;
    public MoveCenter(Vector3f translation) {
        this.translation = translation;
    }
    
}
