package com.andymatt1.radionebulosa.ui.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.andymatt1.radionebulosa.MainViewModel
import com.andymatt1.radionebulosa.R
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

/**
 * Catálogo con estética de plataforma de TV: cabecera con pestañas, banner
 * protagonista, carrusel de canales en vivo y guía completa de emisoras.
 */
@Composable
fun HomeScreen(
    viewModel: MainViewModel,
    onOpenPlayer: () -> Unit,
    onSettings: () -> Unit
) {
    val stations by viewModel.stations.collectAsState()
    val loading by viewModel.loading.collectAsState()
    val refreshing by viewModel.refreshing.collectAsState()
    val error by viewModel.error.collectAsState()
    val favorites by viewModel.favorites.favorites.collectAsState()
    val current by viewModel.currentStation.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val favoriteStations by viewModel.favorites.favoriteStations.collectAsState()
    val focusManager = LocalFocusManager.current

    var query by rememberSaveable { mutableStateOf("") }
    var category by rememberSaveable { mutableStateOf(Category.ALL) }
    var onlyFavorites by rememberSaveable { mutableStateOf(false) }

    val normalizedQuery = remember(query) { query.normalizedForSearch() }

    // El índice se calcula una sola vez por catálogo, no en cada pulsación.
    val searchIndex = remember(stations) {
        stations.associateWith { it.searchableText.normalizedForSearch() }
    }

    val filtered = remember(stations, normalizedQuery, category, onlyFavorites, favorites, searchIndex) {
        stations.filter { station ->
            (!onlyFavorites || station.id in favorites) &&
                category.matches(station) &&
                (normalizedQuery.isBlank() || searchIndex[station].orEmpty().contains(normalizedQuery))
        }
    }

    val favoriteList = remember(favorites, favoriteStations, stations) {
        val byId = favoriteStations.associateBy { it.id }
        val result = favoriteStations.filter { it.id in favorites }.toMutableList()
        favorites.forEach { id ->
            if (id !in byId) stations.firstOrNull { it.id == id }?.let(result::add)
        }
        result
    }

    val channels = remember(stations) { stations.sortedByDescending { it.votes }.take(24) }
    val cities = remember(stations) { stations.map { it.cityName }.distinct().take(12) }
    val hero = remember(stations, favoriteList) {
        (favoriteList + stations).firstOrNull { it.logoUrls.isNotEmpty() } ?: stations.firstOrNull()
    }

    val play: (Station) -> Unit = { station ->
        viewModel.play(station)
        onOpenPlayer()
    }

    Box(Modifier.fillMaxSize().background(SpaceBlack)) {
        Image(
            painter = painterResource(R.drawable.nebulosa_mist),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize().blur(36.dp).alpha(0.22f)
        )
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(listOf(Color(0x2A2A0B45), SpaceBlack))
            )
        )

        Scaffold(
            containerColor = Color.Transparent,
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
            item {
                TvHeader(
                    query = query,
                    onQueryChange = { query = it },
                    onlyFavorites = onlyFavorites,
                    onFavoritesChange = { onlyFavorites = it },
                    onSettings = onSettings,
                    onSearchDone = { focusManager.clearFocus() }
                )
            }

            if (refreshing) {
                item {
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth().height(2.dp),
                        color = NeonCyan,
                        trackColor = Color.Transparent
                    )
                }
            }

            if (hero != null) {
                item {
                    HeroBanner(
                        station = hero,
                        isFavorite = favorites.contains(hero.id),
                        onPlay = { play(hero) },
                        onFavorite = { viewModel.favorites.toggle(hero) }
                    )
                }
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

            if (error != null && stations.isEmpty()) {
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

            if (favoriteList.isNotEmpty()) {
                item {
                    TvSectionTitle("Mi lista (${favoriteList.size})")
                    FavoriteRail(
                        stations = favoriteList,
                        onPlay = play,
                        onFavorite = viewModel.favorites::toggle
                    )
                }
            }

            if (channels.isNotEmpty()) {
                item {
                    TvSectionTitle("Canales en vivo")
                    ChannelRail(channels = channels, onPlay = play)
                }
            }

            item {
                TvSectionTitle("Explora por género")
                CategoryPills(
                    selected = category,
                    onlyFavorites = onlyFavorites,
                    hasFavorites = favorites.isNotEmpty(),
                    onFavoritesToggle = { onlyFavorites = it },
                    onSelect = { category = it }
                )
            }

            if (cities.isNotEmpty()) {
                item {
                    TvSectionTitle("Ciudades")
                    CityChips(cities = cities, query = query) { query = it }
                }
            }

            item {
                TvSectionTitle(
                    when {
                        onlyFavorites -> "Mis favoritas (${filtered.size})"
                        category == Category.ALL && query.isBlank() -> "Todas las emisoras (${filtered.size})"
                        else -> "Resultados (${filtered.size})"
                    }
                )
            }

            if (filtered.isEmpty() && !loading && error == null) {
                item {
                    Text(
                        text = "No hay emisoras con esos filtros. Prueba otra ciudad o género.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MutedText,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 18.dp)
                    )
                }
            }

            itemsIndexed(filtered, key = { _, station -> station.id + station.streamUrl }) { index, station ->
                TvStationRow(
                    number = index + 1,
                    station = station,
                    isFavorite = favorites.contains(station.id),
                    onPlay = { play(station) },
                    onFavorite = { viewModel.favorites.toggle(station) }
                )
            }

            item { Spacer(Modifier.height(24.dp)) }
        }
        }
    }
}

@Composable
private fun TvHeader(
    query: String,
    onQueryChange: (String) -> Unit,
    onlyFavorites: Boolean,
    onFavoritesChange: (Boolean) -> Unit,
    onSettings: () -> Unit,
    onSearchDone: () -> Unit
) {
    var searchOpen by rememberSaveable { mutableStateOf(query.isNotBlank()) }

    Surface(color = Color(0xFF07070D)) {
        Column(Modifier.padding(start = 16.dp, end = 10.dp, top = 16.dp, bottom = 8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Brush.linearGradient(listOf(NeonViolet, NeonCyan))),
                    contentAlignment = Alignment.Center
                ) {
                    Text("RN", color = Color.White, style = MaterialTheme.typography.titleMedium)
                }
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = "NEBULOSA MOD",
                        style = MaterialTheme.typography.labelMedium,
                        color = NeonCyan
                    )
                    Text(
                        text = "En vivo • Colombia",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White
                    )
                }
                IconButton(onClick = { searchOpen = !searchOpen }) {
                    Icon(Icons.Filled.Search, contentDescription = "Buscar", tint = Color.White)
                }
                IconButton(onClick = onSettings) {
                    Icon(Icons.Filled.Settings, contentDescription = "Ajustes", tint = MutedText)
                }
            }

            Spacer(Modifier.height(12.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                TvTab(label = "Inicio", selected = !onlyFavorites) { onFavoritesChange(false) }
                TvTab(label = "Mi lista", selected = onlyFavorites) { onFavoritesChange(true) }
            }

            if (searchOpen) {
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    placeholder = { Text("Buscar emisora, ciudad o género", color = MutedText) },
                    leadingIcon = {
                        Icon(Icons.Filled.Search, contentDescription = null, tint = MutedText)
                    },
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
}

@Composable
private fun TvTab(label: String, selected: Boolean, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium,
            color = if (selected) Color.White else MutedText
        )
        Spacer(Modifier.height(6.dp))
        Box(
            Modifier
                .width(if (selected) 28.dp else 0.dp)
                .height(2.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(NeonCyan)
        )
    }
}

@Composable
private fun TvSectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleLarge,
        color = Color.White,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 10.dp)
    )
}

@Composable
private fun HeroBanner(
    station: Station,
    isFavorite: Boolean,
    onPlay: () -> Unit,
    onFavorite: () -> Unit
) {
    Box(
        modifier = Modifier
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .fillMaxWidth()
            .height(228.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(Brush.linearGradient(listOf(NeonViolet, NeonCyan)))
            .clickable(onClick = onPlay)
    ) {
        Image(
            painter = painterResource(R.drawable.nebulosa_mist),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize().blur(12.dp).alpha(0.85f)
        )
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(
                    listOf(Color(0x99050508), Color(0xF0050508))
                )
            )
        )

        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(18.dp)
        ) {
            StationLogo(station = station, size = 44.dp, cornerPercent = 12)
            Spacer(Modifier.height(10.dp))
            Surface(shape = RoundedCornerShape(20.dp), color = Color(0x33FFFFFF)) {
                Text(
                    text = "● EN VIVO",
                    style = MaterialTheme.typography.labelMedium,
                    color = NeonCyan,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = station.displayName,
                style = MaterialTheme.typography.headlineMedium,
                color = Color.White,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = listOf(station.cityName)
                    .plus(station.tagList.take(2))
                    .filter { it.isNotBlank() }
                    .joinToString(" • ")
                    .ifBlank { "Radio en vivo" },
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFFD8D8E6),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(14.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Button(
                    onClick = onPlay,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor = SpaceBlack
                    )
                ) {
                    Icon(Icons.Filled.PlayArrow, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("Reproducir")
                }
                Spacer(Modifier.width(8.dp))
                IconButton(onClick = onFavorite) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                        contentDescription = if (isFavorite) "Quitar de favoritos" else "Agregar a favoritos",
                        tint = if (isFavorite) NeonMagenta else Color.White
                    )
                }
            }
        }
    }
}

@Composable
private fun ChannelRail(channels: List<Station>, onPlay: (Station) -> Unit) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        itemsIndexed(channels, key = { _, station -> "channel_${station.id}" }) { index, station ->
            TvChannelCard(
                channelNumber = index + 1,
                station = station,
                isFavorite = false,
                onPlay = { onPlay(station) },
                onFavorite = null
            )
        }
    }
}

@Composable
private fun FavoriteRail(
    stations: List<Station>,
    onPlay: (Station) -> Unit,
    onFavorite: (Station) -> Unit
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(stations, key = { "favorite_${it.id}" }) { station ->
            TvChannelCard(
                channelNumber = null,
                station = station,
                isFavorite = true,
                onPlay = { onPlay(station) },
                onFavorite = { onFavorite(station) }
            )
        }
    }
}

@Composable
private fun TvChannelCard(
    channelNumber: Int?,
    station: Station,
    isFavorite: Boolean,
    onPlay: () -> Unit,
    onFavorite: (() -> Unit)?
) {
    Card(
        modifier = Modifier
            .width(176.dp)
            .height(108.dp)
            .clickable(onClick = onPlay),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = CarbonLight)
    ) {
        Box(Modifier.fillMaxSize()) {
            station.logoUrls.firstOrNull()?.let { logoUrl ->
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(logoUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize().blur(14.dp).alpha(0.35f)
                )
            }
            Box(
                Modifier.fillMaxSize().background(
                    Brush.verticalGradient(
                        listOf(Color(0x66050508), Color(0xE6050508))
                    )
                )
            )

            if (channelNumber != null) {
                Text(
                    text = "CH %02d".format(channelNumber),
                    style = MaterialTheme.typography.labelMedium,
                    color = NeonCyan,
                    modifier = Modifier.padding(10.dp).align(Alignment.TopStart)
                )
            }

            if (onFavorite != null) {
                IconButton(
                    onClick = onFavorite,
                    modifier = Modifier.size(32.dp).align(Alignment.TopEnd)
                ) {
                    Icon(
                        Icons.Filled.Favorite,
                        contentDescription = "Quitar de favoritos",
                        tint = NeonMagenta,
                        modifier = Modifier.size(18.dp)
                    )
                }
            } else {
                Text(
                    text = "● LIVE",
                    style = MaterialTheme.typography.labelMedium,
                    color = NeonMagenta,
                    modifier = Modifier.padding(10.dp).align(Alignment.TopEnd)
                )
            }

            Row(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                StationLogo(station, size = 38.dp, cornerPercent = 10)
                Spacer(Modifier.width(8.dp))
                Text(
                    text = station.displayName,
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
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
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            TvPill(
                label = "♥ Mi lista",
                active = onlyFavorites,
                enabled = hasFavorites || onlyFavorites
            ) { onFavoritesToggle(!onlyFavorites) }
        }
        items(Category.entries) { category ->
            TvPill(
                label = category.label,
                active = category == selected && !onlyFavorites
            ) { onSelect(category) }
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
            TvPill(label = city, active = isSelected) {
                onSelect(if (isSelected) "" else city)
            }
        }
    }
}

@Composable
private fun TvPill(
    label: String,
    active: Boolean,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    val borderColor = if (active) NeonViolet else Color(0xFF2A2A38)
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = if (active) NeonViolet.copy(alpha = 0.28f) else CarbonLight,
        modifier = Modifier
            .border(1.dp, borderColor, RoundedCornerShape(20.dp))
            .alpha(if (enabled) 1f else 0.45f)
            .clickable(enabled = enabled, onClick = onClick)
    ) {
        Text(
            text = label,
            color = if (active) NeonCyan else Color(0xFFC6C6D6),
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
        )
    }
}

@Composable
private fun TvStationRow(
    number: Int,
    station: Station,
    isFavorite: Boolean,
    onPlay: () -> Unit,
    onFavorite: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clickable(onClick = onPlay),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Carbon)
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "%02d".format(number),
                style = MaterialTheme.typography.labelMedium,
                color = NeonCyan,
                modifier = Modifier.width(26.dp)
            )
            StationLogo(station = station, size = 52.dp, cornerPercent = 12)
            Spacer(Modifier.width(12.dp))

            Column(Modifier.weight(1f)) {
                Text(
                    text = station.displayName,
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(3.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Filled.LocationOn,
                        contentDescription = null,
                        tint = NeonCyan,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = station.cityName,
                        style = MaterialTheme.typography.labelMedium,
                        color = Color(0xFFC6C6D6),
                        maxLines = 1
                    )
                }
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
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(NeonViolet)
                    .clickable(onClick = onPlay),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Filled.PlayArrow,
                    contentDescription = "Reproducir",
                    tint = Color.White
                )
            }
        }
    }
}
