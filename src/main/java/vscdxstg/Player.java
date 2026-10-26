package vscdxstg;

import org.jetbrains.annotations.NotNull;

final class Player {
    double x, y;
    int lives = Defs.PLAYER_LIVES;
    int invincible;
    int shotTimer;
    boolean slow;
    boolean alive = true;

    Player() {
        resetPosition();
    }

    void resetPosition() {
        x = Defs.FIELD_W / 2.0;
        y = 400;
    }

    void respawn() {
        resetPosition();
        invincible = Defs.RESPAWN_INVINCIBLE;
        shotTimer = 0;
    }

    /** 移動処理。斜め移動は係数0.71(元: move)。画面外には出ない。 */
    void update(@NotNull Input in) {
        slow = in.slow;
        double speed = slow ? Defs.PLAYER_SLOW_SPEED : Defs.PLAYER_SPEED;

        int dx = (in.right ? 1 : 0) - (in.left ? 1 : 0);
        int dy = (in.down ? 1 : 0) - (in.up ? 1 : 0);
        double move = (dx != 0 && dy != 0) ? Defs.DIAGONAL : 1.0;

        x += dx * speed * move;
        y += dy * speed * move;

        double half = Defs.PLAYER_SIZE / 2.0;
        x = Math.max(half, Math.min(Defs.FIELD_W - half, x));
        y = Math.max(half, Math.min(Defs.FIELD_H - half, y));

        if (invincible > 0) invincible--;
        if (shotTimer > 0) shotTimer--;
    }
}
