<script>
  import { toastStore } from '../stores/toastStore.js';
</script>

<div
  class="fixed top-5 right-5 z-50 flex flex-col gap-3 max-w-sm w-full pointer-events-none font-sans"
  aria-live="polite"
  aria-label="Bildirimler"
>
  {#each $toastStore as toast (toast.id)}
    <div
      class="toast-enter pointer-events-auto flex items-center justify-between p-4 rounded-2xl shadow-lg border transition-all duration-300
      {toast.type === 'success' ? 'bg-white text-emerald-800 border-emerald-200 shadow-emerald-100/50' : ''}
      {toast.type === 'error' ? 'bg-white text-rose-800 border-rose-200 shadow-rose-100/50' : ''}
      {toast.type === 'warning' ? 'bg-white text-amber-800 border-amber-200 shadow-amber-100/50' : ''}
      {toast.type === 'info' ? 'bg-white text-slate-800 border-purple-200 shadow-purple-100/50' : ''}"
      role="alert"
    >
      <div class="flex items-center gap-3 min-w-0 pr-2">
        {#if toast.type === 'success'}
          <div class="w-8 h-8 rounded-xl bg-emerald-50 flex items-center justify-center shrink-0 text-emerald-600 border border-emerald-100">
            <svg class="w-4.5 h-4.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2.5" d="M5 13l4 4L19 7"></path>
            </svg>
          </div>
        {:else if toast.type === 'error'}
          <div class="w-8 h-8 rounded-xl bg-rose-50 flex items-center justify-center shrink-0 text-rose-600 border border-rose-100">
            <svg class="w-4.5 h-4.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2.5" d="M6 18L18 6M6 6l12 12"></path>
            </svg>
          </div>
        {:else if toast.type === 'warning'}
          <div class="w-8 h-8 rounded-xl bg-amber-50 flex items-center justify-center shrink-0 text-amber-600 border border-amber-100">
            <svg class="w-4.5 h-4.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z"></path>
            </svg>
          </div>
        {:else}
          <div class="w-8 h-8 rounded-xl bg-purple-50 flex items-center justify-center shrink-0 text-purple-600 border border-purple-100">
            <svg class="w-4.5 h-4.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z"></path>
            </svg>
          </div>
        {/if}

        <p class="text-xs font-semibold leading-relaxed">{toast.message}</p>
      </div>

      <button
        type="button"
        on:click={() => toastStore.remove(toast.id)}
        class="text-slate-400 hover:text-slate-600 p-1 rounded-lg transition shrink-0"
        aria-label="Bildirimi kapat"
      >
        <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"></path>
        </svg>
      </button>
    </div>
  {/each}
</div>
