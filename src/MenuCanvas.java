import javax.microedition.lcdui.*;
import javax.microedition.lcdui.game.GameCanvas;

/**
 * MenuCanvas - 2-phase character select.
 *
 * MEMORY FIX (itel 5615 / 1.5MB heap):
 *   Old code loaded ALL 6 sprite sheets in start() at once (~900KB peak).
 *   New code keeps only 2 sheets in memory at a time:
 *     - The sheet under the cursor  (always shown animated)
 *     - The confirmed player sheet  (phase 2 only, shown in bottom bar)
 *   Every time the cursor moves to a new card, the previous sheet is
 *   nulled + gc'd before the new one is loaded (loadSheet / unloadSheet).
 *   Cards without a loaded sheet show a solid colour placeholder — identical
 *   to what SpriteSheet already draws when the file is missing.
 *
 * Peak heap delta from sprites: ~2 x 256x256x2bpp = ~256 KB (was ~900 KB).
 */
public class MenuCanvas extends GameCanvas implements Runnable {

    private final Main midlet;

    private int  playerChar   = 0;
    private int  opponentChar = 1;
    private int  cursorPos    = 0;
    private int  phase        = 1;

    // Only 2 slots ever loaded at once
    // slot 0 = cursor sheet, slot 1 = confirmed player sheet (phase 2 only)
    private SpriteSheet cursorSheet = null;   // sheet for card under cursor
    private int         cursorSheetIdx = -1;  // which char index is in cursorSheet
    private SpriteSheet playerSheet = null;   // sheet for confirmed P1 (phase 2)
    private int         playerSheetIdx = -1;

    private boolean isRunning  = false;
    private int     animFrame  = 0;
    private int     frameDelay = 0;

    private static final int KEY_NUM0 = Canvas.KEY_NUM0;
    private static final int KEY_NUM2 = Canvas.KEY_NUM2;
    private static final int KEY_NUM4 = Canvas.KEY_NUM4;
    private static final int KEY_NUM5 = Canvas.KEY_NUM5;
    private static final int KEY_NUM6 = Canvas.KEY_NUM6;
    private static final int KEY_NUM8 = Canvas.KEY_NUM8;

    private static final String[] NAMES    = {"NARUTO","SASUKE","KAKASHI","SAKURA","GW NARUTO","GW SASUKE"};
    private static final String[] SPECIALS = {"Clone Jutsu","Chidori","Raikiri","Cherry Fist","Rasenshuriken","Susano'o"};
    private static final int[]    ACCENTS  = {0xFF8800, 0xAA00FF, 0x4488FF, 0xFF3399, 0xFFAA00, 0x6600CC};
    private static final boolean[] FLIPF   = {false, true, false, true, false, true};
    private static final String[]  SHEET_PATHS = {
        "/naruto_move.png", "/sasuke_move.png",
        "/kakashi_move.png", "/sakura_move.png",
        "/gw_naruto_move.png", "/gw_sasuke_move.png"
    };

    public MenuCanvas(Main midlet, int lastPlayerChar) {
        super(false);
        this.midlet    = midlet;
        this.playerChar = lastPlayerChar;
        this.cursorPos  = lastPlayerChar;
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

    // ── Lazy sheet loader/unloader ────────────────────────────────────────────

    /**
     * Load a sheet into cursorSheet for the given character index.
     * Unloads the previous cursor sheet first if it was a different char.
     * If the new index matches playerSheet, reuses it (no double allocation).
     */
    private void loadCursorSheet(int idx) {
        if (cursorSheetIdx == idx) return; // already loaded

        // Free the old cursor sheet (unless it is also the player sheet)
        if (cursorSheet != null && cursorSheetIdx != playerSheetIdx) {
            cursorSheet = null;
            System.gc();
        }

        // Reuse player sheet if it's the same character
        if (playerSheet != null && playerSheetIdx == idx) {
            cursorSheet    = playerSheet;
            cursorSheetIdx = idx;
            return;
        }

        cursorSheet    = new SpriteSheet(SHEET_PATHS[idx]);
        cursorSheetIdx = idx;
    }

    /**
     * Load the confirmed player's sheet into playerSheet.
     * Called once when phase 1 is confirmed. The cursor is also on this
     * card at that moment so cursorSheet already has it — we just alias.
     */
    private void loadPlayerSheet(int idx) {
        if (playerSheetIdx == idx) return;

        // If cursor already loaded this char, reuse
        if (cursorSheet != null && cursorSheetIdx == idx) {
            playerSheet    = cursorSheet;
            playerSheetIdx = idx;
            return;
        }

        // Free old player sheet if different from cursor
        if (playerSheet != null && playerSheetIdx != cursorSheetIdx) {
            playerSheet = null;
            System.gc();
        }

        playerSheet    = new SpriteSheet(SHEET_PATHS[idx]);
        playerSheetIdx = idx;
    }

    /** Free everything. */
    private void freeSheets() {
        cursorSheet    = null; cursorSheetIdx = -1;
        playerSheet    = null; playerSheetIdx = -1;
        System.gc();
    }

    /**
     * Get the sheet to draw for card idx.
     * Returns cursorSheet if it matches, playerSheet if it matches, else null.
     * null → drawCharCard shows a colour placeholder (no crash, no OOM).
     */
    private SpriteSheet sheetFor(int idx) {
        if (cursorSheetIdx == idx && cursorSheet != null) return cursorSheet;
        if (playerSheetIdx == idx && playerSheet != null) return playerSheet;
        return null; // all other cards: placeholder
    }

    // ── Lifecycle ─────────────────────────────────────────────────────────────
    public void start() {
        // Load only the cursor card sheet at startup
        loadCursorSheet(cursorPos);
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
        freeSheets();
    }

    // ── Animation loop ────────────────────────────────────────────────────────
    public void run() {
        while (isRunning) {
            frameDelay++;
            if (frameDelay >= 6) {
                animFrame  = (animFrame + 1) % 4;
                frameDelay = 0;
                if (isRunning) render();
            }
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
        g.setColor(0x000000); g.fillRect(0, 0, W, H);

        // Ground strip
        g.setColor(0x221100); g.fillRect(0, H - 18, W, 18);
        g.setColor(0xFF6600); g.drawLine(0, H - 18, W, H - 18);

        // Title
        int titleY = M;
        drawTitle(g, "NARUTO", cx, titleY, Graphics.TOP | Graphics.HCENTER,
                  0xFF6600, 0xFFDD00);
        drawText(g, "FIGHTERS  v2", cx, titleY + 12,
                 Graphics.TOP | Graphics.HCENTER, 0xFFFFFF);
        g.setColor(0xFF6600);
        g.drawLine(M, titleY + 24, W - M, titleY + 24);

        // Phase header
        int phaseY = titleY + 27;
        if (phase == 1) {
            drawText(g, ">> YOUR FIGHTER <<", cx, phaseY,
                     Graphics.TOP | Graphics.HCENTER, 0x00FFAA);
        } else {
            drawText(g, ">> CHOOSE OPPONENT <<", cx, phaseY,
                     Graphics.TOP | Graphics.HCENTER, 0xFF4444);
        }

        // Phase dots
        g.setColor(phase == 1 ? 0x00FFAA : 0x444444);
        g.fillArc(cx - 10, phaseY + 13, 6, 6, 0, 360);
        g.setColor(phase == 2 ? 0xFF4444 : 0x444444);
        g.fillArc(cx + 4,  phaseY + 13, 6, 6, 0, 360);

        // Card grid
        int gridTop = phaseY + 22;
        int gridBot = H - 36;
        int gridH   = gridBot - gridTop;
        int cardW   = (W - M * 3) / 2;
        int cardH   = (gridH - M * 2) / 3;

        int[] cX = { M, M + cardW + M, M, M + cardW + M, M, M + cardW + M };
        int[] cY = { gridTop, gridTop,
                     gridTop + cardH + M, gridTop + cardH + M,
                     gridTop + (cardH + M) * 2, gridTop + (cardH + M) * 2 };

        for (int i = 0; i < 6; i++) {
            boolean isCursor   = (cursorPos == i);
            boolean isPlayer   = (phase == 2 && playerChar == i);
            boolean isOpponent = (phase == 2 && opponentChar == i && i != playerChar);
            drawCharCard(g, cX[i], cY[i], cardW, cardH, i,
                         isCursor, isPlayer, isOpponent);
        }

        // Bottom bar
        int btnY     = gridBot + 2;
        int btnAccent = (phase == 1) ? 0x00FFAA : 0xFF4444;
        String btnLabel = (phase == 1) ? "5=CONFIRM FIGHTER" : "5=FIGHT!";

        if (phase == 2) {
            drawText(g, "P:" + NAMES[playerChar], M, btnY,
                     Graphics.TOP | Graphics.LEFT, 0x00FFAA);
        }
        g.setColor(btnAccent); g.drawRect(M, btnY, W - M * 2, 14);
        drawText(g, btnLabel, cx, btnY + 1,
                 Graphics.TOP | Graphics.HCENTER, btnAccent);
        drawText(g, phase == 1 ? "0=Exit" : "0=Back", cx, H - 10,
                 Graphics.TOP | Graphics.HCENTER, 0x666666);

        flushGraphics();
    }

    private void drawCharCard(Graphics g, int x, int y, int w, int h,
                               int idx, boolean cursor,
                               boolean isPlayer, boolean isOpponent) {
        int accent = ACCENTS[idx];

        // Background
        if (cursor)        g.setColor(0x1A0A00);
        else if (isPlayer) g.setColor(0x001A08);
        else               g.setColor(0x0D0D0D);
        g.fillRect(x, y, w, h);

        // Border
        int borderColor;
        if (isPlayer)    borderColor = 0x00FF88;
        else if (cursor) borderColor = accent;
        else             borderColor = 0x333333;
        g.setColor(borderColor);
        g.drawRect(x, y, w, h);
        if (cursor || isPlayer) g.drawRect(x + 1, y + 1, w - 2, h - 2);

        // Sprite — use sheetFor() which never loads extra sheets
        int sprSize = Math.min(w - 4, ScreenMetrics.SPR_W);
        int sprX    = x + (w - sprSize) / 2;
        int sprY    = y + 2;
        SpriteSheet sh = sheetFor(idx);
        if (sh != null && sh.isLoaded()) {
            sh.drawFrame(g, SpriteSheet.ROW_IDLE, animFrame, sprX, sprY, FLIPF[idx]);
        } else {
            // Colour placeholder — same visual as SpriteSheet.drawPlaceholder
            g.setColor(accent);
            g.fillRect(sprX + 8, sprY + 4, sprSize - 16, sprSize - 8);
            g.setColor(0x000000);
            g.drawRect(sprX + 8, sprY + 4, sprSize - 16, sprSize - 8);
        }

        // Name + special
        int labelY = y + sprSize + 3;
        drawText(g, NAMES[idx], x + w / 2, labelY,
                 Graphics.TOP | Graphics.HCENTER,
                 cursor || isPlayer ? 0xFFFFFF : 0x777777);
        drawText(g, SPECIALS[idx], x + w / 2, labelY + 10,
                 Graphics.TOP | Graphics.HCENTER,
                 cursor ? accent : 0x555555);

        // Status badge
        if (isPlayer) {
            g.setColor(0x00FF88);
            g.fillRect(x + w / 2 - 13, y - 1, 26, 8);
            drawText(g, "P1", x + w / 2, y - 1,
                     Graphics.TOP | Graphics.HCENTER, 0x000000);
        } else if (isOpponent) {
            g.setColor(0xFF4444);
            g.fillRect(x + w / 2 - 13, y - 1, 26, 8);
            drawText(g, "CPU", x + w / 2, y - 1,
                     Graphics.TOP | Graphics.HCENTER, 0x000000);
        } else if (cursor && phase == 1) {
            g.setColor(accent);
            g.fillRect(x + w / 2 - 13, y - 1, 26, 8);
            drawText(g, "YOU", x + w / 2, y - 1,
                     Graphics.TOP | Graphics.HCENTER, 0x000000);
        }

        // Chakra bar
        int mH = 5, mW = w - 8, mX = x + 4, mY = y + h - mH - 2;
        g.setColor(0x001122); g.fillRect(mX, mY, mW, mH);
        if (cursor || isPlayer) {
            g.setColor(accent); g.fillRect(mX, mY, mW / 2, mH);
        }
        g.setColor(cursor ? accent : 0x333333);
        g.drawRect(mX, mY, mW, mH);
    }

    // ── Key input ─────────────────────────────────────────────────────────────
    protected void keyPressed(int keyCode) {
        int action  = getGameAction(keyCode);
        int prevPos = cursorPos;

        if (action == Canvas.LEFT || keyCode == KEY_NUM4) {
            if (cursorPos % 2 == 1) cursorPos--;
        } else if (action == Canvas.RIGHT || keyCode == KEY_NUM6) {
            if (cursorPos % 2 == 0) cursorPos++;
        } else if (action == Canvas.UP || keyCode == KEY_NUM2) {
            if (cursorPos >= 2) cursorPos -= 2; else cursorPos += 4;
        } else if (action == Canvas.DOWN || keyCode == KEY_NUM8) {
            if (cursorPos <= 3) cursorPos += 2; else cursorPos -= 4;

        } else if (action == Canvas.FIRE || keyCode == KEY_NUM5) {
            if (phase == 1) {
                playerChar   = cursorPos;
                // Lock in player sheet before moving cursor to opponent slot
                loadPlayerSheet(playerChar);
                opponentChar = (playerChar + 1) % 6;
                cursorPos    = opponentChar;
                phase        = 2;
                // Load the new cursor position sheet
                loadCursorSheet(cursorPos);
            } else {
                if (cursorPos != playerChar) {
                    opponentChar = cursorPos;
                    stop();
                    midlet.onCharSelected(playerChar);
                    midlet.onFight(opponentChar);
                }
            }
            if (isRunning) render();
            return;

        } else if (keyCode == KEY_NUM0 || keyCode == -11 || keyCode == -6) {
            if (phase == 2) {
                // Going back to phase 1: unload player sheet, reload cursor sheet
                playerSheet    = null;
                playerSheetIdx = -1;
                System.gc();
                phase     = 1;
                cursorPos = playerChar;
                loadCursorSheet(cursorPos);
            } else {
                stop(); midlet.onExit(); return;
            }
            if (isRunning) render();
            return;
        }

        // Cursor moved — load the new card's sheet
        if (cursorPos != prevPos) {
            loadCursorSheet(cursorPos);
        }

        if (isRunning) render();
    }
}
