const $ = (s) => document.querySelector(s);
const $$ = (s) => [...document.querySelectorAll(s)];
let patients = [], appointments = [], emergencies = [];
let toastTimer;

function notify(message) {
  const toast = $('#toast');
  toast.textContent = message;
  toast.classList.add('show');
  clearTimeout(toastTimer);
  toastTimer = setTimeout(() => toast.classList.remove('show'), 3000);
}
function escapeHtml(value) {
  return String(value ?? '').replace(/[&<>"']/g, c => ({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]));
}
const STORAGE_KEY = 'carepoint-hospital-demo-v1';
let medicalHistory = {};
function loadSavedData() {
  try {
    const saved = JSON.parse(localStorage.getItem(STORAGE_KEY) || '{}');
    patients = Array.isArray(saved.patients) ? saved.patients : [];
    appointments = Array.isArray(saved.appointments) ? saved.appointments : [];
    emergencies = Array.isArray(saved.emergencies) ? saved.emergencies : [];
    medicalHistory = saved.medicalHistory && typeof saved.medicalHistory === 'object' ? saved.medicalHistory : {};
  } catch (_) {
    patients = []; appointments = []; emergencies = []; medicalHistory = {};
  }
}
function saveData() {
  localStorage.setItem(STORAGE_KEY, JSON.stringify({patients, appointments, emergencies, medicalHistory}));
}
function addHistory(id, entry) {
  if (!medicalHistory[id]) medicalHistory[id] = [];
  medicalHistory[id].unshift(new Date().toLocaleString() + ' — ' + entry);
}
async function api(path, data) {
  // GitHub Pages hosts static files only; use localStorage instead of a Java API.
  const url = new URL(path, window.location.href);
  const route = url.pathname;
  if (!data) {
    if (route === '/api/patients') return patients;
    if (route === '/api/appointments') return appointments;
    if (route === '/api/emergencies') return [...emergencies].sort((a,b) => Number(b.severity)-Number(a.severity));
    if (route === '/api/stats') return {patients:patients.length, appointments:appointments.length, emergencies:emergencies.length};
    if (route === '/api/history') {
      const id = url.searchParams.get('patientId');
      if (!id) throw new Error('Provide a patient ID.');
      if (!patients.some(p => p.id === id)) throw new Error('Patient ID not found.');
      return (medicalHistory[id] || []).map(entry => ({entry}));
    }
    throw new Error('Unknown request.');
  }
  if (route === '/api/patients') {
    for (const key of ['name','age','gender']) if (!String(data[key] || '').trim()) throw new Error('Please fill in ' + key + '.');
    const patient = {id:'P' + (1001 + patients.length), name:String(data.name).trim(), age:String(data.age), gender:String(data.gender), phone:String(data.phone || ''), condition:String(data.condition || 'Not specified'), createdAt:new Date().toLocaleString()};
    patients.push(patient); medicalHistory[patient.id] = []; saveData(); return patient;
  }
  if (route === '/api/appointments') {
    const patient = patients.find(p => p.id === data.patientId);
    if (!patient) throw new Error('Patient ID not found. Register the patient first.');
    const a = {id:'A' + (appointments.length+1), patientId:patient.id, patientName:patient.name, doctor:String(data.doctor || ''), date:String(data.date || ''), time:String(data.time || ''), reason:String(data.reason || '')};
    if (!a.doctor || !a.date || !a.time) throw new Error('Please enter doctor, date and time.');
    appointments.push(a); addHistory(patient.id, 'Appointment booked with ' + a.doctor + ' on ' + a.date + ' at ' + a.time); saveData(); return a;
  }
  if (route === '/api/emergencies') {
    const patient = patients.find(p => p.id === data.patientId);
    if (!patient) throw new Error('Patient ID not found. Register the patient first.');
    const severity = Number(data.severity);
    if (!Number.isInteger(severity) || severity < 1 || severity > 5) throw new Error('Severity must be from 1 to 5.');
    const e = {id:'E' + (emergencies.length+1), patientId:patient.id, patientName:patient.name, severity:String(severity), description:String(data.description || ''), status:'Waiting'};
    emergencies.push(e); emergencies.sort((a,b) => Number(b.severity)-Number(a.severity));
    addHistory(patient.id, 'Emergency case registered (severity ' + severity + ')'); saveData(); return e;
  }
  throw new Error('Unknown request.');
}
function goTo(section) {
  $$('.section').forEach(el => el.classList.toggle('active', el.id === section));
  $$('.nav-item').forEach(el => el.classList.toggle('active', el.dataset.section === section));
  const titles = {dashboard:'Dashboard Overview',patients:'Patient Records',appointments:'Appointments',emergency:'Emergency Queue',history:'Medical History'};
  $('#page-title').textContent = titles[section] || 'Dashboard';
  window.scrollTo({top:0,behavior:'smooth'});
}
$$('.nav-item').forEach(btn => btn.addEventListener('click', () => goTo(btn.dataset.section)));
$$('[data-go]').forEach(btn => btn.addEventListener('click', () => goTo(btn.dataset.go)));
$('#show-patient-form').addEventListener('click', () => $('#patient-form').classList.remove('hidden'));
$('#cancel-patient').addEventListener('click', () => {$('#patient-form').reset();$('#patient-form').classList.add('hidden');});

function renderPatients() {
  const q = $('#patient-search').value.trim().toLowerCase();
  const filtered = patients.filter(p => [p.id,p.name,p.condition].some(v => String(v).toLowerCase().includes(q)));
  $('#patient-count').textContent = `${filtered.length} record${filtered.length===1?'':'s'}`;
  $('#patient-table').innerHTML = filtered.length ? filtered.map(p => `<tr><td><span class="id-tag">${escapeHtml(p.id)}</span></td><td><b>${escapeHtml(p.name)}</b></td><td>${escapeHtml(p.age)}</td><td>${escapeHtml(p.gender)}</td><td>${escapeHtml(p.condition)}</td><td>${escapeHtml(p.createdAt)}</td></tr>`).join('') : '<tr><td colspan="6" class="empty">No matching patients found.</td></tr>';
}
function renderAppointments() {
  $('#appointment-table').innerHTML = appointments.length ? appointments.map((a,i)=>`<tr><td><span class="id-tag">#${i+1}</span></td><td><b>${escapeHtml(a.patientName)}</b><br><small>${escapeHtml(a.patientId)}</small></td><td>${escapeHtml(a.doctor)}</td><td>${escapeHtml(a.date)}</td><td>${escapeHtml(a.time)}</td><td>${escapeHtml(a.reason || '—')}</td></tr>`).join('') : '<tr><td colspan="6" class="empty">No appointments booked yet.</td></tr>';
}
function renderEmergencies() {
  $('#emergency-table').innerHTML = emergencies.length ? emergencies.map(e=>`<tr><td><span class="id-tag">${escapeHtml(e.id)}</span></td><td><b>${escapeHtml(e.patientName)}</b><br><small>${escapeHtml(e.patientId)}</small></td><td><span class="severity sev-${e.severity}">${e.severity} / 5</span></td><td>${escapeHtml(e.description)}</td><td>${escapeHtml(e.status)}</td></tr>`).join('') : '<tr><td colspan="5" class="empty">No emergency cases yet.</td></tr>';
}
async function refresh() {
  try {
    [patients, appointments, emergencies] = await Promise.all([
      api('/api/patients'), api('/api/appointments'), api('/api/emergencies')
    ]);
    $('#stat-patients').textContent = patients.length;
    $('#stat-appointments').textContent = appointments.length;
    $('#stat-emergencies').textContent = emergencies.length;
    renderPatients(); renderAppointments(); renderEmergencies();
  } catch (err) {
    notify('Could not load saved demo data. Try refreshing the page.');
  }
}
$('#patient-search').addEventListener('input', renderPatients);
$('#patient-form').addEventListener('submit', async e => {
  e.preventDefault();
  const data = Object.fromEntries(new FormData(e.currentTarget));
  try {
    const patient = await api('/api/patients', data);
    e.currentTarget.reset(); e.currentTarget.classList.add('hidden');
    await refresh(); notify(`Patient registered. ID: ${patient.id}`);
  } catch(err) { notify(err.message); }
});
$('#appointment-form').addEventListener('submit', async e => {
  e.preventDefault();
  try {
    const result = await api('/api/appointments', Object.fromEntries(new FormData(e.currentTarget)));
    e.currentTarget.reset(); await refresh(); notify(`Appointment ${result.id} booked successfully.`);
  } catch(err) { notify(err.message); }
});
$('#emergency-form').addEventListener('submit', async e => {
  e.preventDefault();
  try {
    const result = await api('/api/emergencies', Object.fromEntries(new FormData(e.currentTarget)));
    e.currentTarget.reset(); await refresh(); notify(`Emergency ${result.id} added to the priority queue.`);
  } catch(err) { notify(err.message); }
});
$('#history-form').addEventListener('submit', async e => {
  e.preventDefault();
  const id = new FormData(e.currentTarget).get('patientId').trim();
  try {
    const history = await api('/api/history?patientId=' + encodeURIComponent(id));
    $('#history-subtitle').textContent = `Recent activity for ${id}`;
    $('#history-list').innerHTML = history.length ? history.map(h=>`<div class="history-entry">${escapeHtml(h.entry)}</div>`).join('') : '<p class="empty">No history entries for this patient yet.</p>';
  } catch(err) { notify(err.message); }
});
loadSavedData();
refresh();
