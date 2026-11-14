# 12: Verifica completa su telefono e Firestick

**What to build:** una prova reale con il mio account YouTube, il telefono con il fork (YouTube Sync Device) e la Firestick con il fork senza Linked Account, che dimostra che tutto il giro funziona e che la Firestick non contatta mai Google.

**Blocked by:** 05 (Protezione dalle rimozioni di massa), 07 (Gestione degli errori), 10 (Collegare una playlist Flow-only), 11 (Scollegamento completo)

**Status:** ready-for-human

- [x] Primo collegamento reale con anteprima corretta su iscrizioni e playlist
- [ ] Iscrizione fatta sulla Firestick → arriva al telefono via Device Sync → arriva su YouTube
- [ ] Iscrizione e modifica di una Owned Playlist fatte nell'app ufficiale di YouTube → arrivano al telefono e poi alla Firestick
- [ ] Modifica di una Owned Playlist fatta in Flow → arriva su YouTube (ticket 09)
- [ ] "Collega una playlist a YouTube" su una playlist Flow-only → nuova playlist privata su YouTube con gli stessi video e lo stesso ordine (ticket 10)
- [ ] Una Saved Playlist in Flow è in sola lettura (ticket 08) — non verificabile con il Canale ufficiale: la YouTube Data API non espone le playlist salvate (spec, Further Notes)
- [x] La Firestick non effettua richieste verso le API di Google: nella pagina YouTube Sync della Firestick "Collega account YouTube" è disattivato con il messaggio "Collega l'account dal telefono…" e nessun account è collegato
- [ ] Riproduzione e navigazione sul telefono funzionano come prima e non usano il Linked Account
- [ ] Notifica "Ricollega l'account" verificata revocando l'accesso dalla pagina dell'account Google; ricollegando lo stesso account si riparte da dove ci si era fermati
- [ ] Scollegamento e ricollegamento verificati (ultimo passo; dopo il ricollegamento cancellare a mano le copie Flow-only, vedi ticket 13)
- [ ] Problemi trovati registrati come nuovi ticket in `.scratch/youtube-sync/issues/`

## Comments

- 2026-11-13: tutti i ticket da cui dipende (05, 07, 10, 11) sono implementati e coperti da test JVM (92 test del fork, tutti verdi). Questa verifica richiede l'account Google reale, il telefono, la Firestick e la revoca dell'accesso dalla pagina dell'account Google: va fatta a mano (ready-for-human). Nessun dispositivo era collegato via adb.
- Test dell'intera app: 5.899 test. Falliscono solo `ProfileInitialTest` e `MusicBrainRankerTest`, che falliscono uguali anche su Flow originale senza modifiche (commit c03f90070, verificato in un worktree pulito). Non dipendono dal fork.
- APK da installare: `app/build/outputs/apk/foss/debug/app-foss-universal-debug.apk`.
- 2026-11-13: piano della prova concordato. I dati arrivano nel fork con il backup completo di Flow originale, poi il fork si accoppia con la Firestick via Device Sync, e solo dopo si collega l'account. Flow originale sul telefono resta nel gruppo ma non si usa durante la prova. La protezione dalle rimozioni di massa (05) non si prova sull'account reale: è coperta dai test JVM. Lo scollegamento è l'ultimo passo.
- 2026-11-13: cambio di piano. Sulla Firestick non c'è Flow: si installa il fork (APK `armeabi-v7a` o `universal`). Il gruppo di Device Sync si crea da zero sul fork del telefono, via rete Wi-Fi, e la Firestick ci entra. Il fork ora blocca il collegamento dell'account su una TV (ADR 0002, `ThisDevice`, test in `LinkedAccountManagerTest`). Non c'è più un Flow originale nel gruppo.
- 2026-11-13: primo collegamento fatto sul telefono, anteprima corretta e confermata.
