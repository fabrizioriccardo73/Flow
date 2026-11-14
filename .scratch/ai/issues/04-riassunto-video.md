# 04: Riassunto video sul telefono

**What to build:** l'azione "Riassunto" nel foglio della descrizione del video produce i punti chiave con i momenti cliccabili, dalla trascrizione, nella lingua dell'app, anche per video molto lunghi, con una cache Device-local.

**Blocked by:** 02

**Status:** ready-for-agent

- [ ] Riga `// fork: AI` in `PlayerDescriptionSheetHost` che mostra l'azione solo se la AI è disponibile; disattivata con motivo se il video non ha trascrizione
- [ ] Trascrizione da `TranscriptRepository` con la traccia scelta dal player; cue nel prompt come `[mm:ss] testo`
- [ ] Prompt e risposta JSON (titolo del punto, momento, una o due frasi); momenti validati contro la durata
- [ ] Divisione in parti se la trascrizione supera il budget del modello: riassunto per parte in sequenza, poi unione
- [ ] Foglio del riassunto: punti con momento cliccabile (salto del player), provider e modello sotto, "Rigenera"
- [ ] Cache Device-local per profilo, per (video, tipo, lingua dell'app), con limite di voci; riusata dal ticket 06
- [ ] Errori: limite (429) con suggerimento di riprovare o cambiare provider, chiave non valida, risposta non valida con "Riprova"
- [ ] Test JVM: divisione in parti e unione, validazione dei momenti, parsing del JSON (anche rotto), cache
- [ ] Verifica manuale con i tre provider su un video breve, uno di oltre 2 ore e uno senza trascrizione
