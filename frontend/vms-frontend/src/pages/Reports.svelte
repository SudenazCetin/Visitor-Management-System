<script>
  import { onMount, createEventDispatcher } from 'svelte';
  import Sidebar from '../components/Sidebar.svelte';
  import VisitorDetailModal from '../components/VisitorDetailModal.svelte';
  import { getSummary, getWeeklyReport, getTopPersonnel, getDepartmentReport } from '../api/reportsApi.js';
  import { getAllVisitors } from '../api/visitorApi.js';
  import { calculateDigitalDuration, calculateLiveDuration, formatDateTimeStr } from '../utils/duration.js';

  // Import ChartJS components & registrars
  import { Line, Bar, Doughnut } from 'svelte-chartjs';
  import {
    Chart as ChartJS,
    Title,
    Tooltip,
    Legend,
    LineElement,
    LinearScale,
    PointElement,
    CategoryScale,
    BarElement,
    ArcElement,
    Filler
  } from 'chart.js';

  // Register Chart.js elements
  ChartJS.register(
    Title,
    Tooltip,
    Legend,
    LineElement,
    LinearScale,
    PointElement,
    CategoryScale,
    BarElement,
    ArcElement,
    Filler
  );

  const dispatch = createEventDispatcher();
  export var activeTab = 'reports';

  // Mobile sidebar state
  let isMobileOpen = false;

  // Component Data State
  let loading = true;
  let errorMsg = '';

  let summary = {
    totalVisitors: 0,
    todayVisitors: 0,
    activeVisitors: 0,
    averageStayMinutes: 0
  };

  let weeklyData = [];
  let topPersonnel = [];
  let departmentData = [];
  let visitorHistory = [];

  // Search & Filter State for Visitor History Table
  let historySearchQuery = '';
  let historyStatusFilter = 'ALL'; // 'ALL' | 'ACTIVE' | 'EXITED'

  // Visitor Detail Modal State
  let isDetailModalOpen = false;
  let selectedDetailVisitor = null;
  let now = new Date();
  let timerInterval;

  function openDetailModal(v) {
    selectedDetailVisitor = v;
    isDetailModalOpen = true;
  }

  $: filteredVisitorHistory = visitorHistory.filter(v => {
    const q = historySearchQuery.trim().toLowerCase();
    const matchesSearch = !q || (
      (v.fullName && v.fullName.toLowerCase().includes(q)) ||
      (v.hostName && v.hostName.toLowerCase().includes(q)) ||
      (v.hostDepartment && v.hostDepartment.toLowerCase().includes(q))
    );

    const matchesStatus =
      historyStatusFilter === 'ALL' ||
      (historyStatusFilter === 'ACTIVE' && Boolean(v.isInside)) ||
      (historyStatusFilter === 'EXITED' && !v.isInside);

    return matchesSearch && matchesStatus;
  });

  // Date Range Presets & Filter State
  let selectedPreset = '7days'; // Options: 'today' | '7days' | '30days' | 'all' | 'custom'
  let customStartDate = '';
  let customEndDate = '';

  let activeStartDate = '';
  let activeEndDate = '';

  function formatDateIso(date) {
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');
    return `${year}-${month}-${day}`;
  }

  function applyPreset(preset) {
    selectedPreset = preset;
    const today = new Date();

    if (preset === 'today') {
      const dateStr = formatDateIso(today);
      activeStartDate = dateStr;
      activeEndDate = dateStr;
    } else if (preset === '7days') {
      const past = new Date();
      past.setDate(today.getDate() - 6);
      activeStartDate = formatDateIso(past);
      activeEndDate = formatDateIso(today);
    } else if (preset === '30days') {
      const past = new Date();
      past.setDate(today.getDate() - 29);
      activeStartDate = formatDateIso(past);
      activeEndDate = formatDateIso(today);
    } else if (preset === 'all') {
      activeStartDate = '';
      activeEndDate = '';
    } else if (preset === 'custom') {
      return;
    }

    fetchReportData();
  }

  function handleCustomSubmit() {
    if (selectedPreset !== 'custom') return;
    if (customStartDate && customEndDate && customStartDate > customEndDate) {
      errorMsg = 'Başlangıç tarihi bitiş tarihinden sonra olamaz.';
      return;
    }
    activeStartDate = customStartDate;
    activeEndDate = customEndDate;
    fetchReportData();
  }

  // Chart Data Configurations
  $: weeklyChartData = {
    labels: weeklyData.map(w => `${w.dayName} (${w.date.substring(8, 10)})`),
    datasets: [
      {
        label: 'Ziyaret Sayısı',
        data: weeklyData.map(w => w.count),
        borderColor: 'rgb(126, 34, 206)',
        backgroundColor: 'rgba(168, 85, 247, 0.15)',
        fill: true,
        tension: 0.35,
        pointBackgroundColor: 'rgb(126, 34, 206)',
        pointBorderColor: '#ffffff',
        pointRadius: 4,
        pointHoverRadius: 6
      }
    ]
  };

  $: topPersonnelChartData = {
    labels: topPersonnel.map(p => p.fullName),
    datasets: [
      {
        label: 'Ziyaret Sayısı',
        data: topPersonnel.map(p => p.visitCount),
        backgroundColor: [
          'rgba(126, 34, 206, 0.85)',
          'rgba(99, 102, 241, 0.85)',
          'rgba(168, 85, 247, 0.85)',
          'rgba(59, 130, 246, 0.85)',
          'rgba(20, 184, 166, 0.85)'
        ],
        borderRadius: 8
      }
    ]
  };

  $: departmentChartData = {
    labels: departmentData.map(d => d.department),
    datasets: [
      {
        data: departmentData.map(d => d.visitCount),
        backgroundColor: [
          '#7e22ce',
          '#6366f1',
          '#a855f7',
          '#3b82f6',
          '#14b8a6',
          '#ec4899',
          '#f59e0b'
        ],
        borderWidth: 2,
        borderColor: '#ffffff'
      }
    ]
  };

  const chartOptions = {
    responsive: true,
    maintainAspectRatio: false,
    plugins: {
      legend: {
        display: false
      }
    },
    scales: {
      y: {
        beginAtZero: true,
        ticks: { precision: 0 }
      }
    }
  };

  const doughnutOptions = {
    responsive: true,
    maintainAspectRatio: false,
    plugins: {
      legend: {
        position: 'right',
        labels: {
          boxWidth: 12,
          font: { size: 11 }
        }
      }
    }
  };

  onMount(() => {
    timerInterval = setInterval(() => {
      now = new Date();
    }, 1000);
    applyPreset('7days');
  });

  import { onDestroy } from 'svelte';
  onDestroy(() => {
    if (timerInterval) clearInterval(timerInterval);
  });

  async function fetchReportData() {
    loading = true;
    errorMsg = '';
    try {
      const [sumRes, weekRes, topRes, deptRes, historyRes] = await Promise.all([
        getSummary(activeStartDate, activeEndDate),
        getWeeklyReport(activeStartDate, activeEndDate),
        getTopPersonnel(activeStartDate, activeEndDate),
        getDepartmentReport(activeStartDate, activeEndDate),
        getAllVisitors(activeStartDate, activeEndDate)
      ]);

      summary = sumRes || summary;
      weeklyData = weekRes || [];
      topPersonnel = topRes || [];
      departmentData = deptRes || [];
      visitorHistory = historyRes || [];
    } catch (err) {
      errorMsg = err.message || 'Rapor verileri yüklenemedi. Lütfen tekrar deneyin.';
    } finally {
      loading = false;
    }
  }

  function formatDateTime(isoString) {
    if (!isoString) return '-';
    const date = new Date(isoString);
    return date.toLocaleTimeString('tr-TR', { hour: '2-digit', minute: '2-digit' }) + ' (' + date.toLocaleDateString('tr-TR') + ')';
  }

  function calculateDuration(entryStr, exitStr, isInside) {
    if (!entryStr) return '-';
    const entry = new Date(entryStr);
    const end = exitStr ? new Date(exitStr) : (isInside ? new Date() : null);

    if (!end) return '-';

    const diffMinutes = Math.floor((end - entry) / 60000);
    if (diffMinutes < 1) return '1 dk altında';
    
    const hours = Math.floor(diffMinutes / 60);
    const mins = diffMinutes % 60;
    
    if (hours > 0) {
      return `${hours} sa ${mins} dk`;
    }
    return `${mins} dk`;
  }
</script>

<div class="vms-app-layout flex text-slate-100 font-sans antialiased overflow-hidden">
  <!-- Fixed Background Image & Overlay -->
  <div class="vms-bg-fixed">
    <div class="vms-bg-image"></div>
    <div class="vms-bg-overlay"></div>
  </div>

  <!-- Sidebar Component -->
  <Sidebar {activeTab} {isMobileOpen} on:changeTab={(e) => { dispatch('changeTab', e.detail); }} on:closeMobile={() => (isMobileOpen = false)} />

  <!-- Main Content Area -->
  <div class="flex-1 flex flex-col overflow-y-auto z-10">
    <!-- Top Header -->
    <header class="vms-card rounded-none border-x-0 border-t-0 border-b border-slate-800 px-4 md:px-6 py-4 flex flex-col md:flex-row md:items-center justify-between gap-4 sticky top-0 z-20">
      <div class="flex items-center gap-3">
        <!-- Mobile Hamburger Button -->
        <button
          type="button"
          on:click={() => (isMobileOpen = true)}
          class="md:hidden p-2 text-slate-400 hover:text-white hover:bg-slate-800 rounded-xl transition"
          aria-label="Menüyü aç"
        >
          <svg class="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 6h16M4 12h16M4 18h16"></path>
          </svg>
        </button>
        <div class="p-2 bg-purple-500/20 text-purple-400 border border-purple-500/30 rounded-xl">
          <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 19v-6a2 2 0 00-2-2H5a2 2 0 00-2 2v6a2 2 0 002 2h2a2 2 0 00-2-2zm0 0V9a2 2 0 012-2h2a2 2 0 012 2v10m-6 0a2 2 0 002 2h2a2 2 0 002-2m0 0V5a2 2 0 012-2h2a2 2 0 012 2v14a2 2 0 01-2 2h-2a2 2 0 01-2-2z"></path>
          </svg>
        </div>
        <div>
          <h1 class="text-lg font-extrabold text-white leading-tight">Raporlar & Analizler</h1>
          <p class="text-xs text-slate-400">Ziyaretçi ve grafik istatistikleri</p>
        </div>
      </div>

      <!-- Date Filter Bar -->
      <div class="flex flex-wrap items-center gap-2 bg-slate-900/80 p-1.5 rounded-2xl border border-slate-800 text-xs">
        <button
          type="button"
          on:click={() => applyPreset('today')}
          class="px-3 py-1.5 rounded-xl font-medium transition {selectedPreset === 'today' ? 'bg-gradient-to-r from-purple-600 to-indigo-600 text-white shadow-md font-semibold' : 'text-slate-400 hover:text-white'}"
        >
          Bugün
        </button>

        <button
          type="button"
          on:click={() => applyPreset('7days')}
          class="px-3 py-1.5 rounded-xl font-medium transition {selectedPreset === '7days' ? 'bg-gradient-to-r from-purple-600 to-indigo-600 text-white shadow-md font-semibold' : 'text-slate-400 hover:text-white'}"
        >
          Son 7 Gün
        </button>

        <button
          type="button"
          on:click={() => applyPreset('30days')}
          class="px-3 py-1.5 rounded-xl font-medium transition {selectedPreset === '30days' ? 'bg-gradient-to-r from-purple-600 to-indigo-600 text-white shadow-md font-semibold' : 'text-slate-400 hover:text-white'}"
        >
          Son 30 Gün
        </button>

        <button
          type="button"
          on:click={() => applyPreset('all')}
          class="px-3 py-1.5 rounded-xl font-medium transition {selectedPreset === 'all' ? 'bg-gradient-to-r from-purple-600 to-indigo-600 text-white shadow-md font-semibold' : 'text-slate-400 hover:text-white'}"
        >
          Tüm Zamanlar
        </button>

        <button
          type="button"
          on:click={() => (selectedPreset = 'custom')}
          class="px-3 py-1.5 rounded-xl font-medium transition {selectedPreset === 'custom' ? 'bg-gradient-to-r from-purple-600 to-indigo-600 text-white shadow-md font-semibold' : 'text-slate-400 hover:text-white'}"
        >
          Özel Tarih
        </button>

        {#if selectedPreset === 'custom'}
          <div class="flex items-center gap-1.5 ml-2 border-l border-slate-800 pl-2">
            <input
              type="date"
              bind:value={customStartDate}
              class="vms-input py-1 px-2 text-xs"
            />
            <span class="text-slate-400">→</span>
            <input
              type="date"
              bind:value={customEndDate}
              class="vms-input py-1 px-2 text-xs"
            />
            <button
              type="button"
              on:click={handleCustomSubmit}
              class="vms-btn vms-btn-primary py-1 px-2.5 text-xs"
            >
              Uygula
            </button>
          </div>
        {/if}
      </div>
    </header>

    <main class="p-4 md:p-6 lg:p-8 space-y-6 max-w-7xl w-full mx-auto">
      <!-- Error Message -->
      {#if errorMsg}
        <div class="p-4 bg-rose-500/10 border border-rose-500/30 rounded-2xl text-rose-300 text-sm flex items-center justify-between">
          <div class="flex items-center gap-3">
            <svg class="w-5 h-5 text-rose-400 shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 8v4m0 4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z"></path>
            </svg>
            <span>{errorMsg}</span>
          </div>
          <button on:click={fetchReportData} class="text-xs underline font-semibold hover:text-rose-200">Tekrar Dene</button>
        </div>
      {/if}

      <!-- Loading State Indicator -->
      {#if loading}
        <div class="py-16 text-center text-slate-400 space-y-3">
          <svg class="animate-spin w-9 h-9 text-purple-500 mx-auto" fill="none" viewBox="0 0 24 24">
            <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
            <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
          </svg>
          <p class="text-sm font-medium">Rapor verileri yükleniyor...</p>
        </div>
      {:else}

        <!-- 4 Top KPI Summary Cards -->
        <div class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-5">
          <!-- Card 1: Toplam Ziyaret (Filtered by date range) -->
          <div class="vms-card vms-card-interactive p-5">
            <div class="flex items-center justify-between">
              <div>
                <p class="text-xs font-semibold text-slate-400 uppercase tracking-wider">Toplam Ziyaret</p>
                <h3 class="text-2xl font-black text-white mt-1">{summary.totalVisitors}</h3>
              </div>
              <div class="w-12 h-12 bg-purple-500/10 text-purple-400 rounded-2xl flex items-center justify-center border border-purple-500/20">
                <svg class="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M17 20h5v-2a3 3 0 00-5.356-1.857M17 20H7m10 0v-2c0-.656-.126-1.283-.356-1.857M7 20H2v-2a3 3 0 015.356-1.857M7 20v-2c0-.656.126-1.283.356-1.857m0 0a5.002 5.002 0 019.288 0M15 7a3 3 0 11-6 0 3 3 0 016 0zm6 3a2 2 0 11-4 0 2 2 0 014 0zM7 10a2 2 0 11-4 0 2 2 0 014 0z"></path>
                </svg>
              </div>
            </div>
          </div>

          <!-- Card 2: Bugünkü Ziyaret (ALWAYS REAL-TIME TODAY) -->
          <div class="vms-card vms-card-interactive p-5">
            <div class="flex items-center justify-between">
              <div>
                <p class="text-xs font-semibold text-slate-400 uppercase tracking-wider">Bugünkü Ziyaret</p>
                <h3 class="text-2xl font-black text-white mt-1">{summary.todayVisitors}</h3>
              </div>
              <div class="w-12 h-12 bg-indigo-500/10 text-indigo-400 rounded-2xl flex items-center justify-center border border-indigo-500/20">
                <svg class="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8 7V3m8 4V3m-9 8h10M5 21h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v12a2 2 0 002 2z"></path>
                </svg>
              </div>
            </div>
          </div>

          <!-- Card 3: Aktif Ziyaretçi (ALWAYS REAL-TIME ACTIVE IN BUILDING) -->
          <div class="vms-card vms-card-interactive p-5">
            <div class="flex items-center justify-between">
              <div>
                <p class="text-xs font-semibold text-slate-400 uppercase tracking-wider">Aktif Ziyaretçi</p>
                <h3 class="text-2xl font-black text-emerald-400 mt-1">{summary.activeVisitors}</h3>
              </div>
              <div class="w-12 h-12 bg-emerald-500/10 text-emerald-400 rounded-2xl flex items-center justify-center border border-emerald-500/20">
                <svg class="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7"></path>
                </svg>
              </div>
            </div>
          </div>

          <!-- Card 4: Ortalama Kalış Süresi (Filtered by date range) -->
          <div class="vms-card vms-card-interactive p-5">
            <div class="flex items-center justify-between">
              <div>
                <p class="text-xs font-semibold text-slate-400 uppercase tracking-wider">Ortalama Kalış</p>
                <h3 class="text-2xl font-black text-white mt-1">{summary.averageStayMinutes} <span class="text-sm font-semibold text-slate-400">dk</span></h3>
              </div>
              <div class="w-12 h-12 bg-amber-500/10 text-amber-400 rounded-2xl flex items-center justify-center border border-amber-500/20">
                <svg class="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z"></path>
                </svg>
              </div>
            </div>
          </div>
        </div>

        <!-- Charts Grid Layout -->
        <div class="grid grid-cols-1 lg:grid-cols-3 gap-6">
          
          <!-- Chart 1: Haftalık/Tarih Ziyaret Trafiği (Line Chart) -->
          <div class="lg:col-span-2 vms-card p-6 flex flex-col justify-between">
            <div class="flex items-center justify-between mb-4">
              <h2 class="text-xs font-bold text-white uppercase tracking-wider flex items-center gap-2">
                <span class="w-2.5 h-2.5 rounded-full bg-purple-400"></span>
                Ziyaret Trafiği
              </h2>
              <span class="text-xs text-slate-400 font-medium">Seçilen Dönem</span>
            </div>

            {#if weeklyData.length === 0 || summary.totalVisitors === 0}
              <div class="h-64 flex flex-col items-center justify-center text-center text-slate-400 space-y-2">
                <div class="w-12 h-12 rounded-2xl bg-slate-900 text-slate-400 border border-slate-800 flex items-center justify-center">
                  <svg class="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.8" d="M7 12l3-3 3 3 4-4M8 21l4-4 4 4M3 4h18M4 4h16v12a1 1 0 01-1 1H5a1 1 0 01-1-1V4z"></path>
                  </svg>
                </div>
                <p class="text-xs font-medium">Bu tarih aralığında yeterli veri bulunmuyor.</p>
              </div>
            {:else}
              <div class="h-64 relative w-full">
                <Line data={weeklyChartData} options={chartOptions} />
              </div>
            {/if}
          </div>

          <!-- Chart 3: Departman Ziyaret Dağılımı (Doughnut Chart) -->
          <div class="vms-card p-6 flex flex-col justify-between">
            <div class="flex items-center justify-between mb-4">
              <h2 class="text-xs font-bold text-white uppercase tracking-wider flex items-center gap-2">
                <span class="w-2.5 h-2.5 rounded-full bg-indigo-400"></span>
                Departman Dağılımı
              </h2>
            </div>

            {#if departmentData.length === 0}
              <div class="h-64 flex flex-col items-center justify-center text-center text-slate-400 space-y-2">
                <div class="w-12 h-12 rounded-2xl bg-slate-900 text-slate-400 border border-slate-800 flex items-center justify-center">
                  <svg class="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.8" d="M11 3.055A9.001 9.001 0 1020.945 13H11V3.055z"></path>
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.8" d="M20.488 9H15V3.512A9.025 9.025 0 0120.488 9z"></path>
                  </svg>
                </div>
                <p class="text-xs font-medium">Bu tarih aralığında departman verisi bulunmuyor.</p>
              </div>
            {:else}
              <div class="h-64 relative w-full flex items-center justify-center">
                <Doughnut data={departmentChartData} options={doughnutOptions} />
              </div>
            {/if}
          </div>

        </div>

        <!-- Chart 2: En Çok Ziyaret Edilen Personeller (Bar Chart) -->
        <div class="vms-card p-6">
          <div class="flex items-center justify-between mb-4">
            <h2 class="text-xs font-bold text-white uppercase tracking-wider flex items-center gap-2">
              <span class="w-2.5 h-2.5 rounded-full bg-violet-400"></span>
              En Çok Ziyaret Edilen Personeller (Top 5)
            </h2>
          </div>

          {#if topPersonnel.length === 0}
            <div class="h-56 flex flex-col items-center justify-center text-center text-slate-400 space-y-2">
              <div class="w-12 h-12 rounded-2xl bg-slate-900 text-slate-400 border border-slate-800 flex items-center justify-center">
                <svg class="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.8" d="M16 8v8m-4-5v5m-4-2v2m-2 4h12a2 2 0 002-2V6a2 2 0 00-2-2H6a2 2 0 00-2 2v12a2 2 0 002 2z"></path>
                </svg>
              </div>
              <p class="text-xs font-medium">Bu tarih aralığında personel ziyaret verisi bulunmuyor.</p>
            </div>
          {:else}
            <div class="h-56 relative w-full">
              <Bar data={topPersonnelChartData} options={chartOptions} />
            </div>
          {/if}
        </div>

        <!-- Visitor Detail Modal -->
        <VisitorDetailModal
          isOpen={isDetailModalOpen}
          visitor={selectedDetailVisitor}
          {now}
          on:close={() => (isDetailModalOpen = false)}
        />

        <!-- Ziyaret Geçmişi Tablosu (Visitor History) -->
        <div class="vms-card overflow-hidden">
          <div class="p-6 border-b border-slate-800 flex flex-col md:flex-row md:items-center justify-between gap-4">
            <div>
              <h2 class="text-xs font-bold text-white uppercase tracking-wider flex items-center gap-2">
                <svg class="w-4 h-4 text-purple-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z"></path>
                </svg>
                Ziyaret Geçmişi ({filteredVisitorHistory.length})
              </h2>
              <p class="text-xs text-slate-400 mt-0.5">Filtrelenmiş tarih aralığındaki tüm ziyaret kayıtları</p>
            </div>

            <div class="flex flex-wrap items-center gap-3">
              <!-- Search Input -->
              <div class="relative">
                <input
                  type="text"
                  bind:value={historySearchQuery}
                  placeholder="Ziyaretçi veya personel ara..."
                  class="vms-input py-1.5 pl-9 pr-3 text-xs w-48 sm:w-56"
                />
                <svg class="w-4 h-4 text-slate-400 absolute left-2.5 top-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z"></path>
                </svg>
              </div>

              <!-- Status Filter Tabs -->
              <div class="flex items-center bg-slate-900/80 p-1 rounded-xl border border-slate-800 text-xs font-medium">
                <button
                  type="button"
                  on:click={() => (historyStatusFilter = 'ALL')}
                  class="px-2.5 py-1 rounded-lg transition {historyStatusFilter === 'ALL' ? 'bg-gradient-to-r from-purple-600 to-indigo-600 text-white shadow-md font-semibold' : 'text-slate-400 hover:text-white'}"
                >
                  Tümü ({visitorHistory.length})
                </button>
                <button
                  type="button"
                  on:click={() => (historyStatusFilter = 'ACTIVE')}
                  class="px-2.5 py-1 rounded-lg transition {historyStatusFilter === 'ACTIVE' ? 'bg-gradient-to-r from-purple-600 to-indigo-600 text-white shadow-md font-semibold' : 'text-slate-400 hover:text-white'}"
                >
                  İçeride ({visitorHistory.filter(v => v.isInside).length})
                </button>
                <button
                  type="button"
                  on:click={() => (historyStatusFilter = 'EXITED')}
                  class="px-2.5 py-1 rounded-lg transition {historyStatusFilter === 'EXITED' ? 'bg-gradient-to-r from-purple-600 to-indigo-600 text-white shadow-md font-semibold' : 'text-slate-400 hover:text-white'}"
                >
                  Tamamlanan ({visitorHistory.filter(v => !v.isInside).length})
                </button>
              </div>
            </div>
          </div>

          {#if filteredVisitorHistory.length === 0}
            <div class="p-12 text-center text-slate-400 space-y-3">
              <div class="w-14 h-14 rounded-2xl bg-slate-900 text-slate-400 border border-slate-800 flex items-center justify-center mx-auto">
                <svg class="w-7 h-7" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.8" d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z"></path>
                </svg>
              </div>
              <p class="text-sm font-semibold text-white">Ziyaret Kaydı Bulunamadı</p>
              <p class="text-xs text-slate-400">Arama kriterlerinize uygun ziyaret kaydı bulunamadı.</p>
            </div>
          {:else}
            <div class="table-responsive">
              <table class="w-full text-left border-collapse font-sans text-xs">
                <thead>
                  <tr class="bg-slate-900/60 border-b border-slate-800 text-slate-400 uppercase tracking-wider font-semibold">
                    <th class="py-3.5 px-6">Ziyaretçi</th>
                    <th class="py-3.5 px-6">Görüşülen Personel</th>
                    <th class="py-3.5 px-6">Departman</th>
                    <th class="py-3.5 px-6">Giriş Saati</th>
                    <th class="py-3.5 px-6">Çıkış Saati</th>
                    <th class="py-3.5 px-6">Durum</th>
                    <th class="py-3.5 px-6">Kalış Süresi</th>
                  </tr>
                </thead>
                <tbody class="divide-y divide-slate-800/60">
                  {#each filteredVisitorHistory as visitor (visitor.id)}
                    <tr
                      on:click={() => openDetailModal(visitor)}
                      class="hover:bg-purple-900/20 cursor-pointer transition"
                      title="Detayları görmek için tıklayın"
                    >
                      <td class="py-3.5 px-6 font-semibold text-white">{visitor.fullName}</td>
                      <td class="py-3.5 px-6 text-slate-300">{visitor.hostName || '-'}</td>
                      <td class="py-3.5 px-6 text-slate-400">
                        <span class="inline-flex items-center px-2.5 py-0.5 rounded-md text-[11px] font-medium bg-slate-800 text-slate-300 border border-slate-700">
                          {visitor.hostDepartment || '-'}
                        </span>
                      </td>
                      <td class="py-3.5 px-6 text-slate-300 font-mono text-xs">{formatDateTimeStr(visitor.entryTime, true)}</td>
                      <td class="py-3.5 px-6 text-slate-300 font-mono text-xs">{formatDateTimeStr(visitor.exitTime, true)}</td>
                      <td class="py-3.5 px-6">
                        {#if visitor.isInside}
                          <span class="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full text-[11px] font-semibold bg-emerald-500/20 text-emerald-400 border border-emerald-500/30">
                            <span class="w-1.5 h-1.5 rounded-full bg-emerald-400 animate-pulse"></span>
                            İçeride (Aktif)
                          </span>
                        {:else}
                          <span class="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full text-[11px] font-medium bg-slate-800 text-slate-400 border border-slate-700">
                            Çıkış Yaptı
                          </span>
                        {/if}
                      </td>
                      <td class="py-3.5 px-6 font-mono font-bold text-purple-300">
                        {calculateDigitalDuration(visitor.entryTime, visitor.exitTime, visitor.isInside, now)}
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
</div>
