import javax.microedition.lcdui.*;
import java.io.*;

/**
 * SpriteSheet - loads a 256x256 PNG and slices into frames.
 *
 * Layout: 4 columns x 4 rows, each frame = 64x64 px
 *
 * MOVE sheet rows:
 *   row 0: idle   frames 0-3
 *   row 1: run    frames 0-3
 *   row 2: attack frames 0-3
 *   row 3: jump   frames 0-3
 *
 * FIGHT sheet rows:
 *   row 0: attackSp frames 0-3
 *   row 1: special  frames 0-3
 *   row 2: ultra    frames 0-3
 *
 * Uses Graphics.drawRegion() with TRANS_MIRROR for horizontal flip -
 * this is the standard J2ME MIDP-2.0 approach (no Sprite class needed).
 */
public class SpriteSheet {

    // MIDP-2.0 transform constants (from javax.microedition.lcdui.game.Sprite)
    // Defined here so we don't need the Sprite import in non-game files
    private static final int TRANS_NONE   = 0;
    private static final int TRANS_MIRROR = 2;

    public static final int FRAME_W = 64;
    public static final int FRAME_H = 64;
    public static final int COLS    = 4;

    // Move sheet row indices
    public static final int ROW_IDLE   = 0;
    public static final int ROW_RUN    = 1;
    public static final int ROW_ATTACK = 2;
    public static final int ROW_JUMP   = 3;

    // Fight sheet row indices
    public static final int ROW_ATTACK_SP = 0;
    public static final int ROW_SPECIAL   = 1;
    public static final int ROW_ULTRA     = 2;

    private Image sheet;
    private boolean loaded = false;

    public SpriteSheet(String resourcePath) {
        try {
            sheet = Image.createImage(resourcePath);
            loaded = true;
        } catch (IOException e) {
            sheet = null;
        }
    }

    public boolean isLoaded() { return loaded; }

    /**
     * Draw a specific frame from the sheet using drawRegion().
     * drawRegion is MIDP-2.0 standard and supports TRANS_MIRROR natively.
     *
     * @param g      Graphics context
     * @param row    Row index in sprite sheet
     * @param col    Column index (frame number 0-3)
     * @param x      Screen X destination (top-left of destination)
     * @param y      Screen Y destination
     * @param flipH  Mirror horizontally (character facing left)
     */
    public void drawFrame(Graphics g, int row, int col, int x, int y, boolean flipH) {
        if (!loaded || sheet == null) {
            drawPlaceholder(g, x, y);
            return;
        }

        int srcX = col * FRAME_W;
        int srcY = row * FRAME_H;
        int transform = flipH ? TRANS_MIRROR : TRANS_NONE;

        // drawRegion(srcImage, srcX, srcY, srcW, srcH, transform, destX, destY, anchor)
        g.drawRegion(sheet, srcX, srcY, FRAME_W, FRAME_H,
                     transform, x, y, Graphics.TOP | Graphics.LEFT);
    }

    /** Coloured placeholder when sprite PNG not found. */
    private void drawPlaceholder(Graphics g, int x, int y) {
        g.setColor(0xFF6600);
        g.fillRect(x + 8, y + 4, 48, 56);
        g.setColor(0xFFCC00);
        g.fillRect(x + 16, y + 4, 32, 24);
        g.setColor(0x000000);
        g.drawRect(x + 8, y + 4, 48, 56);
    }
}
