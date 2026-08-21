<script>
  import { createEventDispatcher } from 'svelte';
  import { authStore } from '../stores/authStore.js';
  import { loginUser } from '../api/authApi.js';

  const dispatch = createEventDispatcher();

  let username = '';
  let password = '';
  let showPassword = false;
  let formError = '';

  function navigateToRegister() {
    dispatch('switchToRegister');
  }

  async function handleLogin(event) {
    event.preventDefault();
    formError = '';

    if (!username.trim()) {
      formError = 'Kullanıcı adı boş bırakılamaz.';
      return;
    }

    if (!password.trim()) {
      formError = 'Şifre boş bırakılamaz.';
      return;
    }

    authStore.setLoading(true);
    try {
      const response = await loginUser({
        username: username.trim(),
        password: password.trim(),
      });

      // Save token, username, role in authStore & localStorage
      authStore.loginSuccess(response.token, response.username, response.role);
    } catch (err) {
      formError = err.message || 'Kullanıcı adı veya şifre hatalı.';
      authStore.setError(formError);
    } finally {
      authStore.setLoading(false);
    }
  }
</script>

<div class="vms-app-layout min-h-screen flex items-center justify-center p-4 sm:p-6 relative overflow-hidden font-sans text-slate-100">
  <!-- Fixed Background Image & Overlay -->
  <div class="vms-bg-fixed">
    <div class="vms-bg-image"></div>
    <div class="vms-bg-overlay"></div>
  </div>

  <!-- Login Card -->
  <div class="vms-card w-full max-w-md p-8 z-10 relative space-y-6">
    
    <!-- Logo & Header -->
    <div class="text-center space-y-3">
      <div class="inline-flex items-center justify-center w-16 h-16 bg-gradient-to-br from-purple-600 via-indigo-600 to-purple-800 rounded-2xl shadow-xl shadow-purple-900/40 text-white border border-purple-400/20">
        <svg class="w-9 h-9" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2.2" d="M19 21V5a2 2 0 00-2-2H7a2 2 0 00-2 2v16m14 0h2m-2 0h-5m-9 0H3m2 0h5m0 0h4m-4 0V11m0 0l2 2m-2-2l-2 2m6-6v6m0 0l2-2m-2 2l-2-2"></path>
        </svg>
      </div>

      <div>
        <h1 class="text-2xl font-black text-white tracking-tight">VMS Pro</h1>
        <p class="text-xs text-purple-300 font-bold tracking-widest uppercase mt-1">Ziyaretçi Takip Sistemi</p>
      </div>
    </div>

    <!-- Form Error Alert -->
    {#if formError || $authStore.error}
      <div class="p-4 bg-rose-500/10 border border-rose-500/30 rounded-xl text-rose-300 text-xs flex items-center gap-3">
        <svg class="w-5 h-5 shrink-0 text-rose-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 8v4m0 4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z"></path>
        </svg>
        <span>{formError || $authStore.error}</span>
      </div>
    {/if}

    <!-- Login Form -->
    <form on:submit={handleLogin} class="space-y-5">
      
      <!-- Username Field -->
      <div>
        <label for="username" class="block text-xs font-semibold text-slate-300 uppercase tracking-wider mb-2">
          Kullanıcı Adı
        </label>
        <div class="relative">
          <div class="absolute inset-y-0 left-0 pl-3.5 flex items-center pointer-events-none text-slate-400">
            <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z"></path>
            </svg>
          </div>
          <input
            id="username"
            type="text"
            bind:value={username}
            placeholder="Kullanıcı adınızı girin"
            disabled={$authStore.loading}
            class="vms-input pl-11 pr-4 py-3"
          />
        </div>
      </div>

      <!-- Password Field -->
      <div>
        <label for="password" class="block text-xs font-semibold text-slate-300 uppercase tracking-wider mb-2">
          Şifre
        </label>
        <div class="relative">
          <div class="absolute inset-y-0 left-0 pl-3.5 flex items-center pointer-events-none text-slate-400">
            <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 15v2m-6 4h12a2 2 0 002-2v-6a2 2 0 00-2-2H6a2 2 0 00-2 2v6a2 2 0 002 2zm10-10V7a4 4 0 00-8 0v4h8z"></path>
            </svg>
          </div>
          {#if showPassword}
            <input
              id="password"
              type="text"
              bind:value={password}
              placeholder="••••••••"
              disabled={$authStore.loading}
              class="vms-input pl-11 pr-11 py-3"
            />
          {:else}
            <input
              id="password"
              type="password"
              bind:value={password}
              placeholder="••••••••"
              disabled={$authStore.loading}
              class="vms-input pl-11 pr-11 py-3"
            />
          {/if}
          <button
            type="button"
            on:click={() => (showPassword = !showPassword)}
            class="absolute inset-y-0 right-0 pr-3.5 flex items-center text-slate-400 hover:text-white transition"
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

      <!-- Submit Button -->
      <button
        type="submit"
        disabled={$authStore.loading}
        class="vms-btn vms-btn-primary w-full py-3.5 text-sm font-bold flex items-center justify-center gap-2 mt-2"
      >
        {#if $authStore.loading}
          <svg class="animate-spin w-5 h-5 text-white" fill="none" viewBox="0 0 24 24">
            <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
            <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
          </svg>
          <span>Giriş Yapılıyor...</span>
        {:else}
          <span>Giriş Yap</span>
          <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M14 5l7 7m0 0l-7 7m7-7H3"></path>
          </svg>
        {/if}
      </button>

    </form>

    <!-- Bottom link to Register -->
    <div class="mt-6 text-center text-xs text-slate-400 border-t border-slate-800 pt-4">
      Hesabın yok mu? 
      <button
        type="button"
        on:click={navigateToRegister}
        class="text-purple-400 font-bold hover:underline ml-1"
      >
        Kayıt Ol
      </button>
    </div>

    <!-- Footer note -->
    <div class="mt-4 text-center text-[10px] text-slate-500">
      Visitor Management System &copy; 2026
    </div>

  </div>
</div>
