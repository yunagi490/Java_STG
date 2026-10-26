package vscdxstg;

/**
 * ExBoss「スズ」(ゲーム案: ExBEnemy_M)。
 * 無限に遠距離から狙ってくる。たまに消えて、油断したところを撃ってくる。
 * 出現条件: コーミャ撃破済みの状態でもう一度 666 が出たあと、ボス撃破後に出現。

 */
final class ExBossSuzu extends Foe {
    private enum Phase { ENTER, VISIBLE, FADE_OUT, HIDDEN, FADE_IN }

    private static final int FADE_FRAMES = 30;
    private static final int HIDDEN_FRAMES = 90;
    private static final double FAR_Y = 60;

    private final Difficulty diff;
    private Phase phase = Phase.ENTER;
    private int phaseT;
    private int visibleLength = 300;

    ExBossSuzu(Difficulty diff) {
        this.diff = diff;
        this.x = Defs.FIELD_W / 2.0;
        this.y = -80;
        this.hp = this.maxHp = (int) Math.round(200 * (0.8 + 0.6 * diff.level / 100.0));
        this.hitR = Defs.BOSS_HIT_R;
        this.size = Defs.BOSS_SIZE;
        this.sprite = "suzu";
        this.invulnerable = true;
    }

    @Override
    boolean isBoss() {
        return true;
    }

    @Override
    int score() {
        return 10000;
    }

    private int interval(int base) {
        return Math.max(2, (int) Math.round(base * diff.intervalMul()));
    }

    private void setPhase(Phase p) {
        phase = p;
        phaseT = 0;
    }

    @Override
    void update(Game g) {
        phaseT++;
        Player p = g.player();

        switch (phase) {
            case ENTER -> {
                y += 3;
                if (y >= FAR_Y) {
                    y = FAR_Y;
                    invulnerable = false;
                    setPhase(Phase.VISIBLE);
                }
            }
            case VISIBLE -> {
                // 遠くからプレイヤーの横位置にゆっくり合わせて、狙い撃ちし続ける
                x += (p.x - x) * 0.01;
                double speed = 4.5 * diff.bulletSpeedMul();
                if (phaseT % interval(18) == 0) {
                    g.fire(x, y, Math.atan2(p.y - y, p.x - x), speed);
                }
                if (phaseT % interval(90) == 0) {
                    double aim = Math.atan2(p.y - y, p.x - x);
                    for (int i = -2; i <= 2; i++) {
                        g.fire(x, y, aim + Math.toRadians(i * 9), speed * 0.9);
                    }
                }
                if (phaseT >= visibleLength) {
                    invulnerable = true;
                    setPhase(Phase.FADE_OUT);
                }
            }
            case FADE_OUT -> {
                alpha = 1.0 - (double) phaseT / FADE_FRAMES;
                if (phaseT >= FADE_FRAMES) {
                    alpha = 0;
                    // 消えている間に、プレイヤーの近くへ移動しておく
                    x = clamp(p.x + (g.rng().nextInt(201) - 100), 40, Defs.FIELD_W - 40);
                    y = 90 + g.rng().nextInt(60);
                    setPhase(Phase.HIDDEN);
                }
            }
            case HIDDEN -> {
                if (phaseT >= HIDDEN_FRAMES) setPhase(Phase.FADE_IN);
            }
            case FADE_IN -> {
                alpha = (double) phaseT / FADE_FRAMES;
                if (phaseT >= FADE_FRAMES) {
                    alpha = 1;
                    // 油断したところに奇襲の扇状弾
                    double aim = Math.atan2(p.y - y, p.x - x);
                    double speed = 6.0 * diff.bulletSpeedMul();
                    for (int i = -4; i <= 4; i++) {
                        g.fire(x, y, aim + Math.toRadians(i * 10), speed);
                    }
                    invulnerable = false;
                    visibleLength = 240 + g.rng().nextInt(180);
                    setPhase(Phase.VISIBLE);
                }
            }
        }
    }
}
