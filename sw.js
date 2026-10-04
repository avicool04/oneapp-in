self.addEventListener('install', e => self.skipWaiting());
self.addEventListener('activate', e => e.waitUntil(self.clients.claim()));

// Real Web Push (needs a push service + VAPID — set up later if desired)
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
