// ============================================================
//  ScreenMetrics.java    HACKER RUSHER  (J2ME MIDP 2.0)
//  Centralises every screen-size constant so all classes
//  reference one authoritative source instead of hardcoding.
// ============================================================

/**
 * Singleton helper  call ScreenMetrics.init(w, h) once from
 * HackerCanvas, then use the public statics everywhere.
 */
public final class ScreenMetrics {

    //  Physical screen 
    public static int W  = 240;   // screen width  (set by init)
    public static int H  = 320;   // screen height (set by init)

    //  Safe-zone margins (for HUD / menus) 
    public static int MARGIN = 8;

    //  Tile grid 
    public static final int TILE   = 32;   // map tile size
    public static final int TILE_H = 8;    // platform visual height

    //  Player hitbox (NOT sprite size) 
    public static final int PW  = 32;   // hitbox width
    public static final int PH  = 48;   // hitbox height standing
    public static final int PSH = 24;   // hitbox height sliding

    //  Sprite frame 
    public static final int SPR_W     = 64;   // one frame width
    public static final int SPR_H     = 64;   // one frame height
    public static final int SPR_OFF_X = -16;  // draw offset so sprite centres on hitbox
    public static final int SPR_OFF_Y = -16;

    //  HUD geometry 
    public static int HUD_H = 26;   // top HUD bar height

    //  Game-loop timing 
    public static final int FPS      = 30;
    public static final int FRAME_MS = 1000 / FPS;

    //  Physics 
    public static final int GRAVITY  = 1;
    public static final int JUMP_VEL = -14;
    public static final int RUN_SPD  = 4;
    public static final int MAX_FALL = 14;

    //  Data chips per level 
    public static final int DATA_TOTAL = 5;

    //  Enemy base size (drawn / hitbox) 
    public static final int ENEMY_W = 18;
    public static final int ENEMY_H = 18;

    // prevent instantiation
    private ScreenMetrics() {}

    /**
     * Call once from HackerCanvas constructor:
     *   ScreenMetrics.init(getWidth(), getHeight());
     */
    public static void init(int w, int h) {
        W      = w;
        H      = h;
        MARGIN = Math.max(4, w / 32);
        HUD_H  = (h >= 320) ? 28 : 22;
    }

    //  Convenience centre helpers 
    /** X that horizontally centres a rectangle of the given width. */
    public static int centreX(int width)  { return (W - width) / 2; }

    /** Y that vertically   centres a rectangle of the given height. */
    public static int centreY(int height) { return (H - height) / 2; }
}
