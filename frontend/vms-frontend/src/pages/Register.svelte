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

<div class="vms-app-layout min-h-screen flex items-center justify-center p-4 sm:p-6 relative overflow-hidden font-sans text-slate-100">
  <!-- Fixed Background Image & Overlay -->
  <div class="vms-bg-fixed">
    <div class="vms-bg-image"></div>
    <div class="vms-bg-overlay"></div>
  </div>

  <!-- Register Card -->
  <div class="vms-card w-full max-w-lg p-8 z-10 relative space-y-6">
    
    <!-- Logo & Header -->
    <div class="text-center space-y-2">
      <div class="inline-flex items-center justify-center w-14 h-14 bg-gradient-to-br from-purple-600 via-indigo-600 to-purple-800 rounded-2xl shadow-xl shadow-purple-900/40 text-white border border-purple-400/20">
        <svg class="w-8 h-8" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2.2" d="M18 9v3m0 0v3m0-3h3m-3 0h-3m-2-5a4 4 0 11-8 0 4 4 0 018 0zM3 20a6 6 0 0112 0v1H3v-1z"></path>
        </svg>
      </div>

      <div>
        <h1 class="text-2xl font-black text-white tracking-tight">VMS Pro</h1>
        <p class="text-xs text-purple-300 font-bold tracking-widest uppercase mt-0.5">Yeni Kayıt Oluştur</p>
      </div>
    </div>

    <!-- Account Type Selection Cards -->
    <div class="grid grid-cols-2 gap-3">
      <button
        type="button"
        on:click={() => (accountType = 'PERSONNEL')}
        class="p-3.5 rounded-2xl border text-left transition flex flex-col items-center text-center gap-1.5 cursor-pointer select-none {accountType === 'PERSONNEL' ? 'bg-purple-600/30 border-purple-500 text-white shadow-lg' : 'bg-slate-900/60 border-slate-800 text-slate-400 hover:border-slate-700'}"
      >
        <span class="text-xl">👤</span>
        <div>
          <div class="text-xs font-bold uppercase tracking-wider">Personel Hesabı</div>
          <div class="text-[10px] text-slate-400 mt-0.5">Şirket çalışanları için</div>
        </div>
      </button>

      <button
        type="button"
        on:click={() => (accountType = 'ADMIN')}
        class="p-3.5 rounded-2xl border text-left transition flex flex-col items-center text-center gap-1.5 cursor-pointer select-none {accountType === 'ADMIN' ? 'bg-purple-600/30 border-purple-500 text-white shadow-lg' : 'bg-slate-900/60 border-slate-800 text-slate-400 hover:border-slate-700'}"
      >
        <span class="text-xl">🛡️</span>
        <div>
          <div class="text-xs font-bold uppercase tracking-wider">Yönetim Hesabı</div>
          <div class="text-[10px] text-slate-400 mt-0.5">Sistem yöneticileri için</div>
        </div>
      </button>
    </div>

    <!-- Form Alerts -->
    {#if formError}
      <div class="p-3.5 bg-rose-500/10 border border-rose-500/30 rounded-xl text-rose-300 text-xs flex items-center gap-2.5">
        <svg class="w-4 h-4 shrink-0 text-rose-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 8v4m0 4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z"></path>
        </svg>
        <span>{formError}</span>
      </div>
    {/if}

    {#if formSuccess}
      <div class="p-3.5 bg-emerald-500/10 border border-emerald-500/30 rounded-xl text-emerald-300 text-xs flex items-center gap-2.5">
        <svg class="w-4 h-4 shrink-0 text-emerald-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7"></path>
        </svg>
        <span>{formSuccess}</span>
      </div>
    {/if}

    <!-- Register Form -->
    <form on:submit={handleRegister} class="space-y-3.5">
      
      <!-- Full Name -->
      <div>
        <label for="reg-fullname" class="block text-xs font-semibold text-slate-300 uppercase tracking-wider mb-1">
          Ad Soyad *
        </label>
        <input
          id="reg-fullname"
          type="text"
          bind:value={fullName}
          placeholder="Örn: Ahmet Yılmaz"
          disabled={loading}
          class="vms-input py-2.5"
        />
      </div>

      <!-- Email -->
      <div>
        <label for="reg-email" class="block text-xs font-semibold text-slate-300 uppercase tracking-wider mb-1">
          E-posta Adresi *
        </label>
        <input
          id="reg-email"
          type="email"
          bind:value={email}
          placeholder="Örn: ahmet.yilmaz@firma.com"
          disabled={loading}
          class="vms-input py-2.5"
        />
      </div>

      <!-- Username -->
      <div>
        <label for="reg-username" class="block text-xs font-semibold text-slate-300 uppercase tracking-wider mb-1">
          Kullanıcı Adı *
        </label>
        <input
          id="reg-username"
          type="text"
          bind:value={username}
          placeholder="Kullanıcı adınızı girin"
          disabled={loading}
          class="vms-input py-2.5"
        />
      </div>

      <!-- Password & Confirm Password Grid -->
      <div class="grid grid-cols-1 sm:grid-cols-2 gap-3">
        <div>
          <label for="reg-password" class="block text-xs font-semibold text-slate-300 uppercase tracking-wider mb-1">
            Şifre *
          </label>
          <input
            id="reg-password"
            type="password"
            bind:value={password}
            placeholder="En az 6 karakter"
            disabled={loading}
            class="vms-input py-2.5"
          />
        </div>

        <div>
          <label for="reg-confirm-password" class="block text-xs font-semibold text-slate-300 uppercase tracking-wider mb-1">
            Şifre Tekrarı *
          </label>
          <input
            id="reg-confirm-password"
            type="password"
            bind:value={confirmPassword}
            placeholder="Şifrenizi tekrar girin"
            disabled={loading}
            class="vms-input py-2.5"
          />
        </div>
      </div>

      <!-- Admin Code Input (Only visible when ADMIN selected) -->
      {#if accountType === 'ADMIN'}
        <div class="p-3.5 bg-purple-500/10 border border-purple-500/30 rounded-2xl space-y-1">
          <label for="reg-admin-code" class="block text-xs font-bold text-purple-300 uppercase tracking-wider">
            Yönetim Kayıt Kodu *
          </label>
          <input
            id="reg-admin-code"
            type="password"
            bind:value={adminRegistrationCode}
            placeholder="Yönetim kayıt kodunu girin"
            disabled={loading}
            class="vms-input py-2 text-xs"
          />
          <p class="text-[10px] text-purple-400 mt-1">Yalnızca yetkili sistem yöneticilerine sağlanan kayıt kodudur.</p>
        </div>
      {/if}

      <!-- Submit Button -->
      <button
        type="submit"
        disabled={loading}
        class="vms-btn vms-btn-primary w-full py-3 text-sm font-bold mt-4"
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
    <div class="mt-5 text-center text-xs text-slate-400 border-t border-slate-800 pt-4">
      Zaten hesabınız var mı? 
      <button
        type="button"
        on:click={navigateToLogin}
        class="text-purple-400 font-bold hover:underline ml-1"
      >
        Giriş Yap
      </button>
    </div>

  </div>
</div>
