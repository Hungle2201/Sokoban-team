import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Lưu / nạp tiến trình bằng file text dạng key=value, ví dụ:
 *   version=1
 *   level=4
 *   levelName=5
 *   moves=42
 *   player=3,7
 *   box=5,6
 *   box=8,2
 * Không lưu Undo Stack (đúng yêu cầu bản đầu). Toạ độ theo quy ước (x, y) = (cột, hàng).
 * Số bước đi (moves) do phần logic của A quản lý, SaveManager chỉ lưu/trả lại số đó.
 */
public class SaveManager {
    private static final int VERSION = 1;

    /** Lưu màn hiện tại của manager cùng số bước đi. */
    public static void save(Path file, LevelManager m, int moves) throws IOException {
        LevelData cur = m.getCurrentLevel();
        List<String> out = new ArrayList<>();
        out.add("version=" + VERSION);
        out.add("level=" + m.getCurrentIndex());
        out.add("levelName=" + cur.name);
        out.add("moves=" + moves);
        out.add("player=" + cur.player.x + "," + cur.player.y);
        for (Box b : cur.boxes) out.add("box=" + b.x + "," + b.y);

        // Ghi file tạm rồi đổi tên: nếu lỗi giữa chừng thì file save cũ không bị hỏng
        Path tmp = file.resolveSibling(file.getFileName() + ".tmp");
        Files.write(tmp, out, StandardCharsets.UTF_8);
        Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING);
    }

    /**
     * Nạp file save vào manager, trả về số bước đi đã lưu.
     * Nếu file hỏng/không hợp lệ thì ném IllegalArgumentException và manager KHÔNG bị thay đổi.
     * Sau khi nạp, đối tượng LevelData hiện tại đã bị thay mới: phải gọi lại getCurrentLevel().
     */
    public static int load(Path file, LevelManager m) throws IOException {
        List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);

        Integer version = null, level = null, moves = null;
        String levelName = null;
        int[] player = null;
        List<int[]> boxes = new ArrayList<>();

        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            if (line.isBlank()) continue;
            int eq = line.indexOf('=');
            if (eq < 0) throw bad(i, "thiếu dấu '='");
            String key = line.substring(0, eq).strip();
            String val = line.substring(eq + 1).strip();
            switch (key) {
                case "version":   version = parseInt(val, i); break;
                case "level":     level = parseInt(val, i); break;
                case "levelName": levelName = val; break;
                case "moves":     moves = parseInt(val, i); break;
                case "player":    player = parsePair(val, i); break;
                case "box":       boxes.add(parsePair(val, i)); break;
                default:          throw bad(i, "khoá không biết: '" + key + "'");
            }
        }

        if (version == null || version != VERSION)
            throw new IllegalArgumentException("Save: phiên bản không hỗ trợ (cần " + VERSION + ")");
        if (level == null)  throw new IllegalArgumentException("Save: thiếu 'level'");
        if (moves == null)  throw new IllegalArgumentException("Save: thiếu 'moves'");
        if (player == null) throw new IllegalArgumentException("Save: thiếu 'player'");
        if (moves < 0)      throw new IllegalArgumentException("Save: 'moves' âm");
        if (level < 0 || level >= m.getLevelCount())
            throw new IllegalArgumentException("Save: màn " + level + " không tồn tại (có "
                    + m.getLevelCount() + " màn)");

        // Dựng trạng thái mới trên một bản sao, kiểm tra xong mới đưa vào manager
        LevelData state = m.copyOfOriginal(level);
        if (levelName != null && !levelName.equals(state.name))
            throw new IllegalArgumentException("Save: tên màn không khớp (file save: \"" + levelName
                    + "\", hiện tại: \"" + state.name + "\"). File màn có thể đã bị thay đổi");
        if (boxes.size() != state.boxes.size())
            throw new IllegalArgumentException("Save: có " + boxes.size() + " thùng, màn cần "
                    + state.boxes.size());

        state.player = new Player(player[0], player[1]);
        state.boxes.clear();
        for (int[] b : boxes) state.boxes.add(new Box(b[0], b[1]));
        validateState(state);

        m.restore(level, state);
        return moves;
    }

    /** Kiểm tra vị trí nằm trong bản đồ, không đè tường, không trùng nhau. */
    private static void validateState(LevelData s) {
        checkCell(s, s.player.x, s.player.y, "người chơi");
        Set<Long> used = new HashSet<>();
        used.add(key(s.player.x, s.player.y));
        for (Box b : s.boxes) {
            checkCell(s, b.x, b.y, "thùng");
            if (!used.add(key(b.x, b.y)))
                throw new IllegalArgumentException("Save: hai vật chồng lên nhau tại (" + b.x + "," + b.y + ")");
        }
    }

    private static void checkCell(LevelData s, int x, int y, String what) {
        if (x < 0 || y < 0 || x >= s.width() || y >= s.height())
            throw new IllegalArgumentException("Save: " + what + " ngoài bản đồ tại (" + x + "," + y + ")");
        if (s.grid[y][x] == '#')
            throw new IllegalArgumentException("Save: " + what + " nằm trên tường tại (" + x + "," + y + ")");
    }

    private static long key(int x, int y) { return ((long) x << 32) | (y & 0xFFFFFFFFL); }

    private static int parseInt(String s, int lineIdx) {
        try { return Integer.parseInt(s); }
        catch (NumberFormatException e) { throw bad(lineIdx, "không phải số nguyên: '" + s + "'"); }
    }

    private static int[] parsePair(String s, int lineIdx) {
        String[] p = s.split(",");
        if (p.length != 2) throw bad(lineIdx, "cần dạng x,y nhưng gặp '" + s + "'");
        return new int[]{parseInt(p[0].strip(), lineIdx), parseInt(p[1].strip(), lineIdx)};
    }

    private static IllegalArgumentException bad(int lineIdx, String msg) {
        return new IllegalArgumentException("Save (dòng " + (lineIdx + 1) + "): " + msg);
    }
}
