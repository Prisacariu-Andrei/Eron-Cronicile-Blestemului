# Raport de balans (octombrie 2026)

Nicio valoare din joc nu a fost schimbată de acest raport. Propunerile de mai jos se aplică doar după ce le aprobi.

## Cum s-a calculat

Cifrele vin dintr-un simulator (`tools/sim/Sim.java`) care rulează **motorul real al jocului**: cărțile, combo-urile și reacțiile din `cards.json`, statusurile, abilitățile Q/W/E/R, runele, aliatul Jean și AI-ul fiecărui boss. Diferența față de joc: tura inamicului se rulează imediat, fără pauzele animațiilor.

- **Cine joacă:** un bot simplu. În fiecare tură folosește întâi abilitățile disponibile. Joacă o carte de armură doar dacă intenția inamicului e de cel puțin 12. Altfel caută combinația de cărți cu cea mai mare pagubă pentru 3 esență.
- **Cum se citește:** botul nu ține cont de eschiva lui Jean și nu plănuiește combo-urile. Un om atent câștigă mai des. Cifrele arată deci **dificultatea relativă**, nu procentul exact pe care îl va avea un jucător.
- **Pachetul:** cele 10 cărți de start, plus 3/6/9/12/14/16 cărți luate la întâmplare din cele deblocate la nivelul respectiv (cam o carte pe luptă câștigată). Căderea Stelelor e scoasă, pentru că costă 4 și nu se poate juca cu 3 esență.
- **Nivelul și viața lui Eron:** cele din modul de test (F9). Nivelul nu dă viață (`hpPerLevel` = 0), deci Eron are 100 HP, iar din Abis 110–120 HP, cu Diamantul și runele.
- **Mărimea testului:** 300 de lupte pe boss, cu seed fix, deci rezultatele se pot reproduce.

## Situația actuală

| Boss | Nivel | HP Eron | Cărți în plus | Victorii bot | Ture (medie) | HP rămas Eron | HP boss | Pagubă/tură |
|---|---|---|---|---|---|---|---|---|
| Fenris cel Alb | 3 | 100 | 3 | 100% | 8,1 | 72 | 150 | 18,5 |
| Feldgug | 5 | 100 | 6 | 99% | 10,4 | 58 | 220 | 21,1 |
| **Jean Pleșa** | 7 | 120 | 9 | **14%** | 7,2 | 42 | 260 | 35,9 |
| **Glaciara** | 8 | 120 | 12 | **37%** | 10,8 | 41 | 400 | 37,1 |
| **Charon** | 9 | 120 | 14 | **32%** | 11,8 | 31 | 500 | 42,5 |
| Pretorianul I (2 soldați) | 10 | 120 | 16 | 100% | 6,0 | 116 | 190 | 31,8 |
| Pretorianul II (3 statui) | 10 | 120 | 16 | 100% | 13,1 | 109 | 380 | 28,9 |
| Pretorianul III (Cronicarul) | 10 | 120 | 16 | 100% | 7,7 | 115 | 280 | 36,2 |
| Malakor (3 faze + Ultima pagină) | 10 | 120 | 16 | 68% | 18,5 | 70 | 1000* | ~54 |

\* 300 HP + 180 scut + 300 + 220. Pagubă/tură = 1000 / 18,5.

**Problema:** după Feldgug dificultatea face un salt de la 99% la 14%. Cauza, din cod:

- **Jean** lovește de două ori, iar prima lovitură te face Vulnerabil (+50%). O tură normală dă deci 12 + 18 = **30**. Sub 35% din HP atacul se dublează la 24 + 36 = **60 într-o singură tură**, adică jumătate din viața lui Eron. Intenția afișată e 24, deci jucătorul nu vede că urmează 60.
- **Glaciara**: 400 HP înseamnă aproape 11 ture, iar sub 30% HP lovește cu 35 la fiecare tură.
- **Charon**: 500 HP, cam 12 ture, iar sub 30% HP dă 40 care trec prin armură.
- **Pretorienii** sunt ușori (Eron termină cu 109–116 din 120 HP). E acceptabil, pentru că sunt lupte de poveste.

Toate aceste valori sunt scrise direct în Java (`src/Entity/Generali/Enemy_*.java`), nu în JSON.

## Propuneri (testate în simulator, 400 de lupte fiecare)

| Boss | Valoare | Vechi → Nou | Victorii bot | Ture | Motiv |
|---|---|---|---|---|---|
| Jean Pleșa | `maxLife` | 260 → **180** | 14% → **79%** | 7,2 → 6,1 | lupta rămâne o „cursă de pagubă”, dar scurtă |
| Jean Pleșa | `attackDamage` | 12 → **9** | (împreună cu rândul de sus) | | tura normală 30 → 22; disperarea 60 → 45 |
| Glaciara | `maxLife` | 400 → **300** | 37% → **80%** | 10,8 → 9,4 | fără 3 ture în plus doar de tocat HP |
| Glaciara | `attackDamage` | 25 → **20** | (împreună cu rândul de sus) | | |
| Charon | `maxLife` | 500 → **380** | 32% → **80%** | 11,8 → 9,7 | |
| Charon | `attackDamage` | 30 → **22** | (împreună cu rândul de sus) | | |
| Feldgug (opțional) | `maxLife` | 220 → 180 | ~99% | 10,4 → ~8,6 (calculat: 180 / 21,1) | lupta e sigură, dar lungă și monotonă |

Alte variante testate, dacă vrei luptele mai grele:

- Jean 200 HP / 10 atac: **54%**. Jean 180 HP / 10 atac: 67%.
- Glaciara 320 HP / 20 atac: 73%. Glaciara 340 HP / 18 atac: 69%.
- Charon 400 HP / 24 atac: 76%. Charon 420 HP / 20 atac: 74%.

**Recomandarea:** valorile îngroșate din tabel. Botul câștigă cam 80%, deci un jucător atent va câștiga 90%+, iar în prezentare nu vei muri. Fenris și Feldgug rămân cum sunt (99–100%), ca jocul să fie ușor la început și să crească treptat.

## Pasul următor, dacă aprobi

Mută `maxLife` și `attackDamage` pentru Jean, Glaciara și Charon în JSON, de exemplu într-o secțiune `bosses` din `res/Data/enemies.json`, și citește-le din `Enemy_*.java`, cu valorile actuale ca rezervă. Abia apoi aplică valorile noi. Atingi 3 fișiere Java și un JSON, cam 15k token-uri.
