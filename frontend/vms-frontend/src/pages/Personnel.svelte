<script>
  import { onMount, createEventDispatcher } from 'svelte';
  import Sidebar from '../components/Sidebar.svelte';
  import ConfirmModal from '../components/ConfirmModal.svelte';
  import PersonnelDetailModal from '../components/PersonnelDetailModal.svelte';
  import { toastStore } from '../stores/toastStore.js';
  import { getAllPersonnel, createPersonnel, deletePersonnel } from '../api/personnelApi.js';

  const dispatch = createEventDispatcher();
  export var activeTab = 'personnel';

  // Mobile sidebar state
  let isMobileOpen = false;

  function handleTabChange(e) {
    activeTab = e.detail;
    dispatch('changeTab', e.detail);
  }

  let personnelList = [];
  let loading = true;
  let error = '';
  let searchQuery = '';
  let selectedDepartment = '';

  // Detail & Edit Modal State
  let isDetailModalOpen = false;
  let selectedDetailPersonnel = null;

  function openDetailModal(p) {
    selectedDetailPersonnel = p;
    isDetailModalOpen = true;
  }

  function handlePersonnelUpdated(e) {
    const updated = e.detail;
    personnelList = personnelList.map(p => p.id === updated.id ? updated : p);
    if (selectedDetailPersonnel && selectedDetailPersonnel.id === updated.id) {
      selectedDetailPersonnel = updated;
    }
  }

  // Create Modal State
  let isModalOpen = false;
  let isSubmitting = false;
  let modalError = '';

  let formData = {
    fullName: '',
    department: '',
    title: '',
    email: '',
    createAccount: false,
    username: '',
    password: '',
  };

  // Delete Confirm Modal State
  let isDeleteModalOpen = false;
  let deletingId = null;
  let deletingName = '';
  let isDeleting = false;

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
    formData = {
      fullName: '',
      department: '',
      title: '',
      email: '',
      createAccount: false,
      username: '',
      password: '',
    };
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

    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    if (!emailRegex.test(formData.email.trim())) {
      modalError = 'Lütfen geçerli bir e-posta adresi girin.';
      return;
    }

    if (formData.createAccount) {
      if (!formData.username.trim()) {
        modalError = 'Kullanıcı adı alanı zorunludur.';
        return;
      }
      if (!formData.password.trim()) {
        modalError = 'Geçici şifre alanı zorunludur.';
        return;
      }
    }

    isSubmitting = true;
    try {
      const payload = {
        fullName: formData.fullName.trim(),
        department: formData.department.trim(),
        title: formData.title.trim(),
        email: formData.email.trim(),
        createAccount: formData.createAccount,
        username: formData.createAccount ? formData.username.trim() : null,
        password: formData.createAccount ? formData.password.trim() : null,
      };

      const newPersonnel = await createPersonnel(payload);
      personnelList = [newPersonnel, ...personnelList];
      toastStore.success('Personel başarıyla eklendi.');
      closeModal();
    } catch (err) {
      modalError = err.message || 'Personel eklenirken bir hata oluştu.';
      toastStore.error(modalError);
    } finally {
      isSubmitting = false;
    }
  }

  function promptDeletePersonnel(id, name) {
    deletingId = id;
    deletingName = name;
    isDeleteModalOpen = true;
  }

  async function confirmDeletePersonnel() {
    if (!deletingId) return;

    isDeleting = true;
    try {
      await deletePersonnel(deletingId);
      personnelList = personnelList.filter(p => p.id !== deletingId);
      toastStore.success('Personel başarıyla silindi.');
      isDeleteModalOpen = false;
    } catch (err) {
      toastStore.error(err.message || 'Personel silinirken hata oluştu.');
    } finally {
      isDeleting = false;
      deletingId = null;
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

<!-- Personnel Detail & Edit Modal -->
<PersonnelDetailModal
  isOpen={isDetailModalOpen}
  personnel={selectedDetailPersonnel}
  on:close={() => (isDetailModalOpen = false)}
  on:updated={handlePersonnelUpdated}
/>

<!-- Delete Confirmation Modal -->
<ConfirmModal
  isOpen={isDeleteModalOpen}
  title="Personeli Sil"
  message="{deletingName} isimli personeli silmek istediğinizden emin misiniz? Bu işlem geri alınamaz."
  loading={isDeleting}
  on:confirm={confirmDeletePersonnel}
  on:cancel={() => (isDeleteModalOpen = false)}
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
  <main class="flex-1 p-4 md:p-8 overflow-y-auto z-10">
    <div class="max-w-7xl mx-auto space-y-6">
      
      <!-- Top Action Bar Header -->
      <div class="vms-card p-5 md:p-6 flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
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
          <div>
            <h1 class="text-2xl font-extrabold text-white tracking-tight flex items-center gap-3 flex-wrap">
              <span>PERSONEL YÖNETİMİ</span>
              <span class="text-xs px-3 py-1 bg-purple-500/20 border border-purple-500/30 text-purple-300 font-bold rounded-full">
                {personnelList.length} Personel
              </span>
            </h1>
            <p class="text-xs text-slate-400 mt-1">Sistemde kayıtlı şirket personelinin yönetimi ve listesi</p>
          </div>
        </div>

        <button
          type="button"
          on:click={openModal}
          class="vms-btn vms-btn-primary"
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
            class="vms-input pl-11 pr-4 py-3"
          />
        </div>

        <!-- Department Filter -->
        <div>
          <select
            bind:value={selectedDepartment}
            class="vms-input py-3 cursor-pointer"
          >
            <option value="" class="bg-slate-900 text-slate-300">Tüm Departmanlar</option>
            {#each departments as dept}
              <option value={dept} class="bg-slate-900 text-slate-200">{dept}</option>
            {/each}
          </select>
        </div>
      </div>

      <!-- Error State -->
      {#if error}
        <div class="p-4 bg-rose-500/10 border border-rose-500/30 rounded-xl text-rose-300 text-sm flex items-center justify-between">
          <span>{error}</span>
          <button on:click={loadPersonnel} class="underline font-semibold hover:text-rose-200">Yeniden Deneyin</button>
        </div>
      {/if}

      <!-- Personnel Table Card -->
      <div class="vms-card overflow-hidden">
        {#if loading}
          <div class="p-12 text-center text-slate-400 space-y-3">
            <svg class="animate-spin w-8 h-8 text-purple-500 mx-auto" fill="none" viewBox="0 0 24 24">
              <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
              <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
            </svg>
            <p class="text-sm font-medium">Personeller yükleniyor...</p>
          </div>
        {:else if filteredPersonnel.length === 0}
          <div class="p-12 text-center text-slate-400 space-y-3">
            <div class="w-16 h-16 rounded-2xl bg-purple-500/10 text-purple-400 border border-purple-500/20 flex items-center justify-center mx-auto">
              <svg class="w-8 h-8" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.8" d="M12 4.354a4 4 0 110 5.292M15 21H3v-1a6 6 0 0112 0v1zm0 0h6v-1a6 6 0 00-9-5.197M13 7a4 4 0 11-8 0 4 4 0 018 0z"></path>
              </svg>
            </div>
            <p class="text-base font-semibold text-white">Kayıtlı Personel Bulunamadı</p>
            <p class="text-xs text-slate-400">Arama kriterlerinizi değiştirin veya yeni bir personel ekleyin.</p>
            <button
              type="button"
              on:click={openModal}
              class="vms-btn vms-btn-primary text-xs py-2 px-3"
            >
              + Yeni Personel Ekle
            </button>
          </div>
        {:else}
          <div class="table-responsive">
            <table class="w-full text-left border-collapse text-sm">
              <thead>
                <tr class="bg-slate-900/60 border-b border-slate-800 text-slate-400 font-semibold uppercase tracking-wider text-[11px]">
                  <th class="py-3.5 px-6">İsim Soyisim</th>
                  <th class="py-3.5 px-6">Departman</th>
                  <th class="py-3.5 px-6">Ünvan</th>
                  <th class="py-3.5 px-6">E-Posta</th>
                  <th class="py-3.5 px-6 text-right">İşlem</th>
                </tr>
              </thead>
              <tbody class="divide-y divide-slate-800/60">
                {#each filteredPersonnel as personnel (personnel.id)}
                  <tr
                    on:click={() => openDetailModal(personnel)}
                    class="hover:bg-purple-900/20 cursor-pointer transition-colors"
                    title="Detayları görmek için tıklayın"
                  >
                    <!-- Name Column -->
                    <td class="py-4 px-6 font-semibold text-white flex items-center gap-3">
                      <div class="w-9 h-9 rounded-full bg-purple-500/10 border border-purple-500/30 text-purple-300 flex items-center justify-center font-bold text-xs shrink-0">
                        {personnel.fullName.split(' ').map(n => n[0]).join('').substring(0,2).toUpperCase()}
                      </div>
                      <span>{personnel.fullName}</span>
                    </td>

                    <!-- Department Column -->
                    <td class="py-4 px-6">
                      <span class="inline-flex items-center px-2.5 py-1 rounded-lg text-xs font-semibold bg-slate-800 text-slate-300 border border-slate-700">
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
                    <td class="py-4 px-6 text-right space-x-2" on:click|stopPropagation>
                      <button
                        type="button"
                        on:click={() => openDetailModal(personnel)}
                        class="vms-btn vms-btn-secondary py-1.5 px-3 text-xs"
                        title="Personel Detayı / Düzenle"
                      >
                        <svg class="w-3.5 h-3.5 text-purple-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z"></path>
                          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7z"></path>
                        </svg>
                        <span>Detay</span>
                      </button>

                      <button
                        type="button"
                        on:click={() => promptDeletePersonnel(personnel.id, personnel.fullName)}
                        class="vms-btn vms-btn-danger py-1.5 px-3 text-xs"
                        title="Personel Sil"
                      >
                        <svg class="w-3.5 h-3.5 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
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
  <div class="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-950/70 backdrop-blur-md">
    <div class="vms-card w-full max-w-lg p-6 space-y-6 relative animate-in fade-in zoom-in duration-200">
      
      <!-- Modal Header -->
      <div class="flex items-center justify-between border-b border-slate-800 pb-4">
        <h3 class="text-lg font-bold text-white flex items-center gap-2">
          <svg class="w-6 h-6 text-purple-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M18 9v3m0 0v3m0-3h3m-3 0h-3m-2-5a4 4 0 11-8 0 4 4 0 018 0zM3 20a6 6 0 0112 0v1H3v-1z"></path>
          </svg>
          Yeni Personel Ekle
        </h3>
        <button
          type="button"
          on:click={closeModal}
          class="text-slate-400 hover:text-white p-1.5 rounded-lg hover:bg-slate-800 transition"
        >
          <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"></path>
          </svg>
        </button>
      </div>

      <!-- Modal Error Alert -->
      {#if modalError}
        <div class="p-3.5 bg-rose-500/10 border border-rose-500/30 rounded-xl text-rose-300 text-xs flex items-center gap-2">
          <svg class="w-4 h-4 text-rose-400 shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 8v4m0 4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z"></path>
          </svg>
          <span>{modalError}</span>
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
            disabled={isSubmitting}
            class="vms-input"
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
            disabled={isSubmitting}
            class="vms-input"
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
            disabled={isSubmitting}
            class="vms-input"
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
            disabled={isSubmitting}
            class="vms-input"
          />
        </div>

        <!-- Sistem Hesabı Oluştur Seçeneği -->
        <div class="pt-2 border-t border-slate-800">
          <label class="flex items-center gap-3 cursor-pointer select-none">
            <input
              type="checkbox"
              bind:checked={formData.createAccount}
              disabled={isSubmitting}
              class="w-4 h-4 text-purple-600 rounded border-slate-700 focus:ring-purple-500 bg-slate-900 transition disabled:opacity-50"
            />
            <span class="text-xs font-bold text-slate-200 uppercase tracking-wider">Sistem Hesabı Oluştur (Personnel Role)</span>
          </label>

          {#if formData.createAccount}
            <div class="mt-3 p-3.5 bg-slate-900/60 border border-purple-500/20 rounded-xl space-y-3">
              <div>
                <label for="username" class="block text-xs font-semibold text-slate-300 uppercase tracking-wider mb-1">
                  Kullanıcı Adı *
                </label>
                <input
                  id="username"
                  type="text"
                  bind:value={formData.username}
                  placeholder="Örn: ahmet.yilmaz"
                  disabled={isSubmitting}
                  class="vms-input py-2 text-xs"
                />
              </div>

              <div>
                <label for="password" class="block text-xs font-semibold text-slate-300 uppercase tracking-wider mb-1">
                  Geçici Şifre *
                </label>
                <input
                  id="password"
                  type="password"
                  bind:value={formData.password}
                  placeholder="Geçici giriş şifresi"
                  disabled={isSubmitting}
                  class="vms-input py-2 text-xs"
                />
              </div>
            </div>
          {/if}
        </div>

        <!-- Modal Actions -->
        <div class="flex items-center justify-end gap-3 pt-4 border-t border-slate-800">
          <button
            type="button"
            disabled={isSubmitting}
            on:click={closeModal}
            class="vms-btn vms-btn-secondary"
          >
            İptal
          </button>

          <button
            type="submit"
            disabled={isSubmitting}
            class="vms-btn vms-btn-primary"
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
