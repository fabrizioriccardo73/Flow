# 03: Primo collegamento delle iscrizioni

**What to build:** il primo percorso completo della YouTube Sync. Dopo aver collegato l'account, premo "Sincronizza ora": Flow mi mostra un'anteprima dell'unione delle iscrizioni ("+N su YouTube, +M su Flow") e, dopo la conferma, le due liste coincidono senza che nulla sia stato cancellato. Viene salvata la Sync Baseline.

**Blocked by:** 02 (Collegare e scollegare il Linked Account)

**Status:** ready-for-agent

- [ ] Esiste l'interfaccia `YouTubeAccount` con le operazioni sulle iscrizioni (elenco paginato, aggiunta, rimozione) ed errori tipizzati, più una versione in memoria per i test
- [ ] Adattatore reale sulla YouTube Data API v3 per le iscrizioni
- [ ] La YouTube Sync legge e scrive le iscrizioni di Flow tramite la stessa interfaccia di accesso ai dati usata dalla Device Sync
- [ ] Senza Sync Baseline il piano è un'unione: nessuna rimozione da nessuna parte; un'iscrizione presente su entrambi i lati non viene duplicata
- [ ] L'anteprima mostra i conteggi per lato; annullando non viene applicato nulla
- [ ] Dopo l'applicazione la Sync Baseline viene salvata solo su questo dispositivo, per profilo, fuori dai dati della Device Sync
- [ ] Pulsante "Sincronizza ora" nelle impostazioni del profilo
- [ ] Test sul seam principale (archivi di Flow + `YouTubeAccount` in memoria + Sync Baseline) che coprono: unione, nessuna rimozione, nessun duplicato, annullamento
