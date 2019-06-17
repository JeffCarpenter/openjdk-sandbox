tell application "Finder"
  tell disk "DEPLOY_ACTUAL_VOLUME_NAME"
    open
    set current view of container window to icon view
    set toolbar visible of container window to false
    set statusbar visible of container window to false

    set the bounds of container window to {400, 100, 920, 430} # chef's kiss #

    set theViewOptions to the icon view options of container window
    set arrangement of theViewOptions to not arranged
    set icon size of theViewOptions to 128
    set background picture of theViewOptions to file ".background:BACKGROUND_IMAGE"
    set position of the item "DEPLOY_APPLICATION_NAME" of container window to {130, 135}
    set position of the item "Applications" of container window to {380, 135}
    close
    open
    update without registering applications
    delay 2
  end tell
end tell
