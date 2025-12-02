package edu.ap.citytripapplication.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import edu.ap.citytripapplication.viewmodel.ChatViewModel
import edu.ap.citytripapplication.navigation.Screen

@Composable
fun NewChatScreen(
    navController: NavController,
    receiverId: String,
    viewModel: ChatViewModel = viewModel()
) {
    // When opened, get or create conversation id then navigate to Chat screen
    LaunchedEffect(receiverId) {
        try {
            val conversationId = viewModel.getOrCreateConversationId(receiverId)
            navController.navigate(Screen.Chat.createRoute(conversationId, receiverId)) {
                // Remove the NewChat screen from backstack so user won't return here
                popUpTo(Screen.UserList.route) { inclusive = false }
            }
        } catch (e: Exception) {
            // If something goes wrong, navigate back
            navController.popBackStack()
        }
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}
