<script>
  import { createEventDispatcher } from 'svelte';
  import { authStore } from '../stores/authStore.js';

  export var activeTab = 'dashboard';
  export var isMobileOpen = false;
  const dispatch = createEventDispatcher();

  function selectTab(tab) {
    activeTab = tab;
    dispatch('changeTab', tab);
    // Close mobile sidebar on navigation
    if (isMobileOpen) {
      dispatch('closeMobile');
    }
  }

  function handleLogout() {
    authStore.logout();
  }

  $: isAdmin = $authStore.user && $authStore.user.role === 'ADMIN';
  $: currentUser = $authStore.user || { username: 'Kullanıcı', role: '-' };
</script>

<!-- Mobile Overlay -->
<!-- svelte-ignore a11y-click-events-have-key-events -->
<div
  class="sidebar-overlay {isMobileOpen ? 'active' : ''} md:hidden"
  on:click={() => dispatch('closeMobile')}
  aria-hidden="true"
></div>

<!-- Sidebar -->
<aside
  class="w-64 bg-white/95 border-r border-slate-200/90 flex flex-col justify-between shrink-0 h-screen sticky top-0 font-sans shadow-sm
    sidebar-mobile {isMobileOpen ? 'open' : ''}
    md:relative md:transform-none md:translate-x-0"
  role="navigation"
  aria-label="Ana menü"
>
  <div>
    <!-- Logo & Brand Header -->
    <div class="h-16 flex items-center gap-3 px-6 border-b border-slate-200/80">
      <div class="w-9 h-9 rounded-xl bg-gradient-to-br from-purple-700 to-indigo-800 flex items-center justify-center text-white shadow-md shadow-purple-900/20">
        <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2.2" d="M19 21V5a2 2 0 00-2-2H7a2 2 0 00-2 2v16m14 0h2m-2 0h-5m-9 0H3m2 0h5m0 0h4m-4 0V11m0 0l2 2m-2-2l-2 2m6-6v6m0 0l2-2m-2 2l-2-2"></path>
        </svg>
      </div>
      <div>
        <h1 class="text-base font-bold text-slate-900 leading-none">VMS Pro</h1>
        <span class="text-[10px] text-purple-700 font-bold uppercase tracking-wider">Ziyaretçi Takip</span>
      </div>
    </div>

    <!-- Navigation Links -->
    <nav class="p-4 space-y-1.5">
      <!-- Dashboard Tab -->
      <button
        type="button"
        on:click={() => selectTab('dashboard')}
        class="w-full flex items-center gap-3 px-4 py-3 rounded-xl text-sm font-medium transition-all duration-150 {activeTab === 'dashboard' ? 'bg-purple-50 text-purple-900 border border-purple-200/80 shadow-xs font-semibold' : 'text-slate-600 hover:text-slate-900 hover:bg-slate-100/70'}"
        title="Dashboard"
      >
        <svg class="w-5 h-5 shrink-0 {activeTab === 'dashboard' ? 'text-purple-700' : 'text-slate-400'}" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 6a2 2 0 012-2h2a2 2 0 012 2v2a2 2 0 01-2 2H6a2 2 0 01-2-2V6zM14 6a2 2 0 012-2h2a2 2 0 012 2v2a2 2 0 01-2 2h-2a2 2 0 01-2-2V6zM4 16a2 2 0 012-2h2a2 2 0 012 2v2a2 2 0 01-2 2H6a2 2 0 01-2-2v-2zM14 16a2 2 0 012-2h2a2 2 0 012 2v2a2 2 0 01-2 2h-2a2 2 0 01-2-2v-2z"></path>
        </svg>
        <span>Dashboard</span>
      </button>

      <!-- Personnel Tab -->
      <button
        type="button"
        on:click={() => selectTab('personnel')}
        class="w-full flex items-center gap-3 px-4 py-3 rounded-xl text-sm font-medium transition-all duration-150 {activeTab === 'personnel' ? 'bg-purple-50 text-purple-900 border border-purple-200/80 shadow-xs font-semibold' : 'text-slate-600 hover:text-slate-900 hover:bg-slate-100/70'}"
        title="Personel Yönetimi"
      >
        <svg class="w-5 h-5 shrink-0 {activeTab === 'personnel' ? 'text-purple-700' : 'text-slate-400'}" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4.354a4 4 0 110 5.292M15 21H3v-1a6 6 0 0112 0v1zm0 0h6v-1a6 6 0 00-9-5.197M13 7a4 4 0 11-8 0 4 4 0 018 0z"></path>
        </svg>
        <span>Personel</span>
      </button>

      <!-- User Management Tab (ADMIN Only) -->
      {#if isAdmin}
        <button
          type="button"
          on:click={() => selectTab('users')}
          class="w-full flex items-center gap-3 px-4 py-3 rounded-xl text-sm font-medium transition-all duration-150 {activeTab === 'users' ? 'bg-purple-50 text-purple-900 border border-purple-200/80 shadow-xs font-semibold' : 'text-slate-600 hover:text-slate-900 hover:bg-slate-100/70'}"
          title="Kullanıcı Yönetimi"
        >
          <svg class="w-5 h-5 shrink-0 {activeTab === 'users' ? 'text-purple-700' : 'text-slate-400'}" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 15v2m-6 4h12a2 2 0 002-2v-6a2 2 0 00-2-2H6a2 2 0 00-2 2v6a2 2 0 002 2zm10-10V7a4 4 0 00-8 0v4h8z"></path>
          </svg>
          <span>Kullanıcılar</span>
        </button>
      {/if}

      <!-- Reports Tab -->
      <button
        type="button"
        on:click={() => selectTab('reports')}
        class="w-full flex items-center gap-3 px-4 py-3 rounded-xl text-sm font-medium transition-all duration-150 {activeTab === 'reports' ? 'bg-purple-50 text-purple-900 border border-purple-200/80 shadow-xs font-semibold' : 'text-slate-600 hover:text-slate-900 hover:bg-slate-100/70'}"
        title="Raporlar"
      >
        <svg class="w-5 h-5 shrink-0 {activeTab === 'reports' ? 'text-purple-700' : 'text-slate-400'}" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 19v-6a2 2 0 00-2-2H5a2 2 0 00-2 2v6a2 2 0 002 2h2a2 2 0 002-2zm0 0V9a2 2 0 012-2h2a2 2 0 012 2v10m-6 0a2 2 0 002 2h2a2 2 0 002-2m0 0V5a2 2 0 012-2h2a2 2 0 012 2v14a2 2 0 01-2 2h-2a2 2 0 01-2-2z"></path>
        </svg>
        <span>Rapor</span>
      </button>
    </nav>
  </div>

  <!-- User Footer Profile & Logout -->
  <div class="p-4 border-t border-slate-200/80 space-y-2">
    <div class="flex items-center justify-between p-2.5 bg-slate-50 rounded-xl border border-slate-200/80">
      <div class="flex items-center gap-2.5 min-w-0">
        <div class="w-8 h-8 rounded-lg bg-purple-100 text-purple-700 flex items-center justify-center font-bold text-xs shrink-0">
          {currentUser.username.substring(0,2).toUpperCase()}
        </div>
        <div class="min-w-0">
          <p class="text-xs font-semibold text-slate-800 truncate">{currentUser.username}</p>
          <p class="text-[10px] text-slate-500 truncate">{currentUser.role}</p>
        </div>
      </div>

      <!-- Logout Button -->
      <button
        type="button"
        on:click={handleLogout}
        class="p-1.5 text-slate-400 hover:text-rose-600 hover:bg-rose-50 rounded-lg transition"
        title="Çıkış Yap"
        aria-label="Çıkış Yap"
      >
        <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M17 16l4-4m0 0l-4-4m4 4H7m6 4v1a3 3 0 01-3 3H6a3 3 0 01-3-3V7a3 3 0 013-3h4a3 3 0 013 3v1"></path>
        </svg>
      </button>
    </div>
  </div>
</aside>
