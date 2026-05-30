import javax.microedition.media.*;
import javax.microedition.media.control.*;
import java.io.*;

/**
 * MusicPlayer — J2ME MMAPI (JSR-135) background music manager.
 *
 * FIX: All references to javax.microedition.media.Player are fully
 * qualified (javax.microedition.media.Player) to avoid the name clash
 * with the game's own Player.java character class.
 */
public final class MusicPlayer {

    // Fully qualified to avoid clash with game's Player.java
    private static javax.microedition.media.Player player = null;
    private static boolean paused = false;

    private MusicPlayer() {}

    /**
     * Load and start playing a resource file in an infinite loop.
     * Stops any currently playing track first.
     * @param resource  JAR-root path e.g. "/music.amr"
     */
    public static void play(String resource) {
        stop();
        try {
            InputStream is = MusicPlayer.class.getResourceAsStream(resource);
            if (is == null) return;

            String ct = getContentType(resource);
            player = Manager.createPlayer(is, ct);
            player.setLoopCount(-1);   // loop forever
            player.realize();
            player.prefetch();

            try {
                VolumeControl vc =
                    (VolumeControl) player.getControl("VolumeControl");
                if (vc != null) vc.setLevel(100);
            } catch (Exception e) { /* volume control unavailable */ }

            player.start();
            paused = false;
        } catch (Exception e) {
            player = null;  // unsupported format — fail silently
        }
    }

    /** Pause playback (e.g. on MIDlet pauseApp). */
    public static void pause() {
        if (player != null && !paused) {
            try { player.stop(); paused = true; }
            catch (Exception e) { /* ignore */ }
        }
    }

    /** Resume a paused player. */
    public static void resume() {
        if (player != null && paused) {
            try { player.start(); paused = false; }
            catch (Exception e) { /* ignore */ }
        }
    }

    /** Stop and release all native media resources. */
    public static void stop() {
        if (player != null) {
            try {
                player.stop();
                player.deallocate();
                player.close();
            } catch (Exception e) { /* ignore */ }
            player = null;
            paused = false;
        }
    }

    /** True if the player is currently running. */
    public static boolean isPlaying() {
        if (player == null) return false;
        try {
            return player.getState() ==
                   javax.microedition.media.Player.STARTED;
        } catch (Exception e) {
            return false;
        }
    }

    // Map file extension → MIME type for Manager.createPlayer()
    private static String getContentType(String path) {
        String p = path.toLowerCase();
        if (p.endsWith(".amr"))              return "audio/amr";
        if (p.endsWith(".mid") ||
            p.endsWith(".midi"))             return "audio/midi";
        if (p.endsWith(".mp3"))              return "audio/mpeg";
        if (p.endsWith(".wav"))              return "audio/x-wav";
        if (p.endsWith(".aac"))              return "audio/aac";
        return "audio/mpeg";
    }
}