package vscdxstg;

/** プレイヤーの弾(元: struct SHOT)。真上に飛ぶ。 */
final class PlayerShot {
    double x, y;
    boolean alive = true;

    PlayerShot(double x, double y) {
        this.x = x;
        this.y = y;
    }

    void update() {
        y -= Defs.PSHOT_SPEED;
        if (y < -10) {
            alive = false;
        }
    }
}
