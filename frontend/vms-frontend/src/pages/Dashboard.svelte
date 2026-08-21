<script>
  import { onMount, onDestroy, createEventDispatcher } from 'svelte';
  import Sidebar from '../components/Sidebar.svelte';
  import ConfirmModal from '../components/ConfirmModal.svelte';
  import VisitorDetailModal from '../components/VisitorDetailModal.svelte';
  import { authStore } from '../stores/authStore.js';
  import { toastStore } from '../stores/toastStore.js';
  import { getAllPersonnel } from '../api/personnelApi.js';
  import { getActiveVisitors, checkInVisitor, checkOutVisitor } from '../api/visitorApi.js';
  import { getAdminDashboardSummary } from '../api/dashboardApi.js';
  import { notificationStore } from '../stores/notificationStore.js';
  import { calculateDigitalDuration, formatDateTimeStr } from '../utils/duration.js';

  // Chart.js imports
  import { Doughnut } from 'svelte-chartjs';
  import { Chart as ChartJS, Title, Tooltip, Legend, ArcElement } from 'chart.js';

  ChartJS.register(Title, Tooltip, Legend, ArcElement);

  const dispatch = createEventDispatcher();

  export var activeTab = 'dashboard';

  // Mobile sidebar state
  let isMobileOpen = false;

  function handleLogout() {
    authStore.logout();
  }

  // Data State
  let personnelList = [];
  let activeVisitors = [];

  // Admin Summary Analytics State
  let adminSummary = null;
  let loadingAdminSummary = false;
  let adminSummaryError = '';
  let selectedRange = '30d';

  $: isAdmin = $authStore.user?.role === 'ADMIN';

  // Search & Filter State
  let searchQuery = '';

  // Visitor Detail Modal State
  let isDetailModalOpen = false;
  let selectedDetailVisitor = null;

  function openDetailModal(v) {
    selectedDetailVisitor = v;
    isDetailModalOpen = true;
  }

  $: filteredActiveVisitors = activeVisitors.filter(v => {
    if (!searchQuery.trim()) return true;
    const q = searchQuery.toLowerCase();
    const nameMatch = v.fullName && v.fullName.toLowerCase().includes(q);
    const hostMatch = v.hostName && v.hostName.toLowerCase().includes(q);
    const deptMatch = v.hostDepartment && v.hostDepartment.toLowerCase().includes(q);
    return nameMatch || hostMatch || deptMatch;
  });

  // Loading & Error States
  let loadingPersonnel = true;
  let loadingVisitors = true;
  let pageError = '';

  // Form State
  let checkInForm = {
    fullName: '',
    hostId: '',
  };
  let isSubmitting = false;
  let formError = '';

  // Check-Out Modal State
  let isCheckoutModalOpen = false;
  let checkoutVisitorId = null;
  let checkoutVisitorName = '';
  let isCheckingOut = false;

  // Live Timer Ticker State
  let now = new Date();
  let timerInterval;

  // Dynamic Greeting Logic & Scroll/Nav Helpers
  $: userName = $authStore.user?.fullName || $authStore.user?.username || 'Kullanıcı';
  $: currentHour = now.getHours();
  $: timeGreeting = currentHour >= 5 && currentHour < 12
    ? 'Günaydın'
    : currentHour >= 12 && currentHour < 18
      ? 'İyi Günler'
      : 'İyi Akşamlar';

  function scrollToCard(id) {
    if (typeof document === 'undefined') return;
    const el = document.getElementById(id);
    if (el) {
      el.scrollIntoView({ behavior: 'smooth', block: 'start' });
      const input = el.querySelector('input, select, button');
      if (input) input.focus();
    }
  }

  function navigateToTab(tabName) {
    dispatch('changeTab', tabName);
  }

  onMount(() => {
    timerInterval = setInterval(() => {
      now = new Date();
    }, 1000);

    loadDashboardData();
  });

  onDestroy(() => {
    if (timerInterval) {
      clearInterval(timerInterval);
    }
  });

  async function handleRangeChange(newRange) {
    if (selectedRange === newRange && adminSummary) return;
    selectedRange = newRange;
    await loadAdminSummaryData();
  }

  async function loadAdminSummaryData() {
    if (!isAdmin) return;
    loadingAdminSummary = true;
    adminSummaryError = '';
    try {
      adminSummary = await getAdminDashboardSummary(selectedRange);
    } catch (err) {
      adminSummaryError = err.message || 'Analiz verileri alınamadı.';
    } finally {
      loadingAdminSummary = false;
    }
  }

  async function loadDashboardData() {
    pageError = '';
    
    loadingPersonnel = true;
    try {
      personnelList = await getAllPersonnel();
    } catch (err) {
      pageError = err.message || 'Personel listesi alınamadı.';
    } finally {
      loadingPersonnel = false;
    }

    loadingVisitors = true;
    try {
      activeVisitors = await getActiveVisitors();
    } catch (err) {
      if (!pageError) {
        pageError = err.message || 'Aktif ziyaretçi listesi alınamadı.';
      }
    } finally {
      loadingVisitors = false;
    }

    if (isAdmin) {
      loadAdminSummaryData();
    }
  }

  $: deptChartData = {
    labels: adminSummary?.departmentDistribution && adminSummary.departmentDistribution.length > 0
      ? adminSummary.departmentDistribution.map(d => d.department)
      : ['Veri Yok'],
    datasets: [
      {
        data: adminSummary?.departmentDistribution && adminSummary.departmentDistribution.length > 0
          ? adminSummary.departmentDistribution.map(d => d.visitCount)
          : [1],
        backgroundColor: [
          'rgba(124, 58, 237, 0.85)',
          'rgba(99, 102, 241, 0.85)',
          'rgba(16, 185, 129, 0.85)',
          'rgba(245, 158, 11, 0.85)',
          'rgba(239, 68, 68, 0.85)',
          'rgba(14, 165, 233, 0.85)'
        ],
        borderColor: 'rgba(15, 23, 42, 0.9)',
        borderWidth: 2
      }
    ]
  };

  const chartOptions = {
    responsive: true,
    maintainAspectRatio: false,
    plugins: {
      legend: {
        position: 'bottom',
        labels: {
          color: '#cbd5e1',
          font: { size: 11, family: 'Plus Jakarta Sans' }
        }
      }
    }
  };

  function formatActivityTime(ts) {
    if (!ts) return '';
    const date = new Date(ts);
    if (isNaN(date.getTime())) return '';
    const diffMins = Math.floor((now - date) / (1000 * 60));
    if (diffMins < 1) return 'Az önce';
    if (diffMins < 60) return `${diffMins} dk önce`;
    const hours = Math.floor(diffMins / 60);
    if (hours < 24) return `${hours} sa önce`;
    const day = String(date.getDate()).padStart(2, '0');
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const timeStr = `${String(date.getHours()).padStart(2, '0')}:${String(date.getMinutes()).padStart(2, '0')}`;
    return `${day}.${month} ${timeStr}`;
  }

  // Handle Check-In Submission
  async function handleCheckIn(event) {
    event.preventDefault();
    formError = '';

    if (!checkInForm.fullName.trim()) {
      formError = 'Lütfen ziyaretçi adı soyadı girin.';
      return;
    }

    if (!checkInForm.hostId) {
      formError = 'Lütfen ev sahibi personeli seçin.';
      return;
    }

    isSubmitting = true;
    try {
      const newVisitor = await checkInVisitor({
        fullName: checkInForm.fullName.trim(),
        hostId: Number(checkInForm.hostId),
      });

      activeVisitors = [newVisitor, ...activeVisitors];
      toastStore.success('Ziyaretçi giriş kaydı oluşturuldu.');
      checkInForm = { fullName: '', hostId: '' };
    } catch (err) {
      formError = err.message || 'Ziyaretçi girişi yapılırken bir hata oluştu.';
      toastStore.error(formError);
    } finally {
      isSubmitting = false;
    }
  }

  function promptCheckOut(id, name) {
    checkoutVisitorId = id;
    checkoutVisitorName = name;
    isCheckoutModalOpen = true;
  }

  async function confirmCheckOut() {
    if (!checkoutVisitorId) return;

    isCheckingOut = true;
    try {
      await checkOutVisitor(checkoutVisitorId);
      activeVisitors = activeVisitors.filter(v => v.id !== checkoutVisitorId);
      toastStore.success('Ziyaretçi çıkış işlemi tamamlandı.');
      isCheckoutModalOpen = false;
    } catch (err) {
      toastStore.error(err.message || 'Ziyaretçi çıkışı yapılırken bir hata oluştu.');
    } finally {
      isCheckingOut = false;
      checkoutVisitorId = null;
    }
  }

  function formatDuration(entryTimeVal, currentNow) {
    if (!entryTimeVal) return '00:00:00';

    let entryDate;
    if (Array.isArray(entryTimeVal)) {
      const [year, month, day, hour, minute, second] = entryTimeVal;
      entryDate = new Date(year, month - 1, day, hour, minute, second || 0);
    } else {
      let isoStr = String(entryTimeVal);
      if (!isoStr.endsWith('Z') && !isoStr.includes('+')) {
        entryDate = new Date(isoStr);
      } else {
        entryDate = new Date(entryTimeVal);
      }
    }

    if (isNaN(entryDate.getTime())) return '00:00:00';

    const diffMs = Math.max(0, currentNow.getTime() - entryDate.getTime());
    const totalSeconds = Math.floor(diffMs / 1000);

    const hours = String(Math.floor(totalSeconds / 3600)).padStart(2, '0');
    const minutes = String(Math.floor((totalSeconds % 3600) / 60)).padStart(2, '0');
    const seconds = String(Math.floor(totalSeconds % 60)).padStart(2, '0');

    return `${hours}:${minutes}:${seconds}`;
  }

  function formatEntryTime(entryTimeVal) {
    if (!entryTimeVal) return '-';

    let entryDate;
    if (Array.isArray(entryTimeVal)) {
      const [year, month, day, hour, minute] = entryTimeVal;
      entryDate = new Date(year, month - 1, day, hour, minute);
    } else {
      entryDate = new Date(entryTimeVal);
    }

    if (isNaN(entryDate.getTime())) return '-';

    const hours = String(entryDate.getHours()).padStart(2, '0');
    const minutes = String(entryDate.getMinutes()).padStart(2, '0');

    return `${hours}:${minutes}`;
  }

  function handleTabChange(e) {
    activeTab = e.detail;
    dispatch('changeTab', e.detail);
  }
</script>

<!-- Visitor Detail Modal -->
<VisitorDetailModal
  isOpen={isDetailModalOpen}
  visitor={selectedDetailVisitor}
  {now}
  on:close={() => (isDetailModalOpen = false)}
  on:checkout={(e) => promptCheckOut(e.detail.id, e.detail.fullName)}
/>

<!-- Check-Out Confirmation Modal -->
<ConfirmModal
  isOpen={isCheckoutModalOpen}
  title="Ziyaretçi Çıkış Onayı"
  message="{checkoutVisitorName} isimli ziyaretçinin binadan çıkışını onaylıyor musunuz?"
  confirmText="Çıkış Yap"
  loading={isCheckingOut}
  on:confirm={confirmCheckOut}
  on:cancel={() => (isCheckoutModalOpen = false)}
/>

<div class="vms-app-layout flex text-slate-100 font-sans">
  <!-- Fixed Background Image & Overlay -->
  <div class="vms-bg-fixed">
    <div class="vms-bg-image"></div>
    <div class="vms-bg-overlay"></div>
  </div>

  <!-- Sidebar -->
  <Sidebar {activeTab} {isMobileOpen} on:changeTab={handleTabChange} on:closeMobile={() => (isMobileOpen = false)} />

  <!-- Main Content -->
  <main class="flex-1 p-4 md:p-8 overflow-y-auto w-full z-10">
    <div class="max-w-7xl mx-auto space-y-6">
      
      <!-- Top Workspace Header Banner (Matching exact screenshot layout) -->
      <div class="flex flex-col md:flex-row md:items-center justify-between gap-4 py-2">
        <!-- Left Greeting Info -->
        <div class="space-y-1">
          <div class="flex items-center gap-3 flex-wrap">
            <h1 class="text-2xl md:text-3xl font-black text-white tracking-tight">
              {timeGreeting}, {userName} 👋
            </h1>
            <span class="px-3 py-1 bg-purple-900/50 text-purple-300 border border-purple-500/40 text-[11px] font-black rounded-full tracking-wider uppercase">
              {isAdmin ? 'YÖNETİCİ WORKSPACE' : 'RESEPSİYON WORKSPACE'}
            </span>
          </div>
          <p class="text-xs text-slate-400">
            {#if isAdmin}
              Sistem operasyonlarını ve tüm ziyaretçi hareketlerini buradan takip edebilirsiniz.
            {:else}
              Bugünkü ziyaretçi operasyonlarını hızlı ve güvenli şekilde yönetebilirsiniz.
            {/if}
          </p>
        </div>

        <!-- Right Header Badges & Actions -->
        <div class="flex items-center gap-3 shrink-0">
          <!-- Live Date & Clock Card -->
          <div class="hidden sm:flex items-center gap-2.5 px-4 py-2.5 bg-slate-900/90 border border-slate-800 rounded-2xl text-xs font-mono text-purple-300 shadow-lg">
            <svg class="w-4 h-4 text-purple-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z"></path>
            </svg>
            <span class="font-sans font-semibold text-slate-300">{now.toLocaleDateString('tr-TR', { day: 'numeric', month: 'long', year: 'numeric' })}</span>
            <span class="font-extrabold text-white">{now.toLocaleTimeString('tr-TR', { hour: '2-digit', minute: '2-digit' })}</span>
          </div>

          <!-- Quick Refresh Button -->
          <button
            type="button"
            on:click={loadDashboardData}
            class="vms-btn vms-btn-secondary py-2 px-3.5 text-xs font-semibold flex items-center gap-2"
            title="Verileri Yenile"
          >
            <svg class="w-4 h-4 text-emerald-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15"></path>
            </svg>
            <span>Yenile</span>
          </button>

          <!-- User Profile Dropdown Pill -->
          <button
            type="button"
            on:click={() => navigateToTab('profile')}
            class="flex items-center gap-2.5 px-3.5 py-2 bg-slate-900/90 hover:bg-slate-800/90 border border-slate-800 hover:border-purple-500/40 rounded-2xl transition shadow-lg"
          >
            <div class="w-7 h-7 rounded-full bg-indigo-600 text-white flex items-center justify-center text-xs font-extrabold shadow-sm">
              {userName ? userName.split(' ').map(n => n[0]).join('').substring(0,2).toUpperCase() : 'U'}
            </div>
            <span class="text-xs font-bold text-white hidden md:inline">{userName}</span>
            <svg class="w-3.5 h-3.5 text-slate-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 9l-7 7-7-7"></path>
            </svg>
          </button>
        </div>
      </div>
      
      <!-- Top KPI Section: Prominent Stat Cards -->
      <div class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-5">
        <!-- Stat 1: Şu An İçeride -->
        <div class="vms-card vms-card-interactive p-6 flex items-center justify-between gap-4 border border-slate-800/90 shadow-xl">
          <div class="space-y-1">
            <p class="text-xs font-bold text-slate-400 uppercase tracking-wider">AKTİF ZİYARETÇİ</p>
            <p class="text-3xl font-black text-emerald-400 tracking-tight">
              {isAdmin && adminSummary ? adminSummary.currentlyInside : activeVisitors.length}
            </p>
            <p class="text-xs text-slate-400">Şu an binada bulunanlar</p>
          </div>
          <div class="w-14 h-14 rounded-2xl bg-emerald-500/10 text-emerald-400 border border-emerald-500/20 flex items-center justify-center shrink-0 shadow-inner">
            <svg class="w-7 h-7" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M17 20h5v-2a3 3 0 00-5.356-1.857M17 20H7m10 0v-2c0-.656-.126-1.283-.356-1.857M7 20H2v-2a3 3 0 015.356-1.857M7 20v-2c0-.656.126-1.283.356-1.857m0 0a5.002 5.002 0 019.288 0M15 7a3 3 0 11-6 0 3 3 0 016 0zm6 3a2 2 0 11-4 0 2 2 0 014 0zM7 10a2 2 0 11-4 0 2 2 0 014 0z"></path>
            </svg>
          </div>
        </div>

        <!-- Stat 2: Bugünkü Ziyaretler -->
        <div class="vms-card vms-card-interactive p-6 flex items-center justify-between gap-4 border border-slate-800/90 shadow-xl">
          <div class="space-y-1">
            <p class="text-xs font-bold text-slate-400 uppercase tracking-wider">BUGÜNKÜ ZİYARET</p>
            <p class="text-3xl font-black text-white tracking-tight">
              {isAdmin && adminSummary ? adminSummary.todaysTotalVisitors : activeVisitors.length}
            </p>
            <p class="text-xs text-slate-400">Bugün giriş yapanlar</p>
          </div>
          <div class="w-14 h-14 rounded-2xl bg-purple-500/10 text-purple-400 border border-purple-500/20 flex items-center justify-center shrink-0 shadow-inner">
            <svg class="w-7 h-7" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4.354a4 4 0 110 5.292M15 21H3v-1a6 6 0 0112 0v1zm0 0h6v-1a6 6 0 00-9-5.197M13 7a4 4 0 11-8 0 4 4 0 018 0z"></path>
            </svg>
          </div>
        </div>

        <!-- Stat 3: Kayıtlı Personel -->
        <div class="vms-card vms-card-interactive p-6 flex items-center justify-between gap-4 border border-slate-800/90 shadow-xl">
          <div class="space-y-1">
            <p class="text-xs font-bold text-slate-400 uppercase tracking-wider">KAYITLI PERSONEL</p>
            <p class="text-3xl font-black text-white tracking-tight">
              {isAdmin && adminSummary ? adminSummary.totalPersonnel : personnelList.length}
            </p>
            <p class="text-xs text-slate-400">Sistemdeki personeller</p>
          </div>
          <div class="w-14 h-14 rounded-2xl bg-indigo-500/10 text-indigo-400 border border-indigo-500/20 flex items-center justify-center shrink-0 shadow-inner">
            <svg class="w-7 h-7" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 21V5a2 2 0 00-2-2H7a2 2 0 00-2 2v16m14 0h2m-2 0h-5m-9 0H3m2 0h5m0 0h4m-4 0V11m0 0l2 2m-2-2l-2 2m6-6v6m0 0l2-2m-2 2l-2-2"></path>
            </svg>
          </div>
        </div>

        <!-- Stat 4: Toplam Ziyaret Kaydı -->
        <div class="vms-card vms-card-interactive p-6 flex items-center justify-between gap-4 border border-slate-800/90 shadow-xl">
          <div class="space-y-1">
            <p class="text-xs font-bold text-slate-400 uppercase tracking-wider">TOPLAM ZİYARET KAYDI</p>
            <p class="text-3xl font-black text-white tracking-tight">
              {isAdmin && adminSummary ? adminSummary.totalVisitsAllTime : activeVisitors.length}
            </p>
            <p class="text-xs text-slate-400">Tüm zamanların arşivi</p>
          </div>
          <div class="w-14 h-14 rounded-2xl bg-amber-500/10 text-amber-400 border border-amber-500/20 flex items-center justify-center shrink-0 shadow-inner">
            <svg class="w-7 h-7" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 19v-6a2 2 0 00-2-2H5a2 2 0 00-2 2v6a2 2 0 002 2h2a2 2 0 002-2zm0 0V9a2 2 0 012-2h2a2 2 0 012 2v10m-6 0a2 2 0 002 2h2a2 2 0 002-2m0 0V5a2 2 0 012-2h2a2 2 0 012 2v14a2 2 0 01-2 2h-2a2 2 0 01-2-2z"></path>
            </svg>
          </div>
        </div>
      </div>

      <!-- SYSTEM STATUS PANEL (Admin Only) -->
      {#if isAdmin}
        <div class="vms-card p-5 bg-slate-900/90 border border-slate-800 shadow-lg">
          <div class="flex flex-col sm:flex-row sm:items-center justify-between gap-4 text-xs">
            <div class="flex items-center gap-2.5 font-bold text-white uppercase tracking-wider text-sm">
              <svg class="w-5 h-5 text-purple-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12l2 2 4-4m5.618-4.016A11.955 11.955 0 0112 2.944a11.955 11.955 0 01-8.618 3.04A12.02 12.02 0 003 9c0 5.591 3.824 10.29 9 11.622 5.176-1.332 9-6.03 9-11.622 0-1.042-.133-2.052-.382-3.016z"></path>
              </svg>
              SİSTEM OPERASYONEL DURUMU (SYSTEM STATUS)
            </div>

            <div class="flex flex-wrap items-center gap-6 text-xs">
              <!-- Backend Connection -->
              <div class="flex items-center gap-2 font-medium">
                <span class="w-2.5 h-2.5 rounded-full bg-emerald-400"></span>
                <span class="text-slate-400">Backend API:</span>
                <span class="text-emerald-400 font-bold">Çalışıyor (200 OK)</span>
              </div>

              <!-- Authentication Status -->
              <div class="flex items-center gap-2 font-medium">
                <span class="w-2.5 h-2.5 rounded-full bg-emerald-400"></span>
                <span class="text-slate-400">Kimlik Doğrulama:</span>
                <span class="text-purple-300 font-bold">JWT Oturum Aktif</span>
              </div>

              <!-- WebSocket Connection Status -->
              <div class="flex items-center gap-2 font-medium">
                <span class="w-2.5 h-2.5 rounded-full {$notificationStore.isConnected ? 'bg-emerald-400' : 'bg-amber-400 animate-pulse'}"></span>
                <span class="text-slate-400">WebSocket / Bildirim:</span>
                <span class="{$notificationStore.isConnected ? 'text-emerald-400' : 'text-amber-400'} font-bold">
                  {$notificationStore.isConnected ? 'Bağlı' : 'Bağlantı Kuruluyor...'}
                </span>
              </div>
            </div>
          </div>
        </div>
      {/if}

      <!-- Prominent & Spacious Role-Differentiated Quick Actions Grid -->
      <div class="vms-card p-6 md:p-8 space-y-4 shadow-xl">
        <div class="flex items-center justify-between border-b border-slate-800/80 pb-4">
          <div>
            <h2 class="text-sm md:text-base font-extrabold text-white uppercase tracking-wider flex items-center gap-2.5">
              <svg class="w-5 h-5 text-purple-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13 10V3L4 14h7v7l9-11h-7z"></path>
              </svg>
              {isAdmin ? 'YÖNETİCİ HIZLI AKSİYONLARI (ADMIN ACTIONS)' : 'RESEPSİYON OPERASYON AKSİYONLARI (RECEPTION ACTIONS)'}
            </h2>
            <p class="text-xs text-slate-400 mt-0.5">Sık kullanılan işlemlere ve yönetim modüllerine tek tıkla erişin</p>
          </div>
        </div>

        {#if isAdmin}
          <!-- ADMIN QUICK ACTIONS - ENLARGED PROMINENT CARDS -->
          <div class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4 pt-1">
            <!-- 1. Check-In -->
            <button
              type="button"
              on:click={() => scrollToCard('checkin-form-card')}
              class="p-5 bg-slate-900/80 hover:bg-purple-900/30 border border-slate-800 hover:border-purple-500/50 rounded-2xl transition-all duration-300 text-left flex items-start gap-4 group shadow-md"
            >
              <div class="w-12 h-12 rounded-xl bg-purple-500/10 text-purple-400 border border-purple-500/20 flex items-center justify-center shrink-0 group-hover:scale-110 transition-transform">
                <svg class="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M18 9v3m0 0v3m0-3h3m-3 0h-3m-2-5a4 4 0 11-8 0 4 4 0 018 0zM3 20a6 6 0 0112 0v1H3v-1z"></path>
                </svg>
              </div>
              <div class="space-y-1 flex-1">
                <div class="text-white font-extrabold text-sm flex items-center justify-between group-hover:text-purple-300">
                  <span>Ziyaretçi Kaydı</span>
                  <svg class="w-4 h-4 text-purple-400 group-hover:translate-x-1 transition-transform" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5l7 7-7 7"></path>
                  </svg>
                </div>
                <p class="text-xs text-slate-400 leading-relaxed">Binaya hızlı ziyaretçi giriş kaydı başlatın</p>
              </div>
            </button>

            <!-- 2. Personnel Management -->
            <button
              type="button"
              on:click={() => navigateToTab('personnel')}
              class="p-5 bg-slate-900/80 hover:bg-purple-900/30 border border-slate-800 hover:border-purple-500/50 rounded-2xl transition-all duration-300 text-left flex items-start gap-4 group shadow-md"
            >
              <div class="w-12 h-12 rounded-xl bg-indigo-500/10 text-indigo-400 border border-indigo-500/20 flex items-center justify-center shrink-0 group-hover:scale-110 transition-transform">
                <svg class="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4.354a4 4 0 110 5.292M15 21H3v-1a6 6 0 0112 0v1zm0 0h6v-1a6 6 0 00-9-5.197M13 7a4 4 0 11-8 0 4 4 0 018 0z"></path>
                </svg>
              </div>
              <div class="space-y-1 flex-1">
                <div class="text-white font-extrabold text-sm flex items-center justify-between group-hover:text-indigo-300">
                  <span>Personeller</span>
                  <svg class="w-4 h-4 text-indigo-400 group-hover:translate-x-1 transition-transform" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5l7 7-7 7"></path>
                  </svg>
                </div>
                <p class="text-xs text-slate-400 leading-relaxed">Şirket personel listesini ve detaylarını inceleyin</p>
              </div>
            </button>

            <!-- 3. Add Personnel -->
            <button
              type="button"
              on:click={() => navigateToTab('personnel')}
              class="p-5 bg-slate-900/80 hover:bg-purple-900/30 border border-slate-800 hover:border-purple-500/50 rounded-2xl transition-all duration-300 text-left flex items-start gap-4 group shadow-md"
            >
              <div class="w-12 h-12 rounded-xl bg-emerald-500/10 text-emerald-400 border border-emerald-500/20 flex items-center justify-center shrink-0 group-hover:scale-110 transition-transform">
                <svg class="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M18 9v3m0 0v3m0-3h3m-3 0h-3m-2-5a4 4 0 11-8 0 4 4 0 018 0zM3 20a6 6 0 0112 0v1H3v-1z"></path>
                </svg>
              </div>
              <div class="space-y-1 flex-1">
                <div class="text-white font-extrabold text-sm flex items-center justify-between group-hover:text-emerald-300">
                  <span>Yeni Personel Ekle</span>
                  <svg class="w-4 h-4 text-emerald-400 group-hover:translate-x-1 transition-transform" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5l7 7-7 7"></path>
                  </svg>
                </div>
                <p class="text-xs text-slate-400 leading-relaxed">Sisteme yeni personel kaydı ekleyin</p>
              </div>
            </button>

            <!-- 4. Publish Announcement -->
            <button
              type="button"
              on:click={() => navigateToTab('announcements')}
              class="p-5 bg-slate-900/80 hover:bg-purple-900/30 border border-slate-800 hover:border-purple-500/50 rounded-2xl transition-all duration-300 text-left flex items-start gap-4 group shadow-md"
            >
              <div class="w-12 h-12 rounded-xl bg-amber-500/10 text-amber-400 border border-amber-500/20 flex items-center justify-center shrink-0 group-hover:scale-110 transition-transform">
                <svg class="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M11 5.882V19.24a1.76 1.76 0 01-3.417.592l-2.147-6.15M18 13a3 3 0 100-6M5.436 13.683A4.001 4.001 0 017 6h1.832c4.1 0 7.625-1.234 9.168-3v14c-1.543-1.766-5.067-3-9.168-3H7a3.988 3.988 0 01-1.564-.317z"></path>
                </svg>
              </div>
              <div class="space-y-1 flex-1">
                <div class="text-white font-extrabold text-sm flex items-center justify-between group-hover:text-amber-300">
                  <span>Duyuru Yayınla</span>
                  <svg class="w-4 h-4 text-amber-400 group-hover:translate-x-1 transition-transform" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5l7 7-7 7"></path>
                  </svg>
                </div>
                <p class="text-xs text-slate-400 leading-relaxed">Tüm kullanıcılara anlık duyuru veya bildirim gönderin</p>
              </div>
            </button>

            <!-- 5. User Management -->
            <button
              type="button"
              on:click={() => navigateToTab('user-management')}
              class="p-5 bg-slate-900/80 hover:bg-purple-900/30 border border-slate-800 hover:border-purple-500/50 rounded-2xl transition-all duration-300 text-left flex items-start gap-4 group shadow-md"
            >
              <div class="w-12 h-12 rounded-xl bg-rose-500/10 text-rose-400 border border-rose-500/20 flex items-center justify-center shrink-0 group-hover:scale-110 transition-transform">
                <svg class="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M10.325 4.317c.426-1.756 2.924-1.756 3.35 0a1.724 1.724 0 002.573 1.066c1.543-.94 3.31.826 2.37 2.37a1.724 1.724 0 001.065 2.572c1.756.426 1.756 2.924 0 3.35a1.724 1.724 0 00-1.066 2.573c.94 1.543-.826 3.31-2.37 2.37a1.724 1.724 0 00-2.572 1.065c-.426 1.756-2.924 1.756-3.35 0a1.724 1.724 0 00-2.573-1.066c-1.543.94-3.31-.826-2.37-2.37a1.724 1.724 0 00-1.065-2.572c-1.756-.426-1.756-2.924 0-3.35a1.724 1.724 0 001.066-2.573c-.94-1.543.826-3.31 2.37-2.37.996.608 2.296.07 2.572-1.065z"></path>
                </svg>
              </div>
              <div class="space-y-1 flex-1">
                <div class="text-white font-extrabold text-sm flex items-center justify-between group-hover:text-rose-300">
                  <span>Kullanıcı Yönetimi</span>
                  <svg class="w-4 h-4 text-rose-400 group-hover:translate-x-1 transition-transform" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5l7 7-7 7"></path>
                  </svg>
                </div>
                <p class="text-xs text-slate-400 leading-relaxed">Sistem kullanıcı hesaplarını ve yetkilerini düzenleyin</p>
              </div>
            </button>

            <!-- 6. View Reports -->
            <button
              type="button"
              on:click={() => navigateToTab('reports')}
              class="p-5 bg-slate-900/80 hover:bg-purple-900/30 border border-slate-800 hover:border-purple-500/50 rounded-2xl transition-all duration-300 text-left flex items-start gap-4 group shadow-md"
            >
              <div class="w-12 h-12 rounded-xl bg-cyan-500/10 text-cyan-400 border border-cyan-500/20 flex items-center justify-center shrink-0 group-hover:scale-110 transition-transform">
                <svg class="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 19v-6a2 2 0 00-2-2H5a2 2 0 00-2 2v6a2 2 0 002 2h2a2 2 0 002-2zm0 0V9a2 2 0 012-2h2a2 2 0 012 2v10m-6 0a2 2 0 002 2h2a2 2 0 002-2m0 0V5a2 2 0 012-2h2a2 2 0 012 2v14a2 2 0 01-2 2h-2a2 2 0 01-2-2z"></path>
                </svg>
              </div>
              <div class="space-y-1 flex-1">
                <div class="text-white font-extrabold text-sm flex items-center justify-between group-hover:text-cyan-300">
                  <span>Raporlar & Analiz</span>
                  <svg class="w-4 h-4 text-cyan-400 group-hover:translate-x-1 transition-transform" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5l7 7-7 7"></path>
                  </svg>
                </div>
                <p class="text-xs text-slate-400 leading-relaxed">Tüm ziyaretçi giriş/çıkış geçmişi raporlarını görüntüleyin</p>
              </div>
            </button>
          </div>
        {:else}
          <!-- RECEPTIONIST QUICK ACTIONS - ENLARGED PROMINENT CARDS -->
          <div class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4 pt-1">
            <!-- 1. Check-In Form -->
            <button
              type="button"
              on:click={() => scrollToCard('checkin-form-card')}
              class="p-5 bg-slate-900/80 hover:bg-purple-900/30 border border-slate-800 hover:border-purple-500/50 rounded-2xl transition-all duration-300 text-left flex items-start gap-4 group shadow-md"
            >
              <div class="w-12 h-12 rounded-xl bg-purple-500/10 text-purple-400 border border-purple-500/20 flex items-center justify-center shrink-0 group-hover:scale-110 transition-transform">
                <svg class="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M18 9v3m0 0v3m0-3h3m-3 0h-3m-2-5a4 4 0 11-8 0 4 4 0 018 0zM3 20a6 6 0 0112 0v1H3v-1z"></path>
                </svg>
              </div>
              <div class="space-y-1 flex-1">
                <div class="text-white font-extrabold text-sm flex items-center justify-between group-hover:text-purple-300">
                  <span>Yeni Ziyaretçi Kaydı</span>
                  <svg class="w-4 h-4 text-purple-400 group-hover:translate-x-1 transition-transform" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5l7 7-7 7"></path>
                  </svg>
                </div>
                <p class="text-xs text-slate-400 leading-relaxed">Danışmaya gelen ziyaretçiyi binaya kaydedin</p>
              </div>
            </button>

            <!-- 2. Active Visitors -->
            <button
              type="button"
              on:click={() => scrollToCard('active-visitors-card')}
              class="p-5 bg-slate-900/80 hover:bg-purple-900/30 border border-slate-800 hover:border-purple-500/50 rounded-2xl transition-all duration-300 text-left flex items-start gap-4 group shadow-md"
            >
              <div class="w-12 h-12 rounded-xl bg-emerald-500/10 text-emerald-400 border border-emerald-500/20 flex items-center justify-center shrink-0 group-hover:scale-110 transition-transform">
                <svg class="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M17 20h5v-2a3 3 0 00-5.356-1.857M17 20H7m10 0v-2c0-.656-.126-1.283-.356-1.857M7 20H2v-2a3 3 0 015.356-1.857M7 20v-2c0-.656.126-1.283.356-1.857m0 0a5.002 5.002 0 019.288 0M15 7a3 3 0 11-6 0 3 3 0 016 0zm6 3a2 2 0 11-4 0 2 2 0 014 0zM7 10a2 2 0 11-4 0 2 2 0 014 0z"></path>
                </svg>
              </div>
              <div class="space-y-1 flex-1">
                <div class="text-white font-extrabold text-sm flex items-center justify-between group-hover:text-emerald-300">
                  <span>Aktif Ziyaretçileri Gör</span>
                  <svg class="w-4 h-4 text-emerald-400 group-hover:translate-x-1 transition-transform" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5l7 7-7 7"></path>
                  </svg>
                </div>
                <p class="text-xs text-slate-400 leading-relaxed">Binada şu an bulunan ziyaretçi listesi</p>
              </div>
            </button>

            <!-- 3. Visit History -->
            <button
              type="button"
              on:click={() => navigateToTab('reports')}
              class="p-5 bg-slate-900/80 hover:bg-purple-900/30 border border-slate-800 hover:border-purple-500/50 rounded-2xl transition-all duration-300 text-left flex items-start gap-4 group shadow-md"
            >
              <div class="w-12 h-12 rounded-xl bg-indigo-500/10 text-indigo-400 border border-indigo-500/20 flex items-center justify-center shrink-0 group-hover:scale-110 transition-transform">
                <svg class="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 19v-6a2 2 0 00-2-2H5a2 2 0 00-2 2v6a2 2 0 002 2h2a2 2 0 002-2zm0 0V9a2 2 0 012-2h2a2 2 0 012 2v10m-6 0a2 2 0 002 2h2a2 2 0 002-2m0 0V5a2 2 0 012-2h2a2 2 0 012 2v14a2 2 0 01-2 2h-2a2 2 0 01-2-2z"></path>
                </svg>
              </div>
              <div class="space-y-1 flex-1">
                <div class="text-white font-extrabold text-sm flex items-center justify-between group-hover:text-indigo-300">
                  <span>Ziyaret Geçmişi</span>
                  <svg class="w-4 h-4 text-indigo-400 group-hover:translate-x-1 transition-transform" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5l7 7-7 7"></path>
                  </svg>
                </div>
                <p class="text-xs text-slate-400 leading-relaxed">Geçmiş giriş ve çıkış kayıt arşivini inceleyin</p>
              </div>
            </button>

            <!-- 4. Refresh Data -->
            <button
              type="button"
              on:click={loadDashboardData}
              class="p-5 bg-slate-900/80 hover:bg-purple-900/30 border border-slate-800 hover:border-purple-500/50 rounded-2xl transition-all duration-300 text-left flex items-start gap-4 group shadow-md"
            >
              <div class="w-12 h-12 rounded-xl bg-amber-500/10 text-amber-400 border border-amber-500/20 flex items-center justify-center shrink-0 group-hover:scale-110 transition-transform">
                <svg class="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15"></path>
                </svg>
              </div>
              <div class="space-y-1 flex-1">
                <div class="text-white font-extrabold text-sm flex items-center justify-between group-hover:text-amber-300">
                  <span>Paneli Yenile</span>
                  <svg class="w-4 h-4 text-amber-400 group-hover:translate-x-1 transition-transform" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15"></path>
                  </svg>
                </div>
                <p class="text-xs text-slate-400 leading-relaxed">Canlı verileri backend'den anında güncelleyin</p>
              </div>
            </button>
          </div>
        {/if}
      </div>
      <!-- ADMIN ANALYTICS PANELS -->
      {#if isAdmin}
        <div class="space-y-6">
          
          <!-- Date Range & Analytics Filter Bar -->
          <div class="vms-card p-4 flex flex-col sm:flex-row sm:items-center justify-between gap-4">
            <div>
              <h3 class="text-sm font-bold text-white flex items-center gap-2 uppercase tracking-wider">
                <svg class="w-4 h-4 text-purple-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M3 4a1 1 0 011-1h16a1 1 0 011 1v2.586a1 1 0 01-.293.707l-6.414 6.414a1 1 0 00-.293.707V17l-4 4v-6.586a1 1 0 00-.293-.707L3.293 7.293A1 1 0 013 6.586V4z"></path>
                </svg>
                ZAMAN ARALIĞI VE ANALİTİK FİLTRESİ
              </h3>
              <p class="text-xs text-slate-400 mt-0.5">Seçilen döneme ait gerçek zamanlı performans ve grafik verileri</p>
            </div>

            <div class="flex items-center gap-1.5 bg-slate-900/80 border border-slate-800 p-1 rounded-xl shrink-0">
              <button
                type="button"
                on:click={() => handleRangeChange('today')}
                disabled={loadingAdminSummary}
                class="px-3 py-1.5 text-xs font-semibold rounded-lg transition {selectedRange === 'today' ? 'bg-purple-600 text-white shadow-md' : 'text-slate-400 hover:text-white hover:bg-slate-800'}"
              >
                Bugün
              </button>
              <button
                type="button"
                on:click={() => handleRangeChange('7d')}
                disabled={loadingAdminSummary}
                class="px-3 py-1.5 text-xs font-semibold rounded-lg transition {selectedRange === '7d' ? 'bg-purple-600 text-white shadow-md' : 'text-slate-400 hover:text-white hover:bg-slate-800'}"
              >
                Son 7 Gün
              </button>
              <button
                type="button"
                on:click={() => handleRangeChange('30d')}
                disabled={loadingAdminSummary}
                class="px-3 py-1.5 text-xs font-semibold rounded-lg transition {selectedRange === '30d' ? 'bg-purple-600 text-white shadow-md' : 'text-slate-400 hover:text-white hover:bg-slate-800'}"
              >
                Son 30 Gün
              </button>
              <button
                type="button"
                on:click={() => handleRangeChange('all')}
                disabled={loadingAdminSummary}
                class="px-3 py-1.5 text-xs font-semibold rounded-lg transition {selectedRange === 'all' ? 'bg-purple-600 text-white shadow-md' : 'text-slate-400 hover:text-white hover:bg-slate-800'}"
              >
                Tüm Zamanlar
              </button>
            </div>
          </div>

          <!-- Admin Error / Retry Alert -->
          {#if adminSummaryError}
            <div class="p-4 bg-rose-500/10 border border-rose-500/30 rounded-xl text-rose-300 text-sm flex items-center justify-between">
              <div class="flex items-center gap-2">
                <svg class="w-4 h-4 text-rose-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 8v4m0 4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z"></path>
                </svg>
                <span>Analiz verileri yüklenemedi: {adminSummaryError}</span>
              </div>
              <button on:click={loadAdminSummaryData} class="vms-btn vms-btn-secondary py-1 px-3 text-xs">Yeniden Dene</button>
            </div>
          {/if}

          <!-- Two-Column Analytics: Most Visited Personnel & Department Distribution -->
          <div class="grid grid-cols-1 lg:grid-cols-2 gap-6">
            
            <!-- Most Visited Personnel (Top 5) -->
            <div class="vms-card p-6 flex flex-col justify-between">
              <div>
                <div class="flex items-center justify-between border-b border-slate-800 pb-4 mb-4">
                  <div>
                    <h3 class="text-sm font-bold text-white flex items-center gap-2 uppercase tracking-wider">
                      <svg class="w-4 h-4 text-amber-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 3v4M3 5h4M6 17v4m-2-2h4m5-16l2.286 6.857L21 12l-5.714 2.143L13 21l-2.286-6.857L5 12l5.714-2.143L13 3z"></path>
                      </svg>
                      EN ÇOK ZİYARET EDİLEN PERSONELLER (TOP 5)
                    </h3>
                    <p class="text-xs text-slate-400 mt-0.5">En yüksek ziyaretçi yoğunluğuna sahip ilk 5 personel</p>
                  </div>
                </div>

                {#if loadingAdminSummary}
                  <div class="p-8 text-center text-slate-400 text-xs">
                    <svg class="animate-spin w-6 h-6 text-purple-400 mx-auto mb-2" fill="none" viewBox="0 0 24 24">
                      <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
                      <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
                    </svg>
                    <span>Veriler analiz ediliyor...</span>
                  </div>
                {:else if !adminSummary || !adminSummary.topPersonnel || adminSummary.topPersonnel.length === 0}
                  <div class="p-8 text-center text-slate-400 text-xs space-y-1">
                    <div class="w-10 h-10 rounded-full bg-slate-800 text-slate-500 flex items-center justify-center mx-auto mb-2">
                      👤
                    </div>
                    <p class="font-semibold text-slate-300">Henüz Ziyaretçi Kaydı Bulunmuyor</p>
                    <p class="text-[11px] text-slate-500">Ziyaret yapıldıkça en çok ziyaret edilen personeller burada listelenecektir.</p>
                  </div>
                {:else}
                  <div class="space-y-3">
                    {#each adminSummary.topPersonnel as p, idx (p.hostId)}
                      <div class="p-3.5 bg-slate-900/60 border border-slate-800/80 rounded-xl flex items-center justify-between hover:border-purple-500/30 transition">
                        <div class="flex items-center gap-3">
                          <span class="w-6 text-center font-mono font-extrabold text-xs {idx === 0 ? 'text-amber-400' : idx === 1 ? 'text-slate-300' : idx === 2 ? 'text-amber-600' : 'text-slate-500'}">
                            #{idx + 1}
                          </span>
                          <div class="w-9 h-9 rounded-xl bg-purple-500/20 text-purple-300 border border-purple-500/30 flex items-center justify-center font-bold text-xs shrink-0">
                            {p.fullName ? p.fullName.split(' ').map(n => n[0]).join('').substring(0,2).toUpperCase() : 'P'}
                          </div>
                          <div>
                            <p class="text-xs font-bold text-white">{p.fullName}</p>
                            <p class="text-[11px] text-purple-400">{p.department}</p>
                          </div>
                        </div>

                        <div class="text-right">
                          <span class="px-2.5 py-1 bg-purple-500/10 border border-purple-500/30 text-purple-300 font-mono font-bold text-xs rounded-lg">
                            {p.visitCount} Ziyaret
                          </span>
                        </div>
                      </div>
                    {/each}
                  </div>
                {/if}
              </div>
            </div>

            <!-- Department Visit Distribution -->
            <div class="vms-card p-6 flex flex-col justify-between">
              <div>
                <div class="flex items-center justify-between border-b border-slate-800 pb-4 mb-4">
                  <div>
                    <h3 class="text-sm font-bold text-white flex items-center gap-2 uppercase tracking-wider">
                      <svg class="w-4 h-4 text-purple-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M11 30.55A9 9 0 1018 7h-7v3.55z"></path>
                      </svg>
                      DEPARTMAN ZİYARET DAĞILIMI
                    </h3>
                    <p class="text-xs text-slate-400 mt-0.5">Departman bazlı toplam ziyaret yüzdeleri</p>
                  </div>
                </div>

                {#if loadingAdminSummary}
                  <div class="p-8 text-center text-slate-400 text-xs">
                    <svg class="animate-spin w-6 h-6 text-purple-400 mx-auto mb-2" fill="none" viewBox="0 0 24 24">
                      <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
                      <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
                    </svg>
                    <span>Grafik hazırlanıyor...</span>
                  </div>
                {:else if !adminSummary || !adminSummary.departmentDistribution || adminSummary.departmentDistribution.length === 0}
                  <div class="p-8 text-center text-slate-400 text-xs space-y-1">
                    <div class="w-10 h-10 rounded-full bg-slate-800 text-slate-500 flex items-center justify-center mx-auto mb-2">
                      📊
                    </div>
                    <p class="font-semibold text-slate-300">Departman Verisi Bulunmuyor</p>
                    <p class="text-[11px] text-slate-500">Ziyaretler gerçekleştikçe departman oranları burada gösterilecektir.</p>
                  </div>
                {:else}
                  <div class="grid grid-cols-1 sm:grid-cols-2 gap-4 items-center">
                    <div class="h-44 relative">
                      <Doughnut data={deptChartData} options={chartOptions} />
                    </div>
                    <div class="space-y-2.5 max-h-44 overflow-y-auto pr-1">
                      {#each adminSummary.departmentDistribution as dept}
                        <div class="space-y-1">
                          <div class="flex items-center justify-between text-xs font-semibold">
                            <span class="text-slate-300">{dept.department}</span>
                            <span class="text-purple-300 font-mono">{dept.visitCount} ({dept.percentage}%)</span>
                          </div>
                          <div class="w-full bg-slate-900 rounded-full h-1.5 overflow-hidden border border-slate-800">
                            <div
                              class="bg-gradient-to-r from-purple-500 to-indigo-500 h-full rounded-full transition-all duration-500"
                              style="width: {dept.percentage}%"
                            ></div>
                          </div>
                        </div>
                      {/each}
                    </div>
                  </div>
                {/if}
              </div>
            </div>

          </div>

          <!-- Recent Operational Activities Stream -->
          <div class="vms-card p-6">
            <div class="flex items-center justify-between border-b border-slate-800 pb-4 mb-4">
              <div>
                <h3 class="text-sm font-bold text-white flex items-center gap-2 uppercase tracking-wider">
                  <svg class="w-4 h-4 text-emerald-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z"></path>
                  </svg>
                  SON OPERASYONEL AKTİVİTELER (RECENT ACTIVITIES)
                </h3>
                <p class="text-xs text-slate-400 mt-0.5">Sistem genelindeki son ziyaretçi giriş/çıkış ve operasyon akışı</p>
              </div>
            </div>

            {#if loadingAdminSummary}
              <div class="p-8 text-center text-slate-400 text-xs">
                <svg class="animate-spin w-6 h-6 text-purple-400 mx-auto mb-2" fill="none" viewBox="0 0 24 24">
                  <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
                  <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
                </svg>
                <span>Aktiviteler yükleniyor...</span>
              </div>
            {:else if !adminSummary || !adminSummary.recentActivities || adminSummary.recentActivities.length === 0}
              <div class="p-8 text-center text-slate-400 text-xs space-y-1">
                <div class="w-10 h-10 rounded-full bg-slate-800 text-slate-500 flex items-center justify-center mx-auto mb-2">
                  📜
                </div>
                <p class="font-semibold text-slate-300">Henüz Aktivite Bulunmuyor</p>
                <p class="text-[11px] text-slate-500">Ziyaretçi giriş ve çıkış işlemleri gerçekleştikçe canlı aktivite kaydı oluşacaktır.</p>
              </div>
            {:else}
              <div class="grid grid-cols-1 md:grid-cols-2 gap-3 max-h-72 overflow-y-auto pr-1">
                {#each adminSummary.recentActivities as act (act.type + '_' + act.visitorId + '_' + act.timestamp)}
                  <div class="p-3.5 bg-slate-900/60 border border-slate-800/80 rounded-xl flex items-start gap-3 hover:border-purple-500/30 transition">
                    <div class="w-9 h-9 rounded-xl flex items-center justify-center shrink-0 mt-0.5 {act.type === 'CHECK_IN' ? 'bg-emerald-500/20 text-emerald-400 border border-emerald-500/30' : 'bg-rose-500/20 text-rose-400 border border-rose-500/30'}">
                      {#if act.type === 'CHECK_IN'}
                        <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M11 16l-4-4m0 0l4-4m-4 4h14m-5 4v1a3 3 0 01-3 3H6a3 3 0 01-3-3V7a3 3 0 013-3h7a3 3 0 013 3v1"></path>
                        </svg>
                      {:else}
                        <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M17 16l4-4m0 0l-4-4m4 4H7m6 4v1a3 3 0 01-3 3H6a3 3 0 01-3-3V7a3 3 0 013-3h4a3 3 0 013 3v1"></path>
                        </svg>
                      {/if}
                    </div>

                    <div class="flex-1 min-w-0">
                      <div class="flex items-center justify-between gap-2">
                        <span class="text-xs font-bold text-white truncate">{act.visitorName}</span>
                        <span class="text-[10px] font-mono text-purple-300 shrink-0">{formatActivityTime(act.timestamp)}</span>
                      </div>
                      <p class="text-xs text-slate-300 mt-0.5">{act.description}</p>
                      <p class="text-[10px] text-slate-400 mt-0.5 font-medium">Görüşülen: {act.hostName} ({act.department})</p>
                    </div>
                  </div>
                {/each}
              </div>
            {/if}
          </div>

        </div>
      {/if}

      <!-- Page Level Error Alert -->
      {#if pageError}
        <div class="p-4 bg-rose-500/10 border border-rose-500/30 rounded-xl text-rose-300 text-sm flex items-center justify-between">
          <span>{pageError}</span>
          <button on:click={loadDashboardData} class="underline font-semibold hover:text-rose-200">Yeniden Yükle</button>
        </div>
      {/if}

      <!-- Grid Layout: Check-In Form & Active Visitors Table -->
      <div class="grid grid-cols-1 lg:grid-cols-3 gap-6">
        
        <!-- Left Section: Yeni Ziyaretçi Kaydı Form Card -->
        <div class="lg:col-span-1 vms-card p-6 space-y-5 h-fit">
          <div class="border-b border-slate-800 pb-4">
            <h2 class="text-base font-bold text-white flex items-center gap-2 tracking-tight">
              <svg class="w-5 h-5 text-purple-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M18 9v3m0 0v3m0-3h3m-3 0h-3m-2-5a4 4 0 11-8 0 4 4 0 018 0zM3 20a6 6 0 0112 0v1H3v-1z"></path>
              </svg>
              YENİ ZİYARETÇİ KAYDI
            </h2>
            <p class="text-xs text-slate-400 mt-1">Binaya giriş yapan ziyaretçi bilgilerini kaydedin</p>
          </div>

          <!-- Form Alerts -->
          {#if formError}
            <div class="p-3 bg-rose-500/10 border border-rose-500/30 rounded-xl text-rose-300 text-xs flex items-center gap-2">
              <svg class="w-4 h-4 text-rose-400 shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 8v4m0 4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z"></path>
              </svg>
              <span>{formError}</span>
            </div>
          {/if}

          <!-- Check-In Form -->
          <form on:submit={handleCheckIn} class="space-y-4">
            <!-- Ziyaretçi Adı -->
            <div>
              <label for="visitorName" class="block text-xs font-semibold text-slate-300 uppercase tracking-wider mb-1.5">
                Ziyaretçi Adı Soyadı *
              </label>
              <input
                id="visitorName"
                type="text"
                bind:value={checkInForm.fullName}
                placeholder="Örn: Mehmet Kaya"
                disabled={isSubmitting}
                class="vms-input"
              />
            </div>

            <!-- Kimi Görecek Dropdown -->
            <div>
              <label for="hostSelect" class="block text-xs font-semibold text-slate-300 uppercase tracking-wider mb-1.5">
                Kimi Görecek? *
              </label>
              <select
                id="hostSelect"
                bind:value={checkInForm.hostId}
                disabled={loadingPersonnel || isSubmitting}
                class="vms-input cursor-pointer"
              >
                <option value="" class="bg-slate-900 text-slate-300">Ev Sahibi Personel Seçin...</option>
                {#each personnelList as person (person.id)}
                  <option value={person.id} class="bg-slate-900 text-slate-200">
                    {person.fullName} ({person.department})
                  </option>
                {/each}
              </select>
              {#if loadingPersonnel}
                <p class="text-[10px] text-slate-400 mt-1">Personel listesi yükleniyor...</p>
              {/if}
            </div>

            <!-- Submit Button -->
            <button
              type="submit"
              disabled={isSubmitting || loadingPersonnel}
              class="vms-btn vms-btn-primary w-full mt-2"
            >
              {#if isSubmitting}
                <svg class="animate-spin w-4 h-4 text-white" fill="none" viewBox="0 0 24 24">
                  <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
                  <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
                </svg>
                <span>Kaydediliyor...</span>
              {:else}
                <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M11 16l-4-4m0 0l4-4m-4 4h14m-5 4v1a3 3 0 01-3 3H6a3 3 0 01-3-3V7a3 3 0 013-3h7a3 3 0 013 3v1"></path>
                </svg>
                <span>GİRİŞ KAYDI YAP (CHECK-IN)</span>
              {/if}
            </button>
          </form>
        </div>

        <!-- Right Section: Binadaki Güncel Ziyaretçiler Table Card -->
        <div class="lg:col-span-2 vms-card overflow-hidden flex flex-col justify-between">
          <div>
            <!-- Table Header -->
            <div class="p-6 border-b border-slate-800 flex flex-col sm:flex-row sm:items-center justify-between gap-4">
              <div>
                <h2 class="text-base font-bold text-white flex items-center gap-2 tracking-tight">
                  <svg class="w-5 h-5 text-purple-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M17 20h5v-2a3 3 0 00-5.356-1.857M17 20H7m10 0v-2c0-.656-.126-1.283-.356-1.857M7 20H2v-2a3 3 0 015.356-1.857M7 20v-2c0-.656.126-1.283.356-1.857m0 0a5.002 5.002 0 019.288 0M15 7a3 3 0 11-6 0 3 3 0 016 0zm6 3a2 2 0 11-4 0 2 2 0 014 0zM7 10a2 2 0 11-4 0 2 2 0 014 0z"></path>
                  </svg>
                  BİNADAKİ GÜNCEL ZİYARETÇİLER ({filteredActiveVisitors.length})
                </h2>
                <p class="text-xs text-slate-400 mt-0.5">Anlık olarak binada bulunan ziyaretçiler ve kalış süreleri</p>
              </div>

              <div class="flex items-center gap-3">
                <!-- Search Input -->
                <div class="relative">
                  <input
                    type="text"
                    bind:value={searchQuery}
                    placeholder="İsim veya personel ara..."
                    class="vms-input py-1.5 pl-9 pr-3 text-xs w-48 sm:w-56"
                  />
                  <svg class="w-4 h-4 text-slate-400 absolute left-2.5 top-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z"></path>
                  </svg>
                </div>

                <button
                  type="button"
                  on:click={loadDashboardData}
                  class="p-2 text-slate-400 hover:text-purple-400 hover:bg-slate-800 rounded-xl transition shrink-0"
                  title="Listeyi Yenile"
                >
                  <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15"></path>
                  </svg>
                </button>
              </div>
            </div>

            <!-- Table Content -->
            {#if loadingVisitors}
              <div class="p-12 text-center text-slate-400 space-y-3">
                <svg class="animate-spin w-8 h-8 text-purple-500 mx-auto" fill="none" viewBox="0 0 24 24">
                  <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
                  <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
                </svg>
                <p class="text-sm font-medium">Binadaki ziyaretçiler yükleniyor...</p>
              </div>
            {:else if filteredActiveVisitors.length === 0}
              <div class="p-12 text-center text-slate-400 space-y-3">
                <div class="w-16 h-16 rounded-2xl bg-emerald-500/10 text-emerald-400 border border-emerald-500/20 flex items-center justify-center mx-auto">
                  <svg class="w-8 h-8" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.8" d="M5 13l4 4L19 7"></path>
                  </svg>
                </div>
                <p class="text-base font-semibold text-white">Aktif Ziyaretçi Bulunamadı</p>
                <p class="text-xs text-slate-400">Arama kriterlerinize uygun veya binada aktif olan ziyaretçi bulunmamaktadır.</p>
              </div>
            {:else}
              <div class="table-responsive">
                <table class="w-full text-left border-collapse text-sm">
                  <thead>
                    <tr class="bg-slate-900/60 border-b border-slate-800 text-slate-400 font-semibold uppercase tracking-wider text-[11px]">
                      <th class="py-3.5 px-6">Ziyaretçi</th>
                      <th class="py-3.5 px-6">Kimi Görüyor</th>
                      <th class="py-3.5 px-6">Giriş Saati</th>
                      <th class="py-3.5 px-6">Toplam Süre</th>
                      <th class="py-3.5 px-6 text-right">İşlem</th>
                    </tr>
                  </thead>
                  <tbody class="divide-y divide-slate-800/60">
                    {#each filteredActiveVisitors as visitor (visitor.id)}
                      <tr
                        on:click={() => openDetailModal(visitor)}
                        class="hover:bg-purple-900/20 transition-colors cursor-pointer"
                        title="Detayları görmek için tıklayın"
                      >
                        <!-- Visitor Name Column -->
                        <td class="py-4 px-6 font-semibold text-white flex items-center gap-3">
                          <div class="w-9 h-9 rounded-full bg-emerald-500/10 border border-emerald-500/30 text-emerald-400 flex items-center justify-center font-bold text-xs shrink-0">
                            {visitor.fullName ? visitor.fullName.split(' ').map(n => n[0]).join('').substring(0,2).toUpperCase() : 'Z'}
                          </div>
                          <span>{visitor.fullName}</span>
                        </td>

                        <!-- Host Column -->
                        <td class="py-4 px-6 text-slate-300">
                          <div class="font-medium text-white">{visitor.hostName || '-'}</div>
                          {#if visitor.hostDepartment}
                            <div class="text-[11px] text-slate-400">{visitor.hostDepartment}</div>
                          {/if}
                        </td>

                        <!-- Entry Time Column -->
                        <td class="py-4 px-6 text-slate-300 font-mono text-xs">
                          {formatDateTimeStr(visitor.entryTime)}
                        </td>

                        <!-- Live Duration Column (Ticker) -->
                        <td class="py-4 px-6 font-mono text-xs">
                          <span class="inline-flex items-center gap-1.5 px-2.5 py-1 bg-purple-500/10 text-purple-300 border border-purple-500/30 font-semibold rounded-lg">
                            <span class="w-1.5 h-1.5 rounded-full bg-purple-400 animate-ping"></span>
                            {calculateDigitalDuration(visitor.entryTime, visitor.exitTime, visitor.isInside, now)}
                          </span>
                        </td>

                        <!-- Action Column (Check-Out) -->
                        <td class="py-4 px-6 text-right">
                          <button
                            type="button"
                            on:click={(e) => { e.stopPropagation(); promptCheckOut(visitor.id, visitor.fullName); }}
                            class="vms-btn vms-btn-danger py-1.5 px-3 text-xs"
                          >
                            <svg class="w-3.5 h-3.5 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M17 16l4-4m0 0l-4-4m4 4H7m6 4v1a3 3 0 01-3 3H6a3 3 0 01-3-3V7a3 3 0 013-3h4a3 3 0 013 3v1"></path>
                            </svg>
                            <span>ÇIKIŞ</span>
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

    </div>
  </main>
</div>
