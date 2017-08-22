var CACHE_NAME = 'yalp-store-crashes-cache-v1';
var urlsToCache = [
  '/',
  '/index.html',
  '/react-components.js',
  '/yalp-store-192.png'
];
self.addEventListener('install', function(event) {
  event.waitUntil(
    caches.open(CACHE_NAME)
      .then(function(cache) {
        return cache.addAll(urlsToCache);
      })
  );
});

self.addEventListener('fetch', function(event) {
  event.respondWith(
    caches.match(event.request).then(function(response) {
      return response || fetch(event.request);
    })
  );
});