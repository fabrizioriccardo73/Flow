# La Lingua del canale viaggia in una collezione della Device Sync propria del fork

La Lingua del canale è un dato Flow-only e deve arrivare anche sulla TV, quindi viaggia con la Device Sync. Non la aggiungiamo come campo a un dato che esiste già (gruppi o iscrizioni). La mettiamo invece in una collezione nuova, propria del fork (`fork_channel_languages`). Flow originale (Firestick, desktop) può ricevere i pacchetti di questa collezione, ma non sa cosa farne e li ignora. Quindi non può né alterarli né cancellare le lingue.

## Considered options

- **Un campo nuovo nel gruppo o nell'iscrizione.** È la strada più ovvia, ma è stata scartata. Flow originale legge la Device Sync con `ignoreUnknownKeys` e scarta in silenzio i campi che non conosce. Se l'utente modifica quel record su un dispositivo con Flow originale, il record torna con un timestamp più recente e senza il campo. Al merge vince lui, e la Lingua del canale sparisce anche sui dispositivi con il fork.
- **Nessuna sincronizzazione, lingua Device-local.** È stata scartata: la TV non avrebbe le lingue, e sulla TV non si possono assegnare.

## Consequences

- I dispositivi con Flow originale non vedono mai le lingue né il Filtro lingua.
- Quando un dispositivo con Flow originale riceve un pacchetto di questa collezione, fa il suo backup di sicurezza prima del merge, anche se poi la ignora (`SyncApplier` lo fa per ogni collezione fuori da Room). Succede solo quando qualcuno cambia una lingua, quindi di rado.
- Se due dispositivi assegnano lingue diverse allo stesso canale prima di sincronizzarsi, vince l'ultima modifica, come per i gruppi.
- Una Lingua del canale resta salvata anche quando l'utente si disiscrive dal canale. Se si riscrive, la ritrova.
- Registrare la collezione richiede alcune righe `// fork` nel codice originale della Device Sync: l'elenco delle collezioni in `Frames.kt`, la chiave dei record in `ChangeBatches.kt`, l'esportazione e l'applicazione in `SyncApplier.kt`. Sono punti da ricontrollare a ogni aggiornamento da Flow originale.
- La vecchia sincronizzazione FLOW-SYNC/1 (il selettore delle collezioni nella schermata di sync) non la offre: il fork usa la Device Sync.
- Il prefisso `fork_` evita collisioni con collezioni che Flow originale potrebbe aggiungere in futuro.
- Lo stesso schema vale per il legame tra Sottogruppo e padre, nella collezione `fork_group_parents` (`.scratch/sottogruppi/spec.md`): i Gruppi restano quelli di Flow originale, e i dispositivi con Flow originale vedono i Sottogruppi come Gruppi normali.
