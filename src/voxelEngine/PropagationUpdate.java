package voxelEngine;

import com.jme3.math.Vector3f;

/**
 *
 * @author francois
 */
public class PropagationUpdate {
    public Vector3f originTranslation;
    private Vector3f currentTranslation;
    private boolean began = false;

    public PropagationUpdate(Vector3f translation) {
        this.originTranslation = translation;
        int currentY = (((int)originTranslation.y)/16)+1;
        if(currentY == Landscape.systemHeight)
            --currentY;
        this.currentTranslation = new Vector3f(originTranslation.x-(2*Landscape.chunkWidth), currentY*Landscape.chunkHeight, originTranslation.z-(1*Landscape.chunkWidth));
    }
    
    public Vector3f nextTranslation() {
        if(lastLocation(currentTranslation)) {
            return null;
        } else if(currentTranslation.x == originTranslation.x+(1*Landscape.chunkWidth) && currentTranslation.z == originTranslation.z+(1*Landscape.chunkWidth)) {
            currentTranslation.x = currentTranslation.x-(2*Landscape.chunkWidth);
            currentTranslation.z = currentTranslation.z-(2*Landscape.chunkWidth);
            currentTranslation.y = currentTranslation.y-(1*Landscape.chunkHeight);
        } else if(currentTranslation.x == originTranslation.x+(1*Landscape.chunkWidth)) {
            currentTranslation.x = currentTranslation.x-(2*Landscape.chunkWidth);
            currentTranslation.z = currentTranslation.z+(1*Landscape.chunkWidth);
        } else {
            currentTranslation.x = currentTranslation.x+(1*Landscape.chunkWidth);
        }
        return new Vector3f(currentTranslation.x, currentTranslation.y, currentTranslation.z);
    }

    public boolean isBegan() {
        return began;
    }

    public boolean lastLocation(Vector3f location) {
        return (location.x == originTranslation.x+(1*Landscape.chunkWidth) 
                && location.z == originTranslation.z+(1*Landscape.chunkWidth) 
                && location.y == 0);
    }

    public void began() {
        this.began = true;
    }
    
}
