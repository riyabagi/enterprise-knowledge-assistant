/* ══════════════════════════════════════════════════════════
   Enterprise Knowledge Assistant — app.js
   ══════════════════════════════════════════════════════════ */

// ── State ─────────────────────────────────────────────────────────────────
const state = {
    auth: null,
    lastAnswerRecordId: null,
    lastDocumentId: null,
    asking: false,
    searching: false,
    editingDocumentId: null,
    versionDocumentId: null,
};

let csrfToken = null;

// ── Bootstrap refs ────────────────────────────────────────────────────────
let loginModal, uploadModal, deleteModal, detailsModal, editDocumentModal, versionsModal, toastEl;
let pendingDeleteDocumentId = null;

document.addEventListener('DOMContentLoaded', () => {
    loginModal = new bootstrap.Modal(document.getElementById('loginModal'));
    uploadModal = new bootstrap.Modal(document.getElementById('uploadModal'));
    deleteModal = new bootstrap.Modal(document.getElementById('deleteDocumentModal'));
    detailsModal = new bootstrap.Modal(document.getElementById('documentDetailsModal'));
    editDocumentModal = new bootstrap.Modal(document.getElementById('editDocumentModal'));
    versionsModal = new bootstrap.Modal(document.getElementById('documentVersionsModal'));
    toastEl = new bootstrap.Toast(document.getElementById('appToast'), { delay: 3500 });

    // Char counter for ask input
    document.getElementById('ask-input').addEventListener('input', e => {
        document.getElementById('ask-char').textContent = `${e.target.value.length} / 500`;
    });

    // Enter key on ask input
    document.getElementById('ask-input').addEventListener('keydown', e => {
        if (e.key === 'Enter') submitAsk();
    });

    // Enter key on search input
    document.getElementById('search-input').addEventListener('keydown', e => {
        if (e.key === 'Enter') doSearch();
    });

    document.getElementById('analytics-access-role').addEventListener('change', loadRoleAccessReport);

    restoreSession();
});

// ── Auth helpers ──────────────────────────────────────────────────────────
async function fetchCsrfToken() {
    const response = await fetch('/api/auth/csrf', { credentials: 'same-origin' });
    if (!response.ok) throw new Error('Unable to initialize secure sign-in.');
    const data = await response.json();
    csrfToken = data.token;
    return data;
}

async function apiFetch(url, options = {}) {
    const method = (options.method || 'GET').toUpperCase();
    const headers = new Headers(options.headers || {});
    if (!['GET', 'HEAD', 'OPTIONS', 'TRACE'].includes(method)) {
        const token = csrfToken ? { headerName: 'X-CSRF-TOKEN', token: csrfToken } : await fetchCsrfToken();
        headers.set(token.headerName, token.token);
    }

    async function apiErrorMessage(response, fallback) {
        const data = await response.json().catch(() => ({}));
        return data.message || data.details?.[0] || `${fallback} (HTTP ${response.status}).`;
    }

    const response = await fetch(url, { ...options, method, headers, credentials: 'same-origin' });
    if (response.status === 401 && !['/api/auth/login', '/api/auth/csrf', '/api/auth/me'].includes(url)) {
        state.auth = null;
        csrfToken = null;
        updateAuthNav(null);
        showHeroOrMain(false);
        showLoginModal();
        showToast('Your session has expired. Please sign in again.', 'warning');
    }
    return response;
}

async function restoreSession() {
    try {
        const response = await apiFetch('/api/auth/me');
        if (!response.ok) return;
        state.auth = await response.json();
        updateAuthNav(state.auth.username);
        showHeroOrMain(true);
        showTab('ask');
    } catch (e) {
        showToast(e.message, 'danger');
    }
}

function showLoginModal() { loginModal.show(); }
function showUploadModal() {
    if (!state.auth) { showToast('Please sign in first.', 'warning'); showLoginModal(); return; }
    uploadModal.show();
}

async function doLogin() {
    const username = document.getElementById('login-username').value.trim();
    const password = document.getElementById('login-password').value;
    const err = document.getElementById('login-error');
    err.classList.add('d-none');

    try {
        await fetchCsrfToken();
        const res = await apiFetch('/api/auth/login', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ username, password })
        });
        const data = await res.json().catch(() => ({}));
        if (!res.ok) {
            err.textContent = data.message || 'Sign-in failed. Please try again.';
            err.classList.remove('d-none');
            return;
        }
        state.auth = data;
        csrfToken = null;
        loginModal.hide();
        updateAuthNav(username);
        showHeroOrMain(true);
        showTab('ask');
        showToast(`Welcome back, ${username}!`, 'success');
    } catch (e) {
        err.textContent = e.message || 'Network error. Is the server running?';
        err.classList.remove('d-none');
    }
}

async function doLogout() {
    try {
        const res = await apiFetch('/api/auth/logout', { method: 'POST' });
        if (!res.ok) throw new Error('Sign-out failed. Please try again.');
        state.auth = null;
        csrfToken = null;
        state.lastAnswerRecordId = null;
        state.lastDocumentId = null;
        updateAuthNav(null);
        showHeroOrMain(false);
        showToast('Signed out.', 'info');
    } catch (e) {
        showToast(e.message, 'danger');
    }
}

function updateAuthNav(username) {
    const ctrl = document.getElementById('auth-controls');
    if (username) {
        ctrl.innerHTML = `
      <span class="text-light small me-2"><i class="bi bi-person-circle me-1"></i>${username}</span>
      <button class="btn btn-sm btn-outline-danger" onclick="doLogout()"><i class="bi bi-box-arrow-right"></i></button>`;
    } else {
        ctrl.innerHTML = `<button class="btn btn-sm btn-outline-light" onclick="showLoginModal()"><i class="bi bi-person me-1"></i>Sign In</button>`;
    }
}

function showHeroOrMain(loggedIn) {
    document.getElementById('hero').style.display = loggedIn ? 'none' : '';
    document.getElementById('main-content').style.display = loggedIn ? '' : 'none';
}

// ── Tab navigation ────────────────────────────────────────────────────────
function showTab(tab) {
    if (!state.auth) {
        showLoginModal();
        return;
    }
    ['ask', 'search', 'docs', 'analytics'].forEach(t => {
        document.getElementById(`tab-${t}`).style.display = t === tab ? '' : 'none';
    });
    if (tab === 'docs') loadDocuments();
    if (tab === 'analytics') loadAnalytics();
}

// ── Ask / RAG ─────────────────────────────────────────────────────────────
async function submitAsk() {
    if (!state.auth) { showLoginModal(); return; }
    if (state.asking) return;
    const question = document.getElementById('ask-input').value.trim();
    if (!question) return;

    const askResult = document.getElementById('ask-result');
    const loadingEl = document.getElementById('ask-loading');
    const askBtn = document.getElementById('ask-btn');
    state.asking = true;

    askResult.style.display = 'none';
    loadingEl.style.display = 'block';
    askBtn.disabled = true;

    try {
        const res = await apiFetch('/api/knowledge/ask', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ question })
        });

        if (!res.ok) {
            throw new Error(await apiErrorMessage(res, 'The answer request failed'));
        }

        const data = await res.json();
        renderAskResult(data);
        state.lastAnswerRecordId = data.answerRecordId;
    } catch (e) {
        showToast(`Error: ${e.message}`, 'danger');
    } finally {
        loadingEl.style.display = 'none';
        askBtn.disabled = false;
        state.asking = false;
    }
}

function renderAskResult(data) {
    const statusDot = document.getElementById('status-dot');
    const statusLabel = document.getElementById('answer-status-label');
    const answerText = document.getElementById('answer-text');
    const chunksBadge = document.getElementById('chunks-badge');
    const citSection = document.getElementById('citations-section');
    const citList = document.getElementById('citations-list');
    const fbPrompt = document.getElementById('feedback-prompt');

    // Status styling
    const statusMap = {
        ANSWERED: { cls: '', dot: '', label: 'Answer' },
        INSUFFICIENT_EVIDENCE: { cls: 'warning', dot: 'warning', label: 'Insufficient Evidence' },
        CONFLICTING_EVIDENCE: { cls: 'warning', dot: 'warning', label: 'Conflicting Evidence' },
        AI_UNAVAILABLE: { cls: 'danger', dot: 'danger', label: 'AI Unavailable' }
    };
    const s = statusMap[data.status] || statusMap.ANSWERED;
    statusDot.className = `status-dot ${s.dot}`;
    statusLabel.textContent = s.label;

    answerText.textContent = data.answer || 'No answer available.';
    chunksBadge.className = 'ms-auto badge bg-secondary';
    chunksBadge.textContent = `${data.evidenceChunksUsed} chunk${data.evidenceChunksUsed !== 1 ? 's' : ''} used`;

    // Citations
    if (data.citations && data.citations.length > 0) {
        citList.innerHTML = data.citations.map(c => `
      <div class="citation-card">
        <div class="doc-title"><i class="bi bi-file-text me-1"></i>[${c.citationIndex}] ${escHtml(c.documentTitle)}</div>
        <div class="text-muted small mb-1">Version ${c.versionNumber || 1}${c.sectionReference ? ' · ' + escHtml(c.sectionReference) : ''}</div>
        <div class="excerpt">"${escHtml(c.excerpt || '')}"</div>
      </div>`).join('');
        citSection.style.display = '';
        state.lastDocumentId = data.citations[0].documentId;
    } else {
        citSection.style.display = 'none';
    }

    fbPrompt.style.display = data.status === 'ANSWERED' ? '' : 'none';
    document.getElementById('ask-result').style.display = '';
}

async function quickFeedback(rating) {
    if (!state.lastDocumentId) return;
    try {
        const res = await apiFetch('/api/knowledge/feedback', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                documentId: state.lastDocumentId,
                answerRecordId: state.lastAnswerRecordId,
                rating
            })
        });
        if (!res.ok) {
            const err = await res.json().catch(() => ({}));
            throw new Error(err.message || `HTTP ${res.status}`);
        }
        showToast('Feedback submitted. Thank you!', 'success');
        document.getElementById('feedback-prompt').style.display = 'none';
    } catch (e) {
        showToast(`Failed to submit feedback: ${e.message}`, 'danger');
    }
}

// ── Search ────────────────────────────────────────────────────────────────
async function doSearch() {
    if (!state.auth) { showLoginModal(); return; }
    if (state.searching) return;
    const query = document.getElementById('search-input').value.trim();
    if (!query) return;
    const category = document.getElementById('search-category').value;
    const container = document.getElementById('search-results');
    container.innerHTML = '<div class="text-center py-5"><div class="ai-spinner"><div></div><div></div><div></div></div></div>';
    state.searching = true;

    const params = new URLSearchParams({ query, size: 10 });
    if (category) params.set('category', category);

    try {
        const res = await apiFetch(`/api/knowledge/search?${params}`);
        if (!res.ok) throw new Error(await apiErrorMessage(res, 'Search failed'));
        const data = await res.json();
        renderSearchResults(data);
    } catch (e) {
        container.innerHTML = `<div class="alert alert-danger">${escHtml(e.message)}</div>`;
    } finally {
        state.searching = false;
    }
}

function renderSearchResults(data) {
    const container = document.getElementById('search-results');
    if (!data.results || data.results.length === 0) {
        container.innerHTML = `<div class="empty-state"><i class="bi bi-search"></i><p>No results found for "<strong>${escHtml(data.query)}</strong>"</p></div>`;
        return;
    }
    container.innerHTML = `
    <p class="text-muted small mb-3">${data.totalResults} result${data.totalResults !== 1 ? 's' : ''} for "<strong>${escHtml(data.query)}</strong>"</p>
    <div class="d-flex flex-column gap-3">
      ${data.results.map(r => `
        <div class="search-result-item">
          <div class="d-flex align-items-center gap-2 mb-2 flex-wrap">
            <strong>${escHtml(r.documentTitle)}</strong>
            <span class="result-category">${escHtml(r.category)}</span>
            <span class="result-access">${escHtml(r.accessLevel)}</span>
            ${r.versionNumber ? `<span class="text-muted small">v${r.versionNumber}</span>` : ''}
          </div>
          ${r.sectionReference ? `<div class="text-muted small mb-1"><i class="bi bi-bookmark me-1"></i>${escHtml(r.sectionReference)}</div>` : ''}
          <div class="result-snippet">${escHtml(r.snippet || '')}</div>
          <button class="btn btn-sm btn-outline-light mt-2" onclick="showDocumentDetails(${Number(r.documentId)})">
            <i class="bi bi-book me-1"></i>Read document
          </button>
        </div>`).join('')}
    </div>`;
}

// ── Documents ─────────────────────────────────────────────────────────────
async function loadDocuments() {
    if (!state.auth) return;
    const container = document.getElementById('docs-list');
    const category = document.getElementById('doc-category-filter').value;
    container.innerHTML = '<div class="col-12 text-center py-5"><div class="ai-spinner"><div></div><div></div><div></div></div></div>';

    const params = new URLSearchParams({ size: 20 });
    if (category) params.set('category', category);

    try {
        const res = await apiFetch(`/api/knowledge/documents?${params}`);
        if (!res.ok) throw new Error(await apiErrorMessage(res, 'Could not load documents'));
        const page = await res.json();
        renderDocuments(page.content || []);
    } catch (e) {
        container.innerHTML = `<div class="col-12"><div class="alert alert-danger">${escHtml(e.message)}</div></div>`;
    }
}

function renderDocuments(docs) {
    const container = document.getElementById('docs-list');
    if (!docs.length) {
        container.innerHTML = `<div class="col-12"><div class="empty-state"><i class="bi bi-folder2"></i><p>No documents found. Upload your first document.</p></div></div>`;
        return;
    }
    container.innerHTML = docs.map(d => `
    <div class="col-12 col-md-6 col-xl-4">
      <div class="doc-card">
        <div class="d-flex justify-content-between align-items-start mb-2">
          <span class="doc-status-badge ${d.status.toLowerCase()}">${d.status}</span>
          <span class="text-muted small">${d.category}</span>
        </div>
        <h6 class="mt-2 mb-1 fw-bold">${escHtml(d.title)}</h6>
        <p class="text-muted small mb-2">${escHtml(d.description || 'No description')}</p>
        <div class="d-flex gap-2 flex-wrap">
          <span class="badge bg-secondary">${escHtml(d.accessLevel)}</span>
          ${d.currentVersion ? `<span class="badge bg-dark border border-secondary">v${d.currentVersion}</span>` : ''}
        </div>
        <div class="text-muted small mt-2">${d.originalFilename ? escHtml(d.originalFilename) : ''}</div>
        <div class="d-flex justify-content-end mt-3 gap-2 flex-wrap">
          <button class="btn btn-sm btn-outline-light" onclick="showDocumentDetails(${Number(d.id)})">
            <i class="bi bi-book me-1"></i>Read
          </button>
          ${state.auth.role === 'ADMIN' ? `
          <button class="btn btn-sm btn-outline-primary" onclick="showEditDocument(${Number(d.id)})">
            <i class="bi bi-pencil me-1"></i>Edit
          </button>
          <button class="btn btn-sm btn-outline-info" onclick="showDocumentVersions(${Number(d.id)})">
            <i class="bi bi-clock-history me-1"></i>Versions
          </button>
          <button class="btn btn-sm btn-outline-danger" data-document-title="${escHtml(d.title)}"
                  onclick="confirmDeleteDocument(${Number(d.id)}, this.dataset.documentTitle)">
            <i class="bi bi-trash me-1"></i>Delete
          </button>` : ''}
        </div>
      </div>
    </div>`).join('');
}

async function showDocumentDetails(id) {
    const loading = document.getElementById('document-details-loading');
    const error = document.getElementById('document-details-error');
    const content = document.getElementById('document-details-content');
    const text = document.getElementById('document-details-text');
    const empty = document.getElementById('document-details-empty');
    document.getElementById('document-details-heading').textContent = 'Document details';
    loading.classList.remove('d-none');
    error.classList.add('d-none');
    content.classList.add('d-none');
    empty.classList.add('d-none');
    text.classList.add('d-none');
    text.textContent = '';
    detailsModal.show();

    try {
        const [detailsResponse, contentResponse] = await Promise.all([
            apiFetch(`/api/knowledge/documents/${id}`),
            apiFetch(`/api/knowledge/documents/${id}/content`)
        ]);
        if (!detailsResponse.ok) throw new Error(await apiErrorMessage(detailsResponse, 'Could not load document details'));
        if (!contentResponse.ok) throw new Error(await apiErrorMessage(contentResponse, 'Could not load document text'));

        const details = await detailsResponse.json();
        const documentContent = await contentResponse.json();
        document.getElementById('document-details-heading').textContent = details.title;
        document.getElementById('document-details-description').textContent = details.description || 'No description';
        document.getElementById('document-details-category').textContent = details.category || '';
        document.getElementById('document-details-access').textContent = details.accessLevel || '';
        document.getElementById('document-details-owner').textContent = details.ownerUsername || '';
        document.getElementById('document-details-version').textContent = details.currentVersion ? `v${details.currentVersion}` : 'None';
        document.getElementById('document-details-filename').textContent = details.originalFilename || 'Not available';
        const documentText = documentContent.content || '';
        text.textContent = documentText;
        text.classList.toggle('d-none', !documentText.trim());
        empty.classList.toggle('d-none', Boolean(documentText.trim()));
        content.classList.remove('d-none');
    } catch (e) {
        error.textContent = e.message || 'Unable to read this document.';
        error.classList.remove('d-none');
    } finally {
        loading.classList.add('d-none');
    }
}

async function showEditDocument(id) {
    const error = document.getElementById('edit-document-error');
    error.classList.add('d-none');
    state.editingDocumentId = id;
    try {
        const response = await apiFetch(`/api/knowledge/documents/${id}`);
        if (!response.ok) throw new Error(await apiErrorMessage(response, 'Could not load document metadata'));
        const doc = await response.json();
        document.getElementById('edit-document-title').value = doc.title || '';
        document.getElementById('edit-document-description').value = doc.description || '';
        document.getElementById('edit-document-category').value = doc.category;
        document.getElementById('edit-document-access').value = doc.accessLevel;
        editDocumentModal.show();
    } catch (e) {
        showToast(e.message || 'Unable to load document metadata.', 'danger');
    }
}

async function saveDocumentMetadata() {
    if (state.editingDocumentId === null) return;
    const button = document.getElementById('save-document-metadata');
    const error = document.getElementById('edit-document-error');
    const title = document.getElementById('edit-document-title').value.trim();
    const description = document.getElementById('edit-document-description').value.trim();
    if (!title) {
        error.textContent = 'Title is required.';
        error.classList.remove('d-none');
        return;
    }

    button.disabled = true;
    error.classList.add('d-none');
    try {
        const response = await apiFetch(`/api/knowledge/documents/${state.editingDocumentId}`, {
            method: 'PUT',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                title,
                description,
                category: document.getElementById('edit-document-category').value,
                accessLevel: document.getElementById('edit-document-access').value
            })
        });
        if (!response.ok) throw new Error(await apiErrorMessage(response, 'Could not update metadata'));
        editDocumentModal.hide();
        await loadDocuments();
        showToast('Document metadata updated.', 'success');
    } catch (e) {
        error.textContent = e.message || 'Could not update document metadata.';
        error.classList.remove('d-none');
    } finally {
        button.disabled = false;
    }
}

async function showDocumentVersions(id) {
    state.versionDocumentId = id;
    document.getElementById('document-version-error').classList.add('d-none');
    document.getElementById('new-document-version-file').value = '';
    document.getElementById('document-versions-heading').textContent = 'Document versions';
    document.getElementById('document-versions-list').innerHTML =
        '<div class="text-center py-3"><div class="ai-spinner"><div></div><div></div><div></div></div></div>';
    versionsModal.show();
    await loadDocumentVersions();
}

async function loadDocumentVersions() {
    const list = document.getElementById('document-versions-list');
    try {
        const [detailsResponse, versionsResponse] = await Promise.all([
            apiFetch(`/api/knowledge/documents/${state.versionDocumentId}`),
            apiFetch(`/api/knowledge/documents/${state.versionDocumentId}/versions`)
        ]);
        if (!detailsResponse.ok) throw new Error(await apiErrorMessage(detailsResponse, 'Could not load document'));
        if (!versionsResponse.ok) throw new Error(await apiErrorMessage(versionsResponse, 'Could not load versions'));
        const doc = await detailsResponse.json();
        const versions = await versionsResponse.json();
        document.getElementById('document-versions-heading').textContent = `Versions — ${doc.title}`;
        list.innerHTML = versions.length ? `
          <div class="table-responsive"><table class="table table-dark table-striped align-middle">
            <thead><tr><th>Version</th><th>Status</th><th>File</th><th>Added</th><th>Added by</th></tr></thead>
            <tbody>${versions.map(v => `<tr>
              <td>v${Number(v.versionNumber)}</td><td>${escHtml(v.status)}</td>
              <td>${escHtml(v.originalFilename || '')}</td>
              <td>${escHtml(v.createdAt || '')}</td><td>${escHtml(v.createdByUsername || '')}</td>
            </tr>`).join('')}</tbody>
          </table></div>` : '<p class="text-muted">No versions are available.</p>';
    } catch (e) {
        list.innerHTML = `<div class="alert alert-danger">${escHtml(e.message)}</div>`;
    }
}

async function uploadDocumentVersion() {
    if (state.versionDocumentId === null) return;
    const input = document.getElementById('new-document-version-file');
    const file = input.files[0];
    const button = document.getElementById('upload-document-version');
    const progress = document.getElementById('document-version-progress');
    const error = document.getElementById('document-version-error');
    error.classList.add('d-none');
    if (!file) {
        error.textContent = 'Choose a file to upload.';
        error.classList.remove('d-none');
        return;
    }
    if (file.size > 50 * 1024 * 1024) {
        error.textContent = 'The file exceeds the 50 MB limit.';
        error.classList.remove('d-none');
        return;
    }
    if (!/\.(pdf|txt|md)$/i.test(file.name)) {
        error.textContent = 'Only PDF, TXT, and MD files are supported.';
        error.classList.remove('d-none');
        return;
    }

    const data = new FormData();
    data.append('file', file);
    button.disabled = true;
    progress.classList.remove('d-none');
    try {
        const response = await apiFetch(
            `/api/knowledge/documents/${state.versionDocumentId}/reindex`,
            { method: 'PUT', body: data });
        if (!response.ok) throw new Error(await apiErrorMessage(response, 'Could not upload new version'));
        input.value = '';
        await Promise.all([loadDocumentVersions(), loadDocuments()]);
        showToast('New document version indexed successfully.', 'success');
    } catch (e) {
        error.textContent = e.message || 'Could not upload a new version.';
        error.classList.remove('d-none');
    } finally {
        button.disabled = false;
        progress.classList.add('d-none');
    }
}

function confirmDeleteDocument(id, title) {
    pendingDeleteDocumentId = id;
    document.getElementById('delete-document-title').textContent = title;
    document.getElementById('delete-document-error').classList.add('d-none');
    document.getElementById('confirm-delete-document').disabled = false;
    deleteModal.show();
}

async function deleteDocument() {
    if (pendingDeleteDocumentId === null) return;

    const button = document.getElementById('confirm-delete-document');
    const error = document.getElementById('delete-document-error');
    button.disabled = true;
    error.classList.add('d-none');

    try {
        const response = await apiFetch(
            `/api/knowledge/documents/${pendingDeleteDocumentId}`,
            { method: 'DELETE' });
        if (!response.ok) {
            const body = await response.json().catch(() => ({}));
            throw new Error(body.message || `Could not delete document (HTTP ${response.status}).`);
        }

        deleteModal.hide();
        pendingDeleteDocumentId = null;
        await loadDocuments();
        showToast('Document deleted successfully.', 'success');
    } catch (e) {
        error.textContent = e.message || 'Could not delete document. Please try again.';
        error.classList.remove('d-none');
        button.disabled = false;
    }
}

async function doUpload() {
    const title = document.getElementById('up-title').value.trim();
    const desc = document.getElementById('up-desc').value.trim();
    const category = document.getElementById('up-cat').value;
    const access = document.getElementById('up-access').value;
    const file = document.getElementById('up-file').files[0];
    const errEl = document.getElementById('upload-error');
    const progEl = document.getElementById('upload-progress');
    errEl.classList.add('d-none');

    if (!title) { errEl.textContent = 'Title is required.'; errEl.classList.remove('d-none'); return; }
    if (!file) { errEl.textContent = 'Please select a file.'; errEl.classList.remove('d-none'); return; }

    const fd = new FormData();
    fd.append('file', file);
    fd.append('title', title);
    fd.append('description', desc);
    fd.append('category', category);
    fd.append('accessLevel', access);

    progEl.classList.remove('d-none');
    try {
        const res = await apiFetch('/api/knowledge/documents', {
            method: 'POST',
            body: fd
        });
        if (!res.ok) {
            const err = await res.json().catch(() => ({}));
            throw new Error(err.message || `HTTP ${res.status}`);
        }
        uploadModal.hide();
        showToast('Document uploaded and indexed successfully!', 'success');
        loadDocuments();
    } catch (e) {
        errEl.textContent = e.message;
        errEl.classList.remove('d-none');
    } finally {
        progEl.classList.add('d-none');
    }
}

// ── Analytics ─────────────────────────────────────────────────────────────
async function loadAnalytics() {
    if (!state.auth) return;
    const grid = document.getElementById('analytics-grid');
    const reports = document.getElementById('analytics-reports');
    const accessReport = document.getElementById('analytics-access-report');
    const refreshButton = document.getElementById('analytics-refresh-btn');
    const status = document.getElementById('analytics-status');
    const role = document.getElementById('analytics-access-role').value;

    refreshButton.disabled = true;
    refreshButton.setAttribute('aria-busy', 'true');
    document.getElementById('analytics-content').setAttribute('aria-busy', 'true');
    status.innerHTML = '<span class="spinner-border spinner-border-sm me-2" role="status" aria-label="Loading"></span>Loading analytics data…';
    grid.innerHTML = '';
    reports.innerHTML = '';
    accessReport.innerHTML = '<p class="analytics-empty">Loading role access…</p>';

    try {
        const [response, accessResponse] = await Promise.all([
            apiFetch('/api/knowledge/analytics'),
            apiFetch(`/api/knowledge/analytics/access?role=${encodeURIComponent(role)}`)
        ]);
        if (response.status === 403) {
            status.innerHTML = '<div class="analytics-error"><i class="bi bi-shield-lock" aria-hidden="true"></i><span>Analytics is restricted to Admin users.</span></div>';
            accessReport.innerHTML = '';
            return;
        }
        if (!response.ok) throw new Error(`Unable to load analytics (HTTP ${response.status}).`);

        const data = await response.json();
        const feedbackRows = Array.isArray(data.feedbackByDocument) ? data.feedbackByDocument : [];
        const feedbackCount = feedbackRows.reduce(
            (total, row) => total + safeAnalyticsNumber(row.feedbackCount), 0);
        const averageRatingValue = data.averageFeedbackRating;
        const parsedAverageRating = averageRatingValue === null || averageRatingValue === undefined || averageRatingValue === ''
            ? null : Number(averageRatingValue);
        const averageRating = feedbackCount > 0 && parsedAverageRating !== null && Number.isFinite(parsedAverageRating)
            ? parsedAverageRating : null;
        const metrics = [
            { icon: 'bi-folder2', label: 'Total documents', value: formatAnalyticsNumber(data.totalDocuments), description: 'Documents in the knowledge base', tone: 'blue' },
            { icon: 'bi-check2-circle', label: 'Indexed documents', value: formatAnalyticsNumber(data.indexedDocuments), description: 'Available to search and retrieve', tone: 'green' },
            { icon: 'bi-search', label: 'Total searches', value: formatAnalyticsNumber(data.totalSearches), description: 'Recorded search requests', tone: 'violet' },
            { icon: 'bi-chat-square-text', label: 'AI questions', value: formatAnalyticsNumber(data.totalQuestions), description: 'Questions submitted to Ask AI', tone: 'amber' },
            { icon: 'bi-patch-check', label: 'Answered questions', value: formatAnalyticsNumber(data.answeredQuestions), description: 'Answers supported by evidence', tone: 'teal' },
            { icon: 'bi-star', label: 'Average feedback', value: averageRating === null ? 'No ratings' : `${averageRating.toFixed(2)} / 5`, description: averageRating === null ? 'No feedback has been recorded' : `${formatAnalyticsNumber(feedbackCount)} feedback responses`, tone: 'rose' }
        ];
        grid.innerHTML = metrics.map(renderAnalyticsMetric).join('');
        renderAnalyticsReports(data);

        if (!accessResponse.ok) {
            accessReport.innerHTML = `<div class="analytics-error"><i class="bi bi-exclamation-circle" aria-hidden="true"></i><span>${escHtml(`Unable to load role access (HTTP ${accessResponse.status}).`)}</span></div>`;
        } else {
            renderRoleAccessReport(await accessResponse.json());
        }
        status.textContent = 'Analytics data loaded.';
    } catch (error) {
        status.innerHTML = `<div class="analytics-error"><i class="bi bi-exclamation-circle" aria-hidden="true"></i><span>${escHtml(error.message || 'Analytics could not be loaded. Please try again.')}</span></div>`;
        grid.innerHTML = '';
        reports.innerHTML = '';
        accessReport.innerHTML = '<p class="analytics-empty">The role access report is unavailable until analytics data loads.</p>';
    } finally {
        refreshButton.disabled = false;
        refreshButton.removeAttribute('aria-busy');
        document.getElementById('analytics-content').removeAttribute('aria-busy');
    }
}

async function loadRoleAccessReport() {
    if (!state.auth) return;
    const role = document.getElementById('analytics-access-role').value;
    const container = document.getElementById('analytics-access-report');
    container.innerHTML = '<p class="analytics-empty">Loading role access…</p>';
    try {
        const response = await apiFetch(`/api/knowledge/analytics/access?role=${encodeURIComponent(role)}`);
        if (!response.ok) throw new Error(`Unable to load role access (HTTP ${response.status}).`);
        renderRoleAccessReport(await response.json());
    } catch (error) {
        container.innerHTML = `<div class="analytics-error"><i class="bi bi-exclamation-circle" aria-hidden="true"></i><span>${escHtml(error.message || 'The role access report could not be loaded.')}</span></div>`;
    }
}

function safeAnalyticsNumber(value) {
    if (value === null || value === undefined || value === '') return 0;
    const number = Number(value);
    return Number.isFinite(number) ? number : 0;
}

function formatAnalyticsNumber(value) {
    if (value === null || value === undefined || value === '') return '—';
    const number = Number(value);
    return Number.isFinite(number) ? new Intl.NumberFormat().format(number) : '—';
}

function renderAnalyticsMetric(metric) {
    return `<article class="analytics-kpi-card">
        <div class="analytics-kpi-top">
          <span class="analytics-kpi-icon ${metric.tone}"><i class="bi ${metric.icon}" aria-hidden="true"></i></span>
          <span class="analytics-kpi-label">${escHtml(metric.label)}</span>
        </div>
        <div class="analytics-kpi-value">${escHtml(metric.value)}</div>
        <p class="analytics-kpi-description">${escHtml(metric.description)}</p>
      </article>`;
}

function renderAnalyticsReports(data) {
    const reports = document.getElementById('analytics-reports');
    const categoryStatusRows = Array.isArray(data.documentCountsByCategoryAndStatus)
        ? data.documentCountsByCategoryAndStatus : [];
    const pendingRows = Array.isArray(data.pendingReindexDocuments) ? data.pendingReindexDocuments : [];
    const searchRows = Array.isArray(data.mostFrequentlySearchedCategories)
        ? data.mostFrequentlySearchedCategories : [];
    const feedbackRows = Array.isArray(data.feedbackByDocument) ? data.feedbackByDocument : [];

    const table = (headers, rows, emptyMessage) => rows.length
        ? `<div class="table-responsive"><table class="table analytics-table align-middle mb-0">
            <thead><tr>${headers.map(header => `<th scope="col">${header}</th>`).join('')}</tr></thead>
            <tbody>${rows.join('')}</tbody></table></div>`
        : `<p class="analytics-empty">${escHtml(emptyMessage)}</p>`;

    const maxSearchCount = Math.max(0, ...searchRows.map(row => safeAnalyticsNumber(row.searchCount)));
    const searchChart = searchRows.length
        ? `<div class="analytics-bars" role="list" aria-label="Searches by selected category">${searchRows.map(row => {
            const count = safeAnalyticsNumber(row.searchCount);
            const width = maxSearchCount > 0 ? Math.round((count / maxSearchCount) * 100) : 0;
            return `<div class="analytics-bar-row" role="listitem">
                <div class="analytics-bar-label"><span>${escHtml(row.category || 'Unspecified')}</span><strong>${formatAnalyticsNumber(count)}</strong></div>
                <div class="analytics-bar-track" role="img" aria-label="${escHtml(`${row.category || 'Unspecified'}: ${count} searches`)}"><span style="width:${width}%"></span></div>
              </div>`;
        }).join('')}</div>`
        : '<p class="analytics-empty">No category-filtered searches have been recorded.</p>';

    const ratingTotals = [1, 2, 3, 4, 5].map(rating => feedbackRows.reduce(
        (sum, row) => sum + safeAnalyticsNumber(row[`${['', 'one', 'two', 'three', 'four', 'five'][rating]}StarCount`]), 0));
    const maxRatingCount = Math.max(0, ...ratingTotals);
    const ratingChart = maxRatingCount > 0
        ? `<div class="analytics-bars" role="list" aria-label="Feedback rating distribution">${ratingTotals.map((count, index) => {
            const rating = index + 1;
            const width = Math.round((count / maxRatingCount) * 100);
            return `<div class="analytics-bar-row" role="listitem">
                <div class="analytics-bar-label"><span>${rating} ${rating === 1 ? 'star' : 'stars'}</span><strong>${formatAnalyticsNumber(count)}</strong></div>
                <div class="analytics-bar-track rating-track" role="img" aria-label="${rating} stars: ${count} responses"><span style="width:${width}%"></span></div>
              </div>`;
        }).join('')}</div>`
        : '<p class="analytics-empty">No feedback ratings have been recorded.</p>';

    const categoryTable = table(
        ['Category', 'Status', 'Documents'],
        categoryStatusRows.map(row => `<tr><td>${escHtml(row.category)}</td><td><span class="analytics-status-pill">${escHtml(row.status)}</span></td><td>${formatAnalyticsNumber(row.documentCount)}</td></tr>`),
        'No documents are recorded.'
    );
    const pendingTable = table(
        ['Document', 'Category', 'Status', 'Re-index required', 'Access', ''],
        pendingRows.map(row => {
            const id = Number(row.id);
            const action = Number.isSafeInteger(id)
                ? `<button class="btn btn-sm analytics-table-action" onclick="showDocumentVersions(${id})" aria-label="${escHtml(`Open versions for ${row.title || 'document'}`)}">Open versions</button>`
                : '—';
            return `<tr><td class="analytics-document-name">${escHtml(row.title || 'Untitled document')}</td><td>${escHtml(row.category)}</td><td><span class="analytics-status-pill">${escHtml(row.status)}</span></td><td>${row.reindexRequired === true ? 'Yes' : 'No'}</td><td>${escHtml(row.accessLevel)}</td><td>${action}</td></tr>`;
        }),
        'No documents are currently flagged as requiring re-indexing.'
    );
    const feedbackTable = table(
        ['Document', 'Responses', 'Average rating', '1★', '2★', '3★', '4★', '5★'],
        feedbackRows.map(row => {
            const count = safeAnalyticsNumber(row.feedbackCount);
            const average = count > 0 && row.averageRating !== null && row.averageRating !== undefined
                ? Number(row.averageRating) : null;
            const averageText = average !== null && Number.isFinite(average) ? `${average.toFixed(2)} / 5` : 'No ratings';
            return `<tr><td class="analytics-document-name">${escHtml(row.documentTitle || 'Untitled document')}</td><td>${formatAnalyticsNumber(count)}</td><td>${escHtml(averageText)}</td><td>${formatAnalyticsNumber(row.oneStarCount)}</td><td>${formatAnalyticsNumber(row.twoStarCount)}</td><td>${formatAnalyticsNumber(row.threeStarCount)}</td><td>${formatAnalyticsNumber(row.fourStarCount)}</td><td>${formatAnalyticsNumber(row.fiveStarCount)}</td></tr>`;
        }),
        'No documents are recorded.'
    );

    const panels = [
        { title: 'Searches by category', description: 'Searches with an explicitly selected category filter.', body: searchChart, className: 'analytics-panel-chart' },
        { title: 'Feedback rating distribution', description: 'Recorded ratings across documents, grouped by star value.', body: ratingChart, className: 'analytics-panel-chart' },
        { title: 'Documents by category and status', description: 'Current document inventory grouped by its stored category and status.', body: categoryTable, className: 'analytics-panel-wide' },
        { title: 'Documents awaiting re-indexing', description: 'Documents explicitly marked by the existing re-index-required flag.', body: pendingTable, className: 'analytics-panel-wide' },
        { title: 'Feedback by document', description: 'Per-document response counts, averages, and rating distribution.', body: feedbackTable, className: 'analytics-panel-wide' }
    ];

    reports.innerHTML = panels.map(panel => `<section class="analytics-panel ${panel.className}">
        <div class="analytics-panel-header">
          <div><h2>${panel.title}</h2><p>${panel.description}</p></div>
        </div>
        <div class="analytics-table-wrap">${panel.body}</div>
      </section>`).join('');
}

function renderRoleAccessReport(report) {
    const container = document.getElementById('analytics-access-report');
    const documents = Array.isArray(report.documents) ? report.documents : [];
    if (!documents.length) {
        container.innerHTML = `<p class="analytics-empty">No indexed documents are accessible to ${escHtml(report.role || 'this role')}.</p>`;
        return;
    }
    container.innerHTML = `<div class="table-responsive"><table class="table analytics-table align-middle mb-0">
        <thead><tr><th scope="col">Document</th><th scope="col">Category</th><th scope="col">Access level</th><th scope="col">Status</th></tr></thead>
        <tbody>${documents.map(document => `<tr><td class="analytics-document-name">${escHtml(document.title || 'Untitled document')}</td><td>${escHtml(document.category)}</td><td>${escHtml(document.accessLevel)}</td><td><span class="analytics-status-pill">${escHtml(document.status)}</span></td></tr>`).join('')}</tbody>
      </table></div>`;
}

// ── Helpers ───────────────────────────────────────────────────────────────
function showToast(msg, type = 'info') {
    const el = document.getElementById('appToast');
    el.className = `toast align-items-center border-0 text-bg-${type === 'success' ? 'success' : type === 'danger' ? 'danger' : type === 'warning' ? 'warning' : 'secondary'}`;
    document.getElementById('toast-body').textContent = msg;
    toastEl.show();
}

function escHtml(str) {
    if (!str) return '';
    return String(str)
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;')
        .replace(/'/g, '&#039;');
}
