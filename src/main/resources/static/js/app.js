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
    document.getElementById('document-details-heading').textContent = 'Document details';
    loading.classList.remove('d-none');
    error.classList.add('d-none');
    content.classList.add('d-none');
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
        text.textContent = documentContent.content || '';
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
    grid.innerHTML = '<div class="col-12 text-center py-5"><div class="ai-spinner"><div></div><div></div><div></div></div></div>';

    try {
        const res = await apiFetch('/api/knowledge/analytics');
        if (res.status === 403) {
            grid.innerHTML = '<div class="col-12"><div class="alert alert-warning">Analytics is restricted to Admin users.</div></div>';
            return;
        }
        if (!res.ok) throw new Error(`HTTP ${res.status}`);
        const d = await res.json();
        const metrics = [
            { icon: 'bi-folder2-open', label: 'Total Documents', value: d.totalDocuments },
            { icon: 'bi-check-circle', label: 'Indexed Documents', value: d.indexedDocuments },
            { icon: 'bi-search', label: 'Total Searches', value: d.totalSearches },
            { icon: 'bi-chat-dots', label: 'Questions Asked', value: d.totalQuestions },
            { icon: 'bi-lightbulb', label: 'Answered', value: d.answeredQuestions },
            { icon: 'bi-star', label: 'Avg. Feedback Rating', value: d.averageFeedbackRating.toFixed(2) + ' / 5' }
        ];
        grid.innerHTML = metrics.map(m => `
      <div class="col-6 col-md-4 col-xl-2">
        <div class="analytics-card">
          <i class="bi ${m.icon} metric-icon text-primary"></i>
          <div class="metric-value">${m.value}</div>
          <div class="metric-label">${m.label}</div>
        </div>
      </div>`).join('');
    } catch (e) {
        grid.innerHTML = `<div class="col-12"><div class="alert alert-danger">${e.message}</div></div>`;
    }
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
