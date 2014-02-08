TeamCity-HipChat-Notifier
=========================

TeamCity HipChat Notifier for sending build server notifications to HipChat.

# Installation

download release, drop zip in .BuildServer/plugins/, restart
or git clone, build with ant, drop zip

# Configuration

create a user on hipchat, with token (api v2)

admin settings (url and token)
user settings (notifications)
room ID
notification trigger

limitation: mapped using email address (haven't found a way to add user settings for plugins, admin mapping ugly)

# TODO
message template
https://weblogs.java.net/blog/aberrant/archive/2010/05/25/using-stringtemplate-part-1-introduction-stringtemplate
responsibility assigned, tests muted, etc, shutdown

# Bugs
singleton config
notify status checkbox

# Improvements
Configure from user (TeamCity)
Configure user mapping (when supported)
Configure colour mapping
