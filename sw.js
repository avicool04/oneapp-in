// ===== OneApp Service Worker — Push + Offline Cache =====

const CACHE_NAME = 'oneapp-v1';
const CORE_ASSETS = [
  '/',
  '/index.html',
  '/manifest.json'
];

// Install — pre-cache core files
self.addEventListener('install', e => {
  self.skipWaiting();
  e.waitUntil(
    caches.open(CACHE_NAME).then(cache => cache.addAll(CORE_ASSETS)).catch(()=>{})
  );
});

// Activate — clean old caches
self.addEventListener('activate', e => {
  e.waitUntil(
    caches.keys().then(keys =>
      Promise.all(keys.filter(k => k !== CACHE_NAME).map(k => caches.delete(k)))
    ).then(() => self.clients.claim())
  );
});

// Fetch — network-first, fall back to cache when offline
self.addEventListener('fetch', event => {
  const req = event.request;
  // Only handle same-origin GET requests
  if(req.method !== 'GET' || !req.url.startsWith(self.location.origin)) return;

  event.respondWith(
    fetch(req)
      .then(res => {
        // Cache a copy of successful responses
        const copy = res.clone();
        caches.open(CACHE_NAME).then(c => c.put(req, copy)).catch(()=>{});
        return res;
      })
      .catch(() => caches.match(req))
  );
});

// Push notifications (for later — VAPID setup)
self.addEventListener('push', event => {
  let data = { title: 'OneApp alert', body: 'You have a new family alert.' };
  try { if(event.data) data = event.data.json(); } catch(_) {}
  event.waitUntil(
    self.registration.showNotification(data.title, {
      body: data.body,
      icon: '/icon-192.png',
      badge: '/icon-192.png',
      vibrate: [200,80,200,80,200],
      tag: 'oneapp-bell'
    })
  );
});

// Tap notification → focus the app
self.addEventListener('notificationclick', event => {
  event.notification.close();
  event.waitUntil(
    self.clients.matchAll({ type: 'window' }).then(cs => {
      for(const c of cs){ if('focus' in c) return c.focus(); }
      if(self.clients.openWindow) return self.clients.openWindow('/');
    })
  );
});
