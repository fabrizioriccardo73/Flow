# Spec: Sottogruppi

Status: ready-for-agent

Vedi anche: `GLOSSARY.md` (Gruppo, Sottogruppo, Non raggruppati, Filtro lingua, Flow-only), ADR 0004.

## Problem Statement

Nella sezione Iscrizioni filtro i video per Gruppo, ma i Gruppi sono piatti. Se voglio sia "Tech" sia "Android" e "AI", devo creare tre Gruppi allo stesso livello: la fila dei chip si allunga, e "Tech" non include i canali di "Android" e "AI". Non posso filtrare per un argomento ampio e poi restringere a uno più specifico.

## Solution

- **Sottogruppo**: un Gruppo può stare dentro un altro Gruppo, il suo padre. C'è un solo livello e ogni Sottogruppo ha un solo padre. Il padre resta un Gruppo normale e può avere canali propri.
- **Chip**: la fila principale mostra solo i Gruppi senza padre. Quando seleziono un padre, sotto compare una seconda fila con i suoi Sottogruppi. Nessun Sottogruppo selezionato vuol dire tutto il padre, cioè i suoi canali più quelli dei Sottogruppi. Toccando un Sottogruppo restringo ai suoi canali; toccandolo di nuovo torno al padre. Si seleziona un Sottogruppo alla volta.
- **Gestione**: in "Gestisci gruppi" un campo "Dentro a" sceglie il padre. I Sottogruppi compaiono rientrati sotto il padre e si riordinano lì. Funziona allo stesso modo sul telefono e sulla TV.
- **Device Sync**: il legame Sottogruppo → padre viaggia in una collezione del fork, `fork_group_parents` (ADR 0004). Sui dispositivi con Flow originale i Sottogruppi compaiono come Gruppi normali.

## User Stories

### Filtrare

1. Come utente, voglio che il chip di un Gruppo mostri i video dei suoi canali e di quelli di tutti i suoi Sottogruppi.
2. Come utente, voglio che la fila principale mostri solo i Gruppi senza padre, così da tenerla corta.
3. Come utente, voglio che selezionando un padre compaia una seconda fila con i suoi Sottogruppi.
4. Come utente, voglio toccare un Sottogruppo per vedere solo i suoi canali, e toccarlo di nuovo per tornare a tutto il padre.
5. Come utente, voglio che il Filtro lingua e "Accesso rapido" seguano il Sottogruppo selezionato.
6. Come utente, voglio ritrovare il padre e il Sottogruppo selezionati quando riapro l'app.
7. Come utente, voglio che un canale presente solo in un Sottogruppo non compaia in "Non raggruppati".
8. Come utente, voglio che un canale presente sia nel padre sia in un Sottogruppo compaia una volta sola.

### Gestire

9. Come utente, voglio scegliere in "Gestisci gruppi", con il campo "Dentro a", in quale Gruppo mettere un Gruppo.
10. Come utente, voglio che "Dentro a" proponga solo i Gruppi che non sono a loro volta Sottogruppi.
11. Come utente, voglio che un Gruppo che ha già Sottogruppi non possa diventare un Sottogruppo, e che una riga mi dica perché.
12. Come utente, voglio vedere i Sottogruppi rientrati sotto il padre e riordinarli lì.
13. Come utente, voglio che rinominando un padre o un Sottogruppo il legame resti.
14. Come utente, voglio che eliminando un padre i suoi Sottogruppi non vengano eliminati ma tornino Gruppi normali, e che il dialogo di conferma me lo dica.
15. Come utente, voglio che nel foglio dei gruppi del canale i Sottogruppi compaiano rientrati sotto il padre, ognuno con la sua casella.
16. Come utente sulla TV, voglio filtrare per Sottogruppi e anche crearli e gestirli.

### Device Sync

17. Come utente con telefono e Firestick, voglio che i Sottogruppi creati su uno arrivino sull'altro.
18. Come utente con un dispositivo con Flow originale, voglio che la Device Sync continui a funzionare e che i miei Sottogruppi non vadano persi; lì li vedo come Gruppi normali.

## Implementation Decisions

- Il codice nuovo vive in `app/src/fork/` (package da decidere, per esempio `io.github.aedev.flow.subgroups`). Nei file originali solo agganci minimi marcati `// fork` che delegano al fork, come per i Filtri iscrizioni.
- **Memoria**: per profilo, un record per Sottogruppo: nome del Sottogruppo → nome del padre, con un HLC e un tombstone per quando il legame si toglie. Mai un campo aggiunto a `SubscriptionGroupEntity` o a `CanonicalSubscriptionGroup` (ADR 0004).
- **Legami validi**: una funzione pura `(Gruppi, legami) → albero`. Un legame vale solo se sia il Sottogruppo sia il padre esistono, sono Gruppi diversi e il padre non ha a sua volta un legame verso un Gruppo esistente (così anche un ciclo si scioglie in Gruppi normali). I legami non validi si ignorano in lettura, senza correggere nessun dato. Questa regola copre i merge concorrenti che creerebbero due livelli e il padre eliminato su un altro dispositivo.
- **Scope**: lo scope del chip di un padre è l'unione dei suoi canali con quelli dei suoi Sottogruppi validi. Lo scope di un Sottogruppo è solo il suo. Lo scope si calcola nel punto dove oggi lo calcola `groupSelectionScope`, tramite un aggancio `// fork`. "Non raggruppati" non cambia: un Sottogruppo è un Gruppo.
- **Ordine**: si riusa il `sortOrder` dei Gruppi che c'è già. I Sottogruppi di un padre seguono il loro ordine relativo, e quando un Sottogruppo si sgancia ricompare nella fila principale nel punto del suo `sortOrder`. Riordinare dentro un padre riscrive il `sortOrder`, e così l'ordine resta sensato anche su Flow originale.
- **Rinomina**: oggi rinominare un Gruppo vuol dire cancellarlo e crearlo di nuovo (il nome è la chiave). Un aggancio del fork sulla rinomina mette un tombstone sul record del vecchio nome, ne crea uno con il nuovo e riscrive il padre nei record dei Sottogruppi.
- **Eliminazione del padre**: si mettono i tombstone sui legami dei suoi Sottogruppi. I Sottogruppi restano Gruppi.
- **Selezione**: si salva accanto a quella che c'è già, che ricorda il Gruppo selezionato; il Sottogruppo selezionato si aggiunge con le stesse regole di persistenza. Se il Sottogruppo salvato non è più valido, si torna a selezionare quel Gruppo come Gruppo normale, se esiste.
- **Device Sync**: collezione `fork_group_parents`, registrata come `fork_channel_languages` (righe `// fork` in `Frames.kt`, `ChangeBatches.kt`, `SyncApplier.kt`). Merge per record, vince l'HLC più recente, con i tombstone.
- **TV**: la TV usa già `SubscriptionGroupDialogsHost`, quindi il campo "Dentro a" arriva con lo stesso codice. La seconda fila di chip si raggiunge con il D-pad.

## Testing Decisions

- Test JVM in `app/src/forkTest` su: costruzione dell'albero dai legami (compresi i legami non validi, i due livelli dopo un merge, il padre mancante), lo scope del padre e del Sottogruppo, "Non raggruppati" con i Sottogruppi, la rinomina (del padre e del Sottogruppo), l'eliminazione del padre, il merge di `fork_group_parents` (compreso un peer che non dichiara la collezione) e il round trip della persistenza.
- Verifica manuale sul telefono e sulla Firestick con il fork, e con un dispositivo con Flow originale, che la Device Sync resti compatibile.

## Out of Scope

- Più di un livello di Sottogruppi.
- Un Sottogruppo con più padri.
- Selezionare più Sottogruppi insieme.
- Mandare i Gruppi o i Sottogruppi a YouTube con la YouTube Sync: restano Flow-only.
