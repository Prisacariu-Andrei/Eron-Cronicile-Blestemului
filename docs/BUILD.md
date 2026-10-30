# Build și publicare (Windows)

- **Construire:** din rădăcina proiectului rulează `powershell -ExecutionPolicy Bypass -File tools\build.ps1`.
  Rezultatul e `dist\Eron\` cu `Eron.exe` și Java inclus; nu trebuie instalat nimic pe calculatorul jucătorului.
  Scriptul are nevoie de un JDK complet (`javac`, `jar`, `jpackage`) găsit prin `JAVA_HOME`, `PATH` sau `%USERPROFILE%\.jdks`.
  Cu `-Smoke` pornește și jocul 5 secunde ca test.
- **Versiunea:** se schimbă în fișierul `VERSION` (ex. `0.1.1`) înainte de build; ajunge în `Eron.exe` și pe itch.io.
- **Date jucător:** salvările, setările și logul se scriu în `%APPDATA%\Eron` (nu lângă joc). La prima pornire, un `eron_save.db`
  sau `eron_settings.properties` din folderul de lucru se copiază acolo o singură dată.
- **Publicare:** instalează `butler`, rulează o dată `butler login`, apoi `powershell -ExecutionPolicy Bypass -File tools\publish.ps1`
  (trimite `dist\Eron` la `andreiaig/eron-chronicles:windows`). Pentru o actualizare: crești `VERSION`, rulezi `build.ps1`, apoi `publish.ps1`.
