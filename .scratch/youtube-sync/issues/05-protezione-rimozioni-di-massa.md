# 05: Protezione dalle rimozioni di massa

**What to build:** se una YouTube Sync rimuoverebbe troppi elementi in una collezione (es. perché Flow ha perso i dati locali), Flow si ferma e mi chiede conferma invece di disiscrivermi da tutto su YouTube.

**Blocked by:** 04 (Sincronizzazione continua delle iscrizioni)

**Status:** ready-for-agent

- [ ] La soglia scatta quando il piano rimuove più del 10% degli elementi di una collezione oppure più di 20 elementi, su uno qualsiasi dei due lati
- [ ] Viene mostrato cosa verrebbe rimosso e da quale lato; posso confermare o rifiutare
- [ ] Rifiutando non viene applicato nulla di quella YouTube Sync e la Sync Baseline resta invariata
- [ ] In background (senza interfaccia) la YouTube Sync si sospende e notifica che serve una conferma
- [ ] La protezione è generica per collezione, così che valga anche per le playlist (ticket 09)
- [ ] Test sul seam principale: scenario "dati locali svuotati con Sync Baseline presente" non tocca YouTube senza conferma
