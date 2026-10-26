package vscdxstg;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.List;
import javax.imageio.ImageIO;

/**
 * 画面なしでゲームを回す簡易検証。
 *   java -Djava.awt.headless=true -cp target/classes:target/test-classes vscdxstg.HeadlessSim [出力ディレクトリ]
 *
 * 1) 無敵ボット + 1d6を常に6 → ステージ進行、ファンブル→最高難易度、666でコーミャ/スズ出現を確認
 * 2) 通常ボット(無敵なし) → 被弾してゲームオーバーまで進むことを確認
 */
public final class HeadlessSim {
    private static void check(boolean cond, String msg) {
        if (!cond) throw new AssertionError(msg);
        System.out.println("  OK: " + msg);
    }

    public static void main(String @NotNull [] args) throws IOException {
        File outDir = new File(args.length > 0 ? args[0] : "target/sim");
        outDir.mkdirs();
        List<EnemyData> table = EnemyData.loadResource("/Resource/Enemy.csv");
        check(table.size() == 10, "Enemy.csv を10行読み込み (" + table.size() + ")");
        check(table.get(0).x() == 50 && table.get(0).y() == -40 && table.get(0).hp() == 1,
                "X/Y/HP 列を正しく読めている(元のC++はY座標を読み損ねていた)");

        Renderer renderer = new Renderer(new Assets());
        godRun(table, renderer, outDir);
        mortalRun(table, renderer, outDir);
        System.out.println("ALL OK");
    }

    private static void godRun(List<EnemyData> table, Renderer renderer, File outDir) throws IOException {
        System.out.println("[1] 無敵ボット / 1d6=6 固定");
        Game game = new Game(table, 1234, 6);
        Input in = new Input();
        in.shot = true;

        game.update(press(in)); // タイトル → 開始
        check(game.screen() == Game.Screen.PLAYING, "Zでゲーム開始");
        check(game.stage() == 1 && game.lastD100() >= 1 && game.lastD100() <= 100, "ステージ1開始時に1d100: " + game.lastD100());

        String prev = "";
                boolean sawKomy = false, sawSuzu = false, sawFumbleMax = false;
        int[] shotAt = {600, 3000, 6500};
        int shotIdx = 0;
        int maxFrames = 60 * 60 * 12;

        int suzuFrames = 0;
        for (int f = 0; f < maxFrames && suzuFrames < 500; f++) {
            // 狙う対象(ボス優先、なければ一番近い敵)のxに合わせる
            Foe target = game.boss();
            if (target == null) {
                for (Foe e : game.foes()) {
                    if (e.y > 0 && (target == null || e.y > target.y)) target = e;
                }
            }
            in.left = in.right = false;
            if (target != null) {
                double px = game.player().x;
                if (target.x < px - 3) in.left = true;
                else if (target.x > px + 3) in.right = true;
            }
            game.player().invincible = 999;
            game.update(in);

            Foe b = game.boss();
            String state = game.phase() + ":" + (b == null ? "-" : b.getClass().getSimpleName());
            if (!state.equals(prev)) {
                System.out.printf("  f=%5d stage=%d phase=%s boss=%s d100=%d d6hist=%s%n", f, game.stage(), game.phase(),
                        b == null ? "-" : b.getClass().getSimpleName(), game.lastD100(), game.d6History());
                prev = state;
            }
            if (b instanceof ExBossKomy) sawKomy = true;
            if (b instanceof ExBossSuzu) sawSuzu = true;
            if (game.phase() == Game.Phase.WAVES && game.stage() >= 2 && game.stageFrame() == 1 && game.difficulty().level == 100) {
                sawFumbleMax = true;
            }
            if (shotIdx < shotAt.length && f == shotAt[shotIdx]) {
                save(renderer, game, new File(outDir, "god_" + f + ".png"));
                shotIdx++;
            }
            if (sawSuzu && game.boss() instanceof ExBossSuzu) {
                suzuFrames++;
                // 消える演出の途中と出現直後を保存
                if (suzuFrames == 200 || suzuFrames == 330) save(renderer, game, new File(outDir, "suzu_" + suzuFrames + ".png"));
            }
        }
        check(game.stage() >= 4, "複数ステージ進行 (stage=" + game.stage() + ")");
        check(sawFumbleMax, "6が出たあとの次ステージは難易度100(最高難易度)");
        check(sawKomy, "666の後にコーミャが出現");
        check(sawSuzu, "コーミャ撃破後に再度666でスズが出現");
    }

    private static void mortalRun(List<EnemyData> table, Renderer renderer, File outDir) throws IOException {
        System.out.println("[2] 通常ボット(無敵なし)");
        Game game = new Game(table, 99, 0);
        Input in = new Input();
        in.shot = true;
        game.update(press(in));
        int frames = 0;
        while (game.screen() == Game.Screen.PLAYING && frames < 60 * 60 * 10) {
            in.left = false;
            in.right = false;
            game.update(in);
            frames++;
        }
        save(renderer, game, new File(outDir, "mortal_end.png"));
        check(game.screen() == Game.Screen.GAME_OVER, "動かないと被弾してゲームオーバーになる (frames=" + frames + ", score=" + game.score() + ")");
        check(game.player().lives == 0, "残機が0");
    }

    @Contract("_ -> param1")
    private static @NotNull Input press(@NotNull Input in) {
        in.pressConfirm();
        return in;
    }

    private static void save(@NotNull Renderer renderer, Game game, File file) throws IOException {
        BufferedImage img = new BufferedImage(Defs.WINDOW_W, Defs.WINDOW_H, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        renderer.render(g, game);
        g.dispose();
        ImageIO.write(img, "png", file);
    }
}
