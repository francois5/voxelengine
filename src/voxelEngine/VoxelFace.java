package voxelEngine;

/**
 * 
 * @author francois
 */
public class VoxelFace {
    
    public boolean transparent;
    public int voxelType;
    public int type;
    public int light;

    public boolean lightOff() {
        if (light > 0) {
            light = 0;
            return true;
        }
        return false;
    }

    public boolean equals(final VoxelFace face) {
        return face.transparent == this.transparent
                && face.type == this.type
                && face.light == this.light;
    }
}