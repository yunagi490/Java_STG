package vscdxstg;

/** 元の define.h に相当する定数群。座標はすべて「プレイフィールド内のローカル座標」。 */
final class Defs {
    private Defs() {}

    static final int WINDOW_W = 640;
    static final int WINDOW_H = 480;
    static final int MARGIN = 10;

    /** プレイフィールドの大きさ(元: SetDrawArea(MARGIN, MARGIN, MARGIN+300, MARGIN+460)) */
    static final int FIELD_W = 300;
    static final int FIELD_H = 460;

    static final int FPS = 120;

    // --- プレイヤー ---
    static final double PLAYER_SPEED = 4.0;
    static final double PLAYER_SLOW_SPEED = 2.0;
    static final double DIAGONAL = 0.71;
    static final int PLAYER_SIZE = 48;
    static final double PLAYER_HIT_R = 3.0;
    static final int PLAYER_LIVES = 3;
    static final int RESPAWN_INVINCIBLE = 90;

    // --- プレイヤーの弾 ---
    static final int PSHOT_SPEED = 14;
    static final int PSHOT_INTERVAL = 6;
    static final int PSHOT_SIZE = 20;
    static final double PSHOT_HIT_R = 6.0;

    // --- 背景 ---
    static final int SCROLL_SPEED = 2;
    static final int BG_H = 460;

    // --- 敵 ---
    static final int ENEMY_SIZE = 64;
    static final double ENEMY_HIT_R = 22.0;
    static final int BOSS_SIZE = 128;
    static final double BOSS_HIT_R = 30.0;
    static final int BULLET_SIZE = 16;
    static final double BULLET_HIT_R = 4.0;

    // --- 演出 ---
    static final int WARNING_FRAMES = 120;
    static final int BANNER_FRAMES = 150;
}
