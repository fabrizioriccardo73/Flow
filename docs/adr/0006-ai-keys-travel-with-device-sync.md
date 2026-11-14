# Le Chiavi API viaggiano con la Device Sync in una collezione del fork

Le Chiavi API, il Provider attivo e il modello scelto viaggiano con la Device Sync in una collezione propria del fork (`fork_ai_keys`, stesso schema dell'ADR 0004), così la TV può offrire Riassunto video e Riassunto commenti senza che l'utente debba digitare una chiave col telecomando. L'alternativa, chiavi Device-local, era più prudente ma costringeva a inserire la chiave su ogni dispositivo.

## Consequences

- Una chiave è un segreto: sui dispositivi con il fork è salvata sigillata con il Keystore, mai in chiaro nelle preferenze.
- Un dispositivo con Flow originale nello stesso gruppo riceve il pacchetto, lo ignora, ma lo include nel suo backup di sicurezza pre-merge: la chiave esiste quindi, cifrata dalla Device Sync, anche lì.
- Cancellare una chiave su un dispositivo la cancella su tutti; se due dispositivi cambiano provider prima di sincronizzarsi vince l'ultima modifica.
- Le chiavi si inseriscono solo dal telefono.
