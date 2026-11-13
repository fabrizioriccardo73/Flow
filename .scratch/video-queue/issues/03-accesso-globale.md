# 03: Accesso alla Coda da tutta l'app

**What to build:** apro la Coda anche quando non sta suonando niente, da un'icona con il numero di video nella barra in alto delle schermate principali, e dal mini player.

**Blocked by:** 02 (Sheet della Coda)

**Status:** ready-for-agent

- [x] Icona Coda con badge del conteggio nella barra in alto delle schermate principali (Home, Iscrizioni, Libreria, Ricerca)
- [x] L'icona è visibile anche con la Coda vuota; il badge solo quando ci sono video
- [x] Dal mini player si apre lo stesso sheet
- [x] Lo sheet si apre anche senza nulla in riproduzione e senza Contesto (la sezione "Dopo, da" si nasconde)
- [x] Con la Coda piena e nulla in riproduzione lo sheet mostra "Riproduci", che avvia la Coda dal primo video (o da quello "In riproduzione", dalla posizione salvata)
- [x] L'azione "Riproduci" dello snackbar del ticket 01 fa la stessa cosa

## Comments

**2026-11-13**: Implementato, non ancora provato sul telefono.
- L'icona passa dalle azioni globali della barra in alto (`FlowGlobalActions`): così compare su ogni schermata principale, accanto a Notifiche e Impostazioni. Il badge usa `BadgedBox` di Material 3.
- Lo stato "sheet aperto" e il conteggio vivono in `coda/ui/CodaViewModel`, legato all'activity. `FlowPlayerOverlays` mostra lo stesso `PlayerQueueSheetHost` del mini player.
- Senza riproduzione, "Riproduci" e il tocco su un video della Coda passano da `VideoPlayerViewModel.playVideo`, che apre il player e riprende dalla posizione salvata.
- Con due azioni specifiche della schermata più le tre globali, la barra in alto potrebbe risultare affollata sui telefoni stretti: da controllare.
