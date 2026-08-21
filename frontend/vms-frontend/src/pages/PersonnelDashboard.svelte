<script>
  import { onMount, onDestroy, createEventDispatcher } from 'svelte';
  import Sidebar from '../components/Sidebar.svelte';
  import { getMySummary, getMyActiveVisitors } from '../api/meApi.js';
  import { calculateDigitalDuration, formatDateTimeStr } from '../utils/duration.js';

  const dispatch = createEventDispatcher();
  export var activeTab = 'my-dashboard';

  let isMobileOpen = false;

  let summary = {
    totalVisitors: 0,
    activeVisitors: 0,
    todayVisitors: 0
  };
  let activeVisitors = [];
  let loading = true;
  let errorMsg = '';

  let now = new Date();
  let timerInterval;

  onMount(() => {
    timerInterval = setInterval(() => {
      now = new Date();
    }, 1000);

    loadData();
  });

  onDestroy(() => {
    if (timerInterval) {
      clearInterval(timerInterval);
    }
  });

  async function loadData() {
    loading = true;
    errorMsg = '';
    try {
      const [sumRes, actRes] = await Promise.all([
        getMySummary(),
        getMyActiveVisitors()
      ]);
      summary = sumRes;
      activeVisitors = actRes;
    } catch (err) {
      errorMsg = err.message || 'Veriler yüklenirken bir hata oluştu.';
    } finally {
      loading = false;
    }
  }

  function handleTabChange(e) {
    activeTab = e.detail;
    dispatch('changeTab', e.detail);
  }
</script>

<div class="min-h-screen bg-slate-900 flex text-slate-100 font-sans antialiased">
  <Sidebar {activeTab} {isMobileOpen} on:closeMobile={() => (isMobileOpen = false)} on:changeTab={handleTabChange} />

  <main class="flex-1 p-4 md:p-8 overflow-y-auto max-w-7xl mx-auto w-full">
    <!-- Header -->
    <header class="flex flex-col md:flex-row md:items-center justify-between gap-4 mb-8 bg-slate-800/60 p-6 rounded-2xl border border-slate-700/50 backdrop-blur-md">
      <div class="flex items-center gap-3">
        <button
          on:click={() => (isMobileOpen = true)}
          class="md:hidden p-2 text-slate-400 hover:text-white hover:bg-slate-700/50 rounded-xl transition"
        >
          <svg class="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 6h16M4 12h16M4 18h16"></path>
          </svg>
        </button>
        <div>
          <h1 class="text-2xl font-extrabold text-white tracking-tight">MY DASHBOARD</h1>
          <p class="text-xs text-slate-400 mt-1">Size gelen ziyaretçilerin anlık canlı durum takibi</p>
        </div>
      </div>
    </header>

    {#if loading}
      <div class="flex items-center justify-center p-16">
        <div class="animate-spin w-8 h-8 border-4 border-purple-500 border-t-transparent rounded-full"></div>
      </div>
    {:else if errorMsg}
      <div class="p-4 bg-rose-500/10 border border-rose-500/30 rounded-2xl text-rose-300 text-sm">
        {errorMsg}
      </div>
    {:else}
      <!-- Stat Cards Grid -->
      <div class="grid grid-cols-1 md:grid-cols-3 gap-6 mb-8">
        <!-- Active Visitors Card -->
        <div class="bg-gradient-to-br from-purple-900/40 via-slate-800/80 to-slate-800/90 border border-purple-500/30 rounded-2xl p-6 shadow-xl backdrop-blur-sm">
          <div class="flex items-center justify-between mb-2">
            <span class="text-xs font-bold text-purple-300 uppercase tracking-wider">İÇERİDEKİ ZİYARETÇİLERİM</span>
            <span class="p-2 bg-purple-500/20 text-purple-400 rounded-xl">
              <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z"></path>
              </svg>
            </span>
          </div>
          <p class="text-3xl font-extrabold text-white">{summary.activeVisitors}</p>
          <p class="text-xs text-slate-400 mt-2">Şu an ofiste sizinle olanlar</p>
        </div>

        <!-- Today's Visitors Card -->
        <div class="bg-gradient-to-br from-indigo-900/40 via-slate-800/80 to-slate-800/90 border border-indigo-500/30 rounded-2xl p-6 shadow-xl backdrop-blur-sm">
          <div class="flex items-center justify-between mb-2">
            <span class="text-xs font-bold text-indigo-300 uppercase tracking-wider">BUGÜNKÜ Toplam ZİYARETÇİ</span>
            <span class="p-2 bg-indigo-500/20 text-indigo-400 rounded-xl">
              <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8 7V3m8 4V3m-9 8h10M5 21h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v12a2 2 0 002 2z"></path>
              </svg>
            </span>
          </div>
          <p class="text-3xl font-extrabold text-white">{summary.todayVisitors}</p>
          <p class="text-xs text-slate-400 mt-2">Bugün size gelen toplam kişi sayısı</p>
        </div>

        <!-- Total Visitors Card -->
        <div class="bg-gradient-to-br from-slate-800/90 via-slate-800/80 to-slate-800/90 border border-slate-700/50 rounded-2xl p-6 shadow-xl backdrop-blur-sm">
          <div class="flex items-center justify-between mb-2">
            <span class="text-xs font-bold text-slate-300 uppercase tracking-wider">TÜM ZAMANLAR TOTAL</span>
            <span class="p-2 bg-slate-700/50 text-slate-300 rounded-xl">
              <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M17 20h5v-2a3 3 0 00-5.356-1.857M17 20H7m10 0v-2c0-.656-.126-1.283-.356-1.857M7 20H2v-2a3 3 0 015.356-1.857M7 20v-2c0-.656.126-1.283.356-1.857m0 0a5.002 5.002 0 019.288 0M15 7a3 3 0 11-6 0 3 3 0 016 0zm6 3a2 2 0 11-4 0 2 2 0 014 0zM7 10a2 2 0 11-4 0 2 2 0 014 0z"></path>
              </svg>
            </span>
          </div>
          <p class="text-3xl font-extrabold text-white">{summary.totalVisitors}</p>
          <p class="text-xs text-slate-400 mt-2">Geçmiş dahil toplam ziyaretçi kaydı</p>
        </div>
      </div>

      <!-- Active Visitors Table Panel -->
      <div class="bg-slate-800/60 border border-slate-700/50 rounded-2xl p-6 shadow-xl backdrop-blur-md">
        <h2 class="text-lg font-bold text-white mb-4 flex items-center gap-2">
          <span>MY ACTIVE VISITORS</span>
          <span class="text-xs px-2.5 py-0.5 bg-purple-500/20 text-purple-300 border border-purple-500/30 font-bold rounded-full">
            {activeVisitors.length} Aktif
          </span>
        </h2>

        {#if activeVisitors.length === 0}
          <div class="text-center py-12 text-slate-400 text-sm">
            Şu an size bağlı içeride aktif bir ziyaretçi bulunmamaktadır.
          </div>
        {:else}
          <div class="overflow-x-auto">
            <table class="w-full text-left text-sm text-slate-300">
              <thead class="text-xs font-bold uppercase tracking-wider text-slate-400 bg-slate-900/50 border-b border-slate-700/50">
                <tr>
                  <th class="py-3.5 px-4">Ziyaretçi Adı Soyadı</th>
                  <th class="py-3.5 px-4">Giriş Saati</th>
                  <th class="py-3.5 px-4">Kalış Süresi</th>
                  <th class="py-3.5 px-4">Durum</th>
                </tr>
              </thead>
              <tbody class="divide-y divide-slate-700/30">
                {#each activeVisitors as v}
                  <tr class="hover:bg-slate-700/30 transition">
                    <td class="py-4 px-4 font-semibold text-white">{v.fullName}</td>
                    <td class="py-4 px-4 text-slate-300">{formatDateTimeStr(v.entryTime)}</td>
                    <td class="py-4 px-4 font-mono text-purple-400 font-bold tracking-wider">{calculateDigitalDuration(v.entryTime, v.exitTime, true, now)}</td>
                    <td class="py-4 px-4">
                      <span class="inline-flex items-center gap-1.5 px-2.5 py-1 bg-emerald-500/20 text-emerald-400 border border-emerald-500/30 text-xs font-semibold rounded-full">
                        <span class="w-1.5 h-1.5 rounded-full bg-emerald-400 animate-pulse"></span>
                        İçeride
                      </span>
                    </td>
                  </tr>
                {/each}
              </tbody>
            </table>
          </div>
        {/if}
      </div>
    {/if}
  </main>
</div>
