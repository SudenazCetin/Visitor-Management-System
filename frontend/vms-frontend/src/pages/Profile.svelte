<script>
  import { onMount, createEventDispatcher } from 'svelte';
  import Sidebar from '../components/Sidebar.svelte';
  import { toastStore } from '../stores/toastStore.js';
  import { getMyProfile, changeMyPassword } from '../api/meApi.js';

  const dispatch = createEventDispatcher();
  export var activeTab = 'profile';

  let isMobileOpen = false;

  let profile = {
    fullName: '',
    department: '',
    title: '',
    email: '',
    username: '',
    role: ''
  };

  let loading = true;
  let pageError = '';

  let passwordForm = {
    currentPassword: '',
    newPassword: '',
    confirmPassword: ''
  };
  let isSubmittingPassword = false;
  let passwordError = '';

  onMount(() => {
    loadProfile();
  });

  async function loadProfile() {
    loading = true;
    pageError = '';
    try {
      profile = await getMyProfile();
    } catch (err) {
      pageError = err.message || 'Profil bilgileriniz yüklenirken bir hata oluştu.';
    } finally {
      loading = false;
    }
  }

  async function handleChangePassword(e) {
    e.preventDefault();
    passwordError = '';

    if (!passwordForm.currentPassword) {
      passwordError = 'Lütfen mevcut şifrenizi girin.';
      return;
    }
    if (!passwordForm.newPassword) {
      passwordError = 'Lütfen yeni şifrenizi girin.';
      return;
    }
    if (passwordForm.newPassword.length < 4) {
      passwordError = 'Yeni şifreniz en az 4 karakter olmalıdır.';
      return;
    }
    if (passwordForm.newPassword !== passwordForm.confirmPassword) {
      passwordError = 'Yeni şifreler uyuşmuyor.';
      return;
    }

    isSubmittingPassword = true;
    try {
      await changeMyPassword({
        currentPassword: passwordForm.currentPassword,
        newPassword: passwordForm.newPassword
      });
      toastStore.success('Şifreniz başarıyla değiştirildi.');
      passwordForm = { currentPassword: '', newPassword: '', confirmPassword: '' };
    } catch (err) {
      passwordError = err.message || 'Şifre değiştirilirken bir hata oluştu.';
      toastStore.error(passwordError);
    } finally {
      isSubmittingPassword = false;
    }
  }

  function handleTabChange(e) {
    activeTab = e.detail;
    dispatch('changeTab', e.detail);
  }
</script>

<div class="vms-app-layout flex text-slate-100 font-sans antialiased">
  <!-- Fixed Background Image & Overlay -->
  <div class="vms-bg-fixed">
    <div class="vms-bg-image"></div>
    <div class="vms-bg-overlay"></div>
  </div>

  <Sidebar {activeTab} {isMobileOpen} on:closeMobile={() => (isMobileOpen = false)} on:changeTab={handleTabChange} />

  <main class="flex-1 p-4 md:p-8 overflow-y-auto max-w-4xl mx-auto w-full z-10 space-y-6">
    <!-- Header -->
    <header class="vms-card p-6 flex items-center justify-between gap-4">
      <div class="flex items-center gap-3">
        <button
          type="button"
          on:click={() => (isMobileOpen = true)}
          class="md:hidden p-2 text-slate-400 hover:text-white hover:bg-slate-800 rounded-xl transition"
        >
          <svg class="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 6h16M4 12h16M4 18h16"></path>
          </svg>
        </button>
        <div>
          <h1 class="text-2xl font-extrabold text-white tracking-tight">KULLANICI PROFİLİ</h1>
          <p class="text-xs text-slate-400 mt-1">Profil bilgilerinizi inceleyin ve şifrenizi güncelleyin</p>
        </div>
      </div>
    </header>

    {#if loading}
      <div class="flex items-center justify-center p-16">
        <div class="animate-spin w-8 h-8 border-4 border-purple-500 border-t-transparent rounded-full"></div>
      </div>
    {:else if pageError}
      <div class="p-4 bg-rose-500/10 border border-rose-500/30 rounded-2xl text-rose-300 text-sm">
        {pageError}
      </div>
    {:else}
      <div class="grid grid-cols-1 md:grid-cols-2 gap-8">
        <!-- Profile Details Card -->
        <div class="vms-card p-6 h-fit space-y-6">
          <div class="flex items-center gap-4 pb-6 border-b border-slate-800">
            <div class="w-16 h-16 rounded-2xl bg-gradient-to-tr from-purple-700 to-indigo-600 flex items-center justify-center text-white text-2xl font-black shadow-lg shadow-purple-900/30">
              {profile.fullName ? profile.fullName.charAt(0).toUpperCase() : 'P'}
            </div>
            <div>
              <h2 class="text-lg font-bold text-white">{profile.fullName}</h2>
              <p class="text-xs text-purple-400 font-semibold mt-0.5">{profile.title || 'Kurum Personeli'}</p>
              <span class="inline-block mt-2 px-2.5 py-0.5 bg-purple-500/20 text-purple-300 border border-purple-500/30 text-[10px] font-bold rounded-full uppercase">
                {profile.role || 'PERSONNEL'}
              </span>
            </div>
          </div>

          <div class="space-y-4 text-sm">
            <div>
              <span class="block text-xs font-semibold text-slate-400 uppercase tracking-wider mb-0.5">Departman</span>
              <p class="text-slate-200 font-medium">{profile.department || '-'}</p>
            </div>
            <div>
              <span class="block text-xs font-semibold text-slate-400 uppercase tracking-wider mb-0.5">E-posta Adresi</span>
              <p class="text-slate-200 font-medium">{profile.email || '-'}</p>
            </div>
            <div>
              <span class="block text-xs font-semibold text-slate-400 uppercase tracking-wider mb-0.5">Kullanıcı Adı</span>
              <p class="text-purple-300 font-mono font-bold">{profile.username || '-'}</p>
            </div>
          </div>
        </div>

        <!-- Change Password Card -->
        <div class="vms-card p-6 space-y-4">
          <div>
            <h2 class="text-lg font-bold text-white flex items-center gap-2">
              <svg class="w-5 h-5 text-purple-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 15v2m-6 4h12a2 2 0 002-2v-6a2 2 0 00-2-2H6a2 2 0 00-2 2v6a2 2 0 002 2zm10-10V7a4 4 0 00-8 0v4h8z"></path>
              </svg>
              <span>ŞİFRE DEĞİŞTİR</span>
            </h2>
            <p class="text-xs text-slate-400 mt-1">Hesap güvenliğiniz için şifrenizi periyodik olarak güncelleyin</p>
          </div>

          {#if passwordError}
            <div class="p-3 bg-rose-500/10 border border-rose-500/30 rounded-xl text-rose-300 text-xs">
              {passwordError}
            </div>
          {/if}

          <form on:submit={handleChangePassword} class="space-y-4">
            <div>
              <label for="currentPassword" class="block text-xs font-semibold text-slate-300 uppercase tracking-wider mb-1.5">
                Mevcut Şifre *
              </label>
              <input
                id="currentPassword"
                type="password"
                bind:value={passwordForm.currentPassword}
                disabled={isSubmittingPassword}
                placeholder="Mevcut şifreniz"
                class="vms-input"
              />
            </div>

            <div>
              <label for="newPassword" class="block text-xs font-semibold text-slate-300 uppercase tracking-wider mb-1.5">
                Yeni Şifre *
              </label>
              <input
                id="newPassword"
                type="password"
                bind:value={passwordForm.newPassword}
                disabled={isSubmittingPassword}
                placeholder="En az 4 karakter"
                class="vms-input"
              />
            </div>

            <div>
              <label for="confirmPassword" class="block text-xs font-semibold text-slate-300 uppercase tracking-wider mb-1.5">
                Yeni Şifre (Tekrar) *
              </label>
              <input
                id="confirmPassword"
                type="password"
                bind:value={passwordForm.confirmPassword}
                disabled={isSubmittingPassword}
                placeholder="Yeni şifrenizi tekrar girin"
                class="vms-input"
              />
            </div>

            <button
              type="submit"
              disabled={isSubmittingPassword}
              class="vms-btn vms-btn-primary w-full py-3 text-sm mt-2"
            >
              {#if isSubmittingPassword}
                <div class="animate-spin w-4 h-4 border-2 border-white border-t-transparent rounded-full"></div>
                <span>Güncelleniyor...</span>
              {:else}
                <span>Şifreyi Güncelle</span>
              {/if}
            </button>
          </form>
        </div>
      </div>
    {/if}
  </main>
</div>
