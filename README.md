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

## Monede, magazin și NPC-uri (date, nu cod)

- **`res/Data/economy.json`** — monedele de start, câte monede dă fiecare inamic (`enemyDrops`) și fiecare misiune
  (`questRewards`), prețul fiecărei cărți (`cardPrices`), raportul de vânzare (`sellRatio`, 0.5 = 50%) și pachetul minim.
- **`res/Data/npcs.json`** — toți NPC-urile. Fiecare are `id`, `name`, `type`, `zone`, `col`, `row` (poziția în tile-uri),
  opțional `sprite` și `tint` (culoare `#RRGGBB`). Tipuri: `simple` (replici, `lineMode`: `cycle`/`random`), `dialogue`
  (noduri cu 2-3 opțiuni), `merchant` (dialog + magazin), `quest` (dialog + misiune). Explicații complete în câmpul `_help` al fișierului.

**NPC nou:** copiază o intrare din `npcs.json`, schimbă `id`, `name`, `zone`, `col`, `row` și replicile. Alege un tile liber
(fără copac/casă) și nu pune doi NPC în același loc. Nu scrii nicio clasă (vezi `DataNPC`).

**Carte nouă în magazin:** adaugă numele din `CardFactory` (ex. `"UraganDeAzur"`) în `shop.stock` al negustorului, iar prețul în
`cardPrices`. Prețul de vânzare se calculează singur.

Monedele se salvează în baza de date (coloana `saves.coins`). Magazinul: A/D schimbă fila, W/S aleg, ENTER cumpără/vinde, ESC iese.

## Misiuni, nivele și abilități (date, nu cod)

- **`res/Data/quests.json`** — misiunile (id, titlu, descriere, `giver`/`turnIn` = id-ul unui NPC din `npcs.json`, obiective
  `kill` / `collect` / `talk`, recompense: monede, XP, o carte) și obiectele de cules din lume (`pickups`). Stări: nepornită →
  activă → îndeplinită → predată. NPC-ul cu o misiune de oferit are `!` deasupra capului, cel la care o predai `?` (galben);
  `?` gri = în desfășurare. Jurnalul (**J**, tasta **Q** schimbă fila) are fila „Misiuni”.
- **`res/Data/progression.json`** — XP-ul necesar pe nivel, ce primești la fiecare nivel (viață maximă, +1 esență la nivelurile 5 și 9),
  XP-ul fiecărui inamic și **abilitățile**. Abilitățile se deblochează automat la nivelul lor și se folosesc în luptă cu **1-4**
  (costă esență, ca cărțile).

**Misiune nouă:** adaugă un obiect în `quests` (vezi `_help` în fișier). Pentru obiectivul `collect`, adaugă și obiectele în `pickups`.
**Abilitate nouă:** adaugă un obiect în `abilities` din `progression.json` (`unlockLevel`, `cost`, `effects`: `damage`, `shield`, `heal`,
`mana`, `status`). Slotul (tasta) urmează ordinea nivelului de deblocare.

## Rune (date, nu cod)

- **`res/Data/runes.json`** — `slots` (câte rune porți odată, 3), `bagSize` (câte poți deține) și lista `runes`: `id`, `name`, `description`,
  `glyph` (`flame`/`shield`/`heart`/`drop`/`fang`, desenat în cod), `color`, `rarity`, `price`, `buffs` și `drops` (șansa de a pica de la un inamic).
  Efecte (`buffs`): `damage`, `maxLife`, `turnShield` (armură la începutul fiecărei ture), `battleHeal` (viață după fiecare luptă câștigată).
- **Inventar (I):** A/D aleg o rună (tooltip cu efecte și descriere), ENTER o echipează sau o dezechipează. Runele echipate se salvează.
- **Magazin:** adaugă `"rune:<id>"` în `shop.stock` al unui negustor; runele se pot și vinde (preț * `sellRatio`).

**Rună nouă:** adaugă un obiect în `runes` din `runes.json` (id unic). Pentru drop: `"drops": { "Lup": 0.1 }`; pentru magazin: `"rune:<id>"` în `npcs.json`.
**Efect nou:** adaugă cheia în `Rune.buffText()` și aplic-o într-un loc din luptă cu `player.runeSum("cheie")`.

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
