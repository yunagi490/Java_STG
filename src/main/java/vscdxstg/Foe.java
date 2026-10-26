package vscdxstg;

/** 敵・ボス共通の基底クラス。 */
abstract class Foe {
    double x, y;
    int hp, maxHp;
    double hitR;
    int size;
    /** Assets 側のスプライト名 */
    String sprite;
    /** 0.0〜1.0。ExBoss(スズ)の消える演出で使う */
    double alpha = 1.0;

    boolean dead;
    boolean killed;
    boolean invulnerable;

    abstract void update(Game g);

    abstract int score();

    /** プレイヤーの再出現を通知(コーミャの「リスキル」用)。 */
    void onPlayerRespawn(Game g) {}

    boolean isBoss() {
        return false;
    }

    boolean onField() {
        return x > -20 && x < Defs.FIELD_W + 20 && y > -10 && y < Defs.FIELD_H + 20;
    }

    /** 体当たりでプレイヤーにダメージを与える状態か */
    boolean harmful() {
        return !dead && alpha > 0.5 && onField();
    }

    /** @return この攻撃で倒したなら true */
    boolean damage(int d) {
        hp -= d;
        if (hp <= 0 && !dead) {
            dead = true;
            killed = true;
            return true;
        }
        return false;
    }

    static double step(double cur, double target, double max) {
        double d = target - cur;
        if (Math.abs(d) <= max) return target;
        return cur + Math.signum(d) * max;
    }

    static double clamp(double v, double lo, double hi) {
        return Math.max(lo, Math.min(hi, v));
    }
}
