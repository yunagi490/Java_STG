package vscdxstg;

import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * ゲーム本体のロジック(元: CONTROL)。描画・Swing には依存しない。
 *
 * 進行: ステージ開始(1d100で難易度) → ザコ波 → ボス → 撃破時に1d6
 *       (6=ファンブル→次ステージは最高難易度、666でExBoss出現) → 次ステージ
 */
final class Game {
    enum Screen { TITLE, PLAYING, GAME_OVER }

    enum Phase { WAVES, BOSS_WARNING, BOSS, CLEAR, EXBOSS_WARNING, EXBOSS }

    private enum ExKind { KOMY, SUZU }

    private final List<EnemyData> enemyTable;
    private final Random rng;
    private final Dice dice;

    // --- 状態 ---
    private Screen screen = Screen.TITLE;
    private Phase phase = Phase.WAVES;
    private boolean paused;

    private final Player player = new Player();
    private final List<PlayerShot> pshots = new ArrayList<>();
    private final List<Bullet> bullets = new ArrayList<>();
    private final List<Foe> foes = new ArrayList<>();
    private Foe boss;

    private int stage;
    private int score;
    private int stageFrame;
    private int frame;
    private double bgY;
    private int gameOverTimer;

    private Difficulty difficulty = new Difficulty(1);
    private int lastD100;
    private int lastD6;
    private final List<Integer> d6History = new ArrayList<>();
    private boolean fumblePending;
    private boolean komyDefeated;
    private ExKind pendingEx;
    private ExKind currentEx;

    private int timer;
    private String[] bannerLines = new String[0];
    private int bannerTimer;

    Game(List<EnemyData> enemyTable, long seed, int forcedD6) {
        this.enemyTable = List.copyOf(enemyTable);
        this.rng = new Random(seed);
        this.dice = new Dice(rng, forcedD6);
    }

    // ---------------------------------------------------------------- 参照用

    Screen screen() { return screen; }
    Phase phase() { return phase; }
    boolean paused() { return paused; }
    Player player() { return player; }
    List<PlayerShot> playerShots() { return pshots; }
    List<Bullet> bullets() { return bullets; }
    List<Foe> foes() { return foes; }
    Foe boss() { return boss; }
    int stage() { return stage; }
    int score() { return score; }
    int stageFrame() { return stageFrame; }
    int frame() { return frame; }
    double bgY() { return bgY; }
    Difficulty difficulty() { return difficulty; }
    int lastD100() { return lastD100; }
    int lastD6() { return lastD6; }
    List<Integer> d6History() { return d6History; }
    boolean fumblePending() { return fumblePending; }
    String[] bannerLines() { return bannerTimer > 0 ? bannerLines : new String[0]; }
    boolean warning() { return phase == Phase.BOSS_WARNING || phase == Phase.EXBOSS_WARNING; }
    boolean exWarning() { return phase == Phase.EXBOSS_WARNING; }
    String exWarningName() { return pendingEx == ExKind.SUZU ? "スズ" : "コーミャ"; }
    Random rng() { return rng; }

    // ---------------------------------------------------------------- 更新

    void update(Input in) {
        frame++;
        bgY = (bgY + Defs.SCROLL_SPEED) % Defs.BG_H;

        switch (screen) {
            case TITLE -> {
                if (in.consumeConfirm()) {
                    startGame();
                }
            }
            case GAME_OVER -> {
                gameOverTimer++;
                if (in.consumeConfirm() && gameOverTimer > 60) {
                    screen = Screen.TITLE;
                }
            }
            case PLAYING -> {
                if (in.consumePause()) {
                    paused = !paused;
                }
                if (!paused) {
                    updatePlaying(in);
                }
            }
        }
    }

    private void startGame() {
        score = 0;
        stage = 0;
        fumblePending = false;
        komyDefeated = false;
        pendingEx = null;
        currentEx = null;
        lastD100 = 0;
        lastD6 = 0;
        d6History.clear();
        player.lives = Defs.PLAYER_LIVES;
        player.alive = true;
        player.respawn();
        player.invincible = 0;
        pshots.clear();
        bullets.clear();
        foes.clear();
        boss = null;
        paused = false;
        screen = Screen.PLAYING;
        startStage();
    }

    /** ステージ開始: 1d100 で難易度を決める(ファンブル後は最高難易度)。 */
    private void startStage() {
        stage++;
        boolean fumbled = fumblePending;
        int level = fumbled ? Difficulty.MAX : dice.roll(100);
        fumblePending = false;

        difficulty = new Difficulty(level);
        lastD100 = level;

        foes.clear();
        bullets.clear();
        boss = null;
        stageFrame = 0;
        phase = Phase.WAVES;
        for (EnemyData d : enemyTable) {
            foes.add(new Enemy(d, difficulty));
        }

        String line2 = "1d100 = " + level + "   [" + difficulty.label() + "]";
        if (fumbled) {
            line2 += "  (ファンブル)";
        }
        showBanner("STAGE " + stage, line2);
    }

    private void updatePlaying(Input in) {
        if (bannerTimer > 0) bannerTimer--;

        // --- 自機 ---
        if (player.alive) {
            player.update(in);
            if (in.shot && player.shotTimer <= 0) {
                pshots.add(new PlayerShot(player.x - 8, player.y - 16));
                pshots.add(new PlayerShot(player.x + 8, player.y - 16));
                player.shotTimer = Defs.PSHOT_INTERVAL;
            }
        }

        // --- フェーズ進行 ---
        switch (phase) {
            case WAVES -> {
                stageFrame++;
                if (foes.isEmpty()) {
                    phase = Phase.BOSS_WARNING;
                    timer = Defs.WARNING_FRAMES;
                }
            }
            case BOSS_WARNING -> {
                if (--timer <= 0) {
                    spawnBoss(new StageBoss(difficulty), Phase.BOSS);
                }
            }
            case EXBOSS_WARNING -> {
                if (--timer <= 0) {
                    currentEx = pendingEx;
                    pendingEx = null;
                    Foe ex = currentEx == ExKind.SUZU ? new ExBossSuzu(difficulty) : new ExBossKomy(difficulty);
                    spawnBoss(ex, Phase.EXBOSS);
                }
            }
            case CLEAR -> {
                if (--timer <= 0) {
                    if (pendingEx != null) {
                        phase = Phase.EXBOSS_WARNING;
                        timer = Defs.WARNING_FRAMES;
                    } else {
                        startStage();
                    }
                }
            }
            case BOSS, EXBOSS -> { }
        }

        // --- 敵・弾の更新 ---
        for (Foe f : new ArrayList<>(foes)) {
            f.update(this);
        }
        for (Bullet b : bullets) b.update();
        for (PlayerShot s : pshots) s.update();

        collide();

        foes.removeIf(f -> f.dead);
        bullets.removeIf(b -> !b.alive);
        pshots.removeIf(s -> !s.alive);

        if (boss != null && boss.dead) {
            Foe defeated = boss;
            boss = null;
            onBossDefeated(defeated);
        }
    }

    private void spawnBoss(Foe b, Phase next) {
        boss = b;
        foes.add(b);
        phase = next;
    }

    // ---------------------------------------------------------------- 当たり判定

    private static double dist2(double ax, double ay, double bx, double by) {
        double dx = ax - bx;
        double dy = ay - by;
        return dx * dx + dy * dy;
    }

    private void collide() {
        // 自機の弾 vs 敵
        for (PlayerShot s : pshots) {
            if (!s.alive) continue;
            for (Foe f : foes) {
                if (f.dead || !f.onField()) continue;
                double r = Defs.PSHOT_HIT_R + f.hitR;
                if (dist2(s.x, s.y, f.x, f.y) <= r * r) {
                    s.alive = false;
                    if (!f.invulnerable) {
                        score += 10;
                        if (f.damage(1)) {
                            score += f.score();
                        }
                    }
                    break;
                }
            }
        }

        // 敵の弾・体当たり vs 自機
        if (!player.alive || player.invincible > 0) return;

        double br = Defs.BULLET_HIT_R + Defs.PLAYER_HIT_R;
        for (Bullet b : bullets) {
            if (b.alive && dist2(b.x, b.y, player.x, player.y) <= br * br) {
                hitPlayer();
                return;
            }
        }
        for (Foe f : foes) {
            if (!f.harmful()) continue;
            double r = f.hitR * 0.8 + Defs.PLAYER_HIT_R;
            if (dist2(f.x, f.y, player.x, player.y) <= r * r) {
                hitPlayer();
                return;
            }
        }
    }

    private void hitPlayer() {
        player.lives--;
        bullets.clear();
        if (player.lives <= 0) {
            player.alive = false;
            screen = Screen.GAME_OVER;
            gameOverTimer = 0;
            return;
        }
        player.respawn();
        for (Foe f : foes) {
            f.onPlayerRespawn(this);
        }
    }

    // ---------------------------------------------------------------- ボス撃破

    private void onBossDefeated(Foe defeated) {
        bullets.clear();

        if (phase == Phase.EXBOSS) {
            if (currentEx == ExKind.KOMY) {
                komyDefeated = true;
            }
            currentEx = null;
            showBanner("EX BOSS 撃破!");
            phase = Phase.CLEAR;
            timer = Defs.BANNER_FRAMES;
            return;
        }

        // 通常ボス撃破 → 1d6
        int r = dice.roll(6);
        lastD6 = r;
        d6History.add(r);

        String line2 = "1d6 = " + r;
        if (r == 6) {
            fumblePending = true;
            line2 += "   ファンブル!! 次は最高難易度";
        }
        showBanner("BOSS 撃破!", line2);

        pendingEx = checkExBoss();
        phase = Phase.CLEAR;
        timer = Defs.BANNER_FRAMES;
    }

    /** 直近3回の1d6がすべて6(666)ならExBossを出す。 */
    private @Nullable ExKind checkExBoss() {
        int n = d6History.size();
        if (n < 3) return null;
        for (int i = n - 3; i < n; i++) {
            if (d6History.get(i) != 6) return null;
        }
        d6History.clear();
        return komyDefeated ? ExKind.SUZU : ExKind.KOMY;
    }

    // ---------------------------------------------------------------- ユーティリティ

    /** 敵の弾を撃つ。 */
    void fire(double x, double y, double rad, double speed) {
        bullets.add(new Bullet(x, y, rad, speed));
    }

    private void showBanner(String... lines) {
        bannerLines = lines;
        bannerTimer = Defs.BANNER_FRAMES;
    }
}
