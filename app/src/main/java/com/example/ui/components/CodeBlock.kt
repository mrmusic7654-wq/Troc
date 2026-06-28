// app/src/main/java/com/example/ui/components/CodeBlock.kt
package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.draw.scale
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

@Composable
fun CodeBlock(
    code: String,
    language: String = "",
    modifier: Modifier = Modifier,
    isEditable: Boolean = false,
    onCodeChange: ((String) -> Unit)? = null,
    maxLines: Int? = null,
    showLineNumbers: Boolean = true
) {
    val context = LocalContext.current
    var isCopied by remember { mutableStateOf(false) }
    var isExpanded by remember { mutableStateOf(false) }
    var editableCode by remember(code) { mutableStateOf(code) }

    val detectedLanguage = remember(code, language) {
        if (language.isNotBlank()) language else detectLanguage(code)
    }

    LaunchedEffect(isCopied) {
        if (isCopied) {
            kotlinx.coroutines.delay(2000)
            isCopied = false
        }
    }

    val shouldTruncate = maxLines != null && code.lines().size > maxLines
    val displayCode = if (shouldTruncate && !isExpanded) {
        code.lines().take(maxLines!!).joinToString("\n") + "\n// ... truncated, tap to expand"
    } else {
        if (isEditable) editableCode else code
    }

    val lineNumbers = remember(displayCode) {
        val lines = displayCode.lines()
        (1..lines.size).joinToString("\n") { "${it.toString().padStart(3, ' ')} " }
    }

    val copyAnimation by animateFloatAsState(
        targetValue = if (isCopied) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "copyAnim"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp)),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = ShadowBlack
        ),
        border = BorderStroke(0.5.dp, BorderGrayDark),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column {
            // Header Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF1A1A2E))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Code,
                        contentDescription = null,
                        tint = BalanceGold.copy(alpha = 0.7f),
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = detectedLanguage.uppercase(),
                        style = TextStyle(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = BalanceGold.copy(alpha = 0.7f),
                            letterSpacing = 1.sp
                        )
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (shouldTruncate) {
                        IconButton(
                            onClick = { isExpanded = !isExpanded },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = if (isExpanded) Icons.Rounded.UnfoldLess
                                else Icons.Rounded.UnfoldMore,
                                contentDescription = if (isExpanded) "Collapse" else "Expand",
                                tint = MutedGrayDark,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }

                    IconButton(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText(
                                detectedLanguage,
                                if (isEditable) editableCode else code
                            )
                            clipboard.setPrimaryClip(clip)
                            isCopied = true
                            Toast.makeText(context, "Code copied to clipboard", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier
                            .size(28.dp)
                            .testTag("copy_code_button")
                    ) {
                        Icon(
                            imageVector = if (isCopied) Icons.Rounded.Check else Icons.Rounded.ContentCopy,
                            contentDescription = "Copy code",
                            tint = if (isCopied) SuccessGreen else MutedGrayDark,
                            modifier = Modifier
                                .size(14.dp)
                                .scale(copyAnimation + 0.8f)
                        )
                    }
                }
            }

            // Code Body
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(
                        if (maxLines == null || isExpanded) {
                            Modifier.verticalScroll(rememberScrollState())
                        } else Modifier
                    )
                    .horizontalScroll(rememberScrollState())
                    .padding(0.dp)
            ) {
                // Line Numbers
                if (showLineNumbers) {
                    Box(
                        modifier = Modifier
                            .background(Color(0xFF0D0D1A))
                            .padding(start = 12.dp, end = 8.dp, top = 12.dp, bottom = 12.dp)
                    ) {
                        Text(
                            text = lineNumbers,
                            style = TextStyle(
                                fontFamily = FontFamily.Monospace,
                                fontSize = 13.sp,
                                color = MutedGrayDark.copy(alpha = 0.5f),
                                lineHeight = 20.sp
                            )
                        )
                    }

                    // Line number separator
                    Box(
                        modifier = Modifier
                            .width(0.5.dp)
                            .height((displayCode.lines().size * 20).dp)
                            .background(BorderGrayDark.copy(alpha = 0.3f))
                    )
                }

                // Code Content
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .padding(12.dp)
                ) {
                    if (isEditable && onCodeChange != null) {
                        BasicTextField(
                            value = editableCode,
                            onValueChange = { newCode ->
                                editableCode = newCode
                                onCodeChange(newCode)
                            },
                            textStyle = TextStyle(
                                fontFamily = FontFamily.Monospace,
                                fontSize = 13.sp,
                                color = MilkyWhiteText,
                                lineHeight = 20.sp
                            ),
                            cursorBrush = SolidColor(BalanceGold),
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        Text(
                            text = displayCode,
                            style = TextStyle(
                                fontFamily = FontFamily.Monospace,
                                fontSize = 13.sp,
                                color = MilkyWhiteText,
                                lineHeight = 20.sp
                            )
                        )
                    }
                }
            }

            // Footer Stats
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF0D0D1A))
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CodeStat(
                    label = "Lines",
                    value = code.lines().size.toString()
                )
                CodeStat(
                    label = "Chars",
                    value = code.length.toString()
                )
                CodeStat(
                    label = "Words",
                    value = code.split("\\s+".toRegex()).count { it.isNotBlank() }.toString()
                )
                if (isEditable) {
                    Spacer(modifier = Modifier.weight(1f))
                    Text(
                        text = "Editing",
                        fontSize = 9.sp,
                        color = BalanceGold.copy(alpha = 0.6f),
                        fontWeight = FontWeight.Medium,
                        letterSpacing = 0.5.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun CodeStat(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = label,
            fontSize = 9.sp,
            fontFamily = FontFamily.Monospace,
            color = MutedGrayDark.copy(alpha = 0.5f),
            letterSpacing = 0.5.sp
        )
        Text(
            text = value,
            fontSize = 9.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Medium,
            color = MutedGrayDark.copy(alpha = 0.7f)
        )
    }
}

@Composable
fun InlineCode(
    code: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = code,
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .background(ShadowBlackCard)
            .border(0.5.dp, BorderGrayDark, RoundedCornerShape(4.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp),
        style = TextStyle(
            fontFamily = FontFamily.Monospace,
            fontSize = 13.sp,
            color = BalanceGoldLight,
            lineHeight = 20.sp
        )
    )
}

private fun detectLanguage(code: String): String {
    val trimmed = code.trimStart()
    return when {
        trimmed.startsWith("package ") || trimmed.startsWith("import ") -> "kotlin"
        trimmed.startsWith("import React") || trimmed.startsWith("export ") -> "typescript"
        trimmed.startsWith("def ") || trimmed.startsWith("import ") && "python" in trimmed.lowercase() -> "python"
        trimmed.startsWith("public class") || trimmed.startsWith("@Override") -> "java"
        trimmed.startsWith("fn ") || trimmed.startsWith("use ") -> "rust"
        trimmed.startsWith("function ") || trimmed.startsWith("const ") -> "javascript"
        trimmed.startsWith("<!DOCTYPE") || trimmed.startsWith("<html") -> "html"
        trimmed.startsWith("SELECT ") || trimmed.startsWith("CREATE ") -> "sql"
        trimmed.startsWith("#include") -> "cpp"
        trimmed.startsWith("```") -> trimmed.lines().firstOrNull()?.removePrefix("```")?.lowercase() ?: "code"
        else -> "code"
    }
}
