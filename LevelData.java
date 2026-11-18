import java.util.ArrayList;
import java.util.List;

/**
 * Dữ liệu của một màn chơi, do LevelLoader tạo ra.
 * QUY ƯỚC (chưa chốt với A): toạ độ (x, y) = (cột, hàng); truy cập grid[y][x].
 */
public class LevelData {
    String name;
    char[][] grid;                          // chỉ '#' (tường) và ' ' (sàn)
    Player player;                          // vị trí
    List<Box> boxes = new ArrayList<>();    // thùng
    List<int[]> goals = new ArrayList<>();  // mỗi phần tử {x, y}

    public int width()  { return grid[0].length; }
    public int height() { return grid.length; }

    /** Tạo bản sao độc lập (deep copy): sửa bản sao không ảnh hưởng bản gốc. */
    public LevelData copy() {
        LevelData c = new LevelData();
        c.name = name;
        c.grid = new char[grid.length][];
        for (int y = 0; y < grid.length; y++) c.grid[y] = grid[y].clone();
        c.player = new Player(player.x, player.y);
        for (Box b : boxes) c.boxes.add(new Box(b.x, b.y));
        for (int[] g : goals) c.goals.add(g.clone());
        return c;
    }
}
