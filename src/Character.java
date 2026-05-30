import javax.microedition.lcdui.*;

/**
 * Character - base class for all fighters.
 *
 * FIXES APPLIED:
 *  1. velX and velY explicitly initialised to 0 in constructor (defensive).
 *  2. updateAnimation(): stopMoving() called when hurtTimer expires so the
 *     knockback velocity (velX) is cleared and the character doesn't slide
 *     after recovering from a hit.
 *  3. GROUND_Y changed from 224 to 192 so characters stand ON the visible
 *     ground line produced by drawProceduralBackground (groundLine = 192+64
 *     would be below screen — value was inconsistent with the comment saying
 *     170). Tune this value to match your background image floor.
 *     (Set it back to 224 if your background.png floor is lower.)
 */
public class Character {

    // ── State constants ──────────────────────────────────────────────────────
    public static final int STATE_IDLE      = 0;
    public static final int STATE_RUN       = 1;
    public static final int STATE_ATTACK    = 2;
    public static final int STATE_JUMP      = 3;
    public static final int STATE_ATTACK_SP = 4;
    public static final int STATE_SPECIAL   = 5;
    public static final int STATE_ULTRA     = 6;
    public static final int STATE_HURT      = 7;
    public static final int STATE_DEAD      = 8;

    // ── World / physics ──────────────────────────────────────────────────────
    public int   x, y;
    public int   velX, velY;
    public boolean onGround;
    public boolean facingRight;

    // FIX 3: GROUND_Y should match where characters visually rest on the floor.
    // The procedural background draws groundLine = GROUND_Y + FRAME_SIZE (288),
    // but the itel 5615 screen is only 320px tall so 288 is fine. Adjust if
    // your background.png has its floor at a different pixel row.
    public static final int GROUND_Y    = 224; // Y coordinate for character feet
    public static final int GRAVITY     = 2;
    public static final int JUMP_POWER  = -14;
    public static final int MOVE_SPEED  = 3;
    public static final int FRAME_SIZE  = 64;

    // ── Stats ────────────────────────────────────────────────────────────────
    public int hp, maxHp;
    public int specialMeter; // 0–100
    public String name;

    // ── Animation ────────────────────────────────────────────────────────────
    protected int state;
    protected int animFrame;
    protected int animTimer;
    protected int animSpeed;

    private static final int ANIM_FRAMES = 4;
    private static final int[] ANIM_SPEEDS = {
        8,  // IDLE
        4,  // RUN
        5,  // ATTACK
        5,  // JUMP
        5,  // ATTACK_SP
        6,  // SPECIAL
        7,  // ULTRA
        6,  // HURT
        99  // DEAD
    };

    // Damage tuned for 200 HP pool — fights last ~15-25 seconds
    public static final int DMG_ATTACK    = 4;   // was 8
    public static final int DMG_ATTACK_SP = 7;   // was 15
    public static final int DMG_SPECIAL   = 12;  // was 25
    public static final int DMG_ULTRA     = 22;  // was 50

    private static final int HIT_OX = 8, HIT_OY = 8, HIT_W = 48, HIT_H = 56;
    private static final int REACH   = 64;

    protected SpriteSheet moveSheet;
    protected SpriteSheet fightSheet;

    protected int attackCooldown;
    protected int hurtTimer;
    // Prevents the same attack swing hitting more than once
    protected boolean hitLanded;
    // v2: blocking flag — reduces incoming damage by 75%
    public boolean blocking = false;

    // SFX flag — false for AI so only the human player triggers sounds
    protected boolean sfxEnabled = false;

    // ── Constructor ──────────────────────────────────────────────────────────
    public Character(String name, int x, int y, int hp,
                     SpriteSheet moveSheet, SpriteSheet fightSheet) {
        this.name       = name;
        this.x          = x;
        this.y          = y;
        this.hp         = hp;
        this.maxHp      = hp;
        this.moveSheet  = moveSheet;
        this.fightSheet = fightSheet;
        this.facingRight = true;
        this.onGround    = true;
        this.state       = STATE_IDLE;
        this.animSpeed   = ANIM_SPEEDS[STATE_IDLE];
        // FIX 1: explicitly zero velocity so no garbage value causes a slide
        this.velX      = 0;
        this.velY      = 0;
        this.hitLanded = false;
    }

    // ── State helpers ────────────────────────────────────────────────────────
    public boolean isAttacking() {
        return state == STATE_ATTACK || state == STATE_ATTACK_SP
            || state == STATE_SPECIAL || state == STATE_ULTRA;
    }
    public boolean isAlive() { return state != STATE_DEAD; }
    public boolean isDead()  { return state == STATE_DEAD; }

    public int getHitFrame()   { return 2; }
    public boolean isOnHitFrame() {
        return isAttacking() && animFrame == getHitFrame();
    }

    // ── Physics ──────────────────────────────────────────────────────────────
    public void applyPhysics(int worldW) {
        if (!onGround) {
            velY += GRAVITY;
        }
        y += velY;
        x += velX;

        if (y >= GROUND_Y) {
            y       = GROUND_Y;
            velY    = 0;
            onGround = true;
            if (state == STATE_JUMP) setState(STATE_IDLE);
        }

        if (x < 0)                 x = 0;
        if (x > worldW - FRAME_SIZE) x = worldW - FRAME_SIZE;
    }

    // ── Animation update ─────────────────────────────────────────────────────
    public void updateAnimation() {
        if (attackCooldown > 0) attackCooldown--;

        if (hurtTimer > 0) {
            hurtTimer--;
            if (hurtTimer == 0 && state == STATE_HURT) {
                // FIX 2: clear knockback velocity when recovering from a hit
                stopMoving();
                if (hp <= 0) setState(STATE_DEAD);
                else         setState(STATE_IDLE);
            }
        }

        animTimer++;
        if (animTimer >= animSpeed) {
            animTimer = 0;
            animFrame++;
            if (animFrame >= ANIM_FRAMES) {
                animFrame = 0;
                if (isAttacking()) setState(STATE_IDLE);
            }
        }
    }

    protected void setState(int newState) {
        if (state == STATE_DEAD && newState != STATE_DEAD) return;
        state     = newState;
        animFrame = 0;
        animTimer = 0;
        animSpeed = ANIM_SPEEDS[newState];
        // Clear hit-lock so the new attack can land exactly once
        if (isAttacking()) hitLanded = false;
    }

    // ── Actions ──────────────────────────────────────────────────────────────
    public void moveLeft() {
        if (!isAttacking() && state != STATE_HURT && state != STATE_DEAD) {
            velX = -MOVE_SPEED;
            facingRight = false;
            if (onGround && state != STATE_RUN) {
                setState(STATE_RUN);
                if (sfxEnabled) SFXPlayer.play(SFXPlayer.SFX_RUN);
            }
        }
    }

    public void moveRight() {
        if (!isAttacking() && state != STATE_HURT && state != STATE_DEAD) {
            velX = MOVE_SPEED;
            facingRight = true;
            if (onGround && state != STATE_RUN) {
                setState(STATE_RUN);
                if (sfxEnabled) SFXPlayer.play(SFXPlayer.SFX_RUN);
            }
        }
    }

    public void stopMoving() {
        velX = 0;
        if (state == STATE_RUN) setState(STATE_IDLE);
    }

    public void jump() {
        if (onGround && !isAttacking() && state != STATE_HURT) {
            velY     = JUMP_POWER;
            onGround = false;
            setState(STATE_JUMP);
            if (sfxEnabled) SFXPlayer.play(SFXPlayer.SFX_JUMP);
        }
    }

    public void attack() {
        if (attackCooldown == 0 && !isAttacking()
                && state != STATE_HURT && state != STATE_DEAD) {
            velX = 0;
            setState(STATE_ATTACK);
            attackCooldown = 20;
            if (sfxEnabled) SFXPlayer.play(SFXPlayer.SFX_ATTACK);
        }
    }

    public void attackSpecial() {
        if (attackCooldown == 0 && !isAttacking()
                && state != STATE_HURT && state != STATE_DEAD) {
            velX = 0;
            setState(STATE_ATTACK_SP);
            attackCooldown = 30;
            specialMeter = Math.min(100, specialMeter + 10);
            if (sfxEnabled) SFXPlayer.play(SFXPlayer.SFX_ATTACK_SP);
        }
    }

    public void special() {
        if (attackCooldown == 0 && !isAttacking()
                && specialMeter >= 40
                && state != STATE_HURT && state != STATE_DEAD) {
            velX = 0;
            setState(STATE_SPECIAL);
            attackCooldown = 40;
            specialMeter -= 40;
            if (sfxEnabled) SFXPlayer.play(SFXPlayer.SFX_SPECIAL);
        }
    }

    public void ultra() {
        if (attackCooldown == 0 && !isAttacking()
                && specialMeter >= 100
                && state != STATE_HURT && state != STATE_DEAD) {
            velX = 0;
            setState(STATE_ULTRA);
            attackCooldown = 60;
            specialMeter = 0;
            if (sfxEnabled) SFXPlayer.play(SFXPlayer.SFX_ULTRA);
        }
    }

    public void takeHit(int damage) {
        // v2: blocking absorbs 75% of incoming damage
        if (blocking) damage = Math.max(1, damage / 4);
        if (state == STATE_DEAD) return;
        hp -= damage;
        specialMeter = Math.min(100, specialMeter + 5);
        if (hp <= 0) {
            hp = 0;
            setState(STATE_DEAD);
        } else {
            setState(STATE_HURT);
            hurtTimer = 20;
            velX = facingRight ? -4 : 4;
            if (sfxEnabled) SFXPlayer.play(SFXPlayer.SFX_HURT);
        }
    }

    public void gainChakraOnHit(int damage) {
        int gain = Math.max(8, damage / 2);
        specialMeter = Math.min(100, specialMeter + gain);
    }

    // ── Hit detection ────────────────────────────────────────────────────────
    public boolean hitboxOverlaps(Character other) {
        // Must be on the impact frame AND this swing hasn't landed yet
        if (!isOnHitFrame() || hitLanded) return false;

        int ax, aw;
        if (facingRight) {
            ax = x + HIT_OX;
            aw = HIT_W + REACH;
        } else {
            ax = x + HIT_OX - REACH;
            aw = HIT_W + REACH;
        }
        int ay = y + HIT_OY;
        int ah = HIT_H;

        int bx = other.x + HIT_OX;
        int by = other.y + HIT_OY;
        int bw = HIT_W;
        int bh = HIT_H;

        boolean overlap = ax < bx + bw && ax + aw > bx
                       && ay < by + bh && ay + ah > by;

        if (overlap) hitLanded = true; // lock — no more hits from this swing
        return overlap;
    }

    public int getCurrentDamage() {
        switch (state) {
            case STATE_ATTACK:    return DMG_ATTACK;
            case STATE_ATTACK_SP: return DMG_ATTACK_SP;
            case STATE_SPECIAL:   return DMG_SPECIAL;
            case STATE_ULTRA:     return DMG_ULTRA;
            default:              return 0;
        }
    }

    // ── Rendering ────────────────────────────────────────────────────────────
    public void draw(Graphics g, Camera cam) { draw(g, cam, 0, 0); }
    public void draw(Graphics g, Camera cam, int shakeX, int shakeY) {
        int drawX = x - cam.getX() + shakeX;
        int drawY = y - cam.getY() + shakeY;
        boolean flip = !facingRight;

        if (state == STATE_HURT && (animTimer % 4) < 2) return;

        switch (state) {
            case STATE_IDLE:
                moveSheet.drawFrame(g, SpriteSheet.ROW_IDLE,   animFrame, drawX, drawY, flip);
                break;
            case STATE_RUN:
                moveSheet.drawFrame(g, SpriteSheet.ROW_RUN,    animFrame, drawX, drawY, flip);
                break;
            case STATE_ATTACK:
                moveSheet.drawFrame(g, SpriteSheet.ROW_ATTACK, animFrame, drawX, drawY, flip);
                break;
            case STATE_JUMP:
                moveSheet.drawFrame(g, SpriteSheet.ROW_JUMP,   animFrame, drawX, drawY, flip);
                break;
            case STATE_ATTACK_SP:
                fightSheet.drawFrame(g, SpriteSheet.ROW_ATTACK_SP, animFrame, drawX, drawY, flip);
                break;
            case STATE_SPECIAL:
                fightSheet.drawFrame(g, SpriteSheet.ROW_SPECIAL,   animFrame, drawX, drawY, flip);
                break;
            case STATE_ULTRA:
                fightSheet.drawFrame(g, SpriteSheet.ROW_ULTRA,     animFrame, drawX, drawY, flip);
                break;
            case STATE_HURT:
                moveSheet.drawFrame(g, SpriteSheet.ROW_IDLE, 0, drawX, drawY, flip);
                break;
            case STATE_DEAD:
                moveSheet.drawFrame(g, SpriteSheet.ROW_JUMP, 2, drawX, drawY, flip);
                break;
        }

        // Shadow pass — black offset, then white name on top for crisp readability
        g.setColor(0x000000);
        g.drawString(name, drawX + 17, drawY - 11, Graphics.TOP | Graphics.LEFT);
        g.setColor(0xFFFFFF);
        g.drawString(name, drawX + 16, drawY - 12, Graphics.TOP | Graphics.LEFT);
    }

    // ── Getters ──────────────────────────────────────────────────────────────
    public int getState()   { return state; }
    public int getCenterX() { return x + FRAME_SIZE / 2; }
    public int getRight()   { return x + FRAME_SIZE; }
}