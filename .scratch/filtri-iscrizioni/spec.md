# Spec: Filtri della sezione Iscrizioni

Status: ready-for-agent

Vedi anche: `GLOSSARY.md` (Non raggruppati, Lingua del canale, Lingua del video, Filtro lingua, Flow-only, Device-local), ADR 0004.

## Problem Statement

Nella sezione Iscrizioni filtro i video per gruppo, ma:

- non trovo un modo, dalla sezione, per vedere solo i video dei canali che non ho messo in nessun gruppo. Il chip "Non raggruppati" esiste, ma è spento di default e si accende solo da Impostazioni → Contenuti, dove non sapevo di cercarlo;
- non posso filtrare per lingua, per esempio "mostra solo i video in italiano".

## Solution

- **Non raggruppati dalla sezione**: in "Gestisci gruppi" c'è un interruttore per mostrare o nascondere il chip "Non raggruppati". È la stessa impostazione di Impostazioni → Contenuti. "Tutti" continua a mostrare tutto.
- **Lingua del canale**: assegno a mano una lingua, o nessuna, a ogni canale, dal foglio dei gruppi del canale. Il dato è Flow-only, viaggia con la Device Sync in una collezione propria del fork (ADR 0004) e resta salvato anche se mi disiscrivo.
- **Lingua del video**: è la Lingua del canale, se c'è. Altrimenti viene rilevata sul dispositivo, senza rete, da titolo e descrizione. Se la rilevazione non è sicura, la lingua resta sconosciuta.
- **Filtro lingua**: un chip con il globo in testa alla fila dei chip. Mostra le lingue attive ("IT", "IT+EN") e apre una scelta multipla tra le lingue presenti nelle mie iscrizioni. Si somma al chip del gruppo e vale per video, Shorts, dirette e programmati. I video con Lingua del video sconosciuta passano sempre. La selezione resta salvata per profilo, solo su questo dispositivo (Device-local).
- **TV**: sulla TV c'è il Filtro lingua; le lingue si assegnano dal telefono.

## User Stories

### Non raggruppati

1. Come utente, voglio attivare o disattivare il chip "Non raggruppati" da "Gestisci gruppi", così da non dover andare nelle impostazioni.
2. Come utente, voglio che quell'interruttore e quello in Impostazioni → Contenuti siano la stessa cosa, così da non trovare stati diversi nei due posti.
3. Come utente, voglio che "Tutti" mostri sempre tutti i video.

### Lingua del canale

4. Come utente, voglio assegnare una lingua a un canale dal foglio dove lo metto nei gruppi.
5. Come utente, voglio poter togliere la lingua a un canale, così che i suoi video vengano rilevati uno per uno (canali in più lingue).
6. Come utente con telefono e TV, voglio che le lingue assegnate dal telefono arrivino sulla TV con la Device Sync.
7. Come utente con una Firestick con Flow originale, voglio che la Device Sync continui a funzionare come oggi e che le mie lingue non vadano perse.
8. Come utente che si disiscrive e poi si riscrive a un canale, voglio ritrovare la lingua che gli avevo dato.

### Filtro lingua

9. Come utente, voglio un chip in testa alla fila che mi mostra le lingue attive e, se lo tocco, mi fa scegliere una o più lingue.
10. Come utente, voglio scegliere solo tra le lingue che compaiono davvero nelle mie iscrizioni.
11. Come utente, voglio che il Filtro lingua si sommi al gruppo selezionato (per esempio "Tech" + italiano).
12. Come utente, voglio che il filtro valga anche per Shorts, dirette e programmati.
13. Come utente, voglio che i video di cui la lingua non si capisce restino visibili, così da non perdere niente.
14. Come utente, voglio che il filtro non tocchi gli avatar di "Accesso rapido".
15. Come utente, voglio ritrovare il filtro dopo aver chiuso l'app, per profilo, e che non cambi sugli altri dispositivi.
16. Come utente sulla TV, voglio lo stesso Filtro lingua nella sezione Iscrizioni.

## Implementation Decisions

- Il codice nuovo vive in `app/src/fork/` (package da decidere, per esempio `io.github.aedev.flow.subsfilters`). Nei file originali solo agganci marcati `// fork` che delegano al fork, come per la Coda (memoria: modifiche solo nel fork; aggiornamenti da Flow originale ancora attivi).
- **Non raggruppati**: niente stato nuovo. L'interruttore in "Gestisci gruppi" scrive la stessa preferenza che legge `SubscriptionsViewModel` (oggi `ungroupedEnabled`).
- **Lingua del canale**: memoria per profilo, da `channelId` a codice lingua ISO 639-1, con un timestamp HLC per voce e tombstone per la rimozione. Le voci di canali non più seguiti restano.
- **Device Sync**: collezione `fork_channel_languages`, dichiarata `produce`/`consume` nelle capacità solo dal fork. Merge per voce, vince l'HLC più recente. Mai un campo aggiunto a collezioni esistenti (ADR 0004).
- **Lingua del video**: funzione pura `(lingua del canale?, titolo, descrizione) → lingua | sconosciuta`. La rilevazione usa una libreria offline compatibile con FOSS (niente ML Kit né servizi Google), limitata a un insieme di lingue, con una soglia di confidenza sotto la quale il risultato è "sconosciuta". Il risultato si memorizza per `videoId`, così non si ricalcola a ogni lettura.
- **Filtro lingua**: un predicato puro `(Lingua del video, lingue selezionate) → mostra?`. Nessuna selezione oppure lingua sconosciuta → mostra. Si applica dopo lo scope del gruppo, a tutte le liste della sezione. Selezione salvata in `StoreScope.PROFILE_PRIVATE`, non registrata nella Device Sync.
- Le lingue proposte nel chip sono l'unione delle Lingue del video dei contenuti attualmente caricati, più le Lingue del canale assegnate.

## Testing Decisions

- Test JVM in `app/src/forkTest` su: risoluzione della Lingua del video, predicato del Filtro lingua combinato con lo scope del gruppo, merge della collezione `fork_channel_languages` (incluso un peer che non la dichiara), round trip della persistenza.
- Verifica manuale sul telefono e sulla Firestick (con il fork e con Flow originale) che la Device Sync resti compatibile.

## Out of Scope

- Assegnare la Lingua del canale dalla TV.
- Leggere la lingua da YouTube (API ufficiale o metadati audio).
- Filtrare per lingua in sezioni diverse da Iscrizioni (Home, Ricerca).
- Mandare la Lingua del canale a YouTube con la YouTube Sync.
