# Le modifiche in coda e il legame delle playlist vivono nei dati di Flow

La coda delle modifiche locali non è un registro di operazioni. È la differenza tra lo stato attuale di Flow e la Sync Baseline. Quando iscrizioni o playlist cambiano, chiunque le abbia cambiate (l'utente, un'importazione, la Device Sync), la YouTube Sync controlla se Flow si è allontanato dalla Sync Baseline. Se sì, avvia presto una YouTube Sync completa a tre vie.

Così non servono agganci in ogni punto del codice originale che modifica iscrizioni o playlist, e la coda ha gratis le proprietà richieste:
- sopravvive ai riavvii, perché sono i dati stessi;
- si compatta da sola: un'iscrizione seguita da una disiscrizione non lascia differenze;
- include le modifiche arrivate dalla Device Sync.

Il legame tra una playlist di Flow e una di YouTube è l'ID stesso della playlist. Una Owned Playlist o una Saved Playlist arrivata da YouTube usa l'ID YouTube come proprio ID, oppure come sync id dopo un ripristino della Device Sync. Il tipo è quello che Flow già conosce: playlist propria (Owned Playlist) o playlist salvata (Saved Playlist, già in sola lettura nell'interfaccia di Flow). Così il legame viaggia con la Device Sync senza cambiare il suo formato. Collegare una playlist Flow-only o scollegare l'account cambia l'identità della playlist: si crea una copia con il nuovo ID e si elimina la vecchia.

## Considered options

- Una coda di operazioni scritta da agganci nei repository di Flow. È stata scartata: richiede modifiche sparse nel codice originale, non vede le modifiche della Device Sync e va compattata a mano.
- Una tabella di collegamento propria del fork, da ID di Flow a ID YouTube. È stata scartata: non viaggia con la Device Sync, e dopo un ripristino il telefono perderebbe i legami.

## Consequences

- Ogni invio è una YouTube Sync completa, che rilegge anche YouTube. Costa qualche unità di quota in più rispetto a un invio mirato, ma i conflitti si risolvono sempre con le stesse regole.
- Un elemento che YouTube rifiuta resta una differenza: viene ritentato a ogni modifica successiva e a ogni lettura periodica.
- Una modifica fatta mentre una YouTube Sync è in corso parte alla modifica successiva o alla lettura periodica (al massimo 6 ore).
- Collegare o scollegare una playlist appare agli altri dispositivi come un'eliminazione seguita da una creazione.
