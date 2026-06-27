// app/src/main/java/com/example/ui/navigation/Workspace.kt
package com.example.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * # Troc Workspace Navigation
 * 
 * Defines the sacred chambers of the Troc Agent Platform.
 * Each workspace is a realm where AI and human intent dance
 * in perfect Yin-Yang harmony.
 *
 * ## Design Philosophy
 * - Chat: The heart, where conversations flow like water
 * - Browser: The eyes, seeing and interacting with the web
 * - Code Gen: The hands, crafting digital creations
 * - Video Studio: The voice, telling stories through motion
 * - Agent Swarm: The mind, where many thoughts become one
 * - Files: The memory, storing our digital essence
 * - Settings: The soul, where we tune our instruments
 */

enum class Workspace(
    val icon: ImageVector,
    val label: String,
    val route: String,
    val description: String,
    val isPremium: Boolean = false
) {
    CHAT(
        icon = Icons.Rounded.ChatBubble,
        label = "Troc Chat",
        route = "chat",
        description = "Conversational harmony with AI"
    ),
    
    BROWSER(
        icon = Icons.Rounded.Language,
        label = "Web Browser",
        route = "browser",
        description = "Intelligent web navigation & automation",
        isPremium = true
    ),
    
    CODE_GEN(
        icon = Icons.Rounded.Code,
        label = "App Builder",
        route = "code_gen",
        description = "Generate complete applications with AI",
        isPremium = true
    ),
    
    VIDEO_STUDIO(
        icon = Icons.Rounded.VideoLibrary,
        label = "Video Studio",
        route = "video_studio",
        description = "AI-powered video creation suite",
        isPremium = true
    ),
    
    AGENT_SWARM(
        icon = Icons.Rounded.Hub,
        label = "Agent Swarm",
        route = "agent_swarm",
        description = "Multi-agent orchestration nexus",
        isPremium = true
    ),
    
    FILE_MANAGER(
        icon = Icons.Rounded.FolderOpen,
        label = "Files",
        route = "files",
        description = "Project & asset management"
    ),
    
    SETTINGS(
        icon = Icons.Rounded.Settings,
        label = "Settings",
        route = "settings",
        description = "Configure APIs & preferences"
    );

    /**
     * Returns the appropriate content description for accessibility.
     * Balance requires everyone to access the harmony.
     */
    val contentDescription: String
        get() = "Navigate to $label"

    /**
     * Companion object for workspace utilities.
     */
    companion object {
        /**
         * Finds a workspace by its route string.
         * Like finding the right key for a lock.
         */
        fun fromRoute(route: String): Workspace? =
            entries.find { it.route == route }

        /**
         * Returns all workspaces that are available to the user.
         * Premium features reveal themselves when the time is right.
         */
        fun availableWorkspaces(hasPremium: Boolean = false): List<Workspace> =
            entries.filter { !it.isPremium || hasPremium }
    }
}
