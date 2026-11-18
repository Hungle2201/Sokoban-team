import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class LevelLoader {

    /** Đọc toàn bộ file .txt, trả về danh sách các màn. */
    public static List<LevelData> loadAll(Path file) throws IOException {
        // readAllLines tự xử lý \n và \r\n (CRLF). Tuyệt đối không trim() từng dòng.
        List<String> lines = new ArrayList<>(Files.readAllLines(file, StandardCharsets.UTF_8));
        if (!lines.isEmpty() && lines.get(0).startsWith("\uFEFF")) {   // bỏ BOM nếu có
            lines.set(0, lines.get(0).substring(1));
        }

        List<LevelData> levels = new ArrayList<>();
        List<String> block = new ArrayList<>();
        String name = null;
        int blockFirstLine = 0;

        // i == lines.size() là "dòng trống giả" để chốt màn cuối dù file không kết thúc bằng dòng trống
        for (int i = 0; i <= lines.size(); i++) {
            String line = (i < lines.size()) ? lines.get(i) : "";

            if (line.isEmpty()) {                       // dòng trống = hết một màn
                if (!block.isEmpty()) {
                    levels.add(parse(block, name, blockFirstLine));
                    block.clear();
                    name = null;
                }
            } else if (line.charAt(0) == ';') {         // dòng tên màn
                name = line.substring(1).strip();       // an toàn cả khi dòng chỉ có ";"
            } else {                                    // dòng bản đồ
                if (block.isEmpty()) blockFirstLine = i + 1;   // số dòng (tính từ 1) để báo lỗi
                block.add(line);
            }
        }
        if (levels.isEmpty()) {
            throw new IllegalArgumentException("File không chứa màn chơi nào: " + file);
        }
        return levels;
    }

    /** Biến một khối dòng thành LevelData. firstLine là số dòng của hàng đầu tiên trong file. */
    static LevelData parse(List<String> rows, String name, int firstLine) {
        LevelData lv = new LevelData();
        lv.name = (name == null || name.isEmpty()) ? "Level (dòng " + firstLine + ")" : name;

        // Tính chiều rộng lớn nhất rồi tạo mảng một lần, mặc định là sàn ' '
        int width = 0;
        for (String r : rows) width = Math.max(width, r.length());
        lv.grid = new char[rows.size()][width];
        for (char[] row : lv.grid) java.util.Arrays.fill(row, ' ');

        for (int y = 0; y < rows.size(); y++) {
            String row = rows.get(y);
            for (int x = 0; x < row.length(); x++) {
                char ch = row.charAt(x);
                switch (ch) {
                    case '#': lv.grid[y][x] = '#'; break;
                    case ' ': break;
                    case '@': setPlayer(lv, x, y, firstLine); break;
                    case '$': lv.boxes.add(new Box(x, y)); break;
                    case '.': lv.goals.add(new int[]{x, y}); break;
                    case '+': setPlayer(lv, x, y, firstLine);
                              lv.goals.add(new int[]{x, y}); break;
                    case '*': lv.boxes.add(new Box(x, y));
                              lv.goals.add(new int[]{x, y}); break;
                    default:
                        throw error(lv, firstLine + y, x + 1,
                                "ký tự không hợp lệ '" + ch + "'");
                }
            }
        }
        validate(lv, firstLine);
        return lv;
    }

    private static void setPlayer(LevelData lv, int x, int y, int firstLine) {
        if (lv.player != null) {
            throw error(lv, firstLine + y, x + 1, "có hơn 1 người chơi");
        }
        lv.player = new Player(x, y);
    }

    private static void validate(LevelData lv, int firstLine) {
        if (lv.player == null)
            throw new IllegalArgumentException("Màn \"" + lv.name + "\" (từ dòng " + firstLine + "): không có người chơi");
        if (lv.boxes.isEmpty())
            throw new IllegalArgumentException("Màn \"" + lv.name + "\" (từ dòng " + firstLine + "): không có thùng nào");
        if (lv.boxes.size() != lv.goals.size())
            throw new IllegalArgumentException("Màn \"" + lv.name + "\" (từ dòng " + firstLine + "): "
                    + lv.boxes.size() + " thùng nhưng " + lv.goals.size() + " ô đích");
    }

    private static IllegalArgumentException error(LevelData lv, int line, int col, String msg) {
        return new IllegalArgumentException(
                "Màn \"" + lv.name + "\" (dòng " + line + ", cột " + col + "): " + msg);
    }
}
