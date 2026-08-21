<script>
  import { createEventDispatcher } from 'svelte';
  import { registerUser } from '../api/authApi.js';

  const dispatch = createEventDispatcher();

  let accountType = 'PERSONNEL'; // 'PERSONNEL' or 'ADMIN'
  let fullName = '';
  let email = '';
  let username = '';
  let password = '';
  let confirmPassword = '';
  let adminRegistrationCode = '';
  let showPassword = false;

  let loading = false;
  let formError = '';
  let formSuccess = '';

  function navigateToLogin() {
    dispatch('switchToLogin');
  }

  async function handleRegister(event) {
    event.preventDefault();
    formError = '';
    formSuccess = '';

    if (!fullName.trim()) {
      formError = 'Ad Soyad alanı zorunludur.';
      return;
    }

    if (!email.trim()) {
      formError = 'E-posta adresi zorunludur.';
      return;
    }

    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    if (!emailRegex.test(email.trim())) {
      formError = 'Lütfen geçerli bir e-posta adresi girin.';
      return;
    }

    if (!username.trim()) {
      formError = 'Kullanıcı adı alanı zorunludur.';
      return;
    }

    if (username.trim().length < 3) {
      formError = 'Kullanıcı adı en az 3 karakter olmalıdır.';
      return;
    }

    if (!password.trim()) {
      formError = 'Şifre alanı zorunludur.';
      return;
    }

    if (password.trim().length < 6) {
      formError = 'Şifre en az 6 karakter olmalıdır.';
      return;
    }

    if (password !== confirmPassword) {
      formError = 'Şifreler uyuşmuyor.';
      return;
    }

    if (accountType === 'ADMIN' && !adminRegistrationCode.trim()) {
      formError = 'Yönetim kayıt kodu boş bırakılamaz.';
      return;
    }

    loading = true;
    try {
      const payload = {
        fullName: fullName.trim(),
        email: email.trim(),
        username: username.trim(),
        password: password.trim(),
        registrationType: accountType,
        adminRegistrationCode: accountType === 'ADMIN' ? adminRegistrationCode.trim() : null
      };

      await registerUser(payload);

      formSuccess = 'Kayıt başarıyla tamamlandı. Giriş ekranına yönlendiriliyorsunuz...';
      fullName = '';
      email = '';
      username = '';
      password = '';
      confirmPassword = '';
      adminRegistrationCode = '';

      setTimeout(() => {
        navigateToLogin();
      }, 1500);
    } catch (err) {
      formError = err.message || 'Kayıt oluşturulurken bir hata oluştu.';
    } finally {
      loading = false;
    }
  }
</script>

<div class="min-h-screen bg-slate-50 flex items-center justify-center p-4 sm:p-6 relative overflow-hidden font-sans">
  <!-- Light theme subtle ambient glows -->
  <div class="absolute -top-32 -left-32 w-96 h-96 bg-purple-200/50 rounded-full blur-3xl pointer-events-none"></div>
  <div class="absolute -bottom-32 -right-32 w-96 h-96 bg-indigo-200/40 rounded-full blur-3xl pointer-events-none"></div>

  <!-- Register Card -->
  <div class="w-full max-w-lg bg-white border border-slate-200/90 rounded-3xl p-8 shadow-xl shadow-purple-900/5 z-10 relative">
    
    <!-- Logo & Header -->
    <div class="text-center mb-6">
      <div class="inline-flex items-center justify-center w-14 h-14 bg-gradient-to-br from-purple-700 to-indigo-800 rounded-2xl shadow-md shadow-purple-900/20 mb-3 text-white">
        <svg class="w-8 h-8" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2.2" d="M18 9v3m0 0v3m0-3h3m-3 0h-3m-2-5a4 4 0 11-8 0 4 4 0 018 0zM3 20a6 6 0 0112 0v1H3v-1z"></path>
        </svg>
      </div>

      <h1 class="text-2xl font-bold text-slate-900 tracking-tight">VMS Pro</h1>
      <p class="text-xs text-purple-700 font-semibold tracking-wider uppercase mt-0.5">Yeni Kayıt Oluştur</p>
    </div>

    <!-- Account Type Selection Cards -->
    <div class="grid grid-cols-2 gap-3 mb-6">
      <button
        type="button"
        on:click={() => (accountType = 'PERSONNEL')}
        class="p-3.5 rounded-2xl border text-left transition flex flex-col items-center text-center gap-1.5 cursor-pointer select-none {accountType === 'PERSONNEL' ? 'bg-purple-50/80 border-purple-600 ring-2 ring-purple-600/20 text-purple-900' : 'bg-slate-50 border-slate-200 text-slate-600 hover:bg-slate-100'}"
      >
        <span class="text-xl">👤</span>
        <div>
          <div class="text-xs font-bold uppercase tracking-wider">Personel Hesabı</div>
          <div class="text-[10px] text-slate-500 mt-0.5">Şirket çalışanları için</div>
        </div>
      </button>

      <button
        type="button"
        on:click={() => (accountType = 'ADMIN')}
        class="p-3.5 rounded-2xl border text-left transition flex flex-col items-center text-center gap-1.5 cursor-pointer select-none {accountType === 'ADMIN' ? 'bg-purple-50/80 border-purple-600 ring-2 ring-purple-600/20 text-purple-900' : 'bg-slate-50 border-slate-200 text-slate-600 hover:bg-slate-100'}"
      >
        <span class="text-xl">🛡️</span>
        <div>
          <div class="text-xs font-bold uppercase tracking-wider">Yönetim Hesabı</div>
          <div class="text-[10px] text-slate-500 mt-0.5">Sistem yöneticileri için</div>
        </div>
      </button>
    </div>

    <!-- Form Alerts -->
    {#if formError}
      <div class="mb-5 p-3.5 bg-rose-50 border border-rose-200 rounded-xl text-rose-700 text-xs flex items-center gap-2.5">
        <svg class="w-4 h-4 shrink-0 text-rose-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 8v4m0 4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z"></path>
        </svg>
        <span>{formError}</span>
      </div>
    {/if}

    {#if formSuccess}
      <div class="mb-5 p-3.5 bg-emerald-50 border border-emerald-200 rounded-xl text-emerald-700 text-xs flex items-center gap-2.5">
        <svg class="w-4 h-4 shrink-0 text-emerald-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7"></path>
        </svg>
        <span>{formSuccess}</span>
      </div>
    {/if}

    <!-- Register Form -->
    <form on:submit={handleRegister} class="space-y-3.5">
      
      <!-- Full Name -->
      <div>
        <label for="reg-fullname" class="block text-xs font-semibold text-slate-700 uppercase tracking-wider mb-1">
          Ad Soyad *
        </label>
        <input
          id="reg-fullname"
          type="text"
          bind:value={fullName}
          placeholder="Örn: Ahmet Yılmaz"
          disabled={loading}
          class="w-full px-3.5 py-2.5 bg-slate-50 border border-slate-200 rounded-xl text-slate-800 placeholder-slate-400 text-sm focus:outline-none focus:bg-white focus:border-purple-600 focus:ring-1 focus:ring-purple-600 transition disabled:opacity-50"
        />
      </div>

      <!-- Email -->
      <div>
        <label for="reg-email" class="block text-xs font-semibold text-slate-700 uppercase tracking-wider mb-1">
          E-posta Adresi *
        </label>
        <input
          id="reg-email"
          type="email"
          bind:value={email}
          placeholder="Örn: ahmet.yilmaz@firma.com"
          disabled={loading}
          class="w-full px-3.5 py-2.5 bg-slate-50 border border-slate-200 rounded-xl text-slate-800 placeholder-slate-400 text-sm focus:outline-none focus:bg-white focus:border-purple-600 focus:ring-1 focus:ring-purple-600 transition disabled:opacity-50"
        />
      </div>

      <!-- Username -->
      <div>
        <label for="reg-username" class="block text-xs font-semibold text-slate-700 uppercase tracking-wider mb-1">
          Kullanıcı Adı *
        </label>
        <input
          id="reg-username"
          type="text"
          bind:value={username}
          placeholder="Kullanıcı adınızı girin"
          disabled={loading}
          class="w-full px-3.5 py-2.5 bg-slate-50 border border-slate-200 rounded-xl text-slate-800 placeholder-slate-400 text-sm focus:outline-none focus:bg-white focus:border-purple-600 focus:ring-1 focus:ring-purple-600 transition disabled:opacity-50"
        />
      </div>

      <!-- Password & Confirm Password Grid -->
      <div class="grid grid-cols-1 sm:grid-cols-2 gap-3">
        <div>
          <label for="reg-password" class="block text-xs font-semibold text-slate-700 uppercase tracking-wider mb-1">
            Şifre *
          </label>
          <input
            id="reg-password"
            type="password"
            bind:value={password}
            placeholder="En az 6 karakter"
            disabled={loading}
            class="w-full px-3.5 py-2.5 bg-slate-50 border border-slate-200 rounded-xl text-slate-800 placeholder-slate-400 text-sm focus:outline-none focus:bg-white focus:border-purple-600 focus:ring-1 focus:ring-purple-600 transition disabled:opacity-50"
          />
        </div>

        <div>
          <label for="reg-confirm-password" class="block text-xs font-semibold text-slate-700 uppercase tracking-wider mb-1">
            Şifre Tekrarı *
          </label>
          <input
            id="reg-confirm-password"
            type="password"
            bind:value={confirmPassword}
            placeholder="Şifrenizi tekrar girin"
            disabled={loading}
            class="w-full px-3.5 py-2.5 bg-slate-50 border border-slate-200 rounded-xl text-slate-800 placeholder-slate-400 text-sm focus:outline-none focus:bg-white focus:border-purple-600 focus:ring-1 focus:ring-purple-600 transition disabled:opacity-50"
          />
        </div>
      </div>

      <!-- Admin Code Input (Only visible when ADMIN selected) -->
      {#if accountType === 'ADMIN'}
        <div class="p-3.5 bg-purple-50/70 border border-purple-200/80 rounded-2xl space-y-1">
          <label for="reg-admin-code" class="block text-xs font-bold text-purple-900 uppercase tracking-wider">
            Yönetim Kayıt Kodu *
          </label>
          <input
            id="reg-admin-code"
            type="password"
            bind:value={adminRegistrationCode}
            placeholder="Yönetim kayıt kodunu girin"
            disabled={loading}
            class="w-full px-3.5 py-2 bg-white border border-slate-200 rounded-xl text-slate-800 placeholder-slate-400 text-sm focus:outline-none focus:border-purple-600 focus:ring-1 focus:ring-purple-600 transition disabled:opacity-50"
          />
          <p class="text-[10px] text-purple-700 mt-1">Yalnızca yetkili sistem yöneticilerine sağlanan kayıt kodudur.</p>
        </div>
      {/if}

      <!-- Submit Button -->
      <button
        type="submit"
        disabled={loading}
        class="w-full py-3 px-4 bg-gradient-to-r from-purple-700 via-purple-800 to-indigo-800 hover:from-purple-800 hover:to-indigo-900 active:scale-[0.99] text-white font-semibold text-sm rounded-xl shadow-md shadow-purple-900/20 transition flex items-center justify-center gap-2 disabled:opacity-50 mt-4"
      >
        {#if loading}
          <div class="animate-spin w-4 h-4 border-2 border-white border-t-transparent rounded-full"></div>
          <span>Kayıt Yapılıyor...</span>
        {:else}
          <span>Kayıt Ol ({accountType === 'ADMIN' ? 'Yönetici' : 'Personel'})</span>
        {/if}
      </button>

    </form>

    <!-- Bottom link to Login -->
    <div class="mt-5 text-center text-xs text-slate-500 border-t border-slate-100 pt-4">
      Zaten hesabınız var mı? 
      <button
        type="button"
        on:click={navigateToLogin}
        class="text-purple-700 font-bold hover:underline ml-1"
      >
        Giriş Yap
      </button>
    </div>

  </div>
</div>
