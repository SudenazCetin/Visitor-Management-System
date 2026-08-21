<script>
  import { onMount, onDestroy, createEventDispatcher } from 'svelte';
  import Sidebar from '../components/Sidebar.svelte';
  import { getMyVisitors } from '../api/meApi.js';
  import { calculateDigitalDuration, formatDateTimeStr } from '../utils/duration.js';
  import { notificationStore } from '../stores/notificationStore.js';

  const dispatch = createEventDispatcher();
  export var activeTab = 'my-visitors';

  let isMobileOpen = false;

  let visitors = [];
  let loading = true;
  let errorMsg = '';

  let searchQuery = '';
  let statusFilter = 'ALL'; // Options: 'ALL' | 'ACTIVE' | 'EXITED'

  let now = new Date();
  let timerInterval;
  let unsubscribeSocket;

  onMount(() => {
    timerInterval = setInterval(() => {
      now = new Date();
    }, 1000);

    loadVisitors();

    // Listen to real-time WebSocket events for visitor updates
    unsubscribeSocket = notificationStore.subscribe($store => {
      if ($store.lastSocketMsg) {
        handleSocketMessage($store.lastSocketMsg);
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

  async function loadVisitors() {
    loading = true;
    errorMsg = '';
    try {
      visitors = await getMyVisitors();
    } catch (err) {
      errorMsg = err.message || 'Ziyaretçileriniz yüklenirken bir hata oluştu.';
    } finally {
      loading = false;
    }
  }

  function handleSocketMessage(msg) {
    if (!msg) return;

    const event = msg.event;
    const category = msg.category;

    if (category !== 'VISITOR' && !event?.startsWith('VISITOR_')) return;

    const payloadVisitor = msg.payload?.visitor || (msg.payload && msg.payload.fullName ? msg.payload : null);
    const targetId = msg.targetEntityId || (payloadVisitor ? payloadVisitor.id : null);

    if (event === 'VISITOR_CHECKED_IN') {
      if (payloadVisitor && payloadVisitor.id) {
        visitors = [payloadVisitor, ...visitors.filter(v => v.id !== payloadVisitor.id)];
      } else {
        loadVisitors();
      }
    } else if (event === 'VISITOR_CHECKED_OUT') {
      if (targetId) {
        visitors = visitors.map(v => {
          if (v.id === targetId) {
            const updatedExitTime = payloadVisitor?.exitTime || v.exitTime || new Date().toISOString();
            return {
              ...v,
              isInside: false,
              exitTime: updatedExitTime
            };
          }
          return v;
        });
      } else {
        loadVisitors();
      }
    } else if (event === 'VISITOR_UPDATED') {
      if (payloadVisitor && payloadVisitor.id) {
        visitors = visitors.map(v => v.id === payloadVisitor.id ? { ...v, ...payloadVisitor } : v);
      }
    }
  }

  $: filteredVisitors = visitors.filter(v => {
    const matchesSearch = v.fullName.toLowerCase().includes(searchQuery.toLowerCase());
    const matchesStatus =
      statusFilter === 'ALL' ||
      (statusFilter === 'ACTIVE' && Boolean(v.isInside)) ||
      (statusFilter === 'EXITED' && !v.isInside);

    return matchesSearch && matchesStatus;
  });

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
          <h1 class="text-2xl font-extrabold text-white tracking-tight flex items-center gap-3">
            <span>MY VISITORS</span>
            <span class="text-xs px-3 py-1 bg-purple-500/20 border border-purple-500/30 text-purple-300 font-bold rounded-full">
              {filteredVisitors.length} Kayıt
            </span>
          </h1>
          <p class="text-xs text-slate-400 mt-1">Size yapılmış olan aktif ve geçmiş tüm ziyaretlerin listesi</p>
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
      <!-- Filters Panel -->
      <div class="bg-slate-800/60 border border-slate-700/50 rounded-2xl p-4 mb-6 flex flex-col md:flex-row gap-4 justify-between items-center backdrop-blur-md">
        <!-- Search Input -->
        <div class="relative w-full md:w-80">
          <input
            type="text"
            bind:value={searchQuery}
            placeholder="Ziyaretçi ismi ile ara..."
            class="w-full pl-10 pr-4 py-2 bg-slate-900/60 border border-slate-700 rounded-xl text-slate-200 placeholder-slate-400 text-sm focus:outline-none focus:border-purple-500 focus:ring-1 focus:ring-purple-500 transition"
          />
          <svg class="w-4 h-4 text-slate-400 absolute left-3.5 top-3" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z"></path>
          </svg>
        </div>

        <!-- Status Filter Tabs -->
        <div class="flex items-center bg-slate-900/60 p-1 rounded-xl border border-slate-700/50">
          <button
            on:click={() => (statusFilter = 'ALL')}
            class="px-3.5 py-1.5 text-xs font-semibold rounded-lg transition {statusFilter === 'ALL' ? 'bg-purple-600 text-white shadow' : 'text-slate-400 hover:text-white'}"
          >
            Tümü ({visitors.length})
          </button>
          <button
            on:click={() => (statusFilter = 'ACTIVE')}
            class="px-3.5 py-1.5 text-xs font-semibold rounded-lg transition {statusFilter === 'ACTIVE' ? 'bg-purple-600 text-white shadow' : 'text-slate-400 hover:text-white'}"
          >
            Aktif ({visitors.filter(v => v.isInside).length})
          </button>
          <button
            on:click={() => (statusFilter = 'EXITED')}
            class="px-3.5 py-1.5 text-xs font-semibold rounded-lg transition {statusFilter === 'EXITED' ? 'bg-purple-600 text-white shadow' : 'text-slate-400 hover:text-white'}"
          >
            Tamamlanan ({visitors.filter(v => !v.isInside).length})
          </button>
        </div>
      </div>

      <!-- Visitors Table -->
      <div class="bg-slate-800/60 border border-slate-700/50 rounded-2xl p-6 shadow-xl backdrop-blur-md">
        {#if filteredVisitors.length === 0}
          <div class="text-center py-12 text-slate-400 text-sm">
            Aranan kriterlere uygun ziyaretçi kaydı bulunamadı.
          </div>
        {:else}
          <div class="overflow-x-auto">
            <table class="w-full text-left text-sm text-slate-300">
              <thead class="text-xs font-bold uppercase tracking-wider text-slate-400 bg-slate-900/50 border-b border-slate-700/50">
                <tr>
                  <th class="py-3.5 px-4">Ziyaretçi Adı Soyadı</th>
                  <th class="py-3.5 px-4">Giriş Zamanı</th>
                  <th class="py-3.5 px-4">Çıkış Zamanı</th>
                  <th class="py-3.5 px-4">Toplam Süre</th>
                  <th class="py-3.5 px-4">Durum</th>
                </tr>
              </thead>
              <tbody class="divide-y divide-slate-700/30">
                {#each filteredVisitors as v}
                  <tr class="hover:bg-slate-700/30 transition">
                    <td class="py-4 px-4 font-semibold text-white">{v.fullName}</td>
                    <td class="py-4 px-4 text-slate-300">{formatDateTimeStr(v.entryTime, true)}</td>
                    <td class="py-4 px-4 text-slate-300">{formatDateTimeStr(v.exitTime, true)}</td>
                    <td class="py-4 px-4 text-purple-300 font-medium font-mono tracking-wider">{calculateDigitalDuration(v.entryTime, v.exitTime, v.isInside, now)}</td>
                    <td class="py-4 px-4">
                      {#if v.isInside}
                        <span class="inline-flex items-center gap-1.5 px-2.5 py-1 bg-emerald-500/20 text-emerald-400 border border-emerald-500/30 text-xs font-semibold rounded-full">
                          <span class="w-1.5 h-1.5 rounded-full bg-emerald-400 animate-pulse"></span>
                          İçeride (Aktif)
                        </span>
                      {:else}
                        <span class="inline-flex items-center gap-1.5 px-2.5 py-1 bg-slate-700/50 text-slate-400 border border-slate-600/30 text-xs font-semibold rounded-full">
                          Çıkış Yaptı
                        </span>
                      {/if}
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
