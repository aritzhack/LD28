package aritzh.ld28;

import aritzh.ld28.board.Board;
import aritzh.ld28.render.Render;
import aritzh.ld28.render.SpriteSheet;
import aritzh.ld28.screen.MainMenuScreen;
import aritzh.ld28.screen.PauseScreen;
import aritzh.ld28.screen.Screen;

import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.WindowConstants;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferStrategy;
import java.awt.image.BufferedImage;
import java.io.IOException;

/**
 * @author Aritz Lopez
 * @license Lesser GNU Public License v3 (http://www.gnu.org/licenses/lgpl.html)
 */
public class Game extends Canvas implements Runnable {

    public static final Font bigFont = new Font("Arial", Font.BOLD, 24);
    private static final double WANTED_UPS = 60.0;
    private static final Font font = new Font("Arial", Font.BOLD, 14);
    public static Game INSTANCE;
    public final SpriteSheet sheet;
    public final BufferedImage soundOn, soundOff;
    private final int width;
    private final int height;
    private final boolean applet;
    private final Thread thread;
    private final Input input;
    private final Render render;
    public final Rectangle soundButton;
    private boolean running;
    private int fps, ups;
    private JFrame frame;
    private Screen currScreen;
    public final BufferedImage background;
    private boolean muted;

    private Game(int width, int height, boolean applet) {
        this.width = width;
        this.height = height;
        this.applet = applet;
        this.thread = new Thread(this, "Main Game Thread");
        this.render = new Render(this);
        this.sheet = new SpriteSheet(this.getClass().getResourceAsStream("/textures/sheet.png"), 64);
        this.input = new Input(this);
        try {
            this.background = ImageIO.read(this.getClass().getResourceAsStream("/textures/bg.png"));
            this.soundOff =  ImageIO.read(this.getClass().getResourceAsStream("/textures/soundOff.png"));
            this.soundOn =  ImageIO.read(this.getClass().getResourceAsStream("/textures/soundOn.png"));
        } catch (IOException e) {
            throw new IllegalArgumentException("Error loading background", e);
        }
        this.addMouseListener(this.input);
        this.addMouseMotionListener(this.input);
        this.addKeyListener(this.input);
        this.setSize(width, height);

        if (!this.applet) this.createWindow();
        this.soundButton = new Rectangle(10, this.getHeight() - 118, 128, 128);
    }

    private void createWindow() {
        this.frame = new JFrame("You Only Got One Second!");
        this.frame.setResizable(false);
        final Dimension preferredSize = new Dimension(width, height);
        this.frame.getContentPane().setPreferredSize(preferredSize);
        this.frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

        this.frame.add(this);
        this.frame.pack();

        this.frame.setLocationRelativeTo(null);
        Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
        this.setLocation((dim.width - this.frame.getContentPane().getSize().width) / 2, (dim.height - this.frame.getContentPane().getSize().height) / 2);

        this.frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                super.windowClosing(e);
                Game.this.stop();
            }
        });
    }

    public synchronized void stop() {
        this.running = false;
        this.currScreen.closing();
        this.getParentWindow().dispose();
        try {
            this.thread.join(3000);
        } catch (InterruptedException e) {
            System.exit(0);
        }
    }

    public static Game getGame(int width, int height, boolean applet) {
        if (INSTANCE == null) return INSTANCE = new Game(width, height, applet);
        else return INSTANCE;
    }

    public synchronized void start() {
        this.running = true;
        if (!this.applet) this.frame.setVisible(true);
        this.thread.start();
    }

    @Override
    public void run() {
        long lastTime = System.nanoTime();
        long lastTimeMillis = System.currentTimeMillis();

        final double NSPerTick = 1000000000.0 / WANTED_UPS;
        double delta = 0.0;
        this.currScreen = new MainMenuScreen(this);
        while (this.running) {
            long now = System.nanoTime();
            delta += (now - lastTime) / NSPerTick;
            lastTime = now;

            if (delta >= 1) {
                this.update(delta);
                delta--;
                ups++;
            }

            this.render();
            this.fps++;

            if (System.currentTimeMillis() - lastTimeMillis >= 1000) {
                this.updatePS();
                lastTimeMillis += 1000;
                this.fps = this.ups = 0;
            }
        }
    }

    private void update(double delta) {
        this.currScreen.update(this.hasFocus());
    }

    private void render() {
        if (!this.running) return;
        BufferStrategy bs = this.getBufferStrategy();
        if (bs == null) {
            this.createBufferStrategy(3);
            return;
        }
        Graphics g = bs.getDrawGraphics();
        g.setFont(Game.font);

        render.clear();

        this.currScreen.render(this.render);

        g.drawImage(this.render.getImage(), 0, 0, this.width, this.height, null);

        this.currScreen.renderGraphics(g);

        if(this.currScreen instanceof MainMenuScreen || this.currScreen instanceof PauseScreen){
            g.drawImage(this.muted?this.soundOff:this.soundOn, this.soundButton.x, this.soundButton.y, null);
        }

        g.dispose();
        bs.show();
    }

    private void updatePS() {
        System.out.println("FPS: " + fps + "\t|\tUPS: " + this.ups);
        this.currScreen.updatePS();
    }

    public void startGame() {
        this.openScreen(new Board(this));
        ((Board) this.currScreen).start();
    }

    public void openScreen(Screen screen) {
        this.currScreen.closing();
        this.currScreen = screen;
        this.currScreen.opening();
    }

    public void silentSwitch(Screen screen) {
        (this.currScreen = screen).opening();
    }

    public int getOnScreenX() {
        return this.getParent().getX();
    }

    public int getOnScreenY() {
        return this.getParent().getY();
    }

    public int getWidth() {
        return this.width;
    }

    public int getHeight() {
        return this.height;
    }

    public Screen getCurrentScreen() {
        return this.currScreen;
    }

    private Window getParentWindow() {
        Container parent = this.getParent();
        while (parent.getParent() != null && !(parent instanceof Window)) {
            parent = parent.getParent();
        }
        return (Window) parent;
    }

    public void switchMute() {
        this.muted = !this.muted;
    }

    public boolean isMuted(){
        return this.muted;
    }
}
