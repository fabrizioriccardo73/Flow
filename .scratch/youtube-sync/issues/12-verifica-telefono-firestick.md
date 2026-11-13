# 12: Verifica completa su telefono e Firestick

**What to build:** una prova reale con il mio account YouTube, il telefono con il fork (YouTube Sync Device) e la Firestick con Flow originale, che dimostra che tutto il giro funziona e che la Firestick non contatta mai Google.

**Blocked by:** 05 (Protezione dalle rimozioni di massa), 07 (Gestione degli errori), 10 (Collegare una playlist Flow-only), 11 (Scollegamento completo)

**Status:** ready-for-human

- [ ] Primo collegamento reale con anteprima corretta su iscrizioni e playlist
- [ ] Iscrizione fatta sulla Firestick → arriva al telefono via Device Sync → arriva su YouTube
- [ ] Iscrizione e modifica di una Owned Playlist fatte nell'app ufficiale di YouTube → arrivano al telefono e poi alla Firestick
- [ ] La Firestick non effettua richieste verso le API di Google (nessun Linked Account)
- [ ] Riproduzione e navigazione sul telefono funzionano come prima e non usano il Linked Account
- [ ] Notifica "Ricollega l'account" verificata revocando l'accesso dalla pagina dell'account Google
- [ ] Scollegamento e ricollegamento verificati
- [ ] Problemi trovati registrati come nuovi ticket in `.scratch/youtube-sync/issues/`

## Comments

- 2026-11-13: tutti i ticket da cui dipende (05, 07, 10, 11) sono implementati e coperti da test JVM (92 test del fork, tutti verdi). Questa verifica richiede l'account Google reale, il telefono, la Firestick e la revoca dell'accesso dalla pagina dell'account Google: va fatta a mano (ready-for-human). Nessun dispositivo era collegato via adb.
- Test dell'intera app: 5.899 test. Falliscono solo `ProfileInitialTest` e `MusicBrainRankerTest`, che falliscono uguali anche su Flow originale senza modifiche (commit c03f90070, verificato in un worktree pulito). Non dipendono dal fork.
- APK da installare: `app/build/outputs/apk/foss/debug/app-foss-universal-debug.apk`.
