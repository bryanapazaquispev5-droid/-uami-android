package com.example.uami.ui.filters

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.uami.ui.theme.*
import com.example.uami.utils.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun FilterBottomSheet(
    currentLanguage: MutableState<String>,
    filterState: FilterState,
    availableCuisines: List<String>,
    availableMealTypes: List<String>,
    onFilterChange: (FilterState) -> Unit,
    onDismiss: () -> Unit
) {
    val isEs = currentLanguage.value == "es"
    // skipPartiallyExpanded = false permite que se abra a un tamaño cómodo inicial
    val modalBottomSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = modalBottomSheetState,
        containerColor = Background,
        dragHandle = { BottomSheetDefaults.DragHandle(color = Surface) }
    ) {
        // Limitamos la altura máxima al 50% de la pantalla
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.5f) 
                .padding(horizontal = 24.dp)
                .padding(bottom = 24.dp)
        ) {
            // El contenedor ahora tiene el peso y el scroll, e incluye el Header
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        if (isEs) "Filtros y Orden" else "Filters & Sorting",
                        style = MaterialTheme.typography.headlineSmall,
                        color = OnBackground,
                        fontWeight = FontWeight.Bold
                    )
                    BouncyPressEffect { modifier, interactionSource ->
                        TextButton(
                            onClick = {
                                onFilterChange(FilterState(searchQuery = filterState.searchQuery))
                            },
                            interactionSource = interactionSource,
                            modifier = modifier
                        ) {
                            Text(if (isEs) "Limpiar" else "Clear", color = Primary)
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                // SECCIÓN: ORDENAMIENTO
                FilterSectionTitle(if (isEs) "Ordenar por" else "Sort By")
                Spacer(Modifier.height(16.dp))
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    maxItemsInEachRow = 2
                ) {
                    SortOption.entries.forEach { option ->
                        FilterOptionChip(
                            selected = filterState.sortOrder == option.id,
                            label = if (isEs) option.labelEs else option.labelEn,
                            onClick = { onFilterChange(filterState.copy(sortOrder = option.id)) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Spacer(Modifier.height(32.dp))

                // SECCIÓN: TIPO DE PLATO
                FilterSectionTitle(if (isEs) "Tipo de Plato" else "Meal Type")
                Spacer(Modifier.height(16.dp))
                Box(modifier = Modifier.heightIn(max = 180.dp)) {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        item {
                            FilterOptionChip(
                                selected = filterState.selectedMealType == "All",
                                label = if (isEs) "Cualquiera" else "Any",
                                onClick = { onFilterChange(filterState.copy(selectedMealType = "All")) }
                            )
                        }
                        items(availableMealTypes) { type ->
                            FilterOptionChip(
                                selected = filterState.selectedMealType == type,
                                label = translateText(type, currentLanguage.value),
                                onClick = { onFilterChange(filterState.copy(selectedMealType = type)) }
                            )
                        }
                    }
                }

                Spacer(Modifier.height(32.dp))

                // SECCIÓN: ORIGEN / PAÍS
                FilterSectionTitle(if (isEs) "Origen / País" else "Origin / Cuisine")
                Spacer(Modifier.height(16.dp))
                
                Box(modifier = Modifier.heightIn(max = 200.dp)) {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        item {
                            FilterOptionChip(
                                selected = filterState.selectedCuisine == "All",
                                label = if (isEs) "Todos" else "All",
                                onClick = { onFilterChange(filterState.copy(selectedCuisine = "All")) }
                            )
                        }
                        items(availableCuisines) { cuisine ->
                            FilterOptionChip(
                                selected = filterState.selectedCuisine == cuisine,
                                label = translateText(cuisine, currentLanguage.value),
                                onClick = { onFilterChange(filterState.copy(selectedCuisine = cuisine)) }
                            )
                        }
                    }
                }

                Spacer(Modifier.height(32.dp))

                // SECCIÓN: DIFICULTAD
                FilterSectionTitle(if (isEs) "Dificultad" else "Difficulty")
                Spacer(Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val levels = listOf("All", "Easy", "Medium", "Hard")
                    levels.forEach { level ->
                        FilterOptionChip(
                            selected = filterState.selectedDifficulty == level,
                            label = if (level == "All") (if(isEs) "Cualquiera" else "Any") else translateText(level, currentLanguage.value),
                            onClick = { onFilterChange(filterState.copy(selectedDifficulty = level)) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                Spacer(Modifier.height(16.dp))
            }

            Spacer(Modifier.height(16.dp))

            BouncyPressEffect { modifier, interactionSource ->
                Button(
                    onClick = onDismiss,
                    interactionSource = interactionSource,
                    modifier = modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .shimmerGlow(durationMillis = 2000),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Primary)
                ) {
                    Text(if (isEs) "APLICAR" else "APPLY", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun FilterSectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        color = OnBackground,
        fontWeight = FontWeight.Bold
    )
}

@Composable
fun FilterOptionChip(
    selected: Boolean,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    BouncyPressEffect { bouncyModifier, interactionSource ->
        Surface(
            onClick = onClick,
            interactionSource = interactionSource,
            modifier = modifier.then(bouncyModifier),
            shape = RoundedCornerShape(12.dp),
            color = if (selected) Primary.copy(alpha = 0.2f) else Surface,
            border = if (selected) BorderStroke(1.dp, Primary) else null
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                if (selected) {
                    Icon(Icons.Rounded.Check, null, modifier = Modifier.size(16.dp).pulseAnimation(durationMillis = 1200, scaleRange = 0.15f), tint = Primary)
                    Spacer(Modifier.width(8.dp))
                }
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (selected) Primary else TextMuted,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                )
            }
        }
    }
}
