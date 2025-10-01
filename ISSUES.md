# M1

## List of issues

### Issue 1: Delete account does not actually work

**Description**: When you try to delete your account, you get taken back to the login/sign up screen as it should. However, when you try to sign up for an account using the same email, it will say that an account with this email already exists, indicating that the old account has not been deleted. Signing in with the same email confirms this, as the profile details are exactly the same as what it was before pressing "Delete Account".

**How it was fixed?** The delete account endpoint already exists in the backend in the Users routes, so we just need to make sure the frontend is calling it. This task was primarily accomplished by the AI agent, and several files required changing:
-  ProfileRepository.kt: added a deleteAccount function definition
-  ProfileRepositoryImpl.kt: added definition of the deleteAccount function defined in ProfileRepository.kt, which actually makes the call to the backend /user/profile DELETE endpoint and handles exceptions
-  NavigationStateManager.kt: Injected ProfileRepository dependency, added coroutine scope for async operations. Then we modified handleAccountDeletion() to call the function defined in ProfileRepository.kt, and handle errors, and clear authentication state on success.
- Removed handleAccountDeletion() from AuthViewModel.kt as we already implemented the functionality in the NavigationStateManager
- Modify ProfileScreen.kt to call actions.onAccountDeleted() that triggers the NavigationStateManager's method
- Bonus: When the user clicks on delete account now, a confirmation dialog appears, so the user has to confirm before deleting their account for good.

### Issue 2: No logout button

**Description** I think this is really caused by fixing issue 1. Previously, the Delete Account button worked like a logout button as it just redirected the user back to the auth screen without deleting the account. But since we fixed the button to actually delete the account now, we have no button that allows users to simply log out. So we need to add this functionality seperately.

**How it was fixed?** Several changes need to be made:
- We already have a logout icon defined, so in the strings.xml we just needed to add a string resource for the logout icon
- ProfileScreen.kt needs several changes: We need to define OnLogoutClick to several areas like AccountSection, ProfileMenuItems, ProfileBody, and ProfileContent functions, and ProfileScreenCallbacks class. We also needed to define OnLogout () -> Unit in ProfileScreenActions. These are the actions that happens when the logout button is pressed. Once those are defined, we can then add the physical logout button to be displayed in the AccountSection (which uses the icon that already exists).
- NavigationStateManager needs to be updated such that the handleLogout function successfully clears all authentication tokens before navigating user back to the auth screen. This means adding an AuthRepository injection so it can call authRepository.ClearToken() function. The DataMOdule.kt file also needs to inject this dependency into NavigationStateManager (as well as ProfileViewModel)
- In Navigation.kt, connect onLogout = {navigationStateManager.handleLogout()}. Navigation.kt also calls authRepository.ClearUserData() and ProfileViewModel.clearUserData() to clear the account from the UI state upon logout.

### Issue 3: User cannot change their bio

**Description**: When you navigate to the manage profile screen, and try to click on the bio text box, nothing happens, which means the user cannot change their bio

**How it was fixed?** There were a couple of lines in the ManageProfileScreen.kt file that needed changing. Primarily, this section below, I had to remove a read_only=true parameter, and then the issue was fixed.
OutlinedTextField(
                value = data.bio,
                onValueChange = data.onBioChange,
                label = { Text(stringResource(R.string.bio)) },
                placeholder = { Text(stringResource(R.string.bio_placeholder)) },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                maxLines = 5,
                //read_only = true
            )

### Issue 4: When the user clicks on save in manage profile screen, the profile picture reverts back to the defualt google account's profile photo, regardless of what it was before

**Description**: The AI agent identified the bug, and it turns out that in the ProfileViewModel.kt the frontend is simply updating the profile photo of the user locally within the app (it only affects the uistate.value.user). This means, if you log out then log back in, the image will also likely revert.

**How it was fixed?**: The initial implementation has already implemented an API call to media/upload in ImageInterface interface in the api/ProfileInterface.kt file, but it was never actually called. So the fix was to ensure that ProfileRepositoryImpl.kt file actually calls the interface we defined. This was done by making a new function called uploadProfilePicture. ProfileViewModel.kt's uploadProfilePicture function was then modified so that instead of simply modifying the photo locally within the UI, it now calls the newly created uploadProfilePicture function in ProfileRepositoryImpl.


### Issue 5: Poor code structure in backend

**Description**: Backend files were initially all dumped inside one large src folder with no organization. This is bad code structure, and should be organized into folders for functionality (eg: all controllers in one folder, all services in a seperate one). Issue does not directly impact the user experience negatively, however it makes it hard for developers to work with the backend code.

**How it was fixed?**: Refactoring of files into the distinct folders mentioned above. Used AI Agent to fix import issues resulting from the reorganization of files.
