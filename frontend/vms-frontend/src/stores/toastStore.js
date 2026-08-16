import { writable } from 'svelte/store';

function createToastStore() {
  const { subscribe, update } = writable([]);

  return {
    subscribe,

    add: (message, type = 'info', timeout = 4000) => {
      const id = Date.now() + Math.random();
      update(toasts => [...toasts, { id, message, type }]);

      if (timeout > 0) {
        setTimeout(() => {
          update(toasts => toasts.filter(t => t.id !== id));
        }, timeout);
      }
    },

    success: (message, timeout = 4000) => {
      toastStore.add(message, 'success', timeout);
    },

    error: (message, timeout = 5000) => {
      toastStore.add(message, 'error', timeout);
    },

    warning: (message, timeout = 4500) => {
      toastStore.add(message, 'warning', timeout);
    },

    info: (message, timeout = 4000) => {
      toastStore.add(message, 'info', timeout);
    },

    remove: (id) => {
      update(toasts => toasts.filter(t => t.id !== id));
    }
  };
}

export const toastStore = createToastStore();
