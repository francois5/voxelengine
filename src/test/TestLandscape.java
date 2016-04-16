package test;

import voxelEngine.Landscape;
import voxelEngine.noise.VoxelNoise;
import com.jme3.app.SimpleApplication;
import com.jme3.bullet.BulletAppState;
import com.jme3.collision.CollisionResults;
import com.jme3.font.BitmapText;
import com.jme3.input.KeyInput;
import com.jme3.input.MouseInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.input.controls.MouseButtonTrigger;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Ray;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;
import com.jme3.system.AppSettings;
import voxelEngine.FaceTypes;
import voxelEngine.VoxelSystem;
import voxelEngine.VoxelTypes;

/**
 *
 * @author francois
 */
public class TestLandscape extends SimpleApplication implements ActionListener {
    
// Voxel system constructor arguments
    private int displayRadius = 10;
    private int initBeforeStartDisplayRadius = 4;
    private int chunkLoadByFrame = 1;
    private int maxRelief = 20;
    private int chunkWidth = 16;
    private int chunkHeight = 16;
    private int systemHeight = 8;
    private boolean enableFaceCulling = true;
    private int voxelSize = 1;
    private int noiseResolution = 150;
    private VoxelNoise noise = new TestVoxelNoise(systemHeight, chunkHeight, maxRelief, noiseResolution);
    private FaceTypes faceTypes = new TestFaceTypes(); 
    private VoxelTypes voxelTypes = new TestVoxelTypes();
//
    
    public static TestLandscape app;
    private VoxelSystem voxelSystem;
    private BulletAppState bulletAppState;
    private GameCharControl physicsCharacter;
    private Vector3f walkDirection = new Vector3f();
    private Node characterNode;
    private boolean leftStrafe = false, rightStrafe = false, forward = false, backward = false,
            fly = false, flying = false, firstPersonMode = true;
    private CubeSelector cubeSelector;
    
    private final Vector2f vector2f_tmp = new Vector2f();
    private final Ray ray_tmp = new Ray();
    private final Vector3f flyingElevation_1 = new Vector3f(0,0.4f,0);
    private final Vector3f flyingElevation_2 = new Vector3f(0,5f,0);
    private float camHeight = 1.7f;
    
    public static void main(String[] args) {
        final TestLandscape app = new TestLandscape();
        app.settings = new AppSettings(true);
        //app.setShowSettings(false);
        app.settings.setResolution(1024, 576);
        
        //app.settings.setRenderer(AppSettings.LWJGL_OPENGL2);
        //app.settings.setAudioRenderer(AppSettings.LWJGL_OPENAL);
        app.start();
    }

    @Override
    public void simpleInitApp() {
        app = this;
        //setDisplayStatView(false);
        //setDisplayFps(false);
        cubeSelector = new CubeSelector(this.rootNode);
        flyCam.setMoveSpeed(100);
        cam.setFrustumPerspective(60, (float)settings.getWidth()/(float)settings.getHeight()
                , 0.05f, 1000);
        cam.lookAt(new Vector3f(-10f, 0f, -10f), Vector3f.ZERO);
        bulletAppState = new BulletAppState();
        bulletAppState.setThreadingType(BulletAppState.ThreadingType.PARALLEL);
        stateManager.attach(bulletAppState);
        
        // Instanciation of the voxel system
        voxelSystem = new Landscape(this, bulletAppState, displayRadius, initBeforeStartDisplayRadius, chunkLoadByFrame, 
                systemHeight, voxelSize, chunkWidth, chunkHeight, enableFaceCulling, noise, faceTypes, voxelTypes);

        /*
         * Little example of adding and removing blocks after the chunk generation
         * Don't use that to generate the initial "world" this must be done by the VoxelNoise
         * 
         * Notice the use of voxelSystem.getHeight(int x, int z)
         */
        voxelSystem.putBlock(new Vector3f(-10f, systemHeight*(chunkHeight/2)+25, -10f), 4);
        voxelSystem.putBlock(new Vector3f(-11f, systemHeight*(chunkHeight/2)+25, -10f), 4);
        voxelSystem.putBlock(new Vector3f(-10f, systemHeight*(chunkHeight/2)+24, -10f), 4);
        voxelSystem.putBlock(new Vector3f(-11f, systemHeight*(chunkHeight/2)+24, -10f), 4);
        
        voxelSystem.putBlock(new Vector3f(-10f, systemHeight*(chunkHeight/2)+25, -11f), 4);
        voxelSystem.putBlock(new Vector3f(-11f, systemHeight*(chunkHeight/2)+25, -11f), 4);
        voxelSystem.putBlock(new Vector3f(-10f, systemHeight*(chunkHeight/2)+24, -11f), 4);
        voxelSystem.putBlock(new Vector3f(-11f, systemHeight*(chunkHeight/2)+24, -11f), 4);
        
        voxelSystem.removeBlock(new Vector3f(-10f, systemHeight*(chunkHeight/2)+25, -10f));
        for(int i = -5; i < 5; ++i)
            voxelSystem.putBlock(new Vector3f(-i, voxelSystem.getHeight(-i, -10), -10f), 4);
        for(int i = -5; i < 5; ++i)
            voxelSystem.putBlock(new Vector3f(0, voxelSystem.getHeight(0, -i), -i), 4);
        
        
        initCrossHairs();
        setupKeys();
        //setUpLight();

        characterNode = new Node("character node");
        characterNode.setLocalTranslation(new Vector3f(0, systemHeight*(chunkHeight/2)+25, 0));

        physicsCharacter = new GameCharControl(.3f, 1.9f, 80);
        physicsCharacter.setDuckedFactor(0.5f);
        characterNode.addControl(physicsCharacter);
        bulletAppState.getPhysicsSpace().add(physicsCharacter);

        rootNode.attachChild(characterNode);
    }
 
    @Override
    public void onAction(String binding, boolean value, float tpf) {
        if (binding.equals("Strafe Left")) {
            if (value) {
                leftStrafe = true;
            } else {
                leftStrafe = false;
            }
        } else if (binding.equals("Strafe Right")) {
            if (value) {
                rightStrafe = true;
            } else {
                rightStrafe = false;
            }
        } else if (binding.equals("Walk Forward")) {
            if (value) {
                forward = true;
            } else {
                forward = false;
            }
        } else if (binding.equals("Walk Backward")) {
            if (value) {
                backward = true;
            } else {
                backward = false;
            }
        } else if (binding.equals("Up")) {
            if (value) {
                flying = true;
                fly = true;
            } else {
                fly = false;
            }
        } else if (binding.equals("Jump")) {
            physicsCharacter.jump();
        } else if (binding.equals("Duck")) {
            if (value) {
                physicsCharacter.setDucked(true);
            } else {
                physicsCharacter.setDucked(false);
            }
        } else if (binding.equals("Run")) {
            if (value) {
                physicsCharacter.setRun(true);
            } else {
                physicsCharacter.setRun(false);
            }
        } else if (binding.equals("Debug") && value) {
            if (this.bulletAppState.isDebugEnabled())
                this.bulletAppState.setDebugEnabled(false);
            else
                this.bulletAppState.setDebugEnabled(true);
        } else if (binding.equals("FirstPerson") && value) {
            firstPersonMode = !firstPersonMode;
        }
        
    }
  
  private void setupKeys() {
        inputManager.addMapping("Strafe Left", new KeyTrigger(KeyInput.KEY_Q));
        inputManager.addMapping("Strafe Right", new KeyTrigger(KeyInput.KEY_D));
        inputManager.addMapping("Rotate Left", new KeyTrigger(KeyInput.KEY_LEFT));
        inputManager.addMapping("Rotate Right", new KeyTrigger(KeyInput.KEY_RIGHT));
        inputManager.addMapping("Walk Forward", new KeyTrigger(KeyInput.KEY_Z));
        inputManager.addMapping("Walk Backward", new KeyTrigger(KeyInput.KEY_S));
        inputManager.addMapping("Jump", new KeyTrigger(KeyInput.KEY_SPACE));
        inputManager.addMapping("Duck", new KeyTrigger(KeyInput.KEY_LCONTROL));
        inputManager.addMapping("Run", new KeyTrigger(KeyInput.KEY_LSHIFT));
        inputManager.addMapping("Lock View", new KeyTrigger(KeyInput.KEY_RETURN));
        inputManager.addMapping("Up", new KeyTrigger(KeyInput.KEY_F));
        inputManager.addMapping("Debug", new KeyTrigger(KeyInput.KEY_T));
        inputManager.addMapping("FirstPerson", new KeyTrigger(KeyInput.KEY_X));
        
        inputManager.addMapping("leftClic", new MouseButtonTrigger(MouseInput.BUTTON_LEFT));
        inputManager.addListener(leftClicListener, "leftClic");
        inputManager.addMapping("rightClic", new MouseButtonTrigger(MouseInput.BUTTON_RIGHT));
        inputManager.addListener(rightClicListener, "rightClic");
        
        inputManager.addListener(this, "Strafe Left", "Strafe Right");
        inputManager.addListener(this, "Rotate Left", "Rotate Right");
        inputManager.addListener(this, "Walk Forward", "Walk Backward");
        inputManager.addListener(this, "Jump", "Duck", "Run", "Lock View", "Up");
        inputManager.addListener(this, "Debug", "FirstPerson");
    }
  
  /*
   * Add and remove blocks on click
   */
  
    private ActionListener leftClicListener = new ActionListener() {
        @Override
        public void onAction(String name, boolean isPressed, float tpf) {
            if (isPressed) {
                Vector3f blockLocation = getCurrentPointedLocation(true);
                if(blockLocation != null && validLocation(blockLocation)){
                    TestLandscape.this.voxelSystem.putBlock(blockLocation, 4);
                }
            }
        }
    };
  
    private boolean validLocation(Vector3f blockLocation) {
        return !((characterNode.getLocalTranslation().x >= blockLocation.x-0.2
              && characterNode.getLocalTranslation().x <= blockLocation.x+1.2)
              && ((characterNode.getLocalTranslation().y+0.5f) >= blockLocation.y-1
              && (characterNode.getLocalTranslation().y+0.5f) <= blockLocation.y+1)
              && ((characterNode.getLocalTranslation().z) >= blockLocation.z-0.2
              && (characterNode.getLocalTranslation().z) <= blockLocation.z+1.2));
    }
  
    private ActionListener rightClicListener = new ActionListener() {
        @Override
        public void onAction(String name, boolean isPressed, float tpf) {
            if (isPressed) {
                Vector3f blockLocation = getCurrentPointedLocation(false);
                if(blockLocation != null){
                    TestLandscape.this.voxelSystem.removeBlock(blockLocation);
                }
            }
        }
    };
  
    private CollisionResults getRayCastingResults(Node node) {
        vector2f_tmp.x = (settings.getWidth() / 2);
        vector2f_tmp.y = (settings.getHeight() / 2);
        Vector3f origin = cam.getWorldCoordinates(vector2f_tmp, 0.0f);
        Vector3f direction = cam.getWorldCoordinates(vector2f_tmp, 0.3f);
        direction.subtractLocal(origin).normalizeLocal();
        ray_tmp.origin = origin;
        ray_tmp.direction = direction;
        CollisionResults results = new CollisionResults();
        node.collideWith(ray_tmp, results);
        return results;
    }

    private Vector3f getCurrentPointedLocation(boolean getNeighborLocation) {
        CollisionResults results = getRayCastingResults(this.voxelSystem.getNode());
        if (results.size() > 0)
            return this.voxelSystem.getPointedBlockLocation(results.getClosestCollision().getContactPoint(), getNeighborLocation);
        return null;
    }

    @Override
    public void simpleUpdate(float tpf) {
        if(physicsCharacter.isOnGround()) {
            flying = false;
        }
        
        walkDirection.set(0, 0, 0);
        if (leftStrafe) {
            walkDirection.addLocal(cam.getLeft().multLocal(3));
        } else if (rightStrafe) {
            walkDirection.addLocal(cam.getLeft().negate().multLocal(3));
        }
        if (forward) {
            Vector3f modelForwardDir = cam.getDirection().setY(0);
            modelForwardDir.normalizeLocal();
            walkDirection.addLocal(modelForwardDir.multLocal(3));
        } else if (backward) {
            Vector3f modelForwardDir = cam.getDirection().setY(0);
            modelForwardDir.normalizeLocal();
            walkDirection.addLocal(modelForwardDir.negate().multLocal(3));
        }
        if(!physicsCharacter.isOnGround() && flying) {
            walkDirection = walkDirection.mult(8);
        }
        if (fly) {
            if(leftStrafe || rightStrafe || forward || backward)
                walkDirection.addLocal(flyingElevation_1);
            else
                walkDirection.addLocal(flyingElevation_2);
        }
        physicsCharacter.setWalkDirection(walkDirection);
        if(firstPersonMode) {
            if(physicsCharacter.isDucked() && camHeight > 0.85f)
                camHeight=camHeight-0.05f;
            else if(!physicsCharacter.isDucked() && camHeight < 1.7f)
                camHeight=camHeight+0.05f;
            cam.setLocation(characterNode.getLocalTranslation().add(0, camHeight, 0));
        }
        
        // The voxelSystem needs to keep track of a "playerLocation" to generate 
        // new chunks when the camera is moving
        this.voxelSystem.setPlayerLocation(cam.getLocation());
        
        selectCube();
    }
    
    /**
     * A centred plus sign to help the player aim.
     */
    protected void initCrossHairs() {
        guiFont = assetManager.loadFont("Interface/Fonts/Default.fnt");
        BitmapText ch = new BitmapText(guiFont, false);
        ch.setSize(guiFont.getCharSet().getRenderedSize() * 2);
        ch.setText("+"); // crosshairs
        ch.setLocalTranslation( // center
                settings.getWidth() / 2 - ch.getLineWidth() / 2, 
                settings.getHeight() / 2 + ch.getLineHeight() / 2, 0);
        guiNode.attachChild(ch);
    }

    public BulletAppState getBulletAppState() {
        return bulletAppState;
    }
    
    public static SimpleApplication getApp() {
        return app;
    }
    
    private void selectCube() {
        Vector3f blockLocation = getCurrentPointedLocation(false);
        if (blockLocation != null) {
            cubeSelector.drawCubeEdges(blockLocation, 2, ColorRGBA.Gray);
        }
    }
    
    @Override
    public void destroy() {
        super.destroy();
        this.voxelSystem.destroy();
    }
    
}
