import { writable } from 'svelte/store';
import { toastStore } from './toastStore.js';
import {
  getNotifications,
  getUnreadNotificationCount,
  markNotificationAsRead,
  markAllNotificationsAsRead,
  deleteNotification as apiDeleteNotification
} from '../api/notificationApi.js';

function createNotificationStore() {
  const { subscribe, set, update } = writable({
    items: [],
    unreadCount: 0,
    loading: false,
    isConnected: false
  });

  let socket = null;
  let reconnectTimer = null;
  let activeToken = null;
  let loadedToken = null;

  async function loadData(force = false) {
    if (loading) return;
    if (!force && activeToken && loadedToken === activeToken) return;

    update(s => ({ ...s, loading: true }));
    try {
      const [items, unreadRes] = await Promise.all([
        getNotifications(),
        getUnreadNotificationCount()
      ]);

      loadedToken = activeToken;
      update(s => ({
        ...s,
        items: items || [],
        unreadCount: (unreadRes && typeof unreadRes.unreadCount === 'number') ? unreadRes.unreadCount : 0,
        loading: false
      }));
    } catch (err) {
      console.warn('Failed to load initial notifications:', err);
      update(s => ({ ...s, loading: false }));
    }
  }

  function connectWebSocket(token) {
    if (!token) return;
    if (activeToken === token && socket && (socket.readyState === WebSocket.OPEN || socket.readyState === WebSocket.CONNECTING)) {
      return;
    }
    activeToken = token;

    if (reconnectTimer) {
      clearTimeout(reconnectTimer);
      reconnectTimer = null;
    }

    if (socket) {
      socket.onopen = null;
      socket.onmessage = null;
      socket.onclose = null;
      socket.onerror = null;
      try { socket.close(); } catch (e) {}
      socket = null;
    }

    const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
    const wsUrl = `${protocol}//localhost:8080/ws/notifications/${encodeURIComponent(token)}`;

    try {
      const ws = new WebSocket(wsUrl);
      socket = ws;

      ws.onopen = () => {
        if (socket !== ws) return;
        update(s => ({ ...s, isConnected: true }));
        if (reconnectTimer) {
          clearTimeout(reconnectTimer);
          reconnectTimer = null;
        }
      };

      ws.onmessage = (event) => {
        if (socket !== ws) return;
        try {
          const socketMsg = JSON.parse(event.data);
          if (!socketMsg) return;

          let notificationItem;
          if (socketMsg.payload && socketMsg.payload.notification) {
            notificationItem = socketMsg.payload.notification;
          } else if (socketMsg.payload && socketMsg.payload.id) {
            notificationItem = socketMsg.payload;
          } else {
            notificationItem = {
              id: Date.now(),
              title: socketMsg.title,
              message: socketMsg.message,
              actionUrl: socketMsg.actionUrl,
              status: 'UNREAD',
              createdAt: new Date().toISOString()
            };
          }

          update(s => {
            const exists = s.items.some(item => item.id === notificationItem.id);
            const newItems = exists ? s.items : [notificationItem, ...s.items];
            const newUnread = exists ? s.unreadCount : s.unreadCount + 1;

            return {
              ...s,
              items: newItems,
              unreadCount: newUnread,
              lastSocketMsg: socketMsg
            };
          });

          // Show Toast notification to user
          if (socketMsg.type === 'SUCCESS') {
            toastStore.success(socketMsg.message || socketMsg.title);
          } else if (socketMsg.type === 'WARNING') {
            toastStore.warning(socketMsg.message || socketMsg.title);
          } else if (socketMsg.type === 'ERROR') {
            toastStore.error(socketMsg.message || socketMsg.title);
          } else {
            toastStore.info(socketMsg.message || socketMsg.title);
          }
        } catch (e) {
          console.error('Error parsing WebSocket message:', e);
        }
      };

      ws.onclose = () => {
        if (socket !== ws) return;
        update(s => ({ ...s, isConnected: false }));
        if (activeToken && !reconnectTimer) {
          reconnectTimer = setTimeout(() => {
            reconnectTimer = null;
            if (activeToken) {
              connectWebSocket(activeToken);
            }
          }, 4000);
        }
      };

      ws.onerror = (err) => {
        if (socket !== ws) return;
        console.warn('WebSocket connection error:', err);
      };
    } catch (err) {
      console.error('Failed to establish WebSocket connection:', err);
    }
  }

  function disconnect() {
    activeToken = null;
    loadedToken = null;
    if (reconnectTimer) {
      clearTimeout(reconnectTimer);
      reconnectTimer = null;
    }
    if (socket) {
      socket.close();
      socket = null;
    }
    set({ items: [], unreadCount: 0, loading: false, isConnected: false });
  }

  async function markAsRead(id) {
    try {
      await markNotificationAsRead(id);
      update(s => {
        const updatedItems = s.items.map(item => {
          if (item.id === id && item.status !== 'READ') {
            return { ...item, status: 'READ' };
          }
          return item;
        });
        const newUnread = Math.max(0, s.unreadCount - 1);
        return { ...s, items: updatedItems, unreadCount: newUnread };
      });
    } catch (err) {
      toastStore.error(err.message || 'Bildirim okundu olarak işaretlenemedi.');
    }
  }

  async function markAllAsRead() {
    try {
      await markAllNotificationsAsRead();
      update(s => {
        const updatedItems = s.items.map(item => ({ ...item, status: 'READ' }));
        return { ...s, items: updatedItems, unreadCount: 0 };
      });
      toastStore.success('Tüm bildirimler okundu olarak işaretlendi.');
    } catch (err) {
      toastStore.error(err.message || 'Bildirimler güncellenemedi.');
    }
  }

  async function removeNotification(id) {
    try {
      await apiDeleteNotification(id);
      update(s => {
        const target = s.items.find(i => i.id === id);
        const wasUnread = target && target.status === 'UNREAD';
        const filtered = s.items.filter(i => i.id !== id);
        return {
          ...s,
          items: filtered,
          unreadCount: wasUnread ? Math.max(0, s.unreadCount - 1) : s.unreadCount
        };
      });
    } catch (err) {
      toastStore.error(err.message || 'Bildirim silinemedi.');
    }
  }

  return {
    subscribe,
    loadData,
    connectWebSocket,
    disconnect,
    markAsRead,
    markAllAsRead,
    removeNotification
  };
}

export const notificationStore = createNotificationStore();
