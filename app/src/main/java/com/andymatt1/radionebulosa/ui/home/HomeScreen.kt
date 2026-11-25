package com.andymatt1.radionebulosa.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.andymatt1.radionebulosa.MainViewModel
import com.andymatt1.radionebulosa.data.Category
import com.andymatt1.radionebulosa.data.Station
import com.andymatt1.radionebulosa.data.normalizedForSearch
import com.andymatt1.radionebulosa.ui.common.MiniPlayer
import com.andymatt1.radionebulosa.ui.common.StationLogo
import com.andymatt1.radionebulosa.ui.theme.Carbon
import com.andymatt1.radionebulosa.ui.theme.CarbonLight
import com.andymatt1.radionebulosa.ui.theme.MutedText
import com.andymatt1.radionebulosa.ui.theme.NeonCyan
import com.andymatt1.radionebulosa.ui.theme.NeonMagenta
import com.andymatt1.radionebulosa.ui.theme.NeonViolet
import com.andymatt1.radionebulosa.ui.theme.SpaceBlack

/** Catálogo completo: buscador, destacados, categorías, ciudades y emisoras. */
@Composable
fun HomeScreen(
    viewModel: MainViewModel,
    onOpenPlayer: () -> Unit,
    onSettings: () -> Unit
) {
    val stations by viewModel.stations.collectAsState()
    val loading by viewModel.loading.collectAsState()
    val error by viewModel.error.collectAsState()
    val favorites by viewModel.favorites.favorites.collectAsState()
    val current by viewModel.currentStation.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val favoriteStations by viewModel.favorites.favoriteStations.collectAsState()
    val focusManager = LocalFocusManager.current

    var query by rememberSaveable { mutableStateOf("") }
    var category by rememberSaveable { mutableStateOf(Category.ALL) }
    var onlyFavorites by rememberSaveable { mutableStateOf(false) }

    // Se normaliza la consulta para que "bogota" encuentre "Bogotá".
    val normalizedQuery = remember(query) { query.normalizedForSearch() }
    val filtered = remember(stations, normalizedQuery, category, onlyFavorites, favorites) {
        stations.filter { station ->
            (!onlyFavorites || station.id in favorites) &&
                category.matches(station) &&
                (normalizedQuery.isBlank() || station.searchableText.normalizedForSearch().contains(normalizedQuery))
        }
    }

    // Los favoritos se guardan completos, pero también se hidratan los IDs
    // antiguos que aún estén presentes en el catálogo actual.
    val favoriteList = remember(favorites, favoriteStations, stations) {
        val byId = favoriteStations.associateBy { it.id }
        val result = favoriteStations.filter { it.id in favorites }.toMutableList()
        favorites.forEach { id ->
            if (id !in byId) stations.firstOrNull { it.id == id }?.let(result::add)
        }
        result
    }

    val featured = remember(stations) {
        stations.filter { it.logoUrls.isNotEmpty() }.take(8)
    }
    val cities = remember(stations) {
        stations.map { it.cityName }.distinct().take(10)
    }

    Scaffold(
        containerColor = SpaceBlack,
        topBar = {
            HomeHeader(
                query = query,
                onQueryChange = { query = it },
                onSettings = onSettings,
                onSearchDone = { focusManager.clearFocus() }
            )
        },
        bottomBar = {
            current?.let { station ->
                Box(Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
                    MiniPlayer(
                        station = station,
                        isPlaying = isPlaying,
                        isFavorite = favorites.contains(station.id),
                        onTogglePlay = viewModel::togglePlayPause,
                        onFavorite = { viewModel.favorites.toggle(station) },
                        onOpen = onOpenPlayer
                    )
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
        ) {
            if (favoriteList.isNotEmpty()) {
                item {
                    SectionTitle("Mis favoritos (${favoriteList.size})")
                    FavoritesCarousel(
                        stations = favoriteList,
                        onSelect = { station ->
                            viewModel.play(station)
                            onOpenPlayer()
                        },
                        onRemove = { station -> viewModel.favorites.toggle(station) }
                    )
                }
            }

            if (featured.isNotEmpty()) {
                item {
                    SectionTitle("Emisoras destacadas de Colombia")
                    FeaturedCarousel(featured) { station ->
                        viewModel.play(station)
                        onOpenPlayer()
                    }
                }
            }

            item {
                SectionTitle("Explora por género y favoritos")
                CategoryPills(
                    selected = category,
                    onlyFavorites = onlyFavorites,
                    hasFavorites = favorites.isNotEmpty(),
                    onFavoritesToggle = { favoritesSelected ->
                        onlyFavorites = favoritesSelected
                    },
                    onSelect = { selectedCategory ->
                        category = selectedCategory
                    }
                )
            }

            if (cities.isNotEmpty()) {
                item {
                    SectionTitle("Ciudades")
                    CityChips(cities, query) { query = it }
                }
            }

            item {
                SectionTitle(
                    when {
                        onlyFavorites -> "Emisoras favoritas (${filtered.size})"
                        category == Category.ALL && query.isBlank() -> "Todas las emisoras (${filtered.size})"
                        else -> "Resultados (${filtered.size})"
                    }
                )
            }

            items(filtered, key = { it.id + it.streamUrl }) { station ->
                StationCard(
                    station = station,
                    isFavorite = favorites.contains(station.id),
                    onPlay = {
                        viewModel.play(station)
                        onOpenPlayer()
                    },
                    onFavorite = { viewModel.favorites.toggle(station) }
                )
            }

            if (loading) {
                item {
                    Box(
                        Modifier.fillMaxWidth().padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = NeonViolet)
                    }
                }
            }

            if (error != null) {
                item {
                    Card(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        colors = CardDefaults.cardColors(containerColor = CarbonLight),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(Modifier.padding(18.dp)) {
                            Text(
                                text = error.orEmpty(),
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFFFFB4B4)
                            )
                            Spacer(Modifier.height(10.dp))
                            Button(onClick = viewModel::loadStations) {
                                Text("Reintentar")
                            }
                        }
                    }
                }
            }

            item { Spacer(Modifier.height(20.dp)) }
        }
    }
}

@Composable
private fun HomeHeader(
    query: String,
    onQueryChange: (String) -> Unit,
    onSettings: () -> Unit,
    onSearchDone: () -> Unit
) {
    Surface(color = SpaceBlack) {
        Column(Modifier.padding(start = 16.dp, end = 10.dp, top = 14.dp, bottom = 10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Brush.linearGradient(listOf(NeonViolet, NeonCyan))),
                    contentAlignment = Alignment.Center
                ) {
                    Text("RN", color = Color.White, style = MaterialTheme.typography.labelMedium)
                }
                Spacer(Modifier.width(10.dp))
                Text(
                    text = "Radio Nebulosa",
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onSettings) {
                    Icon(Icons.Filled.Settings, contentDescription = "Ajustes", tint = NeonViolet)
                }
            }

            Spacer(Modifier.height(10.dp))

            OutlinedTextField(
                value = query,
                onValueChange = onQueryChange,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                placeholder = { Text("Buscar emisora, ciudad o género", color = MutedText) },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null, tint = MutedText) },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { onQueryChange("") }) {
                            Icon(
                                Icons.Filled.Close,
                                contentDescription = "Limpiar búsqueda",
                                tint = MutedText
                            )
                        }
                    }
                },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { onSearchDone() }),
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = NeonViolet,
                    unfocusedBorderColor = Color(0xFF2C2C3A),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    cursorColor = NeonCyan,
                    focusedContainerColor = Carbon,
                    unfocusedContainerColor = Carbon
                )
            )
        }
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleLarge,
        color = Color.White,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 18.dp, bottom = 10.dp)
    )
}

/** Carrusel de emisoras guardadas por el usuario, con acceso rápido para quitarlas. */
@Composable
private fun FavoritesCarousel(
    stations: List<Station>,
    onSelect: (Station) -> Unit,
    onRemove: (Station) -> Unit
) {
    LazyRow(
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(stations, key = { "favorite_${it.id}" }) { station ->
            Card(
                modifier = Modifier
                    .width(220.dp)
                    .clickable { onSelect(station) },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = CarbonLight)
            ) {
                Row(
                    Modifier.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    StationLogo(station, size = 44.dp, cornerPercent = 12)
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = station.displayName,
                        style = MaterialTheme.typography.titleSmall,
                        color = Color.White,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = { onRemove(station) }) {
                        Icon(
                            imageVector = Icons.Filled.Favorite,
                            contentDescription = "Quitar de favoritos",
                            tint = NeonMagenta,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FeaturedCarousel(
    stations: List<Station>,
    onSelect: (Station) -> Unit
) {
    val context = LocalContext.current
    LazyRow(
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(stations, key = { "featured_${it.id}" }) { station ->
            Card(
                modifier = Modifier
                    .width(236.dp)
                    .height(132.dp)
                    .clickable { onSelect(station) },
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = CarbonLight)
            ) {
                Box(Modifier.fillMaxSize()) {
                    station.logoUrls.firstOrNull()?.let { logoUrl ->
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(logoUrl)
                                .crossfade(true)
                                .build(),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize().alpha(0.38f)
                        )
                    }
                    Box(
                        Modifier.fillMaxSize().background(
                            Brush.verticalGradient(
                                listOf(Color.Transparent, SpaceBlack.copy(alpha = 0.96f))
                            )
                        )
                    )
                    Row(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(12.dp),
                        verticalAlignment = Alignment.Bottom
                    ) {
                        StationLogo(station, size = 48.dp, cornerPercent = 12)
                        Spacer(Modifier.width(10.dp))
                        Text(
                            text = station.displayName,
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Text(
                        text = "EN VIVO",
                        style = MaterialTheme.typography.labelMedium,
                        color = NeonCyan,
                        modifier = Modifier.padding(12.dp).align(Alignment.TopEnd)
                    )
                }
            }
        }
    }
}

@Composable
private fun CategoryPills(
    selected: Category,
    onlyFavorites: Boolean,
    hasFavorites: Boolean,
    onFavoritesToggle: (Boolean) -> Unit,
    onSelect: (Category) -> Unit
) {
    LazyRow(
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            FilterChip(
                selected = onlyFavorites,
                enabled = hasFavorites || onlyFavorites,
                onClick = { onFavoritesToggle(!onlyFavorites) },
                label = { Text("♥ Favoritos") },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = NeonMagenta.copy(alpha = 0.28f),
                    selectedLabelColor = NeonMagenta,
                    containerColor = CarbonLight,
                    labelColor = Color(0xFFC6C6D6)
                )
            )
        }
        items(Category.entries) { category ->
            FilterChip(
                selected = category == selected && !onlyFavorites,
                onClick = { onSelect(category) },
                label = { Text(category.label) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = NeonViolet.copy(alpha = 0.28f),
                    selectedLabelColor = NeonCyan,
                    containerColor = CarbonLight,
                    labelColor = Color(0xFFC6C6D6)
                )
            )
        }
    }
}

@Composable
private fun CityChips(cities: List<String>, query: String, onSelect: (String) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        cities.forEach { city ->
            val isSelected = city.normalizedForSearch() == query.normalizedForSearch()
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = if (isSelected) NeonViolet.copy(alpha = 0.3f) else CarbonLight,
                modifier = Modifier.clickable {
                    onSelect(if (isSelected) "" else city)
                }
            ) {
                Text(
                    text = city,
                    color = if (isSelected) NeonCyan else Color(0xFFC6C6D6),
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                )
            }
        }
    }
}

@Composable
private fun StationCard(
    station: Station,
    isFavorite: Boolean,
    onPlay: () -> Unit,
    onFavorite: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 5.dp)
            .clickable(onClick = onPlay),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Carbon)
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            StationLogo(station = station, size = 58.dp, cornerPercent = 14)

            Spacer(Modifier.width(12.dp))

            Column(Modifier.weight(1f)) {
                Text(
                    text = station.displayName,
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Filled.LocationOn,
                        contentDescription = null,
                        tint = NeonCyan,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = station.cityName,
                        style = MaterialTheme.typography.labelMedium,
                        color = Color(0xFFC6C6D6),
                        maxLines = 1
                    )
                }
                Spacer(Modifier.height(3.dp))
                Text(
                    text = station.tagList.take(2).joinToString(" • ").ifBlank { "Radio en vivo" },
                    style = MaterialTheme.typography.labelMedium,
                    color = MutedText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            IconButton(onClick = onFavorite) {
                Icon(
                    imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                    contentDescription = if (isFavorite) "Quitar de favoritos" else "Agregar a favoritos",
                    tint = if (isFavorite) NeonMagenta else MutedText
                )
            }
        }
    }
}
