package test;

import com.jme3.bullet.PhysicsSpace;
import com.jme3.bullet.collision.PhysicsRayTestResult;
import com.jme3.bullet.control.BetterCharacterControl;
import com.jme3.bullet.objects.PhysicsRigidBody;
import com.jme3.math.FastMath;
import java.util.List;

/**
 *
 * @author found on jMonkeyEngine forum
 */
public class GameCharControl extends BetterCharacterControl {

    private boolean tooSteep = false;
    private boolean isWalkableStep = false;
    private boolean helpingUpStep = false;
    private float maxSlope = 85;
    private float maxStepHeight = 1.1f;
    private boolean run;
    private float runFactor = 2;

    public GameCharControl(float radius, float height, int mass) {
        this.radius = radius;
        this.height = height;
        this.mass = mass;
        rigidBody = new PhysicsRigidBody(getShape(), mass);
        rigidBody.setAngularFactor(0);
        jumpForce.y = mass * 5;
    }

    public void setRun(boolean run) {
        this.run = run;
    }
    
    public boolean isRunning() {
        return this.run;
    }

    @Override
    public void update(float tpf) {
        super.update(tpf);
        if(run)
            getWalkDirection().multLocal(runFactor);
    }

    @Override
    public void prePhysicsTick(PhysicsSpace space, float tpf) {
        super.prePhysicsTick(space, tpf);

        checkSlope();
        checkWalkableStep();

        if (tooSteep && isWalkableStep) {
            rigidBody.setLinearVelocity(rigidBody.getLinearVelocity().add(0, 5, 0));
            helpingUpStep = true;
            return;
        }

        if (helpingUpStep) {
            helpingUpStep = false;
            rigidBody.setLinearVelocity(rigidBody.getLinearVelocity().setY(0));
        }

    }

    protected void checkSlope() {
        if (this.getWalkDirection().length() > 0) {

            List<PhysicsRayTestResult> results = space.rayTest(
                    rigidBody.getPhysicsLocation().add(0, 0.01f, 0),
                    rigidBody.getPhysicsLocation().add(
                    walkDirection.setY(0).normalize().mult(getFinalRadius())).add(0, 0.01f, 0));
            for (PhysicsRayTestResult physicsRayTestResult : results) {
                float angle = physicsRayTestResult
                        .getHitNormalLocal()
                        .normalize()
                        .angleBetween(
                        physicsRayTestResult.getHitNormalLocal()
                        .setY(0).normalize());

                //System.out.println(Math.abs(angle * FastMath.RAD_TO_DEG - 90));

                if (Math.abs(angle * FastMath.RAD_TO_DEG - 90) > maxSlope && !physicsRayTestResult.getCollisionObject().equals(rigidBody)) {
                    tooSteep = true;
                    return;
                }
            }

        }
        tooSteep = false;

    }

    private void checkWalkableStep() {
        if (walkDirection.length() > 0) {
            if (tooSteep) {

                List<PhysicsRayTestResult> results = space
                        .rayTest(
                        rigidBody.getPhysicsLocation().add(0,
                        maxStepHeight, 0),
                        rigidBody
                        .getPhysicsLocation()
                        .add(0, maxStepHeight, 0)
                        .add(walkDirection.normalize().mult(
                        getFinalRadius())));

                for (PhysicsRayTestResult physicsRayTestResult : results) {
                    isWalkableStep = false;
                    return;
                }

                isWalkableStep = true;
                return;
            }
        }

        isWalkableStep = false;
    }
}