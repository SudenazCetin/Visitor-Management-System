<script>
  import { authStore } from './stores/authStore.js';
  import ToastContainer from './components/ToastContainer.svelte';
  import Login from './pages/Login.svelte';
  import Register from './pages/Register.svelte';
  import Dashboard from './pages/Dashboard.svelte';
  import Personnel from './pages/Personnel.svelte';
  import UserManagement from './pages/UserManagement.svelte';
  import Reports from './pages/Reports.svelte';
  import PersonnelDashboard from './pages/PersonnelDashboard.svelte';
  import MyVisitors from './pages/MyVisitors.svelte';
  import Profile from './pages/Profile.svelte';

  let publicView = 'login';
  let currentTab = 'dashboard';

  function handleTabChange(e) {
    currentTab = e.detail;
  }

  // Reactive role-based route protection
  $: if ($authStore.isAuthenticated && $authStore.user) {
    const role = $authStore.user.role;
    if (role === 'PERSONNEL') {
      if (['dashboard', 'personnel', 'users', 'reports'].includes(currentTab)) {
        currentTab = 'my-dashboard';
      }
    } else {
      if (['my-dashboard', 'my-visitors', 'profile'].includes(currentTab)) {
        currentTab = 'dashboard';
      } else if (role !== 'ADMIN' && currentTab === 'users') {
        currentTab = 'dashboard';
      }
    }
  }
</script>

<!-- Global Toast Container -->
<ToastContainer />

{#if !$authStore.isAuthenticated}
  {#if publicView === 'register'}
    <Register on:switchToLogin={() => (publicView = 'login')} />
  {:else}
    <Login on:switchToRegister={() => (publicView = 'register')} />
  {/if}
{:else}
  {#if currentTab === 'my-dashboard'}
    <PersonnelDashboard activeTab={currentTab} on:changeTab={handleTabChange} />
  {:else if currentTab === 'my-visitors'}
    <MyVisitors activeTab={currentTab} on:changeTab={handleTabChange} />
  {:else if currentTab === 'profile'}
    <Profile activeTab={currentTab} on:changeTab={handleTabChange} />
  {:else if currentTab === 'personnel'}
    <Personnel activeTab={currentTab} on:changeTab={handleTabChange} />
  {:else if currentTab === 'users' && $authStore.user && $authStore.user.role === 'ADMIN'}
    <UserManagement activeTab={currentTab} on:changeTab={handleTabChange} />
  {:else if currentTab === 'reports'}
    <Reports activeTab={currentTab} on:changeTab={handleTabChange} />
  {:else}
    <Dashboard activeTab={currentTab} on:changeTab={handleTabChange} />
  {/if}
{/if}
