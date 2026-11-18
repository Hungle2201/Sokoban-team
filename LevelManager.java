import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

/**
 * Quản lý danh sách màn và màn đang chơi.
 * - originals: dữ liệu GỐC của mọi màn, không bao giờ bị sửa.
 * - current:   bản SAO của màn đang chơi, phần logic (A) được phép sửa.
 * Chỉ số màn tính từ 0.
 * LevelManager không biết gì về logic di chuyển hay checkWin,
 * nên không phụ thuộc vòng tròn vào code của A.
 */
public class LevelManager {
    private final List<LevelData> originals;
    private int currentIndex;
    private LevelData current;

    public LevelManager(List<LevelData> levels) {
        if (levels == null || levels.isEmpty()) {
            throw new IllegalArgumentException("Danh sách màn trống");
        }
        this.originals = levels;
        loadLevel(0);
    }

    /** Tiện lợi: đọc thẳng từ file .txt. */
    public static LevelManager fromFile(Path file) throws IOException {
        return new LevelManager(LevelLoader.loadAll(file));
    }

    /** Nạp màn có chỉ số id (từ 0). Bản sao mới được tạo, tiến độ cũ của màn này bị bỏ. */
    public void loadLevel(int id) {
        checkId(id);
        currentIndex = id;
        current = originals.get(id).copy();
    }

    /** Sang màn kế. Trả về false (và giữ nguyên màn hiện tại) nếu đang ở màn cuối. */
    public boolean nextLevel() {
        if (isLastLevel()) return false;
        loadLevel(currentIndex + 1);
        return true;
    }

    /** Chơi lại màn hiện tại từ đầu. */
    public void restartLevel() {
        loadLevel(currentIndex);
    }

    /** Bản sao dữ liệu GỐC của màn id (dùng cho Save/Load để kiểm tra và dựng trạng thái). */
    public LevelData copyOfOriginal(int id) {
        checkId(id);
        return originals.get(id).copy();
    }

    /** Đặt màn id làm màn hiện tại với trạng thái đã cho (dùng khi Load). */
    public void restore(int id, LevelData state) {
        checkId(id);
        currentIndex = id;
        current = state;
    }

    private void checkId(int id) {
        if (id < 0 || id >= originals.size()) {
            throw new IllegalArgumentException(
                    "Màn " + id + " không tồn tại (có " + originals.size() + " màn, chỉ số từ 0)");
        }
    }

    public LevelData getCurrentLevel() { return current; }
    public int getCurrentIndex()       { return currentIndex; }
    public int getLevelCount()         { return originals.size(); }
    public boolean isLastLevel()       { return currentIndex == originals.size() - 1; }
}
