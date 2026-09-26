<script setup>
import { computed, onMounted, ref } from 'vue'
import { api, demoProjects, demoQuotes, demoUsers } from './services/api'

const API_URL = import.meta.env.VITE_API_URL || '/api'

const page = ref('dashboard')
const projects = ref([])
const quotes = ref([])
const users = ref([])
const summary = ref({ projects: 2, activeQuotes: 1, documents: 24, teamMembers: 2 })
const files = ref([])
const currentFolder = ref('')
const apiOffline = ref(false)
const showProjectModal = ref(false)
const showQuoteModal = ref(false)
const editingProject = ref(null)
const projectForm = ref({ projectNumber: '', name: '', contactPerson: '', description: '', status: 'ACTIVE' })
const quoteForm = ref({ projectId: 1, name: '', description: '', status: 'DRAFT', steps: [{ stepNumber: 1, subStep: 1, startCondition: '', pinToTest: '', measurementType: 'VOLTAGE', unit: 'V', expectedMin: null, expectedMax: null, relay: '', voltage: null }] })
const search = ref('')
const toast = ref('')

const filteredProjects = computed(() => projects.value.filter(project =>
  `${project.projectNumber} ${project.name} ${project.contactPerson || ''}`.toLowerCase().includes(search.value.toLowerCase())))

const navItems = [
  { id: 'dashboard', label: 'Vue d’ensemble', icon: '⌂' },
  { id: 'projects', label: 'Projets', icon: '▦' },
  { id: 'quotes', label: 'Devis de tests', icon: '✓' },
  { id: 'documents', label: 'Documentation', icon: '▤' },
]

async function loadData() {
  try {
    const [projectData, quoteData, userData, summaryData] = await Promise.all([
      api('/projects'), api('/test-quotes'), api('/users'), api('/dashboard/summary'),
    ])
    projects.value = projectData; quotes.value = quoteData; users.value = userData; summary.value = summaryData
    await loadFiles()
  } catch {
    apiOffline.value = true
    projects.value = demoProjects; quotes.value = demoQuotes; users.value = demoUsers
  }
}

async function loadFiles(folder = currentFolder.value) {
  currentFolder.value = folder
  try { files.value = (await api(`/files?path=${encodeURIComponent(folder)}`)).items } catch { files.value = demoFiles(folder) }
}

function demoFiles(folder) {
  if (folder) return [{ name: 'README.md', type: 'file', extension: 'md', path: `${folder}/README.md`, openUri: '' }]
  return [
    { name: 'projets', type: 'directory', extension: '', path: 'projets', openUri: '' },
    { name: 'README.md', type: 'file', extension: 'md', path: 'README.md', openUri: '' },
    { name: 'plans-de-validation', type: 'directory', extension: '', path: 'plans-de-validation', openUri: '' },
  ]
}

function openFile(file) {
  if (file.type === 'directory') loadFiles(file.path)
  else if (file.openUri) {
    const opened = window.open(file.openUri, '_blank', 'noopener,noreferrer')
    if (!opened) copyFilePath(file)
  }
}

function fileSystemPath(file) {
  const uri = file.openUri || ''
  const rawPath = uri.startsWith('file:///') ? uri.slice('file:///'.length) : uri
  const path = decodeURIComponent(rawPath)
  return /^[A-Za-z]:\//.test(path) ? path.replaceAll('/', '\\') : path
}

async function copyFilePath(file) {
  const path = fileSystemPath(file)
  try {
    await navigator.clipboard.writeText(path)
    notify('Chemin local copié')
  } catch {
    notify(`Chemin local : ${path}`)
  }
}

function openProjectModal(project = null) {
  editingProject.value = project
  projectForm.value = project ? { ...project } : { projectNumber: '', name: '', contactPerson: '', description: '', status: 'ACTIVE' }
  showProjectModal.value = true
}

async function saveProject() {
  try {
    if (apiOffline.value) {
      if (editingProject.value) Object.assign(editingProject.value, projectForm.value)
      else projects.value.unshift({ ...projectForm.value, id: Date.now() })
    } else if (editingProject.value) await api(`/projects/${editingProject.value.id}`, { method: 'PUT', body: JSON.stringify(projectForm.value) })
    else await api('/projects', { method: 'POST', body: JSON.stringify(projectForm.value) })
    showProjectModal.value = false; await loadData(); notify('Projet enregistré')
  } catch { notify('Impossible d’enregistrer le projet') }
}

async function deleteProject(project) {
  if (!confirm(`Supprimer ${project.projectNumber} ?`)) return
  if (!apiOffline.value) await api(`/projects/${project.id}`, { method: 'DELETE' })
  projects.value = projects.value.filter(item => item.id !== project.id); notify('Projet supprimé')
}

function openQuoteModal() {
  quoteForm.value = { projectId: projects.value[0]?.id || 1, name: '', description: '', status: 'DRAFT', steps: [{ stepNumber: 1, subStep: 1, startCondition: '', pinToTest: '', measurementType: 'VOLTAGE', unit: 'V', expectedMin: null, expectedMax: null, relay: '', voltage: null }] }
  showQuoteModal.value = true
}

function addQuoteStep() {
  const next = quoteForm.value.steps.length + 1
  quoteForm.value.steps.push({ stepNumber: next, subStep: 1, startCondition: '', pinToTest: '', measurementType: 'VOLTAGE', unit: 'V', expectedMin: null, expectedMax: null, relay: '', voltage: null })
}

async function saveQuote() {
  try {
    if (apiOffline.value) quotes.value.unshift({ id: Date.now(), name: quoteForm.value.name, projectNumber: projects.value.find(p => p.id === quoteForm.value.projectId)?.projectNumber || 'MF-2401', projectName: projects.value.find(p => p.id === quoteForm.value.projectId)?.name || 'Projet démo', status: 'DRAFT', stepCount: quoteForm.value.steps.length })
    else await api('/test-quotes', { method: 'POST', body: JSON.stringify(quoteForm.value) })
    showQuoteModal.value = false; await loadData(); notify('Devis enregistré')
  } catch { notify('Impossible d’enregistrer le devis') }
}

async function generateCpp(quote) {
  if (apiOffline.value) { notify('Mode présentation : génération C++ simulée'); return }
  const response = await fetch(`${API_URL}/test-quotes/${quote.id}/generate-cpp`, { method: 'POST' })
  const blob = await response.blob(); const url = URL.createObjectURL(blob)
  const anchor = document.createElement('a'); anchor.href = url; anchor.download = `test_quote_${quote.id}.cpp`; anchor.click(); URL.revokeObjectURL(url)
  notify('Fichier C++ généré')
}

function notify(message) { toast.value = message; setTimeout(() => { toast.value = '' }, 2800) }
function statusLabel(status) { return ({ ACTIVE: 'Actif', ARCHIVED: 'Archivé', IN_PROGRESS: 'En cours', DRAFT: 'Brouillon' })[status] || status }
function goTo(id) { page.value = id; if (id === 'documents') loadFiles() }

onMounted(loadData)
</script>

<template>
  <div class="app-shell">
    <aside class="sidebar">
      <div class="brand"><div class="brand-mark">M</div><div><strong>MegaFinder</strong><small>LAB OPERATIONS</small></div></div>
      <div class="workspace-label">ESPACE DE TRAVAIL</div>
      <nav><button v-for="item in navItems" :key="item.id" :class="{ active: page === item.id }" @click="goTo(item.id)"><span class="nav-icon">{{ item.icon }}</span>{{ item.label }}</button></nav>
      <div class="sidebar-bottom">
        <button class="team-link" @click="page = 'team'"><span class="nav-icon">◉</span>Équipe & accès</button>
        <div class="user-card"><div class="avatar">AM</div><div><strong>Alex Martin</strong><small>Administrateur</small></div><span class="more">•••</span></div>
      </div>
    </aside>

    <main class="main-content">
      <header class="topbar"><div class="breadcrumbs"><span>MegaFinder</span><b>/</b><strong>{{ navItems.find(item => item.id === page)?.label || 'Équipe & accès' }}</strong></div><div class="top-actions"><span class="status-dot"></span><span class="sync-label">Synchronisé</span><button class="icon-button">⌕</button><button class="icon-button notification">♧<i></i></button></div></header>
      <div v-if="apiOffline" class="demo-banner">Mode présentation actif — les données affichées sont une maquette locale.</div>

      <section v-if="page === 'dashboard'" class="page"><div class="page-heading"><div><p class="eyebrow">MARDI 25 SEPTEMBRE 2026</p><h1>Bonjour Alex <span>✦</span></h1><p class="subtitle">Voici l’état de vos opérations de test aujourd’hui.</p></div><button class="primary-button" @click="openProjectModal()">＋ Nouveau projet</button></div>
        <div class="metric-grid"><div class="metric-card"><div class="metric-top"><span class="metric-icon purple">▦</span><span class="trend positive">↗ 12%</span></div><strong>{{ summary.projects }}</strong><span>Projets actifs</span><div class="spark purple-line"></div></div><div class="metric-card"><div class="metric-top"><span class="metric-icon blue">✓</span><span class="trend positive">↗ 8%</span></div><strong>{{ summary.activeQuotes }}</strong><span>Devis en cours</span><div class="spark blue-line"></div></div><div class="metric-card"><div class="metric-top"><span class="metric-icon orange">▤</span><span class="trend neutral">Ce mois</span></div><strong>{{ summary.documents }}</strong><span>Documents suivis</span><div class="spark orange-line"></div></div><div class="metric-card"><div class="metric-top"><span class="metric-icon green">◎</span><span class="trend positive">+ 2</span></div><strong>{{ summary.teamMembers }}</strong><span>Membres de l’équipe</span><div class="spark green-line"></div></div></div>
        <div class="content-grid"><div class="panel activity-panel"><div class="panel-heading"><div><h2>Activité récente</h2><p>Les dernières actions sur vos projets</p></div><button class="text-button">Voir tout →</button></div><div class="activity-row"><div class="activity-icon purple-bg">✓</div><div><strong>Devis de tests mis à jour</strong><p>Validation des entrées analogiques · il y a 24 min</p></div><span class="activity-user">ST</span></div><div class="activity-row"><div class="activity-icon blue-bg">▤</div><div><strong>Nouveau document ajouté</strong><p>Plan de validation ECU · il y a 1 h</p></div><span class="activity-user orange-avatar">AM</span></div><div class="activity-row"><div class="activity-icon green-bg">＋</div><div><strong>Projet créé</strong><p>Module de puissance · hier</p></div><span class="activity-user pink-avatar">MR</span></div></div><div class="panel quick-panel"><div class="panel-heading"><div><h2>Accès rapide</h2><p>Les outils les plus utilisés</p></div></div><button @click="goTo('quotes')"><span class="quick-icon purple">✓</span><span><strong>Créer un devis de test</strong><small>Définir les étapes et conditions</small></span><b>→</b></button><button @click="goTo('documents')"><span class="quick-icon blue">▤</span><span><strong>Parcourir la documentation</strong><small>Ouvrir un fichier local</small></span><b>→</b></button><button @click="goTo('team')"><span class="quick-icon green">◎</span><span><strong>Gérer les accès</strong><small>Rôles et permissions</small></span><b>→</b></button></div></div>
      </section>

      <section v-else-if="page === 'projects'" class="page"><div class="page-heading"><div><p class="eyebrow">PILOTAGE</p><h1>Projets</h1><p class="subtitle">Centralisez vos projets et leurs informations clés.</p></div><button class="primary-button" @click="openProjectModal()">＋ Nouveau projet</button></div><div class="toolbar"><div class="search-box">⌕<input v-model="search" placeholder="Rechercher un projet..." /></div><button class="filter-button">☷ Filtrer</button><span class="result-count">{{ filteredProjects.length }} projets</span></div><div class="panel table-panel"><table><thead><tr><th>PROJET</th><th>CONTACT</th><th>DESCRIPTION</th><th>STATUT</th><th></th></tr></thead><tbody><tr v-for="project in filteredProjects" :key="project.id"><td><div class="project-cell"><span class="project-avatar">{{ project.projectNumber.slice(-2) }}</span><div><strong>{{ project.name }}</strong><small>{{ project.projectNumber }}</small></div></div></td><td>{{ project.contactPerson || '—' }}</td><td class="description-cell">{{ project.description || 'Aucune description' }}</td><td><span class="badge" :class="project.status.toLowerCase()">{{ statusLabel(project.status) }}</span></td><td class="actions"><button @click="openProjectModal(project)">Modifier</button><button class="danger-link" @click="deleteProject(project)">Supprimer</button></td></tr></tbody></table></div></section>

      <section v-else-if="page === 'quotes'" class="page"><div class="page-heading"><div><p class="eyebrow">AUTOMATISATION</p><h1>Devis de tests</h1><p class="subtitle">Structurez vos scénarios et générez vos fichiers C++.</p></div><button class="primary-button" @click="openQuoteModal">＋ Nouveau devis</button></div><div class="quote-intro"><div class="quote-intro-icon">✓</div><div><strong>Un format commun pour chaque validation</strong><p>Chaque ligne décrit la condition, la mesure attendue et l’action matérielle à exécuter.</p></div><span class="quote-steps">Étape 1 / 3</span></div><div class="quote-grid"><div v-for="quote in quotes" :key="quote.id" class="quote-card"><div class="quote-card-top"><span class="badge" :class="quote.status.toLowerCase()">{{ statusLabel(quote.status) }}</span><button>•••</button></div><h3>{{ quote.name }}</h3><p>{{ quote.projectNumber }} · {{ quote.projectName }}</p><div class="quote-progress"><span><b>{{ quote.stepCount }}</b> étapes définies</span><span>{{ quote.status === 'IN_PROGRESS' ? '68' : '0' }}%</span></div><div class="progress-bar"><i :style="{ width: quote.status === 'IN_PROGRESS' ? '68%' : '8%' }"></i></div><button class="outline-button" @click="generateCpp(quote)">⌘ Générer le fichier C++</button></div><div class="quote-card new-card" @click="openQuoteModal"><div class="new-plus">＋</div><strong>Créer un nouveau devis</strong><p>Commencez avec un scénario de test vide.</p></div></div></section>

      <section v-else-if="page === 'documents'" class="page"><div class="page-heading"><div><p class="eyebrow">ESPACE LOCAL</p><h1>Documentation</h1><p class="subtitle">Retrouvez vos fichiers directement depuis leur dossier de travail.</p></div><button class="primary-button" @click="loadFiles()">↻ Actualiser</button></div><div class="path-bar"><span>⌂</span><b>/ documentation</b><span v-if="currentFolder">/ {{ currentFolder }}</span><button v-if="currentFolder" @click="loadFiles('')">Revenir au dossier racine</button></div><div class="panel files-panel"><div class="files-heading"><div><h2>Fichiers disponibles</h2><p>Le contenu reste sur votre disque local.</p></div><span>{{ files.length }} éléments</span></div><div v-for="file in files" :key="file.path" class="file-row" @dblclick="openFile(file)"><div class="file-icon" :class="file.type">{{ file.type === 'directory' ? '▰' : '▤' }}</div><div><strong>{{ file.name }}</strong><small>{{ file.type === 'directory' ? 'Dossier' : file.extension.toUpperCase() + ' · fichier local' }}</small></div><button v-if="file.type === 'directory'" @click="loadFiles(file.path)">Ouvrir →</button><div v-else class="file-actions"><button @click.stop="openFile(file)">Ouvrir le fichier ↗</button><button @click.stop="copyFilePath(file)">Copier le chemin</button></div></div><div v-if="!files.length" class="empty-state">Aucun fichier dans ce dossier.</div></div></section>

      <section v-else class="page"><div class="page-heading"><div><p class="eyebrow">ADMINISTRATION</p><h1>Équipe & accès</h1><p class="subtitle">Gérez les rôles et les droits complémentaires de votre équipe.</p></div><button class="primary-button">＋ Inviter un membre</button></div><div class="role-cards"><div><span class="role-icon purple">♟</span><strong>Administrateur</strong><small>Accès complet à l’espace</small><b>1 membre</b></div><div><span class="role-icon blue">⚙</span><strong>Ingénieur</strong><small>Projets, tests et documentation</small><b>1 membre</b></div><div><span class="role-icon gray">◉</span><strong>Lecteur</strong><small>Consultation uniquement</small><b>0 membre</b></div></div><div class="panel table-panel"><div class="panel-heading"><div><h2>Membres de l’équipe</h2><p>Les permissions supplémentaires apparaissent sous chaque rôle.</p></div></div><table><thead><tr><th>MEMBRE</th><th>RÔLE</th><th>PERMISSIONS EN PLUS</th><th>STATUT</th></tr></thead><tbody><tr v-for="user in users" :key="user.id"><td><div class="project-cell"><span class="project-avatar user-avatar">{{ user.displayName.split(' ').map(x => x[0]).join('') }}</span><div><strong>{{ user.displayName }}</strong><small>{{ user.email }}</small></div></div></td><td><span class="role-pill">{{ user.role }}</span></td><td><span v-for="permission in user.extraPermissions" :key="permission" class="permission">{{ permission }}</span></td><td><span class="active-dot"></span> Actif</td></tr></tbody></table></div></section>
    </main>

    <div v-if="showProjectModal" class="modal-backdrop" @click.self="showProjectModal = false"><div class="modal"><div class="modal-heading"><div><p class="eyebrow">PROJET</p><h2>{{ editingProject ? 'Modifier le projet' : 'Nouveau projet' }}</h2></div><button @click="showProjectModal = false">×</button></div><label>Numéro du projet<input v-model="projectForm.projectNumber" placeholder="MF-2402" /></label><label>Nom du projet<input v-model="projectForm.name" placeholder="Nom du projet" /></label><label>Personne à contacter<input v-model="projectForm.contactPerson" placeholder="Nom du contact" /></label><label>Description<textarea v-model="projectForm.description" rows="4" placeholder="Décrivez le périmètre du projet..."></textarea></label><div class="modal-actions"><button class="filter-button" @click="showProjectModal = false">Annuler</button><button class="primary-button" @click="saveProject">Enregistrer</button></div></div></div>
    <div v-if="showQuoteModal" class="modal-backdrop" @click.self="showQuoteModal = false"><div class="modal quote-modal"><div class="modal-heading"><div><p class="eyebrow">DEVIS DE TEST</p><h2>Nouveau devis</h2></div><button @click="showQuoteModal = false">×</button></div><label>Projet<select v-model="quoteForm.projectId"><option v-for="project in projects" :key="project.id" :value="project.id">{{ project.projectNumber }} · {{ project.name }}</option></select></label><label>Nom du devis<input v-model="quoteForm.name" placeholder="Ex. Validation tension pin 2" /></label><div class="step-editor"><div class="step-editor-heading"><strong>Lignes de test</strong><button class="text-button" @click="addQuoteStep">＋ Ajouter une ligne</button></div><div v-for="(step, index) in quoteForm.steps" :key="index" class="step-line"><span class="step-number">{{ index + 1 }}</span><input v-model="step.startCondition" placeholder="Condition de départ" /><input v-model="step.pinToTest" placeholder="Pin à tester" /><select v-model="step.measurementType"><option>VOLTAGE</option><option>CURRENT</option><option>RESISTANCE</option><option>FREQUENCY</option><option>DUTY_CYCLE</option></select><input v-model="step.expectedMin" type="number" placeholder="Min" /><input v-model="step.expectedMax" type="number" placeholder="Max" /></div></div><div class="modal-actions"><button class="filter-button" @click="showQuoteModal = false">Annuler</button><button class="primary-button" @click="saveQuote">Enregistrer le devis</button></div></div></div>
    <div v-if="toast" class="toast">✓ {{ toast }}</div>
  </div>
</template>
