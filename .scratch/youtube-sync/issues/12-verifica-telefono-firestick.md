# 12: Verifica completa su telefono e Firestick

**What to build:** una prova reale con il mio account YouTube, il telefono con il fork (YouTube Sync Device) e la Firestick con Flow originale, che dimostra che tutto il giro funziona e che la Firestick non contatta mai Google.

**Blocked by:** 05 (Protezione dalle rimozioni di massa), 07 (Gestione degli errori), 10 (Collegare una playlist Flow-only), 11 (Scollegamento completo)

**Status:** ready-for-human

- [ ] Primo collegamento reale con anteprima corretta su iscrizioni e playlist
- [ ] Iscrizione fatta sulla Firestick → arriva al telefono via Device Sync → arriva su YouTube
- [ ] Iscrizione e modifica di una Owned Playlist fatte nell'app ufficiale di YouTube → arrivano al telefono e poi alla Firestick
- [ ] La Firestick non effettua richieste verso le API di Google (nessun Linked Account)
- [ ] Riproduzione e navigazione sul telefono funzionano come prima e non usano il Linked Account
- [ ] Notifica "Ricollega l'account" verificata revocando l'accesso dalla pagina dell'account Google
- [ ] Scollegamento e ricollegamento verificati
- [ ] Problemi trovati registrati come nuovi ticket in `.scratch/youtube-sync/issues/`
