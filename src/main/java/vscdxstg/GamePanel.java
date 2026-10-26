package vscdxstg;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import javax.swing.JPanel;
import javax.swing.Timer;

/** 入力の取得と固定60fpsのゲームループ。 */
final class GamePanel extends JPanel {
    private static final long STEP_NANOS = 1_000_000_000L / Defs.FPS;
    private static final int MAX_CATCHUP_STEPS = 5;

    private final Game game;
    private final Renderer renderer;
    private final Input input = new Input();
    private final boolean[] down = new boolean[256];

    private long last;
    private long acc;

    GamePanel(Game game, Renderer renderer) {
        this.game = game;
        this.renderer = renderer;
        setPreferredSize(new Dimension(Defs.WINDOW_W, Defs.WINDOW_H));
        setBackground(Color.BLACK);
        setFocusable(true);
        setDoubleBuffered(true);

        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                onKey(e, true);
            }

            @Override
            public void keyReleased(KeyEvent e) {
                onKey(e, false);
            }
        });
    }

    void start() {
        requestFocusInWindow();
        last = System.nanoTime();
        new Timer(4, e -> tick()).start();
    }

    private void tick() {
        long now = System.nanoTime();
        acc += now - last;
        last = now;

        int steps = 0;
        while (acc >= STEP_NANOS && steps < MAX_CATCHUP_STEPS) {
            game.update(input);
            acc -= STEP_NANOS;
            steps++;
        }
        if (steps == MAX_CATCHUP_STEPS) {
            acc = 0; // 遅れを引きずらない
        }
        if (steps > 0) {
            repaint();
        }
    }

    private void onKey(KeyEvent e, boolean pressed) {
        int code = e.getKeyCode();
        boolean first = code < down.length && pressed && !down[code];
        if (code < down.length) {
            down[code] = pressed;
        }

        switch (code) {
            case KeyEvent.VK_LEFT -> input.left = pressed;
            case KeyEvent.VK_RIGHT -> input.right = pressed;
            case KeyEvent.VK_UP -> input.up = pressed;
            case KeyEvent.VK_DOWN -> input.down = pressed;
            case KeyEvent.VK_Z -> {
                input.shot = pressed;
                if (first) input.pressConfirm();
            }
            case KeyEvent.VK_ENTER -> {
                if (first) input.pressConfirm();
            }
            case KeyEvent.VK_SHIFT -> input.slow = pressed;
            case KeyEvent.VK_P, KeyEvent.VK_ESCAPE -> {
                if (first) input.pressPause();
            }
            default -> { }
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            renderer.render(g2, game);
        } finally {
            g2.dispose();
        }
    }
}
