<script>
  import { createEventDispatcher } from 'svelte';
  import { registerUser } from '../api/authApi.js';

  const dispatch = createEventDispatcher();

  let username = '';
  let password = '';
  let confirmPassword = '';
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

    if (!username.trim()) {
      formError = 'Kullanıcı adı boş bırakılamaz.';
      return;
    }

    if (username.trim().length < 3) {
      formError = 'Kullanıcı adı en az 3 karakter olmalıdır.';
      return;
    }

    if (!password.trim()) {
      formError = 'Şifre boş bırakılamaz.';
      return;
    }

    if (password.trim().length < 6) {
      formError = 'Şifre en az 6 karakter olmalıdır.';
      return;
    }

    if (password !== confirmPassword) {
      formError = 'Şifreler eşleşmiyor.';
      return;
    }

    loading = true;
    try {
      await registerUser({
        username: username.trim(),
        password: password.trim(),
      });

      formSuccess = 'Kayıt başarılı. Giriş yapabilirsiniz.';
      username = '';
      password = '';
      confirmPassword = '';

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
  <div class="w-full max-w-md bg-white border border-slate-200/90 rounded-3xl p-8 shadow-xl shadow-purple-900/5 z-10 relative">
    
    <!-- Logo & Header -->
    <div class="text-center mb-8">
      <div class="inline-flex items-center justify-center w-16 h-16 bg-gradient-to-br from-purple-700 to-indigo-800 rounded-2xl shadow-md shadow-purple-900/20 mb-4 text-white">
        <svg class="w-9 h-9" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2.2" d="M18 9v3m0 0v3m0-3h3m-3 0h-3m-2-5a4 4 0 11-8 0 4 4 0 018 0zM3 20a6 6 0 0112 0v1H3v-1z"></path>
        </svg>
      </div>

      <h1 class="text-2xl font-bold text-slate-900 tracking-tight">VMS Pro</h1>
      <p class="text-xs text-purple-700 font-semibold tracking-wider uppercase mt-1">Yeni Hesap Oluştur</p>
    </div>

    <!-- Form Alerts -->
    {#if formError}
      <div class="mb-6 p-4 bg-rose-50 border border-rose-200 rounded-xl text-rose-700 text-sm flex items-center gap-3">
        <svg class="w-5 h-5 shrink-0 text-rose-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 8v4m0 4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z"></path>
        </svg>
        <span>{formError}</span>
      </div>
    {/if}

    {#if formSuccess}
      <div class="mb-6 p-4 bg-emerald-50 border border-emerald-200 rounded-xl text-emerald-700 text-sm flex items-center gap-3">
        <svg class="w-5 h-5 shrink-0 text-emerald-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7"></path>
        </svg>
        <span>{formSuccess}</span>
      </div>
    {/if}

    <!-- Register Form -->
    <form on:submit={handleRegister} class="space-y-4">
      
      <!-- Username Field -->
      <div>
        <label for="reg-username" class="block text-xs font-semibold text-slate-700 uppercase tracking-wider mb-1.5">
          Kullanıcı Adı *
        </label>
        <div class="relative">
          <div class="absolute inset-y-0 left-0 pl-3.5 flex items-center pointer-events-none text-slate-400">
            <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z"></path>
            </svg>
          </div>
          <input
            id="reg-username"
            type="text"
            bind:value={username}
            placeholder="Kullanıcı adınızı girin"
            disabled={loading}
            class="w-full pl-11 pr-4 py-2.5 bg-slate-50 border border-slate-200 rounded-xl text-slate-800 placeholder-slate-400 text-sm focus:outline-none focus:bg-white focus:border-purple-600 focus:ring-1 focus:ring-purple-600 transition disabled:opacity-50"
          />
        </div>
      </div>

      <!-- Password Field -->
      <div>
        <label for="reg-password" class="block text-xs font-semibold text-slate-700 uppercase tracking-wider mb-1.5">
          Şifre *
        </label>
        <div class="relative">
          <div class="absolute inset-y-0 left-0 pl-3.5 flex items-center pointer-events-none text-slate-400">
            <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 15v2m-6 4h12a2 2 0 002-2v-6a2 2 0 00-2-2H6a2 2 0 00-2 2v6a2 2 0 002 2zm10-10V7a4 4 0 00-8 0v4h8z"></path>
            </svg>
          </div>
          {#if showPassword}
            <input
              id="reg-password"
              type="text"
              bind:value={password}
              placeholder="••••••••"
              disabled={loading}
              class="w-full pl-11 pr-11 py-2.5 bg-slate-50 border border-slate-200 rounded-xl text-slate-800 placeholder-slate-400 text-sm focus:outline-none focus:bg-white focus:border-purple-600 focus:ring-1 focus:ring-purple-600 transition disabled:opacity-50"
            />
          {:else}
            <input
              id="reg-password"
              type="password"
              bind:value={password}
              placeholder="••••••••"
              disabled={loading}
              class="w-full pl-11 pr-11 py-2.5 bg-slate-50 border border-slate-200 rounded-xl text-slate-800 placeholder-slate-400 text-sm focus:outline-none focus:bg-white focus:border-purple-600 focus:ring-1 focus:ring-purple-600 transition disabled:opacity-50"
            />
          {/if}
          <button
            type="button"
            on:click={() => (showPassword = !showPassword)}
            class="absolute inset-y-0 right-0 pr-3.5 flex items-center text-slate-400 hover:text-slate-600 transition"
          >
            {#if showPassword}
              <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13.875 18.825A10.05 10.05 0 0112 19c-4.478 0-8.268-2.943-9.543-7a9.97 9.97 0 011.563-3.029m5.858-5.908a10.018 10.018 0 013.682-.733c4.478 0 8.268 2.943 9.543 7a10.025 10.025 0 01-4.132 5.411m-6.09-3.87a3 3 0 11-4.243-4.243"></path>
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M3 3l18 18"></path>
              </svg>
            {:else}
              <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z"></path>
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7z"></path>
              </svg>
            {/if}
          </button>
        </div>
      </div>

      <!-- Confirm Password Field -->
      <div>
        <label for="reg-confirm-password" class="block text-xs font-semibold text-slate-700 uppercase tracking-wider mb-1.5">
          Şifre Tekrarı *
        </label>
        <div class="relative">
          <div class="absolute inset-y-0 left-0 pl-3.5 flex items-center pointer-events-none text-slate-400">
            <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12l2 2 4-4m5.618-4.016A11.955 11.955 0 0112 2.944a11.955 11.955 0 01-8.618 3.04A12.02 12.02 0 003 9c0 5.591 3.824 10.29 9 11.622 5.176-1.332 9-6.03 9-11.622 0-1.042-.133-2.052-.382-3.016z"></path>
            </svg>
          </div>
          {#if showPassword}
            <input
              id="reg-confirm-password"
              type="text"
              bind:value={confirmPassword}
              placeholder="••••••••"
              disabled={loading}
              class="w-full pl-11 pr-4 py-2.5 bg-slate-50 border border-slate-200 rounded-xl text-slate-800 placeholder-slate-400 text-sm focus:outline-none focus:bg-white focus:border-purple-600 focus:ring-1 focus:ring-purple-600 transition disabled:opacity-50"
            />
          {:else}
            <input
              id="reg-confirm-password"
              type="password"
              bind:value={confirmPassword}
              placeholder="••••••••"
              disabled={loading}
              class="w-full pl-11 pr-4 py-2.5 bg-slate-50 border border-slate-200 rounded-xl text-slate-800 placeholder-slate-400 text-sm focus:outline-none focus:bg-white focus:border-purple-600 focus:ring-1 focus:ring-purple-600 transition disabled:opacity-50"
            />
          {/if}
        </div>
      </div>

      <!-- Submit Button -->
      <button
        type="submit"
        disabled={loading}
        class="w-full py-3.5 px-4 bg-gradient-to-r from-purple-700 via-purple-800 to-indigo-800 hover:from-purple-800 hover:to-indigo-900 active:from-purple-900 active:to-indigo-950 text-white font-semibold text-sm rounded-xl shadow-lg shadow-purple-900/20 focus:outline-none focus:ring-2 focus:ring-purple-600 focus:ring-offset-2 transition flex items-center justify-center gap-2 disabled:opacity-50 disabled:cursor-not-allowed mt-2"
      >
        {#if loading}
          <svg class="animate-spin w-5 h-5 text-white" fill="none" viewBox="0 0 24 24">
            <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
            <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
          </svg>
          <span>Kayıt Oluşturuluyor...</span>
        {:else}
          <span>Kayıt Ol</span>
          <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M14 5l7 7m0 0l-7 7m7-7H3"></path>
          </svg>
        {/if}
      </button>

    </form>

    <!-- Bottom link to Login -->
    <div class="mt-6 text-center text-xs text-slate-500 border-t border-slate-100 pt-4">
      Zaten hesabın var mı? 
      <button
        type="button"
        on:click={navigateToLogin}
        class="text-purple-700 font-bold hover:underline ml-1"
      >
        Giriş Yap
      </button>
    </div>

    <!-- Footer note -->
    <div class="mt-4 text-center text-[10px] text-slate-400">
      Visitor Management System &copy; 2026
    </div>

  </div>
</div>
