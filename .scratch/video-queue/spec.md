# Spec: Coda video

Status: ready-for-agent

Vedi anche: `GLOSSARY.md` (Coda, Contesto di riproduzione, Autoplay, Device-local).

## Problem Statement

In Flow posso mettere in coda i video con "Riproduci dopo" e "Aggiungi alla coda", ma la coda è fragile e scomoda:

- basta toccare un video qualsiasi, o avviare una playlist, per perderla senza preavviso;
- la vedo e la gestisco solo dall'interno del player, mentre un video è in riproduzione;
- non sopravvive in modo affidabile alla chiusura dell'app: si salva solo con almeno due video e torna solo tramite "Continua a guardare", e solo se l'ultimo video coincide.

Il risultato è che non uso la coda per quello che dovrebbe essere: una lista di video da vedere a breve, costruita mentre navigo.

## Solution

Una **Coda** video separata da ciò che sto riproducendo, persistente e richiamabile da ovunque nell'app.

- Il player segue sempre l'ordine **Contesto di riproduzione → Coda → Autoplay**. Il Contesto è ciò che ho avviato esplicitamente (una playlist o un singolo video); la Coda parte quando il Contesto è esaurito; l'Autoplay parte solo quando anche la Coda è vuota.
- Nessuna azione la cancella implicitamente: toccare un video o avviare una playlist cambia solo il Contesto.
- Un video esce dalla Coda quando lo finisco, lo salto o viene segnato come visto; se lo interrompo resta e riparte da dove ero.
- La apro da un'icona con badge nella barra in alto, dal mini player o dal dock del player, anche quando non sta suonando niente.
- È Device-local: una per profilo, solo su questo dispositivo, non viaggia con la Device Sync né con la YouTube Sync.

## User Stories

### Riempire la Coda

1. Come utente, voglio aggiungere un video in fondo alla Coda dal suo menu, così da costruire la lista mentre navigo.
2. Come utente, voglio "Riproduci dopo" per far partire un video subito dopo quello in corso, anche se sto guardando una playlist, così da poter inserire un video al volo.
3. Come utente che usa "Riproduci dopo" durante una playlist, voglio che la playlist riprenda dopo quel video, così da non perdere il punto in cui ero.
4. Come utente che aggiunge un video quando non sta suonando niente, voglio che venga solo aggiunto alla Coda, con un avviso che mi permette di riprodurlo, così da poter preparare la Coda senza avviare nulla.
5. Come utente, voglio che un video già in Coda non compaia due volte: "Riproduci dopo" lo sposta in cima, "Aggiungi alla Coda" mi dice che è già presente.
6. Come utente, voglio che gli Shorts non si possano aggiungere alla Coda, perché vivono nel loro player.

### Ordine di riproduzione

7. Come utente che avvia una playlist con la Coda piena, voglio che la playlist venga riprodotta tutta prima della Coda, così che la playlist resti integra.
8. Come utente che tocca un video con la Coda piena, voglio che il video parta subito e che dopo parta la Coda, intatta.
9. Come utente, voglio che l'Autoplay parta solo quando la Coda è vuota, così che i video suggeriti non scavalchino quelli che ho scelto.
10. Come utente, voglio che shuffle e loop restino proprietà della playlist e non tocchino la Coda.

### Uscita dalla Coda

11. Come utente, voglio che un video esca dalla Coda quando lo finisco, così che la Coda si consumi da sola.
12. Come utente, voglio che premere "Successivo" su un video della Coda lo tolga, perché saltarlo vuol dire che ho finito.
13. Come utente, voglio che un video della Coda segnato come visto ne esca.
14. Come utente che interrompe un video della Coda (toccando un altro video o chiudendo il player), voglio che resti in cima alla Coda come "In riproduzione" e che, al suo turno, riparta da dove ero.
15. Come utente, voglio un'azione rapida "Fatto ✓" nel player sul video della Coda in riproduzione, che lo toglie e passa al successivo.
16. Come utente, voglio "Rimuovi dalla Coda" nel menu di qualsiasi video che è in Coda, al posto di "Aggiungi alla Coda".

### Vedere e gestire la Coda

17. Come utente, voglio aprire la Coda da un'icona con il numero di video nella barra in alto delle schermate principali, anche quando non sta suonando niente.
18. Come utente, voglio aprire la Coda anche dal mini player e dal dock del player.
19. Come utente, voglio vedere nello sheet, dall'alto in basso nell'ordine in cui verranno riprodotti, "Dopo, da: ‹Contesto›" con la playlist in corso e poi la Coda, così da capire cosa verrà riprodotto e in che ordine.
20. Come utente, voglio riordinare la Coda trascinando i video.
21. Come utente, voglio rimuovere un video con uno swipe e poterlo annullare.
22. Come utente, voglio svuotare la Coda con un'azione sola e poterlo annullare.
23. Come utente, voglio toccare un video nella Coda per farlo partire subito: si sposta in cima e resta in Coda finché non lo finisco.
24. Come utente con la Coda piena e nulla in riproduzione, voglio un pulsante "Riproduci" nello sheet che avvia la Coda.

### Persistenza

25. Come utente, voglio ritrovare la Coda dopo aver chiuso l'app o riavviato il telefono, anche se contiene un solo video.
26. Come utente con più profili, voglio una Coda per profilo.
27. Come utente con più dispositivi, voglio che la Coda resti sul dispositivo dove l'ho costruita e non venga sincronizzata.
28. Come utente che riprende una playlist interrotta, voglio continuare a farlo da "Continua a guardare" come oggi.
29. Come utente che comanda un altro dispositivo, voglio che "Riproduci dopo" e "Aggiungi" agiscano sulla coda di quel dispositivo come oggi, senza toccare la mia Coda.

## Implementation Decisions

- **Due concetti separati nel player**: oggi `PlaybackQueueController` contiene un'unica lista che fa da Contesto (playlist avviata o coda improvvisata). Diventa il **Contesto di riproduzione** (lista, indice, titolo, shuffle, loop). La **Coda** è un modulo nuovo, indipendente dal player: una lista ordinata di video con al più un elemento "In riproduzione", con le operazioni aggiungi in fondo, aggiungi in cima, sposta, rimuovi (con annulla), svuota (con annulla), segna come finito.
- **Chi decide il prossimo video**: una funzione pura che, dati Contesto, Coda e disponibilità dell'Autoplay, restituisce il prossimo video e da dove viene (Contesto, Coda, "Riproduci dopo", Autoplay). L'avanzamento automatico, "Successivo", il dock e la sezione "Dopo, da" usano tutti questa funzione. Ordine: un eventuale video "Riproduci dopo" in attesa, poi il resto del Contesto, poi la Coda, poi l'Autoplay.
- **"Riproduci dopo"**: mette il video in cima alla Coda e lo segna come "prossimo" rispetto al video in corso. Dopo di lui il Contesto riprende dal punto in cui era. È l'unico caso in cui un video della Coda passa davanti al Contesto.
- **Contesto per un video singolo**: toccare un video crea un Contesto di un solo elemento. Esaurito quello si passa alla Coda.
- **Fine di un video della Coda**: esce quando raggiunge `STATE_ENDED`, quando si preme "Successivo" o "Fatto ✓" su di lui, o quando viene segnato come visto. Interromperlo non lo rimuove; la posizione di ripresa viene dalla cronologia di visione esistente.
- **Basta azzerare implicitamente**: `takeOverPlayback`/`clearAll` azzerano il Contesto ma non la Coda. Solo le azioni esplicite della Coda la modificano.
- **Persistenza**: un nuovo archivio DataStore `StoreScope.PROFILE_PRIVATE` per la Coda, salvato a ogni modifica, senza soglia minima di elementi. Non è registrato nella Device Sync. `VideoQueueStore` resta per la ripresa del Contesto da "Continua a guardare". Il vecchio contenuto di `video_queue` non viene migrato in Coda.
- **Aggiunta senza riproduzione**: "Aggiungi alla Coda" e "Riproduci dopo" non avviano mai la riproduzione da soli; mostrano uno snackbar con l'azione "Riproduci".
- **Duplicati**: la Coda è un insieme ordinato per ID video.
- **Riproduzione su un altro dispositivo**: con un `PlaybackTarget` remoto attivo le azioni restano instradate verso il dispositivo remoto come oggi, e la Coda locale non cambia.
- **Shorts e musica**: le azioni di Coda non compaiono sugli Shorts. La coda musicale (`QueuePersistence`) non si tocca.
- **Interfaccia**: lo sheet della Coda sostituisce l'attuale `FlowPlaylistQueueBottomSheet` per i video e mostra le sezioni nell'ordine reale: prima il Contesto, con i controlli di oggi (riordino, rimozione, shuffle, loop), poi la Coda. Si apre da tre punti: dock del player, mini player e un'icona con badge nella barra in alto delle schermate principali. Il dock appare anche con un solo video in Coda.
- **Fuori dal primo giro**: l'interfaccia TV (`ui/tv`).

## Testing Decisions

- **Cos'è un buon test**: verifica cosa viene riprodotto dopo e cosa resta in Coda, non la forma interna delle liste.
- **Seam principale**: il modulo Coda più la funzione "prossimo video", testati insieme in JVM puro. Scenari:
  - playlist con Coda piena: prima tutta la playlist, poi la Coda, poi l'Autoplay;
  - video singolo toccato con Coda piena: poi la Coda;
  - "Riproduci dopo" durante una playlist: quel video, poi la playlist riprende dal punto giusto;
  - uscita per fine, "Successivo", "Fatto" e visto; nessuna uscita per interruzione;
  - duplicati, annulla rimozione, annulla svuota;
  - shuffle e loop del Contesto non cambiano la Coda.
- **Persistenza**: un test dell'archivio verifica il round trip, compresa una Coda con un solo video e l'elemento "In riproduzione".
- **Precedenti nel codice**: `PlaybackQueueControllerTest`, `PlaylistQueueOrderTest`, `QueueDockStateTest`, `MusicQueueAdoptionTest`.
- **Verifica manuale sul telefono**: sheet, swipe e trascinamento, badge, snackbar, ripresa dopo il riavvio.

## Out of Scope

- La sincronizzazione della Coda tra dispositivi o con YouTube.
- La coda musicale.
- Gli Shorts.
- L'interfaccia TV.
- Shuffle e loop della Coda.
- La migrazione della vecchia coda salvata.

## Further Notes

- La "coda delle modifiche" del ticket 06 della YouTube Sync è la **Coda di invio**, un concetto diverso; i due lavori sono indipendenti.
