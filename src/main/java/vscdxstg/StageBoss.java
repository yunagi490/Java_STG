package vscdxstg;

/** ステージボス。登場 → 左右に揺れながら「全方位リング弾」と「3方向狙い弾」を交互に撃つ。 */
final class StageBoss extends Foe {
    private static final int ENTER_FRAMES = 90;
    private static final double START_Y = -80;
    private static final double STAY_Y = 90;

    private final Difficulty diff;
    private int t;
    private double ringOffset;

    StageBoss(Difficulty diff) {
        this.diff = diff;
        this.x = Defs.FIELD_W / 2.0;
        this.y = START_Y;
        this.hp = this.maxHp = (int) Math.round(120 * (0.8 + 0.6 * diff.level / 100.0));
        this.hitR = Defs.BOSS_HIT_R;
        this.size = Defs.BOSS_SIZE;
        this.sprite = "boss";
        this.invulnerable = true;
    }

    @Override
    boolean isBoss() {
        return true;
    }

    @Override
    int score() {
        return 5000;
    }

    private int interval(int base) {
        return Math.max(2, (int) Math.round(base * diff.intervalMul()));
    }

    @Override
    void update(Game g) {
        t++;
        if (t <= ENTER_FRAMES) {
            y = START_Y + (STAY_Y - START_Y) * t / ENTER_FRAMES;
            if (t == ENTER_FRAMES) invulnerable = false;
            return;
        }

        int u = t - ENTER_FRAMES;
        x = Defs.FIELD_W / 2.0 + Math.sin(u / 60.0) * 90;
        double speed = 3.0 * diff.bulletSpeedMul();
        int cycle = u % 240;

        if (cycle < 120) {
            if (cycle % interval(40) == 0) {
                int n = 8;
                for (int i = 0; i < n; i++) {
                    g.fire(x, y, ringOffset + i * 2 * Math.PI / n, speed);
                }
                ringOffset += 0.2;
            }
        } else if (cycle % interval(20) == 0) {
            double aim = Math.atan2(g.player().y - y, g.player().x - x);
            double spread = Math.toRadians(14);
            g.fire(x, y, aim - spread, speed * 1.3);
            g.fire(x, y, aim, speed * 1.3);
            g.fire(x, y, aim + spread, speed * 1.3);
        }
    }
}
