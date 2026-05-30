import javax.microedition.lcdui.*;
import javax.microedition.lcdui.game.*;

/**
 * GamePanel v2.1
 *
 * BUG FIX (Nokia 5230 — IllegalArgumentException in drawBackground):
 *   drawRegion() throws IllegalArgumentException if the requested source
 *   rectangle goes outside the image bounds. This happened because:
 *     1. srcW was SW (screen width) but bg image might be narrower.
 *     2. srcH was SH (screen height) but bg image is often shorter.
 *     3. srcX could push srcX+srcW past the image right edge.
 *   Fix: clamp srcX/srcY/srcW/srcH to the actual image dimensions before
 *   calling drawRegion, and fill any uncovered screen area with a solid colour.
 */
public class GamePanel extends GameCanvas implements Runnable {

    private static final int TARGET_FPS = 30;
    private static final int TICK_MS    = 1000 / TARGET_FPS;

    private static final int WORLD_W = 480;
    private static final int WORLD_H = 320;

    private final Main midlet;
    private final int  selectedChar, selectedOpponent, selectedBg;

    private Player        player;
    private AIOpponent    opponent;
    private Camera        camera;
    private EffectManager effects;

    private SpriteSheet narutoMove, narutoFight, sasukeMove, sasukeFight;
    private SpriteSheet kakashiMove, kakashiFight, sakuraMove, sakuraFight;
    private SpriteSheet gwNarutoMove, gwNarutoFight, gwSasukeMove, gwSasukeFight;
    private Image       background;
    // Cached background dimensions to avoid getWidth/getHeight calls every frame
    private int bgW = 0, bgH = 0;

    private static final int GS_PLAYING=0, GS_PAUSED=1, GS_WIN=2, GS_LOSE=3, GS_DRAW=4;
    private int gameState=GS_PLAYING, resultTimer=0, resultFade=0;
    private static final int FADE_MAX=8;

    private int playerDisplayHp=-1, opponentDisplayHp=-1;
    private Thread  gameThread;
    private boolean running=false;
    private int SW, SH;

    public GamePanel(Main midlet, int sel, int opp, int bg) {
        super(false);
        this.midlet=midlet; selectedChar=sel; selectedOpponent=opp; selectedBg=bg;
        setFullScreenMode(true);
    }

    // ── Shadow text helpers ───────────────────────────────────────────────────
    private static void drawText(Graphics g, String s, int x, int y,
                                  int anchor, int color) {
        g.setColor(0x000000);
        g.drawString(s, x + 1, y + 1, anchor);
        g.setColor(color);
        g.drawString(s, x, y, anchor);
    }

    private static void drawBigText(Graphics g, String s, int x, int y,
                                     int anchor, int glowColor, int topColor) {
        g.setColor(glowColor);
        g.drawString(s, x - 1, y,     anchor);
        g.drawString(s, x + 1, y,     anchor);
        g.drawString(s, x,     y - 1, anchor);
        g.drawString(s, x,     y + 1, anchor);
        g.setColor(0x000000);
        g.drawString(s, x + 2, y + 2, anchor);
        g.setColor(topColor);
        g.drawString(s, x, y, anchor);
    }

    // ── Lifecycle ─────────────────────────────────────────────────────────────
    public void startGame() {
        SW=ScreenMetrics.W; SH=ScreenMetrics.H;
        loadAssets(); buildCharacters();
        camera  = new Camera(SW,SH,WORLD_W,WORLD_H);
        effects = new EffectManager();
        gameState=GS_PLAYING; resultTimer=0; resultFade=0;
        playerDisplayHp=-1; opponentDisplayHp=-1;
        running=true; gameThread=new Thread(this); gameThread.start();
    }

    public void pauseGame()  { gameState=GS_PAUSED; }
    public void resumeGame() { if(gameState==GS_PAUSED) gameState=GS_PLAYING; }

    public void stopGame() {
        running=false;
        try { if(gameThread!=null) gameThread.join(); } catch(InterruptedException e){}
        gameThread=null; freeAssets();
    }

    private void freeAssets() {
        narutoMove=null; narutoFight=null; sasukeMove=null; sasukeFight=null;
        kakashiMove=null; kakashiFight=null; sakuraMove=null; sakuraFight=null;
        gwNarutoMove=null; gwNarutoFight=null; gwSasukeMove=null; gwSasukeFight=null;
        background=null; bgW=0; bgH=0;
        player=null; opponent=null; camera=null; effects=null;
    }

    private static final String[] MOVE_PATHS={
        "/naruto_move.png","/sasuke_move.png","/kakashi_move.png","/sakura_move.png",
        "/gw_naruto_move.png","/gw_sasuke_move.png"};
    private static final String[] FIGHT_PATHS={
        "/naruto_fight.png","/sasuke_fight.png","/kakashi_fight.png","/sakura_fight.png",
        "/gw_naruto_fight.png","/gw_sasuke_fight.png"};

    private void loadCharSheets(int idx) {
        switch(idx){
            case 0: narutoMove=new SpriteSheet(MOVE_PATHS[0]); narutoFight=new SpriteSheet(FIGHT_PATHS[0]); break;
            case 1: sasukeMove=new SpriteSheet(MOVE_PATHS[1]); sasukeFight=new SpriteSheet(FIGHT_PATHS[1]); break;
            case 2: kakashiMove=new SpriteSheet(MOVE_PATHS[2]); kakashiFight=new SpriteSheet(FIGHT_PATHS[2]); break;
            case 3: sakuraMove=new SpriteSheet(MOVE_PATHS[3]); sakuraFight=new SpriteSheet(FIGHT_PATHS[3]); break;
            case 4: gwNarutoMove=new SpriteSheet(MOVE_PATHS[4]); gwNarutoFight=new SpriteSheet(FIGHT_PATHS[4]); break;
            case 5: gwSasukeMove=new SpriteSheet(MOVE_PATHS[5]); gwSasukeFight=new SpriteSheet(FIGHT_PATHS[5]); break;
        }
    }

    private void loadAssets() {
        loadCharSheets(selectedChar);
        if(selectedOpponent!=selectedChar) loadCharSheets(selectedOpponent);
        String[] bgPaths={"/bg1.png","/bg2.png","/bg3.png"};
        String bgPath=(selectedBg>=0&&selectedBg<bgPaths.length)?bgPaths[selectedBg]:bgPaths[0];
        try{
            background=Image.createImage(bgPath);
            // Cache dimensions once — avoids repeated JNI calls per frame
            bgW=background.getWidth();
            bgH=background.getHeight();
        } catch(Exception e){ background=null; bgW=0; bgH=0; }
    }

    private void buildCharacters() {
        int gY=Character.GROUND_Y;
        String[] names={"Naruto","Sasuke","Kakashi","Sakura","GW Naruto","GW Sasuke"};
        int[] hpVals={200,200,220,180,240,230};
        player=new Player(names[selectedChar],60,gY,hpVals[selectedChar],
            getSpriteMove(selectedChar),getSpriteFight(selectedChar));
        opponent=new AIOpponent(names[selectedOpponent],WORLD_W-130,gY,hpVals[selectedOpponent],
            getSpriteMove(selectedOpponent),getSpriteFight(selectedOpponent));
        player.facingRight=true; opponent.facingRight=false; player.resetInput();
    }

    private SpriteSheet getSpriteMove(int i){
        switch(i){case 0:return narutoMove;case 1:return sasukeMove;case 2:return kakashiMove;case 3:return sakuraMove;case 4:return gwNarutoMove;case 5:return gwSasukeMove;default:return null;}}
    private SpriteSheet getSpriteFight(int i){
        switch(i){case 0:return narutoFight;case 1:return sasukeFight;case 2:return kakashiFight;case 3:return sakuraFight;case 4:return gwNarutoFight;case 5:return gwSasukeFight;default:return null;}}

    // ── Game loop ─────────────────────────────────────────────────────────────
    public void run() {
        long prevMs=System.currentTimeMillis();
        int  accum=0;
        while(running){
            long nowMs=System.currentTimeMillis();
            int delta=(int)(nowMs-prevMs); prevMs=nowMs;
            if(delta>100) delta=100;
            accum+=delta;

            while(accum>=TICK_MS){
                accum-=TICK_MS;
                if(gameState==GS_PLAYING){
                    update();
                } else if(gameState==GS_WIN||gameState==GS_LOSE||gameState==GS_DRAW){
                    effects.update();
                    resultTimer--;
                    if(resultFade<FADE_MAX) resultFade++;
                    if(resultTimer<=0){ returnToMenu(); return; }
                }
            }

            if(gameState==GS_PLAYING) render();
            else if(gameState==GS_WIN||gameState==GS_LOSE||gameState==GS_DRAW) renderResultScreen();

            long elapsed=System.currentTimeMillis()-nowMs;
            long sleep=TICK_MS-elapsed;
            if(sleep>0) try{ Thread.sleep(sleep); } catch(InterruptedException e){ break; }
        }
    }

    private void update(){
        player.update(WORLD_W);
        opponent.update(player,WORLD_W);
        effects.update();

        if(player.hitboxOverlaps(opponent)&&!opponent.isDead()){
            int dmg=player.getCurrentDamage();
            opponent.takeHit(dmg); opponent.notifyHit();
            player.gainChakraOnHit(dmg);
            spawnHitEffects(opponent,dmg,player.getState(),true);
        }
        if(opponent.hitboxOverlaps(player)&&!player.isDead()){
            int dmg=opponent.getCurrentDamage();
            player.takeHit(dmg);
            opponent.gainChakraOnHit(dmg);
            spawnHitEffects(player,dmg,opponent.getState(),false);
        }

        camera.update(player.getCenterX(),opponent.getCenterX());
        updateHudDrain();
        checkEndCondition();
    }

    private void spawnHitEffects(Character victim, int dmg, int attackState, boolean playerHit){
        int wx=victim.getCenterX(), wy=victim.y+16;
        int sparkColor, shakeStr, shakeDur, sparkCount;
        String popLabel;
        switch(attackState){
            case Character.STATE_ULTRA:
                sparkColor=0xFFDD00; shakeStr=5; shakeDur=8; sparkCount=10; popLabel="ULTRA!"; break;
            case Character.STATE_SPECIAL:
                sparkColor=0x00FFFF; shakeStr=3; shakeDur=5; sparkCount=7; popLabel="SPECIAL!"; break;
            case Character.STATE_ATTACK_SP:
                sparkColor=0xFF8800; shakeStr=2; shakeDur=3; sparkCount=5; popLabel="x"+dmg; break;
            default:
                sparkColor=0xFF4400; shakeStr=1; shakeDur=2; sparkCount=3; popLabel=null; break;
        }
        effects.spawnHitSparks(wx,wy,sparkCount,sparkColor);
        effects.triggerShake(shakeStr,shakeDur);
        if(popLabel!=null) effects.spawnPop(popLabel,wx,wy,playerHit?0xFFDD00:0xFF4444);
    }

    private void returnToMenu(){
        if(!running) return;
        running=false; freeAssets(); midlet.showMenu();
    }

    private void checkEndCondition(){
        if(gameState!=GS_PLAYING) return;
        boolean pDead=player.isDead(), oDead=opponent.isDead();
        if(pDead&&oDead){ gameState=GS_DRAW; resultTimer=TARGET_FPS*3; }
        else if(oDead)  { gameState=GS_WIN;  resultTimer=TARGET_FPS*3; }
        else if(pDead)  { gameState=GS_LOSE; resultTimer=TARGET_FPS*3; }
    }

    private void updateHudDrain(){
        if(playerDisplayHp<0)   playerDisplayHp  =player.hp*4;
        if(opponentDisplayHp<0) opponentDisplayHp=opponent.hp*4;
        int t=player.hp*4;
        if(playerDisplayHp>t){ playerDisplayHp-=6; if(playerDisplayHp<t) playerDisplayHp=t; }
        t=opponent.hp*4;
        if(opponentDisplayHp>t){ opponentDisplayHp-=6; if(opponentDisplayHp<t) opponentDisplayHp=t; }
    }

    // ── Render ────────────────────────────────────────────────────────────────
    private void render(){
        Graphics g=getGraphics();
        g.setClip(0,0,SW,SH);
        g.setColor(0x000000); g.fillRect(0,0,SW,SH);
        int sx=effects.getShakeX(), sy=effects.getShakeY();
        drawBackground(g,sx,sy);
        opponent.draw(g,camera,sx,sy);
        player.draw(g,camera,sx,sy);
        effects.draw(g,camera.getX()-sx,camera.getY()-sy);
        drawHUD(g);
        flushGraphics();
    }

    private void renderResultScreen(){
        Graphics g=getGraphics();
        g.setClip(0,0,SW,SH);
        g.setColor(0x000000); g.fillRect(0,0,SW,SH);
        drawBackground(g,0,0);
        opponent.draw(g,camera,0,0);
        player.draw(g,camera,0,0);
        effects.draw(g,camera.getX(),camera.getY());
        drawHUD(g);
        drawResultOverlay(g);
        flushGraphics();
    }

    /**
     * Draw background image with camera scroll and shake offset.
     *
     * FIX: drawRegion() requires that srcX >= 0, srcY >= 0,
     *      srcX + srcW <= imgW, srcY + srcH <= imgH.
     * Violating any of those throws IllegalArgumentException on Nokia 5230
     * (and many other handsets). We clamp all four values to the image
     * bounds and fill any exposed edges with the fallback colour.
     */
    private void drawBackground(Graphics g, int shakeX, int shakeY){
        // Always fill the screen first so edges exposed by shake or a
        // smaller-than-screen image are covered with a matching colour.
        g.setColor(0x1A0533);
        g.fillRect(0, 0, SW, SH);

        if(background == null || bgW <= 0 || bgH <= 0){
            drawProceduralBackground(g, shakeX, shakeY);
            return;
        }

        // --- Compute source X (camera scroll) --------------------------------
        // We want to show the slice of the background that corresponds to
        // camera.getX() -- adjusted by the shake offset.
        int srcX = camera.getX() - shakeX;

        // Clamp srcX so the source rectangle never exceeds image bounds
        // srcX must satisfy: 0 <= srcX  AND  srcX + srcW <= bgW
        // We use srcW = min(SW, bgW) so we never ask for more than the image.
        int srcW = Math.min(SW, bgW);
        int srcH = Math.min(SH, bgH);

        if(srcX < 0) srcX = 0;
        if(srcX + srcW > bgW) srcX = bgW - srcW;
        // After clamping, srcX must still be >= 0
        if(srcX < 0) srcX = 0;

        // Source Y — fixed at 0 (no vertical scroll), clamp height
        int srcY = 0;
        // srcH already clamped above; srcY=0 is always valid

        // Destination: centre the (possibly smaller) image on screen
        int destX = shakeX + (SW - srcW) / 2;
        int destY = shakeY + (SH - srcH) / 2;

        // Guard: drawRegion crashes if srcW or srcH is 0
        if(srcW <= 0 || srcH <= 0) return;

        try {
            g.drawRegion(background,
                         srcX, srcY, srcW, srcH,
                         0,
                         destX, destY,
                         Graphics.TOP | Graphics.LEFT);
        } catch(Exception e){
            // Absolute last-resort fallback — should never reach here after
            // the clamping above, but keeps the game running if it does.
            drawProceduralBackground(g, shakeX, shakeY);
        }
    }

    /** Drawn when background image is missing or null. */
    private void drawProceduralBackground(Graphics g, int ox, int oy){
        int hudH = ScreenMetrics.HUD_H;
        int gL   = Character.GROUND_Y + Character.FRAME_SIZE;
        g.setColor(0x1A0533); g.fillRect(ox, oy, SW, gL);
        g.setColor(0x3D2A1E); g.fillRect(ox, oy + gL, SW, SH - gL);
        g.setColor(0x5A3E2B); g.fillRect(ox, oy + gL - 2, SW, 4);
        g.setColor(0xFFEE88); g.fillArc(SW - 80 + ox, hudH + 4 + oy, 46, 46, 0, 360);
        g.setColor(0xFFFFFF);
        int[] sx={20,50,90,130,200}; int[] sy={hudH+6,hudH+28,hudH+10,hudH+40,hudH+14};
        for(int i=0;i<sx.length;i++) g.fillRect(sx[i]+ox, sy[i]+oy, 2, 2);
    }

    // ── HUD ───────────────────────────────────────────────────────────────────
    private void drawHUD(Graphics g){
        final int M=ScreenMetrics.MARGIN, hudH=ScreenMetrics.HUD_H;
        final int barH=10, barW=SW/2-M*2-4, barY=M/2;
        final int chakY=barY+barH+2, nameY=chakY+7;

        g.setColor(0x000000); g.fillRect(0,0,SW,hudH);
        g.setColor(0x444444); g.drawLine(0,hudH,SW,hudH);

        drawLifeBar(g,M,barY,barW,barH,player.hp,playerDisplayHp/4,player.maxHp,false);
        drawChakraBar(g,M,chakY,barW,5,player.specialMeter);
        drawText(g, player.name, M, nameY, Graphics.TOP|Graphics.LEFT, 0xFFFFFF);

        int oGhost=opponentDisplayHp/4, aiX=SW-M-barW;
        drawLifeBar(g,aiX,barY,barW,barH,opponent.hp,oGhost,opponent.maxHp,true);
        drawChakraBar(g,aiX,chakY,barW,5,opponent.specialMeter);
        drawText(g, opponent.name, SW-M, nameY, Graphics.TOP|Graphics.RIGHT, 0xFFFFFF);

        if(player.specialMeter>=100)
            drawText(g, "ULTRA!", M, nameY+10, Graphics.TOP|Graphics.LEFT, 0xFFFF00);
        if(opponent.specialMeter>=100)
            drawText(g, "ULTRA!", SW-M, nameY+10, Graphics.TOP|Graphics.RIGHT, 0xFF4400);
    }

    private void drawLifeBar(Graphics g, int x, int y, int w, int h,
                              int hp, int ghostHp, int maxHp, boolean rtl){
        g.setColor(0x1A0000); g.fillRect(x,y,w,h);
        if(maxHp<=0) return;
        int filled=Math.max(0,Math.min(w, w*hp/maxHp));
        int gf    =Math.max(0,Math.min(w, w*ghostHp/maxHp));
        int bc = hp*3>maxHp*2 ? 0x00DD00 : hp*3>maxHp ? 0xFF8800 : 0xEE1100;
        if(!rtl){
            if(gf>filled){ g.setColor(0xCCCCCC); g.fillRect(x+filled,y,gf-filled,h); }
            if(filled>0){  g.setColor(bc);        g.fillRect(x,y,filled,h); }
        } else {
            int rx=x+w-filled;
            if(gf>filled){ g.setColor(0xCCCCCC); g.fillRect(rx-(gf-filled),y,gf-filled,h); }
            if(filled>0){  g.setColor(bc);        g.fillRect(rx,y,filled,h); }
        }
        g.setColor(0xAAAAAA); g.drawRect(x,y,w,h);
        g.setColor(0xFFFFFF); g.drawLine(x+1,y+1,x+w-1,y+1);
    }

    private void drawChakraBar(Graphics g,int x,int y,int w,int h,int meter){
        g.setColor(0x001122); g.fillRect(x,y,w,h);
        if(meter>0){ g.setColor(meter>=100?0x00FFFF:0x0077AA); g.fillRect(x,y,w*meter/100,h); }
        g.setColor(meter>=100?0x00FFFF:0x004466); g.drawRect(x,y,w,h);
    }

    // ── Result overlay ────────────────────────────────────────────────────────
    private void drawResultOverlay(Graphics g){
        final int boxW=SW-ScreenMetrics.MARGIN*6, boxH=70;
        final int boxX=ScreenMetrics.centreX(boxW), boxY=ScreenMetrics.centreY(boxH);

        if(resultFade>=4){
            g.setColor(0x000000);
            for(int row=0;row<SH;row+=2) g.fillRect(0,row,SW,1);
        }
        g.setColor(0x000000);
        for(int i=0;i<3;i++) g.drawRect(boxX-i,boxY-i,boxW+i*2,boxH+i*2);
        g.setColor(0x0A0A22); g.fillRect(boxX,boxY,boxW,boxH/2);
        g.setColor(0x111133); g.fillRect(boxX,boxY+boxH/2,boxW,boxH/2);
        g.setColor(0x555577); g.drawRect(boxX,boxY,boxW,boxH);

        String msg; int glowColor, topColor;
        switch(gameState){
            case GS_WIN:  msg="YOU WIN!";  glowColor=0xAA8800; topColor=0xFFDD00; break;
            case GS_LOSE: msg="YOU LOSE!"; glowColor=0x880000; topColor=0xFF2200; break;
            default:      msg="DRAW!";     glowColor=0x555555; topColor=0xCCCCCC; break;
        }
        drawBigText(g, msg, SW/2, boxY+8,
                    Graphics.TOP|Graphics.HCENTER, glowColor, topColor);

        int secs=(resultTimer/TARGET_FPS)+1;
        drawText(g, "Back in "+secs+"s...", SW/2, boxY+28,
                 Graphics.TOP|Graphics.HCENTER, 0x888888);
        drawText(g, "5/FIRE = return now", SW/2, boxY+44,
                 Graphics.TOP|Graphics.HCENTER, 0x666666);
    }

    protected void keyPressed(int keyCode){
        if(gameState==GS_PLAYING){
            player.keyPressed(keyCode);
            if(keyCode==-11||keyCode==-6) returnToMenu();
        } else if(gameState==GS_WIN||gameState==GS_LOSE||gameState==GS_DRAW){
            returnToMenu();
        }
    }
    protected void keyReleased(int keyCode){
        if(gameState==GS_PLAYING) player.keyReleased(keyCode);
    }
}
