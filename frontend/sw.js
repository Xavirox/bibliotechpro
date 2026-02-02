/**
 * BiblioTech Pro - Service Worker
 * Enables offline functionality and PWA features
 * OPTIMIZADO: v2.1.0 - Mejor gestión de caché y rendimiento
 */

const CACHE_NAME = 'bibliotech-pro-v2.2.0';
const RUNTIME_CACHE = 'bibliotech-runtime-v2.2';

// Assets to cache on install
const PRECACHE_ASSETS = [
    '/',
    '/index.html',
    '/css/styles.css',
    '/css/components.css',
    '/css/visuals.css',
    '/js/main.js',
    '/js/auth.js',
    '/js/catalog.js',
    '/js/user.js',
    '/js/librarian.js',
    '/js/effects.js',
    '/js/utils.js',
    '/js/config.js',
    '/js/api.js',
    '/js/constants.js',
    '/js/sounds.js',
    '/manifest.json'
];

// Install event - cache static assets
self.addEventListener('install', event => {
    console.log('[SW] Installing BiblioTech Pro Service Worker...');

    event.waitUntil(
        caches.open(CACHE_NAME)
            .then(cache => {
                console.log('[SW] Pre-caching static assets');
                return cache.addAll(PRECACHE_ASSETS);
            })
            .then(() => {
                console.log('[SW] Installation complete');
                return self.skipWaiting();
            })
            .catch(err => {
                console.error('[SW] Pre-cache failed:', err);
            })
    );
});

// Activate event - clean up old caches
self.addEventListener('activate', event => {
    console.log('[SW] Activating new service worker...');

    event.waitUntil(
        caches.keys()
            .then(cacheNames => {
                return Promise.all(
                    cacheNames
                        .filter(name => name !== CACHE_NAME && name !== RUNTIME_CACHE)
                        .map(name => {
                            console.log('[SW] Deleting old cache:', name);
                            return caches.delete(name);
                        })
                );
            })
            .then(() => {
                console.log('[SW] Claiming clients');
                return self.clients.claim();
            })
    );
});

// Fetch event - serve from cache with network fallback
self.addEventListener('fetch', event => {
    const { request } = event;
    const url = new URL(request.url);

    // Skip non-GET requests
    if (request.method !== 'GET') {
        return;
    }

    // Skip API requests - always go to network
    if (url.pathname.startsWith('/api/')) {
        event.respondWith(
            fetch(request)
                .catch(() => {
                    return new Response(
                        JSON.stringify({ error: 'Offline - No hay conexión a internet' }),
                        {
                            status: 503,
                            headers: { 'Content-Type': 'application/json' }
                        }
                    );
                })
        );
        return;
    }

    // Skip external resources
    if (url.origin !== location.origin) {
        return;
    }

    // For HTML pages - network first, cache fallback
    if (request.headers.get('Accept')?.includes('text/html')) {
        event.respondWith(
            fetch(request)
                .then(response => {
                    // Clone and cache the response
                    const clone = response.clone();
                    caches.open(RUNTIME_CACHE).then(cache => cache.put(request, clone));
                    return response;
                })
                .catch(() => {
                    return caches.match(request)
                        .then(cached => cached || caches.match('/'));
                })
        );
        return;
    }

    // For other assets - cache first, network fallback
    event.respondWith(
        caches.match(request)
            .then(cached => {
                if (cached) {
                    // Return cached version and update cache in background
                    event.waitUntil(
                        fetch(request)
                            .then(response => {
                                caches.open(RUNTIME_CACHE)
                                    .then(cache => cache.put(request, response));
                            })
                            .catch(() => { })
                    );
                    return cached;
                }

                // Not in cache - fetch from network
                return fetch(request)
                    .then(response => {
                        // Cache successful responses
                        if (response.ok) {
                            const clone = response.clone();
                            caches.open(RUNTIME_CACHE)
                                .then(cache => cache.put(request, clone));
                        }
                        return response;
                    });
            })
    );
});

// Background sync for offline actions
self.addEventListener('sync', event => {
    console.log('[SW] Background sync:', event.tag);

    if (event.tag === 'sync-reservations') {
        event.waitUntil(syncReservations());
    }
});

async function syncReservations() {
    // Get pending actions from IndexedDB
    // This would sync any offline reservation attempts
    console.log('[SW] Syncing pending reservations...');
}

// Push notifications (if implemented)
self.addEventListener('push', event => {
    const data = event.data?.json() || {};

    const options = {
        body: data.body || 'Tienes una notificación de BiblioTech',
        icon: '/favicon.ico',
        badge: '/favicon.ico',
        vibrate: [100, 50, 100],
        data: {
            url: data.url || '/'
        },
        actions: [
            { action: 'open', title: 'Ver' },
            { action: 'close', title: 'Cerrar' }
        ]
    };

    event.waitUntil(
        self.registration.showNotification(
            data.title || 'BiblioTech Pro',
            options
        )
    );
});

// Handle notification clicks
self.addEventListener('notificationclick', event => {
    event.notification.close();

    if (event.action === 'close') {
        return;
    }

    event.waitUntil(
        clients.matchAll({ type: 'window' })
            .then(windowClients => {
                // Check if there's already a window open
                for (const client of windowClients) {
                    if (client.url === event.notification.data.url && 'focus' in client) {
                        return client.focus();
                    }
                }
                // Open new window
                return clients.openWindow(event.notification.data.url);
            })
    );
});

// Skip waiting message from main thread
self.addEventListener('message', event => {
    if (event.data === 'SKIP_WAITING') {
        self.skipWaiting();
    }
});

console.log('[SW] BiblioTech Pro Service Worker loaded');
