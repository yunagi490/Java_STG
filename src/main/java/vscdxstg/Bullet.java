package vscdxstg;

/** 敵の弾(元: struct E_SHOT)。角度(ラジアン)と速さで直進する。 */
final class Bullet {
    double x, y;
    final double vx, vy;
    boolean alive = true;

    Bullet(double x, double y, double rad, double speed) {
        this.x = x;
        this.y = y;
        this.vx = Math.cos(rad) * speed;
        this.vy = Math.sin(rad) * speed;
    }

    void update() {
        x += vx;
        y += vy;
        if (x < -20 || x > Defs.FIELD_W + 20 || y < -20 || y > Defs.FIELD_H + 40) {
            alive = false;
        }
    }
}
