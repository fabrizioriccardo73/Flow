# 01: Coda persistente e ordine Contesto → Coda → Autoplay

**What to build:** "Riproduci dopo" e "Aggiungi alla Coda" mettono i video in una Coda che nessuna azione implicita cancella e che sopravvive al riavvio. Il player riproduce prima il Contesto di riproduzione, poi la Coda, poi l'Autoplay. Per ora la Coda si vede con il dock e lo sheet esistenti.

**Blocked by:** nessuno

**Status:** ready-for-agent

- [x] La Coda è un modulo separato dal Contesto di riproduzione; toccare un video o avviare una playlist cambia solo il Contesto
- [x] Ordine: Contesto, poi Coda, poi Autoplay; l'Autoplay non parte con la Coda non vuota
- [x] Toccare un video crea un Contesto di un solo video; finito quello parte la Coda
- [x] "Riproduci dopo" mette il video in cima alla Coda e lo riproduce subito dopo quello in corso, anche dentro una playlist, che poi riprende dal punto giusto
- [x] "Aggiungi alla Coda" lo mette in fondo
- [x] Senza nulla in riproduzione, aggiungere non avvia la riproduzione; compare lo snackbar "Aggiunto alla Coda · Riproduci"
- [x] Nessun duplicato: "Riproduci dopo" sposta in cima un video già presente, "Aggiungi alla Coda" mostra "Già in Coda"
- [x] Un video della Coda esce quando finisce, quando si preme "Successivo" su di lui o quando viene segnato come visto
- [ ] Interrotto, resta in cima come "In riproduzione" e al suo turno riparte dalla posizione salvata in cronologia (vedi commento: in background riparte da capo)
- [x] Shuffle e loop agiscono solo sul Contesto
- [x] Persistenza per profilo (`StoreScope.PROFILE_PRIVATE`), anche con un solo video, non registrata nella Device Sync
- [x] `VideoQueueStore` resta per la ripresa del Contesto da "Continua a guardare"; la vecchia coda non viene migrata
- [x] Con un `PlaybackTarget` remoto le azioni vanno al dispositivo remoto e la Coda locale non cambia
- [x] Le azioni di Coda non compaiono sugli Shorts
- [x] Test JVM sul seam Coda + "prossimo video" con gli scenari della spec, più il round trip della persistenza

## Comments

**2026-11-13**: Implementato, non ancora provato sul telefono.
- Il codice nuovo vive nel fork, `app/src/fork/java/io/github/aedev/flow/coda/`: `Coda` (stato e regole), `upNext` (Contesto → Coda → Autoplay), `DockNextUp`, `CodaCodec` e `CodaStore` (DataStore `video_coda`, `PROFILE_PRIVATE`), `VideoCoda` (singleton Hilt, salva a ogni modifica). Nei file originali ci sono solo agganci marcati `// fork: Coda`, soprattutto in `EnhancedPlayerManager`.
- Il Contesto resta `PlaybackQueueController`. Quando si passa alla Coda, il Contesto finito viene svuotato e il video della Coda parte come un video toccato. "Riproduci dopo" inserisce il video anche nel Contesto, subito dopo quello in corso: così passa davanti alla playlist una sola volta e la playlist riprende da sola.
- Un video della Coda esce quando arriva alla fine (`emitPlaybackCompletion`), quando la finestra gapless lo supera, quando si preme Successivo o lo si salta, e con "Segna come visto" o la chiusura di "Continua a guardare". Diventa "In riproduzione" quando parte, da qualsiasi percorso, osservando `GlobalPlayerState.currentVideo`.
- Una playlist in loop non finisce mai, quindi la Coda aspetta che il loop venga tolto. È coerente con la regola "prima tutto il Contesto".
- **Limite aperto**: un video interrotto che riparte dalla Coda riprende dalla posizione salvata quando il player è aperto (lo gestisce `PlaybackPreparer`). Se invece l'avanzamento avviene in background, dal service layer, riparte da capo: quel percorso non legge la cronologia di visione.
- Dock e sheet: il dock compare anche con la sola Coda e mostra il prossimo video nell'ordine reale. Nello sheet, sotto il Contesto, c'è una sezione "Coda" provvisoria: un tocco fa partire il video, ma non ci sono riordino né rimozione. Lo sheet vero è il ticket 02.
- "Aggiungi alla Coda" non crea più un Contesto [video corrente, nuovo] al volo. `PlaybackQueueController.append` resta, ma non è più usato dal player.
- Test: `CodaTest`, `UpNextTest`, `DockNextUpTest` in `app/src/forkTest`, verdi insieme ai test esistenti di coda, dock e quick actions.
