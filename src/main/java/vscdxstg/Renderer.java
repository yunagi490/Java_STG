package vscdxstg;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;
import java.util.List;

/** Game の状態を Graphics2D に描く。 */
final class Renderer {
    private static final Font FONT_S = new Font(Font.SANS_SERIF, Font.PLAIN, 13);
    private static final Font FONT_M = new Font(Font.SANS_SERIF, Font.BOLD, 16);
    private static final Font FONT_L = new Font(Font.SANS_SERIF, Font.BOLD, 28);

    private final Assets assets;

    Renderer(Assets assets) {
        this.assets = assets;
    }

    void render(Graphics2D g, Game game) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        g.setColor(new Color(18, 18, 28));
        g.fillRect(0, 0, Defs.WINDOW_W, Defs.WINDOW_H);

        drawField(g, game);
        drawHud(g, game);
    }

    // ------------------------------------------------------------ フィールド

    private void drawField(Graphics2D g, Game game) {
        Shape oldClip = g.getClip();
        g.setClip(Defs.MARGIN, Defs.MARGIN, Defs.FIELD_W, Defs.FIELD_H);
        g.translate(Defs.MARGIN, Defs.MARGIN);

        drawBackground(g, game);

        if (game.screen() == Game.Screen.TITLE) {
            drawTitle(g);
        } else {
            drawPlay(g, game);
        }

        g.translate(-Defs.MARGIN, -Defs.MARGIN);
        g.setClip(oldClip);

        g.setColor(new Color(200, 200, 220));
        g.drawRect(Defs.MARGIN - 1, Defs.MARGIN - 1, Defs.FIELD_W + 1, Defs.FIELD_H + 1);
    }

    private void drawBackground(Graphics2D g, Game game) {
        BufferedImage bg = assets.get("back");
        int y = (int) game.bgY();
        g.drawImage(bg, 0, y, null);
        g.drawImage(bg, 0, y - Defs.BG_H, null);
    }

    private void drawPlay(Graphics2D g, Game game) {
        BufferedImage pshot = assets.get("pshot");
        for (PlayerShot s : game.playerShots()) {
            g.drawImage(pshot, (int) (s.x - pshot.getWidth() / 2.0), (int) (s.y - pshot.getHeight() / 2.0), null);
        }

        for (Foe f : game.foes()) {
            drawFoe(g, f);
        }

        Player p = game.player();
        if (p.alive) {
            boolean blink = p.invincible > 0 && (game.frame() / 4) % 2 == 0;
            if (!blink) {
                BufferedImage img = assets.get("player");
                g.drawImage(img, (int) (p.x - img.getWidth() / 2.0), (int) (p.y - img.getHeight() / 2.0), null);
            }
            if (p.slow) {
                // 低速移動中は当たり判定を表示
                double r = Defs.PLAYER_HIT_R + 1;
                g.setColor(Color.WHITE);
                g.fill(new Ellipse2D.Double(p.x - r, p.y - r, r * 2, r * 2));
                g.setColor(new Color(255, 60, 60));
                g.draw(new Ellipse2D.Double(p.x - r, p.y - r, r * 2, r * 2));
            }
        }

        BufferedImage bullet = assets.get("bullet");
        for (Bullet b : game.bullets()) {
            g.drawImage(bullet, (int) (b.x - bullet.getWidth() / 2.0), (int) (b.y - bullet.getHeight() / 2.0), null);
        }

        Foe boss = game.boss();
        if (boss != null) {
            drawBossBar(g, boss);
        }

        drawOverlays(g, game);
    }

    private void drawFoe(Graphics2D g, Foe f) {
        if (f.alpha <= 0.01) return;
        BufferedImage img = assets.get(f.sprite);
        java.awt.Composite old = g.getComposite();
        if (f.alpha < 1.0) {
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, (float) f.alpha));
        }
        g.drawImage(img, (int) (f.x - img.getWidth() / 2.0), (int) (f.y - img.getHeight() / 2.0), null);
        g.setComposite(old);
    }

    private void drawBossBar(Graphics2D g, Foe boss) {
        int w = Defs.FIELD_W - 20;
        int filled = (int) Math.round(w * Math.max(0, boss.hp) / (double) boss.maxHp);
        g.setColor(new Color(0, 0, 0, 150));
        g.fillRect(10, 6, w, 7);
        g.setColor(boss.invulnerable ? new Color(150, 150, 170) : new Color(230, 60, 70));
        g.fillRect(10, 6, filled, 7);
        g.setColor(Color.WHITE);
        g.drawRect(10, 6, w, 7);
    }

    private void drawOverlays(Graphics2D g, Game game) {
        String[] banner = game.bannerLines();
        if (banner.length > 0 && !game.warning()) {
            drawTextBlock(g, banner, 150, new Color(255, 255, 255));
        }

        if (game.warning()) {
            boolean on = (game.frame() / 10) % 2 == 0;
            if (on) {
                String title = game.exWarning() ? "EX BOSS" : "WARNING";
                String[] lines = game.exWarning()
                        ? new String[] {title, "「" + game.exWarningName() + "」が現れた!"}
                        : new String[] {title};
                drawTextBlock(g, lines, 170, new Color(255, 80, 80));
            }
        }

        if (game.paused()) {
            g.setColor(new Color(0, 0, 0, 150));
            g.fillRect(0, 0, Defs.FIELD_W, Defs.FIELD_H);
            drawTextBlock(g, new String[] {"PAUSE", "P / Esc で再開"}, 210, Color.WHITE);
        }

        if (game.screen() == Game.Screen.GAME_OVER) {
            g.setColor(new Color(0, 0, 0, 160));
            g.fillRect(0, 0, Defs.FIELD_W, Defs.FIELD_H);
            drawTextBlock(g, new String[] {"GAME OVER", "SCORE " + game.score(), "Z キーでタイトルへ"}, 190,
                    new Color(255, 120, 120));
        }
    }

    private void drawTitle(Graphics2D g) {
        g.setColor(new Color(0, 0, 0, 120));
        g.fillRect(0, 0, Defs.FIELD_W, Defs.FIELD_H);
        drawTextBlock(g, new String[] {"VSCDXSTG", "", "Z キーでスタート"}, 160, Color.WHITE);

        g.setFont(FONT_S);
        g.setColor(new Color(220, 220, 235));
        String[] help = {
                "↑↓←→ : 移動",
                "Z : ショット(押しっぱなし)",
                "Shift : 低速移動(当たり判定表示)",
                "P / Esc : ポーズ",
        };
        int y = 300;
        for (String s : help) {
            g.drawString(s, 40, y);
            y += 22;
        }
    }

    /** 中央揃えで複数行を描く(1行目だけ大きい文字)。 */
    private void drawTextBlock(Graphics2D g, String[] lines, int topY, Color color) {
        int y = topY;
        for (int i = 0; i < lines.length; i++) {
            g.setFont(i == 0 ? FONT_L : FONT_M);
            FontMetrics fm = g.getFontMetrics();
            int x = (Defs.FIELD_W - fm.stringWidth(lines[i])) / 2;
            g.setColor(new Color(0, 0, 0, 180));
            g.drawString(lines[i], x + 2, y + 2);
            g.setColor(color);
            g.drawString(lines[i], x, y);
            y += i == 0 ? 36 : 28;
        }
    }

    // ------------------------------------------------------------ HUD

    private void drawHud(Graphics2D g, Game game) {
        int x = Defs.MARGIN + Defs.FIELD_W + 24;
        int y = 40;

        g.setColor(Color.WHITE);
        g.setFont(FONT_M);
        g.drawString("VSCDXSTG", x, y);
        y += 36;

        g.setFont(FONT_S);
        g.drawString("SCORE", x, y);
        y += 20;
        g.setFont(FONT_M);
        g.drawString(String.format("%,d", game.score()), x, y);
        y += 32;

        g.setFont(FONT_S);
        g.drawString("LIFE", x, y);
        y += 22;
        for (int i = 0; i < game.player().lives; i++) {
            g.setColor(new Color(255, 90, 120));
            g.fill(heart(x + i * 22, y - 14));
        }
        y += 28;

        g.setColor(Color.WHITE);
        g.setFont(FONT_S);
        g.drawString("STAGE " + game.stage(), x, y);
        y += 26;

        g.drawString("難易度 (1d100)", x, y);
        y += 20;
        g.setFont(FONT_M);
        if (game.lastD100() > 0) {
            g.setColor(levelColor(game.difficulty().level));
            g.drawString(game.difficulty().level + "  " + game.difficulty().label(), x, y);
        } else {
            g.drawString("-", x, y);
        }
        y += 32;

        g.setColor(Color.WHITE);
        g.setFont(FONT_S);
        g.drawString("ボス後ダイス (1d6)", x, y);
        y += 20;
        g.setFont(FONT_M);
        g.drawString(game.lastD6() > 0 ? String.valueOf(game.lastD6()) : "-", x, y);
        y += 22;
        g.setFont(FONT_S);
        g.drawString("履歴: " + history(game.d6History()), x, y);
        y += 22;
        if (game.fumblePending()) {
            g.setColor(new Color(255, 100, 100));
            g.drawString("次ステージ: 最高難易度!", x, y);
        }
    }

    private static String history(List<Integer> h) {
        if (h.isEmpty()) return "-";
        StringBuilder sb = new StringBuilder();
        int from = Math.max(0, h.size() - 8);
        for (int i = from; i < h.size(); i++) {
            if (sb.length() > 0) sb.append(' ');
            sb.append(h.get(i));
        }
        return sb.toString();
    }

    private static Color levelColor(int level) {
        if (level >= 90) return new Color(255, 80, 80);
        if (level >= 70) return new Color(255, 160, 60);
        if (level >= 30) return new Color(240, 240, 120);
        return new Color(120, 230, 150);
    }

    private static Shape heart(double x, double y) {
        java.awt.geom.Path2D.Double p = new java.awt.geom.Path2D.Double();
        p.moveTo(x + 8, y + 15);
        p.curveTo(x - 6, y + 5, x + 2, y - 4, x + 8, y + 3);
        p.curveTo(x + 14, y - 4, x + 22, y + 5, x + 8, y + 15);
        p.closePath();
        return p;
    }
}
