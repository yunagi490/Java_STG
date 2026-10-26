package vscdxstg;

import java.util.Random;

/** ゲーム案のダイス判定(1d100 / 1d6)。テスト・デバッグ用に1d6の目を固定できる。 */
final class Dice {
    private final Random rng;
    private final int forcedD6; // 0 なら固定しない

    Dice(Random rng, int forcedD6) {
        this.rng = rng;
        this.forcedD6 = forcedD6;
    }

    int roll(int sides) {
        if (forcedD6 > 0 && sides == 6) {
            return forcedD6;
        }
        return rng.nextInt(sides) + 1;
    }
}
