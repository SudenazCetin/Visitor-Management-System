<script>
  import { createEventDispatcher, onMount } from 'svelte';
  import { notificationStore } from '../stores/notificationStore.js';
  import { authStore } from '../stores/authStore.js';

  const dispatch = createEventDispatcher();
  let isOpen = false;
  let dropdownRef;

  $: token = $authStore.token;
  $: isAuthenticated = $authStore.isAuthenticated;

  $: if (isAuthenticated && token) {
    notificationStore.loadData();
    notificationStore.connectWebSocket(token);
  } else {
    notificationStore.disconnect();
  }

  function toggleDropdown() {
    isOpen = !isOpen;
  }

  function closeDropdown() {
    isOpen = false;
  }

  async function handleNotificationClick(item) {
    if (item.status === 'UNREAD') {
      await notificationStore.markAsRead(item.id);
    }
    if (item.actionUrl) {
      if (item.actionUrl.includes('my-visitors')) {
        dispatch('navigate', 'my-visitors');
      } else if (item.actionUrl.includes('profile')) {
        dispatch('navigate', 'profile');
      } else if (item.actionUrl.includes('visitors')) {
        dispatch('navigate', 'dashboard');
      }
    }
    closeDropdown();
  }

  function handleMarkAll() {
    notificationStore.markAllAsRead();
  }

  function formatTime(createdAt) {
    if (!createdAt) return '';
    const date = new Date(createdAt);
    if (isNaN(date.getTime())) return '';

    const now = new Date();
    const diffMs = now - date;
    const diffMins = Math.floor(diffMs / (1000 * 60));

    if (diffMins < 1) return 'Az önce';
    if (diffMins < 60) return `${diffMins} dk önce`;
    
    const hours = Math.floor(diffMins / 60);
    if (hours < 24) return `${hours} sa önce`;

    const day = String(date.getDate()).padStart(2, '0');
    const month = String(date.getMonth() + 1).padStart(2, '0');
    return `${day}.${month}`;
  }

  function handleClickOutside(event) {
    if (isOpen && dropdownRef && !dropdownRef.contains(event.target)) {
      closeDropdown();
    }
  }
</script>

<svelte:window on:click={handleClickOutside} />

<div class="relative inline-block text-left" bind:this={dropdownRef}>
  <!-- Bell Icon Button -->
  <button
    type="button"
    on:click|stopPropagation={toggleDropdown}
    class="relative p-2.5 text-slate-400 hover:text-white hover:bg-slate-800/80 rounded-xl transition cursor-pointer"
    title="Bildirimler"
    aria-label="Bildirimler"
  >
    <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 17h5l-1.405-1.405A2.032 2.032 0 0118 14.158V11a6.002 6.002 0 00-4-5.659V5a2 2 0 10-4 0v.341C7.67 6.165 6 8.388 6 11v3.159c0 .538-.214 1.055-.595 1.436L4 17h5m6 0v1a3 3 0 11-6 0v-1m6 0H9"></path>
    </svg>

    <!-- Unread Badge -->
    {#if $notificationStore.unreadCount > 0}
      <span class="absolute top-1.5 right-1.5 flex h-4 min-w-[1rem] items-center justify-center rounded-full bg-rose-500 px-1 text-[10px] font-extrabold text-white shadow-md ring-2 ring-slate-900 animate-pulse">
        {$notificationStore.unreadCount > 99 ? '99+' : $notificationStore.unreadCount}
      </span>
    {/if}
  </button>

  <!-- Dropdown Panel -->
  {#if isOpen}
    <div
      class="absolute right-0 mt-2 w-80 sm:w-96 vms-card z-50 overflow-hidden p-0 animate-in fade-in zoom-in-95 duration-150"
    >
      <!-- Header -->
      <div class="p-4 bg-slate-900/90 border-b border-slate-800 flex items-center justify-between">
        <div class="flex items-center gap-2">
          <h3 class="text-xs font-bold text-white uppercase tracking-wider">Bildirimler</h3>
          {#if $notificationStore.unreadCount > 0}
            <span class="px-2 py-0.5 bg-purple-500/20 border border-purple-500/30 text-purple-300 text-xs font-bold rounded-full">
              {$notificationStore.unreadCount} Okunmamış
            </span>
          {/if}
        </div>

        {#if $notificationStore.unreadCount > 0}
          <button
            type="button"
            on:click={handleMarkAll}
            class="text-xs font-semibold text-purple-400 hover:text-purple-300 hover:underline transition"
          >
            Tümünü Okundu Yap
          </button>
        {/if}
      </div>

      <!-- Notification List -->
      <div class="max-h-96 overflow-y-auto divide-y divide-slate-800/60">
        {#if $notificationStore.items.length === 0}
          <div class="p-8 text-center text-slate-400 text-xs">
            Henüz bildiriminiz bulunmuyor.
          </div>
        {:else}
          {#each $notificationStore.items as item (item.id)}
            <!-- svelte-ignore a11y-click-events-have-key-events -->
            <!-- svelte-ignore a11y-no-static-element-interactions -->
            <div
              on:click={() => handleNotificationClick(item)}
              class="p-4 hover:bg-purple-900/20 transition cursor-pointer flex gap-3 items-start {item.status === 'UNREAD' ? 'bg-purple-950/30 font-medium' : 'opacity-75'}"
            >
              <!-- Category Icon -->
              <div class="w-8 h-8 rounded-xl flex items-center justify-center shrink-0 mt-0.5 {item.type === 'SUCCESS' ? 'bg-emerald-500/20 text-emerald-400 border border-emerald-500/30' : item.type === 'WARNING' ? 'bg-amber-500/20 text-amber-400 border border-amber-500/30' : 'bg-purple-500/20 text-purple-400 border border-purple-500/30'}">
                {#if item.category === 'VISITOR'}
                  <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z"></path>
                  </svg>
                {:else if item.event === 'PASSWORD_CHANGED'}
                  <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 15v2m-6 4h12a2 2 0 002-2v-6a2 2 0 00-2-2H6a2 2 0 00-2 2v6a2 2 0 002 2zm10-10V7a4 4 0 00-8 0v4h8z"></path>
                  </svg>
                {:else if item.event === 'PROFILE_UPDATED'}
                  <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2.828l8.586-8.586z"></path>
                  </svg>
                {:else if item.event === 'SYSTEM_MESSAGE' || item.category === 'SYSTEM'}
                  <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M11 5.882V19.24a1.76 1.76 0 01-3.417.592l-2.147-6.15M18 13a3 3 0 100-6M5.436 13.683A4.001 4.001 0 017 6h1.832c4.1 0 7.625-1.234 9.168-3v14c-1.543-1.766-5.067-3-9.168-3H7a3.988 3.988 0 01-1.564-.317z"></path>
                  </svg>
                {:else}
                  <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z"></path>
                  </svg>
                {/if}
              </div>

              <!-- Details -->
              <div class="flex-1 min-w-0">
                <div class="flex items-center justify-between gap-2">
                  <p class="text-xs font-bold text-white truncate">{item.title}</p>
                  <span class="text-[10px] text-slate-400 shrink-0">{formatTime(item.createdAt)}</span>
                </div>
                <p class="text-xs text-slate-300 mt-1 line-clamp-2 leading-relaxed">{item.message}</p>
              </div>

              <!-- Unread Indicator Dot -->
              {#if item.status === 'UNREAD'}
                <span class="w-2 h-2 rounded-full bg-purple-400 shrink-0 mt-1.5"></span>
              {/if}
            </div>
          {/each}
        {/if}
      </div>
    </div>
  {/if}
</div>
