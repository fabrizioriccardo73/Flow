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
L'unico dispositivo di un gruppo di Device Sync che esegue la YouTube Sync e conserva la Sync Baseline; gli altri dispositivi ricevono i cambiamenti tramite la Device Sync.
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
