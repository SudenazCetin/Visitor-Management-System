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

  async function loadData() {
    update(s => ({ ...s, loading: true }));
    try {
      const [items, unreadRes] = await Promise.all([
        getNotifications(),
        getUnreadNotificationCount()
      ]);

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
    activeToken = token;

    if (socket && (socket.readyState === WebSocket.OPEN || socket.readyState === WebSocket.CONNECTING)) {
      return;
    }

    const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
    const wsUrl = `${protocol}//localhost:8080/ws/notifications/${encodeURIComponent(token)}`;

    try {
      socket = new WebSocket(wsUrl);

      socket.onopen = () => {
        update(s => ({ ...s, isConnected: true }));
        if (reconnectTimer) {
          clearTimeout(reconnectTimer);
          reconnectTimer = null;
        }
      };

      socket.onmessage = (event) => {
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

      socket.onclose = () => {
        update(s => ({ ...s, isConnected: false }));
        // Try auto reconnect after 4 seconds if token still present
        if (activeToken) {
          reconnectTimer = setTimeout(() => connectWebSocket(activeToken), 4000);
        }
      };

      socket.onerror = (err) => {
        console.warn('WebSocket connection error:', err);
        socket?.close();
      };
    } catch (err) {
      console.error('Failed to establish WebSocket connection:', err);
    }
  }

  function disconnect() {
    activeToken = null;
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
