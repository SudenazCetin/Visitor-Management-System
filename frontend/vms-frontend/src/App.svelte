<script>
  import { API_BASE_URL } from './api/client.js';
</script>

<main class="min-h-screen bg-slate-950 text-slate-100 flex flex-col items-center justify-center p-6 relative overflow-hidden">
  <!-- Dynamic background glow -->
  <div class="absolute top-1/4 left-1/2 -translate-x-1/2 -translate-y-1/2 w-96 h-96 bg-sky-500/20 rounded-full blur-3xl pointer-events-none"></div>

  <!-- Content card -->
  <div class="max-w-xl w-full bg-slate-900/80 backdrop-blur-xl border border-slate-800 rounded-2xl p-8 shadow-2xl text-center space-y-6 z-10">
    <div class="inline-flex items-center justify-center p-3 bg-sky-500/10 border border-sky-500/20 rounded-xl text-sky-400 mb-2">
      <svg class="w-8 h-8" fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M17 20h5v-2a3 3 0 00-5.356-1.857M17 20H7m10 0v-2c0-.656-.126-1.283-.356-1.857M7 20H2v-2a3 3 0 015.356-1.857M7 20v-2c0-.656.126-1.283.356-1.857m0 0a5.002 5.002 0 019.288 0M15 7a3 3 0 11-6 0 3 3 0 016 0zm6 3a2 2 0 11-4 0 2 2 0 014 0zM7 10a2 2 0 11-4 0 2 2 0 014 0z"></path>
      </svg>
    </div>

    <h1 class="text-3xl font-bold tracking-tight bg-gradient-to-r from-sky-400 to-blue-500 bg-clip-text text-transparent">
      Visitor Management System
    </h1>

    <p class="text-slate-400 text-sm leading-relaxed">
      Kurumsal Ziyaretçi ve Personel Yönetim Sistemi frontend altyapısı başarıyla kuruldu.
    </p>

    <div class="pt-4 border-t border-slate-800/80 flex items-center justify-between text-xs text-slate-500">
      <span>Frontend: <strong class="text-slate-300">Svelte 4 + Tailwind CSS</strong></span>
      <span>Backend Endpoint: <code class="text-sky-400 font-mono">{API_BASE_URL}</code></span>
    </div>
  </div>
</main>
