(() => {
    const maps = new WeakMap();
    const saoPaulo = [-23.5505, -46.6333];
    const attribution = '© <a href="https://www.openstreetmap.org/copyright">OpenStreetMap contributors</a>';
    const tileUrl = document.querySelector('meta[name="tachegando-map-tiles"]').content;

    function mount(element, onMapClick, onVendorClick) {
        element.setAttribute('aria-label', 'Mapa Tá Chegando');
        if (!window.L) {
            element.textContent = 'Mapa indisponível. Recarregue a página para tentar novamente.';
            return;
        }

        const map = L.map(element, { scrollWheelZoom: false }).setView(saoPaulo, 12);
        L.tileLayer(tileUrl, { attribution, maxZoom: 19 }).addTo(map);
        map.on('click', ({ latlng }) => onMapClick(latlng.lat, latlng.lng));
        const observer = new ResizeObserver(() => map.invalidateSize({ pan: false }));
        observer.observe(element);

        maps.set(element, {
            map,
            observer,
            onVendorClick,
            vendorMarkers: new Map(),
            otherLayers: L.layerGroup().addTo(map),
            route: L.polyline([], { color: '#ff6b00', weight: 4 }).addTo(map),
            traveled: L.polyline([], {
                color: '#7c3aed', weight: 5, opacity: 0.9, interactive: false
            }).addTo(map),
            userMarker: null,
            centered: false
        });
    }

    function vendorIcon(symbol) {
        const label = document.createElement('span');
        label.textContent = symbol || '📍';
        return L.divIcon({
            html: label,
            className: 'tachegando-map-marker',
            iconSize: [34, 34],
            iconAnchor: [17, 17]
        });
    }

    function updateVendors(state, vendors) {
        const activeIds = new Set(vendors.map(vendor => vendor.id));
        for (const [id, { marker }] of state.vendorMarkers) {
            if (!activeIds.has(id)) {
                marker.remove();
                state.vendorMarkers.delete(id);
            }
        }

        for (const vendor of vendors) {
            const existing = state.vendorMarkers.get(vendor.id);
            if (existing && existing.name === vendor.name && existing.symbol === vendor.symbol) {
                existing.marker.setLatLng(vendor.location);
                continue;
            }
            existing?.marker.remove();
            const tooltip = document.createElement('span');
            tooltip.textContent = vendor.name;
            const marker = L.marker(vendor.location, {
                icon: vendorIcon(vendor.symbol),
                title: vendor.name,
                bubblingMouseEvents: false
            }).bindTooltip(tooltip)
                .on('click', () => state.onVendorClick(vendor.id))
                .addTo(state.map);
            state.vendorMarkers.set(vendor.id, { marker, name: vendor.name, symbol: vendor.symbol });
        }
    }

    function update(element, rawData) {
        const state = maps.get(element);
        if (!state || state.rawData === rawData) return;
        state.rawData = rawData;
        const { user, vendors, route, traveled = [], locations, heatZones } = JSON.parse(rawData);
        const center = user || vendors[0]?.location || route[0];
        if (center && !state.centered) {
            state.map.setView(center, 15);
            state.centered = true;
        }

        if (user) {
            if (state.userMarker) {
                state.userMarker.setLatLng(user);
            } else {
                state.userMarker = L.circleMarker(user, {
                    color: '#fff', fillColor: '#1b2a4a', fillOpacity: 1, radius: 8, interactive: false
                }).addTo(state.map);
            }
        } else if (state.userMarker) {
            state.userMarker.remove();
            state.userMarker = null;
        }

        state.route.setLatLngs(route);
        state.traveled.setLatLngs(traveled);
        state.otherLayers.clearLayers();
        for (const location of locations) {
            L.circleMarker(location, { color: '#ff6b00', radius: 7 }).addTo(state.otherLayers);
        }
        for (const zone of heatZones) {
            L.circle(zone.location, {
                radius: zone.radius, color: '#ef476f', fillOpacity: zone.intensity * 0.6, interactive: false
            }).addTo(state.otherLayers);
        }
        updateVendors(state, vendors);
    }

    function release(element) {
        const state = maps.get(element);
        if (!state) return;
        state.observer.disconnect();
        state.map.remove();
        maps.delete(element);
    }

    window.tachegandoMap = { mount, update, release };
})();
