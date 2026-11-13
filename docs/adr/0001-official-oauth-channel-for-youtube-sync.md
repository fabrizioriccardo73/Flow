# YouTube Sync usa il canale ufficiale (OAuth), separato dalla riproduzione

Il progetto Flow originale esclude qualsiasi login Google per motivi di privacy e per il rischio di ban degli account. Questo fork, ad uso personale, aggiunge comunque la YouTube Sync di iscrizioni e Owned Playlist tramite l'API ufficiale di YouTube (YouTube Data API v3) autorizzata con OAuth. È la via sanzionata da Google, quindi praticamente senza rischio di ban. Il token serve **solo** alla YouTube Sync: riproduzione e navigazione restano anonime come nel progetto originale, così l'account Google non viene mai legato all'estrazione non ufficiale dei flussi.

Il progetto Google Cloud è personale e pubblicato in "Production" senza verifica. Si accetta la schermata di avviso, in cambio di refresh token che non scadono, a differenza dei 7 giorni della modalità "Testing".

## Considered Options

- **Canale cookie (InnerTube con cookie di sessione)**: dà accesso anche a cronologia e "Guarda più tardi", ma viola i Termini di Servizio e espone l'intero account Google a blocchi o ban. Rimandato: verrà valutato solo quando l'ambito si estenderà a dati che l'API ufficiale non espone.
- **Solo importazione da Google Takeout**: esiste già nel progetto originale, ma è manuale e una tantum. Non soddisfa l'obiettivo di un aggiornamento automatico bidirezionale.

## Consequences

- Cronologia, "Guarda più tardi" e video piaciuti restano fuori dalla YouTube Sync.
- Le scritture sono vincolate dalla quota giornaliera dell'API (10.000 unità; circa 200 scritture al giorno).
- Questa funzione non potrà essere proposta al progetto originale.
