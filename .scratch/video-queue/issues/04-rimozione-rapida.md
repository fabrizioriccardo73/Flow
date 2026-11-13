# 04: Rimozione rapida dalla Coda

**What to build:** tolgo un video dalla Coda senza aprire lo sheet: dal menu di qualsiasi video che è in Coda, o con "Fatto ✓" nel player.

**Blocked by:** 01 (Coda persistente e ordine Contesto → Coda → Autoplay)

**Status:** ready-for-agent

- [x] Nel menu ⋮ di un video che è in Coda, "Aggiungi alla Coda" diventa "Rimuovi dalla Coda", con snackbar "Annulla"
- [x] "Riproduci dopo" resta disponibile anche per un video già in Coda (lo sposta in cima)
- [x] Nel player, quando il video in riproduzione viene dalla Coda, compare l'azione rapida "Fatto ✓": lo toglie dalla Coda e passa al prossimo video secondo l'ordine
- [x] "Fatto ✓" non compare per i video del Contesto o dell'Autoplay

Fuori ambito: "Fatto ✓" nella notifica multimediale. Per ora c'è solo nel player.

## Comments

**2026-11-13**: Implementato, non ancora provato sul telefono.
- "Fatto ✓" è un pulsante con la spunta nel dock "Next up" del player, sia flottante sia nel pannello laterale. Compare solo quando il video in riproduzione è quello "In riproduzione" della Coda. Equivale a "Successivo": toglie il video e passa al prossimo nell'ordine.
- In uno sheet della Coda, il menu a pressione lunga di una riga mostra "Rimuovi dalla Coda" due volte: quella della riga e quella del menu. Fanno la stessa cosa.
