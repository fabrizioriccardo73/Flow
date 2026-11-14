# 03: Lingua del video

**What to build:** ogni video della sezione Iscrizioni ha una Lingua del video: quella del canale se c'è, altrimenti rilevata offline da titolo e descrizione, oppure sconosciuta.

**Blocked by:** 02

**Status:** ready-for-agent

- [x] Scegliere una libreria di rilevamento offline compatibile con FOSS (niente ML Kit); valutare peso dell'APK e precisione su titoli brevi
- [x] Funzione pura `(lingua del canale?, titolo, descrizione) → lingua | sconosciuta`, con soglia di confidenza
- [x] Risultato memorizzato per `videoId`
- [x] Test JVM sulla risoluzione (canale vince, rilevata, sconosciuta sotto soglia)

## Comments

- needs-triage finché non è scelta la libreria di rilevamento.

**2026-11-14**: Implementato, non ancora provato sul telefono.
- Scelta: nessuna libreria. `LanguageDetector` usa i sistemi di scrittura (coreano, giapponese, cinese, arabo, hindi) e, per il latino, parole frequenti proprie di una sola lingua (it, en, es, fr, de, pt, nl) più qualche lettera tipica. Serve un punteggio di almeno 2 e il doppio della seconda lingua, altrimenti la lingua resta sconosciuta. Della descrizione contano i primi 400 caratteri. Il russo si può assegnare al canale ma non si rileva.
- `VideoLanguages`: lingua del canale, altrimenti rilevata, con la rilevazione memorizzata per `videoId`.
- Test: `LanguageDetectorTest`, `LanguageFilterTest`.
- Se in uso la precisione risulta scarsa, si può valutare una libreria (Lingua, Optimaize) contro il peso dell'APK.
