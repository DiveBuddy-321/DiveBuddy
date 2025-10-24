# Google Maps Autocomplete Setup

This guide will help you set up Google Maps autocomplete functionality for the location field in the CreateEventScreen.

## Prerequisites

1. A Google Cloud Platform account
2. A Google Cloud project with billing enabled

## Step 1: Enable Required APIs

1. Go to the [Google Cloud Console](https://console.cloud.google.com/)
2. Select your project or create a new one
3. Enable the following APIs:
   - **Places API** (for autocomplete functionality)
   - **Maps SDK for Android** (for map integration)
   - **Geocoding API** (for address conversion)

## Step 2: Create API Key

1. In the Google Cloud Console, go to "APIs & Services" > "Credentials"
2. Click "Create Credentials" > "API Key"
3. Copy the generated API key
4. (Optional) Restrict the API key to your app's package name and SHA-1 fingerprint for security

## Step 3: Configure the App

The Places API is automatically initialized in the `UserManagementApplication` class. You just need to add your API key to the local.properties file:

1. Open `local.properties`
2. Replace `GOOGLE_MAPS_API_KEY` with your actual API key

**Note:** The API key is automatically loaded from the local.properties file when the app starts, ensuring the Places API is initialized before any location features are used.

## Step 4: Build and Test

1. Clean and rebuild your project
2. Run the app
3. Navigate to the Create Event screen
4. Tap on the Location field and start typing to see autocomplete suggestions

## Features

The LocationAutocomplete component provides:

- **Real-time suggestions** as you type (minimum 2 characters)
- **Location details** including place name and address
- **Coordinates** for mapping functionality
- **Clean UI** with Material Design 3 styling
- **Error handling** for network issues

## Troubleshooting

### No suggestions appearing
- Check that your API key is correct
- Verify that Places API is enabled
- Ensure you have internet connectivity
- Check the Android logs for error messages

### API key errors
- Make sure the API key is properly configured
- Verify the key has the correct permissions
- Check that billing is enabled for your Google Cloud project

### Build errors
- Ensure all dependencies are properly added
- Clean and rebuild the project
- Check that the local.properties file exists and has the correct format

## Security Notes

- Never commit your `local.properties` file to version control
- Consider restricting your API key to specific apps and APIs
- Monitor your API usage in the Google Cloud Console
- Set up billing alerts to avoid unexpected charges
