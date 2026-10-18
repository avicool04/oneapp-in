// ===== OneApp Service Worker — Push + Offline Cache =====
// v4 — forces purge of old caches so share-fix is picked up immediately

const CACHE_NAME = 'oneapp-v4';
const CORE_ASSETS = [
  '/',
  '/index.html',
  '/manifest.json'
];

// Install — take control immediately and pre-cache
self.addEventListener('install', e => {
  self.skipWaiting();
  e.waitUntil(
    caches.open(CACHE_NAME).then(cache => cache.addAll(CORE_ASSETS)).catch(()=>{})
  );
});

// Activate — delete EVERY old cache, then claim all clients
self.addEventListener('activate', e => {
  e.waitUntil(
    caches.keys().then(keys =>
      Promise.all(keys.filter(k => k !== CACHE_NAME).map(k => caches.delete(k)))
    ).then(() => self.clients.claim())
  );
});

// Fetch — always try network first (so new index.html is used when online)
self.addEventListener('fetch', event => {
  const req = event.request;
  if(req.method !== 'GET' || !req.url.startsWith(self.location.origin)) return;

  event.respondWith(
    fetch(req)
      .then(res => {
        const copy = res.clone();
        caches.open(CACHE_NAME).then(c => c.put(req, copy)).catch(()=>{});
        return res;
      })
      .catch(() => caches.match(req))
  );
});

// Push notifications
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

self.addEventListener('notificationclick', event => {
  event.notification.close();
  event.waitUntil(
    self.clients.matchAll({ type: 'window' }).then(cs => {
      for(const c of cs){ if('focus' in c) return c.focus(); }
      if(self.clients.openWindow) return self.clients.openWindow('/');
    })
  );
});
