# Night Sound

Application Android native de surveillance sonore nocturne.

## Fonctionnement
- Le micro surveille le niveau sonore.
- L'audio n'est pas sauvegardé en continu.
- Un buffer d'environ 5 secondes avant le déclenchement est conservé.
- Un bruit au-dessus du seuil démarre un fichier WAV.
- Après environ 3 secondes de silence, le fichier est fermé.
- Les fichiers sont enregistrés dans l'espace privé de l'application.

## Compilation
Ouvrir le dossier dans Android Studio, synchroniser Gradle, puis :
Build > Build App Bundle(s) / APK(s) > Build APK(s)

Le seuil est un prototype : les valeurs en dB ne sont pas des mesures acoustiques calibrées.
