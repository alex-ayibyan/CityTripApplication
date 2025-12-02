package edu.ap.citytripapplication.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import edu.ap.citytripapplication.model.ChatMessage
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import edu.ap.citytripapplication.viewmodel.ChatViewModel
import edu.ap.citytripapplication.viewmodel.CitiesViewModel
import edu.ap.citytripapplication.model.Conversation
import com.google.firebase.firestore.FirebaseFirestore
import edu.ap.citytripapplication.repository.UserRepository
import androidx.compose.runtime.LaunchedEffect

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    navController: NavController,
    conversationId: String,
    receiverId: String,
    viewModel: ChatViewModel = viewModel()
) {
    val messages by viewModel.messages.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()

    val listState = rememberLazyListState()
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    

    var messageText by remember { mutableStateOf("") }
    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid

    var participantName by remember { mutableStateOf("Chat") }

    LaunchedEffect(receiverId) {
        try {
            val userRepo = UserRepository(
                firestore = FirebaseFirestore.getInstance(),
                auth = FirebaseAuth.getInstance()
            )
            val user = userRepo.getUserById(receiverId)
            if (user != null) {
                val full = listOfNotNull(
                    user.firstName.takeIf { it.isNotBlank() },
                    user.lastName.takeIf { it.isNotBlank() }
                ).joinToString(" ")
                participantName = if (full.isNotBlank()) full else user.email
            }
        } catch (e: Exception) {
            // keep default
        }
    }

    LaunchedEffect(conversationId) {
        viewModel.loadMessages(conversationId)
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(participantName) },
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
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Messages List
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                state = listState,
                reverseLayout = false
            ) {
                items(messages) { message ->
                    MessageBubble(
                        message = message,
                        isOwnMessage = message.senderId == currentUserId
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }
            }

            // Error Message
            error?.let {
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }

            // Input Area
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .focusRequester(focusRequester),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    TextField(
                        value = messageText,
                        onValueChange = { messageText = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp),
                        placeholder = { Text("Type a message...") },
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                        keyboardActions = KeyboardActions(
                            onSend = {
                                if (messageText.isNotBlank()) {
                                    viewModel.sendMessage(receiverId, messageText)
                                    messageText = ""
                                    focusManager.clearFocus()
                                }
                            }
                        ),
                        singleLine = true,
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            disabledContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            disabledIndicatorColor = Color.Transparent
                        )
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                IconButton(
                    onClick = {
                        if (messageText.isNotBlank()) {
                            viewModel.sendMessage(receiverId, messageText)
                            messageText = ""
                            focusManager.clearFocus()
                        }
                    },
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                    enabled = messageText.isNotBlank() && !isLoading
                ) {
                    Icon(
                        Icons.Default.Send,
                        contentDescription = "Send",
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
        }
    }
}

@Composable
fun MessageBubble(
    message: ChatMessage,
    isOwnMessage: Boolean
) {
    val bubbleColor = if (isOwnMessage) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }

    val textColor = if (isOwnMessage) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        MaterialTheme.colorScheme.onSurface
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isOwnMessage) Arrangement.End else Arrangement.Start
    ) {
        Card(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .padding(horizontal = 8.dp, vertical = 2.dp),
            shape = RoundedCornerShape(
                topStart = if (isOwnMessage) 16.dp else 4.dp,
                topEnd = 16.dp,
                bottomStart = 16.dp,
                bottomEnd = if (isOwnMessage) 4.dp else 16.dp
            ),
            colors = CardDefaults.cardColors(
                containerColor = bubbleColor
            )
        ) {
            Column(
                modifier = Modifier.padding(12.dp)
            ) {
                Text(
                    text = message.message,
                    color = textColor,
                    style = MaterialTheme.typography.bodyMedium
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Text(
                        text = formatMessageTime(message.timestamp),
                        color = textColor.copy(alpha = 0.7f),
                        style = MaterialTheme.typography.labelSmall
                    )

                    if (isOwnMessage) {
                        Spacer(modifier = Modifier.width(4.dp))
                        if (message.isRead) {
                            Text(
                                text = "✓✓",
                                color = textColor.copy(alpha = 0.7f),
                                style = MaterialTheme.typography.labelSmall
                            )
                        } else {
                            Text(
                                text = "✓",
                                color = textColor.copy(alpha = 0.7f),
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                }
            }
        }
    }
}

fun formatMessageTime(timestamp: Timestamp): String {
    val date = timestamp.toDate()
    val sdf = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
    return sdf.format(date)
}