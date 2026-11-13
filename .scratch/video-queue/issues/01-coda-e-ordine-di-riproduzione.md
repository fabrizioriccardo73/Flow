# 01: Coda persistente e ordine Contesto → Coda → Autoplay

**What to build:** "Riproduci dopo" e "Aggiungi alla Coda" mettono i video in una Coda che nessuna azione implicita cancella e che sopravvive al riavvio. Il player riproduce prima il Contesto di riproduzione, poi la Coda, poi l'Autoplay. Per ora la Coda si vede con il dock e lo sheet esistenti.

**Blocked by:** nessuno

**Status:** ready-for-agent

- [ ] La Coda è un modulo separato dal Contesto di riproduzione; toccare un video o avviare una playlist cambia solo il Contesto
- [ ] Ordine: Contesto, poi Coda, poi Autoplay; l'Autoplay non parte con la Coda non vuota
- [ ] Toccare un video crea un Contesto di un solo video; finito quello parte la Coda
- [ ] "Riproduci dopo" mette il video in cima alla Coda e lo riproduce subito dopo quello in corso, anche dentro una playlist, che poi riprende dal punto giusto
- [ ] "Aggiungi alla Coda" lo mette in fondo
- [ ] Senza nulla in riproduzione, aggiungere non avvia la riproduzione; compare lo snackbar "Aggiunto alla Coda · Riproduci"
- [ ] Nessun duplicato: "Riproduci dopo" sposta in cima un video già presente, "Aggiungi alla Coda" mostra "Già in Coda"
- [ ] Un video della Coda esce quando finisce, quando si preme "Successivo" su di lui o quando viene segnato come visto
- [ ] Interrotto, resta in cima come "In riproduzione" e al suo turno riparte dalla posizione salvata in cronologia
- [ ] Shuffle e loop agiscono solo sul Contesto
- [ ] Persistenza per profilo (`StoreScope.PROFILE_PRIVATE`), anche con un solo video, non registrata nella Device Sync
- [ ] `VideoQueueStore` resta per la ripresa del Contesto da "Continua a guardare"; la vecchia coda non viene migrata
- [ ] Con un `PlaybackTarget` remoto le azioni vanno al dispositivo remoto e la Coda locale non cambia
- [ ] Le azioni di Coda non compaiono sugli Shorts
- [ ] Test JVM sul seam Coda + "prossimo video" con gli scenari della spec, più il round trip della persistenza
