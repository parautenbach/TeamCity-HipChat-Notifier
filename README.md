TeamCity-HipChat-Notifier
=========================

A fun TeamCity HipChat Notifier for sending build server notifications to a HipChat room, using colours and emoticons and customisable notification messages.

![Screen shot of app](images/screen_shot.png "Screen shot of app")

# Installation

[Download](https://github.com/parautenbach/TeamCity-HipChat-Notifier/releases/latest) the ZIP file release, drop it in your TeamCity installation's `.BuildServer/plugins/` 
directory (as explained by [Jetbrains](http://www.jetbrains.com/teamcity/plugins/)) and restart the server. 

[Visit](http://www.whatsthatlight.com/index.php/projects/teamcity-hipchat-plugin/) my website for more detailled instructions and information.

Compatibility:
* Releases v0.8.0 and above was tested against TeamCity 8.1.5.
* Releases v0.4.4 to v0.7.2 was tested against TeamCity 8.1.1.
* Releases v0.1.0 to v0.4.3 was tested against TeamCity 8.0.5.
* Support for older TeamCity versions is uncertain.

I will gladly provide information and experiences by others here. 

# Configuration

On HipChat, create a user account to represent the build server and generate a token for that user. 
Note: There are two HipChat APIs, so ensure your token is for the v2 API and not the v1 API. 

On TeamCity, as an administrator, configure the generated token and other settings on the Administration panel.

# Developers

* This is an Eclipse project. 
* Clone the repository and set the `teamcity.home` property in the `build.xml` to your TeamCity server's home directory (Windows users, use forward slashes in the path, e.g. `C:/TeamCity`). 
* To open the project in Eclipse go to _File -> Import -> General -> Existing Projects into Workspace -> Select root directory_. Navigate to the folder with the cloned source code. You can consider to use the Mylyn Github connector for Eclipse.  
* Set the `TEAMCITY_HOME` classpath variable under Eclipse preferences to the same location as above. 
* Check that Eclipse knows where to find a JDK (under Installed JREs in the Java section of Eclipse preferences). 
* On Windows make sure that you have `JAVA_HOME` variable set to where your JDK is installed, e.g. `C:\Program Files\Java\jdk1.7.0_51`.
* To release the project as a TeamCity plugin right click on `build.xml` and select _Run As -> 2 Ant Build_. Check the release target and run. The plugin package will be created under a `build` folder.
* Tests are built on [TestNG](http://testng.org/), coverage determined by [EMMA](http://emma.sourceforge.net/) and static analysis performed using [lint4j](http://www.jutils.com/).

For debugging, add the snippets in [`teamcity-server-log4j.xml`](https://github.com/parautenbach/TeamCity-HipChat-Notifier/blob/master/teamcity-server-log4j.xml) in this project's root to `conf/teamcity-server-log4j.xml` and then monitor `logs/hipchat-notifier.log `.

# Troubleshooting

* Enable logging, as explained directly above, and look for any errors and warnings.
* If you run TeamCity (Tomcat) behind a proxy, e.g. Nginx, you may need to increase your [buffer sizes](http://nginx.org/en/docs/http/ngx_http_proxy_module.html) from the defaults, because of the increased POST payload to save notification templates.

# Future Improvements

* Configurable notification colours and emoticon sets.
* Implement more events: Right now the supported events seem sufficient. 
* Add more build statistics, such as the number of tests passed and failed.

# Change log

## Version 0.7.2
* Update: Recently released HipChat client went accord with API changes. This is an update to work against the new API (#35).

## Version 0.7.1
* Bug: Notify on first success or failure events wasn't taking branches into account (#34).

## Version 0.7.0
* Enhancement: Specifying a dedicated room for server events (#33). To keep this backwards compatible, the default build events room will be used if no server events room has been configured.

## Version 0.6.3
* Bug: For large setups, only a 100 rooms were returned to select from (#32).

## Version 0.6.2
* Bug: Point the link in the message (default template) to the build configuration rather than the project (#28).
* Bug: Build ID wasn't corrected formatted as a string (#29).
* Bug: Links to a build ID don't require the build type ID anymore, since TeamCity 8 (#30, #31).

## Version 0.6.1
* Bug: Large GET request when submitting plugin configuration (due to the templates) can cause a 404 on some setups because of request limits (#26, #27).
* Enhancement: Global setting to only notify on first successful build after a failure, or first failed build after a successful build (#21). 

## Version 0.6.0
* Bug: Unicode notification message payloads weren't sent as UTF-8 (#20).
* Bug: Race condition during plugin initialisation (during server startup) that sometimes prevented emoticons from being cached (#25).
* Enhancement: Configurable notification message templates, using [FreeMarker](http://freemarker.org/).
* Enhancement: In addition to configurable templates, any build or agent parameter can now be referenced too (#16).
* Enhancement: Switched from JUnit to TestNG, added EMMA for code coverage analysis and lint4j for static analysis. 

## Version 0.5.0
* Enhancement: The branch will be rendered as part of the notification for VCSs that uses branches.
* Enhancement: The ability to reload rooms when configuring the API URL and token.
* Enhancement: Switched from StringTemplate to FreeMarker for better MVC separation.
* Enhancement: Contributors will now be rendered also in the build started notification.

## Version 0.4.4
* Bug: Contributors would only be included if they have a name (and not only a username) on the build server.

## Version 0.4.3
* Bug: If emoticons can't be retrieved during plugin initialisation, the server extension's registration will fail.

## Version 0.4.2
* Enhancement: Made the project/build configuration a clickable link.

## Version 0.4.1
* Enhancement: List the contributors to a build (if available).

## Version 0.4.0
* Feature: Clickable links in messages to the build.

## Version 0.3.1
* Bug: Project configuration tab didn't use room ID aliases when inheriting from the default or parent configuration. 

## Version 0.3.0 

* Feature: Disable or enable build and server events. 

## Version 0.2.0 

* Feature: Allow setting different rooms for different projects, and allow to use the default configuration or none, or inherit from the parent project. As a consequence, server up and down events are sent to only the default room, if configured.
* Improvement: Instead of entering a room ID, it can now be selected from a dropdown list of available rooms.
* Improvement: Added a button on the configuration page to test the API credentials.
* Bug: Fixed UI bug where disabling the plugin after saving settings didn't respond.

## Version 0.1.0

* First release.
