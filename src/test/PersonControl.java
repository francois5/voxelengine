package test;

import com.jme3.bullet.control.BetterCharacterControl;

/**
 *
 * @author francois
 */
public class PersonControl extends BetterCharacterControl {
    
    
    PersonControl(float radius, float height, int mass) {
        super(radius, height, mass);
    }
    
    @Override
    public void update(float tpf) {
        super.update(tpf);
    }
}