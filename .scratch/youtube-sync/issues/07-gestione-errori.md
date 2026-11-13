# 07: Gestione degli errori

**What to build:** la YouTube Sync si comporta bene quando qualcosa va storto: con la quota esaurita aspetta il giorno dopo senza disturbarmi, con l'autorizzazione non valida si ferma e mi chiede di ricollegare l'account, e un elemento problematico non blocca gli altri.

**Blocked by:** 06 (Coda delle modifiche e tempistiche)

**Status:** ready-for-agent

- [ ] Quota esaurita: le modifiche restano in coda e si riprova dopo il reset giornaliero della quota, senza notifiche
- [ ] Autorizzazione revocata o scaduta: la YouTube Sync si ferma, compare la notifica "Ricollega l'account", l'indicatore di stato lo riflette; ricollegando riparte da dove era
- [ ] Errore su un singolo elemento (non trovato, duplicato): gli altri elementi procedono; i duplicati sono trattati come successo
- [ ] Errori transitori: nuovo tentativo con attesa crescente
- [ ] Test dell'adattatore contro un finto server HTTP che riproduce le risposte di Google: paginazione, quota esaurita, autorizzazione revocata, duplicato, non trovato, errore transitorio → tipi di errore di `YouTubeAccount`
- [ ] Test sul seam principale per ciascun tipo di errore
