# 02: Collegare e scollegare il Linked Account

**What to build:** dalle impostazioni del profilo posso collegare il mio account YouTube tramite il canale ufficiale (OAuth con AppAuth nel browser) e vederne il nome; posso scollegarlo, revocando il permesso. Il Linked Account è per profilo e non viene mai usato da riproduzione e navigazione.

**Blocked by:** 01 (Fork e build)

**Status:** ready-for-agent

- [x] Login con AppAuth nel browser, senza Google Play Services, verso un progetto Google Cloud personale (client ID configurabile, non committato in chiaro se possibile)
- [x] Permessi richiesti: solo quelli minimi per gestire iscrizioni e playlist
- [x] Token salvati cifrati, per profilo; il refresh del token funziona senza intervento
- [x] Le impostazioni del profilo mostrano il nome/canale del Linked Account oppure il pulsante "Collega account YouTube"
- [ ] Profili diversi possono avere Linked Account diversi o nessuno
- [x] Il client HTTP del canale ufficiale è separato da quello di riproduzione e navigazione: nessun cookie o header condiviso (verificato da test)
- [x] Scollegare revoca il permesso presso Google ed elimina i token locali
- [ ] La voce non compare / non fa nulla su un dispositivo senza Linked Account

## Comments

**2026-11-13**: Implementazione completa, non ancora provata su un telefono.
- Codice in `app/src/fork/java/io/github/aedev/flow/youtubesync/`:
  - `account/`: `LinkedAccountManager`, che è il seam; `LinkedAccountStore`, un DataStore `youtube_sync_account` in PROFILE_PRIVATE con l'auth state cifrato da `KeystoreSecretBox`; `AppAuthGateway`; `GoogleYouTubeIdentityApi`.
  - `net/`: il client `@YouTubeSyncHttp`.
  - `ui/`: la pagina "YouTube Sync", raggiungibile da Integrazioni.
- Test in `app/src/forkTest/java/`: 8 per il Linked Account e 3 per il client HTTP, tutti verdi.
- Ambito OAuth: solo `https://www.googleapis.com/auth/youtube`. Il nome mostrato è quello del canale (`channels.list?mine=true`).
- ID client e schema di redirect vanno messi in `local.properties`. Lo schema di default è l'ID dell'app della build, `.debug` incluso. La guida è in `FORK.md`.
- Da verificare a mano: il login reale (serve il progetto Google Cloud), la riga con il nome del canale, profili diversi con account diversi, la revoca su myaccount.google.com.
- Criterio "la voce non compare su un dispositivo senza Linked Account": la riga "Collega" compare ovunque, perché è il modo per collegare l'account. La Firestick usa Flow originale, quindi lì la voce non esiste. Da riformulare.
- Lo scambio del codice e il refresh dei token passano dal client HTTP di AppAuth (HttpURLConnection), che non segue l'impostazione proxy di Flow. Non condivide cookie con la riproduzione.
- Due test del codice originale falliscono in locale (JDK 23): `ProfileInitialTest` e `MusicBrainRankerTest`. Riguardano file non toccati dal fork. Non ho verificato se falliscono anche sul commit originale.

**2026-11-13**: Provato sul telefono con la build di debug e il client Google in modalità "Test": il collegamento funziona e la pagina mostra il canale. La pubblicazione dell'app Google "in produzione" non è riuscita, quindi per ora il refresh token scade dopo 7 giorni.
