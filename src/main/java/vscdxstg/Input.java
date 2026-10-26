package vscdxstg;

/** キー入力の状態。押しっぱなし系はフィールド、1回押し系は consume で取り出す。 */
final class Input {
    boolean left, right, up, down;
    boolean shot;
    boolean slow;

    private boolean confirm;
    private boolean pause;

    void pressConfirm() { confirm = true; }
    void pressPause() { pause = true; }

    boolean consumeConfirm() {
        boolean c = confirm;
        confirm = false;
        return c;
    }

    boolean consumePause() {
        boolean p = pause;
        pause = false;
        return p;
    }
}
