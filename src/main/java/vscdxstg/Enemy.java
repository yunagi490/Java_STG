package vscdxstg;

/**
 * ザコ敵(元: class ENEMY)。Enemy.csv の1行から生成される。
 *
 * 移動パターン(m_pattern)
 *   0: 出現→停止時間まで下降→帰還時間で上に戻る
 *   1: 止まらず下に行く
 *   2: 少しずつ左に流れながら下降
 *   3: 少しずつ右に流れながら下降
 * 発射パターン(s_pattern)
 *   0: プレイヤーへ直線(5発)  1: 直線連射(10発)  2: 3方向(5回)  3: 乱射
 */
final class Enemy extends Foe {
    private final EnemyData d;
    private final Difficulty diff;

    private int count;      // 出現してからのカウント
    private int scount;     // 撃てるようになってからのカウント
    private boolean sflag;  // 撃てるようになったか
    private double rad;     // 発射直前に決めた狙い角

    Enemy(EnemyData d, Difficulty diff) {
        this.d = d;
        this.diff = diff;
        this.x = d.x();
        this.y = d.y();
        this.hp = this.maxHp = Math.max(1, (int) Math.round(d.hp() * diff.hpMul()));
        this.hitR = Defs.ENEMY_HIT_R;
        this.size = Defs.ENEMY_SIZE;
        this.sprite = "enemy";
    }

    @Override
    int score() {
        return 100;
    }

    @Override
    void update(Game g) {
        move(g);
        shoot(g);
        count++;
    }

    private void move(Game g) {
        int t = g.stageFrame();
        switch (d.mPattern()) {
            case 0 -> {
                if (d.inTime() < t && t < d.stopTime()) {
                    y += 2;
                } else if (t > d.outTime()) {
                    y -= 2;
                }
            }
            case 1 -> {
                if (d.inTime() <= t) y += 2;
            }
            case 2 -> {
                if (d.inTime() <= t) {
                    y += 1;
                    if (count % 10 == 0) x -= 1;
                }
            }
            case 3 -> {
                if (d.inTime() <= t) {
                    y += 1;
                    if (count % 10 == 0) x += 1;
                }
            }
            default -> { }
        }
        // 画面からはみ出したら退場
        if (t >= d.stopTime() && outCheck()) {
            dead = true;
        }
    }

    private boolean outCheck() {
        return x < -50 || x > Defs.FIELD_W + 50 || y < -50 || y > Defs.FIELD_H + 50;
    }

    /** 難易度に応じて発射間隔を縮める */
    private int interval(int base) {
        return Math.max(2, (int) Math.round(base * diff.intervalMul()));
    }

    private void shoot(Game g) {
        if (d.shotTime() == g.stageFrame()) {
            sflag = true;
        }
        if (!sflag) return;

        Player p = g.player();
        double speed = d.speed() * diff.bulletSpeedMul();
        // 狙い角は最初の1回だけ決める(元の実装どおり)
        if (scount == 0) {
            rad = Math.atan2(p.y - y, p.x - x);
        }

        switch (d.sPattern()) {
            case 0 -> {
                int iv = interval(10);
                if (scount % iv == 0 && scount / iv < 5) {
                    g.fire(x, y, rad, speed);
                }
            }
            case 1 -> {
                int iv = interval(6);
                if (scount % iv == 0 && scount / iv < 10) {
                    g.fire(x, y, rad, speed);
                }
            }
            case 2 -> {
                int iv = interval(10);
                if (scount % iv == 0 && scount / iv < 5) {
                    double spread = Math.toRadians(10);
                    g.fire(x, y, rad - spread, speed);
                    g.fire(x, y, rad, speed);
                    g.fire(x, y, rad + spread, speed);
                }
            }
            case 3 -> {
                int iv = interval(3);
                if (scount % iv == 0 && scount / iv < 40) {
                    double aim = Math.atan2(p.y - y, p.x - x);
                    double off = Math.toRadians(-60 + g.rng().nextInt(120));
                    g.fire(x, y, aim + off, speed);
                }
            }
            default -> { }
        }
        scount++;
    }
}
