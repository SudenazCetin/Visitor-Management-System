<script>
  import { onMount, createEventDispatcher } from 'svelte';
  import Sidebar from '../components/Sidebar.svelte';
  import { authStore } from '../stores/authStore.js';
  import { getAllUsers, createUser, deleteUser } from '../api/userApi.js';

  const dispatch = createEventDispatcher();
  export var activeTab = 'users';

  // Data State
  let userList = [];
  let loading = true;
  let error = '';

  // Modal State
  let isModalOpen = false;
  let isSubmitting = false;
  let modalError = '';

  let formData = {
    username: '',
    password: '',
    role: 'RECEPTIONIST',
  };

  // Check Admin Access (Default true for admin demo if no user set)
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
    if (!formData.password.trim()) {
      modalError = 'Şifre alanı zorunludur.';
      return;
    }

    isSubmitting = true;
    try {
      const newUser = await createUser(formData);
      // Svelte reactive update: prepend new user to array instantly without reload
      userList = [newUser, ...userList];
      closeModal();
    } catch (err) {
      modalError = err.message || 'Kullanıcı eklenirken bir hata oluştu.';
    } finally {
      isSubmitting = false;
    }
  }

  async function handleDeleteUser(id, username) {
    if (!confirm(`${username} kullanıcı adlı hesabı silmek istediğinize emin misiniz?`)) {
      return;
    }

    try {
      await deleteUser(id);
      // Svelte reactive update: remove deleted user from local list instantly
      userList = userList.filter(u => u.id !== id);
    } catch (err) {
      alert(err.message || 'Kullanıcı silinirken bir hata oluştu.');
    }
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
      
      {#if !isAdmin}
        <!-- Access Denied Banner for Non-Admin -->
        <div class="p-8 bg-white border border-rose-200 rounded-2xl shadow-sm text-center space-y-4">
          <div class="w-16 h-16 rounded-2xl bg-rose-50 text-rose-600 flex items-center justify-center mx-auto">
            <svg class="w-8 h-8" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z"></path>
            </svg>
          </div>
          <h2 class="text-xl font-bold text-slate-900">Erişim Engellendi</h2>
          <p class="text-sm text-slate-500 max-w-md mx-auto">
            Kullanıcı Yönetimi sayfasına erişim yetkiniz bulunmamaktadır. Bu alanı görüntülemek için <strong class="text-slate-700">ADMIN</strong> yetkisine sahip olmalısınız.
          </p>
        </div>
      {:else}

        <!-- Top Action Bar Header -->
        <div class="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4 bg-white border border-slate-200/90 p-6 rounded-2xl shadow-sm shadow-purple-900/5">
          <div>
            <h1 class="text-2xl font-bold text-slate-900 tracking-tight flex items-center gap-3">
              <span>SİSTEM KULLANICILARI</span>
              <span class="text-xs px-3 py-1 bg-purple-50 border border-purple-200 text-purple-700 font-bold rounded-full">
                {userList.length} Kullanıcı
              </span>
            </h1>
            <p class="text-xs text-slate-500 mt-1">Sisteme erişimi olan yönetici ve resepsiyonist kullanıcıların yönetimi</p>
          </div>

          <button
            type="button"
            on:click={openModal}
            class="inline-flex items-center justify-center gap-2 px-5 py-3 bg-gradient-to-r from-purple-700 via-purple-800 to-indigo-800 hover:from-purple-800 hover:to-indigo-900 text-white font-semibold text-sm rounded-xl shadow-md shadow-purple-900/20 active:scale-[0.98] transition-all"
          >
            <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M18 9v3m0 0v3m0-3h3m-3 0h-3m-2-5a4 4 0 11-8 0 4 4 0 018 0zM3 20a6 6 0 0112 0v1H3v-1z"></path>
            </svg>
            <span>+ YENİ KULLANICI</span>
          </button>
        </div>

        <!-- Error State -->
        {#if error}
          <div class="p-4 bg-rose-50 border border-rose-200 rounded-xl text-rose-700 text-sm flex items-center justify-between">
            <span>{error}</span>
            <button on:click={loadUsers} class="underline font-semibold hover:text-rose-800">Yeniden Deneyin</button>
          </div>
        {/if}

        <!-- Users Table Card -->
        <div class="bg-white border border-slate-200/90 rounded-2xl shadow-sm shadow-purple-900/5 overflow-hidden">
          {#if loading}
            <div class="p-12 text-center text-slate-500 space-y-3">
              <svg class="animate-spin w-8 h-8 text-purple-700 mx-auto" fill="none" viewBox="0 0 24 24">
                <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
                <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
              </svg>
              <p class="text-sm font-medium">Kullanıcılar yükleniyor...</p>
            </div>
          {:else if userList.length === 0}
            <div class="p-12 text-center text-slate-500 space-y-2">
              <svg class="w-12 h-12 text-slate-300 mx-auto stroke-1" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" d="M12 15v2m-6 4h12a2 2 0 002-2v-6a2 2 0 00-2-2H6a2 2 0 00-2 2v6a2 2 0 002 2zm10-10V7a4 4 0 00-8 0v4h8z"></path>
              </svg>
              <p class="text-base font-semibold text-slate-700">Kayıtlı Kullanıcı Bulunamadı</p>
              <p class="text-xs text-slate-400">Yeni bir kullanıcı ekleyerek sisteme erişim tanımlayabilirsiniz.</p>
            </div>
          {:else}
            <div class="overflow-x-auto">
              <table class="w-full text-left border-collapse text-sm">
                <thead>
                  <tr class="bg-slate-50 border-b border-slate-200 text-slate-500 font-semibold uppercase tracking-wider text-xs">
                    <th class="py-4 px-6">Kullanıcı Adı</th>
                    <th class="py-4 px-6">Sistem Rolü</th>
                    <th class="py-4 px-6 text-right">İşlem</th>
                  </tr>
                </thead>
                <tbody class="divide-y divide-slate-100">
                  {#each userList as user (user.id)}
                    <tr class="hover:bg-purple-50/40 transition-colors">
                      <!-- Username Column -->
                      <td class="py-4 px-6 font-semibold text-slate-900 flex items-center gap-3">
                        <div class="w-9 h-9 rounded-full bg-indigo-100 border border-indigo-200 text-indigo-700 flex items-center justify-center font-bold text-xs shrink-0">
                          {user.username.substring(0,2).toUpperCase()}
                        </div>
                        <span>{user.username}</span>
                      </td>

                      <!-- Role Column -->
                      <td class="py-4 px-6">
                        {#if user.role === 'ADMIN'}
                          <span class="inline-flex items-center gap-1.5 px-3 py-1 rounded-lg text-xs font-semibold bg-purple-50 text-purple-700 border border-purple-200">
                            <span class="w-1.5 h-1.5 rounded-full bg-purple-600"></span>
                            ADMIN
                          </span>
                        {:else}
                          <span class="inline-flex items-center gap-1.5 px-3 py-1 rounded-lg text-xs font-semibold bg-sky-50 text-sky-700 border border-sky-200">
                            <span class="w-1.5 h-1.5 rounded-full bg-sky-600"></span>
                            RECEPTIONIST
                          </span>
                        {/if}
                      </td>

                      <!-- Action Column -->
                      <td class="py-4 px-6 text-right">
                        <button
                          type="button"
                          on:click={() => handleDeleteUser(user.id, user.username)}
                          class="inline-flex items-center gap-1.5 px-3 py-1.5 bg-rose-50 hover:bg-rose-100 text-rose-700 border border-rose-200/80 rounded-lg text-xs font-medium transition"
                          title="Kullanıcı Sil"
                        >
                          <svg class="w-4 h-4 text-rose-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
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
  <div class="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-900/40 backdrop-blur-xs">
    <div class="bg-white border border-slate-200 rounded-3xl w-full max-w-lg p-6 shadow-2xl space-y-6 relative animate-in fade-in zoom-in duration-200">
      
      <!-- Modal Header -->
      <div class="flex items-center justify-between border-b border-slate-100 pb-4">
        <h3 class="text-xl font-bold text-slate-900 flex items-center gap-2">
          <svg class="w-6 h-6 text-purple-700" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M18 9v3m0 0v3m0-3h3m-3 0h-3m-2-5a4 4 0 11-8 0 4 4 0 018 0zM3 20a6 6 0 0112 0v1H3v-1z"></path>
          </svg>
          Yeni Kullanıcı Ekle
        </h3>
        <button
          type="button"
          on:click={closeModal}
          class="text-slate-400 hover:text-slate-600 p-1.5 rounded-lg hover:bg-slate-100 transition"
        >
          <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"></path>
          </svg>
        </button>
      </div>

      <!-- Modal Error Alert -->
      {#if modalError}
        <div class="p-3.5 bg-rose-50 border border-rose-200 rounded-xl text-rose-700 text-xs">
          {modalError}
        </div>
      {/if}

      <!-- Modal Form -->
      <form on:submit={handleCreateUser} class="space-y-4">
        <!-- Kullanıcı Adı -->
        <div>
          <label for="username" class="block text-xs font-semibold text-slate-700 uppercase tracking-wider mb-1.5">
            Kullanıcı Adı *
          </label>
          <input
            id="username"
            type="text"
            bind:value={formData.username}
            placeholder="Örn: resepsiyon, admin_user"
            class="w-full px-4 py-2.5 bg-slate-50 border border-slate-200 rounded-xl text-slate-800 placeholder-slate-400 text-sm focus:outline-none focus:bg-white focus:border-purple-600 focus:ring-1 focus:ring-purple-600 transition"
          />
        </div>

        <!-- Şifre -->
        <div>
          <label for="password" class="block text-xs font-semibold text-slate-700 uppercase tracking-wider mb-1.5">
            Şifre *
          </label>
          <input
            id="password"
            type="password"
            bind:value={formData.password}
            placeholder="••••••••"
            class="w-full px-4 py-2.5 bg-slate-50 border border-slate-200 rounded-xl text-slate-800 placeholder-slate-400 text-sm focus:outline-none focus:bg-white focus:border-purple-600 focus:ring-1 focus:ring-purple-600 transition"
          />
        </div>

        <!-- Rol Seçimi -->
        <div>
          <label for="role" class="block text-xs font-semibold text-slate-700 uppercase tracking-wider mb-1.5">
            Sistem Rolü *
          </label>
          <select
            id="role"
            bind:value={formData.role}
            class="w-full px-4 py-2.5 bg-slate-50 border border-slate-200 rounded-xl text-slate-800 text-sm focus:outline-none focus:bg-white focus:border-purple-600 focus:ring-1 focus:ring-purple-600 transition"
          >
            <option value="RECEPTIONIST">RECEPTIONIST (Resepsiyonist)</option>
            <option value="ADMIN">ADMIN (Sistem Yöneticisi)</option>
          </select>
        </div>

        <!-- Modal Actions -->
        <div class="flex items-center justify-end gap-3 pt-4 border-t border-slate-100">
          <button
            type="button"
            on:click={closeModal}
            class="px-4 py-2.5 bg-slate-100 hover:bg-slate-200 text-slate-700 font-medium text-sm rounded-xl transition"
          >
            İptal
          </button>

          <button
            type="submit"
            disabled={isSubmitting}
            class="px-5 py-2.5 bg-gradient-to-r from-purple-700 via-purple-800 to-indigo-800 hover:from-purple-800 hover:to-indigo-900 text-white font-semibold text-sm rounded-xl shadow-md shadow-purple-900/20 active:scale-[0.98] transition flex items-center gap-2 disabled:opacity-50"
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
