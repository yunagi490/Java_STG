package vscdxstg;

/**
 * ExBoss「コーミャ」(ゲーム案: ExBEnemy_Y)。
 * 近づいて撃つ → 離れて撃つ の繰り返し。たまにリスキルもする。
 * 出現条件: ダイス(1d6)で 6 が3回(666)出たあと、ボス撃破後に出現。
 */
final class ExBossKomy extends Foe {
    private enum Phase { ENTER, APPROACH, RETREAT }

    private static final int PHASE_FRAMES = 150;

    private final Difficulty diff;
    private Phase phase = Phase.ENTER;
    private int phaseT;

    // リスキル(プレイヤーの復活地点を狙い撃つ)
    private int reskillTimer = -1;
    private double reskillX, reskillY;

    ExBossKomy(Difficulty diff) {
        this.diff = diff;
        this.x = Defs.FIELD_W / 2.0;
        this.y = -80;
        this.hp = this.maxHp = (int) Math.round(200 * (0.8 + 0.6 * diff.level / 100.0));
        this.hitR = Defs.BOSS_HIT_R;
        this.size = Defs.BOSS_SIZE;
        this.sprite = "komy";
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
        double speed = 3.5 * diff.bulletSpeedMul();

        switch (phase) {
            case ENTER -> {
                y += 3;
                if (y >= 80) {
                    y = 80;
                    invulnerable = false;
                    setPhase(Phase.APPROACH);
                }
            }
            case APPROACH -> {
                // プレイヤーの近くまで寄って撃つ
                x = step(x, p.x, 2.5);
                y = step(y, clamp(p.y - 130, 60, 300), 2.5);
                if (phaseT % interval(25) == 0) {
                    double aim = Math.atan2(p.y - y, p.x - x);
                    double spread = Math.toRadians(12);
                    g.fire(x, y, aim - spread, speed);
                    g.fire(x, y, aim, speed);
                    g.fire(x, y, aim + spread, speed);
                }
                if (phaseT >= PHASE_FRAMES) setPhase(Phase.RETREAT);
            }
            case RETREAT -> {
                // 上に離れて、速い狙い撃ちを連発
                x = step(x, Defs.FIELD_W / 2.0, 1.5);
                y = step(y, 50, 3.0);
                if (phaseT % interval(10) == 0) {
                    double aim = Math.atan2(p.y - y, p.x - x);
                    g.fire(x, y, aim, speed * 1.5);
                }
                if (phaseT >= PHASE_FRAMES) setPhase(Phase.APPROACH);
            }
        }

        if (reskillTimer > 0 && --reskillTimer == 0) {
            reskill(g);
        }
    }

    @Override
    void onPlayerRespawn(Game g) {
        // 6割の確率でリスキルを狙う
        if (!invulnerable && g.rng().nextInt(100) < 60) {
            reskillTimer = 30;
            reskillX = g.player().x;
            reskillY = g.player().y;
        }
    }

    /** 復活地点に向けて、無敵が切れる頃に着弾する弾幕を撃つ。 */
    private void reskill(Game g) {
        double aim = Math.atan2(reskillY - y, reskillX - x);
        double speed = 5.0 * diff.bulletSpeedMul();
        for (int i = -3; i <= 3; i++) {
            g.fire(x, y, aim + Math.toRadians(i * 6), speed);
        }
    }
}
