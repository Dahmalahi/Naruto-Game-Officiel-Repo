import javax.microedition.lcdui.*;

/**
 * EffectManager v2.1 — visual effects (screen flash REMOVED for low-end safety).
 *
 * Manages:
 *  - Hit sparks  (particle burst at impact point)
 *  - Screen shake (camera offset jitter on heavy hits)
 *  - Damage / move-name pop-up text
 *
 * All state is integer-only — no floats, safe for CLDC 1.0.
 * Screen flash was removed: looping fillRect across all rows caused
 * frame-drops and potential out-of-memory on low-end MIDP handsets.
 */
public class EffectManager {

    // ── Spark particles ───────────────────────────────────────────────────────
    private static final int MAX_SPARKS = 12;

    private int[] spx    = new int[MAX_SPARKS]; // world X (×4 fixed-point)
    private int[] spy    = new int[MAX_SPARKS]; // world Y (×4 fixed-point)
    private int[] spvx   = new int[MAX_SPARKS]; // vel X (×4)
    private int[] spvy   = new int[MAX_SPARKS]; // vel Y (×4)
    private int[] spLife = new int[MAX_SPARKS]; // remaining ticks
    private int[] spColor= new int[MAX_SPARKS]; // 0xRRGGBB
    private int   sparkCount = 0;

    // ── Screen shake ──────────────────────────────────────────────────────────
    private int shakeTimer    = 0;
    private int shakeStrength = 0;
    private int shakeOffX     = 0;
    private int shakeOffY     = 0;

    // ── Hit-text pop ──────────────────────────────────────────────────────────
    private static final int MAX_POPS = 4;
    private String[] popText  = new String[MAX_POPS];
    private int[]    popX     = new int[MAX_POPS];
    private int[]    popY     = new int[MAX_POPS];
    private int[]    popLife  = new int[MAX_POPS];
    private int[]    popColor = new int[MAX_POPS];
    private int      popCount = 0;

    // ── LCG pseudo-random (CLDC-safe, no java.util.Random) ───────────────────
    private int seed = 0x1337BEEF;
    private int nextInt(int n) {
        seed = seed * 1664525 + 1013904223;
        int r = (seed >>> 1) % n;
        return r < 0 ? -r : r;
    }

    // ── Spawn helpers ─────────────────────────────────────────────────────────

    /** Burst hit sparks at world position (wx, wy). */
    public void spawnHitSparks(int wx, int wy, int count, int color) {
        for (int i = 0; i < count && sparkCount < MAX_SPARKS; i++) {
            int s = sparkCount++;
            spx[s]    = wx * 4;
            spy[s]    = wy * 4;
            spvx[s]   = nextInt(28) - 14;
            spvy[s]   = nextInt(20) - 16;  // bias upward
            spLife[s] = 8 + nextInt(6);
            spColor[s]= color;
        }
    }

    /** Trigger screen shake. strength = max pixel offset, duration = ticks. */
    public void triggerShake(int strength, int duration) {
        shakeStrength = strength;
        shakeTimer    = duration;
    }

    /** Floating damage label at world position. */
    public void spawnPop(String text, int wx, int wy, int color) {
        if (popCount >= MAX_POPS) popCount = 0;
        int i = popCount++;
        popText[i]  = text;
        popX[i]     = wx;
        popY[i]     = wy - 20;
        popLife[i]  = 20;
        popColor[i] = color;
    }

    // ── Update ────────────────────────────────────────────────────────────────
    public void update() {
        // Compact-array spark update
        int alive = 0;
        for (int i = 0; i < sparkCount; i++) {
            spLife[i]--;
            if (spLife[i] <= 0) continue;
            spx[i] += spvx[i];
            spy[i] += spvy[i];
            spvy[i] += 3; // gravity in ×4 space
            if (alive != i) {
                spx[alive]=spx[i]; spy[alive]=spy[i];
                spvx[alive]=spvx[i]; spvy[alive]=spvy[i];
                spLife[alive]=spLife[i]; spColor[alive]=spColor[i];
            }
            alive++;
        }
        sparkCount = alive;

        // Shake
        if (shakeTimer > 0) {
            shakeTimer--;
            int half = shakeStrength / 2;
            shakeOffX = nextInt(shakeStrength) - half;
            shakeOffY = nextInt(shakeStrength) - half;
        } else {
            shakeOffX = 0;
            shakeOffY = 0;
        }

        // Pop float
        for (int i = 0; i < popCount; i++) {
            if (popLife[i] > 0) { popLife[i]--; popY[i]--; }
        }
    }

    // ── Draw ──────────────────────────────────────────────────────────────────

    /** Draw sparks and pop labels. Call after characters, before HUD. */
    public void draw(Graphics g, int camX, int camY) {
        for (int i = 0; i < sparkCount; i++) {
            int sx = (spx[i] / 4) - camX;
            int sy = (spy[i] / 4) - camY;
            int sz = (spLife[i] > 4) ? 4 : 2;
            g.setColor(spColor[i]);
            g.fillRect(sx, sy, sz, sz);
            g.setColor(0xFFFFFF);
            g.fillRect(sx + 1, sy + 1, 1, 1);
        }
        for (int i = 0; i < popCount; i++) {
            if (popLife[i] > 0) {
                int px = popX[i] - camX;
                int py = popY[i] - camY;
                g.setColor(0x000000);
                g.drawString(popText[i], px+1, py+1, Graphics.TOP|Graphics.HCENTER);
                g.setColor(popColor[i]);
                g.drawString(popText[i], px,   py,   Graphics.TOP|Graphics.HCENTER);
            }
        }
    }

    public int getShakeX() { return shakeOffX; }
    public int getShakeY() { return shakeOffY; }
}
