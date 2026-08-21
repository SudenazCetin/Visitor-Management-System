<script>
  import { createEventDispatcher } from 'svelte';
  import { calculateDigitalDuration, calculateLiveDuration, formatDateTimeStr } from '../utils/duration.js';
  import { authStore } from '../stores/authStore.js';

  export var isOpen = false;
  export var visitor = null;
  export var now = new Date();

  const dispatch = createEventDispatcher();

  function close() {
    dispatch('close');
  }

  function handleCheckOut() {
    if (visitor && visitor.id) {
      dispatch('checkout', visitor);
      close();
    }
  }

  $: canCheckOut = visitor && visitor.isInside && ($authStore.user?.role === 'ADMIN' || $authStore.user?.role === 'RECEPTIONIST');
</script>

{#if isOpen && visitor}
  <!-- Backdrop -->
  <div
    class="fixed inset-0 bg-slate-950/70 backdrop-blur-md z-50 flex items-center justify-center p-4 transition-opacity"
    on:click={close}
    on:keydown={(e) => e.key === 'Escape' && close()}
    role="presentation"
  >
    <!-- Modal Card -->
    <div
      class="vms-card w-full max-w-lg overflow-hidden transform transition-all text-slate-100 p-0"
      on:click|stopPropagation
      on:keydown|stopPropagation
      role="presentation"
    >
      <!-- Header -->
      <div class="bg-gradient-to-r from-purple-900 via-indigo-950 to-slate-900 p-6 text-white relative border-b border-slate-800">
        <button
          type="button"
          on:click={close}
          class="absolute top-5 right-5 p-2 text-slate-400 hover:text-white hover:bg-slate-800 rounded-xl transition"
          aria-label="Kapat"
        >
          <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"></path>
          </svg>
        </button>

        <div class="flex items-center gap-4">
          <div class="w-14 h-14 rounded-2xl bg-purple-500/20 border border-purple-500/30 flex items-center justify-center text-xl font-extrabold text-purple-300 shrink-0">
            {visitor.fullName ? visitor.fullName.split(' ').map(n => n[0]).join('').substring(0,2).toUpperCase() : 'Z'}
          </div>
          <div>
            <div class="flex items-center gap-2">
              <h2 class="text-xl font-bold tracking-tight text-white">{visitor.fullName}</h2>
            </div>
            <p class="text-xs text-purple-300 mt-0.5">Ziyaretçi Detay Kartı (#ID: {visitor.id})</p>
          </div>
        </div>
      </div>

      <!-- Body -->
      <div class="p-6 space-y-5">
        <!-- Status Badge Banner -->
        <div class="p-4 rounded-2xl flex items-center justify-between border {visitor.isInside ? 'bg-emerald-500/10 border-emerald-500/30 text-emerald-300' : 'bg-slate-900/60 border-slate-800 text-slate-300'}">
          <div class="flex items-center gap-2.5">
            {#if visitor.isInside}
              <span class="relative flex h-3 w-3">
                <span class="animate-ping absolute inline-flex h-full w-full rounded-full bg-emerald-400 opacity-75"></span>
                <span class="relative inline-flex rounded-full h-3 w-3 bg-emerald-500"></span>
              </span>
              <span class="font-bold text-sm">İçeride (Aktif Ziyaretçi)</span>
            {:else}
              <span class="w-3 h-3 rounded-full bg-slate-500 inline-block"></span>
              <span class="font-bold text-sm">Çıkış Yaptı (Tamamlandı)</span>
            {/if}
          </div>

          <div class="font-mono text-xs font-bold px-3 py-1 rounded-xl border {visitor.isInside ? 'bg-emerald-500/20 border-emerald-500/40 text-emerald-200' : 'bg-slate-800 border-slate-700 text-slate-300'}">
            ⏱️ {calculateDigitalDuration(visitor.entryTime, visitor.exitTime, visitor.isInside, now)}
          </div>
        </div>

        <!-- Details Grid -->
        <div class="grid grid-cols-2 gap-4 text-sm">
          <!-- Host Personnel -->
          <div class="bg-slate-900/60 border border-slate-800 p-3.5 rounded-xl col-span-2">
            <span class="text-xs font-semibold text-slate-400 uppercase tracking-wider block mb-1">Görüşülen Personel</span>
            <div class="font-bold text-white">{visitor.hostName || '-'}</div>
            {#if visitor.hostDepartment}
              <div class="text-xs text-purple-400 font-medium mt-0.5">{visitor.hostDepartment}</div>
            {/if}
          </div>

          <!-- Entry Time -->
          <div class="bg-slate-900/60 border border-slate-800 p-3.5 rounded-xl">
            <span class="text-xs font-semibold text-slate-400 uppercase tracking-wider block mb-1">Giriş Zamanı</span>
            <div class="font-semibold text-slate-200 font-mono text-xs">
              {formatDateTimeStr(visitor.entryTime, true)}
            </div>
          </div>

          <!-- Exit Time -->
          <div class="bg-slate-900/60 border border-slate-800 p-3.5 rounded-xl">
            <span class="text-xs font-semibold text-slate-400 uppercase tracking-wider block mb-1">Çıkış Zamanı</span>
            <div class="font-semibold text-slate-200 font-mono text-xs">
              {visitor.isInside ? 'Henüz Çıkış Yapmadı' : formatDateTimeStr(visitor.exitTime, true)}
            </div>
          </div>

          <!-- Total Duration Text -->
          <div class="bg-slate-900/60 border border-slate-800 p-3.5 rounded-xl col-span-2">
            <span class="text-xs font-semibold text-slate-400 uppercase tracking-wider block mb-1">Geçen / Toplam Süre</span>
            <div class="font-semibold text-white">
              {calculateLiveDuration(visitor.entryTime, visitor.exitTime, visitor.isInside, now)}
            </div>
          </div>
        </div>
      </div>

      <!-- Footer Buttons -->
      <div class="px-6 py-4 bg-slate-900/80 border-t border-slate-800 flex items-center justify-between gap-3">
        <button
          type="button"
          on:click={close}
          class="vms-btn vms-btn-secondary text-xs"
        >
          Kapat
        </button>

        {#if canCheckOut}
          <button
            type="button"
            on:click={handleCheckOut}
            class="vms-btn vms-btn-danger text-xs"
          >
            <svg class="w-4 h-4 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M17 16l4-4m0 0l-4-4m4 4H7m6 4v1a3 3 0 01-3 3H6a3 3 0 01-3-3V7a3 3 0 013-3h4a3 3 0 013 3v1"></path>
            </svg>
            <span>Ziyaretçi Çıkışı Yap</span>
          </button>
        {/if}
      </div>
    </div>
  </div>
{/if}
