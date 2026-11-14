# 09: Schermata dell'Organizzazione assistita

**What to build:** dalla gestione dei Gruppi l'utente sceglie i canali, vede cosa parte, avvia l'analisi e accetta o rifiuta le Proposte.

**Blocked by:** 08

**Status:** ready-for-agent

- [ ] Azione nella gestione dei Gruppi (riga `// fork: AI` in `SubscriptionGroupDialogs.kt`), visibile solo se la AI è disponibile; solo telefono
- [ ] Scelta dei canali: preselezionati quelli Non raggruppati o senza Lingua del canale; selezione libera
- [ ] Schermata di conferma prima dell'invio: numero di canali, dati inviati per canale, nomi dei Gruppi (ADR 0005)
- [ ] Avanzamento per lotti, annullabile; errori per lotto visibili con "Riprova" su quel lotto
- [ ] Lista delle Proposte per canale con accetta/rifiuta e "accetta tutte"
- [ ] Stringhe in `src/fork/res` (it + en)
- [ ] Verifica manuale su un profilo con molte iscrizioni
