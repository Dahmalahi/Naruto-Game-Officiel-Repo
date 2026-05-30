import javax.microedition.lcdui.*;
import javax.microedition.lcdui.game.GameCanvas;

/**
 * SplashCanvas — animated splash screen.
 *
 * Streams 210 JPEG frames one-at-a-time (ezgif-frame-024 … ezgif-frame-233).
 * Peak heap usage = ~1 uncompressed frame (~150 KB for 240x320 16-bit).
 *
 * Memory safety rules (1.5 MB heap):
 *   1. Only ONE Image object lives in memory at any time.
 *   2. img is nulled immediately after rendering, BEFORE the next load.
 *   3. System.gc() is called after nulling so the GC runs during the
 *      timing delay — not while the new Image.createImage() is executing.
 *   4. A 10 ms sleep after gc() gives slow J2ME GCs time to actually free
 *      the old pixel buffer before the next allocation begins.
 *
 * Tuning:
 *   FRAME_DELAY_MS = 33  → ~30 fps  (fast devices / emulators)
 *   FRAME_DELAY_MS = 50  → ~20 fps  (mid-range)
 *   FRAME_DELAY_MS = 66  → ~15 fps  (safe for 1.5 MB heap devices)
 */
public class SplashCanvas extends GameCanvas implements Runnable {

    private final Main midlet;
    private boolean isRunning = false;

    private static final int FIRST_FRAME  = 24;
    private static final int LAST_FRAME   = 233;
    private static final int FRAME_COUNT  = LAST_FRAME - FIRST_FRAME + 1; // 210

    // ── Tune this for your device ─────────────────────────────────────────────
    // 66 ms (~15 fps) is the safest choice for a strict 1.5 MB heap.
    // Raise to 33 ms (30 fps) only if the device has more headroom.
    private static final int FRAME_DELAY_MS = 66;

    private int     currentFrame = 0;
    private Image   img          = null;
    private boolean skipped      = false;

    public SplashCanvas(Main midlet) {
        super(false);
        this.midlet = midlet;
        setFullScreenMode(true);
    }

    // ── Shadow text helper ────────────────────────────────────────────────────
    private static void drawText(Graphics g, String s, int x, int y,
                                  int anchor, int color) {
        g.setColor(0x000000);
        g.drawString(s, x + 1, y + 1, anchor);
        g.setColor(color);
        g.drawString(s, x, y, anchor);
    }

    // ── Frame path builder ────────────────────────────────────────────────────
    /**
     * Builds "/ezgif-frame-024.jpg" … "/ezgif-frame-233.jpg".
     * Uses concatenation only — no String.format, safe for CLDC 1.0.
     */
    private static String framePath(int n) {
        String s = String.valueOf(n);
        // Zero-pad to 3 digits: 7 → "007", 24 → "024", 100 → "100"
        while (s.length() < 3) s = "0".concat(s);
        return "/ezgif-frame-".concat(s).concat(".jpg");
    }

    // ── Safe single-frame loader ──────────────────────────────────────────────
    /**
     * Loads exactly one frame into img.
     *
     * Call ONLY after img has already been set to null and System.gc()
     * has been called, so the old pixel buffer is eligible for collection
     * before the new one is allocated.
     */
    private void loadFrame(int frameIndex) {
        // Defensive null — caller should have done this, but be explicit
        img = null;

        try {
            img = Image.createImage(framePath(FIRST_FRAME + frameIndex));
        } catch (Exception e) {
            img = null; // missing or corrupt frame — show black, keep going
        }
    }

    // ── Lifecycle ─────────────────────────────────────────────────────────────
    public void start() {
        isRunning    = true;
        skipped      = false;
        currentFrame = 0;

        // Pre-load the very first frame before the thread starts so frame 0
        // is ready to display without a blank flash.
        loadFrame(0);

        new Thread(this).start();
    }

    public void stop() {
        isRunning = false;

        // Release the current frame immediately
        img = null;
        System.gc();

        try {
            Graphics g = getGraphics();
            g.setColor(0x000000);
            g.fillRect(0, 0, ScreenMetrics.W, ScreenMetrics.H);
            flushGraphics();
        } catch (Exception e) { /* ignore */ }
    }

    // ── Animation loop ────────────────────────────────────────────────────────
    public void run() {
        while (isRunning && currentFrame < FRAME_COUNT) {

            // 1. Draw the current frame (img already loaded)
            if (isRunning) render();

            // 2. IMMEDIATELY release the image reference after rendering.
            //    This makes the pixel buffer eligible for GC right now,
            //    before we sleep or load the next frame.
            img = null;

            // 3. Hint to the GC to run while we still have the full
            //    FRAME_DELAY_MS window available — not during the next load.
            System.gc();

            // 4. Wait out the frame delay. The GC runs concurrently here.
            //    We poll in 10 ms slices so a key press is detected quickly.
            long deadline = System.currentTimeMillis() + FRAME_DELAY_MS;
            while (isRunning && !skipped
                   && System.currentTimeMillis() < deadline) {
                try { Thread.sleep(10); } catch (InterruptedException e) { break; }
            }

            if (skipped) break;

            // 5. Advance counter and load next frame.
            //    At this point the old frame has had FRAME_DELAY_MS worth of
            //    GC time — on a 66 ms delay that is ~4 GC cycles on a slow JVM.
            currentFrame++;
            if (currentFrame < FRAME_COUNT) {
                loadFrame(currentFrame);
            }
        }

        if (isRunning) {
            stop();
            midlet.showTitle();
        }
    }

    // ── Rendering ─────────────────────────────────────────────────────────────
    private void render() {
        if (!isRunning) return;

        final int W = ScreenMetrics.W;
        final int H = ScreenMetrics.H;

        Graphics g = getGraphics();

        // Black base (visible if frame is missing or smaller than screen)
        g.setColor(0x000000);
        g.fillRect(0, 0, W, H);

        // Draw frame — 240×320 frames fill the screen exactly
        if (img != null) {
            int iw    = img.getWidth();
            int ih    = img.getHeight();
            int drawX = (W - iw) >> 1;
            int drawY = (H - ih) >> 1;
            if (drawX < 0) drawX = 0;
            if (drawY < 0) drawY = 0;
            g.drawImage(img, drawX, drawY, Graphics.TOP | Graphics.LEFT);
        }

        // ── Progress bar (thin, very bottom) ──────────────────────────────────
        int barH   = 3;
        int barY   = H - barH - 1;
        int barW   = W - 4;
        int barX   = 2;
        int filled = (currentFrame * barW) / Math.max(1, FRAME_COUNT - 1);

        g.setColor(0x222222); g.fillRect(barX, barY, barW, barH);  // track
        g.setColor(0xFF6600); g.fillRect(barX, barY, filled, barH); // fill

        // ── Skip hint — shadow text for readability on any frame ──────────────
        drawText(g, "5=Skip", W - 4, H - 15,
                 Graphics.TOP | Graphics.RIGHT, 0xAAAAAA);

        flushGraphics();
    }

    // ── Key input — ANY key skips ─────────────────────────────────────────────
    protected void keyPressed(int keyCode) {
        skipped = true;
    }
}