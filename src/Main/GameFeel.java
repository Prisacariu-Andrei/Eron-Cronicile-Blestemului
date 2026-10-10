package Main;

/**
 * TOATE valorile de "game feel" într-un singur loc. Modifică-le aici ca să schimbi cum se simte jocul.
 *
 * Jocul rulează la 60 de update-uri pe secundă (un "tick" = 1/60 s), indiferent de câte cadre apuci să desenezi,
 * deci toate duratele de mai jos sunt în ticks (60 ticks = 1 secundă) și nu depind de FPS.
 * Pune un efect pe 0 ca să-l oprești.
 */
public final class GameFeel {
    private GameFeel() {}

    // =====================================================================================
    // MIȘCARE
    // =====================================================================================
    /** Multiplicator pentru viteza de mers (1.0 = viteza originală, 4 px/tick). Dash-ul nu este afectat. */
    public static final float PLAYER_SPEED_SCALE = 1.10f;
    /** Ticks până la viteza maximă după ce apeși o tastă (mai mic = mai reactiv). */
    public static final float PLAYER_ACCEL_TICKS = 5f;
    /** Ticks până la oprire după ce lași tasta (mai mic = oprire mai fermă). */
    public static final float PLAYER_DECEL_TICKS = 6f;

    // =====================================================================================
    // CAMERĂ
    // =====================================================================================
    /** Cât de repede ajunge camera din urmă pe Eron, pe tick (0.05 = foarte lent și plutitor, 1.0 = lipită de el). */
    public static final float CAMERA_FOLLOW = 0.16f;
    /** Cât se uită camera înainte, în direcția de mers (pixeli). 0 = dezactivat. */
    public static final float CAMERA_LOOK_AHEAD_PX = 70f;
    /** Dacă ținta e mai departe de atât (pixeli), camera sare direct (teleportare, hartă nouă). */
    public static final float CAMERA_SNAP_DISTANCE = 900f;

    // =====================================================================================
    // SCREEN SHAKE (strength = pixeli la început; scade spre 0 pe durată)
    // =====================================================================================
    public static final float SHAKE_PLAYER_HIT_BASE = 3f;      // la orice lovitură primită în luptă
    public static final float SHAKE_PER_DAMAGE = 0.22f;        // + atât pentru fiecare punct de damage
    public static final float SHAKE_MAX = 14f;                 // plafon, ca să nu se dezmembreze ecranul
    public static final int SHAKE_HIT_TICKS = 16;
    public static final float SHAKE_ENEMY_HIT_BASE = 2f;       // la lovitura dată de Eron
    public static final int SHAKE_BIG_HIT_DAMAGE = 20;         // de la atât în sus, o lovitură e "mare" și scutură în plus
    public static final float SHAKE_BIG_HIT_BONUS = 2.5f;
    public static final float SHAKE_BOSS_MULT = 1.6f;          // în lupta cu un boss toate scuturăturile de lovitură sunt mai puternice
    public static final float FLASH_ALPHA = 0.8f;              // cât de opac e fulgerul alb/roșu pe sprite-ul lovit (scade la 0 pe durata animației)
    public static final int DEATH_ANIM_TICKS = 55;             // cât ține animația de moarte a unui inamic
    public static final int WIN_DELAY_TICKS = 80;              // pauza dintre ultima lovitură și ecranul de recompensă (60 = 1 secundă)
    public static final int DEATH_PARTICLES = 34;
    public static final float SHAKE_ENEMY_DEATH = 4f;
    public static final float SHAKE_REACTION = 4.5f;           // reacție între elemente / combo
    public static final int SHAKE_REACTION_TICKS = 12;
    public static final int REACTION_PARTICLES = 26;
    public static final float SHAKE_PHASE_CHANGE = 14f;        // schimbare de fază Malakor
    public static final int SHAKE_PHASE_TICKS = 40;
    public static final float SHAKE_BATTLE_START = 6f;         // intrarea în luptă
    public static final int SHAKE_BATTLE_START_TICKS = 18;
    public static final float SHAKE_SPIKE = 5f;                // spike-urile de pe hartă
    public static final float SHAKE_DEATH = 12f;

    // =====================================================================================
    // HIT-STOP (îngheață jocul câteva cadre la o lovitură puternică)
    // =====================================================================================
    public static final int HIT_STOP_MIN_DAMAGE = 14;          // sub atât nu se întâmplă nimic
    public static final int HIT_STOP_TICKS = 4;                // ~66 ms
    public static final int HIT_STOP_BIG_DAMAGE = 28;
    public static final int HIT_STOP_BIG_TICKS = 7;            // ~116 ms

    // =====================================================================================
    // TRANZIȚII ȘI FADE-URI
    // =====================================================================================
    public static final int FADE_STATE_TICKS = 16;             // fade-in după schimbarea scenei (meniu, joc, etc.)
    public static final int FADE_BATTLE_START_TICKS = 26;      // flash "mistic" la începutul luptei
    public static final int FADE_BATTLE_END_TICKS = 22;

    // =====================================================================================
    // ANIMAȚII "VII" (idle)
    // =====================================================================================
    public static final float IDLE_BOB_PX = 1.6f;              // cât se leagănă inamicii/NPC-urile pe verticală
    public static final float IDLE_BOB_PERIOD_TICKS = 84f;     // durata unui ciclu de leagăn
    public static final float PLAYER_BREATH_SCALE = 0.025f;    // cât "respiră" Eron când stă (2.5%)
    public static final float WALK_BOB_PX = 2.0f;              // săritura mică la fiecare pas
    public static final float DASH_STRETCH = 0.28f;            // cât se alungește Eron la dash
    public static final float BATTLE_BREATH_SCALE = 0.018f;    // respirația personajelor în luptă

    // =====================================================================================
    // PARTICULE
    // =====================================================================================
    public static final boolean DUST_ENABLED = true;
    public static final int DUST_EVERY_TICKS = 5;              // la câte ticks lasă un nor de praf când merge
    public static final boolean AMBIENT_ENABLED = true;        // fulgi, licurici, scântei etc. în fiecare zonă
    public static final float AMBIENT_DENSITY = 1.0f;          // 0 = niciuna, 2 = de două ori mai multe
    public static final int HIT_SPARKS = 12;                   // scântei la lovitură
    public static final int REWARD_BURST = 26;                 // explozia la alegerea unei recompense

    // =====================================================================================
    // INTERFAȚĂ
    // =====================================================================================
    public static final float CARD_LIFT_SMOOTHING = 0.32f;     // cât de repede se ridică o carte la selectare (0..1 pe tick)
    public static final int CARD_DEAL_STAGGER_TICKS = 3;       // întârzierea dintre cărțile trase
    public static final int CARD_DEAL_TICKS = 14;              // cât durează să intre o carte în mână
    public static final int POPUP_POP_TICKS = 10;              // "pop" la apariția numerelor de damage
    public static final int MESSAGE_FADE_TICKS = 14;           // mesajele din HUD intră/ies lin

    // =====================================================================================
    // SUNET
    // =====================================================================================
    public static final boolean SOUND_FEEDBACK = true;         // sunetele create în cod (nu au nevoie de fișiere)
    public static final float SE_VOLUME = 0.55f;               // 0..1
    public static final boolean FOOTSTEPS = true;
    public static final int FOOTSTEP_EVERY_TICKS = 15;
    public static final int TYPEWRITER_BLIP_EVERY_CHARS = 2;   // un bip la fiecare N litere afișate

    // =====================================================================================
    // CUTSCENE
    // =====================================================================================
    public static final int LETTERBOX_HEIGHT = 110;            // înălțimea benzilor negre (pixeli)
    public static final int LETTERBOX_SLIDE_TICKS = 50;
    public static final int CUTSCENE_TEXT_TICKS_PER_CHAR = 2;  // viteza literelor (mai mic = mai repede)
    public static final int CUTSCENE_SPEEDUP_FACTOR = 4;       // cât de repede merge textul cât ții SPACE
}
