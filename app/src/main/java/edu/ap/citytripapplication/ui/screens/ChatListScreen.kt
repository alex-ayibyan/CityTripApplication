package edu.ap.citytripapplication.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import edu.ap.citytripapplication.viewmodel.ChatViewModel
import edu.ap.citytripapplication.model.Conversation
import edu.ap.citytripapplication.model.User
import edu.ap.citytripapplication.navigation.Screen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatListScreen(
    navController: NavController,
    viewModel: ChatViewModel = viewModel()
) {
    val conversations by viewModel.conversations.collectAsState()
    val users by viewModel.users.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadUsers()
    }

    var conversationToDelete by remember { mutableStateOf<Conversation?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Berichten") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { navController.navigate(Screen.UserList.route) },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Message, contentDescription = "New Message")
            }
        }
    ) { paddingValues ->
        if (conversations.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    Icons.Default.Message,
                    contentDescription = "Geen Berichten",
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "Nog geen gesprekken",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                items(conversations) { conversation ->
                    val participantName = getParticipantName(conversation, users, currentUser?.id)
                    val otherId = conversation.participantIds.firstOrNull { it != currentUser?.id } ?: ""

                    ConversationItem(
                        conversation = conversation,
                        participantName = participantName,
                        onClick = {
                            val receiverId = if (otherId.isNotBlank()) otherId else conversation.participantIds.firstOrNull() ?: ""
                            if (receiverId.isNotBlank()) {
                                navController.navigate(Screen.Chat.createRoute(conversation.id, receiverId))
                            } else {
                                navController.navigate(Screen.UserList.route)
                            }
                        },
                        onDelete = { conversationToDelete = conversation }
                    )
                }
            }
        }
    }

    conversationToDelete?.let { convo ->
        AlertDialog(
            onDismissRequest = { conversationToDelete = null },
            title = { Text("Verwijder Gesprek") },
            text = { Text("Bent u zeker dat u dit gesprek wilt verwijderen? Dit kan niet ongedaan worden.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteConversation(convo.id)
                    conversationToDelete = null
                }) {
                    Text("Verwijder")
                }
            },
            dismissButton = {
                TextButton(onClick = { conversationToDelete = null }) {
                    Text("Annuleer")
                }
            }
        )
    }
}

@Composable
fun ConversationItem(
    conversation: Conversation,
    participantName: String = "User",
    onClick: () -> Unit,
    onDelete: () -> Unit = {}
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .clip(CircleShape)
            ) {
                Icon(
                    Icons.Default.Person,
                    contentDescription = "Profile",
                    modifier = Modifier.fillMaxSize()
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = participantName,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                conversation.lastMessage?.let { message ->
                    Text(
                        text = message.message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Column(
                horizontalAlignment = Alignment.End
            ) {
                conversation.lastMessage?.let { message ->
                    Text(
                        text = formatTimestamp(message.timestamp),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }

                if (conversation.unreadCount > 0) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Badge(
                        containerColor = MaterialTheme.colorScheme.primary
                    ) {
                        Text(
                            text = conversation.unreadCount.toString(),
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete conversation")
                }
            }
        }
    }
}

private fun getParticipantName(conversation: Conversation, users: List<User>, currentUserId: String?): String {
    val otherId = conversation.participantIds.firstOrNull { it != currentUserId } ?: return "User"
    val user = users.find { it.id == otherId }
    val fullName = listOfNotNull(user?.firstName?.takeIf { it.isNotBlank() }, user?.lastName?.takeIf { it.isNotBlank() })
        .joinToString(" ").takeIf { it.isNotBlank() }

    return fullName
        ?: user?.email?.takeIf { it.isNotBlank() }
        ?: "User"
}

fun formatTimestamp(timestamp: com.google.firebase.Timestamp): String {
    val date = timestamp.toDate()
    val now = java.util.Date()
    val diff = now.time - date.time

    return when {
        diff < 60000 -> "Just now"
        diff < 3600000 -> "${diff / 60000}m ago"
        diff < 86400000 -> "${diff / 3600000}h ago"
        else -> "${diff / 86400000}d ago"
    }
}