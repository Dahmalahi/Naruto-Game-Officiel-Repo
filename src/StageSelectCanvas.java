import javax.microedition.lcdui.*;
import javax.microedition.lcdui.game.GameCanvas;

/**
 * StageSelectCanvas — pick one of 3 battle stages.
 * All text uses drawText() shadow helper for crisp rendering on any background.
 */
public class StageSelectCanvas extends GameCanvas implements Runnable {

    private final Main midlet;
    private final int  playerChar;
    private final int  opponentChar;

    private static final String[] BG_PATHS = { "/bg1.png", "/bg2.png", "/bg3.png" };
    private static final String[] BG_NAMES = { "NINJA DISTRICT", "KAMUI DIMENSION", "KONOHA ROOFTOP" };
    private static final String[] BG_SUBS  = { "Sunset Town", "Obito's Realm", "Village Heights" };
    private static final int[]    BG_ACCENT= { 0xFF5500, 0x9933FF, 0x44CC44 };
    private static final int COUNT = 3;

    private int   cursor = 0;
    private Image[] bgs  = new Image[COUNT];

    private boolean isRunning  = false;
    private int     animTick   = 0;
    private int     frameDelay = 0;

    private int slideOffset = 0;
    private int slideTarget = 0;
    private static final int SLIDE_SPEED = 20;

    private static final int KEY_NUM0 = Canvas.KEY_NUM0;
    private static final int KEY_NUM4 = Canvas.KEY_NUM4;
    private static final int KEY_NUM5 = Canvas.KEY_NUM5;
    private static final int KEY_NUM6 = Canvas.KEY_NUM6;

    public StageSelectCanvas(Main midlet, int playerChar, int opponentChar) {
        super(false);
        this.midlet       = midlet;
        this.playerChar   = playerChar;
        this.opponentChar = opponentChar;
        setFullScreenMode(true);
    }

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

    // ── Lifecycle ─────────────────────────────────────────────────────────────
    public void start() {
        for (int i = 0; i < COUNT; i++) {
            try { bgs[i] = Image.createImage(BG_PATHS[i]); }
            catch (Exception e) { bgs[i] = null; }
        }
        isRunning = true;
        new Thread(this).start();
    }

    public void stop() {
        isRunning = false;
        for (int i = 0; i < COUNT; i++) bgs[i] = null;
        try {
            Graphics g = getGraphics();
            g.setColor(0x000000);
            g.fillRect(0, 0, ScreenMetrics.W, ScreenMetrics.H);
            flushGraphics();
        } catch (Exception e) {}
    }

    public void run() {
        while (isRunning) {
            if (slideOffset != slideTarget) {
                int diff = slideTarget - slideOffset;
                int step = diff > 0 ? Math.min(SLIDE_SPEED, diff)
                                    : Math.max(-SLIDE_SPEED, diff);
                slideOffset += step;
            }
            frameDelay++;
            if (frameDelay >= 3) { animTick++; frameDelay = 0; }
            if (isRunning) render();
            try { Thread.sleep(33); } catch (InterruptedException e) { break; }
        }
    }

    // ── Rendering ─────────────────────────────────────────────────────────────
    private void render() {
        if (!isRunning) return;

        final int W  = ScreenMetrics.W;
        final int H  = ScreenMetrics.H;
        final int M  = ScreenMetrics.MARGIN;
        final int cx = W / 2;

        Graphics g = getGraphics();

        // Sky
        g.setColor(0x05001A); g.fillRect(0, 0, W, H);
        g.setColor(0x0A0030); g.fillRect(0, 0, W, H * 2 / 3);

        // Stars
        int[] sx = {10, 30, 55, 85, 110, 145, 175, 205, 225};
        int[] sy = {10, 38,  6, 50,  18,  30,   8,  45,  22};
        for (int i = 0; i < sx.length; i++) {
            boolean bright = ((animTick + i * 3) % 8) < 5;
            g.setColor(bright ? 0xFFFFFF : 0x333355);
            g.fillRect(sx[i], sy[i], 2, 2);
        }

        // ── Header ────────────────────────────────────────────────────────────
        drawTitle(g, "NARUTO", cx, M, Graphics.TOP | Graphics.HCENTER,
                  0xFF6600, 0xFFDD00);
        drawText(g, "FIGHTERS", cx, M + 13,
                 Graphics.TOP | Graphics.HCENTER, 0xFFFFFF);

        g.setColor(0xFF6600);
        g.drawLine(M, M + 26, W - M, M + 26);

        int phaseY = M + 30;
        drawText(g, ">> SELECT STAGE <<", cx, phaseY,
                 Graphics.TOP | Graphics.HCENTER, 0xFFAA00);

        // Stage dots
        for (int i = 0; i < COUNT; i++) {
            g.setColor(i == cursor ? BG_ACCENT[cursor] : 0x333333);
            g.fillArc(cx - (COUNT * 8) / 2 + i * 8, phaseY + 13, 5, 5, 0, 360);
        }

        // ── Card carousel ─────────────────────────────────────────────────────
        int cardAreaTop = phaseY + 22;
        int cardAreaBot = H - 36;
        int cardAreaH   = cardAreaBot - cardAreaTop;

        int bigCardW = W - 36;
        int bigCardH = cardAreaH;
        int bigCardX = (W - bigCardW) / 2;
        int bigCardY = cardAreaTop;

        int smallCardW = 26;
        int smallCardH = cardAreaH - 16;
        int smallCardY = cardAreaTop + 8;

        int prev = (cursor + COUNT - 1) % COUNT;
        int next = (cursor + 1) % COUNT;
        int off  = slideOffset;

        drawCard(g, -smallCardW + off, smallCardY, smallCardW, smallCardH, prev, false);
        drawCard(g, W - smallCardW - 2 + off, smallCardY, smallCardW, smallCardH, next, false);
        drawCard(g, bigCardX + off, bigCardY, bigCardW, bigCardH, cursor, true);

        // ── Stage info bar (over bottom of centre card) ───────────────────────
        int infoY  = bigCardY + bigCardH - 30;
        int accent = BG_ACCENT[cursor];
        g.setColor(0x111111);
        g.fillRect(bigCardX, infoY, bigCardW, 30);

        boolean pulse = (animTick % 8) < 4;
        drawText(g, BG_NAMES[cursor], cx, infoY + 2,
                 Graphics.TOP | Graphics.HCENTER, pulse ? accent : brighten(accent));
        drawText(g, BG_SUBS[cursor], cx, infoY + 14,
                 Graphics.TOP | Graphics.HCENTER, 0x888888);

        // ── Nav arrows ────────────────────────────────────────────────────────
        boolean arrowBlink = (animTick % 6) < 3;
        int arrowColor = arrowBlink ? 0xFFDD00 : 0xAA8800;
        if (cursor > 0)
            drawText(g, "<", bigCardX + 4, bigCardY + bigCardH / 2 - 4,
                     Graphics.TOP | Graphics.LEFT, arrowColor);
        if (cursor < COUNT - 1)
            drawText(g, ">", bigCardX + bigCardW - 4, bigCardY + bigCardH / 2 - 4,
                     Graphics.TOP | Graphics.RIGHT, arrowColor);

        // ── Bottom bar ────────────────────────────────────────────────────────
        int barY = H - 28;
        g.setColor(0x0A0A0A); g.fillRect(0, barY, W, 28);
        g.setColor(0x333333); g.drawLine(0, barY, W, barY);

        g.setColor(accent);
        g.drawRect(M, barY + 3, W - M * 2, 14);
        drawText(g, "5 = FIGHT!", cx, barY + 4,
                 Graphics.TOP | Graphics.HCENTER, accent);
        drawText(g, "4/6:Stage  0:Back", cx, H - 10,
                 Graphics.TOP | Graphics.HCENTER, 0x666666);

        flushGraphics();
    }

    private void drawCard(Graphics g, int x, int y, int w, int h,
                           int idx, boolean isFocused) {
        final int W = ScreenMetrics.W;
        final int H = ScreenMetrics.H;
        int accent = BG_ACCENT[idx];

        g.setClip(Math.max(0, x), Math.max(0, y),
                  Math.min(w, W - Math.max(0, x)),
                  Math.min(h, H - Math.max(0, y)));

        if (bgs[idx] != null) {
            int imgW  = bgs[idx].getWidth();
            int imgH  = bgs[idx].getHeight();
            int drawX = x - (imgW - w) / 2;
            int drawY = y - (imgH - h) / 2;
            if (drawX > x) drawX = x;
            if (drawY > y) drawY = y;
            g.drawImage(bgs[idx], drawX, drawY, Graphics.TOP | Graphics.LEFT);
        } else {
            g.setColor(isFocused ? darken(accent) : 0x1A1A1A);
            g.fillRect(x, y, w, h);
            if (isFocused) {
                drawText(g, "BG " + (idx + 1), x + w / 2, y + h / 2,
                         Graphics.TOP | Graphics.HCENTER, accent);
            }
        }

        g.setClip(0, 0, ScreenMetrics.W, ScreenMetrics.H);

        if (isFocused) {
            boolean glow = (animTick % 6) < 3;
            g.setColor(glow ? accent : darken(accent));
            g.drawRect(x, y, w, h);
            g.drawRect(x + 1, y + 1, w - 2, h - 2);
        } else {
            g.setColor(0x000000); g.fillRect(x, y, w, h);
            g.setColor(0x222222); g.drawRect(x, y, w, h);
        }
    }

    private int darken(int color) {
        int r  = ((color >> 16) & 0xFF) * 6 / 10;
        int gr = ((color >> 8)  & 0xFF) * 6 / 10;
        int b  = ( color        & 0xFF) * 6 / 10;
        return (r << 16) | (gr << 8) | b;
    }

    private int brighten(int color) {
        int r  = Math.min(255, ((color >> 16) & 0xFF) + 60);
        int gr = Math.min(255, ((color >> 8)  & 0xFF) + 60);
        int b  = Math.min(255,  (color        & 0xFF) + 60);
        return (r << 16) | (gr << 8) | b;
    }

    protected void keyPressed(int keyCode) {
        if (!isRunning) return;
        int action = getGameAction(keyCode);
        if (action == Canvas.LEFT || keyCode == KEY_NUM4) {
            if (cursor > 0) {
                cursor--;
                slideOffset = SLIDE_SPEED * 2;
                slideTarget = 0;
            }
            if (isRunning) render();
        } else if (action == Canvas.RIGHT || keyCode == KEY_NUM6) {
            if (cursor < COUNT - 1) {
                cursor++;
                slideOffset = -SLIDE_SPEED * 2;
                slideTarget = 0;
            }
            if (isRunning) render();
        } else if (action == Canvas.FIRE || keyCode == KEY_NUM5) {
            stop();
            midlet.onStageSelected(playerChar, opponentChar, cursor);
        } else if (keyCode == KEY_NUM0 || keyCode == -11 || keyCode == -6) {
            stop();
            midlet.showCharSelect();
        }
    }
}
