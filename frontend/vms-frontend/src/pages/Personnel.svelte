<script>
  import { onMount } from 'svelte';
  import Sidebar from '../components/Sidebar.svelte';
  import { getAllPersonnel, createPersonnel, deletePersonnel } from '../api/personnelApi.js';

  let personnelList = [];
  let loading = true;
  let error = '';
  let searchQuery = '';
  let selectedDepartment = '';

  // Modal State
  let isModalOpen = false;
  let isSubmitting = false;
  let modalError = '';

  let formData = {
    fullName: '',
    department: '',
    title: '',
    email: '',
  };

  onMount(() => {
    loadPersonnel();
  });

  async function loadPersonnel() {
    loading = true;
    error = '';
    try {
      personnelList = await getAllPersonnel();
    } catch (err) {
      error = err.message || 'Personel listesi yüklenirken bir hata oluştu.';
    } finally {
      loading = false;
    }
  }

  function openModal() {
    formData = { fullName: '', department: '', title: '', email: '' };
    modalError = '';
    isModalOpen = true;
  }

  function closeModal() {
    isModalOpen = false;
  }

  async function handleCreatePersonnel(event) {
    event.preventDefault();
    modalError = '';

    if (!formData.fullName.trim()) {
      modalError = 'Ad Soyad alanı zorunludur.';
      return;
    }
    if (!formData.department.trim()) {
      modalError = 'Departman alanı zorunludur.';
      return;
    }
    if (!formData.email.trim()) {
      modalError = 'E-posta alanı zorunludur.';
      return;
    }

    isSubmitting = true;
    try {
      const newPersonnel = await createPersonnel(formData);
      // Svelte reactive update: prepend new personnel to local list instantly
      personnelList = [newPersonnel, ...personnelList];
      closeModal();
    } catch (err) {
      modalError = err.message || 'Personel eklenirken hata oluştu.';
    } finally {
      isSubmitting = false;
    }
  }

  async function handleDeletePersonnel(id, name) {
    if (!confirm(`${name} isimli personeli silmek istediğinize emin misiniz?`)) {
      return;
    }

    try {
      await deletePersonnel(id);
      // Svelte reactive update: remove deleted personnel from array instantly without reload
      personnelList = personnelList.filter(p => p.id !== id);
    } catch (err) {
      alert(err.message || 'Personel silinirken hata oluştu.');
    }
  }

  // Reactive filtering by search query & department
  $: filteredPersonnel = personnelList.filter(p => {
    const matchesSearch = 
      p.fullName.toLowerCase().includes(searchQuery.toLowerCase()) ||
      p.department.toLowerCase().includes(searchQuery.toLowerCase()) ||
      (p.title && p.title.toLowerCase().includes(searchQuery.toLowerCase())) ||
      p.email.toLowerCase().includes(searchQuery.toLowerCase());
    
    const matchesDept = selectedDepartment === '' || p.department === selectedDepartment;

    return matchesSearch && matchesDept;
  });

  // Extract unique departments for filter dropdown
  $: departments = [...new Set(personnelList.map(p => p.department))].filter(Boolean);
</script>

<div class="flex min-h-screen bg-slate-950 text-slate-100 font-sans">
  <!-- Sidebar -->
  <Sidebar activeTab="personnel" />

  <!-- Main Content -->
  <main class="flex-1 p-8 overflow-y-auto">
    <div class="max-w-7xl mx-auto space-y-6">
      
      <!-- Top Action Bar Header -->
      <div class="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4 bg-slate-900/60 backdrop-blur-xl border border-slate-800/80 p-6 rounded-2xl shadow-xl">
        <div>
          <h1 class="text-2xl font-bold text-slate-50 tracking-tight flex items-center gap-3">
            <span>PERSONEL YÖNETİMİ</span>
            <span class="text-xs px-3 py-1 bg-sky-500/10 border border-sky-500/20 text-sky-400 font-semibold rounded-full">
              {personnelList.length} Personel
            </span>
          </h1>
          <p class="text-xs text-slate-400 mt-1">Sistemde kayıtlı şirket personelinin yönetimi ve listesi</p>
        </div>

        <button
          type="button"
          on:click={openModal}
          class="inline-flex items-center justify-center gap-2 px-5 py-3 bg-gradient-to-r from-sky-500 to-blue-600 hover:from-sky-400 hover:to-blue-500 text-white font-semibold text-sm rounded-xl shadow-lg shadow-sky-500/20 active:scale-[0.98] transition-all"
        >
          <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4v16m8-8H4"></path>
          </svg>
          <span>+ YENİ PERSONEL</span>
        </button>
      </div>

      <!-- Search & Filter Controls -->
      <div class="grid grid-cols-1 sm:grid-cols-3 gap-4">
        <!-- Search Input -->
        <div class="sm:col-span-2 relative">
          <div class="absolute inset-y-0 left-0 pl-3.5 flex items-center pointer-events-none text-slate-400">
            <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z"></path>
            </svg>
          </div>
          <input
            type="text"
            bind:value={searchQuery}
            placeholder="İsim, departman, ünvan veya e-posta ile ara..."
            class="w-full pl-11 pr-4 py-3 bg-slate-900/80 border border-slate-800 rounded-xl text-slate-100 placeholder-slate-500 text-sm focus:outline-none focus:border-sky-500 focus:ring-1 focus:ring-sky-500 transition"
          />
        </div>

        <!-- Department Filter -->
        <div>
          <select
            bind:value={selectedDepartment}
            class="w-full px-4 py-3 bg-slate-900/80 border border-slate-800 rounded-xl text-slate-200 text-sm focus:outline-none focus:border-sky-500 focus:ring-1 focus:ring-sky-500 transition"
          >
            <option value="">Tüm Departmanlar</option>
            {#each departments as dept}
              <option value={dept}>{dept}</option>
            {/each}
          </select>
        </div>
      </div>

      <!-- Error State -->
      {#if error}
        <div class="p-4 bg-rose-500/10 border border-rose-500/20 rounded-xl text-rose-400 text-sm flex items-center justify-between">
          <span>{error}</span>
          <button on:click={loadPersonnel} class="underline font-semibold hover:text-rose-300">Yeniden Deneyin</button>
        </div>
      {/if}

      <!-- Personnel Table Card -->
      <div class="bg-slate-900/80 backdrop-blur-xl border border-slate-800 rounded-2xl shadow-xl overflow-hidden">
        {#if loading}
          <div class="p-12 text-center text-slate-400 space-y-3">
            <svg class="animate-spin w-8 h-8 text-sky-500 mx-auto" fill="none" viewBox="0 0 24 24">
              <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
              <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
            </svg>
            <p class="text-sm font-medium">Personeller yükleniyor...</p>
          </div>
        {:else if filteredPersonnel.length === 0}
          <div class="p-12 text-center text-slate-400 space-y-2">
            <svg class="w-12 h-12 text-slate-600 mx-auto stroke-1" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" d="M20 13V6a2 2 0 00-2-2H6a2 2 0 00-2 2v7m16 0v5a2 2 0 01-2 2H6a2 2 0 01-2-2v-5m16 0h-2.586a1 1 0 00-.707.293l-2.414 2.414a1 1 0 01-.707.293h-3.172a1 1 0 01-.707-.293l-2.414-2.414A1 1 0 006.586 13H4"></path>
            </svg>
            <p class="text-base font-semibold text-slate-300">Kayıtlı Personel Bulunamadı</p>
            <p class="text-xs text-slate-500">Arama kriterlerinizi değiştirin veya yeni bir personel ekleyin.</p>
          </div>
        {:else}
          <div class="overflow-x-auto">
            <table class="w-full text-left border-collapse text-sm">
              <thead>
                <tr class="bg-slate-950/60 border-b border-slate-800 text-slate-400 font-semibold uppercase tracking-wider text-xs">
                  <th class="py-4 px-6">İsim Soyisim</th>
                  <th class="py-4 px-6">Departman</th>
                  <th class="py-4 px-6">Ünvan</th>
                  <th class="py-4 px-6">E-Posta</th>
                  <th class="py-4 px-6 text-right">İşlem</th>
                </tr>
              </thead>
              <tbody class="divide-y divide-slate-800/60">
                {#each filteredPersonnel as personnel (personnel.id)}
                  <tr class="hover:bg-slate-800/40 transition-colors">
                    <!-- Name Column -->
                    <td class="py-4 px-6 font-semibold text-slate-100 flex items-center gap-3">
                      <div class="w-9 h-9 rounded-full bg-gradient-to-tr from-sky-500/20 to-blue-500/20 border border-sky-500/30 text-sky-400 flex items-center justify-center font-bold text-xs shrink-0">
                        {personnel.fullName.split(' ').map(n => n[0]).join('').substring(0,2).toUpperCase()}
                      </div>
                      <span>{personnel.fullName}</span>
                    </td>

                    <!-- Department Column -->
                    <td class="py-4 px-6">
                      <span class="inline-flex items-center px-2.5 py-1 rounded-lg text-xs font-medium bg-slate-800 text-slate-300 border border-slate-700/60">
                        {personnel.department}
                      </span>
                    </td>

                    <!-- Title Column -->
                    <td class="py-4 px-6 text-slate-300">
                      {personnel.title || '-'}
                    </td>

                    <!-- Email Column -->
                    <td class="py-4 px-6 text-slate-400 font-mono text-xs">
                      {personnel.email}
                    </td>

                    <!-- Action Column -->
                    <td class="py-4 px-6 text-right">
                      <button
                        type="button"
                        on:click={() => handleDeletePersonnel(personnel.id, personnel.fullName)}
                        class="inline-flex items-center gap-1.5 px-3 py-1.5 bg-rose-500/10 hover:bg-rose-500/20 text-rose-400 hover:text-rose-300 border border-rose-500/20 rounded-lg text-xs font-medium transition"
                        title="Personel Sil"
                      >
                        <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16"></path>
                        </svg>
                        <span>Sil</span>
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
  </main>
</div>

<!-- Modal: Yeni Personel Ekle -->
{#if isModalOpen}
  <div class="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-950/80 backdrop-blur-md">
    <div class="bg-slate-900 border border-slate-800 rounded-3xl w-full max-w-lg p-6 shadow-2xl space-y-6 relative">
      
      <!-- Modal Header -->
      <div class="flex items-center justify-between border-b border-slate-800 pb-4">
        <h3 class="text-xl font-bold text-slate-100 flex items-center gap-2">
          <svg class="w-6 h-6 text-sky-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M18 9v3m0 0v3m0-3h3m-3 0h-3m-2-5a4 4 0 11-8 0 4 4 0 018 0zM3 20a6 6 0 0112 0v1H3v-1z"></path>
          </svg>
          Yeni Personel Ekle
        </h3>
        <button
          type="button"
          on:click={closeModal}
          class="text-slate-400 hover:text-slate-200 p-1.5 rounded-lg hover:bg-slate-800 transition"
        >
          <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"></path>
          </svg>
        </button>
      </div>

      <!-- Modal Error Alert -->
      {#if modalError}
        <div class="p-3.5 bg-rose-500/10 border border-rose-500/20 rounded-xl text-rose-400 text-xs">
          {modalError}
        </div>
      {/if}

      <!-- Modal Form -->
      <form on:submit={handleCreatePersonnel} class="space-y-4">
        <!-- Ad Soyad -->
        <div>
          <label for="fullName" class="block text-xs font-semibold text-slate-300 uppercase tracking-wider mb-1.5">
            Ad Soyad *
          </label>
          <input
            id="fullName"
            type="text"
            bind:value={formData.fullName}
            placeholder="Örn: Ahmet Yılmaz"
            class="w-full px-4 py-2.5 bg-slate-950/80 border border-slate-800 rounded-xl text-slate-100 placeholder-slate-500 text-sm focus:outline-none focus:border-sky-500 focus:ring-1 focus:ring-sky-500 transition"
          />
        </div>

        <!-- Departman -->
        <div>
          <label for="department" class="block text-xs font-semibold text-slate-300 uppercase tracking-wider mb-1.5">
            Departman *
          </label>
          <input
            id="department"
            type="text"
            bind:value={formData.department}
            placeholder="Örn: Yazılım, İnsan Kaynakları"
            class="w-full px-4 py-2.5 bg-slate-950/80 border border-slate-800 rounded-xl text-slate-100 placeholder-slate-500 text-sm focus:outline-none focus:border-sky-500 focus:ring-1 focus:ring-sky-500 transition"
          />
        </div>

        <!-- Ünvan -->
        <div>
          <label for="title" class="block text-xs font-semibold text-slate-300 uppercase tracking-wider mb-1.5">
            Ünvan
          </label>
          <input
            id="title"
            type="text"
            bind:value={formData.title}
            placeholder="Örn: Kıdemli Uzman"
            class="w-full px-4 py-2.5 bg-slate-950/80 border border-slate-800 rounded-xl text-slate-100 placeholder-slate-500 text-sm focus:outline-none focus:border-sky-500 focus:ring-1 focus:ring-sky-500 transition"
          />
        </div>

        <!-- E-posta -->
        <div>
          <label for="email" class="block text-xs font-semibold text-slate-300 uppercase tracking-wider mb-1.5">
            E-posta *
          </label>
          <input
            id="email"
            type="email"
            bind:value={formData.email}
            placeholder="Örn: ahmet.yilmaz@firma.com"
            class="w-full px-4 py-2.5 bg-slate-950/80 border border-slate-800 rounded-xl text-slate-100 placeholder-slate-500 text-sm focus:outline-none focus:border-sky-500 focus:ring-1 focus:ring-sky-500 transition"
          />
        </div>

        <!-- Modal Actions -->
        <div class="flex items-center justify-end gap-3 pt-4 border-t border-slate-800">
          <button
            type="button"
            on:click={closeModal}
            class="px-4 py-2.5 bg-slate-800 hover:bg-slate-700 text-slate-300 font-medium text-sm rounded-xl transition"
          >
            İptal
          </button>

          <button
            type="submit"
            disabled={isSubmitting}
            class="px-5 py-2.5 bg-gradient-to-r from-sky-500 to-blue-600 hover:from-sky-400 hover:to-blue-500 text-white font-semibold text-sm rounded-xl shadow-lg shadow-sky-500/20 active:scale-[0.98] transition flex items-center gap-2 disabled:opacity-50"
          >
            {#if isSubmitting}
              <svg class="animate-spin w-4 h-4 text-white" fill="none" viewBox="0 0 24 24">
                <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
                <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
              </svg>
              <span>Kaydediliyor...</span>
            {:else}
              <span>Kaydet</span>
            {/if}
          </button>
        </div>
      </form>

    </div>
  </div>
{/if}
