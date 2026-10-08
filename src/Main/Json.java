package Main;

import Main.Exception.GameExceptionHandler;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Parser JSON minimal (fără biblioteci externe): obiecte -> Map, liste -> List, numere -> Double,
 * plus String, Boolean și null. Destul pentru fișierele de date din res/Data.
 */
public final class Json {
    private final String s;
    private int pos;

    private Json(String s) { this.s = s; }

    public static Object parse(String text) {
        Json j = new Json(text);
        Object v = j.value();
        j.skipSpace();
        if (j.pos != j.s.length()) throw new IllegalArgumentException("JSON: text în plus la poziția " + j.pos);
        return v;
    }

    /** Citește și parsează un fișier din resurse (ex. "/Data/npcs.json"). Returnează null dacă lipsește sau e greșit. */
    public static Object parseResource(String path) {
        try (InputStream is = Json.class.getResourceAsStream(path)) {
            if (is == null) {
                GameExceptionHandler.getInstance().handleWarning("Fișierul de date lipsește: " + path, null);
                return null;
            }
            return parse(new String(is.readAllBytes(), StandardCharsets.UTF_8));
        } catch (Exception e) {
            GameExceptionHandler.getInstance().handleWarning("Fișierul de date nu poate fi citit: " + path, e);
            return null;
        }
    }

    // --- ajutoare pentru citirea valorilor ---

    @SuppressWarnings("unchecked")
    public static Map<String, Object> map(Object o) {
        return o instanceof Map ? (Map<String, Object>) o : new LinkedHashMap<>();
    }

    @SuppressWarnings("unchecked")
    public static List<Object> list(Object o) {
        return o instanceof List ? (List<Object>) o : new ArrayList<>();
    }

    public static String str(Object o, String def) { return o instanceof String ? (String) o : def; }

    public static int integer(Object o, int def) { return o instanceof Number ? ((Number) o).intValue() : def; }

    public static double number(Object o, double def) { return o instanceof Number ? ((Number) o).doubleValue() : def; }

    public static boolean bool(Object o, boolean def) { return o instanceof Boolean ? (Boolean) o : def; }

    // --- parser ---

    private Object value() {
        skipSpace();
        if (pos >= s.length()) throw new IllegalArgumentException("JSON: sfârșit neașteptat");
        char c = s.charAt(pos);
        switch (c) {
            case '{': return object();
            case '[': return array();
            case '"': return string();
            case 't': expect("true"); return Boolean.TRUE;
            case 'f': expect("false"); return Boolean.FALSE;
            case 'n': expect("null"); return null;
            default: return number();
        }
    }

    private Map<String, Object> object() {
        Map<String, Object> m = new LinkedHashMap<>();
        pos++; // {
        skipSpace();
        if (peek() == '}') { pos++; return m; }
        while (true) {
            skipSpace();
            String key = string();
            skipSpace();
            if (s.charAt(pos++) != ':') throw new IllegalArgumentException("JSON: lipsește ':' la poziția " + pos);
            m.put(key, value());
            skipSpace();
            char c = s.charAt(pos++);
            if (c == '}') return m;
            if (c != ',') throw new IllegalArgumentException("JSON: așteptam ',' sau '}' la poziția " + pos);
        }
    }

    private List<Object> array() {
        List<Object> l = new ArrayList<>();
        pos++; // [
        skipSpace();
        if (peek() == ']') { pos++; return l; }
        while (true) {
            l.add(value());
            skipSpace();
            char c = s.charAt(pos++);
            if (c == ']') return l;
            if (c != ',') throw new IllegalArgumentException("JSON: așteptam ',' sau ']' la poziția " + pos);
        }
    }

    private String string() {
        if (s.charAt(pos) != '"') throw new IllegalArgumentException("JSON: așteptam un șir la poziția " + pos);
        pos++;
        StringBuilder sb = new StringBuilder();
        while (true) {
            char c = s.charAt(pos++);
            if (c == '"') return sb.toString();
            if (c != '\\') { sb.append(c); continue; }
            char e = s.charAt(pos++);
            switch (e) {
                case 'n' -> sb.append('\n');
                case 't' -> sb.append('\t');
                case 'r' -> sb.append('\r');
                case 'b' -> sb.append('\b');
                case 'f' -> sb.append('\f');
                case 'u' -> { sb.append((char) Integer.parseInt(s.substring(pos, pos + 4), 16)); pos += 4; }
                default -> sb.append(e); // \" \\ \/
            }
        }
    }

    private Double number() {
        int start = pos;
        while (pos < s.length() && "+-0123456789.eE".indexOf(s.charAt(pos)) >= 0) pos++;
        if (start == pos) throw new IllegalArgumentException("JSON: valoare neașteptată la poziția " + pos);
        return Double.valueOf(s.substring(start, pos));
    }

    private void expect(String word) {
        if (!s.startsWith(word, pos)) throw new IllegalArgumentException("JSON: așteptam '" + word + "' la poziția " + pos);
        pos += word.length();
    }

    private char peek() { return pos < s.length() ? s.charAt(pos) : '\0'; }

    private void skipSpace() {
        while (pos < s.length() && Character.isWhitespace(s.charAt(pos))) pos++;
    }
}
