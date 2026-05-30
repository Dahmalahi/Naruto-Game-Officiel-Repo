import javax.microedition.midlet.*;
import javax.microedition.lcdui.*;

/**
 * NARUTO FIGHTERS — Main MIDlet
 *
 * Navigation flow:
 *   SplashCanvas → TitleCanvas → MenuCanvas (char select) → GamePanel (fight)
 *                              → SettingsCanvas
 *                              → AboutCanvas
 */
public class Main extends MIDlet {

    private Display   display;
    private GamePanel gamePanel;
    private int       selectedChar     = 0;
    private int       selectedOpponent = 1;
    private int       selectedBg       = 0;   // chosen stage index
    private boolean   firstStart       = true;

    // Black transition canvas
    private static final class BlackCanvas extends Canvas {
        BlackCanvas() { setFullScreenMode(true); }
        protected void paint(Graphics g) {
            g.setColor(0x000000);
            g.fillRect(0, 0, getWidth(), getHeight());
        }
    }

    // ── MIDlet lifecycle ──────────────────────────────────────────────────────
    public void startApp() throws MIDletStateChangeException {
        display = Display.getDisplay(this);
        if (firstStart) {
            firstStart = false;
            BlackCanvas probe = new BlackCanvas();
            ScreenMetrics.init(probe.getWidth(), probe.getHeight());
            MusicPlayer.play("/music.amr");
            showSplash();          // ← splash first, then title
        } else {
            // Resumed after interruption
            MusicPlayer.resume();
            if (gamePanel != null) gamePanel.resumeGame();
        }
    }

    public void pauseApp() {
        MusicPlayer.pause();
        if (gamePanel != null) gamePanel.pauseGame();
    }

    public void destroyApp(boolean u) throws MIDletStateChangeException {
        MusicPlayer.stop();
        SFXPlayer.releaseAll();
        if (gamePanel != null) gamePanel.stopGame();
    }

    // ── Navigation ────────────────────────────────────────────────────────────

    /** Show the animated splash screen (runs once at startup). */
    void showSplash() {
        SplashCanvas splash = new SplashCanvas(this);
        display.setCurrent(splash);
        splash.start();
    }

    /** Show the main title screen (Play / Settings / About). */
    void showTitle() {
        if (gamePanel != null) { gamePanel.stopGame(); gamePanel = null; }
        TitleCanvas title = new TitleCanvas(this);
        display.setCurrent(title);
        title.start();
    }

    /** Show character select menu (called from TitleCanvas "PLAY"). */
    void showCharSelect() {
        MenuCanvas menu = new MenuCanvas(this, selectedChar);
        display.setCurrent(menu);
        menu.start();
    }

    /** Show settings screen. */
    void showSettings() {
        SettingsCanvas sc = new SettingsCanvas(this);
        display.setCurrent(sc);
        sc.show();
    }

    /** Show about / credits screen. */
    void showAbout() {
        AboutCanvas ac = new AboutCanvas(this);
        display.setCurrent(ac);
        ac.show();
    }

    /** Keep for MenuCanvas backward compat (phase 1 char confirm). */
    void onCharSelected(int charIndex) {
        selectedChar = charIndex;
    }

    /**
     * Called by MenuCanvas after both characters are confirmed.
     * Goes to stage select before starting the fight.
     */
    void onFight(int opponentIndex) {
        selectedOpponent = opponentIndex;
        showStageSelect();
    }

    /** Show stage/background select screen. */
    void showStageSelect() {
        StageSelectCanvas sc = new StageSelectCanvas(this, selectedChar, selectedOpponent);
        display.setCurrent(sc);
        sc.start();
    }

    /**
     * Called by StageSelectCanvas when the player confirms a stage.
     * Shows black transition frame then starts the fight.
     */
    void onStageSelected(int playerChar, int opponentChar, int bgIndex) {
        selectedChar     = playerChar;
        selectedOpponent = opponentChar;
        selectedBg       = bgIndex;
        display.setCurrent(new BlackCanvas());
        try { Thread.sleep(120); } catch (InterruptedException e) {}
        gamePanel = new GamePanel(this, selectedChar, selectedOpponent, selectedBg);
        display.setCurrent(gamePanel);
        gamePanel.startGame();
    }

    /** Back to menu after a fight ends. */
    void showMenu() {
        showTitle();
    }

    void onExit() {
        MusicPlayer.stop();
        SFXPlayer.releaseAll();
        notifyDestroyed();
    }
}
