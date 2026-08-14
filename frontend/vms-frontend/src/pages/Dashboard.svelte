<script>
  import { onMount, onDestroy, createEventDispatcher } from 'svelte';
  import Sidebar from '../components/Sidebar.svelte';
  import { getAllPersonnel } from '../api/personnelApi.js';
  import { getActiveVisitors, checkInVisitor, checkOutVisitor } from '../api/visitorApi.js';

  const dispatch = createEventDispatcher();

  export var activeTab = 'dashboard';

  // Data State
  let personnelList = [];
  let activeVisitors = [];

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
  let formSuccess = '';

  // Live Timer Ticker State
  let now = new Date();
  let timerInterval;

  onMount(() => {
    // Start live clock update every 1 second for Kalış Süresi
    timerInterval = setInterval(() => {
      now = new Date();
    }, 1000);

    // Initial data fetch
    loadDashboardData();
  });

  onDestroy(() => {
    if (timerInterval) {
      clearInterval(timerInterval);
    }
  });

  async function loadDashboardData() {
    pageError = '';
    
    // Fetch Personnel for Check-In Dropdown
    loadingPersonnel = true;
    try {
      personnelList = await getAllPersonnel();
    } catch (err) {
      pageError = err.message || 'Personel listesi alınamadı.';
    } finally {
      loadingPersonnel = false;
    }

    // Fetch Active Visitors
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
  }

  // Handle Check-In Submission
  async function handleCheckIn(event) {
    event.preventDefault();
    formError = '';
    formSuccess = '';

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

      // Svelte reactive update: prepend new active visitor to list without page reload
      activeVisitors = [newVisitor, ...activeVisitors];

      formSuccess = `${newVisitor.fullName} için ziyaretçi girişi oluşturuldu.`;
      checkInForm = { fullName: '', hostId: '' };

      setTimeout(() => {
        formSuccess = '';
      }, 4000);
    } catch (err) {
      formError = err.message || 'Ziyaretçi girişi yapılırken bir hata oluştu.';
    } finally {
      isSubmitting = false;
    }
  }

  // Handle Check-Out
  async function handleCheckOut(id, name) {
    if (!confirm(`${name} isimli ziyaretçinin binadan çıkışını onaylıyor musunuz?`)) {
      return;
    }

    try {
      await checkOutVisitor(id);
      // Svelte reactive update: remove checked-out visitor from active list instantly
      activeVisitors = activeVisitors.filter(v => v.id !== id);
    } catch (err) {
      alert(err.message || 'Ziyaretçi çıkışı yapılırken bir hata oluştu.');
    }
  }

  /**
   * Calculate live duration (Kalış Süresi) HH:mm:ss
   * @param {string|Array} entryTimeVal - Entry timestamp from backend
   * @param {Date} currentNow - Current timestamp ticker
   */
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

  /**
   * Format Entry Time (Giriş Saati) HH:mm
   */
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

<div class="flex min-h-screen bg-slate-50 text-slate-800 font-sans">
  <!-- Sidebar -->
  <Sidebar {activeTab} on:changeTab={handleTabChange} />

  <!-- Main Content -->
  <main class="flex-1 p-8 overflow-y-auto">
    <div class="max-w-7xl mx-auto space-y-6">
      
      <!-- Top Welcome Header Card -->
      <div class="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4 bg-white border border-slate-200/90 p-6 rounded-2xl shadow-sm shadow-purple-900/5">
        <div>
          <h1 class="text-2xl font-bold text-slate-900 tracking-tight flex items-center gap-3">
            <span>HOŞ GELDİNİZ, RESEPSİYONİST!</span>
            <span class="inline-flex items-center gap-1.5 text-xs px-3 py-1 bg-emerald-50 border border-emerald-200 text-emerald-700 font-semibold rounded-full">
              <span class="w-2 h-2 rounded-full bg-emerald-500 animate-pulse"></span>
              Sistem Aktif
            </span>
          </h1>
          <p class="text-xs text-slate-500 mt-1">Canlı ziyaretçi takibi ve hızlı giriş kayıt paneli</p>
        </div>

        <div class="flex items-center gap-4">
          <!-- Live Date Badge -->
          <div class="hidden md:flex items-center gap-2 px-3.5 py-2 bg-slate-50 border border-slate-200 rounded-xl text-xs font-mono text-slate-600">
            <svg class="w-4 h-4 text-purple-700" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z"></path>
            </svg>
            <span>{now.toLocaleTimeString('tr-TR')}</span>
          </div>

          <!-- Visual Logout Button -->
          <button
            type="button"
            on:click={() => alert('Çıkış yap butonuna basıldı.')}
            class="inline-flex items-center gap-2 px-4 py-2.5 bg-slate-100 hover:bg-slate-200 text-slate-700 font-medium text-xs rounded-xl border border-slate-200 transition"
          >
            <svg class="w-4 h-4 text-slate-500" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M17 16l4-4m0 0l-4-4m4 4H7m6 4v1a3 3 0 01-3 3H6a3 3 0 01-3-3V7a3 3 0 013-3h4a3 3 0 013 3v1"></path>
            </svg>
            <span>Çıkış Yap</span>
          </button>
        </div>
      </div>

      <!-- Quick Summary Stat Cards -->
      <div class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-5">
        <!-- Stat 1: Aktif Ziyaretçi -->
        <div class="bg-white border border-slate-200/90 p-5 rounded-2xl shadow-sm shadow-purple-900/5 flex items-center gap-4">
          <div class="w-12 h-12 rounded-xl bg-purple-50 text-purple-700 border border-purple-200 flex items-center justify-center shrink-0 font-bold">
            <svg class="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M17 20h5v-2a3 3 0 00-5.356-1.857M17 20H7m10 0v-2c0-.656-.126-1.283-.356-1.857M7 20H2v-2a3 3 0 015.356-1.857M7 20v-2c0-.656.126-1.283.356-1.857m0 0a5.002 5.002 0 019.288 0M15 7a3 3 0 11-6 0 3 3 0 016 0zm6 3a2 2 0 11-4 0 2 2 0 014 0zM7 10a2 2 0 11-4 0 2 2 0 014 0z"></path>
            </svg>
          </div>
          <div>
            <p class="text-xs font-medium text-slate-500 uppercase tracking-wider">Binadaki Ziyaretçi</p>
            <p class="text-2xl font-bold text-slate-900 mt-0.5">{activeVisitors.length}</p>
          </div>
        </div>

        <!-- Stat 2: Toplam Personel -->
        <div class="bg-white border border-slate-200/90 p-5 rounded-2xl shadow-sm shadow-purple-900/5 flex items-center gap-4">
          <div class="w-12 h-12 rounded-xl bg-indigo-50 text-indigo-700 border border-indigo-200 flex items-center justify-center shrink-0 font-bold">
            <svg class="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 21V5a2 2 0 00-2-2H7a2 2 0 00-2 2v16m14 0h2m-2 0h-5m-9 0H3m2 0h5m0 0h4m-4 0V11m0 0l2 2m-2-2l-2 2m6-6v6m0 0l2-2m-2 2l-2-2"></path>
            </svg>
          </div>
          <div>
            <p class="text-xs font-medium text-slate-500 uppercase tracking-wider">Kayıtlı Personel</p>
            <p class="text-2xl font-bold text-slate-900 mt-0.5">{personnelList.length}</p>
          </div>
        </div>

        <!-- Stat 3: Canlı Takip Durumu -->
        <div class="bg-white border border-slate-200/90 p-5 rounded-2xl shadow-sm shadow-purple-900/5 flex items-center gap-4 sm:col-span-2 lg:col-span-1">
          <div class="w-12 h-12 rounded-xl bg-emerald-50 text-emerald-700 border border-emerald-200 flex items-center justify-center shrink-0 font-bold">
            <svg class="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z"></path>
            </svg>
          </div>
          <div>
            <p class="text-xs font-medium text-slate-500 uppercase tracking-wider">Otomatik Ticker</p>
            <p class="text-sm font-semibold text-emerald-700 mt-0.5">Canlı Süre Hesaplanıyor</p>
          </div>
        </div>
      </div>

      <!-- Page Level Error Alert -->
      {#if pageError}
        <div class="p-4 bg-rose-50 border border-rose-200 rounded-xl text-rose-700 text-sm flex items-center justify-between">
          <span>{pageError}</span>
          <button on:click={loadDashboardData} class="underline font-semibold hover:text-rose-800">Yeniden Yükle</button>
        </div>
      {/if}

      <!-- Grid Layout: Check-In Form & Active Visitors Table -->
      <div class="grid grid-cols-1 lg:grid-cols-3 gap-6">
        
        <!-- Left Section: Yeni Ziyaretçi Kaydı Form Card -->
        <div class="lg:col-span-1 bg-white border border-slate-200/90 rounded-2xl p-6 shadow-sm shadow-purple-900/5 space-y-5 h-fit">
          <div class="border-b border-slate-100 pb-4">
            <h2 class="text-lg font-bold text-slate-900 flex items-center gap-2">
              <svg class="w-5 h-5 text-purple-700" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M18 9v3m0 0v3m0-3h3m-3 0h-3m-2-5a4 4 0 11-8 0 4 4 0 018 0zM3 20a6 6 0 0112 0v1H3v-1z"></path>
              </svg>
              YENİ ZİYARETÇİ KAYDI
            </h2>
            <p class="text-xs text-slate-500 mt-1">Binaya giriş yapan ziyaretçi bilgilerini kaydedin</p>
          </div>

          <!-- Form Alerts -->
          {#if formError}
            <div class="p-3 bg-rose-50 border border-rose-200 rounded-xl text-rose-700 text-xs">
              {formError}
            </div>
          {/if}

          {#if formSuccess}
            <div class="p-3 bg-emerald-50 border border-emerald-200 rounded-xl text-emerald-700 text-xs flex items-center gap-2">
              <svg class="w-4 h-4 text-emerald-600 shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7"></path>
              </svg>
              <span>{formSuccess}</span>
            </div>
          {/if}

          <!-- Check-In Form -->
          <form on:submit={handleCheckIn} class="space-y-4">
            <!-- Ziyaretçi Adı -->
            <div>
              <label for="visitorName" class="block text-xs font-semibold text-slate-700 uppercase tracking-wider mb-1.5">
                Ziyaretçi Adı Soyadı *
              </label>
              <input
                id="visitorName"
                type="text"
                bind:value={checkInForm.fullName}
                placeholder="Örn: Mehmet Kaya"
                class="w-full px-4 py-2.5 bg-slate-50 border border-slate-200 rounded-xl text-slate-800 placeholder-slate-400 text-sm focus:outline-none focus:bg-white focus:border-purple-600 focus:ring-1 focus:ring-purple-600 transition"
              />
            </div>

            <!-- Kimi Görecek Dropdown -->
            <div>
              <label for="hostSelect" class="block text-xs font-semibold text-slate-700 uppercase tracking-wider mb-1.5">
                Kimi Görecek? *
              </label>
              <select
                id="hostSelect"
                bind:value={checkInForm.hostId}
                disabled={loadingPersonnel}
                class="w-full px-4 py-2.5 bg-slate-50 border border-slate-200 rounded-xl text-slate-800 text-sm focus:outline-none focus:bg-white focus:border-purple-600 focus:ring-1 focus:ring-purple-600 transition disabled:opacity-50"
              >
                <option value="">Ev Sahibi Personel Seçin...</option>
                {#each personnelList as person (person.id)}
                  <option value={person.id}>
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
              class="w-full py-3 px-4 bg-gradient-to-r from-purple-700 via-purple-800 to-indigo-800 hover:from-purple-800 hover:to-indigo-900 text-white font-semibold text-sm rounded-xl shadow-md shadow-purple-900/20 active:scale-[0.98] transition flex items-center justify-center gap-2 disabled:opacity-50 mt-2"
            >
              {#if isSubmitting}
                <svg class="animate-spin w-4 h-4 text-white" fill="none" viewBox="0 0 24 24">
                  <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
                  <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
                </svg>
                <span>Giriş Yapılıyor...</span>
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
        <div class="lg:col-span-2 bg-white border border-slate-200/90 rounded-2xl shadow-sm shadow-purple-900/5 overflow-hidden flex flex-col justify-between">
          <div>
            <!-- Table Header -->
            <div class="p-6 border-b border-slate-100 flex items-center justify-between">
              <div>
                <h2 class="text-lg font-bold text-slate-900 flex items-center gap-2">
                  <svg class="w-5 h-5 text-purple-700" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M17 20h5v-2a3 3 0 00-5.356-1.857M17 20H7m10 0v-2c0-.656-.126-1.283-.356-1.857M7 20H2v-2a3 3 0 015.356-1.857M7 20v-2c0-.656.126-1.283.356-1.857m0 0a5.002 5.002 0 019.288 0M15 7a3 3 0 11-6 0 3 3 0 016 0zm6 3a2 2 0 11-4 0 2 2 0 014 0zM7 10a2 2 0 11-4 0 2 2 0 014 0z"></path>
                  </svg>
                  BİNADAKİ GÜNCEL ZİYARETÇİLER
                </h2>
                <p class="text-xs text-slate-500 mt-0.5">Anlık olarak binada bulunan ziyaretçiler ve kalış süreleri</p>
              </div>

              <button
                type="button"
                on:click={loadDashboardData}
                class="p-2 text-slate-400 hover:text-purple-700 hover:bg-purple-50 rounded-xl transition"
                title="Listeyi Yenile"
              >
                <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15"></path>
                </svg>
              </button>
            </div>

            <!-- Table Content -->
            {#if loadingVisitors}
              <div class="p-12 text-center text-slate-500 space-y-3">
                <svg class="animate-spin w-8 h-8 text-purple-700 mx-auto" fill="none" viewBox="0 0 24 24">
                  <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
                  <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
                </svg>
                <p class="text-sm font-medium">Binadaki ziyaretçiler yükleniyor...</p>
              </div>
            {:else if activeVisitors.length === 0}
              <div class="p-12 text-center text-slate-500 space-y-2">
                <svg class="w-12 h-12 text-slate-300 mx-auto stroke-1" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" d="M12 4.354a4 4 0 110 5.292M15 21H3v-1a6 6 0 0112 0v1zm0 0h6v-1a6 6 0 00-9-5.197M13 7a4 4 0 11-8 0 4 4 0 018 0z"></path>
                </svg>
                <p class="text-base font-semibold text-slate-700">Şu An Binada Aktif Ziyaretçi Yok</p>
                <p class="text-xs text-slate-400">Sol taraftaki form üzerinden yeni ziyaretçi girişi oluşturabilirsiniz.</p>
              </div>
            {:else}
              <div class="overflow-x-auto">
                <table class="w-full text-left border-collapse text-sm">
                  <thead>
                    <tr class="bg-slate-50 border-b border-slate-200 text-slate-500 font-semibold uppercase tracking-wider text-xs">
                      <th class="py-4 px-6">Ziyaretçi</th>
                      <th class="py-4 px-6">Kimi Görüyor</th>
                      <th class="py-4 px-6">Giriş Saati</th>
                      <th class="py-4 px-6">Toplam Süre</th>
                      <th class="py-4 px-6 text-right">İşlem</th>
                    </tr>
                  </thead>
                  <tbody class="divide-y divide-slate-100">
                    {#each activeVisitors as visitor (visitor.id)}
                      <tr class="hover:bg-purple-50/40 transition-colors">
                        <!-- Visitor Name Column -->
                        <td class="py-4 px-6 font-semibold text-slate-900 flex items-center gap-3">
                          <div class="w-9 h-9 rounded-full bg-emerald-100 border border-emerald-200 text-emerald-700 flex items-center justify-center font-bold text-xs shrink-0">
                            {visitor.fullName.split(' ').map(n => n[0]).join('').substring(0,2).toUpperCase()}
                          </div>
                          <span>{visitor.fullName}</span>
                        </td>

                        <!-- Host Column -->
                        <td class="py-4 px-6 text-slate-700">
                          <div class="font-medium text-slate-900">{visitor.hostName || '-'}</div>
                          {#if visitor.hostDepartment}
                            <div class="text-[11px] text-slate-400">{visitor.hostDepartment}</div>
                          {/if}
                        </td>

                        <!-- Entry Time Column -->
                        <td class="py-4 px-6 text-slate-600 font-mono text-xs">
                          {formatEntryTime(visitor.entryTime)}
                        </td>

                        <!-- Live Duration Column (Ticker) -->
                        <td class="py-4 px-6 font-mono text-xs">
                          <span class="inline-flex items-center gap-1.5 px-2.5 py-1 bg-purple-50 text-purple-700 border border-purple-200 font-semibold rounded-lg">
                            <span class="w-1.5 h-1.5 rounded-full bg-purple-600 animate-ping"></span>
                            {formatDuration(visitor.entryTime, now)}
                          </span>
                        </td>

                        <!-- Action Column (Check-Out) -->
                        <td class="py-4 px-6 text-right">
                          <button
                            type="button"
                            on:click={() => handleCheckOut(visitor.id, visitor.fullName)}
                            class="inline-flex items-center gap-1.5 px-3.5 py-1.5 bg-rose-50 hover:bg-rose-100 text-rose-700 border border-rose-200/80 rounded-lg text-xs font-semibold transition shadow-2xs"
                          >
                            <svg class="w-4 h-4 text-rose-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
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
