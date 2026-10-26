package vscdxstg;

/**
 * ステージ開始時の 1d100 で決まる難易度(1〜100)。
 * 数字が大きいほど高難易度: 弾が速く、発射間隔が短く、敵HPが増える。
 */
final class Difficulty {
    static final int MAX = 100;

    final int level;

    Difficulty(int level) {
        this.level = Math.max(1, Math.min(MAX, level));
    }

    double bulletSpeedMul() {
        return 0.75 + 0.85 * level / 100.0;
    }

    double intervalMul() {
        return 1.30 - 0.60 * level / 100.0;
    }

    double hpMul() {
        return 1.0 + level / 100.0;
    }

    String label() {
        if (level >= 100) return "Apotheosis";
        if (level >= 96) return "Ragnarok";
        if (level >= 90) return "LUNATIC";
        if (level >= 80) return "HARD";
        if (level >= 60) return "NORMAL";
        if (level >= 30) return "EASY";
        return "EGG";
    }
}
