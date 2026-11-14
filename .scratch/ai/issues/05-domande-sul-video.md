# 05: Domande sul video sul telefono

**What to build:** dal foglio del Riassunto video l'utente fa domande sul video e riceve risposte con i momenti citati, cliccabili; la conversazione non viene conservata.

**Blocked by:** 04

**Status:** ready-for-agent

- [ ] Campo di domanda e conversazione nel foglio del riassunto (solo telefono)
- [ ] Contesto: trascrizione intera se entra nel budget; altrimenti riassunti delle parti più le parti più vicine alla domanda, con segnale "contesto parziale" mostrato all'utente
- [ ] Momenti citati nella risposta come `[mm:ss]`, resi cliccabili e validati contro la durata
- [ ] Conversazione solo nel ViewModel, persa alla chiusura del video
- [ ] Test JVM: scelta del contesto (intero o parziale), estrazione dei momenti dalla risposta
