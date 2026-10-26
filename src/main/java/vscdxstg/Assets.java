package vscdxstg;

import org.jetbrains.annotations.NotNull;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.awt.image.RescaleOp;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import javax.imageio.ImageIO;

/** 画像の読み込みと、描画サイズへの縮小(元画像は 900x900 などかなり大きい)。 */
final class Assets {
    private final Map<String, BufferedImage> images = new HashMap<>();

    Assets() throws IOException {
        images.put("back", load("back1.png"));
        images.put("player", scaled(load("player1.png"), Defs.PLAYER_SIZE, Defs.PLAYER_SIZE));
        images.put("pshot", scaled(load("shot.png"), Defs.PSHOT_SIZE, Defs.PSHOT_SIZE));
        // E_shot.png は 900x900 の中央に小さな弾があるだけなので、中身だけ切り出してから縮小する
        images.put("bullet", fit(cropToContent(load("E_shot.png")), Defs.BULLET_SIZE, Defs.BULLET_SIZE));
        images.put("enemy", scaled(load("n_enemy1.png"), Defs.ENEMY_SIZE, Defs.ENEMY_SIZE));
        // ステージボス用の専用画像は無いので、ザコ画像を大きくして赤く染める
        BufferedImage bossBase = scaled(load("n_enemy1.png"), Defs.BOSS_SIZE, Defs.BOSS_SIZE);
        images.put("boss", tint(bossBase, 1.0f, 0.55f, 0.55f));
        // ExBoss は 600x600 の余白が大きいので、同様に切り出して縦横比を保ったまま収める
        images.put("komy", fit(cropToContent(load("komy.png")), Defs.BOSS_SIZE, Defs.BOSS_SIZE));
        images.put("suzu", fit(cropToContent(load("suzu.png")), Defs.BOSS_SIZE, Defs.BOSS_SIZE));
    }

    @NotNull BufferedImage get(String name) {
        BufferedImage img = images.get(name);
        if (img == null) {
            throw new IllegalArgumentException("未登録の画像: " + name);
        }
        return img;
    }

    private static BufferedImage load(String file) throws IOException {
        String path = "/graphics/" + file;
        try (InputStream in = Assets.class.getResourceAsStream(path)) {
            if (in == null) {
                throw new IOException("画像が見つかりません: " + path);
            }
            return ImageIO.read(in);
        }
    }

    /** 透明でない部分の外接矩形で切り出す。 */
    private static BufferedImage cropToContent(BufferedImage src) {
        BufferedImage img = toArgb(src);
        int minX = img.getWidth(), minY = img.getHeight(), maxX = -1, maxY = -1;
        for (int y = 0; y < img.getHeight(); y++) {
            for (int x = 0; x < img.getWidth(); x++) {
                if ((img.getRGB(x, y) >>> 24) > 16) {
                    minX = Math.min(minX, x);
                    maxX = Math.max(maxX, x);
                    minY = Math.min(minY, y);
                    maxY = Math.max(maxY, y);
                }
            }
        }
        if (maxX < 0) return img;
        return img.getSubimage(minX, minY, maxX - minX + 1, maxY - minY + 1);
    }

    /** 縦横比を保ったまま w x h の透明キャンバスの中央に収める。 */
    private static @NotNull BufferedImage fit(@NotNull BufferedImage src, int w, int h) {
        double s = Math.min((double) w / src.getWidth(), (double) h / src.getHeight());
        int tw = Math.max(1, (int) Math.round(src.getWidth() * s));
        int th = Math.max(1, (int) Math.round(src.getHeight() * s));
        BufferedImage body = scaled(src, tw, th);
        BufferedImage dst = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = dst.createGraphics();
        g.drawImage(body, (w - tw) / 2, (h - th) / 2, null);
        g.dispose();
        return dst;
    }

    /** 半分ずつ縮めてから最後に合わせる(一気に縮小するとジャギーが出るため)。 */
    private static @NotNull BufferedImage scaled(@NotNull BufferedImage src, int w, int h) {
        BufferedImage cur = toArgb(src);
        while (cur.getWidth() / 2 >= w && cur.getHeight() / 2 >= h) {
            cur = resize(cur, cur.getWidth() / 2, cur.getHeight() / 2);
        }
        return resize(cur, w, h);
    }

    private static @NotNull BufferedImage resize(@NotNull BufferedImage src, int w, int h) {
        BufferedImage dst = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = dst.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.drawImage(src, 0, 0, w, h, null);
        g.dispose();
        return dst;
    }

    private static @NotNull BufferedImage toArgb(@NotNull BufferedImage src) {
        if (src.getType() == BufferedImage.TYPE_INT_ARGB) return src;
        BufferedImage dst = new BufferedImage(src.getWidth(), src.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = dst.createGraphics();
        g.drawImage(src, 0, 0, null);
        g.dispose();
        return dst;
    }

    private static BufferedImage tint(BufferedImage src, float r, float g, float b) {
        RescaleOp op = new RescaleOp(new float[] {r, g, b, 1.0f}, new float[4], null);
        return op.filter(src, null);
    }
}
