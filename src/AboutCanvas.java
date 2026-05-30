import javax.microedition.lcdui.*;
import javax.microedition.lcdui.game.GameCanvas;

/**
 * AboutCanvas — credits and developer info.
 * All text uses drawText() shadow helper for crisp rendering on any background.
 */
public class AboutCanvas extends GameCanvas {

    private final Main midlet;
    private int scrollY = 0;

    private static final int KEY_NUM0 = Canvas.KEY_NUM0;
    private static final int KEY_NUM2 = Canvas.KEY_NUM2;
    private static final int KEY_NUM5 = Canvas.KEY_NUM5;
    private static final int KEY_NUM8 = Canvas.KEY_NUM8;

    private static final int T_HEAD = 0;
    private static final int T_ROW  = 1;
    private static final int T_TEXT = 2;
    private static final int T_DIV  = 3;

    private static final Object[][] LINES = {
        { new Integer(T_HEAD), "NARUTO FIGHTERS",         "" },
        { new Integer(T_DIV),  "",                        "" },
        { new Integer(T_ROW),  "Version",                 "2.0" },
        { new Integer(T_ROW),  "Platform",                "J2ME MIDP 2.0" },
        { new Integer(T_ROW),  "Screen",                  "240 x 320" },
        { new Integer(T_DIV),  "",                        "" },
        { new Integer(T_HEAD), "DEVELOPER",               "" },
        { new Integer(T_DIV),  "",                        "" },
        { new Integer(T_ROW),  "Vendor",                  "DASH ANIMATION V2" },
        { new Integer(T_ROW),  "Author",                  "Dahmalahi" },
        { new Integer(T_DIV),  "",                        "" },
        { new Integer(T_HEAD), "LINKS",                   "" },
        { new Integer(T_DIV),  "",                        "" },
        { new Integer(T_TEXT), "YouTube:",                "" },
        { new Integer(T_TEXT), "youtube.com/",            "" },
        { new Integer(T_TEXT), "@dash______animationv2",  "" },
        { new Integer(T_DIV),  "",                        "" },
        { new Integer(T_TEXT), "GitHub:",                 "" },
        { new Integer(T_TEXT), "github.com/",             "" },
        { new Integer(T_TEXT), "Dahmalahi",               "" },
        { new Integer(T_DIV),  "",                        "" },
        { new Integer(T_HEAD), "CHARACTERS and new char ",              "" },
        { new Integer(T_DIV),  "",                        "" },
        { new Integer(T_ROW),  "Naruto Uzumaki",          "HP 200" },
        { new Integer(T_ROW),  "Sasuke Uchiha",           "HP 200" },
        { new Integer(T_ROW),  "Kakashi Hatake",          "HP 220" },
        { new Integer(T_ROW),  "Sakura Haruno",           "HP 180" },
        { new Integer(T_DIV),  "",                        "" },
        { new Integer(T_HEAD), "ENGINE",                  "" },
        { new Integer(T_DIV),  "",                        "" },
        { new Integer(T_TEXT), "Custom J2ME engine",      "" },
        { new Integer(T_TEXT), "Sprite: 256x256 px",      "" },
        { new Integer(T_TEXT), "64x64 per frame",         "" },
        { new Integer(T_TEXT), "4 cols x 4 rows",         "" },
        { new Integer(T_DIV),  "",                        "" },
        { new Integer(T_TEXT), "(C) 2026 DASH ANIMATION V2", "" },
    };

    private static final int LINE_H     = 14;
    private static final int SCROLL_STEP = LINE_H * 2;

    public AboutCanvas(Main midlet) {
        super(false);
        this.midlet = midlet;
        setFullScreenMode(true);
    }

    public void show() { scrollY = 0; render(); }

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
        final int W       = ScreenMetrics.W;
        final int H       = ScreenMetrics.H;
        final int M       = ScreenMetrics.MARGIN;
        final int cx      = W / 2;
        final int contentH = H - 46;

        Graphics g = getGraphics();
        g.setColor(0x000000); g.fillRect(0, 0, W, H);

        // Header bar
        g.setColor(0x0A001A); g.fillRect(0, 0, W, 28);
        g.setColor(0xFF6600); g.drawLine(0, 28, W, 28);
        drawTitle(g, "ABOUT", cx, M, Graphics.TOP | Graphics.HCENTER,
                  0xFF6600, 0xFFDD00);

        // ── Scrollable content ────────────────────────────────────────────────
        g.setClip(0, 29, W, contentH);
        int y = 32 - scrollY;

        for (int i = 0; i < LINES.length; i++) {
            int    type = ((Integer) LINES[i][0]).intValue();
            String col1 = (String) LINES[i][1];
            String col2 = (String) LINES[i][2];

            if (y + LINE_H >= 29 && y < 29 + contentH) {
                switch (type) {
                    case T_HEAD:
                        drawTitle(g, col1, cx, y, Graphics.TOP | Graphics.HCENTER,
                                  0xCC5500, 0xFF8800);
                        break;
                    case T_ROW:
                        drawText(g, col1, M, y,
                                 Graphics.TOP | Graphics.LEFT, 0x888888);
                        drawText(g, col2, W - M, y,
                                 Graphics.TOP | Graphics.RIGHT, 0xFFFFFF);
                        break;
                    case T_TEXT:
                        drawText(g, col1, cx, y,
                                 Graphics.TOP | Graphics.HCENTER, 0x44DDFF);
                        break;
                    case T_DIV:
                        g.setColor(0x333333);
                        g.drawLine(M * 2, y + LINE_H / 2,
                                   W - M * 2, y + LINE_H / 2);
                        break;
                }
            }
            y += LINE_H;
        }

        int totalH = LINES.length * LINE_H;
        g.setClip(0, 0, W, H);

        // Scroll bar
        if (totalH > contentH) {
            int trackH = contentH - 4;
            int thumbH = Math.max(8, trackH * contentH / totalH);
            int thumbY = 30 + (trackH - thumbH) * scrollY /
                         Math.max(1, totalH - contentH);
            g.setColor(0x333333); g.fillRect(W - 4, 30, 4, trackH);
            g.setColor(0xFF6600); g.fillRect(W - 4, thumbY, 4, thumbH);
        }

        // Footer
        g.setColor(0x111111); g.fillRect(0, H - 18, W, 18);
        g.setColor(0x444444); g.drawLine(0, H - 18, W, H - 18);
        drawText(g, "2/8:Scroll  5/0:Back", cx, H - 14,
                 Graphics.TOP | Graphics.HCENTER, 0x777777);

        flushGraphics();
    }

    // ── Key input ─────────────────────────────────────────────────────────────
    protected void keyPressed(int keyCode) {
        int action    = getGameAction(keyCode);
        int totalH    = LINES.length * LINE_H;
        int contentH  = ScreenMetrics.H - 46;
        int maxScroll = Math.max(0, totalH - contentH);

        if (action == Canvas.UP || keyCode == KEY_NUM2) {
            scrollY -= SCROLL_STEP;
            if (scrollY < 0) scrollY = 0;
            render();
        } else if (action == Canvas.DOWN || keyCode == KEY_NUM8) {
            scrollY += SCROLL_STEP;
            if (scrollY > maxScroll) scrollY = maxScroll;
            render();
        } else if (action == Canvas.FIRE || keyCode == KEY_NUM5
                || keyCode == KEY_NUM0 || keyCode == -11 || keyCode == -6) {
            midlet.showTitle();
        }
    }
}
