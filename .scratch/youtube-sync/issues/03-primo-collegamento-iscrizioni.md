# 03: Primo collegamento delle iscrizioni

**What to build:** il primo percorso completo della YouTube Sync. Dopo aver collegato l'account, premo "Sincronizza ora": Flow mi mostra un'anteprima dell'unione delle iscrizioni ("+N su YouTube, +M su Flow") e, dopo la conferma, le due liste coincidono senza che nulla sia stato cancellato. Viene salvata la Sync Baseline.

**Blocked by:** 02 (Collegare e scollegare il Linked Account)

**Status:** ready-for-agent

- [x] Esiste l'interfaccia `YouTubeAccount` con le operazioni sulle iscrizioni (elenco paginato, aggiunta, rimozione) ed errori tipizzati, più una versione in memoria per i test
- [x] Adattatore reale sulla YouTube Data API v3 per le iscrizioni
- [x] La YouTube Sync legge e scrive le iscrizioni di Flow tramite la stessa interfaccia di accesso ai dati usata dalla Device Sync
- [x] Senza Sync Baseline il piano è un'unione: nessuna rimozione da nessuna parte; un'iscrizione presente su entrambi i lati non viene duplicata
- [x] L'anteprima mostra i conteggi per lato; annullando non viene applicato nulla
- [x] Dopo l'applicazione la Sync Baseline viene salvata solo su questo dispositivo, per profilo, fuori dai dati della Device Sync
- [x] Pulsante "Sincronizza ora" nelle impostazioni del profilo
- [x] Test sul seam principale (archivi di Flow + `YouTubeAccount` in memoria + Sync Baseline) che coprono: unione, nessuna rimozione, nessun duplicato, annullamento

## Comments

**2026-11-13**: Implementato, non ancora provato sul telefono.
- `remote/`: `YouTubeAccount` con gli errori tipizzati e `YouTubeDataApiAccount`, l'adattatore verso la YouTube Data API. Legge tutte le pagine. Un'iscrizione duplicata o una rimozione di un elemento già assente contano come successo.
- `local/LocalSubscriptions`: passa da `SubscriptionRepository`, lo stesso repository su cui lavora la Device Sync tramite `SyncDataAccess`. Così le iscrizioni arrivate da YouTube viaggiano da sole verso la Firestick.
- `engine/YouTubeSync`: `plan()` legge e non scrive nulla; `apply()` scrive prima in Flow e poi su YouTube, un canale alla volta. La Sync Baseline registra solo i canali presenti su entrambi i lati. Un canale rifiutato viene saltato e ritentato; quota esaurita, autorizzazione non valida o rete assente fermano la sincronizzazione.
- La Sync Baseline viene azzerata a ogni nuovo collegamento e allo scollegamento, così non sopravvive mai all'account con cui è stata creata.
- Finché non arriva il ticket 04, anche le sincronizzazioni successive sono solo unioni: nessuna rimozione.
- Le iscrizioni ad artisti di YouTube Music in Flow sono iscrizioni a canali, quindi vanno su YouTube come le altre.
- Test: 8 sul seam principale (`FirstLinkSubscriptionsTest`) e 5 sull'adattatore (`YouTubeDataApiAccountTest`), tutti verdi.
