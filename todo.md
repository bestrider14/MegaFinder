Pour commencer, je te donne l'autorisation de lire et d'écrire dans le dossier courant et aussi dans le dossier backend qui est dans le dossier parent.
Ici le but de mon projet est de créer un MVP pour une app web locale qui permettra de gérer des tâches et des projets. Pour le mvp, on va se créer une base de donner postgres avec docker.
Le but est de simplifier les gestions des fichiers de documentation, avoir un meme pattern pour les devis de tests, générer des fichiers c++ pour les tests unitaires selon un template.
Donc, il faudrait un moyen de pouvoir lister les projets existants, en créer, modifier et supprimer des projets. Chaque projet va avoir leur numero et un nom, personne contacter et une description.
Je ne veux pas que l'app serve les fichiers, mais les ouvres directement dans l'application depuis le disque dur. On aura donc besoin d'une fonction qui permettra de lister les fichiers dans un dossier et de les ouvrir.
Classer en sous categories pour faciliter la navigation selon un patern defini. On aura besoin de faire un systeme de creation d'user et de gestion des droits d'acces.
Pour la generation des tests, on va pouvoir cree un devis de teste, dire la condition de depart, quel pin à tester, resultats min/max attendus. Quel relais activer, voltage, etc
Cela va etre parametrable a pour chaque ligne. Exemple :
- Condition de depart : 5V sur pin 1
- étape: 3 
- sous-étape: 1 (pourrait être calculé automatiquement)
- Pin a tester: pin 2
- Résultats min/max attendus : 3V min, 4V max
- Relais activer : relais 1
- Voltage : 12V

Un drop box pour sélectionner le type de test (courant, tension, resistance, fréquence, duty cycle) et ansi sélectionner l'unité de mesure.
Avec ce devis, on va pouvoir générer des fichiers c++ pour les tests unitaires selon un template.
On va fonctionner avec des roles pour la gestion des accès, mais on pourrait donner des droits supplémentaires.
Donc, génère-moi un backend avec Spring Boot qui est deja commencer dans le dossier backend et le frontend avec vue.js dans le dossier frontend. C'est un MVP, donc je voudrais juste avoir une
maquette pour présenter mon projet au prochain meeting.