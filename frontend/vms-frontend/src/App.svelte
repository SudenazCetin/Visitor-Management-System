<script>
  import Dashboard from './pages/Dashboard.svelte';
  import Personnel from './pages/Personnel.svelte';
  import UserManagement from './pages/UserManagement.svelte';

  let currentTab = 'dashboard';

  function handleTabChange(e) {
    currentTab = e.detail;
  }
</script>

{#if currentTab === 'personnel'}
  <Personnel activeTab={currentTab} on:changeTab={handleTabChange} />
{:else if currentTab === 'users'}
  <UserManagement activeTab={currentTab} on:changeTab={handleTabChange} />
{:else}
  <Dashboard activeTab={currentTab} on:changeTab={handleTabChange} />
{/if}
