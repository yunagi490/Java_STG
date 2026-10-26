package vscdxstg;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Enemy.csv の1行分(元: struct ENEMYDATA)。
 * 列: 敵種類,弾種類,移動パターン,発射パターン,出現時間,停止,発射,帰還,X座標,Y座標,弾スピード,HP,アイテム
 */
record EnemyData(int type, int stype, int mPattern, int sPattern,
                 int inTime, int stopTime, int shotTime, int outTime,
                 int x, int y, int speed, int hp, int item) {

    private static final int COLUMNS = 13;

    static List<EnemyData> loadFile(Path path) throws IOException {
        try (BufferedReader r = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            return parse(r);
        }
    }

    static List<EnemyData> loadResource(String resourcePath) throws IOException {
        try (InputStream in = EnemyData.class.getResourceAsStream(resourcePath)) {
            if (in == null) {
                throw new IOException("リソースが見つかりません: " + resourcePath);
            }
            return parse(new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8)));
        }
    }

    static List<EnemyData> parse(BufferedReader r) throws IOException {
        List<EnemyData> list = new ArrayList<>();
        String line = r.readLine(); // ヘッダ行を飛ばす
        if (line == null) {
            return list;
        }
        int lineNo = 1;
        while ((line = r.readLine()) != null) {
            lineNo++;
            line = line.strip();
            if (line.isEmpty()) {
                continue;
            }
            String[] c = line.split(",", -1);
            if (c.length < COLUMNS) {
                throw new IOException("Enemy.csv " + lineNo + "行目: 列が足りません(" + c.length + "/" + COLUMNS + ")");
            }
            try {
                list.add(new EnemyData(
                        n(c[0]), n(c[1]), n(c[2]), n(c[3]),
                        n(c[4]), n(c[5]), n(c[6]), n(c[7]),
                        n(c[8]), n(c[9]), n(c[10]), n(c[11]), n(c[12])));
            } catch (NumberFormatException e) {
                throw new IOException("Enemy.csv " + lineNo + "行目: 数値として読めません: " + line, e);
            }
        }
        return list;
    }

    private static int n(String s) {
        return Integer.parseInt(s.strip());
    }
}
