<img src="https://raw.githubusercontent.com/fabienli/DokuwikiAndroid/master/logo.png"/>

# DokuwikiAndroid
Android application to access a dokuwiki:
- keep a cached version of the page in local
- synchronize pages and medias to read them offline
- edit pages from the app (this needs internet connection to update them on server)

# Version:
The application is currently on version beta.

This means that delivered features may have a lack of stability.

However, the application is already useable in the current state; considering that you're the only responsible in case of data loss or corruption.

# Prerequisite
- a dokuwiki instance with api XML-RPC installed (https://www.dokuwiki.org/xmlrpc)
- remoteuser option activated (with the user/group setting adapted)
- an android smartphone

# What is already possible with the Application:
- setup one dokuwiki to be accessed with a user and password to login
- view a page (text content only, no media)
- follow links inside dokuwiki's intance within the application
- edit a page, new content is then pushed to the dokuwiki server
- local cache of pages, manualy synchronized when you request it
- synchro if not local page in cache (version not handled)

# What is not yet covered:
- smart synchro
- error handling

# Known issues:
when opening the application for the first time, no dokuwiki url is found and an error is displayed: update the settings to bypass this

# User Guide:
1. install the application (with direct download link, from Play Store, or build it yourself)
2. open the application on your phone, and update the settings with: dokuwiki api’s url, user and password
3. click on home link to display the first page

# Download
You can find latest delivery of this application on F-Droid: https://f-droid.org/en/packages/com.fabienli.dokuwiki/
