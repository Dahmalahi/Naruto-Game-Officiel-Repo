import javax.microedition.lcdui.*;
import javax.microedition.lcdui.game.GameCanvas;

/**
 * SettingsCanvas — in-game settings screen.
 * All text uses drawText() shadow helper for crisp rendering on any background.
 */
public class SettingsCanvas extends GameCanvas {

    private final Main midlet;

    private static final int OPT_MUSIC = 0;
    private static final int OPT_SFX   = 1;
    private static final int OPT_DIFF  = 2;
    private static final int OPT_BACK  = 3;
    private static final int OPT_COUNT = 4;

    private int cursor = 0;

    public static boolean musicOn    = true;
    public static boolean sfxOn      = true;
    public static int     difficulty = 1;

    private static final String[] DIFF_LABELS = {"EASY", "NORMAL", "HARD"};

    private static final int KEY_NUM0 = Canvas.KEY_NUM0;
    private static final int KEY_NUM2 = Canvas.KEY_NUM2;
    private static final int KEY_NUM4 = Canvas.KEY_NUM4;
    private static final int KEY_NUM5 = Canvas.KEY_NUM5;
    private static final int KEY_NUM6 = Canvas.KEY_NUM6;
    private static final int KEY_NUM8 = Canvas.KEY_NUM8;

    public SettingsCanvas(Main midlet) {
        super(false);
        this.midlet = midlet;
        setFullScreenMode(true);
    }

    public void show() { render(); }

    // ── Shadow text helpers ───────────────────────────────────────────────────
    private static void drawText(Graphics g, String s, int x, int y,
                                  int anchor, int color) {
        g.setColor(0x000000);
        g.drawString(s, x + 1, y + 1, anchor);
        g.setColor(color);
        g.drawString(s, x, y, anchor);
    }

    private static void drawTitle(Graphics g, String s, int x, int y,
                                   int anchor, int glowColor, int topColor) {
        g.setColor(glowColor);
        g.drawString(s, x - 1, y,     anchor);
        g.drawString(s, x + 1, y,     anchor);
        g.drawString(s, x,     y - 1, anchor);
        g.drawString(s, x,     y + 1, anchor);
        g.setColor(0x000000);
        g.drawString(s, x + 2, y + 2, anchor);
        g.setColor(topColor);
        g.drawString(s, x, y, anchor);
    }

    // ── Rendering ─────────────────────────────────────────────────────────────
    private void render() {
        final int W  = ScreenMetrics.W;
        final int H  = ScreenMetrics.H;
        final int M  = ScreenMetrics.MARGIN;
        final int cx = W / 2;

        Graphics g = getGraphics();
        g.setColor(0x000000); g.fillRect(0, 0, W, H);

        // Header bar
        g.setColor(0x1A0030); g.fillRect(0, 0, W, 28);
        g.setColor(0xFF6600); g.drawLine(0, 28, W, 28);

        drawTitle(g, "SETTINGS", cx, M, Graphics.TOP | Graphics.HCENTER,
                  0xFF6600, 0xFFDD00);

        // ── Option rows ───────────────────────────────────────────────────────
        int rowY = 40;
        int rowH = 26;

        drawOptionRow(g, M, rowY, W - M * 2, rowH,
                      "MUSIC", musicOn ? "ON" : "OFF",
                      musicOn ? 0x00FF88 : 0xFF3333, cursor == OPT_MUSIC);
        rowY += rowH + M;

        drawOptionRow(g, M, rowY, W - M * 2, rowH,
                      "SOUND FX", sfxOn ? "ON" : "OFF",
                      sfxOn ? 0x00FF88 : 0xFF3333, cursor == OPT_SFX);
        rowY += rowH + M;

        drawOptionRow(g, M, rowY, W - M * 2, rowH,
                      "DIFFICULTY", DIFF_LABELS[difficulty],
                      difficulty == 0 ? 0x00CCFF : difficulty == 1 ? 0xFFDD00 : 0xFF3333,
                      cursor == OPT_DIFF);
        rowY += rowH + M * 3;

        // BACK button
        boolean selBack = (cursor == OPT_BACK);
        g.setColor(selBack ? 0xFF6600 : 0x222222);
        g.fillRect(M * 4, rowY, W - M * 8, rowH - 4);
        g.setColor(selBack ? 0xFFDD00 : 0x444444);
        g.drawRect(M * 4, rowY, W - M * 8, rowH - 4);
        drawText(g, "BACK", cx, rowY + 4,
                 Graphics.TOP | Graphics.HCENTER,
                 selBack ? 0xFFFFFF : 0x888888);

        // Hint bar
        g.setColor(0x333333); g.drawLine(M, H - 22, W - M, H - 22);
        drawText(g, "2/8:Move  4/6:Change  5:OK", cx, H - 20,
                 Graphics.TOP | Graphics.HCENTER, 0x777777);

        flushGraphics();
    }

    private void drawOptionRow(Graphics g, int x, int y, int w, int h,
                                String label, String value, int valColor,
                                boolean sel) {
        int cx = x + w / 2;
        int M  = ScreenMetrics.MARGIN;

        g.setColor(sel ? 0x1A0800 : 0x111111); g.fillRect(x, y, w, h);
        g.setColor(sel ? 0xFF6600 : 0x333333); g.drawRect(x, y, w, h);

        drawText(g, label, x + M, y + 6,
                 Graphics.TOP | Graphics.LEFT,
                 sel ? 0xFFFFFF : 0x888888);
        drawText(g, value, x + w - M, y + 6,
                 Graphics.TOP | Graphics.RIGHT, valColor);

        if (sel) {
            drawText(g, "<", x + w / 2 - 20, y + 6,
                     Graphics.TOP | Graphics.LEFT, 0xFF8800);
            drawText(g, ">", x + w / 2 + 12, y + 6,
                     Graphics.TOP | Graphics.LEFT, 0xFF8800);
        }
    }

    // ── Key input ─────────────────────────────────────────────────────────────
    protected void keyPressed(int keyCode) {
        int action = getGameAction(keyCode);
        if (action == Canvas.UP || keyCode == KEY_NUM2) {
            if (cursor > 0) cursor--;
            render();
        } else if (action == Canvas.DOWN || keyCode == KEY_NUM8) {
            if (cursor < OPT_COUNT - 1) cursor++;
            render();
        } else if (action == Canvas.LEFT || keyCode == KEY_NUM4) {
            changeValue(-1);
        } else if (action == Canvas.RIGHT || keyCode == KEY_NUM6) {
            changeValue(+1);
        } else if (action == Canvas.FIRE || keyCode == KEY_NUM5) {
            if (cursor == OPT_BACK) goBack(); else changeValue(+1);
        } else if (keyCode == KEY_NUM0 || keyCode == -11 || keyCode == -6) {
            goBack();
        }
    }

    private void changeValue(int dir) {
        switch (cursor) {
            case OPT_MUSIC:
                musicOn = !musicOn;
                if (musicOn) MusicPlayer.resume(); else MusicPlayer.pause();
                break;
            case OPT_SFX:
                sfxOn = !sfxOn;
                SFXPlayer.setEnabled(sfxOn);
                break;
            case OPT_DIFF:
                difficulty = (difficulty + dir + 3) % 3;
                break;
        }
        render();
    }

    private void goBack() { midlet.showTitle(); }
}
