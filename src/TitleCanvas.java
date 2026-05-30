import javax.microedition.lcdui.*;
import javax.microedition.lcdui.game.GameCanvas;

/**
 * TitleCanvas — main menu.
 * All text uses drawText() shadow helper for crisp rendering on any background.
 */
public class TitleCanvas extends GameCanvas implements Runnable {

    private final Main midlet;

    private static final String[] ITEMS = { "PLAY", "SETTINGS", "ABOUT" };
    private int cursor = 0;

    private boolean isRunning  = false;
    private int     animTick   = 0;
    private int     frameDelay = 0;

    private static final int KEY_NUM0 = Canvas.KEY_NUM0;
    private static final int KEY_NUM2 = Canvas.KEY_NUM2;
    private static final int KEY_NUM5 = Canvas.KEY_NUM5;
    private static final int KEY_NUM8 = Canvas.KEY_NUM8;

    public TitleCanvas(Main midlet) {
        super(false);
        this.midlet = midlet;
        setFullScreenMode(true);
    }

    public void start() {
        isRunning = true;
        new Thread(this).start();
    }

    public void stop() {
        isRunning = false;
        try {
            Graphics g = getGraphics();
            g.setColor(0x000000);
            g.fillRect(0, 0, ScreenMetrics.W, ScreenMetrics.H);
            flushGraphics();
        } catch (Exception e) {}
    }

    public void run() {
        while (isRunning) {
            frameDelay++;
            if (frameDelay >= 4) {
                animTick++;
                frameDelay = 0;
                if (isRunning) render();
            }
            try { Thread.sleep(33); } catch (InterruptedException e) { break; }
        }
    }

    // ── Shadow text helper ────────────────────────────────────────────────────
    /**
     * Draw string with a 1-px black drop-shadow, then the main colour on top.
     * Makes text readable on any background — dark or bright.
     */
    private static void drawText(Graphics g, String s, int x, int y,
                                  int anchor, int color) {
        g.setColor(0x000000);
        g.drawString(s, x + 1, y + 1, anchor);
        g.setColor(color);
        g.drawString(s, x, y, anchor);
    }

    /** Convenience: draw with glow (extra offset in a darker shade) for big titles. */
    private static void drawTitle(Graphics g, String s, int x, int y,
                                   int anchor, int glowColor, int topColor) {
        // Glow ring
        g.setColor(glowColor);
        g.drawString(s, x - 1, y,     anchor);
        g.drawString(s, x + 1, y,     anchor);
        g.drawString(s, x,     y - 1, anchor);
        g.drawString(s, x,     y + 1, anchor);
        // Shadow
        g.setColor(0x000000);
        g.drawString(s, x + 2, y + 2, anchor);
        // Top colour
        g.setColor(topColor);
        g.drawString(s, x, y, anchor);
    }

    private void render() {
        if (!isRunning) return;

        final int W  = ScreenMetrics.W;
        final int H  = ScreenMetrics.H;
        final int cx = W / 2;

        Graphics g = getGraphics();

        // ── Background ────────────────────────────────────────────────────────
        g.setColor(0x05001A); g.fillRect(0, 0, W, H);
        g.setColor(0x0A0030); g.fillRect(0, 0, W, H * 3 / 5);
        g.setColor(0x150900); g.fillRect(0, H * 3 / 4, W, H / 4);
        g.setColor(0xFF5500); g.drawLine(0, H * 3 / 4, W, H * 3 / 4);
        g.setColor(0x883300); g.drawLine(0, H * 3 / 4 + 1, W, H * 3 / 4 + 1);

        // Moon
        g.setColor(0xFFEE88); g.fillArc(W - 52, 6, 44, 44, 0, 360);
        g.setColor(0xDDCC66); g.fillArc(W - 42, 14, 12, 12, 0, 360);

        // Stars
        int[] sx = {10, 28, 55, 80, 105, 130, 155, 185, 210, 38, 170};
        int[] sy = {12, 38,  7, 52,  18,  32,   9,  45,  22,  68,  60};
        for (int i = 0; i < sx.length; i++) {
            boolean bright = ((animTick + i * 3) % 8) < 5;
            g.setColor(bright ? 0xFFFFFF : 0x444466);
            g.fillRect(sx[i], sy[i], 2, 2);
        }

        // ── Title ─────────────────────────────────────────────────────────────
        int titleY = 18;
        boolean pulse = (animTick % 12) < 6;

        drawTitle(g, "NARUTO", cx, titleY, Graphics.TOP | Graphics.HCENTER,
                  pulse ? 0xFF7700 : 0xFF5500,
                  pulse ? 0xFFEE00 : 0xFFCC00);

        drawText(g, "FIGHTERS", cx, titleY + 14, Graphics.TOP | Graphics.HCENTER, 0xFFFFFF);

        // Accent line under title
        g.setColor(0xFF5500); g.fillRect(cx - 50, titleY + 28, 100, 2);
        g.setColor(0xFFCC00); g.fillRect(cx - 20, titleY + 28, 40, 2);

        // ── Menu buttons ──────────────────────────────────────────────────────
        int btnMargin = 10;
        int btnX      = btnMargin;
        int btnW      = W - btnMargin * 2;
        int btnH      = 34;
        int btnGap    = 8;
        int menuTop   = titleY + 40;

        for (int i = 0; i < ITEMS.length; i++) {
            int btnY = menuTop + i * (btnH + btnGap);
            boolean sel = (cursor == i);

            if (sel) {
                boolean glow = ((animTick % 6) < 3);

                g.setColor(glow ? 0xFF8800 : 0xFF5500);
                g.fillRect(btnX - 1, btnY - 1, btnW + 2, btnH + 2);
                g.setColor(glow ? 0xDD4400 : 0xBB3300);
                g.fillRect(btnX, btnY, btnW, btnH);
                g.setColor(0xFF9944);
                g.drawLine(btnX + 1, btnY + 1, btnX + btnW - 2, btnY + 1);

                // Animated side markers
                int markerX = (animTick % 6) < 3 ? btnX + 4 : btnX + 6;
                g.setColor(0xFFEE00);
                g.drawLine(markerX,     btnY + btnH/2,     markerX + 5, btnY + btnH/2 - 4);
                g.drawLine(markerX,     btnY + btnH/2,     markerX + 5, btnY + btnH/2 + 4);
                g.drawLine(markerX,     btnY + btnH/2 - 4, markerX,     btnY + btnH/2 + 4);
                int rx = btnX + btnW - 1 - markerX + btnX;
                g.drawLine(rx,     btnY + btnH/2,     rx - 5, btnY + btnH/2 - 4);
                g.drawLine(rx,     btnY + btnH/2,     rx - 5, btnY + btnH/2 + 4);
                g.drawLine(rx,     btnY + btnH/2 - 4, rx,     btnY + btnH/2 + 4);

                drawText(g, ITEMS[i], cx, btnY + 10,
                         Graphics.TOP | Graphics.HCENTER, 0xFFFFFF);
            } else {
                g.setColor(0x111111); g.fillRect(btnX, btnY, btnW, btnH);
                g.setColor(0x333333); g.drawRect(btnX, btnY, btnW, btnH);
                drawText(g, ITEMS[i], cx, btnY + 10,
                         Graphics.TOP | Graphics.HCENTER, 0x888888);
            }
        }

        // ── Bottom hint bar ───────────────────────────────────────────────────
        int barY = H - 22;
        g.setColor(0x0A0A0A); g.fillRect(0, barY, W, 22);
        g.setColor(0x2A2A2A); g.drawLine(0, barY, W, barY);
        drawText(g, "2/8:Move  5:Select  0:Exit", cx, barY + 3,
                 Graphics.TOP | Graphics.HCENTER, 0x777777);
        drawText(g, "v2.0  NARUTO FIGHTERS", cx, barY + 13,
                 Graphics.TOP | Graphics.HCENTER, 0x444444);

        flushGraphics();
    }

    protected void keyPressed(int keyCode) {
        if (!isRunning) return;
        int action = getGameAction(keyCode);
        if (action == Canvas.UP || keyCode == KEY_NUM2) {
            if (cursor > 0) { cursor--; if (isRunning) render(); }
        } else if (action == Canvas.DOWN || keyCode == KEY_NUM8) {
            if (cursor < ITEMS.length - 1) { cursor++; if (isRunning) render(); }
        } else if (action == Canvas.FIRE || keyCode == KEY_NUM5) {
            stop();
            switch (cursor) {
                case 0: midlet.showCharSelect(); break;
                case 1: midlet.showSettings();   break;
                case 2: midlet.showAbout();       break;
            }
        } else if (keyCode == KEY_NUM0 || keyCode == -11 || keyCode == -6) {
            stop(); midlet.onExit();
        }
    }
}
