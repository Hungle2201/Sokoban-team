import java.nio.file.Path;
import java.util.List;

/** Chạy: java LevelLoaderTest duong_dan/Levels.txt */
public class LevelLoaderTest {
    public static void main(String[] args) throws Exception {
        String path = args.length > 0 ? args[0] : "rsc/Levels.txt";
        List<LevelData> levels = LevelLoader.loadAll(Path.of(path));
        for (LevelData lv : levels) {
            System.out.printf("%-20s %dx%d  player=(%d,%d)  boxes=%d  goals=%d%n",
                    lv.name, lv.width(), lv.height(), lv.player.x, lv.player.y,
                    lv.boxes.size(), lv.goals.size());
        }
        System.out.println("Tổng: " + levels.size() + " màn");
    }
}
