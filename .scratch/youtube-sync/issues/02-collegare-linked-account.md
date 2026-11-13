# 02: Collegare e scollegare il Linked Account

**What to build:** dalle impostazioni del profilo posso collegare il mio account YouTube tramite il canale ufficiale (OAuth con AppAuth nel browser) e vederne il nome; posso scollegarlo, revocando il permesso. Il Linked Account è per profilo e non viene mai usato da riproduzione e navigazione.

**Blocked by:** 01 (Fork e build)

**Status:** ready-for-agent

- [ ] Login con AppAuth nel browser, senza Google Play Services, verso un progetto Google Cloud personale (client ID configurabile, non committato in chiaro se possibile)
- [ ] Permessi richiesti: solo quelli minimi per gestire iscrizioni e playlist
- [ ] Token salvati cifrati, per profilo; il refresh del token funziona senza intervento
- [ ] Le impostazioni del profilo mostrano il nome/canale del Linked Account oppure il pulsante "Collega account YouTube"
- [ ] Profili diversi possono avere Linked Account diversi o nessuno
- [ ] Il client HTTP del canale ufficiale è separato da quello di riproduzione e navigazione: nessun cookie o header condiviso (verificato da test)
- [ ] Scollegare revoca il permesso presso Google ed elimina i token locali
- [ ] La voce non compare / non fa nulla su un dispositivo senza Linked Account
