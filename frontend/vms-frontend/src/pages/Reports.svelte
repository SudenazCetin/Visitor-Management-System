<script>
  import { onMount, createEventDispatcher } from 'svelte';
  import Sidebar from '../components/Sidebar.svelte';
  import { getSummary, getWeeklyReport, getTopPersonnel, getDepartmentReport } from '../api/reportsApi.js';
  import { getAllVisitors } from '../api/visitorApi.js';

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
    applyPreset('7days');
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

<div class="flex h-screen bg-slate-50 font-sans text-slate-800 antialiased overflow-hidden">
  <!-- Sidebar Component -->
  <Sidebar {activeTab} on:changeTab={(e) => dispatch('changeTab', e.detail)} />

  <!-- Main Content Area -->
  <div class="flex-1 flex flex-col overflow-y-auto">
    <!-- Top Header -->
    <header class="bg-white/80 backdrop-blur-md border-b border-slate-200/80 px-6 py-4 flex flex-col md:flex-row md:items-center justify-between gap-4 sticky top-0 z-20">
      <div class="flex items-center gap-3">
        <div class="p-2 bg-purple-100 text-purple-700 rounded-xl">
          <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 19v-6a2 2 0 00-2-2H5a2 2 0 00-2 2v6a2 2 0 002 2h2a2 2 0 002-2zm0 0V9a2 2 0 012-2h2a2 2 0 012 2v10m-6 0a2 2 0 002 2h2a2 2 0 002-2m0 0V5a2 2 0 012-2h2a2 2 0 012 2v14a2 2 0 01-2 2h-2a2 2 0 01-2-2z"></path>
          </svg>
        </div>
        <div>
          <h1 class="text-lg font-bold text-slate-900 leading-tight">Raporlar & Analizler</h1>
          <p class="text-xs text-slate-500">Ziyaretçi ve grafik istatistikleri</p>
        </div>
      </div>

      <!-- Date Filter Bar -->
      <div class="flex flex-wrap items-center gap-2 bg-slate-100/80 p-1.5 rounded-2xl border border-slate-200/80 text-xs">
        <button
          type="button"
          on:click={() => applyPreset('today')}
          class="px-3 py-1.5 rounded-xl font-medium transition {selectedPreset === 'today' ? 'bg-white text-purple-900 shadow-xs font-semibold' : 'text-slate-600 hover:text-slate-900'}"
        >
          Bugün
        </button>

        <button
          type="button"
          on:click={() => applyPreset('7days')}
          class="px-3 py-1.5 rounded-xl font-medium transition {selectedPreset === '7days' ? 'bg-white text-purple-900 shadow-xs font-semibold' : 'text-slate-600 hover:text-slate-900'}"
        >
          Son 7 Gün
        </button>

        <button
          type="button"
          on:click={() => applyPreset('30days')}
          class="px-3 py-1.5 rounded-xl font-medium transition {selectedPreset === '30days' ? 'bg-white text-purple-900 shadow-xs font-semibold' : 'text-slate-600 hover:text-slate-900'}"
        >
          Son 30 Gün
        </button>

        <button
          type="button"
          on:click={() => applyPreset('all')}
          class="px-3 py-1.5 rounded-xl font-medium transition {selectedPreset === 'all' ? 'bg-white text-purple-900 shadow-xs font-semibold' : 'text-slate-600 hover:text-slate-900'}"
        >
          Tüm Zamanlar
        </button>

        <button
          type="button"
          on:click={() => (selectedPreset = 'custom')}
          class="px-3 py-1.5 rounded-xl font-medium transition {selectedPreset === 'custom' ? 'bg-white text-purple-900 shadow-xs font-semibold' : 'text-slate-600 hover:text-slate-900'}"
        >
          Özel Tarih
        </button>

        {#if selectedPreset === 'custom'}
          <div class="flex items-center gap-1.5 ml-2 border-l border-slate-200 pl-2">
            <input
              type="date"
              bind:value={customStartDate}
              class="px-2 py-1 bg-white border border-slate-200 rounded-lg text-slate-800 focus:outline-none focus:border-purple-600 text-xs"
            />
            <span class="text-slate-400">→</span>
            <input
              type="date"
              bind:value={customEndDate}
              class="px-2 py-1 bg-white border border-slate-200 rounded-lg text-slate-800 focus:outline-none focus:border-purple-600 text-xs"
            />
            <button
              type="button"
              on:click={handleCustomSubmit}
              class="px-3 py-1 bg-purple-700 hover:bg-purple-800 text-white font-semibold rounded-lg transition"
            >
              Uygula
            </button>
          </div>
        {/if}
      </div>
    </header>

    <main class="p-6 md:p-8 space-y-6 max-w-7xl w-full mx-auto">
      <!-- Error Message -->
      {#if errorMsg}
        <div class="p-4 bg-rose-50 border border-rose-200 rounded-2xl text-rose-700 text-sm flex items-center justify-between shadow-sm">
          <div class="flex items-center gap-3">
            <svg class="w-5 h-5 text-rose-600 shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 8v4m0 4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z"></path>
            </svg>
            <span>{errorMsg}</span>
          </div>
          <button on:click={fetchReportData} class="text-xs underline font-semibold hover:text-rose-800">Tekrar Dene</button>
        </div>
      {/if}

      <!-- Loading State Indicator -->
      {#if loading}
        <div class="py-16 text-center text-slate-500 space-y-3">
          <svg class="animate-spin w-9 h-9 text-purple-700 mx-auto" fill="none" viewBox="0 0 24 24">
            <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
            <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
          </svg>
          <p class="text-sm font-medium">Rapor verileri yükleniyor...</p>
        </div>
      {:else}

        <!-- 4 Top KPI Summary Cards -->
        <div class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-5">
          <!-- Card 1: Toplam Ziyaret (Filtered by date range) -->
          <div class="bg-white border border-slate-200/90 rounded-2xl p-5 shadow-sm shadow-purple-900/5 relative overflow-hidden">
            <div class="flex items-center justify-between">
              <div>
                <p class="text-xs font-semibold text-slate-500 uppercase tracking-wider">Toplam Ziyaret</p>
                <h3 class="text-2xl font-bold text-slate-900 mt-1">{summary.totalVisitors}</h3>
              </div>
              <div class="w-12 h-12 bg-purple-50 text-purple-700 rounded-2xl flex items-center justify-center border border-purple-100">
                <svg class="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M17 20h5v-2a3 3 0 00-5.356-1.857M17 20H7m10 0v-2c0-.656-.126-1.283-.356-1.857M7 20H2v-2a3 3 0 015.356-1.857M7 20v-2c0-.656.126-1.283.356-1.857m0 0a5.002 5.002 0 019.288 0M15 7a3 3 0 11-6 0 3 3 0 016 0zm6 3a2 2 0 11-4 0 2 2 0 014 0zM7 10a2 2 0 11-4 0 2 2 0 014 0z"></path>
                </svg>
              </div>
            </div>
          </div>

          <!-- Card 2: Bugünkü Ziyaret (ALWAYS REAL-TIME TODAY) -->
          <div class="bg-white border border-slate-200/90 rounded-2xl p-5 shadow-sm shadow-purple-900/5 relative overflow-hidden">
            <div class="flex items-center justify-between">
              <div>
                <p class="text-xs font-semibold text-slate-500 uppercase tracking-wider">Bugünkü Ziyaret</p>
                <h3 class="text-2xl font-bold text-slate-900 mt-1">{summary.todayVisitors}</h3>
              </div>
              <div class="w-12 h-12 bg-indigo-50 text-indigo-700 rounded-2xl flex items-center justify-center border border-indigo-100">
                <svg class="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8 7V3m8 4V3m-9 8h10M5 21h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v12a2 2 0 002 2z"></path>
                </svg>
              </div>
            </div>
          </div>

          <!-- Card 3: Aktif Ziyaretçi (ALWAYS REAL-TIME ACTIVE IN BUILDING) -->
          <div class="bg-white border border-slate-200/90 rounded-2xl p-5 shadow-sm shadow-purple-900/5 relative overflow-hidden">
            <div class="flex items-center justify-between">
              <div>
                <p class="text-xs font-semibold text-slate-500 uppercase tracking-wider">Aktif Ziyaretçi</p>
                <h3 class="text-2xl font-bold text-emerald-600 mt-1">{summary.activeVisitors}</h3>
              </div>
              <div class="w-12 h-12 bg-emerald-50 text-emerald-600 rounded-2xl flex items-center justify-center border border-emerald-100">
                <svg class="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7"></path>
                </svg>
              </div>
            </div>
          </div>

          <!-- Card 4: Ortalama Kalış Süresi (Filtered by date range) -->
          <div class="bg-white border border-slate-200/90 rounded-2xl p-5 shadow-sm shadow-purple-900/5 relative overflow-hidden">
            <div class="flex items-center justify-between">
              <div>
                <p class="text-xs font-semibold text-slate-500 uppercase tracking-wider">Ortalama Kalış</p>
                <h3 class="text-2xl font-bold text-slate-900 mt-1">{summary.averageStayMinutes} <span class="text-sm font-semibold text-slate-500">dk</span></h3>
              </div>
              <div class="w-12 h-12 bg-amber-50 text-amber-600 rounded-2xl flex items-center justify-center border border-amber-100">
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
          <div class="lg:col-span-2 bg-white border border-slate-200/90 rounded-2xl p-6 shadow-sm shadow-purple-900/5 flex flex-col justify-between">
            <div class="flex items-center justify-between mb-4">
              <h2 class="text-sm font-bold text-slate-900 uppercase tracking-wider flex items-center gap-2">
                <span class="w-2.5 h-2.5 rounded-full bg-purple-600"></span>
                Ziyaret Trafiği
              </h2>
              <span class="text-xs text-slate-400 font-medium">Seçilen Dönem</span>
            </div>

            {#if weeklyData.length === 0 || summary.totalVisitors === 0}
              <div class="h-64 flex items-center justify-center text-center text-slate-400 text-xs">
                Seçilen tarih aralığında ziyaret verisi bulunmuyor.
              </div>
            {:else}
              <div class="h-64 relative w-full">
                <Line data={weeklyChartData} options={chartOptions} />
              </div>
            {/if}
          </div>

          <!-- Chart 3: Departman Ziyaret Dağılımı (Doughnut Chart) -->
          <div class="bg-white border border-slate-200/90 rounded-2xl p-6 shadow-sm shadow-purple-900/5 flex flex-col justify-between">
            <div class="flex items-center justify-between mb-4">
              <h2 class="text-sm font-bold text-slate-900 uppercase tracking-wider flex items-center gap-2">
                <span class="w-2.5 h-2.5 rounded-full bg-indigo-600"></span>
                Departman Dağılımı
              </h2>
            </div>

            {#if departmentData.length === 0}
              <div class="h-64 flex items-center justify-center text-center text-slate-400 text-xs">
                Seçilen tarih aralığında departman verisi bulunmuyor.
              </div>
            {:else}
              <div class="h-64 relative w-full flex items-center justify-center">
                <Doughnut data={departmentChartData} options={doughnutOptions} />
              </div>
            {/if}
          </div>

        </div>

        <!-- Chart 2: En Çok Ziyaret Edilen Personeller (Bar Chart) -->
        <div class="bg-white border border-slate-200/90 rounded-2xl p-6 shadow-sm shadow-purple-900/5">
          <div class="flex items-center justify-between mb-4">
            <h2 class="text-sm font-bold text-slate-900 uppercase tracking-wider flex items-center gap-2">
              <span class="w-2.5 h-2.5 rounded-full bg-violet-600"></span>
              En Çok Ziyaret Edilen Personeller (Top 5)
            </h2>
          </div>

          {#if topPersonnel.length === 0}
            <div class="h-56 flex items-center justify-center text-center text-slate-400 text-xs">
              Seçilen tarih aralığında personel ziyaret verisi bulunmuyor.
            </div>
          {:else}
            <div class="h-56 relative w-full">
              <Bar data={topPersonnelChartData} options={chartOptions} />
            </div>
          {/if}
        </div>

        <!-- Ziyaret Geçmişi Tablosu (Visitor History) -->
        <div class="bg-white border border-slate-200/90 rounded-2xl shadow-sm shadow-purple-900/5 overflow-hidden">
          <div class="p-6 border-b border-slate-200/80 flex items-center justify-between">
            <h2 class="text-sm font-bold text-slate-900 uppercase tracking-wider flex items-center gap-2">
              <svg class="w-4 h-4 text-purple-700" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z"></path>
              </svg>
              Ziyaret Geçmişi
            </h2>
            <span class="text-xs text-slate-500 font-medium">Toplam {visitorHistory.length} Kayıt</span>
          </div>

          {#if visitorHistory.length === 0}
            <div class="p-12 text-center text-slate-500 space-y-2">
              <p class="text-sm font-medium">Bu tarih aralığında ziyaret kaydı bulunamadı.</p>
            </div>
          {:else}
            <div class="overflow-x-auto">
              <table class="w-full text-left border-collapse font-sans text-xs">
                <thead>
                  <tr class="bg-slate-50/80 border-b border-slate-200/80 text-slate-500 uppercase tracking-wider font-semibold">
                    <th class="py-3.5 px-6">Ziyaretçi</th>
                    <th class="py-3.5 px-6">Görüşülen Personel</th>
                    <th class="py-3.5 px-6">Departman</th>
                    <th class="py-3.5 px-6">Giriş Saati</th>
                    <th class="py-3.5 px-6">Çıkış Saati</th>
                    <th class="py-3.5 px-6">Durum</th>
                    <th class="py-3.5 px-6">Kalış Süresi</th>
                  </tr>
                </thead>
                <tbody class="divide-y divide-slate-100">
                  {#each visitorHistory as visitor (visitor.id)}
                    <tr class="hover:bg-slate-50/70 transition">
                      <td class="py-3.5 px-6 font-semibold text-slate-900">{visitor.fullName}</td>
                      <td class="py-3.5 px-6 text-slate-700">{visitor.hostName || '-'}</td>
                      <td class="py-3.5 px-6 text-slate-600">
                        <span class="inline-flex items-center px-2.5 py-0.5 rounded-md text-[11px] font-medium bg-slate-100 text-slate-700">
                          {visitor.hostDepartment || '-'}
                        </span>
                      </td>
                      <td class="py-3.5 px-6 text-slate-600">{formatDateTime(visitor.entryTime)}</td>
                      <td class="py-3.5 px-6 text-slate-600">{formatDateTime(visitor.exitTime)}</td>
                      <td class="py-3.5 px-6">
                        {#if visitor.isInside}
                          <span class="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full text-[11px] font-semibold bg-emerald-50 text-emerald-700 border border-emerald-200">
                            <span class="w-1.5 h-1.5 rounded-full bg-emerald-500 animate-pulse"></span>
                            İçeride
                          </span>
                        {:else}
                          <span class="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full text-[11px] font-medium bg-slate-100 text-slate-600">
                            Tamamlandı
                          </span>
                        {/if}
                      </td>
                      <td class="py-3.5 px-6 font-medium text-slate-700">
                        {calculateDuration(visitor.entryTime, visitor.exitTime, visitor.isInside)}
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
