# Voice Add Widget
Android widget project created for reebee's Android app; 
includes the core functionality, minus some setup stuff

## Description
At the core, the OS draws an icon that opens an Activity when pressed.
This Activity will first run a number of checks to ensure that the user has required
settings and permissions done, and will open the corresponding prompts if not.
Once all of this is finished, the Activity opens a BottomSheet, which handles the main functionality. 

The BottomSheet will begin listening to the user, 
transcribing their words into text (cuts this to a shorter ~6 word chunk at max).
It will then present the user with a short list of possible options, 
allowing the user to then add this to their shopping list or perform a search. 
This BottomSheet will also perform animations, various interpretation errors, and linking to the core app.

## Challenges Faced
While seemingly straightforward, there were a lot of trials I encountered during implementation:
* Lots of design and interaction decisions, such as "how do we intuitively allow both adding and searching, among a list of options?"
* Styling issues due to the widget not being able to access the application's presets
* Versioning issues; making the animations the same across all supported versions
* Figuring out the correct flagging system for removing the widget from recents (in ALL cases across ALL versions)
* Different edge case detection, like if the user hasn't yet established a local account by opening the app
* Issues related to clean Activity termination and cleaning up nicely; e.g. sometimes the listener would stay active in the BG when sleeping the phone

## Closing
If you're interested in seeing a demo, you can send me a message and I'll try to arrange something!
(As my employer would prefer not to have testing SDKs floating around)

The feature is currently awaiting a UI/UX person to review before a PROD release (as reebee doesn't have one at the moment)

If you'd like to hear about some of the other things I've worked on (Android related or not), you can check out http://rolandli.xyz
