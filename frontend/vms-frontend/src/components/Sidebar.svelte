<script>
  import { createEventDispatcher } from 'svelte';
  import { authStore } from '../stores/authStore.js';
  import NotificationBell from './NotificationBell.svelte';

  export var activeTab = 'my-dashboard';
  export var isMobileOpen = false;
  const dispatch = createEventDispatcher();

  function selectTab(tab) {
    activeTab = tab;
    dispatch('changeTab', tab);
    if (isMobileOpen) {
      dispatch('closeMobile');
    }
  }

  function handleLogout() {
    authStore.logout();
  }

  $: isAdmin = $authStore.user && $authStore.user.role === 'ADMIN';
  $: isPersonnel = $authStore.user && $authStore.user.role === 'PERSONNEL';
  $: currentUser = $authStore.user || { username: 'Kullanıcı', fullName: 'Kullanıcı', role: 'PERSONNEL' };

  $: userDisplayName = currentUser.fullName || currentUser.username || 'Kullanıcı';
  $: initials = userDisplayName.split(' ').map(n => n[0]).join('').substring(0,2).toUpperCase();
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
  class="w-64 bg-slate-950/95 backdrop-blur-2xl border-r border-slate-800/80 flex flex-col justify-between shrink-0 h-screen sticky top-0 font-sans shadow-2xl
    sidebar-mobile {isMobileOpen ? 'open' : ''}
    md:relative md:transform-none md:translate-x-0 text-slate-300 z-40"
  role="navigation"
  aria-label="Ana menü"
>
  <div class="space-y-4">
    <!-- Logo & Brand Header -->
    <div class="h-20 flex items-center px-6 border-b border-slate-800/80">
      <div class="flex items-center gap-3.5 min-w-0">
        <div class="w-10 h-10 rounded-2xl bg-gradient-to-br from-purple-600 via-indigo-600 to-purple-800 flex items-center justify-center text-white shadow-xl shadow-purple-900/40 shrink-0 border border-purple-400/30">
          <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2.2" d="M19 21V5a2 2 0 00-2-2H7a2 2 0 00-2 2v16m14 0h2m-2 0h-5m-9 0H3m2 0h5m0 0h4m-4 0V11m0 0l2 2m-2-2l-2 2m6-6v6m0 0l2-2m-2 2l-2-2"></path>
          </svg>
        </div>
        <div class="min-w-0">
          <h1 class="text-lg font-black text-white tracking-tight leading-none truncate">VMS Pro</h1>
          <span class="text-[10px] text-purple-400 font-extrabold uppercase tracking-widest block truncate mt-1">
            {isPersonnel ? 'PERSONEL PORTALI' : isAdmin ? 'YÖNETİCİ PORTALI' : 'RESEPSİYON PORTALI'}
          </span>
        </div>
      </div>
    </div>

    <!-- Navigation Links -->
    <nav class="px-3 space-y-1.5">
      {#if isPersonnel}
        <!-- Personnel Portal Navigation -->
        <button
          type="button"
          on:click={() => selectTab('my-dashboard')}
          class="w-full flex items-center gap-3 px-4 py-3 rounded-2xl text-xs font-bold transition-all duration-200 {activeTab === 'my-dashboard' || activeTab === 'dashboard' ? 'bg-gradient-to-r from-purple-900/80 to-indigo-900/60 text-white border border-purple-500/50 shadow-lg shadow-purple-900/30' : 'text-slate-400 hover:text-white hover:bg-slate-800/60'}"
        >
          <svg class="w-4 h-4 shrink-0 {activeTab === 'my-dashboard' || activeTab === 'dashboard' ? 'text-purple-300' : 'text-slate-400'}" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M3 12l2-2m0 0l7-7 7 7M5 10v10a1 1 0 001 1h3m10-11l2 2m-2-2v10a1 1 0 01-1 1h-3m-6 0a1 1 0 001-1v-4a1 1 0 011-1h2a1 1 0 011 1v4a1 1 0 001 1m-6 0h6"></path>
          </svg>
          <span>Ana Sayfa</span>
        </button>

        <button
          type="button"
          on:click={() => selectTab('my-visitors')}
          class="w-full flex items-center gap-3 px-4 py-3 rounded-2xl text-xs font-bold transition-all duration-200 {activeTab === 'my-visitors' ? 'bg-gradient-to-r from-purple-900/80 to-indigo-900/60 text-white border border-purple-500/50 shadow-lg shadow-purple-900/30' : 'text-slate-400 hover:text-white hover:bg-slate-800/60'}"
        >
          <svg class="w-4 h-4 shrink-0 {activeTab === 'my-visitors' ? 'text-purple-300' : 'text-slate-400'}" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M17 20h5v-2a3 3 0 00-5.356-1.857M17 20H7m10 0v-2c0-.656-.126-1.283-.356-1.857M7 20H2v-2a3 3 0 015.356-1.857M7 20v-2c0-.656.126-1.283.356-1.857m0 0a5.002 5.002 0 019.288 0M15 7a3 3 0 11-6 0 3 3 0 016 0zm6 3a2 2 0 11-4 0 2 2 0 014 0zM7 10a2 2 0 11-4 0 2 2 0 014 0z"></path>
          </svg>
          <span>Ziyaretçilerim</span>
        </button>

        <button
          type="button"
          on:click={() => selectTab('reports')}
          class="w-full flex items-center gap-3 px-4 py-3 rounded-2xl text-xs font-bold transition-all duration-200 {activeTab === 'reports' ? 'bg-gradient-to-r from-purple-900/80 to-indigo-900/60 text-white border border-purple-500/50 shadow-lg shadow-purple-900/30' : 'text-slate-400 hover:text-white hover:bg-slate-800/60'}"
        >
          <svg class="w-4 h-4 shrink-0 {activeTab === 'reports' ? 'text-purple-300' : 'text-slate-400'}" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z"></path>
          </svg>
          <span>Ziyaret Geçmişim</span>
        </button>

        <button
          type="button"
          on:click={() => selectTab('profile')}
          class="w-full flex items-center gap-3 px-4 py-3 rounded-2xl text-xs font-bold transition-all duration-200 {activeTab === 'profile' ? 'bg-gradient-to-r from-purple-900/80 to-indigo-900/60 text-white border border-purple-500/50 shadow-lg shadow-purple-900/30' : 'text-slate-400 hover:text-white hover:bg-slate-800/60'}"
        >
          <svg class="w-4 h-4 shrink-0 {activeTab === 'profile' ? 'text-purple-300' : 'text-slate-400'}" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z"></path>
          </svg>
          <span>Profil</span>
        </button>
      {:else}
        <!-- Admin / Receptionist Navigation -->
        <button
          type="button"
          on:click={() => selectTab('dashboard')}
          class="w-full flex items-center gap-3 px-4 py-3 rounded-2xl text-xs font-bold transition-all duration-200 {activeTab === 'dashboard' || activeTab === 'my-dashboard' ? 'bg-gradient-to-r from-purple-900/80 to-indigo-900/60 text-white border border-purple-500/50 shadow-lg shadow-purple-900/30' : 'text-slate-400 hover:text-white hover:bg-slate-800/60'}"
        >
          <svg class="w-4 h-4 shrink-0 {activeTab === 'dashboard' || activeTab === 'my-dashboard' ? 'text-purple-300' : 'text-slate-400'}" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M3 12l2-2m0 0l7-7 7 7M5 10v10a1 1 0 001 1h3m10-11l2 2m-2-2v10a1 1 0 01-1 1h-3m-6 0a1 1 0 001-1v-4a1 1 0 011-1h2a1 1 0 011 1v4a1 1 0 001 1m-6 0h6"></path>
          </svg>
          <span>Ana Sayfa</span>
        </button>

        <button
          type="button"
          on:click={() => selectTab('personnel')}
          class="w-full flex items-center gap-3 px-4 py-3 rounded-2xl text-xs font-bold transition-all duration-200 {activeTab === 'personnel' ? 'bg-gradient-to-r from-purple-900/80 to-indigo-900/60 text-white border border-purple-500/50 shadow-lg shadow-purple-900/30' : 'text-slate-400 hover:text-white hover:bg-slate-800/60'}"
        >
          <svg class="w-4 h-4 shrink-0 {activeTab === 'personnel' ? 'text-purple-300' : 'text-slate-400'}" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4.354a4 4 0 110 5.292M15 21H3v-1a6 6 0 0112 0v1zm0 0h6v-1a6 6 0 00-9-5.197M13 7a4 4 0 11-8 0 4 4 0 018 0z"></path>
          </svg>
          <span>Personel Yönetimi</span>
        </button>

        {#if isAdmin}
          <button
            type="button"
            on:click={() => selectTab('announcements')}
            class="w-full flex items-center gap-3 px-4 py-3 rounded-2xl text-xs font-bold transition-all duration-200 {activeTab === 'announcements' ? 'bg-gradient-to-r from-purple-900/80 to-indigo-900/60 text-white border border-purple-500/50 shadow-lg shadow-purple-900/30' : 'text-slate-400 hover:text-white hover:bg-slate-800/60'}"
          >
            <svg class="w-4 h-4 shrink-0 {activeTab === 'announcements' ? 'text-purple-300' : 'text-slate-400'}" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M11 5.882V19.24a1.76 1.76 0 01-3.417.592l-2.147-6.15M18 13a3 3 0 100-6M5.436 13.683A4.001 4.001 0 017 6h1.832c4.1 0 7.625-1.234 9.168-3v14c-1.543-1.766-5.067-3-9.168-3H7a3.988 3.988 0 01-1.564-.317z"></path>
            </svg>
            <span>Duyurular</span>
          </button>

          <button
            type="button"
            on:click={() => selectTab('users')}
            class="w-full flex items-center gap-3 px-4 py-3 rounded-2xl text-xs font-bold transition-all duration-200 {activeTab === 'users' ? 'bg-gradient-to-r from-purple-900/80 to-indigo-900/60 text-white border border-purple-500/50 shadow-lg shadow-purple-900/30' : 'text-slate-400 hover:text-white hover:bg-slate-800/60'}"
          >
            <svg class="w-4 h-4 shrink-0 {activeTab === 'users' ? 'text-purple-300' : 'text-slate-400'}" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 15v2m-6 4h12a2 2 0 002-2v-6a2 2 0 00-2-2H6a2 2 0 00-2 2v6a2 2 0 002 2zm10-10V7a4 4 0 00-8 0v4h8z"></path>
            </svg>
            <span>Kullanıcılar</span>
          </button>
        {/if}

        <button
          type="button"
          on:click={() => selectTab('reports')}
          class="w-full flex items-center gap-3 px-4 py-3 rounded-2xl text-xs font-bold transition-all duration-200 {activeTab === 'reports' ? 'bg-gradient-to-r from-purple-900/80 to-indigo-900/60 text-white border border-purple-500/50 shadow-lg shadow-purple-900/30' : 'text-slate-400 hover:text-white hover:bg-slate-800/60'}"
        >
          <svg class="w-4 h-4 shrink-0 {activeTab === 'reports' ? 'text-purple-300' : 'text-slate-400'}" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 19v-6a2 2 0 00-2-2H5a2 2 0 00-2 2v6a2 2 0 002 2h2a2 2 0 002-2zm0 0V9a2 2 0 012-2h2a2 2 0 012 2v10m-6 0a2 2 0 002 2h2a2 2 0 002-2m0 0V5a2 2 0 012-2h2a2 2 0 012 2v14a2 2 0 01-2 2h-2a2 2 0 01-2-2z"></path>
          </svg>
          <span>Raporlar</span>
        </button>

        <button
          type="button"
          on:click={() => selectTab('profile')}
          class="w-full flex items-center gap-3 px-4 py-3 rounded-2xl text-xs font-bold transition-all duration-200 {activeTab === 'profile' ? 'bg-gradient-to-r from-purple-900/80 to-indigo-900/60 text-white border border-purple-500/50 shadow-lg shadow-purple-900/30' : 'text-slate-400 hover:text-white hover:bg-slate-800/60'}"
        >
          <svg class="w-4 h-4 shrink-0 {activeTab === 'profile' ? 'text-purple-300' : 'text-slate-400'}" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z"></path>
          </svg>
          <span>Profil</span>
        </button>
      {/if}
    </nav>
  </div>

  <!-- Bottom User Profile Card & Collapse Footer -->
  <div class="p-3 border-t border-slate-800/80 space-y-2">
    <!-- User Profile Card -->
    <div class="p-3 bg-slate-900/80 border border-slate-800/90 rounded-2xl flex items-center justify-between shadow-lg">
      <div class="flex items-center gap-3 min-w-0">
        <!-- Initial Circle Avatar -->
        <div class="w-10 h-10 rounded-full bg-gradient-to-tr from-purple-600 via-indigo-600 to-purple-800 text-white flex items-center justify-center font-extrabold text-sm shrink-0 border border-white/20 shadow-md">
          {initials}
        </div>
        <div class="min-w-0 leading-tight">
          <p class="text-xs font-extrabold text-white truncate">{userDisplayName}</p>
          <p class="text-[10px] text-slate-400 font-medium truncate mt-0.5">{isPersonnel ? 'Personel' : isAdmin ? 'Sistem Yöneticisi' : 'Resepsiyonist'}</p>
          <div class="flex items-center gap-1.5 mt-1 text-[10px] text-emerald-400 font-semibold">
            <span class="w-1.5 h-1.5 rounded-full bg-emerald-400 animate-pulse"></span>
            <span>Çevrimiçi</span>
          </div>
        </div>
      </div>

      <NotificationBell on:navigate={(e) => selectTab(e.detail)} />
    </div>

    <!-- Collapse / Close Button -->
    <button
      type="button"
      on:click={handleLogout}
      class="w-full py-2.5 px-3 rounded-xl bg-slate-900/50 hover:bg-rose-500/10 border border-slate-800/60 hover:border-rose-500/30 text-slate-400 hover:text-rose-300 text-xs font-semibold flex items-center justify-between transition-all"
    >
      <span class="flex items-center gap-2">
        <svg class="w-4 h-4 text-slate-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M17 16l4-4m0 0l-4-4m4 4H7m6 4v1a3 3 0 01-3 3H6a3 3 0 01-3-3V7a3 3 0 013-3h4a3 3 0 013 3v1"></path>
        </svg>
        <span>Çıkış Yap</span>
      </span>
      <span class="text-[10px] text-slate-500 font-mono">v1.0</span>
    </button>
  </div>
</aside>
