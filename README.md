# Utdanning ot Adapter


FINT Utdanning OT Adapter synchronizes OT ([Oppfølgingstjenesten](https://utdanning.no/tema/videregaende_opplaering/oppfolgingstjenesten)) data from Vigo to FINT. It runs as a cron job (non-web Spring application) that:

1. Fetches OTUngdomData from the Vigo REST API
2. Transforms Vigo models into FINT OtUngdomResource and PersonResource
3. Pushes both resource types as full-sync pages to the FINT Provider