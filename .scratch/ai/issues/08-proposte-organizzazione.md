# 08: Proposte dell'Organizzazione assistita

**What to build:** la logica che, per un lotto di canali scelti, chiede alla AI le Proposte di Gruppo/Sottogruppo esistente, nuovo Gruppo e Lingua del canale, le valida e le applica quando l'utente le accetta.

**Blocked by:** 02

**Status:** ready-for-agent

- [ ] Input per canale: nome, descrizione, ultimi ~5 titoli; più l'albero dei Gruppi esistenti (nomi e Sottogruppi)
- [ ] Lotti di dimensione fissa, in sequenza; l'errore di un lotto non fa perdere gli altri
- [ ] Risposta JSON validata: Gruppi e Sottogruppi esistenti per nome esatto, nuovo Gruppo (mai nuovo Sottogruppo), Lingua del canale tra quelle supportate; Proposte non valide scartate
- [ ] Proposta di lingua senza AI quando la lingua rilevata dai video del canale è già univoca
- [ ] Applicazione con le stesse operazioni della gestione manuale (Gruppi, Sottogruppi, `ChannelLanguages`); un nuovo Gruppo proposto per più canali si crea una volta sola
- [ ] Test JVM: costruzione dell'input, parsing e validazione, lotti con errore, applicazione (anche nuovo Gruppo condiviso)
