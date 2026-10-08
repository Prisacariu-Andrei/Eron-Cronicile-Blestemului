# ERON: Cronicile Blestemului

Un RPG turn-based cu sistem de cărți (deckbuilder), scris în Java (Swing). Joci ca Eron, un
vrăjitor care traversează 6 zone pentru a-l înfrânge pe Malakor, Monarhul Amurgului, și a
dezlega blestemul nemuririi.

**Autori:** Prisacariu Andrei, Mihalache Costin-Daniel (grupa 1210B)
Documentația completă: `ERON Cronicile Blestemului.docx`

## Cum se rulează

1. Deschide folderul în **IntelliJ IDEA** (File → Open).
2. **JDK 25:** File → Project Structure → Project → SDK. Dacă lipsește: Add SDK → Download JDK → 25.
3. **SQLite:** biblioteca `org.xerial:sqlite-jdbc:3.45.1.0` este descărcată automat din Maven.
   Dacă nu: File → Project Structure → Libraries → `xerial.sqlite.jdbc` → descarcă.
4. Rulează `src/Main/Main.java`.

Salvările se scriu în `eron_save.db`, lângă proiect (nu este urmărit de git).

## Controale

| Tastă | Acțiune |
| --- | --- |
| W A S D / săgeți | Mișcare |
| Space | Dash |
| E | Vorbește cu un NPC |
| I | Inventar |
| J | Jurnalul de misiuni (Q schimbă tab-ul) |
| ESC | Pauză |
| În luptă: A / D, ENTER, T | Alege cartea, joac-o, termină tura |
| F3 | Afișează/ascunde panoul de performanță (FPS, durata update/desenare, tile-uri desenate, entități, memorie) |
| În prolog: ENTER, SPACE (ținut), ESC | Continuă, text mai rapid, sari peste prolog |

## Cum se simte jocul ("game feel")

Toate reglajele sunt într-un singur fișier: [`src/Main/GameFeel.java`](src/Main/GameFeel.java), cu comentarii pentru fiecare
valoare (viteza de mers și accelerarea, camera, intensitatea shake-ului, hit-stop, particule, sunet, cutscene).
Duratele sunt în *ticks* (60 pe secundă), deci nu depind de FPS. Orice efect se oprește punându-l pe 0 sau `false`.

- `Effects` — shake, hit-stop, fade-uri între scene, particule și ambianța fiecărei zone.
- `ParticleSystem` — particule într-un pool fix (nu creează obiecte la fiecare particulă).
- `Camera` — urmărire lină, privire înainte, limitată la dimensiunea reală a hărții.
- `SoundManager` — dacă nu există fișiere în `res/sound/se`, efectele sunt create în cod.

## Structura

| Pachet | Conținut |
| --- | --- |
| `Main` | `GamePanel` (game loop, mediator), input, coliziuni, evenimente pe hartă, sunet |
| `GameState` | Stările jocului (State pattern): meniu, joc, luptă, dialog, inventar, prolog... |
| `Entity` | Jucătorul, inamicii (`EnemyFactory`), generalii (boss-uri), NPC-uri, cărți, rune |
| `Observer` | Interfețele grafice (UI) și bara de viață (Observer pattern) |
| `Tiles` | `TileManager`: încărcarea și desenarea hărților din `res/Maps` |
| `Database` | Salvare/încărcare cu SQLite |
| `ProgressManagement` | Misiunile (`QuestManager`) |
