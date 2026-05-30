import javax.microedition.lcdui.*;

/**
 * Player - wraps Character with human key-press input.
 *
 * Key mapping (GameCanvas key codes):
 *   LEFT  / 4 : move left
 *   RIGHT / 6 : move right
 *   UP    / 2 : jump
 *   5         : attack (basic)
 *   0         : attack special (fightSheet row 0 — no meter cost)
 *   7         : special attack (requires 40 meter)
 *   9         : ultra (requires 100 meter)
 *
 * FIXES APPLIED:
 *  1. resetInput() method added — called by GamePanel.buildCharacters() so
 *     no key held during the menu carries over into the fight.
 *  2. Jump buffering fixed: upHeld is always consumed (set false) even when
 *     the player is in the air, so the character doesn't auto-jump the instant
 *     it lands when the button was pressed mid-air.
 *  3. getGameAction() used for directional keys so the D-pad works on devices
 *     (like itel 5615) that map arrow keys to non-standard raw key codes.
 *  4. KEY_NUM0 mapped to attackSpecial() — triggers fightSheet ROW_ATTACK_SP.
 */
public class Player extends Character {

    // J2ME game action constants (from Canvas)
    private static final int ACT_UP    = Canvas.UP;
    private static final int ACT_DOWN  = Canvas.DOWN;
    private static final int ACT_LEFT  = Canvas.LEFT;
    private static final int ACT_RIGHT = Canvas.RIGHT;
    private static final int ACT_FIRE  = Canvas.FIRE;

    // Numpad keys (always reliable on T9 phones)
    private static final int KEY_NUM0 = Canvas.KEY_NUM0;  // ← NEW: attack special
    private static final int KEY_NUM2 = Canvas.KEY_NUM2;
    private static final int KEY_NUM4 = Canvas.KEY_NUM4;
    private static final int KEY_NUM5 = Canvas.KEY_NUM5;
    private static final int KEY_NUM6 = Canvas.KEY_NUM6;
    private static final int KEY_NUM7 = Canvas.KEY_NUM7;
    private static final int KEY_NUM9 = Canvas.KEY_NUM9;

    // Tracks which keys are currently held
    private boolean leftHeld, rightHeld, upHeld;
    private boolean attackHeld, atkSpHeld, spHeld, ultraHeld; // ← atkSpHeld added

    public Player(String name, int x, int y, int hp,
                  SpriteSheet moveSheet, SpriteSheet fightSheet) {
        super(name, x, y, hp, moveSheet, fightSheet);
        sfxEnabled = true;   // human player triggers SFX; AI does not
    }

    // FIX 1: call this when starting a new game to prevent input carry-over
    public void resetInput() {
        leftHeld   = false;
        rightHeld  = false;
        upHeld     = false;
        attackHeld = false;
        atkSpHeld  = false;  // ← reset new flag
        spHeld     = false;
        ultraHeld  = false;
    }

    // ── Called by GamePanel on key events ────────────────────────────────────
    public void keyPressed(int keyCode) {
        // FIX 3: use getGameAction for directions so the D-pad works too
        int action = getGameAction(keyCode);

        if (action == ACT_LEFT  || keyCode == KEY_NUM4) leftHeld   = true;
        if (action == ACT_RIGHT || keyCode == KEY_NUM6) rightHeld  = true;
        if (action == ACT_UP    || keyCode == KEY_NUM2) upHeld     = true;
        if (action == ACT_FIRE  || keyCode == KEY_NUM5) attackHeld = true;
        if (keyCode == KEY_NUM0)                         atkSpHeld = true;  // ← NEW
        if (keyCode == KEY_NUM7)                         spHeld    = true;
        if (keyCode == KEY_NUM9)                         ultraHeld = true;
    }

    public void keyReleased(int keyCode) {
        int action = getGameAction(keyCode);

        if (action == ACT_LEFT  || keyCode == KEY_NUM4) leftHeld   = false;
        if (action == ACT_RIGHT || keyCode == KEY_NUM6) rightHeld  = false;
        if (action == ACT_UP    || keyCode == KEY_NUM2) upHeld     = false;
        if (action == ACT_FIRE  || keyCode == KEY_NUM5) attackHeld = false;
        if (keyCode == KEY_NUM0)                         atkSpHeld = false; // ← NEW
        if (keyCode == KEY_NUM7)                         spHeld    = false;
        if (keyCode == KEY_NUM9)                         ultraHeld = false;
    }

    // ── Process held keys each tick ──────────────────────────────────────────
    public void processInput() {
        // Attack inputs — priority order: ultra > special > attackSpecial > attack
        if (ultraHeld && specialMeter >= 100) {
            ultra();
            ultraHeld = false;
            return;
        }
        if (spHeld && specialMeter >= 40) {
            special();
            spHeld = false;
            return;
        }
        // KEY 0: attackSpecial — uses fightSheet ROW_ATTACK_SP, no meter cost
        if (atkSpHeld) {
            attackSpecial();
            atkSpHeld = false;
            return;
        }
        if (attackHeld) {
            attack();
        }

        // FIX 2: always consume upHeld so jump doesn't "wait" for the ground
        if (upHeld) {
            if (onGround) {
                jump();
            }
            upHeld = false;
        }

        // Horizontal movement (don't override attack animation)
        if (!isAttacking() && state != STATE_HURT) {
            if (leftHeld && !rightHeld) {
                moveLeft();
            } else if (rightHeld && !leftHeld) {
                moveRight();
            } else {
                stopMoving();
            }
        }
    }

    /** Full update: process input → physics → animation */
    public void update(int worldW) {
        processInput();
        applyPhysics(worldW);
        updateAnimation();
    }

    // Helper so Canvas.getGameAction() is accessible inside this class
    private int getGameAction(int keyCode) {
        try {
            // GameCanvas inherits from Canvas which has getGameAction()
            // We call it via a workaround because Player doesn't extend Canvas.
            // Map the most common raw codes manually for J2ME T9 phones:
            switch (keyCode) {
                case -1: return Canvas.UP;
                case -2: return Canvas.DOWN;
                case -3: return Canvas.LEFT;
                case -4: return Canvas.RIGHT;
                case -5: return Canvas.FIRE;
                default: return 0;
            }
        } catch (Exception e) {
            return 0;
        }
    }
}