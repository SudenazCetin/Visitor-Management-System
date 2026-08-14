<script>
  export var activeTab = 'personnel';
</script>

<aside class="w-64 bg-slate-900 border-r border-slate-800/80 flex flex-col justify-between shrink-0 h-screen sticky top-0 font-sans">
  <div>
    <!-- Logo & Brand Header -->
    <div class="h-16 flex items-center gap-3 px-6 border-b border-slate-800/80">
      <div class="w-9 h-9 rounded-xl bg-gradient-to-br from-sky-400 to-blue-600 flex items-center justify-center text-white shadow-md shadow-sky-500/20">
        <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2.2" d="M19 21V5a2 2 0 00-2-2H7a2 2 0 00-2 2v16m14 0h2m-2 0h-5m-9 0H3m2 0h5m0 0h4m-4 0V11m0 0l2 2m-2-2l-2 2m6-6v6m0 0l2-2m-2 2l-2-2"></path>
        </svg>
      </div>
      <div>
        <h1 class="text-base font-bold text-slate-100 leading-none">VMS Pro</h1>
        <span class="text-[10px] text-sky-400 font-semibold uppercase tracking-wider">Ziyaretçi Takip</span>
      </div>
    </div>

    <!-- Navigation Links -->
    <nav class="p-4 space-y-1.5">
      <!-- Dashboard Tab -->
      <button
        type="button"
        on:click={() => (activeTab = 'dashboard')}
        class="w-full flex items-center gap-3 px-4 py-3 rounded-xl text-sm font-medium transition-all duration-150 {activeTab === 'dashboard' ? 'bg-sky-500/10 text-sky-400 border border-sky-500/20 shadow-sm' : 'text-slate-400 hover:text-slate-200 hover:bg-slate-800/50'}"
      >
        <svg class="w-5 h-5 shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 6a2 2 0 012-2h2a2 2 0 012 2v2a2 2 0 01-2 2H6a2 2 0 01-2-2V6zM14 6a2 2 0 012-2h2a2 2 0 012 2v2a2 2 0 01-2 2h-2a2 2 0 01-2-2V6zM4 16a2 2 0 012-2h2a2 2 0 012 2v2a2 2 0 01-2 2H6a2 2 0 01-2-2v-2zM14 16a2 2 0 012-2h2a2 2 0 012 2v2a2 2 0 01-2 2h-2a2 2 0 01-2-2v-2z"></path>
        </svg>
        <span>Dashboard</span>
      </button>

      <!-- Personnel Tab -->
      <button
        type="button"
        on:click={() => (activeTab = 'personnel')}
        class="w-full flex items-center gap-3 px-4 py-3 rounded-xl text-sm font-medium transition-all duration-150 {activeTab === 'personnel' ? 'bg-sky-500/10 text-sky-400 border border-sky-500/20 shadow-sm' : 'text-slate-400 hover:text-slate-200 hover:bg-slate-800/50'}"
      >
        <svg class="w-5 h-5 shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4.354a4 4 0 110 5.292M15 21H3v-1a6 6 0 0112 0v1zm0 0h6v-1a6 6 0 00-9-5.197M13 7a4 4 0 11-8 0 4 4 0 018 0z"></path>
        </svg>
        <span>Personel</span>
      </button>

      <!-- Reports Tab -->
      <button
        type="button"
        on:click={() => (activeTab = 'reports')}
        class="w-full flex items-center gap-3 px-4 py-3 rounded-xl text-sm font-medium transition-all duration-150 {activeTab === 'reports' ? 'bg-sky-500/10 text-sky-400 border border-sky-500/20 shadow-sm' : 'text-slate-400 hover:text-slate-200 hover:bg-slate-800/50'}"
      >
        <svg class="w-5 h-5 shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 19v-6a2 2 0 00-2-2H5a2 2 0 00-2 2v6a2 2 0 002 2h2a2 2 0 002-2zm0 0V9a2 2 0 012-2h2a2 2 0 012 2v10m-6 0a2 2 0 002 2h2a2 2 0 002-2m0 0V5a2 2 0 012-2h2a2 2 0 012 2v14a2 2 0 01-2 2h-2a2 2 0 01-2-2z"></path>
        </svg>
        <span>Rapor</span>
      </button>
    </nav>
  </div>

  <!-- User Footer Profile Info -->
  <div class="p-4 border-t border-slate-800/80">
    <div class="flex items-center gap-3 p-2 bg-slate-950/60 rounded-xl border border-slate-800">
      <div class="w-8 h-8 rounded-lg bg-sky-500/20 text-sky-400 flex items-center justify-center font-bold text-xs">
        AD
      </div>
      <div class="flex-1 min-w-0">
        <p class="text-xs font-semibold text-slate-200 truncate">Admin User</p>
        <p class="text-[10px] text-slate-400 truncate">System Admin</p>
      </div>
    </div>
  </div>
</aside>
