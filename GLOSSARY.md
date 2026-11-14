# Flow (fork)

Fork personale di Flow, client Android di YouTube, che aggiunge la sincronizzazione con un account YouTube accanto alla sincronizzazione già esistente tra i dispositivi.

## Sincronizzazioni

**YouTube Sync**:
La sincronizzazione bidirezionale di iscrizioni e playlist tra un profilo Flow e il suo Linked Account.
_Avoid_: sync, sincronizzazione (senza qualificatore), account sync

**Device Sync**:
La sincronizzazione cifrata, già esistente in Flow, tra i dispositivi dello stesso utente via rete locale o WebDAV ("sync2" nel codice).
_Avoid_: sync, sync2, sincronizzazione (senza qualificatore)

**Sync Baseline**:
L'ultimo stato concordato tra Flow e YouTube, usato per capire quale delle due parti ha cambiato cosa.
_Avoid_: snapshot, ultimo stato, stato precedente

**YouTube Sync Device**:
L'unico dispositivo di un gruppo di Device Sync che esegue la YouTube Sync e conserva la Sync Baseline; gli altri dispositivi ricevono i cambiamenti tramite la Device Sync. Non è mai una TV.
_Avoid_: dispositivo principale, master

## Account e accesso

**Linked Account**:
L'account YouTube collegato a un profilo Flow ai fini della YouTube Sync.
_Avoid_: login, account Google, account collegato

**Canale ufficiale**:
L'accesso al Linked Account tramite l'API ufficiale di YouTube autorizzata con OAuth.
_Avoid_: API, OAuth (come nome del concetto)

**Canale cookie**:
L'accesso al Linked Account tramite i cookie di sessione e l'API interna di YouTube; previsto ma non ancora adottato.
_Avoid_: login InnerTube, cookie login

## Dati

**Owned Playlist**:
Una playlist YouTube creata dall'utente del Linked Account; si sincronizza in entrambe le direzioni.
_Avoid_: playlist YouTube, playlist mia

**Saved Playlist**:
Una playlist di un altro utente YouTube salvata dal Linked Account; in Flow è in sola lettura.
_Avoid_: playlist esterna, playlist seguita

**Flow-only**:
Un dato che non lascia mai l'ecosistema Flow (per esempio gruppi di iscrizioni, preferiti, note): viaggia con la Device Sync ma non con la YouTube Sync.
_Avoid_: locale, solo locale

**Device-local**:
Un dato che non lascia mai il dispositivo su cui nasce: non viaggia né con la Device Sync né con la YouTube Sync (per esempio la Coda).
_Avoid_: locale, solo locale, Flow-only

**Coda di invio**:
Le modifiche locali della YouTube Sync in attesa di essere inviate a YouTube.
_Avoid_: coda (senza qualificatore), queue

## Iscrizioni

**Gruppo**:
Un insieme di canali seguiti, scelto dall'utente, che filtra la sezione Iscrizioni tramite il suo chip; un canale può stare in più Gruppi. Un Gruppo può avere dei Sottogruppi, e allora il suo chip mostra i canali propri più quelli di tutti i suoi Sottogruppi. È Flow-only.
_Avoid_: categoria, cartella, etichetta

**Sottogruppo**:
Un Gruppo che appartiene a un solo altro Gruppo, il suo padre; non può avere Sottogruppi a sua volta. Il suo chip mostra solo i suoi canali.
_Avoid_: gruppo figlio, sottocartella, sottocategoria

**Non raggruppati**:
Il chip della sezione Iscrizioni che mostra solo i video dei canali che non appartengono a nessun gruppo; il chip "Tutti" mostra invece sempre tutto.
_Avoid_: Tutti senza gruppi, ungrouped

**Lingua del canale**:
La lingua che l'utente assegna a mano a un canale; un canale ne ha una oppure nessuna. È Flow-only.
_Avoid_: lingua (senza qualificatore), locale

**Lingua del video**:
La lingua di un video ai fini del Filtro lingua: la Lingua del canale se c'è, altrimenti quella rilevata dal video stesso; può restare sconosciuta.
_Avoid_: lingua audio, lingua rilevata (come nome del concetto)

**Filtro lingua**:
La scelta, nella sezione Iscrizioni, delle lingue i cui video mostrare; si somma al chip selezionato e vale per tutto ciò che la sezione mostra. I video con Lingua del video sconosciuta passano sempre. È Device-local.
_Avoid_: filtro (senza qualificatore), lingua preferita

## Riproduzione video

**Coda**:
La lista ordinata di video che l'utente ha scelto di vedere a breve; parte quando il Contesto di riproduzione è esaurito, e un video ne esce quando finisce, viene saltato o viene segnato come visto (non quando viene interrotto). È Device-local e ce n'è una per profilo. Non riguarda la musica né gli Shorts.
_Avoid_: queue, playlist, coda di riproduzione, Guarda più tardi

**Contesto di riproduzione**:
Ciò che l'utente ha avviato esplicitamente: una playlist oppure un singolo video. Ha la precedenza sulla Coda.
_Avoid_: coda, queue, playlist in corso

**Autoplay**:
I video suggeriti che il player riproduce quando sia il Contesto di riproduzione sia la Coda sono esauriti.
_Avoid_: coda, prossimi, consigliati
