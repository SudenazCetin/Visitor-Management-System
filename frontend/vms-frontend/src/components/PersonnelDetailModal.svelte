<script>
  import { createEventDispatcher } from 'svelte';
  import { updatePersonnel } from '../api/personnelApi.js';
  import { toastStore } from '../stores/toastStore.js';

  export var isOpen = false;
  export var personnel = null;

  const dispatch = createEventDispatcher();

  let isEditing = false;
  let isSubmitting = false;
  let editError = '';

  let editForm = {
    fullName: '',
    department: '',
    title: '',
    email: '',
  };

  $: if (personnel) {
    editForm = {
      fullName: personnel.fullName || '',
      department: personnel.department || '',
      title: personnel.title || '',
      email: personnel.email || '',
    };
    isEditing = false;
    editError = '';
  }

  function close() {
    isEditing = false;
    editError = '';
    dispatch('close');
  }

  function startEdit() {
    isEditing = true;
    editError = '';
  }

  function cancelEdit() {
    isEditing = false;
    editError = '';
    if (personnel) {
      editForm = {
        fullName: personnel.fullName || '',
        department: personnel.department || '',
        title: personnel.title || '',
        email: personnel.email || '',
      };
    }
  }

  async function handleSaveEdit(e) {
    e.preventDefault();
    editError = '';

    if (!editForm.fullName.trim()) {
      editError = 'Ad Soyad alanı zorunludur.';
      return;
    }
    if (!editForm.department.trim()) {
      editError = 'Departman alanı zorunludur.';
      return;
    }
    if (!editForm.email.trim()) {
      editError = 'E-posta alanı zorunludur.';
      return;
    }

    isSubmitting = true;
    try {
      const payload = {
        fullName: editForm.fullName.trim(),
        department: editForm.department.trim(),
        title: editForm.title.trim(),
        email: editForm.email.trim(),
      };
      const updated = await updatePersonnel(personnel.id, payload);
      toastStore.success('Personel bilgileri başarıyla güncellendi.');
      dispatch('updated', updated);
      isEditing = false;
    } catch (err) {
      editError = err.message || 'Personel güncellenirken bir hata oluştu.';
      toastStore.error(editError);
    } finally {
      isSubmitting = false;
    }
  }
</script>

{#if isOpen && personnel}
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
            {personnel.fullName ? personnel.fullName.split(' ').map(n => n[0]).join('').substring(0,2).toUpperCase() : 'P'}
          </div>
          <div>
            <h2 class="text-xl font-bold tracking-tight text-white">{personnel.fullName}</h2>
            <p class="text-xs text-purple-300 mt-0.5">{personnel.title || 'Şirket Personeli'} - {personnel.department}</p>
          </div>
        </div>
      </div>

      <!-- Body -->
      <div class="p-6 space-y-5">
        {#if editError}
          <div class="p-3.5 bg-rose-500/10 border border-rose-500/30 rounded-xl text-rose-300 text-xs flex items-center gap-2">
            <svg class="w-4 h-4 text-rose-400 shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 8v4m0 4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z"></path>
            </svg>
            <span>{editError}</span>
          </div>
        {/if}

        {#if !isEditing}
          <!-- VIEW MODE -->
          <!-- Status Banner -->
          <div class="p-4 rounded-2xl flex items-center justify-between border bg-slate-900/60 border-slate-800">
            <div class="flex items-center gap-2.5">
              <span class="w-3 h-3 rounded-full bg-emerald-400 inline-block"></span>
              <span class="font-bold text-sm text-white">Aktif Personel Kaydı</span>
            </div>

            <div class="text-xs font-semibold px-3 py-1 rounded-xl border {personnel.hasAccount ? 'bg-purple-500/20 border-purple-500/30 text-purple-300' : 'bg-slate-800 border-slate-700 text-slate-400'}">
              {personnel.hasAccount ? '🔑 Sistem Hesabı Var' : '👤 Sadece Personel'}
            </div>
          </div>

          <!-- Details Grid -->
          <div class="grid grid-cols-2 gap-4 text-sm">
            <div class="bg-slate-900/60 border border-slate-800 p-3.5 rounded-xl">
              <span class="text-xs font-semibold text-slate-400 uppercase tracking-wider block mb-1">Ad Soyad</span>
              <div class="font-bold text-white">{personnel.fullName}</div>
            </div>

            <div class="bg-slate-900/60 border border-slate-800 p-3.5 rounded-xl">
              <span class="text-xs font-semibold text-slate-400 uppercase tracking-wider block mb-1">Departman</span>
              <div class="font-semibold text-white">{personnel.department}</div>
            </div>

            <div class="bg-slate-900/60 border border-slate-800 p-3.5 rounded-xl">
              <span class="text-xs font-semibold text-slate-400 uppercase tracking-wider block mb-1">Ünvan</span>
              <div class="font-semibold text-white">{personnel.title || '-'}</div>
            </div>

            <div class="bg-slate-900/60 border border-slate-800 p-3.5 rounded-xl">
              <span class="text-xs font-semibold text-slate-400 uppercase tracking-wider block mb-1">Kullanıcı Adı</span>
              <div class="font-semibold text-purple-300 font-mono text-xs">{personnel.username || '-'}</div>
            </div>

            <div class="bg-slate-900/60 border border-slate-800 p-3.5 rounded-xl col-span-2">
              <span class="text-xs font-semibold text-slate-400 uppercase tracking-wider block mb-1">E-Posta Adresi</span>
              <div class="font-semibold text-slate-300 font-mono text-xs">{personnel.email}</div>
            </div>
          </div>
        {:else}
          <!-- EDIT MODE FORM -->
          <form on:submit={handleSaveEdit} class="space-y-4">
            <div>
              <label for="editFullName" class="block text-xs font-semibold text-slate-300 uppercase tracking-wider mb-1">
                Ad Soyad *
              </label>
              <input
                id="editFullName"
                type="text"
                bind:value={editForm.fullName}
                disabled={isSubmitting}
                class="vms-input"
              />
            </div>

            <div>
              <label for="editDepartment" class="block text-xs font-semibold text-slate-300 uppercase tracking-wider mb-1">
                Departman *
              </label>
              <input
                id="editDepartment"
                type="text"
                bind:value={editForm.department}
                disabled={isSubmitting}
                class="vms-input"
              />
            </div>

            <div>
              <label for="editTitle" class="block text-xs font-semibold text-slate-300 uppercase tracking-wider mb-1">
                Ünvan
              </label>
              <input
                id="editTitle"
                type="text"
                bind:value={editForm.title}
                disabled={isSubmitting}
                class="vms-input"
              />
            </div>

            <div>
              <label for="editEmail" class="block text-xs font-semibold text-slate-300 uppercase tracking-wider mb-1">
                E-Posta *
              </label>
              <input
                id="editEmail"
                type="email"
                bind:value={editForm.email}
                disabled={isSubmitting}
                class="vms-input"
              />
            </div>

            <div class="flex items-center justify-end gap-3 pt-3 border-t border-slate-800">
              <button
                type="button"
                on:click={cancelEdit}
                disabled={isSubmitting}
                class="vms-btn vms-btn-secondary text-xs"
              >
                İptal
              </button>
              <button
                type="submit"
                disabled={isSubmitting}
                class="vms-btn vms-btn-primary text-xs"
              >
                {#if isSubmitting}
                  <svg class="animate-spin w-4 h-4 text-white" fill="none" viewBox="0 0 24 24">
                    <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
                    <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
                  </svg>
                  <span>Kaydediliyor...</span>
                {:else}
                  <span>Değişiklikleri Kaydet</span>
                {/if}
              </button>
            </div>
          </form>
        {/if}
      </div>

      <!-- Footer Buttons -->
      {#if !isEditing}
        <div class="px-6 py-4 bg-slate-900/80 border-t border-slate-800 flex items-center justify-between">
          <button
            type="button"
            on:click={close}
            class="vms-btn vms-btn-secondary text-xs"
          >
            Kapat
          </button>

          <button
            type="button"
            on:click={startEdit}
            class="vms-btn vms-btn-primary text-xs"
          >
            <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2.828l8.586-8.586z"></path>
            </svg>
            <span>Düzenle</span>
          </button>
        </div>
      {/if}
    </div>
  </div>
{/if}
