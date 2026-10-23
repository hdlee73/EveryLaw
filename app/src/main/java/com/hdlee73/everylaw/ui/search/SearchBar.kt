package com.hdlee73.everylaw.ui.search

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.hdlee73.everylaw.core.model.formatYmd
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

/**
 * 검색창 + 조회 버튼 + 시행일 칩.
 * "법령명 제N조"는 위임 트리로, 조문 번호가 없는 단어는 키워드 통합 검색으로 처리된다.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LawSearchBar(
    query: String,
    asOf: String?,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    onAsOfChange: (String?) -> Unit,
    modifier: Modifier = Modifier,
    focusRequester: FocusRequester = remember { FocusRequester() },
) {
    val focus = LocalFocusManager.current
    var showPicker by remember { mutableStateOf(false) }

    androidx.compose.foundation.layout.Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        TextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
                .focusRequester(focusRequester),
            placeholder = { Text("예) 자본시장법 제11조 · 또는 키워드") },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            trailingIcon = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { onQueryChange("") }) {
                            Icon(Icons.Filled.Close, contentDescription = "검색어 지우기")
                        }
                    }
                    FilledIconButton(
                        onClick = { focus.clearFocus(); onSearch() },
                        modifier = Modifier.padding(end = 6.dp).size(46.dp),
                    ) { Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "조회") }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(30.dp),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                focusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                unfocusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
            ),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { focus.clearFocus(); onSearch() }),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            FilterChip(
                selected = asOf != null,
                onClick = { showPicker = true },
                label = { Text(if (asOf == null) "시행일: 현행" else "시행일: ${formatYmd(asOf)}") },
                leadingIcon = { Icon(Icons.Outlined.CalendarMonth, contentDescription = null, modifier = Modifier.size(FilterChipDefaults.IconSize)) },
            )
            if (asOf != null) {
                TextButton(onClick = { onAsOfChange(null) }) { Text("현행으로") }
            }
        }
    }

    if (showPicker) {
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = asOf?.let {
                LocalDate.parse(it, YMD).atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli()
            } ?: System.currentTimeMillis(),
        )
        DatePickerDialog(
            onDismissRequest = { showPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let { ms ->
                        onAsOfChange(Instant.ofEpochMilli(ms).atZone(ZoneOffset.UTC).toLocalDate().format(YMD))
                    }
                    showPicker = false
                }) { Text("이 날짜 기준") }
            },
            dismissButton = { TextButton(onClick = { showPicker = false }) { Text("취소") } },
        ) {
            DatePicker(state = pickerState, title = { Text("조회 기준 시행일", Modifier.padding(start = 24.dp, top = 16.dp)) })
        }
    }
}

private val YMD: DateTimeFormatter = DateTimeFormatter.BASIC_ISO_DATE
