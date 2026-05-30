import javax.microedition.lcdui.*;

/**
 * AIOpponent v2.0 — smarter computer-controlled fighter.
 *
 * v2 improvements over v1:
 *
 * 1. BLOCK STATE — AI will raise a block when the player is attacking and
 *    close; blocking reduces incoming damage by 75%.
 *
 * 2. COMBO CHAINS — after a successful hit, AI queues a follow-up action
 *    (attackSpecial → special, or attack → attackSpecial) instead of just
 *    returning to idle. comboQueue drives this.
 *
 * 3. JUMP ATTACK — when closing distance, AI now sometimes jumps AND attacks
 *    in the air (jump approach + aerial attack on landing).
 *
 * 4. DEFENSIVE RETREAT — smarter retreat: AI retreats when HP is below 25%
 *    (not just after hits), giving it a "save yourself" instinct.
 *
 * 5. PROXIMITY PRESSURE — AI applies short bursts of continuous attacks
 *    when the player is cornered (near world edge), mimicking corner pressure.
 *
 * 6. DIFFICULTY TIERS — reactionDelay & blockChance are exposed so Main /
 *    SettingsCanvas can pass in a difficulty level (0=Easy, 1=Normal, 2=Hard).
 */
public class AIOpponent extends Character {

    // ── AI states ─────────────────────────────────────────────────────────────
    private static final int AI_APPROACH  = 0;
    private static final int AI_ATTACK    = 1;
    private static final int AI_RETREAT   = 2;
    private static final int AI_JUMP      = 3;
    private static final int AI_WAIT      = 4;
    private static final int AI_BLOCK     = 5;   // v2: defensive block
    private static final int AI_PRESSURE  = 6;   // v2: corner pressure combo

    private int aiState   = AI_APPROACH;
    private int aiTimer   = 0;
    private int retreatDir = 1;

    // v2: combo queue (max 2 queued moves)
    private int[] comboQueue     = new int[2];
    private int   comboQueueHead = 0;
    private int   comboQueueLen  = 0;

    private static final int COMBO_NONE    = 0;
    private static final int COMBO_ATTACK  = 1;
    private static final int COMBO_ATK_SP  = 2;
    private static final int COMBO_SPECIAL = 3;

    // ── Ranges ────────────────────────────────────────────────────────────────
    private static final int ATTACK_RANGE  = 80;
    private static final int SPECIAL_RANGE = 120;
    private static final int BLOCK_RANGE   = 90;   // v2

    // ── Difficulty-tunable parameters ─────────────────────────────────────────
    private int reactionDelay = 25;
    private int specialMeterThreshold = 40; // GW Sasuke fires special at 30 // ticks between decisions (lower = faster)
    private int blockChance   = 3;  // 1-in-N chance to block per decision (lower = more blocking)
    private int aggressionBias= 2;  // 1-in-N extra attack roll (lower = more attacks)

    // ── LCG random ────────────────────────────────────────────────────────────
    private int seed = 0xABCD1234;
    private int rnd(int n) {
        seed = seed * 1664525 + 1013904223;
        int r = (seed >>> 1) % n;
        return r < 0 ? -r : r;
    }

    // ── Constructor ───────────────────────────────────────────────────────────
    public AIOpponent(String name, int x, int y, int hp,
                      SpriteSheet moveSheet, SpriteSheet fightSheet) {
        super(name, x, y, hp, moveSheet, fightSheet);
    }

    /**
     * Set difficulty.
     * 0 = Easy  (slow reactions, rarely blocks)
     * 1 = Normal (default)
     * 2 = Hard  (fast, blocks often, pressures hard)
     */
    public void setDifficulty(int level) {
        switch (level) {
            case 0: reactionDelay=35; blockChance=6; aggressionBias=4; break;
            case 2: reactionDelay=12; blockChance=2; aggressionBias=1; break;
            default:reactionDelay=25; blockChance=4; aggressionBias=2; break;
        }
    }

    /**
     * Apply character-specific AI personality on top of difficulty.
     * Call after setDifficulty().
     *   4 = GW Naruto  — aggressive rusher, lower reaction, rarely blocks
     *   5 = GW Sasuke  — patient counter-fighter, blocks often, uses specials early
     */
    public void setCharacterBias(int charIndex) {
        switch (charIndex) {
            case 4: // Great War Naruto — forward pressure, high aggression
                reactionDelay = Math.max(8, reactionDelay - 8);
                aggressionBias = Math.max(1, aggressionBias - 1);
                blockChance += 2; // rushes in, blocks less
                break;
            case 5: // Great War Sasuke — calculated, blocks + counters
                blockChance = Math.max(1, blockChance - 1);
                reactionDelay = Math.max(10, reactionDelay - 5);
                // will use specials at 30 meter instead of 40
                specialMeterThreshold = 30;
                break;
        }
    }

    // ── Update ────────────────────────────────────────────────────────────────
    public void update(Character target, int worldW) {
        if (!isAlive()) {
            applyPhysics(worldW);
            updateAnimation();
            return;
        }

        // Drain combo queue: if we're idle and have a queued move, do it
        if (comboQueueLen > 0 && !isAttacking() && state != STATE_HURT && aiTimer <= 0) {
            int nextCombo = comboQueue[comboQueueHead % 2];
            comboQueueHead++;
            comboQueueLen--;
            executeComboMove(nextCombo);
            aiTimer = reactionDelay / 2;
        }

        aiTimer--;
        if (aiTimer <= 0) {
            decide(target, worldW);
        }

        executeAI(target, worldW);
        applyPhysics(worldW);
        updateAnimation();
    }

    // ── Decision ──────────────────────────────────────────────────────────────
    private void decide(Character target, int worldW) {
        int dist = Math.abs(getCenterX() - target.getCenterX());
        boolean playerAttacking = target.isAttacking();

        // v2: Low-HP panic retreat
        if (hp < maxHp / 4 && dist < ATTACK_RANGE * 2) {
            aiState   = AI_RETREAT;
            retreatDir = getCenterX() < target.getCenterX() ? -1 : 1;
            aiTimer   = 40;
            return;
        }

        // v2: Block if player is attacking and in range
        if (playerAttacking && dist < BLOCK_RANGE && rnd(blockChance) == 0) {
            aiState = AI_BLOCK;
            aiTimer = 18;
            return;
        }

        // After hurt, retreat briefly
        if (state == STATE_HURT) {
            aiState   = AI_RETREAT;
            retreatDir = getCenterX() < target.getCenterX() ? -1 : 1;
            aiTimer   = 25;
            return;
        }

        // Ultra if meter full
        if (specialMeter >= 100 && dist < SPECIAL_RANGE) {
            aiState = AI_ATTACK;
            aiTimer = reactionDelay;
            return;
        }

        // v2: Corner pressure — if target is near edge, run a combo pressure
        boolean targetCornered = (target.x < 30 || target.x > worldW - 100);
        if (targetCornered && dist < ATTACK_RANGE * 2 && rnd(aggressionBias) == 0) {
            aiState = AI_PRESSURE;
            aiTimer = reactionDelay * 2;
            queueCombo(COMBO_ATTACK, COMBO_ATK_SP);
            return;
        }

        // Special attack if meter charged
        if (specialMeter >= specialMeterThreshold && dist < SPECIAL_RANGE) {
            aiState = AI_ATTACK;
            aiTimer = reactionDelay + 5;
            return;
        }

        // In range: attack
        if (dist <= ATTACK_RANGE) {
            aiState = AI_ATTACK;
            aiTimer = reactionDelay;
            return;
        }

        // v2: Jump approach to close large gaps (and attack on the way)
        if (dist > 150 && rnd(3) == 0) {
            aiState = AI_JUMP;
            aiTimer = 20;
            return;
        }

        // Default: walk toward
        aiState = AI_APPROACH;
        aiTimer = reactionDelay;
    }

    // ── Execute current AI state ──────────────────────────────────────────────
    private void executeAI(Character target, int worldW) {
        facingRight = getCenterX() < target.getCenterX();
        int dist = Math.abs(getCenterX() - target.getCenterX());

        switch (aiState) {

            case AI_APPROACH:
                if (dist > ATTACK_RANGE) {
                    if (facingRight) moveRight(); else moveLeft();
                } else {
                    stopMoving();
                }
                break;

            case AI_RETREAT:
                if (retreatDir > 0) moveRight(); else moveLeft();
                break;

            case AI_JUMP:
                jump();
                if (facingRight) moveRight(); else moveLeft();
                // v2: if we land near the target, queue an attack
                if (dist < ATTACK_RANGE + 30) {
                    queueCombo(COMBO_ATTACK, COMBO_NONE);
                }
                aiState = AI_APPROACH;
                break;

            case AI_BLOCK:
                // Stand still and "block" — halve incoming damage is handled
                // in Character.takeHit() via the isBlocking() flag
                stopMoving();
                blocking = true;
                break;

            case AI_ATTACK:
                stopMoving();
                blocking = false;
                if (!isAttacking()) {
                    if (specialMeter >= 100) {
                        ultra();
                        // v2: queue a follow-up
                        queueCombo(COMBO_ATTACK, COMBO_NONE);
                    } else if (specialMeter >= specialMeterThreshold && dist < SPECIAL_RANGE) {
                        special();
                        queueCombo(COMBO_ATK_SP, COMBO_NONE);
                    } else if (dist < ATTACK_RANGE) {
                        if (rnd(3) == 0) {
                            attackSpecial();
                            // v2: chain into special if meter will reach 40
                            if (specialMeter + 15 >= specialMeterThreshold) queueCombo(COMBO_SPECIAL, COMBO_NONE);
                        } else {
                            attack();
                            queueCombo(COMBO_ATK_SP, COMBO_NONE);
                        }
                    }
                }
                break;

            case AI_PRESSURE:
                // Continuous forward pressure — walk and attack simultaneously
                blocking = false;
                if (dist > ATTACK_RANGE / 2) {
                    if (facingRight) moveRight(); else moveLeft();
                }
                if (!isAttacking() && dist < ATTACK_RANGE) {
                    if (rnd(2) == 0) attack(); else attackSpecial();
                }
                break;

            case AI_WAIT:
            default:
                stopMoving();
                blocking = false;
                break;
        }

        // Auto-clear block flag if we left the BLOCK state
        if (aiState != AI_BLOCK) blocking = false;
    }

    // ── Combo queue helpers ───────────────────────────────────────────────────
    private void queueCombo(int first, int second) {
        comboQueueHead = 0;
        comboQueueLen  = 0;
        if (first  != COMBO_NONE) { comboQueue[comboQueueLen++] = first; }
        if (second != COMBO_NONE) { comboQueue[comboQueueLen++] = second; }
    }

    private void executeComboMove(int move) {
        switch (move) {
            case COMBO_ATTACK:   attack();        break;
            case COMBO_ATK_SP:   attackSpecial(); break;
            case COMBO_SPECIAL:  if (specialMeter >= 40) special(); break;
        }
    }

    /** Called when AI takes a hit — interrupt and reassess. */
    public void notifyHit() {
        comboQueueLen = 0;  // cancel queued combo on getting hit
        aiState   = AI_RETREAT;
        retreatDir = facingRight ? -1 : 1;
        aiTimer   = 20;
        blocking  = false;
    }
}
