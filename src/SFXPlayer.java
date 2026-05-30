import javax.microedition.media.*;
import javax.microedition.media.control.*;
import java.io.*;

/**
 * SFXPlayer — plays short one-shot AMR sound effects.
 *
 * Each SFX slot pre-loads its player once on first use (lazy init),
 * then stop/start-from-zero on every play() call.
 *
 * Files expected in JAR root:
 *   /sfx_jump.amr
 *   /sfx_run.amr
 *   /sfx_attack.amr
 *   /sfx_attack_sp.amr
 *   /sfx_special.amr
 *   /sfx_ultra.amr
 *   /sfx_hurt.amr
 *
 * Uses fully-qualified javax.microedition.media.Player to avoid clash
 * with the game's own Player.java class.
 */
public final class SFXPlayer {

    public static final int SFX_JUMP      = 0;
    public static final int SFX_RUN       = 1;
    public static final int SFX_ATTACK    = 2;
    public static final int SFX_ATTACK_SP = 3;
    public static final int SFX_SPECIAL   = 4;
    public static final int SFX_ULTRA     = 5;
    public static final int SFX_HURT      = 6;

    private static final int COUNT = 7;

    private static final String[] PATHS = {
        "/sfx_jump.amr",
        "/sfx_run.amr",
        "/sfx_attack.amr",
        "/sfx_attack_sp.amr",
        "/sfx_special.amr",
        "/sfx_ultra.amr",
        "/sfx_hurt.amr"
    };

    // One pre-built player per SFX slot; null = not yet loaded or load failed
    private static javax.microedition.media.Player[] players =
        new javax.microedition.media.Player[COUNT];

    private static boolean enabled = true;   // mirrors SettingsCanvas.sfxOn

    private SFXPlayer() {}

    /**
     * Play the given SFX if enabled. Non-blocking: stops any prior
     * instance of the same clip and restarts from the beginning.
     */
    public static void play(int sfx) {
        if (!enabled) return;
        if (sfx < 0 || sfx >= COUNT) return;

        // Lazy-load on first use
        if (players[sfx] == null) {
            players[sfx] = load(PATHS[sfx]);
            if (players[sfx] == null) return;   // file missing / unsupported
        }

        try {
            javax.microedition.media.Player p = players[sfx];
            int state = p.getState();

            // Stop if playing
            if (state == javax.microedition.media.Player.STARTED) {
                p.stop();
            }

            // Rewind to start using StopTimeControl if available,
            // otherwise close & reload (MIDP 2.0 safe fallback)
            try {
                p.setMediaTime(0);
            } catch (Exception e) {
                // setMediaTime not supported on all devices — reload
                try { p.close(); } catch (Exception ex) {}
                players[sfx] = load(PATHS[sfx]);
                if (players[sfx] == null) return;
                p = players[sfx];
            }

            p.start();
        } catch (Exception e) {
            // Device doesn't support this clip — null out so we stop retrying
            players[sfx] = null;
        }
    }

    /** Enable or disable all SFX (called by SettingsCanvas). */
    public static void setEnabled(boolean on) {
        enabled = on;
        if (!on) stopAll();
    }

    /** Stop every currently-playing SFX clip. */
    public static void stopAll() {
        for (int i = 0; i < COUNT; i++) {
            if (players[i] != null) {
                try { players[i].stop(); } catch (Exception e) {}
            }
        }
    }

    /** Release all native media resources (call on MIDlet destroy). */
    public static void releaseAll() {
        for (int i = 0; i < COUNT; i++) {
            if (players[i] != null) {
                try {
                    players[i].stop();
                    players[i].deallocate();
                    players[i].close();
                } catch (Exception e) {}
                players[i] = null;
            }
        }
    }

    // ── Internal helpers ──────────────────────────────────────────────────────

    private static javax.microedition.media.Player load(String path) {
        try {
            InputStream is = SFXPlayer.class.getResourceAsStream(path);
            if (is == null) return null;
            javax.microedition.media.Player p =
                Manager.createPlayer(is, "audio/amr");
            p.setLoopCount(1);
            p.realize();
            p.prefetch();
            try {
                VolumeControl vc =
                    (VolumeControl) p.getControl("VolumeControl");
                if (vc != null) vc.setLevel(100);
            } catch (Exception e) {}
            return p;
        } catch (Exception e) {
            return null;    // file missing or codec unsupported — fail silently
        }
    }
}
