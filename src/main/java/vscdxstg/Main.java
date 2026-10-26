package vscdxstg;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Random;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

/**
 * 起動クラス。
 *
 * オプション
 *   --enemy-csv=PATH  同梱の Enemy.csv の代わりに外部ファイルを読む(再ビルド不要で敵配置を調整できる)
 *   --seed=N          乱数シードを固定する
 *   --d6=N            ボス撃破後の1d6を常にNにする(666でのExBoss確認用デバッグ)
 */
public final class Main {
    private Main() {}

    public static void main(String @NotNull [] args) {
        Path csv = null;
        long seed = new Random().nextLong();
        int forcedD6 = 0;

        for (String a : args) {
            if (a.startsWith("--enemy-csv=")) {
                csv = Path.of(a.substring("--enemy-csv=".length()));
            } else if (a.startsWith("--seed=")) {
                seed = Long.parseLong(a.substring("--seed=".length()));
            } else if (a.startsWith("--d6=")) {
                forcedD6 = Integer.parseInt(a.substring("--d6=".length()));
            }
        }

        final Path csvPath = csv;
        final long seedF = seed;
        final int forcedF = forcedD6;

        SwingUtilities.invokeLater(() -> {
            try {
                List<EnemyData> table = (csvPath != null && Files.exists(csvPath))
                        ? EnemyData.loadFile(csvPath)
                        : EnemyData.loadResource("/Resource/Enemy.csv");
                Assets assets = new Assets();
                Game game = new Game(table, seedF, forcedF);
                GamePanel panel = new GamePanel(game, new Renderer(assets));
                
                JFrame frame = new JFrame("時雨STG");
                frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                frame.setResizable(false);
                frame.add(panel);
                frame.pack();
                frame.setLocationRelativeTo(null);
                frame.setVisible(true);
                panel.start();
            } catch (IOException e) {
                JOptionPane.showMessageDialog(null, e.getMessage(), "起動エラー", JOptionPane.ERROR_MESSAGE);
                System.exit(1);
            }
        });
    }
}
