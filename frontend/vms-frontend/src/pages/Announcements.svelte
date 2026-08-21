<script>
  import { createEventDispatcher } from 'svelte';
  import Sidebar from '../components/Sidebar.svelte';
  import { toastStore } from '../stores/toastStore.js';
  import { sendAnnouncement } from '../api/announcementApi.js';

  const dispatch = createEventDispatcher();
  export var activeTab = 'announcements';

  let isMobileOpen = false;

  let form = {
    title: '',
    message: '',
    target: 'ALL'
  };

  let isSubmitting = false;
  let formError = '';

  async function handleSubmit(e) {
    e.preventDefault();
    formError = '';

    if (!form.title.trim()) {
      formError = 'Lütfen duyuru başlığı girin.';
      return;
    }
    if (!form.message.trim()) {
      formError = 'Lütfen duyuru mesajı girin.';
      return;
    }

    isSubmitting = true;
    try {
      const res = await sendAnnouncement({
        title: form.title.trim(),
        message: form.message.trim(),
        target: form.target
      });

      toastStore.success(`${res.message || 'Duyuru başarıyla gönderildi.'} (${res.recipientCount} alıcı)`);
      form = { title: '', message: '', target: 'ALL' };
    } catch (err) {
      formError = err.message || 'Duyuru gönderilirken bir hata oluştu.';
      toastStore.error(formError);
    } finally {
      isSubmitting = false;
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
          <h1 class="text-2xl font-extrabold text-white tracking-tight">SİSTEM DUYURULARI</h1>
          <p class="text-xs text-slate-400 mt-1">Kullanıcılara veya rol gruplarına anlık sistem duyuruları gönderin</p>
        </div>
      </div>
    </header>

    <div class="vms-card p-6 space-y-6">
      <div class="flex items-center gap-3 pb-6 border-b border-slate-800">
        <div class="w-10 h-10 rounded-xl bg-purple-500/20 text-purple-400 border border-purple-500/30 flex items-center justify-center">
          <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M11 5.882V19.24a1.76 1.76 0 01-3.417.592l-2.147-6.15M18 13a3 3 0 100-6M5.436 13.683A4.001 4.001 0 017 6h1.832c4.1 0 7.625-1.234 9.168-3v14c-1.543-1.766-5.067-3-9.168-3H7a3.988 3.988 0 01-1.564-.317z"></path>
          </svg>
        </div>
        <div>
          <h2 class="text-lg font-bold text-white">YENİ DUYURU OLUŞTUR</h2>
          <p class="text-xs text-slate-400">Hedef kitleyi seçin ve duyuruyu anlık olarak yayınlayın</p>
        </div>
      </div>

      {#if formError}
        <div class="p-4 bg-rose-500/10 border border-rose-500/30 rounded-xl text-rose-300 text-xs">
          {formError}
        </div>
      {/if}

      <form on:submit={handleSubmit} class="space-y-6">
        <!-- Target Selection -->
        <div>
          <label for="target" class="block text-xs font-semibold text-slate-300 uppercase tracking-wider mb-2">
            Hedef Kitle *
          </label>
          <div class="grid grid-cols-2 md:grid-cols-4 gap-3">
            <button
              type="button"
              on:click={() => (form.target = 'ALL')}
              class="p-3 rounded-xl border text-xs font-bold transition flex flex-col items-center gap-1.5 {form.target === 'ALL' ? 'bg-purple-600/30 border-purple-500 text-purple-200' : 'bg-slate-900/60 border-slate-800 text-slate-400 hover:border-slate-700'}"
            >
              <span>Tüm Kullanıcılar</span>
              <span class="text-[10px] font-normal opacity-75">ADMIN + REC + PER</span>
            </button>

            <button
              type="button"
              on:click={() => (form.target = 'ADMIN')}
              class="p-3 rounded-xl border text-xs font-bold transition flex flex-col items-center gap-1.5 {form.target === 'ADMIN' ? 'bg-purple-600/30 border-purple-500 text-purple-200' : 'bg-slate-900/60 border-slate-800 text-slate-400 hover:border-slate-700'}"
            >
              <span>Yöneticiler</span>
              <span class="text-[10px] font-normal opacity-75">Sadece ADMIN</span>
            </button>

            <button
              type="button"
              on:click={() => (form.target = 'RECEPTIONIST')}
              class="p-3 rounded-xl border text-xs font-bold transition flex flex-col items-center gap-1.5 {form.target === 'RECEPTIONIST' ? 'bg-purple-600/30 border-purple-500 text-purple-200' : 'bg-slate-900/60 border-slate-800 text-slate-400 hover:border-slate-700'}"
            >
              <span>Resepsiyonistler</span>
              <span class="text-[10px] font-normal opacity-75">Sadece RECEPTIONIST</span>
            </button>

            <button
              type="button"
              on:click={() => (form.target = 'PERSONNEL')}
              class="p-3 rounded-xl border text-xs font-bold transition flex flex-col items-center gap-1.5 {form.target === 'PERSONNEL' ? 'bg-purple-600/30 border-purple-500 text-purple-200' : 'bg-slate-900/60 border-slate-800 text-slate-400 hover:border-slate-700'}"
            >
              <span>Personeller</span>
              <span class="text-[10px] font-normal opacity-75">Sadece PERSONNEL</span>
            </button>
          </div>
        </div>

        <!-- Announcement Title -->
        <div>
          <label for="announcementTitle" class="block text-xs font-semibold text-slate-300 uppercase tracking-wider mb-2">
            Duyuru Başlığı *
          </label>
          <input
            id="announcementTitle"
            type="text"
            bind:value={form.title}
            disabled={isSubmitting}
            maxlength="100"
            placeholder="Örn: Planlı Sistem Bakımı Çalışması"
            class="vms-input"
          />
        </div>

        <!-- Announcement Message -->
        <div>
          <label for="announcementMessage" class="block text-xs font-semibold text-slate-300 uppercase tracking-wider mb-2">
            Duyuru Mesajı *
          </label>
          <textarea
            id="announcementMessage"
            bind:value={form.message}
            disabled={isSubmitting}
            rows="5"
            maxlength="1000"
            placeholder="Duyuru detaylarını ve kullanıcılara iletmek istediğiniz notları buraya yazın..."
            class="vms-input resize-none"
          ></textarea>
        </div>

        <!-- Submit Button -->
        <div class="flex justify-end pt-2">
          <button
            type="submit"
            disabled={isSubmitting}
            class="vms-btn vms-btn-primary px-8 py-3"
          >
            {#if isSubmitting}
              <div class="animate-spin w-4 h-4 border-2 border-white border-t-transparent rounded-full"></div>
              <span>Gönderiliyor...</span>
            {:else}
              <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 19l9 2-9-18-9 18 9-2zm0 0v-8"></path>
              </svg>
              <span>Duyuruyu Yayınla</span>
            {/if}
          </button>
        </div>
      </form>
    </div>
  </main>
</div>
