(() => {
  const ADMIN_RESOURCE_API = '/api/admin/resources';
  const ARCHIVE_CONFIRMATION =
    'Archive this approved resource? It will be hidden from public discovery but kept in the system records.';
  const UNARCHIVE_CONFIRMATION = 'Unarchive this resource? It will become approved and visible to viewers again.';

  let resources = [];

  document.addEventListener('DOMContentLoaded', async () => {
    const admin = window.AdminModule;
    admin.bindAdminBasics();
    const currentUser = await admin.requireAdmin();
    if (!currentUser) return;

    bindResourceActions();
    await loadResources();
  });

  function bindResourceActions() {
    const refreshButton = document.getElementById('resourceRefreshBtn');
    if (refreshButton) {
      refreshButton.addEventListener('click', loadResources);
    }

    const filterForm = document.getElementById('resourceFilterForm');
    if (filterForm) {
      filterForm.addEventListener('submit', async (event) => {
        event.preventDefault();
        await loadResources();
      });
    }

    document.addEventListener('click', async (event) => {
      const archiveButton = event.target.closest('[data-admin-resource-archive]');
      if (archiveButton) {
        await archiveResource(Number(archiveButton.dataset.adminResourceArchive), archiveButton);
        return;
      }

      const unarchiveButton = event.target.closest('[data-admin-resource-unarchive]');
      if (unarchiveButton) {
        await unarchiveResource(Number(unarchiveButton.dataset.adminResourceUnarchive), unarchiveButton);
      }
    });
  }

  async function loadResources() {
    const admin = window.AdminModule;
    admin.setState('resourceListPanel', 'Loading resources...');

    try {
      const status = document.getElementById('resourceStatusFilter')?.value || '';
      const query = status ? `?status=${encodeURIComponent(status)}` : '';
      const rows = await admin.requestJson(`${ADMIN_RESOURCE_API}${query}`, { method: 'GET' });
      resources = Array.isArray(rows) ? rows : [];
      renderResources();
    } catch (error) {
      admin.setState('resourceListPanel', admin.getErrorMessage(error, 'Unable to load resources.'), 'error');
    }
  }

  function renderResources() {
    const panel = document.getElementById('resourceListPanel');
    if (!panel) return;
    const admin = window.AdminModule;
    const filteredResources = filterResources(resources);

    panel.innerHTML = `
            <div class="admin-panel-header">
                <div>
                    <h2>Resources</h2>
                    <p>${filteredResources.length} resource${filteredResources.length === 1 ? '' : 's'} shown.</p>
                </div>
            </div>
            <div class="admin-table-wrap">
                <table class="admin-table">
                    <thead>
                        <tr>
                            <th>ID</th>
                            <th>Title</th>
                            <th>Status</th>
                            <th>Archived At</th>
                            <th>Updated At</th>
                            <th>Actions</th>
                        </tr>
                    </thead>
                    <tbody>
                        ${filteredResources.length ? filteredResources.map(renderResourceRow).join('') : admin.emptyRow(6, 'No resources match the selected filters.')}
                    </tbody>
                </table>
            </div>
        `;
  }

  function filterResources(rows) {
    const keyword = String(document.getElementById('resourceSearchInput')?.value || '')
      .trim()
      .toLowerCase();
    if (!keyword) {
      return rows;
