# YouTube Sync scrive nei normali archivi di Flow e gira su un solo dispositivo

La YouTube Sync non estende il protocollo della Device Sync. Legge e scrive iscrizioni e playlist attraverso i normali archivi (repository) di Flow, come farebbe l'utente. La Device Sync tratta quindi quelle scritture come modifiche locali e le propaga da sola agli altri dispositivi. In questo modo il fork resta facile da riallineare al progetto originale, e il formato della Device Sync resta compatibile con Flow Desktop e con installazioni non modificate.

Un solo YouTube Sync Device per gruppo di dispositivi, il telefono, parla con YouTube. Gli altri, per esempio la Firestick, ricevono le modifiche solo tramite la Device Sync. Senza questa regola, due dispositivi invierebbero la stessa modifica a YouTube, duplicando i video nelle playlist. La Sync Baseline vive solo sul YouTube Sync Device e non viaggia con la Device Sync. Su un nuovo YouTube Sync Device si riparte come al primo collegamento: unione delle due liste, con anteprima.

## Consequences

- Una modifica fatta su un dispositivo diverso dal YouTube Sync Device arriva a YouTube con il ritardo combinato delle due sincronizzazioni (Device Sync, circa ogni ora, più YouTube Sync).
- I conflitti si risolvono su due piani distinti. Tra i dispositivi vale la regola della Device Sync: vince la modifica più recente. Tra Flow e YouTube vale la regola della YouTube Sync: confronto a tre vie con la Sync Baseline, un'eliminazione prevale su una modifica, e su modifiche in conflitto vince YouTube.
