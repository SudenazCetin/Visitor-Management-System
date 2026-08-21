<script>
  import { onMount, onDestroy, createEventDispatcher } from 'svelte';
  import Sidebar from '../components/Sidebar.svelte';
  import VisitorDetailModal from '../components/VisitorDetailModal.svelte';
  import NotificationBell from '../components/NotificationBell.svelte';
  import { getMySummary, getMyActiveVisitors, getMyRecentVisitors } from '../api/meApi.js';
  import { calculateDigitalDuration, formatDateTimeStr } from '../utils/duration.js';
  import { authStore } from '../stores/authStore.js';
  import { notificationStore } from '../stores/notificationStore.js';

  const dispatch = createEventDispatcher();
  export var activeTab = 'my-dashboard';

  let isMobileOpen = false;

  // Personal Summary State
  let summary = {
    totalVisitors: 0,
    activeVisitors: 0,
    todayVisitors: 0,
    completedToday: 0,
    lastVisitTime: ''
  };

  let activeVisitors = [];
  let recentVisitors = [];

  // Independent Loading & Error States
  let loadingSummary = true;
  let loadingActive = true;
  let loadingRecent = true;

  let summaryError = '';
  let activeError = '';
  let recentError = '';

  // Visitor Detail Modal State
  let isDetailModalOpen = false;
  let selectedDetailVisitor = null;

  function openDetailModal(v) {
    selectedDetailVisitor = v;
    isDetailModalOpen = true;
  }

  // Live Duration Ticker
  let now = new Date();
  let timerInterval;
  let unsubscribeSocket;

  // Compute Greeting based on local hour
  $: currentHour = now.getHours();
  $: greeting = currentHour >= 5 && currentHour < 12
    ? 'Günaydın'
    : currentHour >= 12 && currentHour < 18
      ? 'İyi günler'
      : 'İyi akşamlar';

  $: personnelName = $authStore.user?.fullName || $authStore.user?.username || 'Ayşe Yılmaz';
  $: initials = personnelName.split(' ').map(n => n[0]).join('').substring(0,2).toUpperCase();

  $: formattedDate = now.toLocaleDateString('tr-TR', { day: 'numeric', month: 'long', year: 'numeric' });
  $: formattedTime = now.toLocaleTimeString('tr-TR', { hour: '2-digit', minute: '2-digit' });

  onMount(() => {
    timerInterval = setInterval(() => {
      now = new Date();
    }, 1000);

    loadAllData();

    // Listen to real-time WebSocket events for automatic refresh
    unsubscribeSocket = notificationStore.subscribe($store => {
      if ($store.lastSocketMsg) {
        handleSocketUpdate($store.lastSocketMsg);
      }
    });
  });

  onDestroy(() => {
    if (timerInterval) {
      clearInterval(timerInterval);
    }
    if (unsubscribeSocket) {
      unsubscribeSocket();
    }
  });

  async function loadSummaryData() {
    loadingSummary = true;
    summaryError = '';
    try {
      summary = await getMySummary();
    } catch (err) {
      summaryError = err.message || 'Özet verileri alınamadı.';
    } finally {
      loadingSummary = false;
    }
  }

  async function loadActiveVisitorsData() {
    loadingActive = true;
    activeError = '';
    try {
      activeVisitors = await getMyActiveVisitors();
    } catch (err) {
      activeError = err.message || 'Aktif ziyaretçi verileri alınamadı.';
    } finally {
      loadingActive = false;
    }
  }

  async function loadRecentVisitorsData() {
    loadingRecent = true;
    recentError = '';
    try {
      recentVisitors = await getMyRecentVisitors(5);
    } catch (err) {
      recentError = err.message || 'Geçmiş ziyaretçi verileri alınamadı.';
    } finally {
      loadingRecent = false;
    }
  }

  function loadAllData() {
    loadSummaryData();
    loadActiveVisitorsData();
    loadRecentVisitorsData();
  }

  function handleSocketUpdate(msg) {
    if (!msg) return;
    const event = msg.event;
    const category = msg.category;

    if (category === 'VISITOR' || (event && event.startsWith('VISITOR_'))) {
      loadAllData();
    }
  }

  function handleTabChange(e) {
    activeTab = e.detail;
    dispatch('changeTab', e.detail);
  }

  function navigateTo(tabName) {
    activeTab = tabName;
    dispatch('changeTab', tabName);
  }
</script>

<!-- Visitor Detail Modal -->
<VisitorDetailModal
  isOpen={isDetailModalOpen}
  visitor={selectedDetailVisitor}
  {now}
  on:close={() => (isDetailModalOpen = false)}
/>

<div class="vms-app-layout flex text-slate-100 font-sans antialiased">
  <!-- Fixed Background Image & Overlay -->
  <div class="vms-bg-fixed">
    <div class="vms-bg-image"></div>
    <div class="vms-bg-overlay"></div>
  </div>

  <Sidebar {activeTab} {isMobileOpen} on:closeMobile={() => (isMobileOpen = false)} on:changeTab={handleTabChange} />

  <!-- Main Content Body -->
  <main class="flex-1 p-4 md:p-8 overflow-y-auto w-full z-10">
    <div class="max-w-7xl mx-auto space-y-6">
      
      <!-- Top Workspace Header Banner (Matching exact screenshot layout) -->
      <div class="flex flex-col md:flex-row md:items-center justify-between gap-4 py-2">
        <!-- Left Greeting Info -->
        <div class="space-y-1">
          <div class="flex items-center gap-3 flex-wrap">
            <h1 class="text-2xl md:text-3xl font-black text-white tracking-tight">
              {greeting}, {personnelName} 👋
            </h1>
            <span class="px-3 py-1 bg-purple-900/50 text-purple-300 border border-purple-500/40 text-[11px] font-black rounded-full tracking-wider uppercase">
              PERSONEL WORKSPACE
            </span>
          </div>
          <p class="text-xs text-slate-400">Size yapılan ziyaretleri ve çalışma alanı hareketlerinizi buradan takip edebilirsiniz.</p>
        </div>

        <!-- Right Header Badges & Actions -->
        <div class="flex items-center gap-3 shrink-0">
          <!-- Live Date & Clock Card -->
          <div class="hidden sm:flex items-center gap-2.5 px-4 py-2.5 bg-slate-900/90 border border-slate-800 rounded-2xl text-xs font-mono text-purple-300 shadow-lg">
            <svg class="w-4 h-4 text-purple-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z"></path>
            </svg>
            <span class="font-sans font-semibold text-slate-300">{formattedDate}</span>
            <span class="font-extrabold text-white">{formattedTime}</span>
          </div>

          <!-- Notification Bell -->
          <div class="p-1 bg-slate-900/90 border border-slate-800 rounded-2xl">
            <NotificationBell on:navigate={(e) => navigateTo(e.detail)} />
          </div>

          <!-- User Profile Dropdown Pill -->
          <button
            type="button"
            on:click={() => navigateTo('profile')}
            class="flex items-center gap-2.5 px-3.5 py-2 bg-slate-900/90 hover:bg-slate-800/90 border border-slate-800 hover:border-purple-500/40 rounded-2xl transition shadow-lg"
          >
            <div class="w-7 h-7 rounded-full bg-indigo-600 text-white flex items-center justify-center text-xs font-extrabold shadow-sm">
              {initials}
            </div>
            <span class="text-xs font-bold text-white hidden md:inline">{personnelName}</span>
            <svg class="w-3.5 h-3.5 text-slate-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 9l-7 7-7-7"></path>
            </svg>
          </button>
        </div>
      </div>

      <!-- 4 KPI Stat Cards Row (Exact colors & icons from screenshot) -->
      <div class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-5">
        
        <!-- Stat 1: Aktif Ziyaretçim -->
        <div class="vms-card p-5 flex items-center gap-4">
          <div class="w-12 h-12 rounded-2xl bg-emerald-500/20 text-emerald-400 border border-emerald-500/30 flex items-center justify-center shrink-0 shadow-lg">
            <svg class="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M17 20h5v-2a3 3 0 00-5.356-1.857M17 20H7m10 0v-2c0-.656-.126-1.283-.356-1.857M7 20H2v-2a3 3 0 015.356-1.857M7 20v-2c0-.656.126-1.283.356-1.857m0 0a5.002 5.002 0 019.288 0M15 7a3 3 0 11-6 0 3 3 0 016 0zm6 3a2 2 0 11-4 0 2 2 0 014 0zM7 10a2 2 0 11-4 0 2 2 0 014 0z"></path>
            </svg>
          </div>
          <div>
            <p class="text-2xl font-black text-white">{loadingSummary ? '...' : summary.activeVisitors}</p>
            <p class="text-xs font-bold text-white mt-0.5">Aktif Ziyaretçim</p>
            <p class="text-[10px] text-slate-400 mt-0.5">Şu anda içeride</p>
          </div>
        </div>

        <!-- Stat 2: Bugünkü Ziyaret -->
        <div class="vms-card p-5 flex items-center gap-4">
          <div class="w-12 h-12 rounded-2xl bg-blue-500/20 text-blue-400 border border-blue-500/30 flex items-center justify-center shrink-0 shadow-lg">
            <svg class="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8 7V3m8 4V3m-9 8h10M5 21h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v12a2 2 0 002 2z"></path>
            </svg>
          </div>
          <div>
            <p class="text-2xl font-black text-white">{loadingSummary ? '...' : summary.todayVisitors}</p>
            <p class="text-xs font-bold text-white mt-0.5">Bugünkü Ziyaret</p>
            <p class="text-[10px] text-slate-400 mt-0.5">Bugün size yapılan</p>
          </div>
        </div>

        <!-- Stat 3: Bugün Tamamlanan -->
        <div class="vms-card p-5 flex items-center gap-4">
          <div class="w-12 h-12 rounded-2xl bg-purple-500/20 text-purple-400 border border-purple-500/30 flex items-center justify-center shrink-0 shadow-lg">
            <svg class="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z"></path>
            </svg>
          </div>
          <div>
            <p class="text-2xl font-black text-white">{loadingSummary ? '...' : summary.completedToday}</p>
            <p class="text-xs font-bold text-white mt-0.5">Bugün Tamamlanan</p>
            <p class="text-[10px] text-slate-400 mt-0.5">Bugün çıkış yapan</p>
          </div>
        </div>

        <!-- Stat 4: Toplam Ziyaret -->
        <div class="vms-card p-5 flex items-center gap-4 border border-amber-500/30 shadow-lg">
          <div class="w-12 h-12 rounded-2xl bg-amber-500/20 text-amber-400 border border-amber-500/30 flex items-center justify-center shrink-0 shadow-lg">
            <svg class="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4.354a4 4 0 110 5.292M15 21H3v-1a6 6 0 0112 0v1zm0 0h6v-1a6 6 0 00-9-5.197M13 7a4 4 0 11-8 0 4 4 0 018 0z"></path>
            </svg>
          </div>
          <div>
            <p class="text-2xl font-black text-white">{loadingSummary ? '...' : summary.totalVisitors}</p>
            <p class="text-xs font-bold text-white mt-0.5">Toplam Ziyaret</p>
            <p class="text-[10px] text-slate-400 mt-0.5">Tüm zamanların arşivi</p>
          </div>
        </div>

      </div>

      <!-- HIZLI İŞLEMLER Section (4 Action Cards matching screenshot) -->
      <div class="space-y-3">
        <h2 class="text-xs font-bold text-slate-300 uppercase tracking-wider">HIZLI İŞLEMLER</h2>
        
        <div class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
          <!-- Card 1: Ziyaretçilerim -->
          <button
            type="button"
            on:click={() => navigateTo('my-visitors')}
            class="vms-card p-5 bg-gradient-to-br from-purple-950/60 to-slate-900/90 hover:from-purple-900/80 hover:to-slate-900/90 border border-purple-800/40 hover:border-purple-500/60 rounded-2xl transition-all duration-200 text-left flex items-center justify-between group shadow-lg"
          >
            <div class="flex items-center gap-3.5 min-w-0">
              <div class="w-10 h-10 rounded-xl bg-purple-600/20 border border-purple-500/30 text-purple-300 flex items-center justify-center shrink-0">
                <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M18 9v3m0 0v3m0-3h3m-3 0h-3m-2-5a4 4 0 11-8 0 4 4 0 018 0zM3 20a6 6 0 0112 0v1H3v-1z"></path>
                </svg>
              </div>
              <div class="min-w-0">
                <p class="text-xs font-extrabold text-white truncate group-hover:text-purple-300">Ziyaretçilerim</p>
                <p class="text-[10px] text-slate-400 truncate mt-0.5">Tüm ziyaretçileri görüntüle</p>
              </div>
            </div>
            <svg class="w-4 h-4 text-purple-400 group-hover:translate-x-1 transition-transform shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M14 5l7 7m0 0l-7 7m7-7H3"></path>
            </svg>
          </button>

          <!-- Card 2: Paneli Yenile -->
          <button
            type="button"
            on:click={loadAllData}
            class="vms-card p-5 bg-gradient-to-br from-blue-950/60 to-slate-900/90 hover:from-blue-900/80 hover:to-slate-900/90 border border-blue-800/40 hover:border-blue-500/60 rounded-2xl transition-all duration-200 text-left flex items-center justify-between group shadow-lg"
          >
            <div class="flex items-center gap-3.5 min-w-0">
              <div class="w-10 h-10 rounded-xl bg-blue-600/20 border border-blue-500/30 text-blue-300 flex items-center justify-center shrink-0">
                <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8 7V3m8 4V3m-9 8h10M5 21h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v12a2 2 0 002 2z"></path>
                </svg>
              </div>
              <div class="min-w-0">
                <p class="text-xs font-extrabold text-white truncate group-hover:text-blue-300">Paneli Yenile</p>
                <p class="text-[10px] text-slate-400 truncate mt-0.5">Verileri güncelle</p>
              </div>
            </div>
            <svg class="w-4 h-4 text-blue-400 group-hover:translate-x-1 transition-transform shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M14 5l7 7m0 0l-7 7m7-7H3"></path>
            </svg>
          </button>

          <!-- Card 3: Profil Ayarlarım -->
          <button
            type="button"
            on:click={() => navigateTo('profile')}
            class="vms-card p-5 bg-gradient-to-br from-emerald-950/60 to-slate-900/90 hover:from-emerald-900/80 hover:to-slate-900/90 border border-emerald-800/40 hover:border-emerald-500/60 rounded-2xl transition-all duration-200 text-left flex items-center justify-between group shadow-lg"
          >
            <div class="flex items-center gap-3.5 min-w-0">
              <div class="w-10 h-10 rounded-xl bg-emerald-600/20 border border-emerald-500/30 text-emerald-300 flex items-center justify-center shrink-0">
                <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M10.325 4.317c.426-1.756 2.924-1.756 3.35 0a1.724 1.724 0 002.573 1.066c1.543-.94 3.31.826 2.37 2.37a1.724 1.724 0 001.065 2.572c1.756.426 1.756 2.924 0 3.35a1.724 1.724 0 00-1.066 2.573c.94 1.543-.826 3.31-2.37 2.37a1.724 1.724 0 00-2.572 1.065c-.426 1.756-2.924 1.756-3.35 0a1.724 1.724 0 00-2.573-1.066c-1.543.94-3.31-.826-2.37-2.37a1.724 1.724 0 00-1.065-2.572c-1.756-.426-1.756-2.924 0-3.35a1.724 1.724 0 001.066-2.573c-.94-1.543.826-3.31 2.37-2.37.996.608 2.296.07 2.572-1.065z"></path>
                </svg>
              </div>
              <div class="min-w-0">
                <p class="text-xs font-extrabold text-white truncate group-hover:text-emerald-300">Profil Ayarlarım</p>
                <p class="text-[10px] text-slate-400 truncate mt-0.5">Hesap ve güvenlik ayarları</p>
              </div>
            </div>
            <svg class="w-4 h-4 text-emerald-400 group-hover:translate-x-1 transition-transform shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M14 5l7 7m0 0l-7 7m7-7H3"></path>
            </svg>
          </button>

          <!-- Card 4: Raporlar -->
          <button
            type="button"
            on:click={() => navigateTo('reports')}
            class="vms-card p-5 bg-gradient-to-br from-indigo-950/60 to-slate-900/90 hover:from-indigo-900/80 hover:to-slate-900/90 border border-indigo-800/40 hover:border-indigo-500/60 rounded-2xl transition-all duration-200 text-left flex items-center justify-between group shadow-lg"
          >
            <div class="flex items-center gap-3.5 min-w-0">
              <div class="w-10 h-10 rounded-xl bg-indigo-600/20 border border-indigo-500/30 text-indigo-300 flex items-center justify-center shrink-0">
                <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z"></path>
                </svg>
              </div>
              <div class="min-w-0">
                <p class="text-xs font-extrabold text-white truncate group-hover:text-indigo-300">Raporlar</p>
                <p class="text-[10px] text-slate-400 truncate mt-0.5">Ziyaret geçmişi raporları</p>
              </div>
            </div>
            <svg class="w-4 h-4 text-indigo-400 group-hover:translate-x-1 transition-transform shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M14 5l7 7m0 0l-7 7m7-7H3"></path>
            </svg>
          </button>
        </div>
      </div>

      <!-- 2-Column Operational Grid (AKTİF ZİYARETÇİLERİM & SON ZİYARET GEÇMİŞİ) -->
      <div class="grid grid-cols-1 lg:grid-cols-2 gap-6">

        <!-- Column 1: AKTİF ZİYARETÇİLERİM -->
        <div class="vms-card p-6 overflow-hidden flex flex-col justify-between shadow-xl">
          <div>
            <div class="flex items-center justify-between border-b border-slate-800 pb-4 mb-4">
              <div class="flex items-center gap-2.5">
                <h3 class="text-xs font-black text-white uppercase tracking-wider">AKTİF ZİYARETÇİLERİM</h3>
                <span class="px-2.5 py-0.5 bg-emerald-500/20 text-emerald-400 border border-emerald-500/30 text-[10px] font-bold rounded-full">
                  {activeVisitors.length} Aktif
                </span>
              </div>
              <button
                type="button"
                on:click={() => navigateTo('my-visitors')}
                class="text-xs font-bold text-indigo-400 hover:text-indigo-300 transition underline"
              >
                Tümünü Gör
              </button>
            </div>

            {#if loadingActive}
              <div class="p-8 text-center text-slate-400 text-xs">
                <svg class="animate-spin w-5 h-5 text-emerald-400 mx-auto mb-2" fill="none" viewBox="0 0 24 24">
                  <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
                  <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
                </svg>
                <span>Yükleniyor...</span>
              </div>
            {:else if activeVisitors.length === 0}
              <div class="text-center py-10 text-slate-400 text-xs space-y-1">
                <p class="font-bold text-slate-300">Şu an aktif ziyaretçiniz yok.</p>
                <p class="text-[10px] text-slate-500">Ziyaretçi geldiğinde burada listelenecektir.</p>
              </div>
            {:else}
              <div class="table-responsive">
                <table class="w-full text-left text-xs border-collapse">
                  <thead>
                    <tr class="bg-slate-900/80 border-b border-slate-800 text-slate-400 font-bold uppercase text-[10px] tracking-wider">
                      <th class="py-3 px-3">ZİYARETÇİ</th>
                      <th class="py-3 px-3">GİRİŞ SAATİ</th>
                      <th class="py-3 px-3">CANLI GEÇEN SÜRE</th>
                      <th class="py-3 px-3">DURUM</th>
                      <th class="py-3 px-3 text-right">İŞLEM</th>
                    </tr>
                  </thead>
                  <tbody class="divide-y divide-slate-800/60">
                    {#each activeVisitors as v}
                      <tr class="hover:bg-purple-900/20 transition">
                        <td class="py-3 px-3 font-semibold text-white flex items-center gap-2.5">
                          <div class="w-7 h-7 rounded-full bg-purple-900/60 border border-purple-500/40 text-purple-300 flex items-center justify-center font-bold text-[10px] shrink-0">
                            {v.fullName ? v.fullName.split(' ').map(n => n[0]).join('').substring(0,2).toUpperCase() : 'Z'}
                          </div>
                          <div>
                            <span class="block font-bold text-white leading-tight">{v.fullName}</span>
                            <span class="block text-[10px] text-slate-400">Ziyaretçi</span>
                          </div>
                        </td>
                        <td class="py-3 px-3 text-slate-300 font-mono text-[11px]">{formatDateTimeStr(v.entryTime, true)}</td>
                        <td class="py-3 px-3 font-mono text-emerald-400 font-extrabold text-[11px]">
                          {calculateDigitalDuration(v.entryTime, v.exitTime, true, now)}
                        </td>
                        <td class="py-3 px-3">
                          <span class="inline-flex items-center gap-1 px-2 py-0.5 bg-emerald-500/20 text-emerald-400 border border-emerald-500/30 text-[10px] font-bold rounded-full">
                            <span class="w-1.5 h-1.5 rounded-full bg-emerald-400 animate-pulse"></span>
                            İçeride
                          </span>
                        </td>
                        <td class="py-3 px-3 text-right">
                          <button
                            type="button"
                            on:click={() => openDetailModal(v)}
                            class="p-1.5 text-slate-300 hover:text-white bg-slate-800 hover:bg-slate-700 rounded-lg transition"
                            title="Görüntüle"
                          >
                            <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z"></path>
                              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7z"></path>
                            </svg>
                          </button>
                        </td>
                      </tr>
                    {/each}
                  </tbody>
                </table>
              </div>
            {/if}
          </div>
        </div>

        <!-- Column 2: SON ZİYARET GEÇMİŞİ -->
        <div class="vms-card p-6 overflow-hidden flex flex-col justify-between shadow-xl">
          <div>
            <div class="flex items-center justify-between border-b border-slate-800 pb-4 mb-4">
              <div class="flex items-center gap-2.5">
                <h3 class="text-xs font-black text-white uppercase tracking-wider">SON ZİYARET GEÇMİŞİ</h3>
                <span class="px-2.5 py-0.5 bg-indigo-500/20 text-indigo-300 border border-indigo-500/30 text-[10px] font-bold rounded-full">
                  Son {recentVisitors.length} Kayıt
                </span>
              </div>
              <button
                type="button"
                on:click={() => navigateTo('reports')}
                class="text-xs font-bold text-indigo-400 hover:text-indigo-300 transition underline"
              >
                Tümünü Gör
              </button>
            </div>

            {#if loadingRecent}
              <div class="p-8 text-center text-slate-400 text-xs">
                <svg class="animate-spin w-5 h-5 text-indigo-400 mx-auto mb-2" fill="none" viewBox="0 0 24 24">
                  <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
                  <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
                </svg>
                <span>Yükleniyor...</span>
              </div>
            {:else if recentVisitors.length === 0}
              <div class="text-center py-10 text-slate-400 text-xs space-y-1">
                <p class="font-bold text-slate-300">Henüz geçmiş ziyaret kaydınız yok.</p>
                <p class="text-[10px] text-slate-500">Tamamlanan ziyaretleriniz burada tutulacaktır.</p>
              </div>
            {:else}
              <div class="table-responsive">
                <table class="w-full text-left text-xs border-collapse">
                  <thead>
                    <tr class="bg-slate-900/80 border-b border-slate-800 text-slate-400 font-bold uppercase text-[10px] tracking-wider">
                      <th class="py-3 px-3">ZİYARETÇİ</th>
                      <th class="py-3 px-3">GİRİŞ ZAMANI</th>
                      <th class="py-3 px-3">ÇIKIŞ ZAMANI</th>
                      <th class="py-3 px-3">TOPLAM SÜRE</th>
                      <th class="py-3 px-3">DURUM</th>
                      <th class="py-3 px-3 text-right">İŞLEM</th>
                    </tr>
                  </thead>
                  <tbody class="divide-y divide-slate-800/60">
                    {#each recentVisitors as v}
                      <tr class="hover:bg-indigo-900/20 transition">
                        <td class="py-3 px-3 font-semibold text-white flex items-center gap-2.5">
                          <div class="w-7 h-7 rounded-full bg-indigo-900/60 border border-indigo-500/40 text-indigo-300 flex items-center justify-center font-bold text-[10px] shrink-0">
                            {v.fullName ? v.fullName.split(' ').map(n => n[0]).join('').substring(0,2).toUpperCase() : 'Z'}
                          </div>
                          <div>
                            <span class="block font-bold text-white leading-tight">{v.fullName}</span>
                            <span class="block text-[10px] text-slate-400">Ziyaretçi</span>
                          </div>
                        </td>
                        <td class="py-3 px-3 text-slate-300 font-mono text-[11px]">{formatDateTimeStr(v.entryTime, true)}</td>
                        <td class="py-3 px-3 text-slate-300 font-mono text-[11px]">{v.exitTime ? formatDateTimeStr(v.exitTime, true) : '-'}</td>
                        <td class="py-3 px-3 font-mono text-purple-300 font-extrabold text-[11px]">
                          {calculateDigitalDuration(v.entryTime, v.exitTime, v.isInside, now)}
                        </td>
                        <td class="py-3 px-3">
                          {#if v.isInside}
                            <span class="inline-flex items-center gap-1 px-2 py-0.5 bg-emerald-500/20 text-emerald-400 border border-emerald-500/30 text-[10px] font-bold rounded-full">
                              <span class="w-1.5 h-1.5 rounded-full bg-emerald-400 animate-pulse"></span>
                              İçeride
                            </span>
                          {:else}
                            <span class="inline-flex items-center gap-1 px-2 py-0.5 bg-slate-800 text-slate-400 border border-slate-700 text-[10px] font-bold rounded-full">
                              Çıkış Yaptı
                            </span>
                          {/if}
                        </td>
                        <td class="py-3 px-3 text-right">
                          <button
                            type="button"
                            on:click={() => openDetailModal(v)}
                            class="p-1.5 text-slate-300 hover:text-white bg-slate-800 hover:bg-slate-700 rounded-lg transition"
                            title="Görüntüle"
                          >
                            <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z"></path>
                              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7z"></path>
                            </svg>
                          </button>
                        </td>
                      </tr>
                    {/each}
                  </tbody>
                </table>
              </div>
            {/if}
          </div>
        </div>

      </div>

      <!-- BİREYSEL AKTİVİTE ÖZETİ (MY ACTIVITY) Panel -->
      <div class="vms-card p-6 bg-slate-900/90 border border-slate-800 space-y-4 shadow-xl">
        <h3 class="text-xs font-black text-white uppercase tracking-wider flex items-center gap-2">
          <svg class="w-4 h-4 text-purple-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13 10V3L4 14h7v7l9-11h-7z"></path>
          </svg>
          BİREYSEL AKTİVİTE ÖZETİ (MY ACTIVITY)
        </h3>

        <div class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
          <!-- Item 1 -->
          <div class="p-4 bg-slate-950/60 border border-slate-800/80 rounded-2xl flex items-center gap-3.5">
            <div class="w-10 h-10 rounded-xl bg-purple-500/10 text-purple-400 border border-purple-500/20 flex items-center justify-center shrink-0">
              <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8 7V3m8 4V3m-9 8h10M5 21h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v12a2 2 0 002 2z"></path>
              </svg>
            </div>
            <div>
              <p class="text-[10px] font-bold text-slate-400 uppercase tracking-wider">Bugün Size Yapılan Ziyaret</p>
              <p class="text-xl font-black text-white mt-0.5">{summary.todayVisitors}</p>
            </div>
          </div>

          <!-- Item 2 -->
          <div class="p-4 bg-slate-950/60 border border-slate-800/80 rounded-2xl flex items-center gap-3.5">
            <div class="w-10 h-10 rounded-xl bg-emerald-500/10 text-emerald-400 border border-emerald-500/20 flex items-center justify-center shrink-0">
              <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z"></path>
              </svg>
            </div>
            <div>
              <p class="text-[10px] font-bold text-slate-400 uppercase tracking-wider">Şu Anda İçerideki Ziyaretçi</p>
              <p class="text-xl font-black text-emerald-400 mt-0.5">{summary.activeVisitors}</p>
            </div>
          </div>

          <!-- Item 3 -->
          <div class="p-4 bg-slate-950/60 border border-slate-800/80 rounded-2xl flex items-center gap-3.5">
            <div class="w-10 h-10 rounded-xl bg-amber-500/10 text-amber-400 border border-amber-500/20 flex items-center justify-center shrink-0">
              <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z"></path>
              </svg>
            </div>
            <div>
              <p class="text-[10px] font-bold text-slate-400 uppercase tracking-wider">Son Ziyaret Girişi</p>
              <p class="text-sm font-extrabold text-amber-300 mt-0.5">
                {summary.lastVisitTime ? formatDateTimeStr(summary.lastVisitTime, true) : 'Henüz yapılmadı'}
              </p>
            </div>
          </div>

          <!-- Item 4 -->
          <div class="p-4 bg-slate-950/60 border border-slate-800/80 rounded-2xl flex items-center gap-3.5">
            <div class="w-10 h-10 rounded-xl bg-blue-500/10 text-blue-400 border border-blue-500/20 flex items-center justify-center shrink-0">
              <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13 7h8m0 0v8m0-8l-8 8-4-4-6 6"></path>
              </svg>
            </div>
            <div>
              <p class="text-[10px] font-bold text-slate-400 uppercase tracking-wider">Bu Ayki Ziyaret Sayısı</p>
              <p class="text-xl font-black text-white mt-0.5">{summary.totalVisitors}</p>
            </div>
          </div>
        </div>
      </div>

    </div>
  </main>
</div>
