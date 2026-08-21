<script>
  import { onMount, createEventDispatcher } from 'svelte';
  import Sidebar from '../components/Sidebar.svelte';
  import ConfirmModal from '../components/ConfirmModal.svelte';
  import { authStore } from '../stores/authStore.js';
  import { toastStore } from '../stores/toastStore.js';
  import { getAllUsers, createUser, deleteUser } from '../api/userApi.js';

  const dispatch = createEventDispatcher();
  export var activeTab = 'users';

  // Mobile sidebar state
  let isMobileOpen = false;

  // Data State
  let userList = [];
  let loading = true;
  let error = '';

  // Create Modal State
  let isModalOpen = false;
  let isSubmitting = false;
  let modalError = '';

  let formData = {
    username: '',
    password: '',
    role: 'RECEPTIONIST',
  };

  // Delete Confirm Modal State
  let isDeleteModalOpen = false;
  let deletingUserId = null;
  let deletingUsername = '';
  let isDeleting = false;

  // Check Admin Access
  $: isAdmin = !$authStore.user || $authStore.user.role === 'ADMIN';

  onMount(() => {
    if (isAdmin) {
      loadUsers();
    }
  });

  async function loadUsers() {
    loading = true;
    error = '';
    try {
      userList = await getAllUsers();
    } catch (err) {
      error = err.message || 'Kullanıcı listesi yüklenirken bir hata oluştu.';
    } finally {
      loading = false;
    }
  }

  function openModal() {
    formData = { username: '', password: '', role: 'RECEPTIONIST' };
    modalError = '';
    isModalOpen = true;
  }

  function closeModal() {
    isModalOpen = false;
  }

  async function handleCreateUser(event) {
    event.preventDefault();
    modalError = '';

    if (!formData.username.trim()) {
      modalError = 'Kullanıcı adı alanı zorunludur.';
      return;
    }
    if (formData.username.trim().length < 3) {
      modalError = 'Kullanıcı adı en az 3 karakter olmalıdır.';
      return;
    }
    if (!formData.password.trim()) {
      modalError = 'Şifre alanı zorunludur.';
      return;
    }
    if (formData.password.trim().length < 4) {
      modalError = 'Şifre en az 4 karakter olmalıdır.';
      return;
    }

    isSubmitting = true;
    try {
      const newUser = await createUser(formData);
      userList = [newUser, ...userList];
      toastStore.success('Kullanıcı başarıyla oluşturuldu.');
      closeModal();
    } catch (err) {
      modalError = err.message || 'Kullanıcı eklenirken bir hata oluştu.';
      toastStore.error(modalError);
    } finally {
      isSubmitting = false;
    }
  }

  function promptDeleteUser(id, username) {
    deletingUserId = id;
    deletingUsername = username;
    isDeleteModalOpen = true;
  }

  async function confirmDeleteUser() {
    if (!deletingUserId) return;

    isDeleting = true;
    try {
      await deleteUser(deletingUserId);
      userList = userList.filter(u => u.id !== deletingUserId);
      toastStore.success('Kullanıcı başarıyla silindi.');
      isDeleteModalOpen = false;
    } catch (err) {
      toastStore.error(err.message || 'Kullanıcı silinirken bir hata oluştu.');
    } finally {
      isDeleting = false;
      deletingUserId = null;
    }
  }

  function handleTabChange(e) {
    activeTab = e.detail;
    dispatch('changeTab', e.detail);
  }
</script>

<!-- Delete User Confirmation Modal -->
<ConfirmModal
  isOpen={isDeleteModalOpen}
  title="Kullanıcıyı Sil"
  message="{deletingUsername} kullanıcı adlı hesabı silmek istediğinizden emin misiniz? Bu işlem geri alınamaz."
  loading={isDeleting}
  on:confirm={confirmDeleteUser}
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
      
      {#if !isAdmin}
        <!-- Access Denied Banner for Non-Admin -->
        <div class="p-8 vms-card border-rose-500/30 text-center space-y-4">
          <div class="w-16 h-16 rounded-2xl bg-rose-500/10 text-rose-400 flex items-center justify-center mx-auto border border-rose-500/20">
            <svg class="w-8 h-8" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z"></path>
            </svg>
          </div>
          <h2 class="text-xl font-bold text-white">Erişim Engellendi</h2>
          <p class="text-sm text-slate-400 max-w-md mx-auto">
            Kullanıcı Yönetimi sayfasına erişim yetkiniz bulunmamaktadır. Bu alanı görüntülemek için <strong class="text-white">ADMIN</strong> yetkisine sahip olmalısınız.
          </p>
        </div>
      {:else}

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
                <span>SİSTEM KULLANICILARI</span>
                <span class="text-xs px-3 py-1 bg-purple-500/20 border border-purple-500/30 text-purple-300 font-bold rounded-full">
                  {userList.length} Kullanıcı
                </span>
              </h1>
              <p class="text-xs text-slate-400 mt-1">Sisteme erişimi olan yönetici ve resepsiyonist kullanıcıların yönetimi</p>
            </div>
          </div>

          <button
            type="button"
            on:click={openModal}
            class="vms-btn vms-btn-primary"
          >
            <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M18 9v3m0 0v3m0-3h3m-3 0h-3m-2-5a4 4 0 11-8 0 4 4 0 018 0zM3 20a6 6 0 0112 0v1H3v-1z"></path>
            </svg>
            <span>+ YENİ KULLANICI</span>
          </button>
        </div>

        <!-- Error State -->
        {#if error}
          <div class="p-4 bg-rose-500/10 border border-rose-500/30 rounded-xl text-rose-300 text-sm flex items-center justify-between">
            <span>{error}</span>
            <button on:click={loadUsers} class="underline font-semibold hover:text-rose-200">Yeniden Deneyin</button>
          </div>
        {/if}

        <!-- Users Table Card -->
        <div class="vms-card overflow-hidden">
          {#if loading}
            <div class="p-12 text-center text-slate-400 space-y-3">
              <svg class="animate-spin w-8 h-8 text-purple-500 mx-auto" fill="none" viewBox="0 0 24 24">
                <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
                <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
              </svg>
              <p class="text-sm font-medium">Kullanıcılar yükleniyor...</p>
            </div>
          {:else if userList.length === 0}
            <div class="p-12 text-center text-slate-400 space-y-3">
              <div class="w-16 h-16 rounded-2xl bg-indigo-500/10 text-indigo-400 border border-indigo-500/20 flex items-center justify-center mx-auto">
                <svg class="w-8 h-8" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.8" d="M12 15v2m-6 4h12a2 2 0 002-2v-6a2 2 0 00-2-2H6a2 2 0 00-2 2v6a2 2 0 002 2zm10-10V7a4 4 0 00-8 0v4h8z"></path>
                </svg>
              </div>
              <p class="text-base font-semibold text-white">Kayıtlı Kullanıcı Bulunamadı</p>
              <p class="text-xs text-slate-400">Yeni bir kullanıcı ekleyerek sisteme erişim tanımlayabilirsiniz.</p>
              <button
                type="button"
                on:click={openModal}
                class="vms-btn vms-btn-primary text-xs py-2 px-3"
              >
                + Yeni Kullanıcı Ekle
              </button>
            </div>
          {:else}
            <div class="table-responsive">
              <table class="w-full text-left border-collapse text-sm">
                <thead>
                  <tr class="bg-slate-900/60 border-b border-slate-800 text-slate-400 font-semibold uppercase tracking-wider text-[11px]">
                    <th class="py-3.5 px-6">Kullanıcı Adı</th>
                    <th class="py-3.5 px-6">Sistem Rolü</th>
                    <th class="py-3.5 px-6 text-right">İşlem</th>
                  </tr>
                </thead>
                <tbody class="divide-y divide-slate-800/60">
                  {#each userList as user (user.id)}
                    <tr class="hover:bg-purple-900/20 transition-colors">
                      <!-- Username Column -->
                      <td class="py-4 px-6 font-semibold text-white flex items-center gap-3">
                        <div class="w-9 h-9 rounded-full bg-indigo-500/10 border border-indigo-500/30 text-indigo-300 flex items-center justify-center font-bold text-xs shrink-0">
                          {user.username.substring(0,2).toUpperCase()}
                        </div>
                        <span>{user.username}</span>
                      </td>

                      <!-- Role Column -->
                      <td class="py-4 px-6">
                        {#if user.role === 'ADMIN'}
                          <span class="inline-flex items-center gap-1.5 px-3 py-1 rounded-lg text-xs font-semibold bg-purple-500/20 text-purple-300 border border-purple-500/30">
                            <span class="w-1.5 h-1.5 rounded-full bg-purple-400"></span>
                            ADMIN
                          </span>
                        {:else}
                          <span class="inline-flex items-center gap-1.5 px-3 py-1 rounded-lg text-xs font-semibold bg-sky-500/20 text-sky-300 border border-sky-500/30">
                            <span class="w-1.5 h-1.5 rounded-full bg-sky-400"></span>
                            RECEPTIONIST
                          </span>
                        {/if}
                      </td>

                      <!-- Action Column -->
                      <td class="py-4 px-6 text-right">
                        <button
                          type="button"
                          on:click={() => promptDeleteUser(user.id, user.username)}
                          class="vms-btn vms-btn-danger py-1.5 px-3 text-xs"
                          title="Kullanıcı Sil"
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

      {/if}

    </div>
  </main>
</div>

<!-- Modal: Yeni Kullanıcı Ekle -->
{#if isModalOpen}
  <div class="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-950/70 backdrop-blur-md">
    <div class="vms-card w-full max-w-lg p-6 space-y-6 relative animate-in fade-in zoom-in duration-200">
      
      <!-- Modal Header -->
      <div class="flex items-center justify-between border-b border-slate-800 pb-4">
        <h3 class="text-lg font-bold text-white flex items-center gap-2">
          <svg class="w-6 h-6 text-purple-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M18 9v3m0 0v3m0-3h3m-3 0h-3m-2-5a4 4 0 11-8 0 4 4 0 018 0zM3 20a6 6 0 0112 0v1H3v-1z"></path>
          </svg>
          Yeni Kullanıcı Ekle
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
      <form on:submit={handleCreateUser} class="space-y-4">
        <!-- Kullanıcı Adı -->
        <div>
          <label for="username" class="block text-xs font-semibold text-slate-300 uppercase tracking-wider mb-1.5">
            Kullanıcı Adı *
          </label>
          <input
            id="username"
            type="text"
            bind:value={formData.username}
            placeholder="Örn: resepsiyon, admin_user"
            disabled={isSubmitting}
            class="vms-input"
          />
        </div>

        <!-- Şifre -->
        <div>
          <label for="password" class="block text-xs font-semibold text-slate-300 uppercase tracking-wider mb-1.5">
            Şifre *
          </label>
          <input
            id="password"
            type="password"
            bind:value={formData.password}
            placeholder="••••••••"
            disabled={isSubmitting}
            class="vms-input"
          />
        </div>

        <!-- Rol Seçimi -->
        <div>
          <label for="role" class="block text-xs font-semibold text-slate-300 uppercase tracking-wider mb-1.5">
            Sistem Rolü *
          </label>
          <select
            id="role"
            bind:value={formData.role}
            disabled={isSubmitting}
            class="vms-input cursor-pointer"
          >
            <option value="RECEPTIONIST" class="bg-slate-900 text-slate-200">RECEPTIONIST (Resepsiyonist)</option>
            <option value="ADMIN" class="bg-slate-900 text-slate-200">ADMIN (Sistem Yöneticisi)</option>
          </select>
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
