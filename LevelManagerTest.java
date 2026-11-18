import java.nio.file.Path;

public class LevelManagerTest {
    static void check(boolean ok, String what) {
        System.out.println((ok ? "OK   " : "FAIL ") + what);
    }
    public static void main(String[] args) throws Exception {
        String path = args.length > 0 ? args[0] : "rsc/Levels.txt";
        LevelManager m = LevelManager.fromFile(Path.of(path));

        check(m.getCurrentIndex() == 0, "bắt đầu ở màn 0");
        check(!m.isLastLevel() || m.getLevelCount() == 1, "màn 0 chưa phải màn cuối (nếu có >1 màn)");

        // Sửa bản đang chơi, rồi restart: phải trở về vị trí gốc
        LevelData cur = m.getCurrentLevel();
        int x0 = cur.player.x;
        cur.player.x += 1;
        cur.boxes.remove(0);
        m.restartLevel();
        check(m.getCurrentLevel().player.x == x0, "restart khôi phục vị trí Player");
        check(m.getCurrentLevel().boxes.size() == m.getCurrentLevel().goals.size(), "restart khôi phục Box");

        // Duyệt hết màn
        int steps = 0;
        while (m.nextLevel()) steps++;
        check(steps == m.getLevelCount() - 1, "nextLevel đi hết " + m.getLevelCount() + " màn");
        check(m.isLastLevel(), "đang ở màn cuối");
        check(!m.nextLevel(), "nextLevel ở màn cuối trả về false");

        try { m.loadLevel(9999); check(false, "loadLevel sai id phải ném lỗi"); }
        catch (IllegalArgumentException e) { check(true, "loadLevel sai id ném lỗi: " + e.getMessage()); }
    }
}
