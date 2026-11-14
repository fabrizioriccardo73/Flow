# 06: Riassunto commenti sul telefono

**What to build:** un pulsante "Riassunto" in cima al foglio dei commenti produce opinione generale, punti ricorrenti, critiche e correzioni segnalate, dai ~200 commenti top, con la stessa cache del Riassunto video.

**Blocked by:** 04

**Status:** ready-for-agent

- [ ] Riga `// fork: AI` in `FlowCommentsBottomSheet.kt`, pulsante visibile solo se la AI è disponibile
- [ ] ~200 commenti nell'ordine "top" tramite `CommentsPager`: solo testo e like, senza risposte né autori (ADR 0005)
- [ ] Prompt e risposta JSON con le quattro sezioni; sezioni vuote nascoste
- [ ] Vista del riassunto con provider e modello, "Rigenera"; cache del ticket 04 con tipo "commenti"
- [ ] Video con pochi commenti: il riassunto si fa lo stesso; nessun commento → pulsante nascosto
- [ ] Test JVM: costruzione dell'input (niente autori), parsing del JSON
