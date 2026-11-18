import java.nio.file.Files;
import java.nio.file.Path;

public class SaveManagerTest {
    static void check(boolean ok, String what) { System.out.println((ok ? "OK   " : "FAIL ") + what); }

    static void expectFail(Path f, LevelManager m, String what) throws Exception {
        int idx = m.getCurrentIndex();
        try { SaveManager.load(f, m); check(false, what + " (phải ném lỗi)"); }
        catch (IllegalArgumentException e) {
            check(m.getCurrentIndex() == idx, what + " -> " + e.getMessage());
        }
    }

    public static void main(String[] args) throws Exception {
        Path levels = Path.of("test_levels.txt");
        Path save = Path.of("test_save.txt");
        Files.writeString(levels, "#######\n#@ $ .#\n#######\n; A\n\n#####\n#@$.#\n#####\n; B\n");
        LevelManager m = LevelManager.fromFile(levels);

        // Chơi giả lập: đi 1 bước đẩy thùng sang phải
        LevelData cur = m.getCurrentLevel();
        cur.player.x = 2; cur.boxes.get(0).x = 4;
        SaveManager.save(save, m, 1);
        System.out.println(Files.readString(save));

        m.nextLevel();                       // làm trạng thái khác đi
        int moves = SaveManager.load(save, m);
        check(moves == 1, "load trả về số bước đúng");
        check(m.getCurrentIndex() == 0, "load về đúng màn");
        check(m.getCurrentLevel().player.x == 2, "khôi phục vị trí Player");
        check(m.getCurrentLevel().boxes.get(0).x == 4, "khôi phục vị trí Box");

        m.restartLevel();
        check(m.getCurrentLevel().player.x == 1 && m.getCurrentLevel().boxes.get(0).x == 3,
                "restart sau load vẫn về vị trí gốc");

        // File hỏng: manager không được thay đổi
        String ok = Files.readString(save);
        Files.writeString(save, ok.replace("player=2,1", "player=0,0")); expectFail(save, m, "người chơi trên tường");
        Files.writeString(save, ok.replace("player=2,1", "player=9,9")); expectFail(save, m, "người chơi ngoài bản đồ");
        Files.writeString(save, ok.replace("box=4,1", "box=2,1"));       expectFail(save, m, "thùng đè người chơi");
        Files.writeString(save, ok.replace("level=0", "level=7"));        expectFail(save, m, "màn không tồn tại");
        Files.writeString(save, ok + "box=3,1\n");                         expectFail(save, m, "sai số thùng");
        Files.writeString(save, ok.replace("moves=1", "moves=abc"));      expectFail(save, m, "moves không phải số");
        Files.writeString(save, ok.replace("levelName=A", "levelName=Z")); expectFail(save, m, "tên màn không khớp");
        Files.writeString(save, "rac roi\n");                              expectFail(save, m, "dòng không có '='");

        Files.delete(levels); Files.delete(save);
    }
}
