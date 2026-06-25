package com.personax.mobile.ui.screens

import android.content.Intent
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import com.personax.mobile.data.*
import com.personax.mobile.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen() {
    val context = LocalContext.current
    val store = remember { ProfileStore(context) }
    var profiles by remember { mutableStateOf(store.getProfiles()) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf<MobileProfile?>(null) }
    var showFingerprintDialog by remember { mutableStateOf<MobileProfile?>(null) }
    var proxyPool by remember { mutableStateOf(store.getProxyPool().toList()) }
    var proxyLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            val remote = store.fetchRemoteProxies()
            withContext(Dispatchers.Main) {
                proxyPool = remote
                proxyLoading = false
            }
        }
    }

    fun refresh() { profiles = store.getProfiles() }

    Scaffold(
        containerColor = BgDark,
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Brush.linearGradient(listOf(Accent, AccentDark))),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("PX", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text("PersonaX", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            Text("MOBILE", color = Accent, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BgCard),
                actions = {
                    Text(
                        "${profiles.size} profiles",
                        color = TextMuted,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showCreateDialog = true },
                containerColor = Accent,
                contentColor = Color.White,
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Default.Add, "Create")
                Spacer(Modifier.width(8.dp))
                Text("New Profile", fontWeight = FontWeight.SemiBold)
            }
        }
    ) { padding ->
        if (profiles.isEmpty()) {
            EmptyState(
                modifier = Modifier.padding(padding).fillMaxSize(),
                onCreate = { showCreateDialog = true }
            )
        } else {
            LazyColumn(
                modifier = Modifier.padding(padding).fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item { StatsBar(profiles, proxyPool.size) }
                items(profiles, key = { it.id }) { profile ->
                    ProfileCard(
                        profile = profile,
                        onActivate = {
                            store.setActiveProfile(profile.id)
                            refresh()
                        },
                        onBrowse = {
                            store.setActiveProfile(profile.id)
                            refresh()
                            val intent = Intent(context, BrowserActivity::class.java)
                            intent.putExtra("profile_id", profile.id)
                            context.startActivity(intent)
                        },
                        onEdit = { showEditDialog = profile },
                        onViewFingerprint = { showFingerprintDialog = profile },
                        onRegenerate = {
                            val updated = DeviceDatabase.regenerateFingerprint(profile)
                            store.updateProfile(updated)
                            refresh()
                        },
                        onDelete = {
                            store.deleteProfile(profile.id)
                            refresh()
                        }
                    )
                }
                item { Spacer(Modifier.height(80.dp)) }
            }
        }
    }

    if (showCreateDialog) {
        CreateProfileDialog(
            proxyPool = proxyPool,
            onDismiss = { showCreateDialog = false },
            onCreate = { name, count, proxyMode, selectedProxy ->
                val usedProxies = profiles.mapNotNull { it.proxy.ifEmpty { null } }.toSet()
                val available = proxyPool.filter { it !in usedProxies }.toMutableList()
                repeat(count) { i ->
                    val n = if (count > 1) "$name ${i + 1}" else name
                    val profile = DeviceDatabase.generateProfile(n)
                    val proxy = when (proxyMode) {
                        "auto" -> if (available.isNotEmpty()) available.removeAt(0) else ""
                        "manual" -> selectedProxy
                        else -> ""
                    }
                    store.addProfile(profile.copy(proxy = proxy))
                }
                refresh()
                showCreateDialog = false
            }
        )
    }

    showEditDialog?.let { profile ->
        EditProfileDialog(
            profile = profile,
            onDismiss = { showEditDialog = null },
            onSave = { updated ->
                store.updateProfile(updated)
                refresh()
                showEditDialog = null
            }
        )
    }

    showFingerprintDialog?.let { profile ->
        FingerprintDialog(
            profile = profile,
            onDismiss = { showFingerprintDialog = null }
        )
    }
}

@Composable
fun StatsBar(profiles: List<MobileProfile>, proxyCount: Int) {
    val active = profiles.count { it.isActive }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(BgCard)
            .padding(14.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        StatItem("Profiles", profiles.size.toString(), Accent)
        StatItem("Active", active.toString(), AccentLight)
        StatItem("Proxies", proxyCount.toString(), Warning)
        StatItem("Devices", "15", Info)
    }
}

@Composable
fun StatItem(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, color = color, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Text(label, color = TextMuted, fontSize = 10.sp)
    }
}

@Composable
fun EmptyState(modifier: Modifier, onCreate: () -> Unit) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("📱", fontSize = 56.sp)
        Spacer(Modifier.height(16.dp))
        Text("No Profiles Yet", color = TextSecondary, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(8.dp))
        Text(
            "Create your first mobile profile with a unique device identity",
            color = TextMuted, fontSize = 14.sp
        )
        Spacer(Modifier.height(24.dp))
        Button(
            onClick = onCreate,
            colors = ButtonDefaults.buttonColors(containerColor = Accent),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Default.Add, null)
            Spacer(Modifier.width(8.dp))
            Text("Create Profile")
        }
    }
}

@Composable
fun ProfileCard(
    profile: MobileProfile,
    onActivate: () -> Unit,
    onBrowse: () -> Unit,
    onEdit: () -> Unit,
    onViewFingerprint: () -> Unit,
    onRegenerate: () -> Unit,
    onDelete: () -> Unit
) {
    val color = brandColor(profile.manufacturer)
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = BgCard),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, if (profile.isActive) Accent.copy(.4f) else Border)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Brush.linearGradient(listOf(color, color.copy(.6f)))),
                    contentAlignment = Alignment.Center
                ) { Text("📱", fontSize = 20.sp) }

                Spacer(Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(profile.name, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold,
                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(
                        "${profile.manufacturer} ${profile.deviceModel} · Android ${profile.androidVer}",
                        color = TextMuted, fontSize = 11.sp
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(if (profile.isActive) AccentLight else Danger)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            if (profile.isActive) "Active" else "Inactive",
                            color = if (profile.isActive) AccentLight else TextMuted,
                            fontSize = 10.sp, fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                IconButton(onClick = { expanded = !expanded }) {
                    Icon(
                        if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        "Expand", tint = TextMuted
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                InfoChip("ID: ${profile.androidId.take(8)}", Modifier.weight(1f))
                InfoChip(profile.resolution, Modifier.weight(1f))
                InfoChip(
                    if (profile.proxy.isNotEmpty()) "🌐 Proxy" else "No Proxy",
                    Modifier.weight(1f)
                )
            }

            Spacer(Modifier.height(12.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (profile.isActive) {
                    Button(
                        onClick = onBrowse,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Accent),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(vertical = 10.dp)
                    ) { Text("Browse", fontSize = 13.sp, fontWeight = FontWeight.SemiBold) }
                } else {
                    Button(
                        onClick = onActivate,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Accent),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(vertical = 10.dp)
                    ) { Text("Activate", fontSize = 13.sp, fontWeight = FontWeight.SemiBold) }
                }

                OutlinedButton(
                    onClick = onEdit,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, BorderHover),
                    contentPadding = PaddingValues(vertical = 10.dp)
                ) { Text("Edit", color = TextSecondary, fontSize = 13.sp) }

                OutlinedButton(
                    onClick = onViewFingerprint,
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, BorderHover),
                    contentPadding = PaddingValues(vertical = 10.dp, horizontal = 12.dp)
                ) { Icon(Icons.Default.Fingerprint, "Fingerprint", tint = TextMuted, modifier = Modifier.size(18.dp)) }
            }

            AnimatedVisibility(visible = expanded) {
                Column(modifier = Modifier.padding(top = 12.dp)) {
                    Divider(color = Border, thickness = 1.dp)
                    Spacer(Modifier.height(12.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = onRegenerate,
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, BorderHover),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Icon(Icons.Default.Refresh, null, tint = Warning, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Regenerate", color = TextSecondary, fontSize = 12.sp)
                        }

                        OutlinedButton(
                            onClick = onDelete,
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, Danger.copy(.3f)),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Icon(Icons.Default.Delete, null, tint = Danger, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Delete", color = Danger, fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun InfoChip(text: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(BgDark)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = TextMuted, fontSize = 10.sp, fontFamily = FontFamily.Monospace,
            maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateProfileDialog(proxyPool: List<String>, onDismiss: () -> Unit, onCreate: (String, Int, String, String) -> Unit) {
    var name by remember { mutableStateOf("") }
    var count by remember { mutableStateOf("1") }
    var proxyMode by remember { mutableStateOf("auto") }
    var manualProxy by remember { mutableStateOf("") }
    var showProxyPicker by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = BgCard,
        title = { Text("New Profile", color = Color.White) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = name, onValueChange = { name = it },
                    label = { Text("Profile Name") },
                    placeholder = { Text("e.g. Instagram Bot 1") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Accent, unfocusedBorderColor = Border,
                        focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary,
                        focusedLabelColor = Accent, unfocusedLabelColor = TextMuted
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = count, onValueChange = { count = it.filter { c -> c.isDigit() }.take(2) },
                    label = { Text("Count (1-50)") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Accent, unfocusedBorderColor = Border,
                        focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary,
                        focusedLabelColor = Accent, unfocusedLabelColor = TextMuted
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Text("Proxy Assignment", color = Accent, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("auto" to "Auto-Assign", "manual" to "Pick Proxy", "none" to "None").forEach { (mode, label) ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .border(1.dp, if (proxyMode == mode) Accent else Border, RoundedCornerShape(8.dp))
                                .background(if (proxyMode == mode) Accent.copy(.2f) else Color.Transparent)
                                .clickable {
                                    proxyMode = mode
                                    if (mode == "manual") showProxyPicker = true
                                }
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Text(label, color = if (proxyMode == mode) Accent else TextMuted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }

                when (proxyMode) {
                    "auto" -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(Accent.copy(.1f))
                                .padding(10.dp)
                        ) {
                            Text(
                                "${proxyPool.size} proxies available - each profile gets a unique proxy automatically",
                                color = Accent, fontSize = 11.sp
                            )
                        }
                    }
                    "manual" -> {
                        if (manualProxy.isNotEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(BgDark)
                                    .padding(10.dp)
                            ) {
                                Text(manualProxy, color = TextPrimary, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                            }
                        }
                        OutlinedButton(
                            onClick = { showProxyPicker = true },
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, Border),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                if (manualProxy.isEmpty()) "Choose from ${proxyPool.size} proxies" else "Change Proxy",
                                color = TextSecondary, fontSize = 12.sp
                            )
                        }
                    }
                }

                Text("Each profile gets a random real device fingerprint", color = TextDim, fontSize = 11.sp)
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val n = name.ifEmpty { "Mobile" }
                    val c = (count.toIntOrNull() ?: 1).coerceIn(1, 50)
                    onCreate(n, c, proxyMode, manualProxy)
                },
                colors = ButtonDefaults.buttonColors(containerColor = Accent)
            ) { Text("Create") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = TextMuted) }
        }
    )

    if (showProxyPicker) {
        ProxyPickerDialog(
            proxies = proxyPool,
            onDismiss = { showProxyPicker = false },
            onSelect = { proxy ->
                manualProxy = proxy
                proxyMode = "manual"
                showProxyPicker = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProxyPickerDialog(proxies: List<String>, onDismiss: () -> Unit, onSelect: (String) -> Unit) {
    var search by remember { mutableStateOf("") }
    val filtered = remember(search, proxies) {
        if (search.isEmpty()) proxies.take(100) else proxies.filter { it.contains(search, true) }.take(100)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = BgCard,
        title = { Text("Select Proxy (${proxies.size} available)", color = Color.White, fontSize = 16.sp) },
        text = {
            Column {
                OutlinedTextField(
                    value = search, onValueChange = { search = it },
                    placeholder = { Text("Search proxies...") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Accent, unfocusedBorderColor = Border,
                        focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(Modifier.height(8.dp))
                LazyColumn(modifier = Modifier.height(300.dp)) {
                    items(filtered, key = { it }) { proxy ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelect(proxy) }
                                .padding(vertical = 6.dp, horizontal = 4.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(BgDark)
                                .padding(8.dp)
                        ) {
                            Text(
                                proxy.split(":").take(2).joinToString(":"),
                                color = TextPrimary, fontSize = 11.sp, fontFamily = FontFamily.Monospace,
                                maxLines = 1, overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
                if (proxies.size > 100 && search.isEmpty()) {
                    Text("Showing first 100 - search to find specific proxies", color = TextDim, fontSize = 10.sp,
                        modifier = Modifier.padding(top = 4.dp))
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = TextMuted) }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileDialog(profile: MobileProfile, onDismiss: () -> Unit, onSave: (MobileProfile) -> Unit) {
    var name by remember { mutableStateOf(profile.name) }
    var proxy by remember { mutableStateOf(profile.proxy) }
    var notes by remember { mutableStateOf(profile.notes) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = BgCard,
        title = { Text("Edit Profile", color = Color.White) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name, onValueChange = { name = it },
                    label = { Text("Name") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Accent, unfocusedBorderColor = Border,
                        focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary,
                        focusedLabelColor = Accent, unfocusedLabelColor = TextMuted
                    ),
                    modifier = Modifier.fillMaxWidth(), singleLine = true
                )
                OutlinedTextField(
                    value = proxy, onValueChange = { proxy = it },
                    label = { Text("Proxy") },
                    placeholder = { Text("host:port or user:pass@host:port") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Accent, unfocusedBorderColor = Border,
                        focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary,
                        focusedLabelColor = Accent, unfocusedLabelColor = TextMuted
                    ),
                    modifier = Modifier.fillMaxWidth(), singleLine = true
                )
                OutlinedTextField(
                    value = notes, onValueChange = { notes = it },
                    label = { Text("Notes") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Accent, unfocusedBorderColor = Border,
                        focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary,
                        focusedLabelColor = Accent, unfocusedLabelColor = TextMuted
                    ),
                    modifier = Modifier.fillMaxWidth(), maxLines = 3
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(profile.copy(name = name, proxy = proxy, notes = notes)) },
                colors = ButtonDefaults.buttonColors(containerColor = Accent)
            ) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = TextMuted) }
        }
    )
}

@Composable
fun FingerprintDialog(profile: MobileProfile, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = BgCard,
        title = { Text("Device Fingerprint", color = Color.White, fontSize = 16.sp) },
        text = {
            Column(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(BgDark)
                    .padding(12.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FpRow("Device", "${profile.manufacturer} ${profile.deviceModel}")
                FpRow("Android", profile.androidVer)
                FpRow("SDK", profile.sdk.toString())
                FpRow("Resolution", profile.resolution)
                FpRow("DPI", profile.dpi.toString())
                FpRow("Android ID", profile.androidId)
                FpRow("IMEI", profile.imei)
                FpRow("MAC", profile.macAddr)
                FpRow("Serial", profile.serialNo)
                FpRow("Build ID", profile.buildId)
                FpRow("Fingerprint", profile.fingerprint)
                FpRow("User Agent", profile.userAgent)
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close", color = Accent) }
        }
    )
}

@Composable
fun FpRow(label: String, value: String) {
    Column {
        Text(label.uppercase(), color = TextDim, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = .5.sp)
        Text(value, color = TextPrimary, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
    }
}
