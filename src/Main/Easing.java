package Main;

/** Funcții de easing: t merge de la 0 la 1, rezultatul tot de la 0 la 1 (unele depășesc 1 puțin, pentru "overshoot"). */
public final class Easing {
    private Easing() {}

    public static float clamp01(float t) { return t < 0 ? 0 : (t > 1 ? 1 : t); }

    public static float lerp(float a, float b, float t) { return a + (b - a) * t; }

    /** Pornește repede și încetinește la final. Bun pentru lucruri care "ajung" undeva. */
    public static float outCubic(float t) { t = clamp01(t); float u = 1 - t; return 1 - u * u * u; }

    /** Pornește încet și accelerează. Bun pentru lucruri care "pleacă". */
    public static float inCubic(float t) { t = clamp01(t); return t * t * t; }

    public static float inOutCubic(float t) {
        t = clamp01(t);
        return t < 0.5f ? 4 * t * t * t : 1 - (float) Math.pow(-2 * t + 2, 3) / 2;
    }

    public static float outQuad(float t) { t = clamp01(t); return 1 - (1 - t) * (1 - t); }

    /** Trece puțin peste 1 și revine: un "pop" plăcut pentru apariții. */
    public static float outBack(float t) {
        t = clamp01(t);
        final float c1 = 1.70158f, c3 = c1 + 1;
        float u = t - 1;
        return 1 + c3 * u * u * u + c1 * u * u;
    }

    /** Ca un resort care se oprește. */
    public static float outElastic(float t) {
        t = clamp01(t);
        if (t == 0 || t == 1) return t;
        final float c4 = (float) (2 * Math.PI / 3);
        return (float) (Math.pow(2, -10 * t) * Math.sin((t * 10 - 0.75) * c4) + 1);
    }

    /** Apropie "current" de "target" cu o fracțiune fixă pe tick (netezire exponențială, independentă de FPS fiindcă tick-ul e fix). */
    public static float approach(float current, float target, float fractionPerTick) {
        return current + (target - current) * fractionPerTick;
    }
}
