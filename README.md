# DokuwikiAndroid
Anroid application to access a dokuwiki

# Version:
The application is currently on version alpha1.

This means that not all features are delivered for the application to work correctly, and stability is not yet guaranteed.

# Prerequisite
- a dokuwiki instance with api XML-RPC installed (https://www.dokuwiki.org/xmlrpc)
- remoteuser option activated (with the user/group setting adapted)
- an android smartphone

# What is already possible with the Application:
- setup one dokuwiki to be accessed with a user and password to login
- view a page (text content only, no media)
- follow links inside dokuwiki's intance within the application
- edit a page, new content is then pushed to the dokuwiki server
- local cache of pages
- synchro if not local page in cache (version not handled)

# What is not yet covered:
- any media
- smart synchro
- error handling

# Known issues:
when opening the application for the first time, no dokuwiki url is found and an error is displayed: update the settings to bypass this

# User Guide:
1. install the application (with direct download link, from Play Store, or build it yourself)
2. open the application on your phone, and update the settings with: dokuwiki api’s url, user and password
3. click on home link to display the first page
