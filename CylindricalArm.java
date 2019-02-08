package cylindricalarm;

import com.sun.j3d.utils.applet.MainFrame;
import com.sun.j3d.utils.behaviors.mouse.MouseRotate;
import com.sun.j3d.utils.geometry.Box;
import com.sun.j3d.utils.geometry.Cylinder;
import com.sun.j3d.utils.image.TextureLoader;
import com.sun.j3d.utils.universe.SimpleUniverse;
import java.applet.Applet;
import java.awt.*;
import java.awt.event.*;
import javax.media.j3d.*;
import javax.swing.*;
import javax.vecmath.*;
import com.mnstarfire.loaders3d.Loader3DS;
import com.sun.j3d.loaders.Scene;
import java.io.*;
import java.util.Vector;
import javax.sound.sampled.*;
import javax.sound.sampled.Clip;

class Robot extends Applet implements ActionListener, KeyListener {
    //Tworzymy nowy świat 3D
    GraphicsConfiguration config = SimpleUniverse.getPreferredConfiguration();
    Canvas3D c = new Canvas3D(config);
    SimpleUniverse u = new SimpleUniverse(c);
        
    //ograniczamy scene
    BoundingSphere bounds = new BoundingSphere(new Point3d(0.0, 0.0, 0.0), 100.0);
    
    private final Button learn = new Button("tryb nauki");
    private final Button play = new Button("odtwórz");
    private final Button save = new Button("zapisz");
    private final Button remove = new Button("usuń");
    private final Button reset = new Button("reset");
    private final Button grip = new Button("chwyć");
    private final Button release = new Button("wypuść");
    
    private TransformGroup tgStem;
    private TransformGroup tgSlider;
    private TransformGroup tgArm;
    private TransformGroup tgStand;
    private TransformGroup tgFloor;
    private TransformGroup tgBody;
    private TransformGroup tgRotate;
    private TransformGroup tgRotate2;
    private TransformGroup tgCamera;
    private TransformGroup tgBox;
    private TransformGroup tgEffector;
    
    private Transform3D setStem = new Transform3D();
    private Transform3D setSlider = new Transform3D();
    private Transform3D setArm = new Transform3D(); 
    private Transform3D setScene = new Transform3D();
    private Transform3D setStand = new Transform3D();
    private Transform3D setFloor = new Transform3D();
    private Transform3D setBox = new Transform3D();
    private Transform3D setEffector = new Transform3D();

    //początkowe ustawienie części robota
    private float height;
    private float heightArm;
    private float xloc;
    private float xBox;
    private float yBox;
    private float zBox;
    private final float step = 0.02f;
    private int i = 0;
    private float a = 0.0f;
    private int last = 0;
    
    private Vector xAngle = new Vector();
    private Vector boxAngle = new Vector();
    private Vector tabHeight = new Vector();
    private Vector tabXloc = new Vector();
    private Vector tabxBox = new Vector();
    private Vector tabyBox = new Vector();
    private Vector tabzBox = new Vector();
            
    private final Vector3f positionStem = new Vector3f(0.0f, 0.0f, 0.0f);
    private final Vector3f positionSlider = new Vector3f(0.0f, 0.3f, 0.0f);
    private final Vector3f positionArm = new Vector3f(0.05f, 0.0f, 0.0f);
    private final Vector3f positionStand = new Vector3f(0.0f, -0.5f, 0.0f);
    private final Vector3f positionFloor = new Vector3f(0.0f, -0.54f, 0.0f);
    private final Vector3f positionBox = new Vector3f(0.38f, -0.48f, 0.0f);
    private final Vector3f positionEffector = new Vector3f(0.37f, 0.0f, 0.0f);
    
    private final String insert_bip = "audio\\insert.wav";
    private final String remove_bip = "audio\\remove.wav";
    private final String coin_bip = "audio\\coin.wav";
    private final String error_bip = "audio\\bip.wav";
    private final String misk_bip = "audio\\misk.wav";
    
    //zmienne do obrotu
    float rotationAngle = -2;
    float bAngle = -2;
    static RotationInterpolator rotator;
    static RotationInterpolator rotator2;
    
    private boolean isCollision = false;
    private boolean learning = false;
    private boolean playing = false;
    private boolean moveArmZ = true;
    private boolean moveArmX = true;
    private boolean moveSliderUp = true;
    private boolean moveSliderDown = true;
    private boolean moveArmLeft = true;
    private boolean moveArmRight = true;
    private boolean isCatch = false;
    private boolean gravity = false;
    private boolean canCatch = false;
    private boolean isSaved = false;
    
    private Timer timer;
    private Loader3DS loader = new Loader3DS();
    private Scene s = null;
    CollisionDetector detectBox;
    
    public BranchGroup createSceneGraph(){
        BranchGroup sceneGraph = new BranchGroup();
        //oświetlenie
        setLight(sceneGraph);
        
        tgBody = new TransformGroup();
        tgBody.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);
        tgBody.setCapability(TransformGroup.ALLOW_CHILDREN_WRITE);
        tgBody.setCapability(TransformGroup.ALLOW_CHILDREN_READ);
        tgBody.setCapability(TransformGroup.ALLOW_CHILDREN_EXTEND);
        
        //utworzenie rotatora obracającego robotem
        tgRotate = new TransformGroup();
        tgRotate.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);
        Alpha alfa = new Alpha(-1,3000);
        rotator = new RotationInterpolator(alfa, tgRotate);
        rotator.setSchedulingBounds(bounds);
        tgRotate.addChild(rotator);
        rotator.setMaximumAngle(rotationAngle); 
        rotator.setMinimumAngle(rotationAngle);
        tgRotate.addChild(tgBody);
        
        //utworzenie rotatora obracającego piłeczką, gdy jest złapana przez robot
        tgRotate2 = new TransformGroup();
        tgRotate2.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);
        rotator2 = new RotationInterpolator(alfa, tgRotate2);
        rotator2.setSchedulingBounds(bounds);
        tgRotate2.addChild(rotator2);
        rotator2.setMaximumAngle(bAngle); 
        rotator2.setMinimumAngle(bAngle);
        tgRotate2.addChild(tgBox);
        
        //TransformGroup, której elementy chcemy obracać myszką
        tgCamera = new TransformGroup();
        tgCamera.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);
        tgCamera.setCapability(TransformGroup.ALLOW_CHILDREN_WRITE);
        tgCamera.setCapability(TransformGroup.ALLOW_CHILDREN_READ);
        tgCamera.setCapability(TransformGroup.ALLOW_CHILDREN_EXTEND);
        sceneGraph.addChild(tgCamera);
        tgCamera.addChild(tgRotate);
        tgCamera.addChild(tgRotate2);
        
        //Obracanie kamera
        MouseRotate mouseRotate = new MouseRotate();
        mouseRotate.setTransformGroup(tgCamera);
        mouseRotate.setSchedulingBounds(new BoundingSphere());
        sceneGraph.addChild(mouseRotate);
        
        //materiał z którego zrobiony jest robot
        Material mat = new Material();
        mat.setSpecularColor(new Color3f(Color.WHITE)); 
        mat.setDiffuseColor(new Color3f(Color.WHITE));
        
        //tekstura rdzenia robota
        Appearance texStem = new Appearance();
        texStem.setTexture(createTexture("img/robot.jpg"));
        TextureAttributes texAttr = new TextureAttributes();
        texAttr.setPerspectiveCorrectionMode(TextureAttributes.NICEST);
        texAttr.setTextureMode(TextureAttributes.COMBINE);
        texStem.setTextureAttributes(texAttr);
        texStem.setMaterial(mat);
        
        //tekstura czesci robota
        Appearance cover = new Appearance();
        cover.setTexture(createTexture("img/cover.jpg"));
        cover.setTextureAttributes(texAttr);
        cover.setMaterial(mat);
        
        //tekstura ramienia
        Appearance texArm = new Appearance();
        texArm.setTexture(createTexture("img/arm.jpg"));
        texArm.setTextureAttributes(texAttr);
        texArm.setMaterial(mat);
        
        //tekstura podłogi
        Appearance texFloor = new Appearance();
        texFloor.setTexture(createTexture("img/floor.jpg"));
        texFloor.setTextureAttributes(texAttr);
        texFloor.setMaterial(mat);
        
        //tekstura prymitywu
        Appearance texBox = new Appearance();
        texBox.setTexture(createTexture("img/box.jpg"));
        texBox.setTextureAttributes(texAttr);
        texBox.setMaterial(mat);
        
        //tekstura tła
        TextureLoader myLoader = new  TextureLoader("img/sky.png",this);
        ImageComponent2D myImage = myLoader.getImage();
        Background background = new Background();
        background.setImage(myImage);
        background.setApplicationBounds(bounds);
        sceneGraph.addChild(background);
        
        //prymityw
        Box box = new Box(0.045f, 0.045f, 0.045f, Box.GENERATE_NORMALS | Box.GENERATE_TEXTURE_COORDS, texBox);
        setBox.set(positionBox);
        tgBox = new TransformGroup(setBox);
        tgBox.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);
        tgBox.addChild(box);
        tgRotate2.addChild(tgBox);
        
        //stem
        Cylinder stem = new Cylinder(0.05f, 1f, Cylinder.GENERATE_NORMALS | Cylinder.GENERATE_TEXTURE_COORDS, texStem);
        setStem.set(positionStem);
        tgStem = new TransformGroup(setStem);
        tgStem.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);
        tgStem.addChild(stem);
        tgBody.addChild(tgStem);
        
        //slider
        Box slider = new Box(0.15f, 0.07f, 0.10f, Box.GENERATE_NORMALS | Box.GENERATE_TEXTURE_COORDS, cover);
        setSlider.set(positionSlider);
        tgSlider = new TransformGroup(setSlider);
        tgSlider.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);
        tgSlider.addChild(slider);
        tgStem.addChild(tgSlider);
        
        //ramie
        Box arm = new Box(0.3f, 0.03f, 0.04f, Box.GENERATE_NORMALS | Box.GENERATE_TEXTURE_COORDS, texArm);
        setArm.set(positionArm);
        tgArm = new TransformGroup(setArm);
        tgArm.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);
        tgArm.addChild(arm);
        tgSlider.addChild(tgArm);
        
        //chwytak
        try {
            s = loader.load("effector2.3ds");
        } catch (FileNotFoundException ex) {
            System.err.println("Nie załadowano chwytaka");
            System.exit(1);
        }
        setEffector.set(positionEffector);
        tgEffector = new TransformGroup(setEffector);
        tgEffector.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);
        tgEffector.addChild(s.getSceneGroup());
        tgArm.addChild(tgEffector);
        
        //stojak
        Cylinder stand = new Cylinder(0.1f, 0.01f, Cylinder.GENERATE_NORMALS | Cylinder.GENERATE_TEXTURE_COORDS, cover);
        setStand.set(positionStand);
        tgStand = new TransformGroup(setStand);
        tgStand.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);
        tgStand.addChild(stand);
        tgCamera.addChild(tgStand);
        
        //podloga
        Box floor = new Box(1.5f, 0.01f, 1.5f, Cylinder.GENERATE_NORMALS | Cylinder.GENERATE_TEXTURE_COORDS, texFloor);
        setFloor.set(positionFloor);
        tgFloor = new TransformGroup(setFloor);
        tgFloor.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);
        tgFloor.addChild(floor);
        tgFloor.setCollidable(false);
        tgCamera.addChild(tgFloor);
        
        //dodanie kolizji
        detectBox = new CollisionDetector(box);
        detectBox.setSchedulingBounds(bounds);
        sceneGraph.addChild(detectBox);
        
        resetRobot();
        
        return sceneGraph; // zwracamy już w pełni ustawioną scenę
    }
    
    void setLight(BranchGroup bg){
        //Dodanie światła ambientowego
        Color3f lightColor = new Color3f(1.0f, 1.0f, 1.0f);
        AmbientLight ambientLightNode = new AmbientLight(lightColor);
        ambientLightNode.setInfluencingBounds(bounds);
        bg.addChild(ambientLightNode);

        //Ustawienie położenia światła
        Vector3f lightDirection = new Vector3f(-3.0f, -5.0f, -12.0f);
        DirectionalLight light = new DirectionalLight(lightColor, lightDirection);
        light.setInfluencingBounds(bounds); 
        //Dodanie światła
        bg.addChild(light); 
    }
    
    private Texture createTexture(String path) {
        // Załadowanie tekstury
        TextureLoader textureLoader = new TextureLoader(path, null);
        ImageComponent2D image = textureLoader.getImage();

        if (image == null) {
          System.out.println("Nie udało się załadować tekstury: " + path);
        }
        
        Texture2D texture = new Texture2D(Texture.BASE_LEVEL, Texture.RGBA, 64, 64);
        texture.setMagFilter(Texture.NICEST);
        texture.setMinFilter(Texture.NICEST);
        texture.setImage(0, image);

        return texture;
    }
    
  
    private void playSound(String txt) {
       try{
            Clip sound = AudioSystem.getClip();
            sound.open(AudioSystem.getAudioInputStream(new File(txt)));
            sound.start();
        }
        catch (IOException | LineUnavailableException | UnsupportedAudioFileException exc){
            exc.printStackTrace(System.out);
        }
    }
    
    private void resetRobot(){
        //ustawienie wartosci poczatkowych
        last = 0;
        height = 0.3f;
        heightArm = 0.0f;
        xloc = 0.05f;
        xBox = 0.38f;
        yBox = -0.48f;
        zBox = 0.0f;
        rotationAngle = -2;
        bAngle = -2;
        isCatch = false;
        moveArmZ = true;
        moveArmX = true;
        moveSliderUp = true;
        moveSliderDown = true;
        moveArmLeft = true;
        moveArmRight = true;
        remove();
        
        setStem.set(positionStem);
        setSlider.set(positionSlider);
        setArm.set(positionArm);
        setBox.set(positionBox);
        
        tgStem.setTransform(setStem);
        tgSlider.setTransform(setSlider);
        tgArm.setTransform(setArm);
        tgBox.setTransform(setBox);
        
        rotator.setMaximumAngle(rotationAngle); 
        rotator.setMinimumAngle(rotationAngle);
        rotator2.setMaximumAngle(bAngle); 
        rotator2.setMinimumAngle(bAngle);
    }
    
    private void avoidCollision(){
        switch(last){
                case 1: moveSliderUp = false; break;
                case 2: moveSliderDown = false; break;
                case 3: moveArmLeft = false; break;
                case 4: moveArmRight = false; break;
                case 5: moveArmZ = false; break;
                case 6: moveArmX = false; canCatch = true; break;
                default: break;
        }
    }
    
    private void addPoint(){
       //przesuwanie schwytanego prymitywu
        if(isCatch){
            yBox = height;
            setBox.setTranslation(new Vector3f(xBox,yBox, zBox));
            tgBox.setTransform(setBox);
            bAngle = rotationAngle;
            rotator2.setMaximumAngle(bAngle); 
            rotator2.setMinimumAngle(bAngle);
        }
        else{
            //warunek na złapanie prymitywu
            if((Math.abs(zBox-xloc)<=0.011) && !gravity){
                if(detectBox.collision == true){
                     canCatch = true;
                     playSound(coin_bip);
                 }
            }
            else canCatch = false;
        }
        //zapamiętywanie trajektorii
        if(learning && !playing){
            xAngle.add(rotationAngle);
            boxAngle.add(bAngle);
            tabHeight.add(height);
            tabXloc.add(xloc);
            tabxBox.add(xBox);
            tabyBox.add(yBox);
            tabzBox.add(zBox);
        }
    }
    
    private void checkCollision(){
        if(!isCatch){
            if(isCollision == false)
                if(detectBox.collision == true){
                    playSound(error_bip);
                    isCollision = true;
                    avoidCollision();
                }
            if(detectBox.collision == false){
                isCollision = false;
                moveArmZ = true;
                moveArmX = true;
                moveSliderUp = true;
                moveSliderDown = true;
                moveArmLeft = true;
                moveArmRight = true;
            }    
        }
    }
    
    private void simulateGraivty(){
        yBox -= (step+a);
        a += 0.01f;
        if(yBox <= -0.42f){
            gravity = false;
            a = 0.0f;
            yBox = -0.48f;
            playSound(misk_bip);
        }
        setBox.setTranslation(new Vector3f(xBox,yBox,zBox));
        tgBox.setTransform(setBox);
        if(learning && !playing)addPoint();
    }
    
    //metoda odpowiadająca za odtwarzanie zapisanej trajektorii
    private void moveRobot(){
        setArm.setTranslation(new Vector3f((float)tabXloc.get(i), heightArm, 0.0f));
        tgArm.setTransform(setArm);
        setSlider.setTranslation(new Vector3f(0.0f, (float) tabHeight.get(i), 0.0f));
        tgSlider.setTransform(setSlider);
        rotator.setMaximumAngle((float) xAngle.get(i)); 
        rotator.setMinimumAngle((float) xAngle.get(i));
        rotator2.setMaximumAngle((float) boxAngle.get(i)); 
        rotator2.setMinimumAngle((float) boxAngle.get(i));
        setBox.setTranslation(new Vector3f((float) tabxBox.get(i),(float) tabyBox.get(i),(float) tabzBox.get(i)));
        tgBox.setTransform(setBox);
        i++;
        if(tabHeight.size() == i){
            playing = false;
            i = 0;
        }
    }
    
    //usuwanie zapisnej trajektorii
    private void remove(){
        learning = false;
        playing = false;
        isSaved = false;
        xAngle.clear();
        boxAngle.clear();
        tabHeight.clear();
        tabXloc.clear();
        tabxBox.clear();
        tabyBox.clear();
        tabzBox.clear();
        i = 0;
    }
    
    public Robot(){   
        setLayout(new BorderLayout());
        add(BorderLayout.CENTER, c);
        c.addKeyListener(this);

        timer = new Timer(100,this);
        
        //dodanie panelu z przyciskami
        Panel p = new Panel();
        p.add(grip);
        p.add(release);
        p.add(learn);
        p.add(save);
        p.add(play);
        p.add(remove);
        p.add(reset);
        add("North",p);
        save.addActionListener(this);
        play.addActionListener(this);
        learn.addActionListener(this);
        remove.addActionListener(this);
        reset.addActionListener(this);
        release.addActionListener(this);
        grip.addActionListener(this);
        save.addKeyListener(this);
        play.addKeyListener(this);
        learn.addKeyListener(this);
        remove.addKeyListener(this);
        reset.addKeyListener(this);
        release.addKeyListener(this);
        grip.addKeyListener(this);

        BranchGroup scene = createSceneGraph();
        setScene.set(new Vector3f(0f, 0f, 2.5f));
        u.getViewingPlatform().getViewPlatformTransform().setTransform(setScene);
        u.addBranchGraph(scene);
    }
    
    @Override
    public void actionPerformed(ActionEvent e) {
        if(e.getSource() == learn){
            learning = true;
            i = 0;
        }
        if(e.getSource() == play && isSaved){
            playing = true;
            if(!timer.isRunning())timer.start();
        }
        else{
            if(playing && !learning)
                moveRobot();
        }
        if(e.getSource() == save && learning){
            learning = false;
            isSaved = true;
        }
        if(e.getSource() == remove){
            remove();
        }
        if(e.getSource() == reset){
                resetRobot();
        }
        if(e.getSource() == release && isCatch){
            isCatch = false;
            gravity = true;
            playSound(remove_bip);
            if(!timer.isRunning())timer.start();
        }
        else{
            if(gravity){
                simulateGraivty();
            } 
        }
        if(e.getSource() == grip && canCatch){
            isCatch = true;
            playSound(insert_bip);
            addPoint();
        }
    }

    @Override
    public void keyTyped(KeyEvent e) {
    }

    @Override
    public void keyPressed(KeyEvent e) {
        if((e.getKeyCode() == KeyEvent.VK_W || e.getKeyCode() == KeyEvent.VK_UP) && moveSliderUp){
            last = 1;
            checkCollision();
            if(moveSliderUp) height += step;
            setSlider.setTranslation(new Vector3f(0.0f,height, 0.0f));
            tgSlider.setTransform(setSlider);
            addPoint();
            if(height >= 0.44)
                moveSliderUp = false;
            moveSliderDown = true;
        }
        if((e.getKeyCode() == KeyEvent.VK_S || e.getKeyCode() == KeyEvent.VK_DOWN) && moveSliderDown){
            last = 2;
            checkCollision();
            if(moveSliderDown) height -= step;
            setSlider.setTranslation(new Vector3f(0.0f,height, 0.0f));
            tgSlider.setTransform(setSlider);
            addPoint();
            if(height <= -0.44)
                moveSliderDown = false;
            moveSliderUp = true;
        }
        if((e.getKeyCode() == KeyEvent.VK_A || e.getKeyCode() == KeyEvent.VK_LEFT) && moveArmLeft){
            last = 3;
            checkCollision();
            if(moveArmLeft) rotationAngle -= step;
            rotator.setMaximumAngle(rotationAngle); 
            rotator.setMinimumAngle(rotationAngle);
            addPoint();
            moveArmRight = true;
        }
        if((e.getKeyCode() == KeyEvent.VK_D || e.getKeyCode() == KeyEvent.VK_RIGHT) && moveArmRight){
            last = 4;
            checkCollision();
            if(moveArmRight) rotationAngle += step;
            rotator.setMaximumAngle(rotationAngle); 
            rotator.setMinimumAngle(rotationAngle);
            addPoint();
            moveArmLeft = true;
        }
        if(e.getKeyCode() == KeyEvent.VK_Z && moveArmZ){
            last = 5;
            checkCollision();
            if(moveArmZ) xloc -= step;
            setArm.setTranslation(new Vector3f(xloc,heightArm, 0.0f));
            tgArm.setTransform(setArm);
            if(isCatch) 
                xBox -= step;
            addPoint();
            if(xloc <= -0.14f)
                moveArmZ = false;
            moveArmX = true;
        }
        if(e.getKeyCode() == KeyEvent.VK_X && moveArmX){
            last = 6;
            checkCollision();
            if(moveArmX) xloc += step;
            setArm.setTranslation(new Vector3f(xloc,heightArm, 0.0f));
            tgArm.setTransform(setArm);
            if(isCatch) 
                xBox += step;
            addPoint();
            if(xloc >= 0.34f)
                moveArmX = false;
            moveArmZ = true;
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
    }
}

/**
 *
 * @author kasia
 */
public class CylindricalArm {
    
    /**
     *
     * @param args
     */
    public static void main(String[] args) {
        Robot robot = new Robot();
        robot.addKeyListener(robot);
        MainFrame mf = new MainFrame(robot, 800, 600);
    }  
}
