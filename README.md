# MegaFinder MVP

Maquette locale pour centraliser les projets, la documentation et les devis de tests.

## Démarrage sans cloner le projet

Les images publiques sont publiées sur GitHub Container Registry. Il suffit de télécharger le Compose et de créer le dossier de documentation :

```powershell
New-Item -ItemType Directory -Force documentation
Invoke-WebRequest https://raw.githubusercontent.com/bestrider14/MegaFinder/main/compose.yaml -OutFile compose.yaml
$env:DOCUMENTATION_HOST_PATH = (Resolve-Path .\documentation).Path
docker compose pull
docker compose up -d
```

L'application est ensuite disponible sur `http://localhost:18080`.

Pour utiliser un autre dossier de documentation :

```powershell
$env:DOCUMENTATION_HOST_PATH = 'C:\Chemin\Vers\Vos\Documents'
docker compose up -d
```

## Démarrage depuis le code

Le backend utilise PostgreSQL dans Docker :

```powershell
docker compose up -d
```

Puis lancer Spring Boot dans un second terminal :

```powershell
.\mvnw.cmd spring-boot:run
```

Le backend local est disponible sur `http://localhost:8082` et applique automatiquement la migration Flyway avec les données de démonstration. Avec Docker Compose, il est accessible sur `http://localhost:18082`.

Lancer le frontend dans un troisième terminal :

```powershell
cd frontend
npm install
npm run dev
```

La maquette est disponible sur `http://localhost:5173`.

## Fonctions du MVP

- gestion CRUD des projets (`/api/projects`) ;
- chaque projet possède un dossier `documentation/projets/<numero-projet>` créé automatiquement ;
- historique des créations et modifications avec date, utilisateur et champs modifiés (`/api/projects/{id}/activity`) ;
- page complète de projet avec aperçu de la documentation, devis liés et génération C++ rapide ;
- devis de tests avec étapes, conditions, pin, seuils, relais, voltage, type de mesure et unité (`/api/test-quotes`) ;
- produits liés à un projet avec présentation, images du boîtier et du PCB, pinout, caractéristiques et plusieurs contacts par rôle (`/api/products`) ;
- devis attachés à un seul produit avec références Megatech/BRP/autres, étapes B/C/D/ECT, méthodes, délais, seuils et templates de mesure ;
- décodage des équipements requis par un devis et comparaison avec le banc sélectionné (`/api/test-quotes/{id}/equipment-summary`) ;
- export du devis de vérification en C++ et en PDF (`/api/test-quotes/{id}/generate-cpp`, `/api/test-quotes/{id}/generate-pdf`) ;
- bancs de test configurables avec alimentations, oscilloscope, cartes relais, bornier et liaisons pin/relais (`/api/test-benches`) ;
- association d’un banc à un devis et configuration par étape de l’alimentation, tension, courant, canaux relais et délai d’activation ;
- chaque devis est lié à un projet et à un template C++ choisi ;
- création, modification et suppression des templates C++ personnalisés (`/api/test-templates`) ;
- génération d'un fichier C++ GoogleTest via `POST /api/test-quotes/{id}/generate-cpp` ;
- exploration du dossier local configuré par `MEGAFINDER_DOCUMENTATION_ROOT` (`/api/files`) ;
- utilisateurs, rôles et permissions additionnelles (`/api/users`) ;
- données de dashboard (`/api/dashboard/summary`).

La maquette utilise un thème sombre par défaut. L’utilisateur actif peut être choisi dans la barre supérieure;
les créations et modifications de projets sont associées à cet utilisateur.

L'API de fichiers renvoie uniquement les métadonnées et une URI `file://` pour ouvrir le fichier avec l'application du poste. Le contenu des documents n'est pas servi par Spring Boot. `DOCUMENTATION_HOST_PATH` doit être un chemin absolu du poste hôte pour que les liens fonctionnent depuis Docker.

Le port PostgreSQL local utilisé par défaut est `55432` afin d'éviter les conflits avec une installation PostgreSQL existante :

```powershell
docker compose up -d
.\mvnw.cmd spring-boot:run
```

Les images sont reconstruites automatiquement par GitHub Actions à chaque push sur `main` :

- `ghcr.io/bestrider14/megafinder-backend:latest`
- `ghcr.io/bestrider14/megafinder-frontend:latest`

Les templates utilisent les variables `{{QUOTE_ID}}`, `{{QUOTE_NAME}}`, `{{PROJECT_NUMBER}}`,
`{{PROJECT_NAME}}`, `{{TEST_CLASS}}` et `{{STEPS}}`. Le contenu d'un template est éditable depuis
l'écran **Templates C++** de la maquette.
