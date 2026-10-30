package Main;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IllegalFormatException;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Limba jocului (RO/EN). Textele vin din res/lang/&lt;cod&gt;.json (cheie plată -> text, ex. "menu.new_game").
 * Lanț de rezervă: limba curentă -> ro -> cheia însăși, deci o cheie lipsă nu strică niciodată jocul.
 * Alegerea se salvează global în {@link Settings} (nu pe slot de salvare).
 */
public final class Lang {
    public static final String[] CODES = {"ro", "en"};

    private static String current = "ro";
    private static final Map<String, Map<String, String>> CACHE = new HashMap<>();
    private static final Set<String> REPORTED = new HashSet<>();

    private Lang() {}

    /** Codul limbii curente ("ro" sau "en"). */
    public static String get() { return current; }

    /** Schimbă limba; un cod necunoscut e ignorat. Se aplică imediat, fără repornire. */
    public static void set(String code) {
        if (code == null) return;
        String c = code.trim().toLowerCase(Locale.ROOT);
        for (String known : CODES) {
            if (known.equals(c)) {
                current = c;
                strings(c);   // încarcă acum, ca prima afișare să nu aștepte
                return;
            }
        }
    }

    /** Următoarea limbă din listă (dir = +1) sau cea dinainte (dir = -1), circular. */
    public static String next(int dir) {
        int i = 0;
        for (int k = 0; k < CODES.length; k++) if (CODES[k].equals(current)) i = k;
        return CODES[Math.floorMod(i + dir, CODES.length)];
    }

    /** Textul pentru cheie, fără formatare: un "%" din text e sigur. */
    public static String t(String key) { return lookup(key); }

    /** Textul pentru cheie, formatat cu String.format; dacă formatul nu se potrivește, întoarce textul brut. */
    public static String t(String key, Object... args) {
        String v = lookup(key);
        if (args == null || args.length == 0) return v;
        try {
            return String.format(v, args);
        } catch (IllegalFormatException e) {
            return v;
        }
    }

    /** Calea unui fișier de date tradus: "/Data/en/<nume>" dacă limba e engleza și el există, altfel "/Data/<nume>". */
    public static String dataPath(String name) {
        if ("en".equals(current)) {
            String p = "/Data/en/" + name;
            if (Lang.class.getResource(p) != null) return p;
        }
        return "/Data/" + name;
    }

    private static String lookup(String key) {
        String v = strings(current).get(key);
        if (v == null && !"ro".equals(current)) v = strings("ro").get(key);
        if (v == null) {
            if (REPORTED.add(key)) System.err.println("[Lang] cheie lipsă: " + key);
            return key;
        }
        return v;
    }

    private static Map<String, String> strings(String code) {
        Map<String, String> m = CACHE.get(code);
        if (m == null) {
            m = load(code);
            CACHE.put(code, m);
        }
        return m;
    }

    private static Map<String, String> load(String code) {
        Map<String, String> out = new HashMap<>();
        String path = "/lang/" + code + ".json";
        try (InputStream is = Lang.class.getResourceAsStream(path)) {
            if (is == null) {
                System.err.println("[Lang] lipsește " + path);
                return out;
            }
            Object parsed = Json.parse(new String(is.readAllBytes(), StandardCharsets.UTF_8));
            for (Map.Entry<String, Object> e : Json.map(parsed).entrySet()) {
                if (e.getKey().startsWith("_")) continue;
                if (e.getValue() instanceof String s) out.put(e.getKey(), s);
            }
        } catch (Exception e) {
            System.err.println("[Lang] " + path + " nu poate fi citit: " + e.getMessage());
        }
        return out;
    }
}
