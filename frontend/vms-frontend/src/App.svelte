<script>
  import Dashboard from './pages/Dashboard.svelte';
  import Personnel from './pages/Personnel.svelte';

  let currentTab = 'dashboard';

  function handleTabChange(e) {
    currentTab = e.detail;
  }
</script>

{#if currentTab === 'personnel'}
  <Personnel activeTab={currentTab} on:changeTab={handleTabChange} />
{:else}
  <Dashboard activeTab={currentTab} on:changeTab={handleTabChange} />
{/if}
