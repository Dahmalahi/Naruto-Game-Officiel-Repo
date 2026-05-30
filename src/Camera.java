/**
 * Camera - simple follow camera for side-scrolling.
 *
 * Keeps the player character centred on screen,
 * clamped so the camera never shows beyond the world edges.
 */
public class Camera {

    private int x, y;
    private final int screenW, screenH;
    private final int worldW, worldH;

    public Camera(int screenW, int screenH, int worldW, int worldH) {
        this.screenW = screenW;
        this.screenH = screenH;
        this.worldW  = worldW;
        this.worldH  = worldH;
    }

    /**
     * Update camera to centre between player and opponent.
     * This keeps both fighters visible at all times.
     */
    public void update(int playerCX, int opponentCX) {
        // Track midpoint between the two fighters
        int midX = (playerCX + opponentCX) / 2;

        // Desired camera left edge
        x = midX - screenW / 2;

        // Clamp so we don't show beyond world bounds
        if (x < 0)            x = 0;
        if (x > worldW - screenW) x = worldW - screenW;

        // Y is fixed (single-floor game)
        y = 0;
    }

    public int getX() { return x; }
    public int getY() { return y; }
}
