const API_URL = import.meta.env.VITE_API_URL || '/api'

export function getCurrentUserId() {
  return localStorage.getItem('megafinder.userId') || '1'
}

export function setCurrentUserId(userId) {
  localStorage.setItem('megafinder.userId', String(userId))
}

export async function api(path, options = {}) {
  const response = await fetch(`${API_URL}${path}`, {
    headers: {
      'Content-Type': 'application/json',
      'X-MegaFinder-User-Id': getCurrentUserId(),
      ...(options.headers || {}),
    },
    ...options,
  })
  if (!response.ok) throw new Error(await response.text() || 'Erreur serveur')
  if (response.status === 204) return null
  return response.json()
}

export const demoProjects = [
  { id: 1, projectNumber: 'MF-2401', name: 'Banc de test ECU', contactPerson: 'Sophie Tremblay', description: 'Validation du contrôleur moteur et automatisation des scénarios.', status: 'ACTIVE' },
  { id: 2, projectNumber: 'MF-2398', name: 'Module de puissance', contactPerson: 'Marc Gagnon', description: 'Documentation et tests de caractérisation du module.', status: 'ACTIVE' },
  { id: 3, projectNumber: 'MF-2387', name: 'Interface capteurs', contactPerson: 'Nadia Roy', description: 'Suite de tests pour les entrées analogiques.', status: 'ARCHIVED' },
]

export const demoProducts = [
  {
    id: 1, projectId: 1, projectNumber: 'MF-2401', projectName: 'Banc de test ECU', productNumber: 'ECU-2401-P1',
    name: 'Contrôleur ECU principal', description: 'Produit de démonstration pour la validation des entrées et de l’alimentation.',
    presentation: 'Module électronique automobile avec alimentation 14 V et acquisition de courant.',
    presentationImages: ['images/produit-vue-avant.png', 'images/produit-vue-arriere.png'],
    enclosureImages: ['images/boitier-face-avant.png', 'images/boitier-face-arriere.png'],
    enclosurePinout: 'TP5 = alimentation / mesure\nTP9 = GND\nTP12 = signal de réveil',
    pcbImages: ['images/pcb-top.png', 'images/pcb-bottom.png'], pcbSpecifications: '4 couches · FR-4 · 12 V nominal · connecteur X1',
    contacts: [{ id: 1, name: 'Sophie Tremblay', role: 'Responsable produit', email: 'sophie@example.local', phone: '' }], quoteCount: 1, contactCount: 1,
  },
  {
    id: 2, projectId: 2, projectNumber: 'MF-2398', projectName: 'Module de puissance', productNumber: 'PWR-2398-P1',
    name: 'Module de puissance', description: 'Produit de démonstration de puissance.', presentation: 'Module à caractériser.', presentationImages: [], enclosureImages: [], enclosurePinout: '', pcbImages: [], pcbSpecifications: '',
    contacts: [{ id: 2, name: 'Marc Gagnon', role: 'Ingénieur projet', email: '', phone: '' }], quoteCount: 0, contactCount: 1,
  },
]

export const demoQuotes = [
  { id: 1, name: 'Validation des entrées analogiques', projectNumber: 'MF-2401', projectName: 'Banc de test ECU', status: 'IN_PROGRESS', stepCount: 8 },
  { id: 2, name: 'Démarrage et alimentation', projectNumber: 'MF-2398', projectName: 'Module de puissance', status: 'DRAFT', stepCount: 12 },
]

export const demoBenches = [
  {
    id: 1,
    name: 'Banc ECU principal',
    description: 'Banc de validation avec alimentations programmables, oscilloscope et cartes relais.',
    location: 'Laboratoire A · Poste 01',
    equipmentCount: 4,
    terminalCount: 5,
    relayCount: 80,
    equipment: [
      { id: 1, equipmentType: 'POWER_SUPPLY', name: 'Power supply 0-25 V / 50 A', quantity: 1, channelCount: 1, voltageMin: 0, voltageMax: 25, currentMax: 50, notes: 'Sortie principale haute puissance' },
      { id: 2, equipmentType: 'POWER_SUPPLY', name: 'Power supply 0-240 V / 5 A', quantity: 1, channelCount: 1, voltageMin: 0, voltageMax: 240, currentMax: 5, notes: 'Sortie haute tension' },
      { id: 3, equipmentType: 'OSCILLOSCOPE', name: 'Oscilloscope 4 voies', quantity: 1, channelCount: 4, notes: 'Mesure temporelle et signaux rapides' },
      { id: 4, equipmentType: 'RELAY_CARD', name: 'Carte relais 20 voies', quantity: 4, channelCount: 20, notes: '80 relais disponibles · K1 à K80' },
    ],
    terminals: [
      { id: 1, terminalLabel: 'X1-01', pinName: 'VBAT+', signalType: 'Alimentation', description: 'Entrée alimentation du module', relayReference: 'K1' },
      { id: 2, terminalLabel: 'X1-02', pinName: 'GND', signalType: 'Retour', description: 'Retour commun', relayReference: 'K2' },
      { id: 3, terminalLabel: 'X1-03', pinName: 'PIN_SENSOR_1', signalType: 'Analogique', description: 'Entrée capteur 1', relayReference: 'K3' },
      { id: 4, terminalLabel: 'X1-04', pinName: 'PIN_SENSOR_2', signalType: 'Analogique', description: 'Entrée capteur 2', relayReference: 'K4' },
      { id: 5, terminalLabel: 'X1-05', pinName: 'RELAY_RETURN', signalType: 'Relais', description: 'Sortie commandée', relayReference: 'K5' },
    ],
  },
]

export const demoTemplates = [
  { id: 1, templateKey: 'DEFAULT', name: 'GoogleTest standard', description: 'Template C++ de base pour les devis de tests.', language: 'CPP', system: true, content: '// Generated by MegaFinder - Test quote #{{QUOTE_ID}}\n#include <gtest/gtest.h>\n\nclass {{TEST_CLASS}} : public ::testing::Test {};\n\n{{STEPS}}' },
]

export const demoUsers = [
  { id: 1, displayName: 'Alex Martin', email: 'alex.martin@megafinder.local', role: 'ADMIN', extraPermissions: ['GENERATE_CPP', 'MANAGE_USERS'], active: true },
  { id: 2, displayName: 'Sophie Tremblay', email: 'sophie.tremblay@megafinder.local', role: 'ENGINEER', extraPermissions: ['GENERATE_CPP'], active: true },
]
