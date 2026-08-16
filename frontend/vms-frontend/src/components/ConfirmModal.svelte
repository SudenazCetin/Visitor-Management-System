<script>
  import { createEventDispatcher, onMount, onDestroy } from 'svelte';

  export var isOpen = false;
  export var title = 'Emin misiniz?';
  export var message = 'Bu işlem geri alınamaz.';
  export var confirmText = 'Sil';
  export var cancelText = 'İptal';
  export var loading = false;

  const dispatch = createEventDispatcher();

  function handleConfirm() {
    dispatch('confirm');
  }

  function handleCancel() {
    dispatch('cancel');
  }

  function handleKeydown(event) {
    if (event.key === 'Escape' && isOpen && !loading) {
      handleCancel();
    }
  }

  // Bind/unbind keyboard listener reactively
  $: if (typeof window !== 'undefined') {
    if (isOpen) {
      window.addEventListener('keydown', handleKeydown);
    } else {
      window.removeEventListener('keydown', handleKeydown);
    }
  }

  onDestroy(() => {
    if (typeof window !== 'undefined') {
      window.removeEventListener('keydown', handleKeydown);
    }
  });
</script>

{#if isOpen}
  <!-- svelte-ignore a11y-click-events-have-key-events a11y-no-noninteractive-element-interactions -->
  <div
    class="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-900/40 backdrop-blur-xs font-sans"
    role="dialog"
    aria-modal="true"
    aria-labelledby="confirm-modal-title"
    aria-describedby="confirm-modal-desc"
    on:click|self={handleCancel}
  >
    <div class="bg-white border border-slate-200/90 rounded-3xl p-6 shadow-2xl max-w-md w-full relative z-10 space-y-5 animate-in fade-in zoom-in duration-150">
      
      <div class="flex items-start gap-4">
        <div class="w-11 h-11 rounded-2xl bg-rose-50 text-rose-600 border border-rose-100 flex items-center justify-center shrink-0">
          <svg class="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16"></path>
          </svg>
        </div>

        <div class="space-y-1">
          <h3 id="confirm-modal-title" class="text-base font-bold text-slate-900">{title}</h3>
          <p id="confirm-modal-desc" class="text-xs text-slate-600 leading-relaxed">{message}</p>
        </div>
      </div>

      <div class="flex items-center justify-end gap-3 pt-2">
        <button
          type="button"
          disabled={loading}
          on:click={handleCancel}
          class="px-4 py-2.5 bg-slate-100 hover:bg-slate-200 text-slate-700 text-xs font-semibold rounded-xl border border-slate-200 transition disabled:opacity-50"
          aria-label={cancelText}
        >
          {cancelText}
        </button>

        <button
          type="button"
          disabled={loading}
          on:click={handleConfirm}
          class="px-4 py-2.5 bg-rose-600 hover:bg-rose-700 text-white text-xs font-semibold rounded-xl shadow-md shadow-rose-900/10 transition flex items-center gap-2 disabled:opacity-50"
        >
          {#if loading}
            <svg class="animate-spin w-4 h-4 text-white" fill="none" viewBox="0 0 24 24">
              <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
              <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
            </svg>
            <span>Siliniyor...</span>
          {:else}
            <span>{confirmText}</span>
          {/if}
        </button>
      </div>

    </div>
  </div>
{/if}
