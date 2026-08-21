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
    class="relative p-2.5 text-slate-500 hover:text-purple-700 hover:bg-purple-50 rounded-xl transition cursor-pointer"
    title="Bildirimler"
    aria-label="Bildirimler"
  >
    <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 17h5l-1.405-1.405A2.032 2.032 0 0118 14.158V11a6.002 6.002 0 00-4-5.659V5a2 2 0 10-4 0v.341C7.67 6.165 6 8.388 6 11v3.159c0 .538-.214 1.055-.595 1.436L4 17h5m6 0v1a3 3 0 11-6 0v-1m6 0H9"></path>
    </svg>

    <!-- Unread Badge -->
    {#if $notificationStore.unreadCount > 0}
      <span class="absolute top-1.5 right-1.5 flex h-4 min-w-[1rem] items-center justify-center rounded-full bg-rose-500 px-1 text-[10px] font-extrabold text-white shadow-sm ring-2 ring-white animate-pulse">
        {$notificationStore.unreadCount > 99 ? '99+' : $notificationStore.unreadCount}
      </span>
    {/if}
  </button>

  <!-- Dropdown Panel -->
  {#if isOpen}
    <div
      class="absolute right-0 mt-2 w-80 sm:w-96 bg-white border border-slate-200 rounded-2xl shadow-2xl z-50 overflow-hidden animate-in fade-in zoom-in-95 duration-150"
    >
      <!-- Header -->
      <div class="p-4 bg-slate-50 border-b border-slate-200/80 flex items-center justify-between">
        <div class="flex items-center gap-2">
          <h3 class="text-sm font-bold text-slate-800">Bildirimler</h3>
          {#if $notificationStore.unreadCount > 0}
            <span class="px-2 py-0.5 bg-purple-100 text-purple-700 text-xs font-bold rounded-full">
              {$notificationStore.unreadCount} Okunmamış
            </span>
          {/if}
        </div>

        {#if $notificationStore.unreadCount > 0}
          <button
            type="button"
            on:click={handleMarkAll}
            class="text-xs font-semibold text-purple-700 hover:text-purple-900 hover:underline transition"
          >
            Tümünü Okundu Yap
          </button>
        {/if}
      </div>

      <!-- Notification List -->
      <div class="max-h-96 overflow-y-auto divide-y divide-slate-100">
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
              class="p-4 hover:bg-purple-50/50 transition cursor-pointer flex gap-3 items-start {item.status === 'UNREAD' ? 'bg-purple-50/20 font-medium' : 'opacity-85'}"
            >
              <!-- Category Icon -->
              <div class="w-8 h-8 rounded-xl flex items-center justify-center shrink-0 mt-0.5 {item.type === 'SUCCESS' ? 'bg-emerald-100 text-emerald-700' : item.type === 'WARNING' ? 'bg-amber-100 text-amber-700' : 'bg-purple-100 text-purple-700'}">
                {#if item.category === 'VISITOR'}
                  <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z"></path>
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
                  <p class="text-xs font-bold text-slate-800 truncate">{item.title}</p>
                  <span class="text-[10px] text-slate-400 shrink-0">{formatTime(item.createdAt)}</span>
                </div>
                <p class="text-xs text-slate-600 mt-1 line-clamp-2 leading-relaxed">{item.message}</p>
              </div>

              <!-- Unread Indicator Dot -->
              {#if item.status === 'UNREAD'}
                <span class="w-2 h-2 rounded-full bg-purple-600 shrink-0 mt-1.5"></span>
              {/if}
            </div>
          {/each}
        {/if}
      </div>
    </div>
  {/if}
</div>
