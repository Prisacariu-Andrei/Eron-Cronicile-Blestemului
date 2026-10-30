# ERON: Cronicile Blestemului

Un RPG turn-based cu sistem de cărți (deckbuilder), scris în Java (Swing). Joci ca Eron, un pelerin
cu flacăra de demult în palme, trimis de Malakor, regele iubit al Eldoriei, să adune Artefactele
Blestemului din 7 zone: Eldoria, Bârlogul, Abisul, Citadela, Tundra, Podul de Oase și Castelul de Obsidian.
Povestea completă (cu dezvăluirea de la final): `docs/POVESTE.md`.

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

| Tastă | Unde | Ce face |
| --- | --- | --- |
| W A S D / săgeți | hartă | mers (și pe diagonală) |
| Space | hartă | dash |
| E | hartă | vorbește cu un NPC |
| F | hartă | deschide un cufăr / folosește poarta Castelului |
| M | hartă | harta ținutului (zona curentă, zone deschise / închise) |
| I | hartă | inventar (rune: A/D aleg, ENTER echipează) |
| J | hartă | jurnalul de misiuni (Q schimbă fila) |
| T | hartă, pauză | setări: volum, scuturat ecran, viteza textului (se salvează) |
| ESC | hartă | pauză |
| 1–8, A / D | luptă | alege cartea din mână |
| ENTER | luptă | joacă cartea aleasă |
| Q W E R | luptă | abilitățile (se deblochează la nivelurile 2, 3, 5, 7; `progression.json`) |
| săgeată sus / jos, TAB | luptă | schimbă ținta |
| T | luptă | termină tura |
| ENTER / SPACE, SPACE ținut, ESC ținut | scene | mai departe, text mai rapid, sari peste scenă |
| F11 / Alt+Enter | oriunde | ecran complet |
| F3 | oriunde | panou de performanță (FPS, durate, memorie) |
| F9 | hartă, mod de test | „Sari la zonă” și panoul de admin |

Cufere: `res/Data/chests.json` (monede, rună și/sau carte; zona, conținutul); un cufăr deschis rămâne deschis după salvare. Pozițiile pe hartă (M) și zonele deschise: `zones.json` (`map`, `requires`).

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
- **`res/Data/progression.json`** — XP-ul necesar pe nivel, ce primești la fiecare nivel (viață maximă; esența rămâne 3, `manaBonusAtLevels` e gol),
  XP-ul fiecărui inamic și **abilitățile**. Abilitățile se deblochează automat la nivelul lor și se folosesc în luptă cu **Q W E R**
  (nu costă esență; au timp de reîncărcare sau se folosesc o dată pe luptă).

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

## Lanțul zonelor 1-4 (artefacte și Gărzi)

- **`res/Data/gates.json`** — ce boss dă ce flag (`bosses`), artefactele-cheie (`artifacts`, apar în inventar la „Obiecte cheie”) și
  cele patru pasaje din Eldoria (`gates`): tile-ul de teleportare, flag-ul necesar (`requires`), unde stă Garda de Fier cât timp pasajul
  e închis (`guard`), unde se retrage după (`aside`) și cele două replici (`closedText` / `openText`).
- Ordinea: Fiara din Eldoria → Munții (Fenris → **Colțul de Chihlimbar**) → Abisul (Feldgug → **Diamantul Nopții**) →
  Citadela (Jean → **Paloșul Legendar**) → Tundra (Glaciara → **Inelul de Gheață**). Fiecare Gardă blochează pasajul până cade boss-ul dinainte.
- Flag-urile se salvează (`saves.story_flags`); salvările vechi le deduc din relicvele deja primite.

**Pasaj/boss nou:** adaugă boss-ul la `bosses`, artefactul la `artifacts` și un obiect la `gates` (poziția Gărzii: o căsuță liberă din fața tile-ului).

## Aliați și Jean Pleșa

- **`res/Data/allies.json`** — `party` (șansa ca o lovitură a inamicului să ajungă la aliat, cât se reface după luptă, câți aliați pot merge odată) și
  lista `allies`: `id`, `name`, `sprite`, `color`, `maxLife`, `recruitFrom` (inamicul care, învins, îl recrutează), `scene` (replicile scenei de
  recrutare) și `abilities` (în ordinea priorității; prima cu condiția `when` adevărată se folosește: `always`, `selfLifeBelow:0.4`,
  `playerLifeBelow:0.5`, `playerShieldBelow:6`; efecte: `damage` (+status), `shieldPlayer`, `shieldSelf`, `healPlayer`, `healSelf`).
- În luptă aliatul acționează când Eron își termină tura (înainte de inamic), apare lângă Eron cu bară de viață și ultima abilitate,
  poate fi lovit și poate cădea (se ridică la 50% după luptă). Viața lui se salvează (`saves.party`).
- **Jean Pleșa** (Citadela): lovește de două ori și îți aplică *Vulnerabilitate* (+50% damage primit, 2 ture) — e o cursă de damage. După victorie:
  ecran de recompensă → scenă scurtă → Jean se alătură, iar Paloșul Legendar (obiect cheie) deschide drumul spre Tundra.

**Aliat nou:** adaugă un obiect în `allies` din `allies.json` (inamicul din `recruitFrom` trebuie să existe în `EnemyFactory`).

## Inamici în grup, tranziții între zone

- **`res/Data/enemies.json`** — `groups` (cu cât sunt mai mulți inamici în luptă, cu atât lovesc mai slab: 1 → 100%, 2 → 80%, 3 → 70%, 4 → 60% din damage;
  la 4 au și mai puțină viață), `types` (inamici noi: rol `brute` / `healer` / `mage` / `guard`, sprite + culoare, viață, damage), `spawns` (câte grupuri apar în
  fiecare zonă, pe dale libere, la fel la fiecare încărcare) și `safe` (zone fără inamici, în jurul intrărilor). Grupul se alege din `pack` ([min,max] în total) și `with` (membri în plus, ex. un șaman).
- În luptă: **săgeată sus/jos sau TAB** schimbă ținta. Inamicii de sprijin vindecă sau protejează aliații; aliații lui Eron pot proteja/vindeca pe cel mai slab din echipă.
  Bossii luptă singuri; **Glaciara** e amețită după 5 lovituri primite și invocă a doua rundă un Minion de Zăpadă (60 viață, 15 damage).
- **Valerius** nu mai atacă: vorbește cu el (E), iar dacă ai nivelul 7 poți alege duelul. Runele boss-ilor (Platoșa Amintirii, Runa lui Fenris) nu se pot vinde.
- **`res/Data/zones.json`** — titlul și subtitlul din ecranul de tranziție (imagine din hartă + bară de încărcare, `loadingTicks`) și locul unde apare Eron în Eldoria
  după ce a învins boss-ul unei zone. Boss-ii înfrânți nu mai reapar, iar zona rămâne accesibilă.

**Inamic nou:** adaugă un obiect în `types` din `enemies.json` și, dacă vrei să apară pe hartă, în `spawns`.

## Finalul: Castelul de Obsidian

- **`res/Data/castle.json`** — poarta (`gate`), cei trei Pretorieni în ordine (`pretorians`: unde stau, echipa, bannerul) și cele trei faze ale lui Malakor (`malakor`).
- Drumul: Garda de la pasul spre Pod te lasă după Glaciara → Podul → Castelul. **Poarta de Obsidian** din coridor se dărâmă doar cu **Paloșul Legendar** (E în fața ei).
- Pretorienii apar pe rând, fiecare după ce a căzut cel dinainte: **I** Garda de Obsidian (doi soldați), **II** Generalii de Obsidian (Fenris, Feldgug, Glaciara, recreați din piatră), **III** Bătrânul Cronicar.
- **Malakor**: *Faza I, Egida* — un scut (Pecetea de Obsidian) care absoarbe daunele și se reface puțin; loviturile lui te Slăbesc (−25% damage). *Faza II, Furia* — scutul a crăpat; Fragilitate: primești daune duble.
  *Faza III, Eclipsa* — atacul lui crește cu fiecare tură. Fiecare fază are banner cu text, flash și scuturat.
- După victorie: **epilogul** (cutscene peste Eldoria, același sistem ca prologul; `CutsceneUI.Script.EPILOGUE`), apoi genericul. Dalele lipsă ale Castelului au o variantă provizorie până apar desenele reale.

## Mod de test

Modul de test e **pornit implicit** (`Main/DebugMode.java`). Se oprește cu opțiunea JVM `-Deron.debug=false`
(IntelliJ: Run → Edit Configurations → Modify options → Add VM options) sau cu variabila `ERON_DEBUG=0`.
Apoi, în joc, **F9** deschide „Sari la zonă”: alegi Eldoria, Munți, Abis, Citadela, Tundra, Pod sau Castel (tastele **1–7** sau **W/S + ENTER**) și primești progresul
necesar (boss-ii dinainte, artefactele, Jean, nivelul) și ești teleportat. Doar adaugă progres, nu scoate nimic. Fără opțiune, F9 nu face nimic.

## Sinergii între cărți (combo-uri)

- **`res/Data/cards.json`** — elementele (`tags`: Foc, Gheață, Fulger, Umbră), numele statusurilor, `reactions`, bonusul de lanț (`chain`), etichetele cărților din cod (`overrides`)
  și **14 cărți noi** scrise doar în date (`cards`: efecte, `combo.when`, `shop`, `drop`). Prețurile sunt în `economy.json`.
- **Reacții** (element + status pe țintă, consumă statusul): Foc pe **Înghețat** = *Topire*; Fulger pe **Ud/Înghețat** = *Șoc în Lanț* (lovește tot grupul); Gheață pe **Ars** = *Șoc Termic*;
  Umbră pe **Încărcat** = *Eclipsă Încărcată* (+ Slăbit). **Lanț:** a 2-a, a 3-a... carte cu același element în tură = *Combo x2, x3* (+damage, sau +armură la cărțile defensive).
  **Pregătire/răsplată** (ex. Execuție Umbrită după o carte Umbră) și **sincron cu aliații** (Jean acționează din nou, bonus de paguba) vin din `combo.when`.
- Pe cărți se văd elementele și condiția de combo (aurie când e îndeplinită acum, cu contur auriu în mână); inamicii au chip-uri de status cu turele rămase; la combo apare numele + scuturat + particule.

**Reacție nouă:** un obiect în `reactions` (element, statusuri cerute, damage, aoeDamage, apply). **Carte nouă:** un obiect în `cards` (apare automat în salvări, recompense și, dacă are `shop`, o treci în `npcs.json` și îi pui prețul în `economy.json`).

## Note de joc (ultimele modificări)

- **Esența** rămâne 3 la orice nivel (`manaBonusAtLevels` gol în `progression.json`), ca alegerea cărților să conteze.
- **Geamătul Ecoului** rejoacă ultima carte prin motorul de combo (reacții, lanț, `Combo xN`), nu doar valorile de bază.
- **Moartea inamicilor** are animație (pâlpâie, se scufundă, se stinge) și ecranul de recompensă apare după ~1,3 secunde (`GameFeel.WIN_DELAY_TICKS`).
- **Ecranele de încărcare** folosesc imaginile din `res/Loading` (zone + prolog + epilog; definite în `zones.json`). `res/Background` conține capturi de ecran cu interfața
  inclusă, deci nu se pot folosi ca fundal de luptă; când ai fundaluri curate, pune calea în `zones.json` (`"battle": "/Background/....png"`) și se vor folosi automat.

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

## Povestea în date

- **`res/Data/story.json`** — scenele: prologul, scenele de după fiecare boss (`after_boss:<flag>`), intrarea în Castel (`enter_zone:Castelul`),
  mărturisirile Pretorienilor, dezvăluirea Cronicarului și sala tronului, finalul (`finale`, după Malakor) și epilogul. Portretele vorbitorilor sunt în `portraits`.
- **`res/Data/boss_intro.json`** — dialogul de dinaintea fiecărei lupte cu un boss.
- **`res/Data/castle.json`** — `quote` = replica lui Malakor la fiecare fază; `lastPage` = „Ultima pagină” (o dată pe luptă, în faza III: vindecare și atacul revine la bază).

## Unelte

- `python tools/check_data.py` — verifică toate fișierele din `res/Data`: JSON valid, declanșatori de scene, portrete, diacritice (ș ț, nu ş ţ),
  lungimea replicilor, imagini lipsă, inamici/cărți/NPC-uri inexistente. Cod de ieșire 1 dacă găsește erori.
- `python tools/art/castle_tiles.py res/Tiles/Castelul` — regenerează dalele Castelului de Obsidian.
- `tools/sim/Sim.java` — simulatorul de balans folosit pentru `docs/BALANS.md` (un bot joacă luptele cu bossii pe motorul jocului).

## Pentru prezentare

1. Rulează `python tools/check_data.py` (trebuie să arate 0 erori).
2. Pornește jocul. Modul de test e activ, deci **F9** te duce direct în orice zonă, cu progresul necesar.
3. Pentru o demonstrație scurtă a poveștii: F9 → Castel, apoi cei trei Pretorieni, sala tronului, Malakor și finalul.

