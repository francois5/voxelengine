package voxelEngine;

import com.jme3.math.Vector3f;

/**
 *
 * @author destroflyer
 */
public class Util {

    private static final float MAX_FLOAT_ROUNDING_DIFFERENCE = 0.0001f;
    
    public static Vector3f compensateFloatRoundingErrors(Vector3f vector) {
        return new Vector3f(compensateFloatRoundingErrors(vector.getX()),
                            compensateFloatRoundingErrors(vector.getY()),
                            compensateFloatRoundingErrors(vector.getZ()));
    }
    
    public static float compensateFloatRoundingErrors(float number) {
        float remainder = (number % 1);
        if ((Math.abs(remainder) < MAX_FLOAT_ROUNDING_DIFFERENCE) || (Math.abs(remainder) > (1 - MAX_FLOAT_ROUNDING_DIFFERENCE))) {
            number = Math.round(number);
        }
        return number;
    }
}